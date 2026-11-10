package br.com.mochiladeemergencia.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import br.com.mochiladeemergencia.app.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Testa as migrações do banco: 1 → 2 (localização, foto, "a repor" e contatos)
 * e 2 → 3 (dica nas sugestões, forma de consumo e categorias renomeadas).
 *
 * Cria um banco no formato da versão 1, aplica exatamente o SQL da migração e confirma
 * que as colunas novas existem e que os dados antigos continuam lá.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class MigracaoBancoTest {

    @Test
    fun `migracao 1 para 2 preserva dados e adiciona colunas`() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        val arquivo = File(contexto.cacheDir, "migracao_teste.db")
        arquivo.delete()
        val db = SQLiteDatabase.openOrCreateDatabase(arquivo, null)

        // ---- esquema da versão 1 ----
        db.execSQL(
            "CREATE TABLE perfis (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "nome TEXT NOT NULL, criadoEm INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE itens (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "perfilId INTEGER NOT NULL, nome TEXT NOT NULL, categoria TEXT NOT NULL, " +
                "quantidade REAL NOT NULL, unidade TEXT NOT NULL, validadeEpochDay INTEGER, " +
                "local TEXT NOT NULL, observacao TEXT NOT NULL, marcadoComoFalta INTEGER NOT NULL, " +
                "chaveChecklist TEXT, criadoEm INTEGER NOT NULL, atualizadoEm INTEGER NOT NULL)"
        )
        db.execSQL("CREATE INDEX index_itens_perfilId ON itens (perfilId)")
        db.execSQL("CREATE INDEX index_itens_chaveChecklist ON itens (chaveChecklist)")

        val perfil = ContentValues().apply {
            put("nome", "Casa")
            put("criadoEm", 1000L)
        }
        val perfilId = db.insert("perfis", null, perfil)
        val item = ContentValues().apply {
            put("perfilId", perfilId)
            put("nome", "Água")
            put("categoria", "AGUA_E_ALIMENTOS")
            put("quantidade", 6.0)
            put("unidade", "L")
            put("validadeEpochDay", 20000L)
            put("local", "Cozinha")
            put("observacao", "")
            put("marcadoComoFalta", 0)
            put("criadoEm", 1000L)
            put("atualizadoEm", 1000L)
        }
        db.insert("itens", null, item)

        // ---- SQL da migração 1 -> 2 (mesmo conteúdo de AppDatabase.MIGRACAO_1_2) ----
        db.execSQL("ALTER TABLE perfis ADD COLUMN localizacao TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE itens ADD COLUMN aRepor INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE itens ADD COLUMN fotoArquivo TEXT")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS contatos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, perfilId INTEGER NOT NULL, " +
                "nome TEXT NOT NULL, telefone TEXT NOT NULL, parentesco TEXT NOT NULL, " +
                "observacao TEXT NOT NULL, criadoEm INTEGER NOT NULL, " +
                "FOREIGN KEY(perfilId) REFERENCES perfis(id) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_contatos_perfilId ON contatos (perfilId)")
        db.version = 2

        // ---- verificações ----
        db.rawQuery("PRAGMA table_info(perfis)", null).use { cursor ->
            val colunas = gerarLista(cursor, "name")
            assertTrue(colunas.contains("localizacao"))
        }
        db.rawQuery("PRAGMA table_info(itens)", null).use { cursor ->
            val colunas = gerarLista(cursor, "name")
            assertTrue(colunas.contains("aRepor"))
            assertTrue(colunas.contains("fotoArquivo"))
        }
        db.rawQuery("SELECT nome, localizacao FROM perfis", null).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Casa", cursor.getString(0))
            assertEquals("", cursor.getString(1))
        }
        db.rawQuery("SELECT nome, aRepor, fotoArquivo FROM itens", null).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Água", cursor.getString(0))
            assertEquals(0, cursor.getInt(1))
            assertTrue(cursor.isNull(2))
        }
        // Tabela de contatos criada e funcional.
        val contato = ContentValues().apply {
            put("perfilId", perfilId)
            put("nome", "Maria")
            put("telefone", "21999998888")
            put("parentesco", "Vizinha")
            put("observacao", "")
            put("criadoEm", 2000L)
        }
        assertTrue(db.insert("contatos", null, contato) > 0)
        db.rawQuery("SELECT COUNT(*) FROM contatos", null).use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        db.close()
        arquivo.delete()

        // A migração declarada no banco tem a versão correta.
        assertEquals(2, AppDatabase.MIGRACAO_1_2.endVersion)
        assertEquals(1, AppDatabase.MIGRACAO_1_2.startVersion)
    }

    @Test
    fun `banco novo ja nasce na versao atual com contatos`() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        val banco = AppDatabase.emMemoria(contexto)
        try {
            val perfilId = kotlinx.coroutines.runBlocking {
                banco.perfilDao().inserir(
                    br.com.mochiladeemergencia.app.data.local.entity.PerfilEntity(nome = "Casa")
                )
            }
            kotlinx.coroutines.runBlocking {
                banco.contatoDao().inserir(
                    br.com.mochiladeemergencia.app.data.local.entity.ContatoEntity(
                        perfilId = perfilId,
                        nome = "SAMU da região",
                        telefone = "192"
                    )
                )
            }
            assertEquals(1, kotlinx.coroutines.runBlocking { banco.contatoDao().listarTodos() }.size)
        } finally {
            banco.close()
        }
    }

    @Test
    fun `migracao 2 para 3 adiciona dica e forma de consumo e renomeia categorias`() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        val arquivo = File(contexto.cacheDir, "migracao_2_3.db")
        arquivo.delete()
        val db = SQLiteDatabase.openOrCreateDatabase(arquivo, null)

        db.execSQL(
            "CREATE TABLE perfis (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "nome TEXT NOT NULL, localizacao TEXT NOT NULL DEFAULT '', criadoEm INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE itens (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "perfilId INTEGER NOT NULL, nome TEXT NOT NULL, categoria TEXT NOT NULL, " +
                "quantidade REAL NOT NULL, unidade TEXT NOT NULL, validadeEpochDay INTEGER, " +
                "local TEXT NOT NULL, observacao TEXT NOT NULL, marcadoComoFalta INTEGER NOT NULL, " +
                "aRepor INTEGER NOT NULL DEFAULT 0, fotoArquivo TEXT, chaveChecklist TEXT, " +
                "criadoEm INTEGER NOT NULL, atualizadoEm INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE checklist (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "perfilId INTEGER NOT NULL, chave TEXT NOT NULL, nome TEXT NOT NULL, " +
                "categoria TEXT NOT NULL, quantidadeBase REAL NOT NULL, unidade TEXT NOT NULL, " +
                "porPessoa INTEGER NOT NULL, ordem INTEGER NOT NULL, oculto INTEGER NOT NULL, " +
                "personalizado INTEGER NOT NULL)"
        )
        val perfilId = db.insert("perfis", null, ContentValues().apply {
            put("nome", "Casa")
            put("criadoEm", 1000L)
        })
        db.insert("itens", null, ContentValues().apply {
            put("perfilId", perfilId)
            put("nome", "Sardinha")
            put("categoria", "AGUA_E_ALIMENTOS")
            put("quantidade", 1.0)
            put("unidade", "lata")
            put("local", "")
            put("observacao", "")
            put("marcadoComoFalta", 0)
            put("criadoEm", 1000L)
            put("atualizadoEm", 1000L)
        })
        db.insert("checklist", null, ContentValues().apply {
            put("perfilId", perfilId)
            put("chave", "lanterna")
            put("nome", "Lanterna")
            put("categoria", "ILUMINACAO")
            put("quantidadeBase", 1.0)
            put("unidade", "un")
            put("porPessoa", 0)
            put("ordem", 0)
            put("oculto", 0)
            put("personalizado", 0)
        })

        // ---- SQL da migração 2 -> 3 (mesmo conteúdo de AppDatabase.MIGRACAO_2_3) ----
        db.execSQL("ALTER TABLE checklist ADD COLUMN dica TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE itens ADD COLUMN formaConsumo TEXT NOT NULL DEFAULT ''")
        listOf(
            "AGUA_E_ALIMENTOS" to "COMIDA_DE_EMERGENCIA",
            "ILUMINACAO" to "ILUMINACAO_E_COMUNICACAO",
            "COMUNICACAO" to "ILUMINACAO_E_COMUNICACAO",
            "ABRIGO" to "FERRAMENTAS_E_ABRIGO",
            "FERRAMENTAS" to "FERRAMENTAS_E_ABRIGO",
            "PRIMEIROS_SOCORROS" to "SAUDE_E_PRIMEIROS_SOCORROS",
            "DOCUMENTOS" to "DOCUMENTOS_E_VALORES"
        ).forEach { (antiga, nova) ->
            db.execSQL("UPDATE itens SET categoria = '$nova' WHERE categoria = '$antiga'")
            db.execSQL("UPDATE checklist SET categoria = '$nova' WHERE categoria = '$antiga'")
        }
        db.version = 3

        db.rawQuery("SELECT categoria, formaConsumo FROM itens", null).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("COMIDA_DE_EMERGENCIA", cursor.getString(0))
            assertEquals("", cursor.getString(1))
        }
        db.rawQuery("SELECT categoria, dica FROM checklist", null).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("ILUMINACAO_E_COMUNICACAO", cursor.getString(0))
            assertEquals("", cursor.getString(1))
        }
        db.close()
        arquivo.delete()

        assertEquals(3, AppDatabase.MIGRACAO_2_3.endVersion)
        assertEquals(2, AppDatabase.MIGRACAO_2_3.startVersion)
    }

    private fun gerarLista(cursor: android.database.Cursor, coluna: String): List<String> {
        val indice = cursor.getColumnIndex(coluna)
        val valores = mutableListOf<String>()
        while (cursor.moveToNext()) valores.add(cursor.getString(indice))
        return valores
    }
}
