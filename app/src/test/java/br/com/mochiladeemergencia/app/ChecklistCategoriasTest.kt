package br.com.mochiladeemergencia.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.mochiladeemergencia.app.data.local.AppDatabase
import br.com.mochiladeemergencia.app.data.local.entity.ChecklistEntity
import br.com.mochiladeemergencia.app.data.local.entity.ItemEntity
import br.com.mochiladeemergencia.app.data.repository.ChecklistPadrao
import br.com.mochiladeemergencia.app.data.repository.ChecklistRepository
import br.com.mochiladeemergencia.app.data.repository.ContatosEmergenciaFixos
import br.com.mochiladeemergencia.app.data.repository.ItemRepository
import br.com.mochiladeemergencia.app.data.repository.PerfilRepository
import br.com.mochiladeemergencia.app.domain.model.Categoria
import br.com.mochiladeemergencia.app.domain.model.FormaConsumo
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regras do checklist por categoria: sugestões editáveis, dicas opcionais,
 * nenhuma quantidade imposta, sincronização entre versões e o que o app não sugere.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class ChecklistCategoriasTest {

    private lateinit var banco: AppDatabase
    private lateinit var checklist: ChecklistRepository
    private lateinit var itens: ItemRepository
    private lateinit var perfis: PerfilRepository

    @Before
    fun preparar() {
        val contexto: Context = ApplicationProvider.getApplicationContext()
        banco = AppDatabase.emMemoria(contexto)
        checklist = ChecklistRepository(banco.checklistDao(), banco.itemDao())
        itens = ItemRepository(banco.itemDao())
        perfis = PerfilRepository(banco.perfilDao(), banco.checklistDao(), checklist)
    }

    @After
    fun encerrar() = banco.close()

    // --------------------------------- conteúdo ---------------------------------

    @Test
    fun `sugestoes cobrem todas as categorias do escopo e tem nome curto`() {
        val categorias = ChecklistPadrao.SUGESTOES.map { it.categoria }.toSet()
        assertTrue(categorias.contains(Categoria.AGUA_E_BEBIDAS))
        assertTrue(categorias.contains(Categoria.COMIDA_DE_EMERGENCIA))
        assertTrue(categorias.contains(Categoria.UTENSILIOS))
        assertTrue(categorias.contains(Categoria.ILUMINACAO_E_COMUNICACAO))
        assertTrue(categorias.contains(Categoria.FERRAMENTAS_E_ABRIGO))
        assertTrue(categorias.contains(Categoria.HIGIENE))
        assertTrue(categorias.contains(Categoria.SAUDE_E_PRIMEIROS_SOCORROS))
        assertTrue(categorias.contains(Categoria.DOCUMENTOS_E_VALORES))
        assertTrue(categorias.contains(Categoria.VESTUARIO))
        assertTrue(categorias.contains(Categoria.FAMILIA_E_NECESSIDADES))
        assertTrue(categorias.contains(Categoria.CONFORTO_EMOCIONAL))
        // Todas as categorias de exibição, exceto "Outros", estão representadas.
        assertEquals(Categoria.entries.size - 1, categorias.size)

        ChecklistPadrao.SUGESTOES.forEach { sugestao ->
            assertTrue("Nome vazio em ${sugestao.chave}", sugestao.nome.isNotBlank())
            // Nome curto: cabe em uma linha de card.
            assertTrue("Nome longo em ${sugestao.chave}: ${sugestao.nome}", sugestao.nome.length <= 52)
            assertTrue("Dica longa em ${sugestao.chave}", sugestao.dica.length <= 120)
        }
        // As chaves são únicas.
        assertEquals(
            ChecklistPadrao.SUGESTOES.size,
            ChecklistPadrao.SUGESTOES.map { it.chave }.toSet().size
        )
    }

    @Test
    fun `app nao sugere itens de uso restrito nem exige preparo elaborado`() {
        val proibidos = listOf(
            "arma", "munição", "municao", "rifle", "pistola", "faca de combate",
            "explosivo", "pólvora", "polvora", "caça", "caca", "bebida alcoólica",
            "refrigerar", "geladeira", "congelado"
        )
        val texto = ChecklistPadrao.SUGESTOES.joinToString(" ") { "${it.nome} ${it.dica}" }.lowercase()
        proibidos.forEach { termo ->
            assertFalse("Sugestão proibida encontrada: $termo", texto.contains(termo))
        }
        // Remédios aparecem só como uso contínuo e a receita junto — nunca um remédio específico.
        assertTrue(ChecklistPadrao.SUGESTOES.any { it.chave == "remedios_continuos" })
        assertTrue(ChecklistPadrao.SUGESTOES.any { it.chave == "recipiente_remedios" })
        assertFalse(texto.contains("dipirona"))
        assertFalse(texto.contains("antibiótico"))
    }

    @Test
    fun `dica de fosforos e de agua seguem o texto pedido`() {
        val fosforos = ChecklistPadrao.SUGESTOES.first { it.chave == "fosforos" }
        assertEquals("Prefira fósforos à prova d'água", fosforos.dica)

        val agua = ChecklistPadrao.SUGESTOES.first { it.chave == "agua" }
        assertEquals(
            "2 litros por pessoa por dia; 6 litros por pessoa para 72 horas",
            agua.dica
        )

        val enlatados = ChecklistPadrao.SUGESTOES.first { it.chave == "leguminosas_lata" }
        assertTrue(enlatados.dica.contains("amassadas"))
    }

    @Test
    fun `biblia continua no checklist e sem dica`() {
        val biblia = ChecklistPadrao.SUGESTOES.first { it.chave == "biblia" }
        assertEquals("Bíblia", biblia.nome)
        assertEquals(Categoria.CONFORTO_EMOCIONAL, biblia.categoria)
        assertEquals("", biblia.dica)
        // Nenhuma sugestão traz texto que sugira julgamento sobre a escolha do usuário.
        assertTrue(ChecklistPadrao.SUGESTOES.none { it.dica.contains("sentido para você") })
    }

    @Test
    fun `lista fixa de emergencia tem os quatro numeros e nao tem mais o CVV`() {
        val numeros = ContatosEmergenciaFixos.LISTA.map { it.numero }
        assertEquals(listOf("192", "193", "190", "199"), numeros)
        assertFalse(numeros.contains("188"))
        assertFalse(ContatosEmergenciaFixos.LISTA.any { it.nome.contains("CVV", ignoreCase = true) })
    }

    // ------------------------------ comportamento ------------------------------

    @Test
    fun `checklist padrao nasce com dicas e sem quantidade imposta`() = runBlocking {
        val perfilId = perfis.garantirPerfilPadrao()
        val sugestoes = checklist.listar(perfilId)
        assertEquals(ChecklistPadrao.SUGESTOES.size, sugestoes.size)

        // Toda sugestão padrão aponta para uma categoria válida (nunca "Outros").
        assertTrue(sugestoes.none { it.categoria == Categoria.OUTROS.name })
        // Nenhuma sugestão padrão "carrega" uma quantidade grande para a mochila.
        assertTrue(sugestoes.all { it.quantidadeBase <= 6.0 })

        val agua = sugestoes.first { it.chave == "agua" }
        assertTrue(agua.dica.isNotBlank())
        assertTrue(checklist.adicionarAMochila(perfilId, agua))

        val item = itens.listar(perfilId).single()
        assertEquals(1.0, item.quantidade, 0.001)
        assertEquals("L", item.unidade)
        assertEquals(Categoria.AGUA_E_BEBIDAS.name, item.categoria)
    }

    @Test
    fun `dica por pessoa so aparece com o ajuste ligado e nao altera o item`() = runBlocking {
        val perfilId = perfis.garantirPerfilPadrao()
        val agua = checklist.listar(perfilId).first { it.chave == "agua" }

        assertNull(checklist.dicaDeQuantidade(agua, pessoasCasa = 4, ajustarPorPessoas = false))
        assertEquals("Para 4 pessoas: cerca de 24 L", checklist.dicaDeQuantidade(agua, 4, true))
        // Refazer a dica não grava nada no banco.
        checklist.dicaDeQuantidade(agua, 4, true)
        assertEquals(
            checklist.listar(perfilId).first { it.chave == "agua" }.quantidadeBase,
            agua.quantidadeBase,
            0.001
        )

        // Sugestão sem referência por pessoa não mostra dica nenhuma.
        val lanterna = checklist.listar(perfilId).first { it.chave == "lanterna" }
        assertNull(checklist.dicaDeQuantidade(lanterna, 4, true))
    }

    @Test
    fun `sugestao personalizada pode ter dica e aparece no checklist`() = runBlocking {
        val perfilId = perfis.garantirPerfilPadrao()
        val totalAntes = checklist.listar(perfilId).size
        checklist.criarSugestao(
            perfilId = perfilId,
            nome = "Pilha AA extra",
            categoria = Categoria.ILUMINACAO_E_COMUNICACAO,
            unidade = "un",
            dica = "Guardar fora do aparelho"
        )
        val lista = checklist.listar(perfilId)
        assertEquals(totalAntes + 1, lista.size)
        val nova = lista.first { it.nome == "Pilha AA extra" }
        assertTrue(nova.personalizado)
        assertEquals("Guardar fora do aparelho", nova.dica)
        assertEquals(Categoria.ILUMINACAO_E_COMUNICACAO.name, nova.categoria)
    }

    @Test
    fun `sincronizacao troca sugestoes antigas e preserva personalizadas`() = runBlocking {
        val perfilId = perfis.garantirPerfilPadrao()
        // Simula um perfil vindo da versão antiga: sugestão padrão obsoleta + personalizada.
        banco.checklistDao().inserir(
            ChecklistEntity(
                perfilId = perfilId,
                chave = "sardinha",
                nome = "Sardinha em lata",
                categoria = Categoria.COMIDA_DE_EMERGENCIA.name,
                quantidadeBase = 2.0,
                unidade = "lata",
                ordem = 999,
                personalizado = false
            )
        )
        banco.checklistDao().inserir(
            ChecklistEntity(
                perfilId = perfilId,
                chave = "minha_custom",
                nome = "Meu item",
                categoria = Categoria.HIGIENE.name,
                quantidadeBase = 1.0,
                unidade = "un",
                ordem = 1000,
                personalizado = true,
                dica = "minha dica"
            )
        )

        checklist.sincronizarPadroes(perfilId)
        val lista = checklist.listar(perfilId)

        // A sugestão que saiu do código foi removida...
        assertTrue(lista.none { it.chave == "sardinha" })
        // ...a personalizada continua, com a dica...
        assertTrue(lista.any { it.chave == "minha_custom" && it.dica == "minha dica" })
        // ...e todas as sugestões atuais existem.
        ChecklistPadrao.CHAVES.forEach { chave ->
            assertTrue("Faltou $chave", lista.any { it.chave == chave })
        }
    }

    @Test
    fun `dica antiga da biblia gravada no banco e corrigida ao sincronizar`() = runBlocking {
        val perfilId = perfis.garantirPerfilPadrao()
        val biblia = checklist.listar(perfilId).first { it.chave == "biblia" }

        // Simula o banco de quem instalou a 1.2.0: a linha já existe com o texto antigo.
        checklist.atualizar(biblia.copy(dica = "Se fizer sentido para você"))
        assertEquals("Se fizer sentido para você", checklist.listar(perfilId).first { it.chave == "biblia" }.dica)

        checklist.sincronizarPadroes(perfilId)

        val depois = checklist.listar(perfilId).first { it.chave == "biblia" }
        assertEquals("", depois.dica)
        assertTrue(ChecklistPadrao.SUGESTOES.first { it.chave == "biblia" }.dica.isBlank())
    }

    @Test
    fun `correcao de texto antigo roda na abertura e nao apaga edicao do usuario`() = runBlocking {
        val perfilId = perfis.garantirPerfilPadrao()
        val biblia = checklist.listar(perfilId).first { it.chave == "biblia" }
        checklist.atualizar(biblia.copy(dica = "Se fizer sentido para você"))
        val lanterna = checklist.listar(perfilId).first { it.chave == "lanterna" }
        checklist.atualizar(lanterna.copy(dica = "Deixar no criado-mudo"))

        val corrigidas = checklist.corrigirTextosAntigos(perfilId)

        assertEquals(1, corrigidas)
        assertEquals("", checklist.listar(perfilId).first { it.chave == "biblia" }.dica)
        // Dica escrita pelo usuário (que não é um texto antigo conhecido) fica intacta.
        assertEquals("Deixar no criado-mudo", checklist.listar(perfilId).first { it.chave == "lanterna" }.dica)
    }

    @Test
    fun `correcao de texto antigo nao mexe em sugestao personalizada`() = runBlocking {
        val perfilId = perfis.garantirPerfilPadrao()
        banco.checklistDao().inserir(
            ChecklistEntity(
                perfilId = perfilId,
                chave = "biblia_minha",
                nome = "Bíblia de bolso",
                categoria = Categoria.CONFORTO_EMOCIONAL.name,
                quantidadeBase = 1.0,
                unidade = "un",
                ordem = 1001,
                personalizado = true,
                dica = "Se fizer sentido para você"
            )
        )

        checklist.corrigirTextosAntigos(perfilId)

        val minha = checklist.listar(perfilId).first { it.chave == "biblia_minha" }
        assertEquals("Se fizer sentido para você", minha.dica)
    }

    @Test
    fun `deteccao do texto antigo tolera variacoes de escrita`() {
        // Variações que podem existir no banco de versões anteriores.
        listOf(
            "Se fizer sentido para você",
            "Se fizer sentido para você.",
            "se fizer sentido pra voce",
            "FAZ SENTIDO PRA VC",
            "  Se fizer sentido para você  "
        ).forEach { variacao ->
            assertTrue("Não detectou: $variacao", ChecklistPadrao.dicaDesatualizada("biblia", variacao))
        }
        // Dicas legítimas não são confundidas.
        assertFalse(ChecklistPadrao.dicaDesatualizada("biblia", ""))
        assertFalse(ChecklistPadrao.dicaDesatualizada("biblia", "Deixar na sala"))
        // Outros itens não têm texto antigo mapeado.
        assertFalse(ChecklistPadrao.dicaDesatualizada("lanterna", "Se fizer sentido para você"))
    }

    @Test
    fun `sincronizacao preserva edicao feita pelo usuario em sugestao padrao`() = runBlocking {
        val perfilId = perfis.garantirPerfilPadrao()
        val lanterna = checklist.listar(perfilId).first { it.chave == "lanterna" }
        checklist.atualizar(lanterna.copy(nome = "Lanterna de cabeça"))

        checklist.sincronizarPadroes(perfilId)
        val depois = checklist.listar(perfilId).first { it.chave == "lanterna" }
        assertEquals("Lanterna de cabeça", depois.nome)
    }

    @Test
    fun `restaurar padroes volta ao texto original e mantem personalizadas`() = runBlocking {
        val perfilId = perfis.garantirPerfilPadrao()
        val lanterna = checklist.listar(perfilId).first { it.chave == "lanterna" }
        checklist.atualizar(lanterna.copy(nome = "Lanterna de cabeça"))
        checklist.criarSugestao(perfilId, "Meu item", Categoria.OUTROS, unidade = "un")

        checklist.restaurarPadroes(perfilId)
        val lista = checklist.listar(perfilId)
        assertEquals("Lanterna", lista.first { it.chave == "lanterna" }.nome)
        assertTrue(lista.any { it.nome == "Meu item" })
        assertEquals(ChecklistPadrao.SUGESTOES.size + 1, lista.size)
    }

    // ------------------------------- categorias -------------------------------

    @Test
    fun `categorias antigas continuam sendo reconhecidas`() {
        assertEquals(Categoria.COMIDA_DE_EMERGENCIA, Categoria.dePersistido("AGUA_E_ALIMENTOS"))
        assertEquals(Categoria.ILUMINACAO_E_COMUNICACAO, Categoria.dePersistido("ILUMINACAO"))
        assertEquals(Categoria.ILUMINACAO_E_COMUNICACAO, Categoria.dePersistido("COMUNICACAO"))
        assertEquals(Categoria.FERRAMENTAS_E_ABRIGO, Categoria.dePersistido("ABRIGO"))
        assertEquals(Categoria.FERRAMENTAS_E_ABRIGO, Categoria.dePersistido("FERRAMENTAS"))
        assertEquals(Categoria.SAUDE_E_PRIMEIROS_SOCORROS, Categoria.dePersistido("PRIMEIROS_SOCORROS"))
        assertEquals(Categoria.DOCUMENTOS_E_VALORES, Categoria.dePersistido("DOCUMENTOS"))
        assertEquals(Categoria.OUTROS, Categoria.dePersistido("QUALQUER_COISA"))
        assertEquals(Categoria.OUTROS, Categoria.dePersistido(null))
        assertEquals(Categoria.VESTUARIO, Categoria.dePersistido("VESTUARIO"))
    }

    // ---------------------------- forma de consumo ----------------------------

    @Test
    fun `forma de consumo e opcional e fica gravada no item`() = runBlocking {
        val perfilId = perfis.garantirPerfilPadrao()
        assertEquals(FormaConsumo.NAO_INFORMADO, FormaConsumo.dePersistido(""))
        assertEquals("", FormaConsumo.persistir(FormaConsumo.NAO_INFORMADO))
        assertEquals("AQUECER", FormaConsumo.persistir(FormaConsumo.AQUECER))
        assertEquals(FormaConsumo.ADICIONAR_AGUA, FormaConsumo.dePersistido("ADICIONAR_AGUA"))
        // Valor desconhecido (ex.: backup de terceiros) cai no "não informado".
        assertEquals(FormaConsumo.NAO_INFORMADO, FormaConsumo.dePersistido("FRITAR"))

        val id = itens.salvar(
            ItemEntity(
                perfilId = perfilId,
                nome = "Macarrão instantâneo",
                categoria = Categoria.COMIDA_DE_EMERGENCIA.name,
                formaConsumo = FormaConsumo.persistir(FormaConsumo.AQUECER)
            )
        )
        val salvo = itens.buscar(id)
        assertNotNull(salvo)
        assertEquals("AQUECER", salvo!!.formaConsumo)

        // Item sem forma de consumo continua válido.
        val outro = itens.salvar(ItemEntity(perfilId = perfilId, nome = "Corda"))
        assertEquals("", itens.buscar(outro)!!.formaConsumo)
    }
}
