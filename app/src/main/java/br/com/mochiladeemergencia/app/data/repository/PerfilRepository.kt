package br.com.mochiladeemergencia.app.data.repository

import br.com.mochiladeemergencia.app.data.local.dao.ChecklistDao
import br.com.mochiladeemergencia.app.data.local.dao.PerfilDao
import br.com.mochiladeemergencia.app.data.local.entity.PerfilEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Regras de perfis/família. */
class PerfilRepository(
    private val perfilDao: PerfilDao,
    private val checklistDao: ChecklistDao,
    private val checklistRepository: ChecklistRepository
) {

    private val mutex = Mutex()

    val perfis: Flow<List<PerfilEntity>> = perfilDao.observarTodos()

    /**
     * Garante que exista ao menos o perfil padrão "Casa".
     * Idempotente e protegido por Mutex contra chamadas concorrentes.
     */
    suspend fun garantirPerfilPadrao(): Long = mutex.withLock {
        val existentes = perfilDao.listarTodos()
        if (existentes.isNotEmpty()) {
            val primeiro = existentes.first()
            if (checklistDao.contarPorPerfil(primeiro.id) == 0) {
                checklistRepository.criarChecklistPadrao(primeiro.id)
            }
            return@withLock primeiro.id
        }
        val id = perfilDao.inserir(PerfilEntity(nome = "Casa"))
        checklistRepository.criarChecklistPadrao(id)
        id
    }

    suspend fun criar(nome: String): Long {
        val nomeLimpo = nome.trim().take(60)
        val id = perfilDao.inserir(PerfilEntity(nome = nomeLimpo))
        checklistRepository.criarChecklistPadrao(id)
        return id
    }

    /** Define/edita onde a mochila fica guardada. */
    suspend fun definirLocalizacao(perfil: PerfilEntity, localizacao: String) {
        perfilDao.atualizar(perfil.copy(localizacao = localizacao.trim().take(120)))
    }

    suspend fun renomear(perfil: PerfilEntity, novoNome: String) {
        val nomeLimpo = novoNome.trim().take(60)
        perfilDao.atualizar(perfil.copy(nome = nomeLimpo))
    }

    /** Exclui um perfil (itens/documentos/checklist caem em cascata). Nunca remove o último. */
    suspend fun excluir(perfil: PerfilEntity): ResultadoExclusaoPerfil {
        val todos = perfilDao.listarTodos()
        if (todos.size <= 1) return ResultadoExclusaoPerfil.UltimoPerfil
        perfilDao.excluir(perfil)
        return ResultadoExclusaoPerfil.Sucesso(todos.firstOrNull { it.id != perfil.id }?.id)
    }

    suspend fun buscarPorId(id: Long): PerfilEntity? = perfilDao.buscarPorId(id)
}

sealed interface ResultadoExclusaoPerfil {
    data object UltimoPerfil : ResultadoExclusaoPerfil
    data class Sucesso(val novoPerfilAtivoId: Long?) : ResultadoExclusaoPerfil
}
