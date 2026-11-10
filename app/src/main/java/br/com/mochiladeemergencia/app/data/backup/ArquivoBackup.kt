package br.com.mochiladeemergencia.app.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import br.com.mochiladeemergencia.app.util.Formatos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Leitura e escrita de arquivos de backup escolhidos pelo usuário.
 * Nada é gravado fora do diretório privado do app ou do destino escolhido no seletor
 * do Android — nenhum caminho é construído a partir de texto do usuário/JSON.
 */
object ArquivoBackup {

    private const val NOME_MIME = "application/json"

    /** Lê até [limiteBytes] do URI escolhido. Lança erro se o arquivo for maior que o limite. */
    suspend fun lerTexto(context: Context, uri: Uri, limiteBytes: Int): String =
        withContext(Dispatchers.IO) {
            val entrada = context.contentResolver.openInputStream(uri)
                ?: throw BackupInvalidoException("Não foi possível abrir o arquivo escolhido.")
            entrada.use { fluxo ->
                val saida = ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val lidos = fluxo.read(buffer)
                    if (lidos <= 0) break
                    total += lidos
                    if (total > limiteBytes) {
                        throw BackupInvalidoException(
                            "O arquivo é maior que o limite de " +
                                "${limiteBytes / (1024 * 1024)} MB permitido para backups."
                        )
                    }
                    saida.write(buffer, 0, lidos)
                }
                saida.toByteArray().toString(Charsets.UTF_8)
            }
        }

    /** Grava o JSON no destino escolhido pelo usuário (ACTION_CREATE_DOCUMENT). */
    suspend fun salvarEmUri(context: Context, uri: Uri, json: String) = withContext(Dispatchers.IO) {
        val saida = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IllegalStateException("Não foi possível gravar no local escolhido.")
        saida.use { it.write(json.toByteArray(Charsets.UTF_8)) }
    }

    /**
     * Gera o arquivo dentro de cache/exports e devolve um Intent de compartilhamento
     * usando FileProvider (authority `${applicationId}.fileprovider`).
     */
    suspend fun criarIntentCompartilhar(context: Context, json: String): Intent =
        withContext(Dispatchers.IO) {
            val pasta = File(context.cacheDir, "exports").apply { mkdirs() }
            val arquivo = File(pasta, "mochila-backup-${Formatos.dataHoraArquivo(System.currentTimeMillis())}.json")
            arquivo.writeText(json, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                arquivo
            )
            Intent(Intent.ACTION_SEND).apply {
                type = NOME_MIME
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Backup — Mochila de Emergência")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

    /** Nome sugerido para o arquivo de backup (sem dados pessoais). */
    fun nomeArquivoSugerido(): String =
        "mochila-backup-${Formatos.dataHoraArquivo(System.currentTimeMillis())}.json"
}
