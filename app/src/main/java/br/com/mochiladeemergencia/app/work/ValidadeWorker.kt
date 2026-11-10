package br.com.mochiladeemergencia.app.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.mochiladeemergencia.app.MochilaApp
import br.com.mochiladeemergencia.app.util.Notificacoes
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Verificação diária de validade (WorkManager).
 *
 * Fluxo:
 * 1. respeita a preferência de notificações e a permissão do Android 13+;
 * 2. calcula os avisos (30/7/1/0 dias + antecedência configurada);
 * 3. não repete aviso no mesmo dia;
 * 4. publica uma única notificação resumida.
 * Se a permissão foi negada, o app continua funcionando e os avisos aparecem no painel.
 */
class ValidadeWorker(
    contexto: Context,
    parametros: WorkerParameters
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? MochilaApp ?: return Result.success()
        val servicos = app.servicos

        return try {
            val config = servicos.preferencias.configuracoes.first()
            if (!config.notificacoesAtivas) return Result.success()
            if (!Notificacoes.permissaoConcedida(applicationContext)) return Result.success()

            val hoje = LocalDate.now()
            val perfis = servicos.banco.perfilDao().listarTodos()
            val nomePorPerfil = perfis.associate { it.id to it.nome }
            val itens = servicos.banco.itemDao().listarTodos()
                .filter { it.validadeEpochDay != null }
                .map {
                    ItemParaAviso(
                        perfilNome = nomePorPerfil[it.perfilId] ?: "",
                        nome = it.nome,
                        validadeEpochDay = it.validadeEpochDay
                    )
                }

            val avisos = RegraNotificacao.avisos(itens, config.antecedenciaDias, hoje)
            val vencidos = RegraNotificacao.contarVencidos(itens, hoje)
            val diaAtual = hoje.toEpochDay()

            if (!RegraNotificacao.deveNotificar(avisos, diaAtual, config.ultimoAvisoDia)) {
                return Result.success()
            }

            Notificacoes.mostrarResumo(
                context = applicationContext,
                titulo = RegraNotificacao.montarTitulo(avisos.size),
                mensagem = RegraNotificacao.montarMensagem(avisos, vencidos)
            )
            servicos.preferencias.registrarAviso(
                diaEpoch = diaAtual,
                hashConteudo = RegraNotificacao.hashConteudo(avisos, vencidos)
            )
            Result.success()
        } catch (erro: Exception) {
            // Não registra dados do usuário em log — apenas a categoria do erro.
            Log.w(TAG, "Falha ao verificar validades: ${erro.javaClass.simpleName}")
            Result.success()
        }
    }

    companion object {
        private const val TAG = "ValidadeWorker"
        const val NOME_TRABALHO = "verificacao_diaria_validade"
    }
}
