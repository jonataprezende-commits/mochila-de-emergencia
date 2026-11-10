package br.com.mochiladeemergencia.app.domain.model

/**
 * Categorias de itens da mochila de emergência.
 *
 * A ordem das entradas é a mesma usada para agrupar o checklist na tela.
 * O nome do enum é persistido no banco; o rótulo é apenas para exibição.
 *
 * Os nomes antigos (`AGUA_E_ALIMENTOS`, `ILUMINACAO`, `COMUNICACAO`, `ABRIGO`,
 * `FERRAMENTAS`, `PRIMEIROS_SOCORROS`, `DOCUMENTOS`) continuam sendo aceitos por
 * [dePersistido] e são convertidos na migração 2 → 3 do banco.
 */
enum class Categoria(val rotulo: String) {
    AGUA_E_BEBIDAS("Água e bebidas"),
    COMIDA_DE_EMERGENCIA("Comida de emergência"),
    UTENSILIOS("Utensílios de cozinha e consumo"),
    ILUMINACAO_E_COMUNICACAO("Iluminação e comunicação"),
    FERRAMENTAS_E_ABRIGO("Ferramentas e abrigo"),
    HIGIENE("Higiene"),
    SAUDE_E_PRIMEIROS_SOCORROS("Saúde e primeiros socorros"),
    DOCUMENTOS_E_VALORES("Documentos e valores"),
    VESTUARIO("Vestuário"),
    FAMILIA_E_NECESSIDADES("Família e necessidades especiais"),
    CONFORTO_EMOCIONAL("Conforto e apoio emocional"),
    OUTROS("Outros");

    companion object {
        /** Nomes usados por versões anteriores do app, mapeados para as categorias atuais. */
        private val CATEGORIAS_ANTIGAS: Map<String, Categoria> = mapOf(
            "AGUA_E_ALIMENTOS" to COMIDA_DE_EMERGENCIA,
            "ILUMINACAO" to ILUMINACAO_E_COMUNICACAO,
            "COMUNICACAO" to ILUMINACAO_E_COMUNICACAO,
            "ABRIGO" to FERRAMENTAS_E_ABRIGO,
            "FERRAMENTAS" to FERRAMENTAS_E_ABRIGO,
            "PRIMEIROS_SOCORROS" to SAUDE_E_PRIMEIROS_SOCORROS,
            "DOCUMENTOS" to DOCUMENTOS_E_VALORES
        )

        /** Categorias usadas nos itens de comida (onde faz sentido informar a forma de consumo). */
        val DE_ALIMENTO: Set<Categoria> = setOf(AGUA_E_BEBIDAS, COMIDA_DE_EMERGENCIA)

        /** Converte um texto persistido em [Categoria], com fallback seguro para [OUTROS]. */
        fun dePersistido(valor: String?): Categoria {
            if (valor.isNullOrBlank()) return OUTROS
            entries.firstOrNull { it.name == valor }?.let { return it }
            return CATEGORIAS_ANTIGAS[valor] ?: OUTROS
        }
    }
}

/** Status exibido de um item, calculado a partir da validade e da marcação do usuário. */
enum class StatusItem(val rotulo: String) {
    TENHO("Tenho"),
    FALTA("Falta"),
    REPOR("Repor"),
    VENCENDO("Vencendo"),
    VENCIDO("Vencido")
}

/** Tipos de documento que podem ser guardados (link ou arquivo local). */
enum class TipoDocumento(val rotulo: String) {
    LINK("Link"),
    PDF("PDF"),
    IMAGEM("Imagem"),
    VIDEO("Vídeo"),
    TEXTO("Texto");

    companion object {
        fun dePersistido(valor: String?): TipoDocumento =
            entries.firstOrNull { it.name == valor } ?: LINK
    }
}

/**
 * Forma de consumo de um item de comida (campo opcional).
 * Guardado como texto vazio quando o usuário não informa.
 */
enum class FormaConsumo(val rotulo: String) {
    NAO_INFORMADO("Não informar"),
    ABRIR_E_COMER("Abrir e comer"),
    AQUECER("Aquecer"),
    ADICIONAR_AGUA("Adicionar água");

    companion object {
        fun dePersistido(valor: String?): FormaConsumo =
            entries.firstOrNull { it.name == valor && it != NAO_INFORMADO } ?: NAO_INFORMADO

        /** Texto gravado no banco (vazio = não informado). */
        fun persistir(forma: FormaConsumo): String =
            if (forma == NAO_INFORMADO) "" else forma.name
    }
}

/** Preferência de tema do app. */
enum class Tema(val rotulo: String) {
    SISTEMA("Sistema"),
    CLARO("Claro"),
    ESCURO("Escuro");

    companion object {
        fun dePersistido(valor: String?): Tema =
            entries.firstOrNull { it.name == valor } ?: SISTEMA
    }
}

/** Unidades usuais para a quantidade do item. */
val UNIDADES_PADRAO: List<String> = listOf(
    "un", "kg", "g", "L", "ml", "caixa", "pacote", "lata", "garrafa", "kit", "m", "par", "rolo"
)
