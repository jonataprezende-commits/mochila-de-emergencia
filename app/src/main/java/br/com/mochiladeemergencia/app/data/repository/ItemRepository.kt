package br.com.mochiladeemergencia.app.data.repository

import br.com.mochiladeemergencia.app.data.local.dao.ItemDao
import br.com.mochiladeemergencia.app.data.local.entity.ItemEntity
import br.com.mochiladeemergencia.app.domain.model.Categoria
import br.com.mochiladeemergencia.app.domain.model.StatusItem
import kotlinx.coroutines.flow.Flow

/** Operações de itens da mochila, com validação defensiva dos dados gravados. */
class ItemRepository(private val dao: ItemDao) {

    fun observar(perfilId: Long): Flow<List<ItemEntity>> = dao.observarPorPerfil(perfilId)

    suspend fun listar(perfilId: Long): List<ItemEntity> = dao.listarPorPerfil(perfilId)

    suspend fun listarTodos(): List<ItemEntity> = dao.listarTodos()

    suspend fun buscar(id: Long): ItemEntity? = dao.buscarPorId(id)

    /**
     * Insere ou atualiza o item. Retorna o id do item salvo.
     * @throws IllegalArgumentException quando os dados obrigatórios são inválidos.
     */
    suspend fun salvar(item: ItemEntity): Long {
        val saneado = validarESanear(item)
        return if (saneado.id == 0L) {
            dao.inserir(saneado)
        } else {
            dao.atualizar(saneado)
            saneado.id
        }
    }

    suspend fun excluir(item: ItemEntity) = dao.excluir(item)

    /**
     * Marca o item como consumido/substituído: ele sai da mochila (status "Repor")
     * e perde a validade antiga, pois a próxima compra terá nova data.
     */
    suspend fun marcarComoConsumido(item: ItemEntity) {
        dao.atualizar(
            item.copy(
                aRepor = true,
                marcadoComoFalta = false,
                validadeEpochDay = null,
                atualizadoEm = System.currentTimeMillis()
            )
        )
    }

    /** Repõe o item: volta para a mochila com status "Tenho". */
    suspend fun repor(item: ItemEntity) {
        dao.atualizar(
            item.copy(
                aRepor = false,
                marcadoComoFalta = false,
                atualizadoEm = System.currentTimeMillis()
            )
        )
    }

    /** Marca o item como "Tenho" (sem alterar a lista de reposição). */
    suspend fun marcarComoReposto(item: ItemEntity) {
        dao.atualizar(
            item.copy(marcadoComoFalta = false, atualizadoEm = System.currentTimeMillis())
        )
    }

    /** Auditoria "Conferir mochila": define o estado do item em uma única gravação. */
    suspend fun aplicarConferencia(item: ItemEntity, resultado: StatusItem) {
        val atualizado = when (resultado) {
            StatusItem.TENHO ->
                item.copy(aRepor = false, marcadoComoFalta = false)
            StatusItem.FALTA ->
                item.copy(aRepor = false, marcadoComoFalta = true)
            // Substituir = consumido/vencido que sai da mochila e vai para a lista Repor.
            StatusItem.REPOR ->
                item.copy(aRepor = true, marcadoComoFalta = false, validadeEpochDay = null)
            StatusItem.VENCENDO,
            StatusItem.VENCIDO ->
                item.copy(aRepor = true, marcadoComoFalta = false, validadeEpochDay = null)
        }
        dao.atualizar(atualizado.copy(atualizadoEm = System.currentTimeMillis()))
    }

    private fun validarESanear(item: ItemEntity): ItemEntity {
        val nome = item.nome.trim().take(120)
        require(nome.isNotEmpty()) { "O nome do item é obrigatório." }
        require(item.quantidade > 0.0 && item.quantidade.isFinite()) {
            "A quantidade deve ser maior que zero."
        }
        item.validadeEpochDay?.let {
            require(it in VALIDADE_MINIMA..VALIDADE_MAXIMA) { "Data de validade inválida." }
        }
        // Somente nomes de arquivo simples, gerados pelo próprio app (sem caminhos).
        item.fotoArquivo?.let { nome ->
            require(Regex("^[A-Za-z0-9._-]{1,80}$").matches(nome) && !nome.contains("..")) {
                "Foto inválida."
            }
        }
        return item.copy(
            nome = nome,
            categoria = Categoria.dePersistido(item.categoria).name,
            quantidade = item.quantidade.coerceIn(0.01, 999999.0),
            unidade = item.unidade.trim().take(12).ifBlank { "un" },
            local = item.local.trim().take(80),
            observacao = item.observacao.trim().take(500),
            atualizadoEm = System.currentTimeMillis()
        )
    }

    companion object {
        /** 01/01/1970 até 31/12/2999 (epoch day). */
        val VALIDADE_MINIMA: Long = 0L
        val VALIDADE_MAXIMA: Long = 375_000L
    }
}
