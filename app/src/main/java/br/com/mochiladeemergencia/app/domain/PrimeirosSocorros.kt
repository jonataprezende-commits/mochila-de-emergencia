package br.com.mochiladeemergencia.app.domain

/**
 * Conteúdo informativo de primeiros socorros, exibido offline.
 *
 * O texto segue o material fornecido pelo responsável pelo app e fica restrito a
 * orientações gerais de primeiros socorros — **não** há instruções de combate a
 * incêndio nem procedimentos invasivos. O aviso de que o conteúdo não substitui
 * treinamento prático nem atendimento médico aparece fixo no topo da tela e em
 * cada card (ver `PrimeirosSocorrosScreen`).
 */
data class CardPrimeirosSocorros(
    val titulo: String,
    val sinal: String?,
    val passos: List<String>,
    val naoFaca: String,
    val numeroDestaque: String
)

object PrimeirosSocorros {

    const val AVISO: String =
        "Estas orientações são informativas e não substituem treinamento prático nem " +
            "atendimento médico profissional. Em qualquer emergência grave, ligue " +
            "imediatamente para o SAMU (192) ou Bombeiros (193)."

    const val FONTE: String = "Baseado em orientações do SAMU e Ministério da Saúde"

    /** Mensagem do lembrete semestral de revisão. */
    const val LEMBRETE_REVISAO: String =
        "Revise sua mochila — mesmo itens sem validade vencem ou enferrujam " +
            "(pilha, repelente aberto, isqueiro, lona ressecada)"

    val CARDS: List<CardPrimeirosSocorros> = listOf(
        CardPrimeirosSocorros(
            titulo = "Engasgo (adulto ou criança maior)",
            sinal = "pessoa não consegue falar, tossir ou respirar; leva as mãos ao pescoço.",
            passos = listOf(
                "Pergunte: \"Você está engasgado?\" Se ela não conseguir responder, aja.",
                "Se ainda conseguir tossir, incentive a tossir — a tosse é a melhor forma de desobstruir.",
                "Se não conseguir tossir, falar ou respirar: aplique a Manobra de Heimlich — fique " +
                    "atrás da pessoa, feche o punho acima do umbigo e faça compressões para dentro e para cima.",
                "Repita até o objeto sair ou a pessoa desmaiar.",
                "Se desmaiar, inicie RCP e ligue 192."
            ),
            naoFaca = "não introduza o dedo na boca \"às cegas\" para tentar puxar o objeto.",
            numeroDestaque = "192"
        ),
        CardPrimeirosSocorros(
            titulo = "RCP",
            sinal = "pessoa inconsciente, não respira ou respira de forma anormal (suspiros).",
            passos = listOf(
                "Ligue 192 imediatamente ou peça para alguém ligar.",
                "Coloque a pessoa de costas em superfície firme.",
                "Posicione as mãos no centro do peito (metade inferior do esterno).",
                "Faça compressões fortes e rápidas: cerca de 100 a 120 por minuto, afundando o " +
                    "peito uns 5 cm.",
                "Se souber fazer ventilação, alterne 30 compressões + 2 ventilações. Se não souber " +
                    "ou não quiser, faça somente compressões até o SAMU chegar."
            ),
            naoFaca = "não interrompa as compressões sem necessidade. Não tenha medo de machucar — " +
                "sem RCP, a chance de sobrevivência é quase zero.",
            numeroDestaque = "192"
        ),
        CardPrimeirosSocorros(
            titulo = "Queimadura",
            sinal = null,
            passos = listOf(
                "Resfrie a área com água corrente em temperatura ambiente por 10 a 20 minutos.",
                "Retire roupas e joias da região, a menos que estejam grudadas na pele.",
                "Cubra a queimadura com pano limpo ou gaze úmida.",
                "Ligue 192 se a queimadura for extensa, profunda ou atingir rosto, mãos, pés ou genitais."
            ),
            naoFaca = "não aplique pasta de dente, manteiga, borra de café, pomada caseira nem gelo. " +
                "Não estoure bolhas.",
            numeroDestaque = "192"
        ),
        CardPrimeirosSocorros(
            titulo = "Desmaio",
            sinal = null,
            passos = listOf(
                "Deite a pessoa em local ventilado.",
                "Eleve as pernas levemente (cerca de 30 cm).",
                "Afrouxe roupas apertadas.",
                "Verifique se ela está respirando normalmente.",
                "Se não recobrar a consciência em 1 minuto, ligue 192."
            ),
            naoFaca = "não jogue água no rosto, não dê líquidos enquanto ela estiver inconsciente, " +
                "não tente levantar a pessoa à força.",
            numeroDestaque = "192"
        ),
        CardPrimeirosSocorros(
            titulo = "Convulsão",
            sinal = null,
            passos = listOf(
                "Proteja a cabeça — coloque algo macio embaixo.",
                "Afaste objetos próximos que possam machucar.",
                "Marque a duração da crise (use o celular).",
                "Após a crise, coloque a pessoa de lado (posição lateral de segurança) e verifique " +
                    "a respiração.",
                "Ligue 192 se a crise durar mais de 5 minutos, se repetir ou se for a primeira " +
                    "convulsão da pessoa."
            ),
            naoFaca = "nunca coloque nada na boca da pessoa (nem colher, nem pano, nem dedo). " +
                "Não tente segurar os movimentos à força.",
            numeroDestaque = "192"
        ),
        CardPrimeirosSocorros(
            titulo = "Hemorragia",
            sinal = null,
            passos = listOf(
                "Aplique pressão contínua com pano limpo sobre o ferimento por pelo menos 10 minutos.",
                "Se possível, eleve o membro acima da altura do peito.",
                "Se o sangramento não parar ou for pulsátil (jato), ligue 192.",
                "Mantenha a pressão até a chegada do socorro."
            ),
            naoFaca = "não retire o pano de pressão para \"olhar\" o ferimento — isso reinicia o " +
                "sangramento. Não use torniquete improvisado sem orientação, a menos que seja " +
                "hemorragia grave em membro e você saiba fazer.",
            numeroDestaque = "192"
        )
    )
}
