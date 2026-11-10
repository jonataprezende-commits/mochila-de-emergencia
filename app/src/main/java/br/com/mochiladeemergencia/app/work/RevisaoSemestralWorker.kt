package br.com.mochiladeemergencia.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.mochiladeemergencia.app.MochilaApp
import br.com.mochiladeemergencia.app.domain.PrimeirosSocorros
import br.com.mochiladeemergencia.app.util.Notificacoes
import kotlinx.coroutines.flow.first

/**
 * Lembrete semestral de revisão da mochila (WorkManager, a cada 6 meses).
 *
 * Itens sem data de validade também se degradam (pilha enferruja, repelente aberto perde
 * validade, isqueiro vaza, lona resseca), por isso o lembrete não depende de validade.
 * Respeita a preferência "lembrete semestral" e a permissão de notificações.
 */
class RevisaoSemestralWorker(
    contexto: Context,
    parametros: WorkerParameters
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? MochilaApp ?: return Result.success()
        return try {
            val config = app.servicos.preferencias.configuracoes.first()
            if (!config.lembreteSemestralAtivo) return Result.success()
            if (!Notificacoes.permissaoConcedida(applicationContext)) return Result.success()

            Notificacoes.mostrarRevisao(
                context = applicationContext,
                titulo = "Revisão semestral da mochila",
                mensagem = PrimeirosSocorros.LEMBRETE_REVISAO
            )
            Result.success()
        } catch (_: Exception) {
            // Nunca registra dados do usuário; falha silenciosa não quebra o app.
            Result.success()
        }
    }

    companion object {
        const val NOME_TRABALHO = "lembrete_semestral_revisao"
        const val PERIODO_DIAS = 182L
    }
}
