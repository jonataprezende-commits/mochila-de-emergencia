package br.com.mochiladeemergencia.app.data.repository

import br.com.mochiladeemergencia.app.data.local.dao.ChecklistDao
import br.com.mochiladeemergencia.app.data.local.dao.ItemDao
import br.com.mochiladeemergencia.app.data.local.entity.ChecklistEntity
import br.com.mochiladeemergencia.app.data.local.entity.ItemEntity
import br.com.mochiladeemergencia.app.domain.model.Categoria
import br.com.mochiladeemergencia.app.util.Formatos
import kotlinx.coroutines.flow.Flow

/**
 * Uma sugestão do checklist padrão (definida em código, copiada para o banco por perfil).
 *
 * Não existe quantidade "imposta": [porPessoaReferencia] serve apenas para montar uma
 * **dica** aproximada quando o usuário liga o ajuste por número de pessoas.
 */
data class SugestaoPadrao(
    val chave: String,
    val nome: String,
    val categoria: Categoria,
    val unidade: String = "un",
    val dica: String = "",
    val porPessoaReferencia: Double? = null
)

/**
 * Checklist padrão sugerido, organizado por categoria.
 *
 * Regras de conteúdo (deliberadas):
 * - são **sugestões editáveis**, não imposições — o usuário pode editar ou remover qualquer linha;
 * - nada de alimentos que exijam geladeira ou cozimento elaborado;
 * - nada de armas, munição, itens de caça ou objetos de uso restrito;
 * - nada de instruções de fabricação de fogo, explosivos ou procedimentos invasivos;
 * - medicamentos apenas como "uso contínuo" e "cópia de receita" (nunca um remédio específico);
 * - latas amassadas/estufadas são citadas só como alerta de descarte.
 */
object ChecklistPadrao {
    val SUGESTOES: List<SugestaoPadrao> = listOf(
        // ------------------------------ Água e bebidas ------------------------------
        SugestaoPadrao(
            "agua", "Água potável", Categoria.AGUA_E_BEBIDAS, "L",
            dica = "2 litros por pessoa por dia; 6 litros por pessoa para 72 horas",
            porPessoaReferencia = 6.0
        ),
        SugestaoPadrao(
            "suco_caixinha", "Suco de caixinha", Categoria.AGUA_E_BEBIDAS,
            dica = "Longa vida (UHT), dura meses na despensa"
        ),
        SugestaoPadrao(
            "leite_po", "Leite em pó", Categoria.AGUA_E_BEBIDAS,
            dica = "Só se houver água potável para reconstituir"
        ),
        SugestaoPadrao(
            "achocolatado", "Achocolatado em pó", Categoria.AGUA_E_BEBIDAS,
            dica = "Opcional"
        ),

        // --------------------------- Comida de emergência ---------------------------
        SugestaoPadrao(
            "frutas_secas", "Frutas secas", Categoria.COMIDA_DE_EMERGENCIA, "pacote",
            dica = "Uva-passa, damasco, banana-passa"
        ),
        SugestaoPadrao(
            "barras_cereal", "Barras de cereal ou de proteína", Categoria.COMIDA_DE_EMERGENCIA,
            dica = "Fáceis de comer sem preparo"
        ),
        SugestaoPadrao(
            "biscoitos", "Biscoitos", Categoria.COMIDA_DE_EMERGENCIA, "pacote",
            dica = "Água e sal, cream cracker, maisena"
        ),
        SugestaoPadrao(
            "enlatados", "Alimentos enlatados prontos", Categoria.COMIDA_DE_EMERGENCIA, "lata",
            dica = "Atum, sardinha, milho, ervilha, seleta de legumes, salsicha"
        ),
        SugestaoPadrao(
            "leguminosas_lata", "Feijão, lentilha ou grão-de-bico em lata",
            Categoria.COMIDA_DE_EMERGENCIA, "lata",
            dica = "Já cozidos. Descarte latas amassadas ou estufadas"
        ),
        SugestaoPadrao(
            "arroz", "Arroz branco", Categoria.COMIDA_DE_EMERGENCIA, "pacote",
            dica = "Guardado em recipiente hermético"
        ),
        SugestaoPadrao(
            "macarrao", "Macarrão seco ou instantâneo", Categoria.COMIDA_DE_EMERGENCIA, "pacote"
        ),
        SugestaoPadrao(
            "chocolate", "Chocolate", Categoria.COMIDA_DE_EMERGENCIA,
            dica = "Prefira meio amargo, dura mais"
        ),
        SugestaoPadrao(
            "mel", "Mel", Categoria.COMIDA_DE_EMERGENCIA,
            dica = "Praticamente não vence"
        ),
        SugestaoPadrao(
            "pacoca_amendoim", "Paçoca, amendoim e castanhas", Categoria.COMIDA_DE_EMERGENCIA,
            dica = "Boa fonte de energia"
        ),
        SugestaoPadrao(
            "racao_pet", "Ração e petiscos do animal", Categoria.COMIDA_DE_EMERGENCIA, "pacote",
            dica = "Se houver animais de estimação na casa"
        ),

        // ---------------------- Utensílios de cozinha e consumo ----------------------
        SugestaoPadrao(
            "talheres", "Talheres, prato e copo reutilizável", Categoria.UTENSILIOS, "kit",
            dica = "Um conjunto por pessoa"
        ),
        SugestaoPadrao(
            "abridor_lata", "Abridor de lata manual", Categoria.UTENSILIOS,
            dica = "Não depende de energia"
        ),

        // ------------------------ Iluminação e comunicação --------------------------
        SugestaoPadrao("lanterna", "Lanterna", Categoria.ILUMINACAO_E_COMUNICACAO),
        SugestaoPadrao(
            "pilhas", "Pilhas e baterias adicionais", Categoria.ILUMINACAO_E_COMUNICACAO,
            dica = "Confira o tamanho que cada aparelho usa"
        ),
        SugestaoPadrao(
            "radio", "Rádio a bateria", Categoria.ILUMINACAO_E_COMUNICACAO,
            dica = "Recebe avisos da Defesa Civil sem internet"
        ),
        SugestaoPadrao(
            "apito", "Apito", Categoria.ILUMINACAO_E_COMUNICACAO,
            dica = "Serve para pedir ajuda de longe"
        ),
        SugestaoPadrao(
            "carregador", "Carregador portátil e cabo", Categoria.ILUMINACAO_E_COMUNICACAO,
            dica = "Mantenha carregado e recarregue a cada revisão"
        ),

        // ---------------------------- Ferramentas e abrigo --------------------------
        SugestaoPadrao("corda", "Corda", Categoria.FERRAMENTAS_E_ABRIGO, "m"),
        SugestaoPadrao("canivete", "Canivete ou kit de ferramentas", Categoria.FERRAMENTAS_E_ABRIGO),
        SugestaoPadrao(
            "fosforos", "Fósforos à prova d'água e/ou isqueiro", Categoria.FERRAMENTAS_E_ABRIGO,
            dica = "Prefira fósforos à prova d'água"
        ),
        SugestaoPadrao("lona", "Lona plástica para abrigo", Categoria.FERRAMENTAS_E_ABRIGO),
        SugestaoPadrao(
            "fita_adesiva", "Fita adesiva resistente", Categoria.FERRAMENTAS_E_ABRIGO,
            dica = "Prefira a versão à prova d'água"
        ),
        SugestaoPadrao(
            "sacos_plasticos", "Sacos plásticos resistentes", Categoria.FERRAMENTAS_E_ABRIGO,
            dica = "Úteis para proteger documentos e alimentos"
        ),
        SugestaoPadrao(
            "mascaras", "Máscaras contra poeira (PFF2/N95)", Categoria.FERRAMENTAS_E_ABRIGO,
            dica = "Uma por pessoa da casa"
        ),
        SugestaoPadrao("cobertor", "Cobertor térmico ou cobertor comum", Categoria.FERRAMENTAS_E_ABRIGO),

        // ---------------------------------- Higiene ---------------------------------
        SugestaoPadrao("sabonete", "Sabonete", Categoria.HIGIENE),
        SugestaoPadrao("escova_pasta", "Escova e pasta de dente", Categoria.HIGIENE, "kit"),
        SugestaoPadrao(
            "toalha", "Toalha", Categoria.HIGIENE,
            dica = "Uma por pessoa", porPessoaReferencia = 1.0
        ),
        SugestaoPadrao("papel_higienico", "Papel higiênico", Categoria.HIGIENE, "rolo"),
        SugestaoPadrao(
            "absorventes", "Absorventes ou fraldas", Categoria.HIGIENE, "pacote",
            dica = "Conforme a necessidade da casa"
        ),
        SugestaoPadrao("repelente", "Repelente", Categoria.HIGIENE, dica = "Tem validade após aberto"),

        // ------------------------ Saúde e primeiros socorros ------------------------
        SugestaoPadrao("kit_primeiros_socorros", "Kit de primeiros socorros", Categoria.SAUDE_E_PRIMEIROS_SOCORROS, "kit"),
        SugestaoPadrao(
            "remedios_continuos", "Remédios de uso contínuo", Categoria.SAUDE_E_PRIMEIROS_SOCORROS,
            dica = "Leve a receita junto"
        ),
        SugestaoPadrao(
            "recipiente_remedios", "Recipiente à prova d'água", Categoria.SAUDE_E_PRIMEIROS_SOCORROS,
            dica = "Para remédios, cópias de receitas e documentos de saúde"
        ),

        // --------------------------- Documentos e valores ---------------------------
        SugestaoPadrao(
            "documentos", "Cópias de documentos importantes", Categoria.DOCUMENTOS_E_VALORES, "kit",
            dica = "RG, CPF, certidões e cartão do SUS"
        ),
        SugestaoPadrao(
            "lista_contatos", "Lista de contatos de emergência", Categoria.DOCUMENTOS_E_VALORES,
            dica = "Impressa, para quando faltar bateria"
        ),
        SugestaoPadrao(
            "mapa", "Mapa da região", Categoria.DOCUMENTOS_E_VALORES,
            dica = "Impresso ou disponível offline"
        ),
        SugestaoPadrao(
            "cartoes_dinheiro", "Cartões e dinheiro em espécie", Categoria.DOCUMENTOS_E_VALORES, "kit",
            dica = "Notas pequenas ajudam quando não há troco"
        ),
        SugestaoPadrao(
            "chaves_copia", "Cópia das chaves de casa e do carro", Categoria.DOCUMENTOS_E_VALORES, "kit"
        ),

        // --------------------------------- Vestuário --------------------------------
        SugestaoPadrao(
            "roupas", "Roupas resistentes", Categoria.VESTUARIO,
            dica = "Adequadas ao clima da sua região"
        ),
        SugestaoPadrao("calcado", "Calçado resistente e fechado", Categoria.VESTUARIO, "par"),
        SugestaoPadrao("meias", "Meias extras", Categoria.VESTUARIO, "par"),

        // -------------------- Família e necessidades especiais ----------------------
        SugestaoPadrao(
            "itens_criancas", "Itens para crianças", Categoria.FAMILIA_E_NECESSIDADES, "kit",
            dica = "Fralda, leite, comida infantil, fórmula"
        ),
        SugestaoPadrao(
            "itens_idosos", "Itens para idosos", Categoria.FAMILIA_E_NECESSIDADES, "kit",
            dica = "Remédios, óculos, bengala, alimentos de fácil mastigação"
        ),
        SugestaoPadrao(
            "itens_deficiencia", "Itens para pessoas com deficiência",
            Categoria.FAMILIA_E_NECESSIDADES, "kit",
            dica = "Cuidados específicos de cada pessoa"
        ),
        SugestaoPadrao(
            "papel_lapis_brinquedos", "Papel, lápis, livros e brinquedos",
            Categoria.FAMILIA_E_NECESSIDADES, "kit",
            dica = "Ajuda a distrair as crianças"
        ),

        // ------------------------ Conforto e apoio emocional ------------------------
        SugestaoPadrao("biblia", "Bíblia", Categoria.CONFORTO_EMOCIONAL),
        SugestaoPadrao(
            "livro_caderno", "Livro ou caderno para anotações", Categoria.CONFORTO_EMOCIONAL
        ),
        SugestaoPadrao(
            "item_conforto", "Item de conforto pessoal", Categoria.CONFORTO_EMOCIONAL,
            dica = "Opcional"
        )
    )

    /** Chaves sugeridas, na ordem em que aparecem no checklist. */
    /**
     * Dicas padrão que **mudaram de texto** entre versões do app.
     *
     * Existe porque a sincronização insere o que falta, mas não reescreve o que já está
     * gravado no banco: quem instalou uma versão anterior continuaria vendo o texto velho
     * para sempre. A correção só é aplicada quando a dica gravada **contém** um dos trechos
     * antigos conhecidos — qualquer outro texto do usuário é preservado.
     */
    private val DICAS_ANTIGAS: Map<String, List<Regex>> = mapOf(
        // 1.2.0 → 1.2.1: a dica soava como julgamento e foi removida.
        // O padrão roda sobre o texto já normalizado (sem acento, minúsculo), então tolera
        // "para/pra", "você/voce/vc" e pontuação que possam existir no banco antigo.
        "biblia" to listOf(Regex("""sentido\s+(para|pra)\s+(voce|vc)\b"""))
    )

    /** true quando [dicaAtual] contém um texto antigo conhecido de [chave]. */
    fun dicaDesatualizada(chave: String, dicaAtual: String): Boolean {
        val padroes = DICAS_ANTIGAS[chave] ?: return false
        val atual = Formatos.normalizarBusca(dicaAtual)
        if (atual.isEmpty()) return false
        return padroes.any { padrao -> padrao.containsMatchIn(atual) }
    }

    val CHAVES: List<String> = SUGESTOES.map { it.chave }

    /** Categorias na ordem de exibição do checklist (só as que têm sugestões). */
    val CATEGORIAS: List<Categoria> = SUGESTOES.map { it.categoria }.distinct()
}

/**
 * Checklist por perfil: sugestões padrão editáveis + sugestões personalizadas.
 *
 * As sugestões padrão são sincronizadas com o código a cada abertura do app:
 * as novas entram, as que saíram do código somem e as editadas pelo usuário são preservadas.
 */
class ChecklistRepository(
    private val checklistDao: ChecklistDao,
    private val itemDao: ItemDao
) {

    fun observar(perfilId: Long): Flow<List<ChecklistEntity>> =
        checklistDao.observarPorPerfil(perfilId)

    suspend fun listar(perfilId: Long): List<ChecklistEntity> =
        checklistDao.listarPorPerfil(perfilId)

    /**
     * Cria o checklist padrão do perfil caso ainda não exista e aplica as mudanças
     * de sugestões entre versões do app. É idempotente.
     */
    suspend fun criarChecklistPadrao(perfilId: Long) = sincronizarPadroes(perfilId)

    /** Alinha as sugestões padrão do perfil com [ChecklistPadrao] sem apagar personalizações. */
    suspend fun sincronizarPadroes(perfilId: Long) {
        val atuais = checklistDao.listarPorPerfil(perfilId)
        val chavesValidas = ChecklistPadrao.CHAVES.toSet()

        // Sugestões padrão que não existem mais no código (a lista mudou de versão).
        atuais.filter { !it.personalizado && it.chave !in chavesValidas }
            .forEach { checklistDao.excluir(it) }

        val chavesExistentes = atuais.map { it.chave }.toSet()
        val faltantes = ChecklistPadrao.SUGESTOES.withIndex()
            .filter { (_, sugestao) -> sugestao.chave !in chavesExistentes }
            .map { (indice, sugestao) -> sugestao.paraEntidade(perfilId, indice) }

        if (faltantes.isNotEmpty()) checklistDao.inserirTodos(faltantes)

        // Repara dicas que mudaram de texto entre versões (ver ChecklistPadrao.DICAS_ANTIGAS).
        corrigirTextosAntigos(perfilId)
    }

    /**
     * Reescreve a dica de sugestões **padrão** cujo texto gravado ainda é um texto antigo
     * conhecido. Nunca toca em sugestões personalizadas nem em dicas editadas pelo usuário.
     *
     * Roda na abertura do app e a cada sincronização — é idempotente e barato (uma leitura
     * da tabela do perfil, sem escrita quando nada mudou).
     */
    suspend fun corrigirTextosAntigos(perfilId: Long): Int {
        val padraoPorChave = ChecklistPadrao.SUGESTOES.associateBy { it.chave }
        val corrigidas = checklistDao.listarPorPerfil(perfilId).mapNotNull { linha ->
            if (linha.personalizado) return@mapNotNull null
            val padrao = padraoPorChave[linha.chave] ?: return@mapNotNull null
            if (linha.dica == padrao.dica) return@mapNotNull null
            if (!ChecklistPadrao.dicaDesatualizada(linha.chave, linha.dica)) return@mapNotNull null
            linha.copy(dica = padrao.dica)
        }
        corrigidas.forEach { checklistDao.atualizar(it) }
        return corrigidas.size
    }

    /**
     * Dica de quantidade aproximada, exibida **somente** quando o usuário liga o ajuste
     * por número de pessoas. Não altera nenhuma quantidade.
     */
    fun dicaDeQuantidade(
        sugestao: ChecklistEntity,
        pessoasCasa: Int,
        ajustarPorPessoas: Boolean
    ): String? {
        if (!ajustarPorPessoas || !sugestao.porPessoa) return null
        val pessoas = pessoasCasa.coerceIn(1, 20)
        val total = sugestao.quantidadeBase * pessoas
        val quantidade = Formatos.quantidade(total)
        val rotulo = if (pessoas == 1) "1 pessoa" else "$pessoas pessoas"
        return "Para $rotulo: cerca de $quantidade ${sugestao.unidade}"
    }

    /**
     * Adiciona a sugestão à mochila do perfil.
     *
     * A quantidade entra como **1** (a menor unidade) e o usuário ajusta depois — o app
     * não impõe quantidades a partir do checklist.
     *
     * @return false quando o item já foi adicionado (identificado pela chave da sugestão).
     */
    suspend fun adicionarAMochila(
        perfilId: Long,
        sugestao: ChecklistEntity,
        quantidade: Double = 1.0
    ): Boolean {
        val existente = itemDao.buscarPorChaveChecklist(perfilId, sugestao.chave)
        if (existente != null) return false
        itemDao.inserir(
            ItemEntity(
                perfilId = perfilId,
                nome = sugestao.nome,
                categoria = sugestao.categoria,
                quantidade = quantidade.coerceIn(0.01, 999999.0),
                unidade = sugestao.unidade.ifBlank { "un" },
                chaveChecklist = sugestao.chave
            )
        )
        return true
    }

    suspend fun criarSugestao(
        perfilId: Long,
        nome: String,
        categoria: Categoria,
        quantidadeBase: Double = 1.0,
        unidade: String = "un",
        dica: String = ""
    ): Long {
        val proximaOrdem = (checklistDao.listarPorPerfil(perfilId).filter { it.personalizado }
            .maxOfOrNull { it.ordem } ?: (ChecklistPadrao.SUGESTOES.size + 1))
        val chave = "custom_${System.currentTimeMillis()}_${(0..999).random()}"
        return checklistDao.inserir(
            ChecklistEntity(
                perfilId = perfilId,
                chave = chave,
                nome = nome.trim().take(80),
                categoria = categoria.name,
                quantidadeBase = quantidadeBase.coerceIn(0.01, 999999.0),
                unidade = unidade.trim().take(12).ifBlank { "un" },
                dica = dica.trim().take(160),
                ordem = proximaOrdem,
                personalizado = true
            )
        )
    }

    suspend fun atualizar(sugestao: ChecklistEntity) = checklistDao.atualizar(sugestao)

    suspend fun excluir(sugestao: ChecklistEntity) = checklistDao.excluir(sugestao)

    /** Restaura as sugestões padrão (mantém as personalizadas). */
    suspend fun restaurarPadroes(perfilId: Long) {
        checklistDao.excluirPadroesPorPerfil(perfilId)
        ChecklistPadrao.SUGESTOES.mapIndexed { indice, sugestao ->
            sugestao.paraEntidade(perfilId, indice)
        }.let { checklistDao.inserirTodos(it) }
    }

    private fun SugestaoPadrao.paraEntidade(perfilId: Long, ordem: Int) = ChecklistEntity(
        perfilId = perfilId,
        chave = chave,
        nome = nome,
        categoria = categoria.name,
        // A referência por pessoa fica em quantidadeBase apenas para montar a dica.
        quantidadeBase = porPessoaReferencia ?: 1.0,
        unidade = unidade,
        porPessoa = porPessoaReferencia != null,
        dica = dica,
        ordem = ordem,
        personalizado = false
    )
}
