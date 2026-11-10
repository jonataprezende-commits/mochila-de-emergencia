package br.com.mochiladeemergencia.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import br.com.mochiladeemergencia.app.data.backup.BackupRepository
import br.com.mochiladeemergencia.app.data.local.AppDatabase
import br.com.mochiladeemergencia.app.data.local.entity.ContatoEntity
import br.com.mochiladeemergencia.app.data.local.entity.ItemEntity
import br.com.mochiladeemergencia.app.data.prefs.ArmazenamentoConfiguracoes
import br.com.mochiladeemergencia.app.data.prefs.Configuracoes
import br.com.mochiladeemergencia.app.data.repository.ChecklistRepository
import br.com.mochiladeemergencia.app.data.repository.ContatoRepository
import br.com.mochiladeemergencia.app.data.repository.ItemRepository
import br.com.mochiladeemergencia.app.data.repository.PerfilRepository
import br.com.mochiladeemergencia.app.domain.PrimeirosSocorros
import br.com.mochiladeemergencia.app.domain.StatusCalculadora
import br.com.mochiladeemergencia.app.domain.model.StatusItem
import br.com.mochiladeemergencia.app.domain.model.Tema
import br.com.mochiladeemergencia.app.util.Fotos
import br.com.mochiladeemergencia.app.work.RevisaoSemestralWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import java.io.File
import java.time.LocalDate

/**
 * Testes dos recursos da versão 2: status "Repor", localização da mochila,
 * fotos (com backup Base64), contatos pessoais e conteúdo de primeiros socorros.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class NovosFluxosTest {

    private lateinit var contexto: Context
    private lateinit var banco: AppDatabase
    private lateinit var itens: ItemRepository
    private lateinit var perfis: PerfilRepository
    private lateinit var contatos: ContatoRepository
    private lateinit var backup: BackupRepository

    @Before
    fun preparar() {
        contexto = ApplicationProvider.getApplicationContext()
        banco = AppDatabase.emMemoria(contexto)
        itens = ItemRepository(banco.itemDao())
        val checklist = ChecklistRepository(banco.checklistDao(), banco.itemDao())
        perfis = PerfilRepository(banco.perfilDao(), banco.checklistDao(), checklist)
        contatos = ContatoRepository(banco.contatoDao())
        backup = BackupRepository(banco, ConfigEmMemoria(), contexto)
    }

    @After
    fun encerrar() {
        banco.close()
    }

    // ----------------------------- status Repor -----------------------------

    @Test
    fun `item consumido vai para Repor e volta para Tenho ao repor`() = runBlocking {
        val perfilId = perfis.garantirPerfilPadrao()
        val id = itens.salvar(
            ItemEntity(
                perfilId = perfilId,
                nome = "Sardinha",
                validadeEpochDay = LocalDate.now().plusDays(200).toEpochDay()
            )
        )

        itens.marcarComoConsumido(itens.buscar(id)!!)
        val consumido = itens.buscar(id)!!
        assertTrue(consumido.aRepor)
        assertFalse(consumido.marcadoComoFalta)
        assertNull(consumido.validadeEpochDay)
        assertEquals(
            StatusItem.REPOR,
            StatusCalculadora.status(consumido.validadeEpochDay, consumido.marcadoComoFalta, 60, aRepor = consumido.aRepor)
        )

        itens.repor(consumido)
        val reposto = itens.buscar(id)!!
        assertFalse(reposto.aRepor)
        assertEquals(
            StatusItem.TENHO,
            StatusCalculadora.status(reposto.validadeEpochDay, reposto.marcadoComoFalta, 60, aRepor = reposto.aRepor)
        )
    }

    @Test
    fun `status Repor tem precedencia sobre vencido`() {
        val status = StatusCalculadora.status(
            validadeEpochDay = LocalDate.of(2020, 1, 1).toEpochDay(),
            marcadoComoFalta = false,
            antecedenciaDias = 60,
            hoje = LocalDate.of(2026, 11, 10),
            aRepor = true
        )
        assertEquals(StatusItem.REPOR, status)
    }

    @Test
    fun `conferencia aplica Tenho Falta e Substituir`() = runBlocking {
        val perfilId = perfis.garantirPerfilPadrao()
        val a = itens.salvar(ItemEntity(perfilId = perfilId, nome = "A", marcadoComoFalta = true))
        val b = itens.salvar(ItemEntity(perfilId = perfilId, nome = "B"))
        val c = itens.salvar(
            ItemEntity(
                perfilId = perfilId,
                nome = "C",
                validadeEpochDay = LocalDate.now().plusDays(300).toEpochDay()
            )
        )

        itens.aplicarConferencia(itens.buscar(a)!!, StatusItem.TENHO)
        itens.aplicarConferencia(itens.buscar(b)!!, StatusItem.FALTA)
        itens.aplicarConferencia(itens.buscar(c)!!, StatusItem.REPOR)

        assertFalse(itens.buscar(a)!!.marcadoComoFalta)
        assertTrue(itens.buscar(b)!!.marcadoComoFalta)
        assertTrue(itens.buscar(c)!!.aRepor)
        assertNull(itens.buscar(c)!!.validadeEpochDay)
    }

    // ------------------------- localização da mochila -------------------------

    @Test
    fun `localizacao da mochila e salva e aparece no perfil`() = runBlocking {
        val perfilId = perfis.garantirPerfilPadrao()
        val perfil = perfis.buscarPorId(perfilId)!!
        perfis.definirLocalizacao(perfil, "  Armário da entrada  ")
        assertEquals("Armário da entrada", perfis.buscarPorId(perfilId)!!.localizacao)
    }

    // --------------------------------- fotos ---------------------------------

    @Test
    fun `foto e reduzida salva localmente e vira miniatura`() {
        val bitmap = Bitmap.createBitmap(2000, 1500, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.GREEN)
        }
        val pasta = Fotos.pasta(contexto)
        val arquivo = File(pasta, "entrada.jpg")
        arquivo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }

        val nome = Fotos.salvarDeArquivo(contexto, arquivo)
        assertNotNull(nome)
        assertTrue(Fotos.nomeValido(nome))
        val salvo = Fotos.arquivo(contexto, nome!!)!!
        assertTrue("A foto deve ser recomprimida abaixo de ~400 KB", salvo.length() < 400 * 1024)

        val miniatura = Fotos.miniatura(contexto, nome, lado = 160)
        assertNotNull(miniatura)
        assertTrue(miniatura!!.width <= 2000)
    }

    @Test
    fun `nomes de foto invalidos sao recusados (sem path traversal)`() {
        val invalidos = listOf(
            "../fora.jpg",
            "..%2Ffora.jpg",
            "/etc/passwd.jpg",
            "foto.jpg/../x.jpg",
            "foto.exe",
            null
        )
        invalidos.forEach { nome ->
            assertFalse("Deveria recusar: $nome", Fotos.nomeValido(nome))
            assertNull(Fotos.arquivo(contexto, nome))
        }
        assertTrue(Fotos.nomeValido("foto_123.jpg"))
    }

    @Test
    fun `foto sobrevive ao ciclo exportar e importar backup`() = runBlocking {
        val perfilId = perfis.garantirPerfilPadrao()
        val bitmap = Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.BLUE)
        }
        val tmp = File(Fotos.pastaTemporaria(contexto), "tmp.jpg")
        tmp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        val nomeFoto = Fotos.salvarDeArquivo(contexto, tmp)
        assertNotNull(nomeFoto)

        itens.salvar(
            ItemEntity(perfilId = perfilId, nome = "Lanterna", fotoArquivo = nomeFoto)
        )

        val json = backup.exportarJson()
        assertTrue("O JSON deve conter a foto em Base64", json.contains("fotoBase64"))
        val validado = backup.validar(json)
        val fotoNoBackup = validado.perfis.first().itens.first().fotoBase64
        assertNotNull(fotoNoBackup)

        // Importa substituindo: a foto deve ser recriada com um novo nome.
        backup.importar(validado, substituir = true)
        val itemImportado = banco.itemDao().listarTodos().first()
        assertNotNull(itemImportado.fotoArquivo)
        assertTrue(Fotos.arquivo(contexto, itemImportado.fotoArquivo) != null)
    }

    // -------------------------------- contatos --------------------------------

    @Test
    fun `contato pessoal e salvo com telefone saneado`() = runBlocking {
        val perfilId = perfis.garantirPerfilPadrao()
        contatos.salvar(
            ContatoEntity(
                perfilId = perfilId,
                nome = "  Maria (vizinha)  ",
                telefone = "(21) 99999-8888",
                parentesco = "Vizinha",
                observacao = "Tem chave reserva"
            )
        )
        val lista = contatos.listar(perfilId)
        assertEquals(1, lista.size)
        assertEquals("Maria (vizinha)", lista.first().nome)
        assertEquals("21999998888", lista.first().telefone)
    }

    @Test
    fun `contato sem nome ou sem numero e recusado`() = runBlocking {
        val perfilId = perfis.garantirPerfilPadrao()
        try {
            contatos.salvar(ContatoEntity(perfilId = perfilId, nome = " ", telefone = "123"))
            fail("Deveria recusar nome vazio")
        } catch (_: IllegalArgumentException) {
        }
        try {
            contatos.salvar(ContatoEntity(perfilId = perfilId, nome = "Zé", telefone = "---"))
            fail("Deveria recusar telefone sem números")
        } catch (_: IllegalArgumentException) {
        }
        assertTrue(contatos.listar(perfilId).isEmpty())
    }

    @Test
    fun `contatos entram no backup e voltam na importacao`() = runBlocking {
        val perfilId = perfis.garantirPerfilPadrao()
        contatos.salvar(
            ContatoEntity(perfilId = perfilId, nome = "João", telefone = "21999990000", parentesco = "Pai")
        )
        val json = backup.exportarJson()
        val validado = backup.validar(json)
        assertEquals(1, validado.perfis.first().contatos.size)

        backup.importar(validado, substituir = true)
        val importados = banco.contatoDao().listarTodos()
        assertEquals(1, importados.size)
        assertEquals("João", importados.first().nome)
        assertEquals("21999990000", importados.first().telefone)
    }

    @Test
    fun `backup json com contato invalido e recusado`() = runBlocking {
        val perfilId = perfis.garantirPerfilPadrao()
        contatos.salvar(ContatoEntity(perfilId = perfilId, nome = "João", telefone = "21999990000"))
        val jsonComContatoInvalido = """
            {"aplicativo":"Mochila de Emergência","versaoFormato":2,
             "perfis":[{"nome":"Casa","contatos":[{"nome":"X","telefone":"---"}]}]}
        """.trimIndent()
        try {
            backup.validar(jsonComContatoInvalido)
            fail("Deveria recusar contato sem número")
        } catch (_: Exception) {
        }
        assertEquals(1, banco.contatoDao().listarTodos().size)
    }

    @Test
    fun `excluir perfil remove contatos em cascata`() = runBlocking {
        val casa = perfis.garantirPerfilPadrao()
        val sitio = perfis.criar("Sítio")
        contatos.salvar(ContatoEntity(perfilId = casa, nome = "Casa", telefone = "111"))
        contatos.salvar(ContatoEntity(perfilId = sitio, nome = "Sítio", telefone = "222"))

        perfis.excluir(perfis.buscarPorId(sitio)!!)
        assertEquals(1, banco.contatoDao().listarTodos().size)
        assertEquals("Casa", banco.contatoDao().listarTodos().first().nome)
    }

    // ---------------------------- primeiros socorros ----------------------------

    @Test
    fun `cards de primeiros socorros tem conteudo completo`() {
        assertEquals(6, PrimeirosSocorros.CARDS.size)
        assertEquals(
            listOf("Engasgo (adulto ou criança maior)", "RCP", "Queimadura", "Desmaio", "Convulsão", "Hemorragia"),
            PrimeirosSocorros.CARDS.map { it.titulo }
        )
        PrimeirosSocorros.CARDS.forEach { card ->
            assertTrue("Passos do card ${card.titulo}", card.passos.isNotEmpty())
            assertTrue("Máximo de 6 passos", card.passos.size <= 6)
            assertTrue("Não faça do card ${card.titulo}", card.naoFaca.isNotBlank())
            assertEquals("192", card.numeroDestaque)
        }
        // Sinais exigidos pelo escopo.
        assertNotNull(PrimeirosSocorros.CARDS.first { it.titulo.startsWith("Engasgo") }.sinal)
        assertNotNull(PrimeirosSocorros.CARDS.first { it.titulo == "RCP" }.sinal)

        // Aviso e fonte padronizados.
        assertTrue(PrimeirosSocorros.AVISO.contains("não substituem treinamento prático"))
        assertEquals("Baseado em orientações do SAMU e Ministério da Saúde", PrimeirosSocorros.FONTE)

        // Nenhum card deve conter instruções de fogo/invasivas.
        val proibidos = listOf("incêndio", "extintor", "torniquete improvisado é sempre")
        PrimeirosSocorros.CARDS.forEach { card ->
            proibidos.forEach { termo ->
                assertFalse(
                    "Card ${card.titulo} não deve instruir sobre $termo",
                    card.passos.joinToString(" ").lowercase().contains(termo)
                )
            }
        }
    }

    @Test
    fun `lembrete semestral usa o texto e o periodo definidos`() {
        assertTrue(PrimeirosSocorros.LEMBRETE_REVISAO.contains("Revise sua mochila"))
        assertTrue(PrimeirosSocorros.LEMBRETE_REVISAO.contains("pilha"))
        assertEquals(182L, RevisaoSemestralWorker.PERIODO_DIAS)
        assertEquals("lembrete_semestral_revisao", RevisaoSemestralWorker.NOME_TRABALHO)
    }
}

/** Configurações em memória para os testes do backup. */
private class ConfigEmMemoria : ArmazenamentoConfiguracoes {
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
            lembreteSemestralAtivo = lembreteSemestralAtivo ?: estado.value.lembreteSemestralAtivo
        )
    }

    override suspend fun definirPerfilAtivo(perfilId: Long) {
        estado.value = estado.value.copy(perfilAtivoId = perfilId)
    }
}
