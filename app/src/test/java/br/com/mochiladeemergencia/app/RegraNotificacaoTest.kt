package br.com.mochiladeemergencia.app

import br.com.mochiladeemergencia.app.work.ItemParaAviso
import br.com.mochiladeemergencia.app.work.RegraNotificacao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Testes das regras de notificação: marcos, agrupamento e anti-duplicação diária. */
class RegraNotificacaoTest {

    private val hoje = LocalDate.of(2026, 11, 10)
    private fun emDias(dias: Long): Long = hoje.plusDays(dias).toEpochDay()

    private fun item(nome: String, dias: Long?, perfil: String = "Casa") =
        ItemParaAviso(perfilNome = perfil, nome = nome, validadeEpochDay = dias?.let { emDias(it) })

    @Test
    fun `gera aviso nos marcos 60 30 7 1 e 0`() {
        val itens = listOf(
            item("Água", 60),
            item("Sardinha", 30),
            item("Lanterna", 7),
            item("Pilhas", 1),
            item("Corda", 0)
        )
        val avisos = RegraNotificacao.avisos(itens, antecedenciaDias = 60, hoje = hoje)
        assertEquals(5, avisos.size)
        assertEquals(listOf(0, 1, 7, 30, 60), avisos.map { it.diasRestantes })
    }

    @Test
    fun `nao gera aviso fora dos marcos`() {
        val itens = listOf(item("Água", 45), item("Sabonete", 90 - 1), item("Lona", 3))
        assertTrue(RegraNotificacao.avisos(itens, 60, hoje).isEmpty())
    }

    @Test
    fun `item sem validade nunca gera aviso`() {
        val itens = listOf(item("Sem validade", null))
        assertTrue(RegraNotificacao.avisos(itens, 60, hoje).isEmpty())
    }

    @Test
    fun `item vencido nao gera aviso proprio mas entra na contagem de vencidos`() {
        val itens = listOf(item("Vencido", -2), item("Perto de vencer", 7))
        val avisos = RegraNotificacao.avisos(itens, 60, hoje)
        assertEquals(1, avisos.size)
        assertEquals(1, RegraNotificacao.contarVencidos(itens, hoje))
    }

    @Test
    fun `mensagem resume os itens e os vencidos`() {
        val avisos = RegraNotificacao.avisos(
            listOf(item("Água", 7), item("Pilhas", 0)),
            antecedenciaDias = 60,
            hoje = hoje
        )
        val texto = RegraNotificacao.montarMensagem(avisos, vencidos = 1)
        assertTrue(texto.contains("Pilhas"))
        assertTrue(texto.contains("Água"))
        assertTrue(texto.contains("1 item já venceu"))
    }

    @Test
    fun `titulo indica a quantidade de itens`() {
        assertEquals("1 item perto de vencer", RegraNotificacao.montarTitulo(1))
        assertEquals("3 itens perto de vencer", RegraNotificacao.montarTitulo(3))
    }

    @Test
    fun `nao notifica quando ja avisou no mesmo dia`() {
        val avisos = RegraNotificacao.avisos(listOf(item("Água", 7)), 60, hoje)
        val diaAtual = hoje.toEpochDay()
        assertFalse(RegraNotificacao.deveNotificar(avisos, diaAtual, diaAtual))
    }

    @Test
    fun `notifica quando ainda nao avisou hoje`() {
        val avisos = RegraNotificacao.avisos(listOf(item("Água", 7)), 60, hoje)
        val diaAtual = hoje.toEpochDay()
        assertFalse(RegraNotificacao.deveNotificar(avisos, diaAtual, diaAtual - 1).not())
        assertTrue(RegraNotificacao.deveNotificar(avisos, diaAtual, diaAtual - 1))
    }

    @Test
    fun `nao notifica sem avisos`() {
        assertFalse(RegraNotificacao.deveNotificar(emptyList(), hoje.toEpochDay(), -1))
    }

    @Test
    fun `hash muda quando o conteudo muda`() {
        val avisosA = RegraNotificacao.avisos(listOf(item("Água", 7)), 60, hoje)
        val avisosB = RegraNotificacao.avisos(listOf(item("Água", 7), item("Pilhas", 1)), 60, hoje)
        assertTrue(
            RegraNotificacao.hashConteudo(avisosA, 0) != RegraNotificacao.hashConteudo(avisosB, 0)
        )
    }

    @Test
    fun `mensagem traz os nomes dos perfis quando ha mais de um`() {
        val avisos = RegraNotificacao.avisos(
            listOf(
                item("Água", 7, perfil = "Casa"),
                item("Corda", 1, perfil = "Sítio")
            ),
            antecedenciaDias = 60,
            hoje = hoje
        )
        assertEquals(2, avisos.size)
        assertEquals(setOf("Casa", "Sítio"), avisos.map { it.perfilNome }.toSet())
    }
}
