package br.com.mochiladeemergencia.app

import android.app.Application
import br.com.mochiladeemergencia.app.di.ServicosLocais
import br.com.mochiladeemergencia.app.util.Notificacoes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MochilaApp : Application() {

    lateinit var servicos: ServicosLocais
        private set

    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Nada de trabalho pesado aqui: a Application roda na thread principal e segura
        // a primeira tela. Criar banco, canal de notificação e agendar o WorkManager
        // (que inicializa a biblioteca inteira) vai para background.
        servicos = ServicosLocais(this)

        escopo.launch {
            runCatching { Notificacoes.criarCanal(this@MochilaApp) }
            runCatching { servicos.agendadorAvisos.agendarVerificacaoDiaria() }

            // Garante o perfil "Casa" e o checklist padrão no primeiro uso.
            runCatching {
                val perfilId = servicos.perfilRepository.garantirPerfilPadrao()
                // Corrige dicas padrão cujo texto mudou entre versões: quem atualiza o app
                // não pode continuar vendo o texto antigo gravado no banco.
                servicos.checklistRepository.corrigirTextosAntigos(perfilId)
                val config = servicos.preferencias.configuracoes.first()
                servicos.agendadorAvisos.configurarLembreteSemestral(config.lembreteSemestralAtivo)
            }
        }
    }
}
