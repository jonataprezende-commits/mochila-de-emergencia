package br.com.mochiladeemergencia.app.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.mochiladeemergencia.app.MainActivity
import br.com.mochiladeemergencia.app.R

/** Canal e exibição das notificações locais de validade. */
object Notificacoes {

    const val CANAL_VALIDADE = "canal_validade"
    const val CANAL_REVISAO = "canal_revisao"
    const val ID_RESUMO = 2001
    const val ID_REVISAO = 2002

    fun criarCanal(context: Context) {
        // minSdk 26: NotificationChannel está sempre disponível.
        val canal = NotificationChannel(
            CANAL_VALIDADE,
            context.getString(R.string.canal_validade_nome),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.canal_validade_descricao)
            setShowBadge(true)
        }
        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(canal)

        // Canal separado do lembrete semestral, para o usuário poder silenciar um sem o outro.
        val canalRevisao = NotificationChannel(
            CANAL_REVISAO,
            context.getString(R.string.canal_revisao_nome),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.canal_revisao_descricao)
            setShowBadge(true)
        }
        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(canalRevisao)
    }

    /** Lembrete semestral: usa o canal "Revisão da mochila". */
    @SuppressLint("MissingPermission")
    fun mostrarRevisao(context: Context, titulo: String, mensagem: String) {
        if (!permissaoConcedida(context) || !notificacoesHabilitadasNoSistema(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendente = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacao = NotificationCompat.Builder(context, CANAL_REVISAO)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo)
            .setContentText(mensagem)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensagem))
            .setContentIntent(pendente)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setOnlyAlertOnce(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(ID_REVISAO, notificacao)
        } catch (_: SecurityException) {
            // Permissão revogada entre a checagem e o envio.
        }
    }

    /** No Android 13+ o app precisa da permissão POST_NOTIFICATIONS. */
    fun permissaoConcedida(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun notificacoesHabilitadasNoSistema(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Exibe uma única notificação resumida. Silencioso se a permissão não existir. */
    @SuppressLint("MissingPermission")
    fun mostrarResumo(context: Context, titulo: String, mensagem: String) {
        if (!permissaoConcedida(context) || !notificacoesHabilitadasNoSistema(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendente = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacao = NotificationCompat.Builder(context, CANAL_VALIDADE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo)
            .setContentText(mensagem)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensagem))
            .setContentIntent(pendente)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setOnlyAlertOnce(true)
            .setSubText(context.getString(R.string.notificacao_ressalva))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(ID_RESUMO, notificacao)
        } catch (_: SecurityException) {
            // Permissão revogada entre a checagem e o envio: o app segue funcionando.
        }
    }
}
