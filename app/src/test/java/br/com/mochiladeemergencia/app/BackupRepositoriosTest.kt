package br.com.mochiladeemergencia.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.mochiladeemergencia.app.data.backup.BackupInvalidoException
import br.com.mochiladeemergencia.app.data.backup.BackupRepository
import br.com.mochiladeemergencia.app.data.local.AppDatabase
import br.com.mochiladeemergencia.app.data.local.entity.DocumentoEntity
import br.com.mochiladeemergencia.app.data.local.entity.ItemEntity
import br.com.mochiladeemergencia.app.data.local.entity.PerfilEntity
import br.com.mochiladeemergencia.app.data.prefs.ArmazenamentoConfiguracoes
import br.com.mochiladeemergencia.app.data.prefs.Configuracoes
import br.com.mochiladeemergencia.app.data.repository.ChecklistRepository
import br.com.mochiladeemergencia.app.data.repository.DocumentoRepository
import br.com.mochiladeemergencia.app.data.repository.ItemRepository
import br.com.mochiladeemergencia.app.data.repository.PerfilRepository
import br.com.mochiladeemergencia.app.data.repository.ResultadoExclusaoPerfil
import br.com.mochiladeemergencia.app.domain.model.Tema
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Testes de integração em JVM (Robolectric) com Room em memória.
 * Cobrem: cadastro/edição/exclusão de itens, status, múltiplos perfis,
 * cascata de exclusão e exportação/importação de backup JSON.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class BackupRepositoriosTest {

    private lateinit var contexto: Context
    private lateinit var banco: AppDatabase
    private lateinit var itens: ItemRepository
    private lateinit var documentos: DocumentoRepository
    private lateinit var perfis: PerfilRepository
    private lateinit var backup: BackupRepository

    @Before
    fun preparar() {
        contexto = ApplicationProvider.getApplicationContext()
        banco = AppDatabase.emMemoria(contexto)
        itens = ItemRepository(banco.itemDao())
        documentos = DocumentoRepository(banco.documentoDao())
        val checklist = ChecklistRepository(banco.checklistDao(), banco.itemDao())
        perfis = PerfilRepository(banco.perfilDao(), banco.checklistDao(), checklist)
        backup = BackupRepository(banco, ConfiguracoesEmMemoria(), contexto)
    }

    @After
    fun encerrar() {
        banco.close()
    }

    // ------------------------------- itens ----------------------------------

    @Test
    fun `salvar item com e sem validade`() = runBlocking {
        val perfilId = perfis.garantirPerfilPadrao()

        val comValidade = itens.salvar(
            ItemEntity(
                perfilId = perfilId,
                nome = "Água",
                quantidade = 6.0,
                unidade = "L",
                validadeEpochDay = LocalDate.now().plusDays(20).toEpochDay()
            )
        )
        val semValidade = itens.salvar(
            ItemEntity(perfilId = perfilId, nome = "Corda", quantidade = 10.0, unidade = "m")
        )

        val lista = itens.listar(perfilId)
        assertEquals(2, lista.size)
        assertNotNull(itens.buscar(comValidade)?.validadeEpochDay)
        assertNull(itens.buscar(semValidade)?.validadeEpochDay)
    }

    @Test
    fun `nome obrigatorio e quantidade positiva sao validados`() = runBlocking {
        val perfilId = perfis.garantirPerfilPadrao()
        try {
            itens.salvar(ItemEntity(perfilId = perfilId, nome = "   "))
            fail("Deveria recusar nome vazio")
        } catch (_: IllegalArgumentException) {
        }
        try {
            itens.salvar(ItemEntity(perfilId = perfilId, nome = "Pilhas", quantidade = 0.0))
            fail("Deveria recusar quantidade zero")
        } catch (_: IllegalArgumentException) {
        }
        assertTrue(itens.listar(perfilId).isEmpty())
    }

    @Test
    fun `editar e excluir item`() = runBlocking {
        val perfilId = perfis.garantirPerfilPadrao()
        val id = itens.salvar(ItemEntity(perfilId = perfilId, nome = "Lanterna"))
        val original = itens.buscar(id)!!
        itens.salvar(original.copy(nome = "Lanterna LED", quantidade = 2.0))

        val editado = itens.buscar(id)!!
        assertEquals("Lanterna LED", editado.nome)
        assertEquals(2.0, editado.quantidade, 0.001)

        itens.excluir(editado)
        assertNull(itens.buscar(id))
    }

    @Test
    fun `marcar como consumido envia o item para a lista Repor e sem validade`() = runBlocking {
        val perfilId = perfis.garantirPerfilPadrao()
        val id = itens.salvar(
            ItemEntity(
                perfilId = perfilId,
                nome = "Sardinha",
                validadeEpochDay = LocalDate.now().plusDays(400).toEpochDay()
            )
        )
        itens.marcarComoConsumido(itens.buscar(id)!!)
        val item = itens.buscar(id)!!
        // v2: consumido/substituído vai para "Repor" (não mais para "Falta").
        assertTrue(item.aRepor)
        assertFalse(item.marcadoComoFalta)
        assertNull(item.validadeEpochDay)

        // Repor devolve o item para a mochila com status Tenho.
        itens.repor(item)
        val reposto = itens.buscar(id)!!
        assertFalse(reposto.aRepor)
        assertFalse(reposto.marcadoComoFalta)

        itens.marcarComoReposto(reposto)
        assertFalse(itens.buscar(id)!!.marcadoComoFalta)
    }

    // ------------------------------- perfis ---------------------------------

    @Test
    fun `perfil padrao Casa e criado uma unica vez`() = runBlocking {
        val primeiro = perfis.garantirPerfilPadrao()
        val segundo = perfis.garantirPerfilPadrao()
        assertEquals(primeiro, segundo)
        assertEquals(1, perfis.perfis.first().size)
        assertEquals("Casa", perfis.perfis.first().first().nome)
    }

    @Test
    fun `multiplos perfis isolam itens e exclusao e em cascata`() = runBlocking {
        val casa = perfis.garantirPerfilPadrao()
        val sitio = perfis.criar("Sítio")
        itens.salvar(ItemEntity(perfilId = casa, nome = "Água"))
        itens.salvar(ItemEntity(perfilId = sitio, nome = "Lona"))

        assertEquals(1, itens.listar(casa).size)
        assertEquals(1, itens.listar(sitio).size)

        val perfilSitio = perfis.buscarPorId(sitio)!!
        val resultado = perfis.excluir(perfilSitio)
        assertTrue(resultado is ResultadoExclusaoPerfil.Sucesso)
        assertTrue(itens.listar(sitio).isEmpty())
        assertEquals(1, itens.listar(casa).size)
    }

    @Test
    fun `nao e possivel excluir o ultimo perfil`() = runBlocking {
        perfis.garantirPerfilPadrao()
        val unico = perfis.perfis.first().first()
        assertEquals(ResultadoExclusaoPerfil.UltimoPerfil, perfis.excluir(unico))
    }

    // ------------------------------ documentos ------------------------------

    @Test
    fun `documento aceita link http e arquivo local content uri`() = runBlocking {
        val perfilId = perfis.garantirPerfilPadrao()
        documentos.salvar(
            DocumentoEntity(
                perfilId = perfilId,
                titulo = "Defesa Civil",
                tipo = "LINK",
                uri = "https://www.gov.br/defesacivil"
            )
        )
        documentos.salvar(
            DocumentoEntity(
                perfilId = perfilId,
                titulo = "Documentos da família",
                tipo = "PDF",
                uri = "content://com.android.providers.media.documents/document/document%3A42"
            )
        )
        val lista = documentos.listar(perfilId)
        assertEquals(2, lista.size)
        assertTrue(lista.any { it.arquivoLocal })
    }

    @Test
    fun `documento recusa link perigoso ou vazio`() = runBlocking {
        val perfilId = perfis.garantirPerfilPadrao()
        try {
            documentos.salvar(
                DocumentoEntity(perfilId = perfilId, titulo = "Ruim", tipo = "LINK", uri = "javascript:alert(1)")
            )
            fail("Deveria recusar javascript:")
        } catch (_: IllegalArgumentException) {
        }
        try {
            documentos.salvar(
                DocumentoEntity(perfilId = perfilId, titulo = "Ruim", tipo = "LINK", uri = "file:///etc/passwd")
            )
            fail("Deveria recusar file://")
        } catch (_: IllegalArgumentException) {
        }
    }

    // -------------------------------- backup --------------------------------

    @Test
    fun `backup exporta e importa preservando itens e documentos`() = runBlocking {
        val perfilId = perfis.garantirPerfilPadrao()
        itens.salvar(
            ItemEntity(
                perfilId = perfilId,
                nome = "Água",
                quantidade = 12.0,
                unidade = "L",
                validadeEpochDay = LocalDate.of(2027, 5, 20).toEpochDay()
            )
        )
        documentos.salvar(
            DocumentoEntity(
                perfilId = perfilId,
                titulo = "Contatos",
                tipo = "TEXTO",
                uri = "content://docs/1"
            )
        )

        val json = backup.exportarJson()
        val validado = backup.validar(json)
        assertEquals(1, validado.perfis.size)
        assertEquals(1, validado.perfis.first().itens.size)
        assertEquals(1, validado.perfis.first().documentos.size)

        // Importa substituindo: os dados atuais são trocados pelos do arquivo.
        val resumo = backup.importar(validado, substituir = true)
        assertEquals(1, resumo.perfis)
        assertEquals(1, resumo.itens)
        assertEquals(1, resumo.documentos)

        val perfisApos = banco.perfilDao().listarTodos()
        assertEquals(1, perfisApos.size)
        val itensApos = banco.itemDao().listarTodos()
        assertEquals(1, itensApos.size)
        assertEquals("Água", itensApos.first().nome)
        assertEquals(12.0, itensApos.first().quantidade, 0.001)
    }

    @Test
    fun `importacao mesclada mantem os dados existentes`() = runBlocking {
        val casa = perfis.garantirPerfilPadrao()
        itens.salvar(ItemEntity(perfilId = casa, nome = "Lanterna"))

        val json = backup.exportarJson()
        val validado = backup.validar(json)
        val resumo = backup.importar(validado, substituir = false)

        assertEquals(1, resumo.perfis)
        assertEquals(2, banco.perfilDao().listarTodos().size)
        assertEquals(2, banco.itemDao().listarTodos().size)
    }

    @Test
    fun `json invalido nao altera dados existentes`() = runBlocking {
        val casa = perfis.garantirPerfilPadrao()
        itens.salvar(ItemEntity(perfilId = casa, nome = "Água"))

        val invalidos = listOf(
            "",
            "isto não é json",
            "{}",
            """{"aplicativo":"Outro app","perfis":[]}""",
            """{"perfis":[{"nome":""}]}""",
            """{"versaoFormato":99,"perfis":[{"nome":"Casa"}]}""",
            """{"perfis":[{"nome":"Casa","itens":[{"nome":"X","validade":"31/12/2026"}]}]}""",
            """{"perfis":[{"nome":"Casa","documentos":[{"titulo":"X","uri":"javascript:1","tipo":"LINK"}]}]}"""
        )
        invalidos.forEach { texto ->
            try {
                backup.validar(texto)
                fail("Deveria rejeitar: $texto")
            } catch (_: BackupInvalidoException) {
            }
        }
        assertEquals(1, banco.itemDao().listarTodos().size)
        assertEquals(1, banco.perfilDao().listarTodos().size)
    }

    @Test
    fun `backup grande demais e recusado sem processar`() {
        val texto = "x".repeat(BackupRepository.TAMANHO_MAXIMO_CARACTERES + 10)
        try {
            backup.validar(texto)
            fail("Deveria recusar backup acima do limite")
        } catch (_: BackupInvalidoException) {
        }
    }

    @Test
    fun `checklist padrao e criado com sugestoes e pode ir para a mochila`() = runBlocking {
        val perfilId = perfis.garantirPerfilPadrao()
        val checklist = ChecklistRepository(banco.checklistDao(), banco.itemDao())
        val sugestoes = checklist.listar(perfilId)
        assertTrue(sugestoes.size >= 40)
        assertTrue(sugestoes.any { it.chave == "agua" })

        val agua = sugestoes.first { it.chave == "agua" }
        // O checklist nunca impõe quantidade: a dica é só informativa.
        assertEquals("Para 3 pessoas: cerca de 18 L", checklist.dicaDeQuantidade(agua, 3, true))
        assertNull(checklist.dicaDeQuantidade(agua, 3, false))

        assertTrue(checklist.adicionarAMochila(perfilId, agua))
        val item = itens.listar(perfilId).single()
        assertEquals(1.0, item.quantidade, 0.001)
        assertEquals("L", item.unidade)
        // Não duplica o mesmo item do checklist.
        assertFalse(checklist.adicionarAMochila(perfilId, agua))
        assertEquals(1, itens.listar(perfilId).size)
    }

    @Test
    fun `perfil novo recebe checklist padrao`() = runBlocking {
        val casa = perfis.garantirPerfilPadrao()
        val novo = perfis.criar("Casa da praia")
        assertTrue(banco.checklistDao().listarPorPerfil(novo).isNotEmpty())
        assertTrue(banco.checklistDao().listarPorPerfil(casa).isNotEmpty())
        assertEquals(
            banco.checklistDao().listarPorPerfil(casa).size,
            banco.checklistDao().listarPorPerfil(novo).size
        )
    }
}

/** Implementação em memória das configurações, usada nos testes (sem DataStore). */
class ConfiguracoesEmMemoria : ArmazenamentoConfiguracoes {
    private val estado = MutableStateFlow(Configuracoes())

    override val configuracoes: Flow<Configuracoes> = estado

    override suspend fun aplicarConfiguracoesImportadas(
        antecedenciaDias: Int?,
        notificacoesAtivas: Boolean?,
        pessoasCasa: Int?,
        tema: Tema?,
        lembreteSemestralAtivo: Boolean?
    ) {
        estado.value = estado.value.copy(
            antecedenciaDias = antecedenciaDias ?: estado.value.antecedenciaDias,
            notificacoesAtivas = notificacoesAtivas ?: estado.value.notificacoesAtivas,
            pessoasCasa = pessoasCasa ?: estado.value.pessoasCasa,
            tema = tema ?: estado.value.tema,
            lembreteSemestralAtivo = lembreteSemestralAtivo
                ?: estado.value.lembreteSemestralAtivo
        )
    }

    override suspend fun definirPerfilAtivo(perfilId: Long) {
        estado.value = estado.value.copy(perfilAtivoId = perfilId)
    }
}
