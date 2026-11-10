package br.com.mochiladeemergencia.app.util

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Utilitários de data e número com localização pt-BR. */
object Formatos {

    val LOCALE_BR: Locale = Locale.forLanguageTag("pt-BR")

    private val FORMATO_DATA: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy", LOCALE_BR)

    private val FORMATO_DATA_HORA: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", LOCALE_BR)

    private val FORMATO_ARQUIVO: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", LOCALE_BR)

    /** Regex de acentos compilada uma única vez (era recriada a cada item, a cada tecla). */
    private val ACENTOS: Regex = Regex("\\p{InCombiningDiacriticalMarks}+")

    /**
     * NumberFormat **não é thread-safe**, então cada thread tem o seu.
     * Antes era criado um novo a cada chamada — caro para listas longas.
     */
    private val FORMATADOR_NUMERO: ThreadLocal<NumberFormat> =
        object : ThreadLocal<NumberFormat>() {
            override fun initialValue(): NumberFormat =
                NumberFormat.getNumberInstance(LOCALE_BR).apply {
                    maximumFractionDigits = 2
                    minimumFractionDigits = 0
                }
        }

    fun hoje(): LocalDate = LocalDate.now()

    fun paraEpochDay(data: LocalDate): Long = data.toEpochDay()

    fun deEpochDay(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    /** dd/MM/yyyy a partir de epoch day. Retorna texto vazio quando não há data. */
    fun data(epochDay: Long?): String =
        epochDay?.let { FORMATO_DATA.format(LocalDate.ofEpochDay(it)) } ?: ""

    fun dataEHora(epochMillis: Long): String =
        FORMATO_DATA_HORA.format(
            Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
        )

    fun dataHoraArquivo(epochMillis: Long): String =
        FORMATO_ARQUIVO.format(
            Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
        )

    /** Quantidade sem casas decimais quando inteira (ex.: "2" e não "2,0"). */
    fun quantidade(valor: Double): String = FORMATADOR_NUMERO.get().format(valor)

    fun quantidadeComUnidade(valor: Double, unidade: String): String =
        "${quantidade(valor)} ${unidade.trim()}".trim()

    /**
     * O DatePicker do Compose devolve milissegundos em UTC.
     * A conversão para LocalDate precisa ser feita em UTC para não "pular" um dia.
     */
    fun utcMillisParaEpochDay(millis: Long): Long =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()

    fun epochDayParaUtcMillis(epochDay: Long): Long =
        LocalDate.ofEpochDay(epochDay).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    /** Normaliza um texto de busca (minúsculas, sem acentos) para comparações tolerantes. */
    fun normalizarBusca(texto: String): String {
        val semAcento = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
            .replace(ACENTOS, "")
        return semAcento.lowercase(LOCALE_BR).trim()
    }
}
