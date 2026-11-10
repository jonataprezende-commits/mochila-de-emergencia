package br.com.mochiladeemergencia.app.data.repository

import br.com.mochiladeemergencia.app.data.local.dao.DocumentoDao
import br.com.mochiladeemergencia.app.data.local.entity.DocumentoEntity
import br.com.mochiladeemergencia.app.domain.model.TipoDocumento
import kotlinx.coroutines.flow.Flow

/**
 * Documentos do perfil: links (http/https) ou arquivos locais escolhidos pelo usuário
 * (content://). O app nunca baixa conteúdo por conta própria.
 */
class DocumentoRepository(private val dao: DocumentoDao) {

    fun observar(perfilId: Long): Flow<List<DocumentoEntity>> = dao.observarPorPerfil(perfilId)

    suspend fun listar(perfilId: Long): List<DocumentoEntity> = dao.listarPorPerfil(perfilId)

    suspend fun listarTodos(): List<DocumentoEntity> = dao.listarTodos()

    suspend fun buscar(id: Long): DocumentoEntity? = dao.buscarPorId(id)

    /** @throws IllegalArgumentException quando o destino (link/URI) não é aceitável. */
    suspend fun salvar(documento: DocumentoEntity): Long {
        val saneado = validarESanear(documento)
        return if (saneado.id == 0L) {
            dao.inserir(saneado)
        } else {
            dao.atualizar(saneado)
            saneado.id
        }
    }

    suspend fun excluir(documento: DocumentoEntity) = dao.excluir(documento)

    private fun validarESanear(documento: DocumentoEntity): DocumentoEntity {
        val titulo = documento.titulo.trim().take(120)
        require(titulo.isNotEmpty()) { "O título do documento é obrigatório." }

        val tipo = TipoDocumento.dePersistido(documento.tipo)
        val destino = documento.uri.trim().take(2048)
        require(destino.isNotEmpty()) { "Informe um link ou escolha um arquivo." }

        if (tipo == TipoDocumento.LINK) {
            val minusculo = destino.lowercase()
            require(minusculo.startsWith("http://") || minusculo.startsWith("https://")) {
                "O link deve começar com http:// ou https://."
            }
            require(!minusculo.startsWith("javascript:")) { "Link não permitido." }
        } else {
            require(destino.startsWith("content://")) {
                "Para arquivos, escolha um arquivo pelo seletor do Android."
            }
        }

        return documento.copy(
            titulo = titulo,
            tipo = tipo.name,
            uri = destino,
            categoria = documento.categoria.trim().take(40),
            observacao = documento.observacao.trim().take(1000),
            arquivoLocal = tipo != TipoDocumento.LINK
        )
    }
}
