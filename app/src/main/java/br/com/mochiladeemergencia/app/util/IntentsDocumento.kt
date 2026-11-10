package br.com.mochiladeemergencia.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import android.provider.OpenableColumns
import br.com.mochiladeemergencia.app.data.local.entity.DocumentoEntity
import br.com.mochiladeemergencia.app.domain.model.TipoDocumento

/**
 * Abertura de documentos em apps externos.
 * Links vão para o navegador; arquivos locais usam o content:// escolhido pelo usuário.
 * O app não usa WebView e não acessa a rede por conta própria.
 */
object IntentsDocumento {

    fun abrir(contexto: Context, documento: DocumentoEntity): Boolean {
        val uri = runCatching { documento.uri.toUri() }.getOrNull() ?: return false
        val tipo = TipoDocumento.dePersistido(documento.tipo)

        val intent = if (documento.arquivoLocal || tipo != TipoDocumento.LINK) {
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime(tipo))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_VIEW, uri)
        }

        return try {
            contexto.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    fun mime(tipo: TipoDocumento): String = when (tipo) {
        TipoDocumento.PDF -> "application/pdf"
        TipoDocumento.IMAGEM -> "image/*"
        TipoDocumento.VIDEO -> "video/*"
        TipoDocumento.TEXTO -> "text/plain"
        TipoDocumento.LINK -> "text/uri-list"
    }

    /** Nome de exibição do arquivo escolhido (sem gravar nada). */
    fun nomeExibicao(contexto: Context, uri: Uri): String {
        return try {
            contexto.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val indice = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (indice >= 0 && cursor.moveToFirst()) cursor.getString(indice) else null
                } ?: "Arquivo selecionado"
        } catch (_: Exception) {
            "Arquivo selecionado"
        }
    }
}
