package br.com.mochiladeemergencia.app

import br.com.mochiladeemergencia.app.domain.StatusCalculadora
import br.com.mochiladeemergencia.app.domain.model.StatusItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Testes unitários das regras de validade (JVM pura, sem Android).
 * Cobrem os critérios de aceite: cálculo de status e marcos de aviso 30/7/1/0.
 */
class StatusCalculadoraTest {

    private val hoje = LocalDate.of(2026, 11, 10)

    private fun diaEpoch(data: LocalDate): Long = data.toEpochDay()

    @Test
    fun `item sem validade e marcado como tenho retorna TENHO`() {
        val status = StatusCalculadora.status(
            validadeEpochDay = null,
            marcadoComoFalta = false,
            antecedenciaDias = 60,
            hoje = hoje
        )
        assertEquals(StatusItem.TENHO, status)
    }

    @Test
    fun `item sem validade e marcado como falta retorna FALTA`() {
        val status = StatusCalculadora.status(
            validadeEpochDay = null,
            marcadoComoFalta = true,
            antecedenciaDias = 60,
            hoje = hoje
        )
        assertEquals(StatusItem.FALTA, status)
    }

    @Test
    fun `validade no passado retorna VENCIDO`() {
        val status = StatusCalculadora.status(
            validadeEpochDay = diaEpoch(hoje.minusDays(1)),
            marcadoComoFalta = false,
            antecedenciaDias = 60,
            hoje = hoje
        )
        assertEquals(StatusItem.VENCIDO, status)
    }

    @Test
    fun `validade hoje retorna VENCENDO (marco zero)`() {
        val status = StatusCalculadora.status(
            validadeEpochDay = diaEpoch(hoje),
            marcadoComoFalta = false,
            antecedenciaDias = 60,
            hoje = hoje
        )
        assertEquals(StatusItem.VENCENDO, status)
    }

    @Test
    fun `validade exatamente na antecedencia retorna VENCENDO`() {
        val status = StatusCalculadora.status(
            validadeEpochDay = diaEpoch(hoje.plusDays(60)),
            marcadoComoFalta = false,
            antecedenciaDias = 60,
            hoje = hoje
        )
        assertEquals(StatusItem.VENCENDO, status)
    }

    @Test
    fun `validade um dia depois da antecedencia retorna TENHO`() {
        val status = StatusCalculadora.status(
            validadeEpochDay = diaEpoch(hoje.plusDays(61)),
            marcadoComoFalta = false,
            antecedenciaDias = 60,
            hoje = hoje
        )
        assertEquals(StatusItem.TENHO, status)
    }

    @Test
    fun `antecedencia de 30 dias nao marca item a 45 dias como vencendo`() {
        val status = StatusCalculadora.status(
            validadeEpochDay = diaEpoch(hoje.plusDays(45)),
            marcadoComoFalta = false,
            antecedenciaDias = 30,
            hoje = hoje
        )
        assertEquals(StatusItem.TENHO, status)
    }

    @Test
    fun `antecedencia de 90 dias marca item a 80 dias como vencendo`() {
        val status = StatusCalculadora.status(
            validadeEpochDay = diaEpoch(hoje.plusDays(80)),
            marcadoComoFalta = false,
            antecedenciaDias = 90,
            hoje = hoje
        )
        assertEquals(StatusItem.VENCENDO, status)
    }

    @Test
    fun `item marcado como falta mas com validade proxima aparece como VENCENDO`() {
        // Precedência definida: validade tem prioridade sobre a marcação manual.
        val status = StatusCalculadora.status(
            validadeEpochDay = diaEpoch(hoje.plusDays(10)),
            marcadoComoFalta = true,
            antecedenciaDias = 60,
            hoje = hoje
        )
        assertEquals(StatusItem.VENCENDO, status)
    }

    @Test
    fun `dias ate vencer conta corretamente`() {
        assertEquals(0, StatusCalculadora.diasAteVencer(diaEpoch(hoje), hoje))
        assertEquals(7, StatusCalculadora.diasAteVencer(diaEpoch(hoje.plusDays(7)), hoje))
        assertEquals(-3, StatusCalculadora.diasAteVencer(diaEpoch(hoje.minusDays(3)), hoje))
        assertNull(StatusCalculadora.diasAteVencer(null, hoje))
    }

    @Test
    fun `marcos de aviso incluem antecedencia configurada e 30 7 1 0`() {
        assertEquals(setOf(60, 30, 7, 1, 0), StatusCalculadora.marcosDeAviso(60))
        assertEquals(setOf(90, 30, 7, 1, 0), StatusCalculadora.marcosDeAviso(90))
        assertEquals(setOf(30, 7, 1, 0), StatusCalculadora.marcosDeAviso(30))
    }

    @Test
    fun `precisaAviso respeita marcos e ignora vencidos e sem validade`() {
        assertTrue(StatusCalculadora.precisaAviso(60, 60))
        assertTrue(StatusCalculadora.precisaAviso(30, 60))
        assertTrue(StatusCalculadora.precisaAviso(7, 60))
        assertTrue(StatusCalculadora.precisaAviso(1, 60))
        assertTrue(StatusCalculadora.precisaAviso(0, 60))
        assertFalse(StatusCalculadora.precisaAviso(45, 60))
        assertFalse(StatusCalculadora.precisaAviso(-1, 60))
        assertFalse(StatusCalculadora.precisaAviso(null, 60))
    }

    @Test
    fun `descricao dos dias usa texto claro em portugues`() {
        assertEquals("Vence hoje", StatusCalculadora.descreverDias(0))
        assertEquals("Vence amanhã", StatusCalculadora.descreverDias(1))
        assertEquals("Vence em 30 dias", StatusCalculadora.descreverDias(30))
        assertEquals("Vencido há 1 dia", StatusCalculadora.descreverDias(-1))
        assertEquals("Vencido há 5 dias", StatusCalculadora.descreverDias(-5))
        assertEquals("Sem validade", StatusCalculadora.descreverDias(null))
    }
}
