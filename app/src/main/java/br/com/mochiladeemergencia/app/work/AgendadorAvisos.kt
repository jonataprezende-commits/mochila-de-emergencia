package br.com.mochiladeemergencia.app.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Agenda a verificação diária de validade às 9h (horário local). */
class AgendadorAvisos(private val context: Context) {

    /**
     * Agenda a verificação diária. Falhas de inicialização do WorkManager não podem
     * derrubar o app: os avisos continuam visíveis no painel.
     */
    fun agendarVerificacaoDiaria() {
        val agora = LocalDateTime.now()
        val horaAlvo = LocalTime.of(HORA_VERIFICACAO, 0)
        var proxima = LocalDateTime.of(agora.toLocalDate(), horaAlvo)
        if (!proxima.isAfter(agora)) proxima = proxima.plusDays(1)
        val atrasoMinutos = Duration.between(agora, proxima).toMinutes().coerceAtLeast(1)

        val pedido = PeriodicWorkRequestBuilder<ValidadeWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(atrasoMinutos, TimeUnit.MINUTES)
            .setConstraints(Constraints.NONE)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.MINUTES)
            .addTag(TAG)
            .build()

        runCatching {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                ValidadeWorker.NOME_TRABALHO,
                ExistingPeriodicWorkPolicy.UPDATE,
                pedido
            )
        }
    }

    /**
     * Agenda o lembrete semestral (182 dias). Usa `KEEP` para não reiniciar a contagem
     * a cada abertura do app; se o usuário desativou, o trabalho é cancelado.
     */
    fun configurarLembreteSemestral(ativo: Boolean) {
        runCatching {
            if (!ativo) {
                WorkManager.getInstance(context)
                    .cancelUniqueWork(RevisaoSemestralWorker.NOME_TRABALHO)
                return@runCatching
            }
            val pedido = PeriodicWorkRequestBuilder<RevisaoSemestralWorker>(
                RevisaoSemestralWorker.PERIODO_DIAS,
                TimeUnit.DAYS
            )
                .setConstraints(Constraints.NONE)
                .addTag(TAG_REVISAO)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                RevisaoSemestralWorker.NOME_TRABALHO,
                ExistingPeriodicWorkPolicy.KEEP,
                pedido
            )
        }
    }

    /** Permite ao usuário forçar a verificação (usado no botão "Verificar agora"). */
    fun verificarAgora() {
        val pedido = OneTimeWorkRequestBuilder<ValidadeWorker>()
            .setConstraints(Constraints.NONE)
            .addTag(TAG)
            .build()
        runCatching { WorkManager.getInstance(context).enqueue(pedido) }
    }

    companion object {
        private const val TAG = "avisos_validade"
        private const val TAG_REVISAO = "lembrete_revisao"
        private const val HORA_VERIFICACAO = 9
    }
}
