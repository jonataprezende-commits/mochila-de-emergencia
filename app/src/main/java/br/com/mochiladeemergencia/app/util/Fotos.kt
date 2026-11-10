// Usamos o ExifInterface da plataforma de propósito: ele existe desde a API 24 (minSdk 26) e
// evita uma dependência extra só para ler a orientação de JPEGs gerados pelo próprio app.
@file:Suppress("ExifInterface")

package br.com.mochiladeemergencia.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import androidx.core.graphics.scale
import androidx.core.net.toUri
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/**
 * Fotos dos itens, guardadas no diretório privado do app.
 *
 * Decisões de segurança:
 * - o arquivo original escolhido pelo usuário é **copiado e recomprimido** (máx. 1024 px, JPEG 80),
 *   em vez de guardar a URI de terceiros;
 * - o nome do arquivo é sempre gerado pelo app (UUID) — nunca vem do usuário nem do JSON;
 * - ao ler uma foto de backup, o nome é validado (sem separadores, sem ".."): sem path traversal;
 * - nenhuma permissão ampla de mídia é solicitada (Photo Picker / câmera via intent).
 */
object Fotos {

    private const val LADO_MAXIMO = 1024
    private const val QUALIDADE_JPEG = 80
    private const val LIMITE_BYTES_ARQUIVO = 400 * 1024
    private val NOME_VALIDO = Regex("^[A-Za-z0-9._-]{1,80}\\.jpg$")

    /** Cache das miniaturas já decodificadas (a lista não decodifica o mesmo arquivo duas vezes). */
    @Volatile
    private var cache: android.util.LruCache<String, Bitmap>? = null

    private fun cache(): android.util.LruCache<String, Bitmap> =
        cache ?: synchronized(this) {
            cache ?: object : android.util.LruCache<String, Bitmap>(8 * 1024 * 1024) {
                // A API de LruCache mede o tamanho em "unidades"; aqui a unidade é byte de bitmap.
                override fun sizeOf(chave: String, valor: Bitmap): Int = valor.byteCount
            }.also { cache = it }
        }

    fun pasta(context: Context): File = File(context.filesDir, "fotos").apply { mkdirs() }

    fun pastaTemporaria(context: Context): File =
        File(context.cacheDir, "fotos_temp").apply { mkdirs() }

    fun nomeValido(nome: String?): Boolean =
        nome != null && NOME_VALIDO.matches(nome) && !nome.contains("..")

    fun arquivo(context: Context, nome: String?): File? {
        if (!nomeValido(nome)) return null
        val arquivo = File(pasta(context), nome!!)
        return if (arquivo.exists()) arquivo else null
    }

    /**
     * Copia a imagem do URI escolhido para o armazenamento interno (recomprimida).
     * @return nome do arquivo salvo, ou null em caso de falha.
     */
    fun salvarDeUri(context: Context, uri: Uri): String? = try {
        val bitmap = contextoParaBitmap(context, uri) ?: return null
        gravarBitmap(context, bitmap)
    } catch (_: Exception) {
        null
    }

    /** Converte um arquivo temporário (ex.: foto da câmera) em foto interna. */
    fun salvarDeArquivo(context: Context, origem: File): String? = try {
        val bitmap = decodificar(origem.readBytes()) ?: return null
        gravarBitmap(context, bitmap)
    } catch (_: Exception) {
        null
    }

    private fun gravarBitmap(context: Context, bitmap: Bitmap): String {
        val nome = "foto_${UUID.randomUUID()}.jpg"
        val destino = File(pasta(context), nome)
        var saida = comprimir(bitmap, QUALIDADE_JPEG)
        // Se ainda ficou grande, reduz a qualidade; se mesmo assim passar do limite, reduz a escala.
        if (saida.size > LIMITE_BYTES_ARQUIVO) saida = comprimir(bitmap, 60)
        if (saida.size > LIMITE_BYTES_ARQUIVO) {
            val menor = bitmap.scale(
                (bitmap.width * 0.7f).toInt().coerceAtLeast(64),
                (bitmap.height * 0.7f).toInt().coerceAtLeast(64)
            )
            saida = comprimir(menor, 60)
        }
        destino.writeBytes(saida)
        reciclar(bitmap)
        return nome
    }

    fun excluir(context: Context, nome: String?) {
        val alvo = arquivo(context, nome) ?: return
        alvo.delete()
        val prefixo = alvo.absolutePath + "#"
        val atual = cache()
        // Remove do cache qualquer tamanho de miniatura desse arquivo.
        atual.snapshot().forEach { (chave, _) -> if (chave.startsWith(prefixo)) atual.remove(chave) }
    }

    /**
     * Miniatura para a lista, com cache em memória.
     *
     * Chamada sempre fora da thread principal (ver `MiniaturaFoto`): decodificar JPEG é
     * caro e travava a rolagem quando acontecia durante a composição.
     */
    fun miniatura(context: Context, nome: String?, lado: Int = 160): Bitmap? {
        val arquivo = arquivo(context, nome) ?: return null
        val chave = "${arquivo.absolutePath}#$lado"
        cache().get(chave)?.let { if (!it.isRecycled) return it }
        return try {
            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(arquivo.absolutePath, limites)
            if (limites.outWidth <= 0) return null
            var amostra = 1
            while (limites.outWidth / (amostra * 2) >= lado && limites.outHeight / (amostra * 2) >= lado) {
                amostra *= 2
            }
            BitmapFactory.decodeFile(
                arquivo.absolutePath,
                BitmapFactory.Options().apply {
                    inSampleSize = amostra
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
            )?.also { bitmap -> cache().put(chave, bitmap) }
        } catch (_: Exception) {
            null
        }
    }

    // ----------------------------- backup (Base64) -----------------------------

    /** Converte a foto em Base64 para o backup JSON. Null se não existir. */
    fun paraBase64(context: Context, nome: String?): String? {
        val arquivo = arquivo(context, nome) ?: return null
        return try {
            Base64.encodeToString(arquivo.readBytes(), Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Recria a foto a partir do Base64 do backup.
     * Sempre gera um novo nome local (o nome do JSON é ignorado de propósito).
     */
    fun deBase64(context: Context, base64: String): String? {
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            if (bytes.isEmpty() || bytes.size > LIMITE_BYTES_ARQUIVO * 2) {
                null
            } else {
                decodificar(bytes)?.let { bitmap -> gravarBitmap(context, bitmap) }
            }
        } catch (_: Exception) {
            null
        }
    }

    // --------------------------------- interno ---------------------------------

    private fun contextoParaBitmap(context: Context, uri: Uri): Bitmap? {
        val entrada = context.contentResolver.openInputStream(uri) ?: return null
        return entrada.use { fluxo -> decodificar(fluxo.readBytes()) }
    }

    private fun decodificar(bytes: ByteArray): Bitmap? {
        if (bytes.isEmpty()) return null
        // Primeiro mede sem alocar o bitmap inteiro.
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, limites)
        if (limites.outWidth <= 0 || limites.outHeight <= 0) return null
        var amostra = 1
        while (limites.outWidth / (amostra * 2) >= LADO_MAXIMO &&
            limites.outHeight / (amostra * 2) >= LADO_MAXIMO
        ) {
            amostra *= 2
        }
        val bitmap = BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply { inSampleSize = amostra }
        ) ?: return null
        // Aplica a rotação do EXIF: sem isso, fotos tiradas em pé ficam deitadas na lista.
        return reduzir(aplicarOrientacao(bitmap, orientacao(bytes)), LADO_MAXIMO)
    }

    /** Orientação EXIF do arquivo; [ExifInterface.ORIENTATION_NORMAL] se não houver/não der para ler. */
    private fun orientacao(bytes: ByteArray): Int = try {
        ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    } catch (_: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }

    private fun aplicarOrientacao(bitmap: Bitmap, orientacao: Int): Bitmap {
        val matriz = Matrix()
        when (orientacao) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matriz.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matriz.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matriz.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matriz.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matriz.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matriz.postRotate(90f)
                matriz.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matriz.postRotate(270f)
                matriz.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return try {
            val girado = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
            if (girado != bitmap) reciclar(bitmap)
            girado
        } catch (_: Exception) {
            bitmap
        }
    }

    private fun reduzir(bitmap: Bitmap, ladoMaximo: Int): Bitmap {
        val maiorLado = maxOf(bitmap.width, bitmap.height)
        if (maiorLado <= ladoMaximo) return bitmap
        val fator = ladoMaximo.toFloat() / maiorLado
        val reduzido = bitmap.scale(
            (bitmap.width * fator).toInt().coerceAtLeast(1),
            (bitmap.height * fator).toInt().coerceAtLeast(1)
        )
        if (reduzido != bitmap) reciclar(bitmap)
        return reduzido
    }

    private fun comprimir(bitmap: Bitmap, qualidade: Int): ByteArray {
        val saida = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, qualidade, saida)
        return saida.toByteArray()
    }

    private fun reciclar(bitmap: Bitmap) {
        if (!bitmap.isRecycled) bitmap.recycle()
    }

    /** URI do FileProvider para compartilhar/exibir a foto (não exposta a outros apps). */
    fun uriArquivo(raw: String): Uri = raw.toUri()
}
