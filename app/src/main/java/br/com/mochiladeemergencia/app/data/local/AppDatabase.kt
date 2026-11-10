package br.com.mochiladeemergencia.app.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.mochiladeemergencia.app.data.local.dao.ChecklistDao
import br.com.mochiladeemergencia.app.data.local.dao.ContatoDao
import br.com.mochiladeemergencia.app.data.local.dao.DocumentoDao
import br.com.mochiladeemergencia.app.data.local.dao.ItemDao
import br.com.mochiladeemergencia.app.data.local.dao.PerfilDao
import br.com.mochiladeemergencia.app.data.local.entity.ChecklistEntity
import br.com.mochiladeemergencia.app.data.local.entity.ContatoEntity
import br.com.mochiladeemergencia.app.data.local.entity.DocumentoEntity
import br.com.mochiladeemergencia.app.data.local.entity.ItemEntity
import br.com.mochiladeemergencia.app.data.local.entity.PerfilEntity

/**
 * Banco local (Room). Nenhum dado sai do aparelho.
 *
 * Versão 2: localização da mochila no perfil, foto e marcação "a repor" nos itens
 * e nova tabela de contatos pessoais de emergência (migração não destrutiva).
 * Versão 3: categorias do checklist reorganizadas, dica nas sugestões e
 * "forma de consumo" opcional nos itens (migração não destrutiva).
 */
@Database(
    entities = [
        PerfilEntity::class,
        ItemEntity::class,
        DocumentoEntity::class,
        ChecklistEntity::class,
        ContatoEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun perfilDao(): PerfilDao
    abstract fun itemDao(): ItemDao
    abstract fun documentoDao(): DocumentoDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun contatoDao(): ContatoDao

    companion object {
        private const val NOME_ARQUIVO = "mochila.db"

        /** Migração 1 → 2: adiciona localização, foto, flag "a repor" e a tabela de contatos. */
        val MIGRACAO_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `perfis` ADD COLUMN `localizacao` TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL("ALTER TABLE `itens` ADD COLUMN `aRepor` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `itens` ADD COLUMN `fotoArquivo` TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `contatos` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`perfilId` INTEGER NOT NULL, " +
                        "`nome` TEXT NOT NULL, " +
                        "`telefone` TEXT NOT NULL, " +
                        "`parentesco` TEXT NOT NULL, " +
                        "`observacao` TEXT NOT NULL, " +
                        "`criadoEm` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`perfilId`) REFERENCES `perfis`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_contatos_perfilId` ON `contatos` (`perfilId`)")
            }
        }

        /**
         * Migração 2 → 3: acrescenta a dica do checklist e a forma de consumo,
         * e renomeia as categorias antigas para os grupos novos.
         */
        val MIGRACAO_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `checklist` ADD COLUMN `dica` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `itens` ADD COLUMN `formaConsumo` TEXT NOT NULL DEFAULT ''")
                // Categorias renomeadas: os itens continuam aparecendo no grupo certo.
                val mapa = listOf(
                    "AGUA_E_ALIMENTOS" to "COMIDA_DE_EMERGENCIA",
                    "ILUMINACAO" to "ILUMINACAO_E_COMUNICACAO",
                    "COMUNICACAO" to "ILUMINACAO_E_COMUNICACAO",
                    "ABRIGO" to "FERRAMENTAS_E_ABRIGO",
                    "FERRAMENTAS" to "FERRAMENTAS_E_ABRIGO",
                    "PRIMEIROS_SOCORROS" to "SAUDE_E_PRIMEIROS_SOCORROS",
                    "DOCUMENTOS" to "DOCUMENTOS_E_VALORES"
                )
                mapa.forEach { (antiga, nova) ->
                    db.execSQL("UPDATE `itens` SET `categoria` = '$nova' WHERE `categoria` = '$antiga'")
                    db.execSQL("UPDATE `checklist` SET `categoria` = '$nova' WHERE `categoria` = '$antiga'")
                }
            }
        }

        @Volatile
        private var instancia: AppDatabase? = null

        fun getInstance(context: android.content.Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    NOME_ARQUIVO
                )
                    .addMigrations(MIGRACAO_1_2, MIGRACAO_2_3)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build()
                    .also { instancia = it }
            }

        /** Usado apenas em testes para criar um banco em memória. */
        fun emMemoria(context: android.content.Context): AppDatabase =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }
}
