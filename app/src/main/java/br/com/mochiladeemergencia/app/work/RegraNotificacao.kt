package br.com.mochiladeemergencia.app.work

import br.com.mochiladeemergencia.app.domain.StatusCalculadora
import java.time.LocalDate

/** Item mínimo necessário para decidir se há aviso (desacoplado do banco para testes). */
data class ItemParaAviso(
    val perfilNome: String,
    val nome: String,
    val validadeEpochDay: Long?
)

data class AvisoValidade(
    val perfilNome: String,
    val itemNome: String,
    val diasRestantes: Int
)

/**
 * Regras puras de notificação, testáveis em JVM:
 * - avisa nos marcos 30, 7, 1 e 0 dias e na antecedência configurada (30/60/90);
 * - itens sem validade nunca geram aviso;
 * - itens já vencidos não geram aviso próprio (aparecem como "Vencido" no app);
 * - nunca repete a mesma notificação no mesmo dia.
 */
object RegraNotificacao {

    fun avisos(
        itens: List<ItemParaAviso>,
        antecedenciaDias: Int,
        hoje: LocalDate = LocalDate.now()
    ): List<AvisoValidade> = itens.mapNotNull { item ->
        val dias = StatusCalculadora.diasAteVencer(item.validadeEpochDay, hoje) ?: return@mapNotNull null
        if (!StatusCalculadora.precisaAviso(dias, antecedenciaDias)) return@mapNotNull null
        AvisoValidade(perfilNome = item.perfilNome, itemNome = item.nome, diasRestantes = dias)
    }.sortedBy { it.diasRestantes }

    fun contarVencidos(itens: List<ItemParaAviso>, hoje: LocalDate = LocalDate.now()): Int =
        itens.count { item ->
            val dias = StatusCalculadora.diasAteVencer(item.validadeEpochDay, hoje)
            dias != null && dias < 0
        }

    /** Mensagem resumida e agrupada (sem dados sensíveis além dos nomes dos itens). */
    fun montarMensagem(avisos: List<AvisoValidade>, vencidos: Int): String {
        val partes = avisos.take(4).joinToString("; ") { aviso ->
            "${aviso.itemNome} — ${StatusCalculadora.descreverDias(aviso.diasRestantes)}"
        }
        val restantes = avisos.size - 4
        val textoItens = when {
            avisos.isEmpty() -> ""
            restantes > 0 -> "$partes; e mais $restantes item(ns)"
            else -> partes
        }
        val textoVencidos = when {
            vencidos == 0 -> ""
            vencidos == 1 -> "1 item já venceu e precisa ser trocado."
            else -> "$vencidos itens já venceram e precisam ser trocados."
        }
        return listOf(textoItens, textoVencidos)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "Confira a mochila de emergência." }
    }

    fun montarTitulo(quantidadeAvisos: Int): String = when {
        quantidadeAvisos == 1 -> "1 item perto de vencer"
        quantidadeAvisos > 1 -> "$quantidadeAvisos itens perto de vencer"
        else -> "Itens vencidos na mochila"
    }

    /** Impressão digital do conteúdo do aviso (para não repetir no mesmo dia). */
    fun hashConteudo(avisos: List<AvisoValidade>, vencidos: Int): Int {
        val base = avisos.joinToString("|") { "${it.perfilNome}:${it.itemNome}:${it.diasRestantes}" } +
            "#vencidos=$vencidos"
        return base.hashCode()
    }

    /**
     * Decide se a notificação deve ser exibida agora.
     * Regra: no máximo uma notificação de validade por dia (sempre que houver avisos).
     */
    fun deveNotificar(
        avisos: List<AvisoValidade>,
        diaAtualEpochDay: Long,
        ultimoDiaNotificado: Long
    ): Boolean {
        if (avisos.isEmpty()) return false
        return ultimoDiaNotificado != diaAtualEpochDay
    }
}
