package br.com.mochiladeemergencia.app.data.repository

import br.com.mochiladeemergencia.app.data.local.dao.ContatoDao
import br.com.mochiladeemergencia.app.data.local.entity.ContatoEntity
import kotlinx.coroutines.flow.Flow

/** Contato fixo de emergência (número público, discado via ACTION_DIAL). */
data class ContatoEmergencia(
    val nome: String,
    val numero: String,
    val descricao: String
)

object ContatosEmergenciaFixos {
    val LISTA: List<ContatoEmergencia> = listOf(
        ContatoEmergencia("SAMU", "192", "Atendimento médico de urgência"),
        ContatoEmergencia("Bombeiros", "193", "Incêndio, resgate e urgência"),
        ContatoEmergencia("Polícia Militar", "190", "Segurança e emergências policiais"),
        ContatoEmergencia("Defesa Civil", "199", "Desastres e riscos de desabamento, enchentes")
    )
}

/**
 * Contatos pessoais de emergência.
 * O telefone é saneado para dígitos e símbolos de discagem — nada é chamado pelo app
 * (a discagem é aberta no app de telefone via ACTION_DIAL).
 */
class ContatoRepository(private val dao: ContatoDao) {

    fun observar(perfilId: Long): Flow<List<ContatoEntity>> = dao.observarPorPerfil(perfilId)

    suspend fun listar(perfilId: Long): List<ContatoEntity> = dao.listarPorPerfil(perfilId)

    suspend fun listarTodos(): List<ContatoEntity> = dao.listarTodos()

    suspend fun buscar(id: Long): ContatoEntity? = dao.buscarPorId(id)

    /** @throws IllegalArgumentException quando nome ou telefone são inválidos. */
    suspend fun salvar(contato: ContatoEntity): Long {
        val saneado = validarESanear(contato)
        return if (saneado.id == 0L) {
            dao.inserir(saneado)
        } else {
            dao.atualizar(saneado)
            saneado.id
        }
    }

    suspend fun excluir(contato: ContatoEntity) = dao.excluir(contato)

    private fun validarESanear(contato: ContatoEntity): ContatoEntity {
        val nome = contato.nome.trim().take(80)
        require(nome.isNotEmpty()) { "Informe o nome do contato." }

        val telefone = sanearTelefone(contato.telefone)
        require(telefone.isNotEmpty()) { "Informe o telefone do contato." }
        require(telefone.any { it.isDigit() }) { "O telefone deve conter números." }
        require(telefone.length <= 24) { "Telefone longo demais." }

        return contato.copy(
            nome = nome,
            telefone = telefone,
            parentesco = contato.parentesco.trim().take(40),
            observacao = contato.observacao.trim().take(200)
        )
    }

    companion object {
        /** Mantém apenas dígitos e os símbolos aceitos pela discagem (+ * #). */
        fun sanearTelefone(entrada: String): String =
            entrada.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }.take(24)
    }
}
