package br.com.mochiladeemergencia.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Discagem segura: abre o aplicativo de telefone já com o número preenchido
 * (`ACTION_DIAL`). O app **não** faz chamadas — não existe `CALL_PHONE` no manifesto.
 */
object Telefone {

    /** Abre o discador com [numero]. Retorna false quando não há app de telefone. */
    fun discar(contexto: Context, numero: String): Boolean {
        val saneado = numero.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
        if (saneado.isBlank()) return false
        val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", saneado, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            contexto.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
