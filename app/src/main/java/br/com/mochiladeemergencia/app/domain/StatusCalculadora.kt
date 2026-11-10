package br.com.mochiladeemergencia.app.domain

import br.com.mochiladeemergencia.app.domain.model.StatusItem
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Regras de negócio de validade — mantidas puras (sem Android) para serem
 * testadas em testes unitários de JVM.
 *
 * Regra de precedência do status:
 * 1. REPOR     -> o item foi consumido/substituído e precisa ser reposto;
 * 2. VENCIDO   -> a data de validade já passou;
 * 3. VENCENDO  -> faltam entre 0 e [antecedenciaDias] dias (inclusive);
 * 4. FALTA     -> o usuário marcou o item como faltando;
 * 5. TENHO     -> caso contrário (ou quando o item não tem validade).
 *
 * Itens sem validade nunca geram alerta de vencimento.
 */
object StatusCalculadora {

    const val ANTECEDENCIA_PADRAO = 60
    val OPCOES_ANTECEDENCIA: List<Int> = listOf(30, 60, 90)

    /** Dias que geram aviso: a antecedência configurada mais marcos fixos (30, 7, 1 e 0). */
    val MARCOS_FIXOS: Set<Int> = setOf(30, 7, 1, 0)

    /** Converte a data (epoch day) em dias restantes até vencer. Null quando não há validade. */
    fun diasAteVencer(validadeEpochDay: Long?, hoje: LocalDate = LocalDate.now()): Int? =
        validadeEpochDay?.let { ChronoUnit.DAYS.between(hoje, LocalDate.ofEpochDay(it)).toInt() }

    /** Calcula o status exibido do item. */
    fun status(
        validadeEpochDay: Long?,
        marcadoComoFalta: Boolean,
        antecedenciaDias: Int,
        hoje: LocalDate = LocalDate.now(),
        aRepor: Boolean = false
    ): StatusItem {
        val dias = diasAteVencer(validadeEpochDay, hoje)
        return when {
            // Item consumido sai da mochila até ser reposto.
            aRepor -> StatusItem.REPOR
            dias == null -> if (marcadoComoFalta) StatusItem.FALTA else StatusItem.TENHO
            dias < 0 -> StatusItem.VENCIDO
            dias <= antecedenciaDias.coerceAtLeast(0) -> StatusItem.VENCENDO
            marcadoComoFalta -> StatusItem.FALTA
            else -> StatusItem.TENHO
        }
    }

    /**
     * Indica se o item deve gerar aviso hoje.
     * Apenas itens com validade, ainda não vencidos, cujo número de dias restantes
     * esteja exatamente em um dos marcos de aviso (antecedência configurada + 30/7/1/0).
     */
    fun precisaAviso(diasRestantes: Int?, antecedenciaDias: Int): Boolean {
        if (diasRestantes == null) return false
        if (diasRestantes < 0) return false
        return diasRestantes in marcosDeAviso(antecedenciaDias)
    }

    /** Conjunto de marcos de aviso para a antecedência escolhida. */
    fun marcosDeAviso(antecedenciaDias: Int): Set<Int> =
        MARCOS_FIXOS + antecedenciaDias.coerceIn(0, 365)

    /** Texto amigável para a quantidade de dias restantes. */
    fun descreverDias(diasRestantes: Int?): String = when {
        diasRestantes == null -> "Sem validade"
        diasRestantes < 0 -> {
            val vencidos = -diasRestantes
            if (vencidos == 1) "Vencido há 1 dia" else "Vencido há $vencidos dias"
        }
        diasRestantes == 0 -> "Vence hoje"
        diasRestantes == 1 -> "Vence amanhã"
        else -> "Vence em $diasRestantes dias"
    }
}
