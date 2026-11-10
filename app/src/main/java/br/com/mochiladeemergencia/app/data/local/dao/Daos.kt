package br.com.mochiladeemergencia.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.mochiladeemergencia.app.data.local.entity.ChecklistEntity
import br.com.mochiladeemergencia.app.data.local.entity.ContatoEntity
import br.com.mochiladeemergencia.app.data.local.entity.DocumentoEntity
import br.com.mochiladeemergencia.app.data.local.entity.ItemEntity
import br.com.mochiladeemergencia.app.data.local.entity.PerfilEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PerfilDao {

    @Query("SELECT * FROM perfis ORDER BY id ASC")
    fun observarTodos(): Flow<List<PerfilEntity>>

    @Query("SELECT * FROM perfis ORDER BY id ASC")
    suspend fun listarTodos(): List<PerfilEntity>

    @Query("SELECT * FROM perfis WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): PerfilEntity?

    @Query("SELECT COUNT(*) FROM perfis")
    suspend fun contar(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserir(perfil: PerfilEntity): Long

    @Update
    suspend fun atualizar(perfil: PerfilEntity)

    @Delete
    suspend fun excluir(perfil: PerfilEntity)

    @Query("DELETE FROM perfis")
    suspend fun excluirTodos()
}

@Dao
interface ItemDao {

    @Query(
        "SELECT * FROM itens WHERE perfilId = :perfilId " +
            "ORDER BY (validadeEpochDay IS NULL), validadeEpochDay ASC, nome COLLATE NOCASE ASC"
    )
    fun observarPorPerfil(perfilId: Long): Flow<List<ItemEntity>>

    @Query("SELECT * FROM itens WHERE perfilId = :perfilId")
    suspend fun listarPorPerfil(perfilId: Long): List<ItemEntity>

    @Query("SELECT * FROM itens")
    suspend fun listarTodos(): List<ItemEntity>

    @Query("SELECT * FROM itens WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): ItemEntity?

    @Query("SELECT * FROM itens WHERE perfilId = :perfilId AND chaveChecklist = :chave LIMIT 1")
    suspend fun buscarPorChaveChecklist(perfilId: Long, chave: String): ItemEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserir(item: ItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserirTodos(itens: List<ItemEntity>)

    @Update
    suspend fun atualizar(item: ItemEntity)

    @Delete
    suspend fun excluir(item: ItemEntity)

    @Query("DELETE FROM itens WHERE perfilId = :perfilId")
    suspend fun excluirPorPerfil(perfilId: Long)

    @Query("DELETE FROM itens")
    suspend fun excluirTodos()
}

@Dao
interface DocumentoDao {

    @Query("SELECT * FROM documentos WHERE perfilId = :perfilId ORDER BY titulo COLLATE NOCASE ASC")
    fun observarPorPerfil(perfilId: Long): Flow<List<DocumentoEntity>>

    @Query("SELECT * FROM documentos WHERE perfilId = :perfilId ORDER BY titulo COLLATE NOCASE ASC")
    suspend fun listarPorPerfil(perfilId: Long): List<DocumentoEntity>

    @Query("SELECT * FROM documentos")
    suspend fun listarTodos(): List<DocumentoEntity>

    @Query("SELECT * FROM documentos WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): DocumentoEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserir(documento: DocumentoEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserirTodos(documentos: List<DocumentoEntity>)

    @Update
    suspend fun atualizar(documento: DocumentoEntity)

    @Delete
    suspend fun excluir(documento: DocumentoEntity)

    @Query("DELETE FROM documentos")
    suspend fun excluirTodos()
}

@Dao
interface ChecklistDao {

    @Query("SELECT * FROM checklist WHERE perfilId = :perfilId ORDER BY ordem ASC, id ASC")
    fun observarPorPerfil(perfilId: Long): Flow<List<ChecklistEntity>>

    @Query("SELECT * FROM checklist WHERE perfilId = :perfilId ORDER BY ordem ASC, id ASC")
    suspend fun listarPorPerfil(perfilId: Long): List<ChecklistEntity>

    @Query("SELECT * FROM checklist")
    suspend fun listarTodos(): List<ChecklistEntity>

    @Query("SELECT COUNT(*) FROM checklist WHERE perfilId = :perfilId")
    suspend fun contarPorPerfil(perfilId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserirTodos(sugestoes: List<ChecklistEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserir(sugestao: ChecklistEntity): Long

    @Update
    suspend fun atualizar(sugestao: ChecklistEntity)

    @Delete
    suspend fun excluir(sugestao: ChecklistEntity)

    @Query("DELETE FROM checklist WHERE perfilId = :perfilId AND personalizado = 0")
    suspend fun excluirPadroesPorPerfil(perfilId: Long)

    @Query("DELETE FROM checklist")
    suspend fun excluirTodos()
}

@Dao
interface ContatoDao {

    @Query("SELECT * FROM contatos WHERE perfilId = :perfilId ORDER BY nome COLLATE NOCASE ASC")
    fun observarPorPerfil(perfilId: Long): Flow<List<ContatoEntity>>

    @Query("SELECT * FROM contatos WHERE perfilId = :perfilId ORDER BY nome COLLATE NOCASE ASC")
    suspend fun listarPorPerfil(perfilId: Long): List<ContatoEntity>

    @Query("SELECT * FROM contatos")
    suspend fun listarTodos(): List<ContatoEntity>

    @Query("SELECT * FROM contatos WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): ContatoEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserir(contato: ContatoEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserirTodos(contatos: List<ContatoEntity>)

    @Update
    suspend fun atualizar(contato: ContatoEntity)

    @Delete
    suspend fun excluir(contato: ContatoEntity)

    @Query("DELETE FROM contatos")
    suspend fun excluirTodos()
}
