package br.com.mochiladeemergencia.app.di

import android.content.Context
import br.com.mochiladeemergencia.app.data.backup.BackupRepository
import br.com.mochiladeemergencia.app.data.local.AppDatabase
import br.com.mochiladeemergencia.app.data.prefs.PreferenciasRepository
import br.com.mochiladeemergencia.app.data.repository.ChecklistRepository
import br.com.mochiladeemergencia.app.data.repository.ContatoRepository
import br.com.mochiladeemergencia.app.data.repository.DocumentoRepository
import br.com.mochiladeemergencia.app.data.repository.ItemRepository
import br.com.mochiladeemergencia.app.data.repository.PerfilRepository
import br.com.mochiladeemergencia.app.work.AgendadorAvisos

/**
 * Injeção de dependências manual (sem bibliotecas de DI pagas ou pesadas).
 * Mantém uma única instância do banco e dos repositórios por processo.
 */
class ServicosLocais(context: Context) {

    val contexto: Context = context.applicationContext
    private val appContext: Context = contexto

    val banco: AppDatabase = AppDatabase.getInstance(appContext)
    val preferencias: PreferenciasRepository = PreferenciasRepository(appContext)
    val itemRepository: ItemRepository = ItemRepository(banco.itemDao())
    val documentoRepository: DocumentoRepository = DocumentoRepository(banco.documentoDao())
    val checklistRepository: ChecklistRepository =
        ChecklistRepository(banco.checklistDao(), banco.itemDao())
    val perfilRepository: PerfilRepository =
        PerfilRepository(banco.perfilDao(), banco.checklistDao(), checklistRepository)
    val contatoRepository: ContatoRepository = ContatoRepository(banco.contatoDao())
    val backupRepository: BackupRepository = BackupRepository(banco, preferencias, appContext)
    val agendadorAvisos: AgendadorAvisos = AgendadorAvisos(appContext)
}
