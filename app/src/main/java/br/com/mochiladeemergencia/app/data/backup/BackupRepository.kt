package br.com.mochiladeemergencia.app.data.backup

import androidx.room.withTransaction
import br.com.mochiladeemergencia.app.data.local.AppDatabase
import br.com.mochiladeemergencia.app.data.local.entity.ChecklistEntity
import br.com.mochiladeemergencia.app.data.local.entity.ContatoEntity
import br.com.mochiladeemergencia.app.data.local.entity.DocumentoEntity
import br.com.mochiladeemergencia.app.data.local.entity.ItemEntity
import br.com.mochiladeemergencia.app.data.local.entity.PerfilEntity
import br.com.mochiladeemergencia.app.data.prefs.ArmazenamentoConfiguracoes
import br.com.mochiladeemergencia.app.data.repository.ItemRepository
import br.com.mochiladeemergencia.app.domain.model.Categoria
import br.com.mochiladeemergencia.app.domain.model.FormaConsumo
import br.com.mochiladeemergencia.app.domain.model.Tema
import br.com.mochiladeemergencia.app.domain.model.TipoDocumento
import br.com.mochiladeemergencia.app.util.Fotos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** Erro de validação do backup — a mensagem é exibida ao usuário. */
class BackupInvalidoException(mensagem: String) : Exception(mensagem)

data class ResumoImportacao(
    val perfis: Int,
    val itens: Int,
    val documentos: Int,
    val sugestoes: Int,
    val contatos: Int,
    val substituiuDados: Boolean
)

/** Estrutura já validada de um backup, pronta para ser gravada no banco. */
data class BackupValidado(
    val versaoFormato: Int,
    val exportadoEm: Long,
    val antecedenciaDias: Int?,
    val notificacoesAtivas: Boolean?,
    val pessoasCasa: Int?,
    val tema: Tema?,
    val lembreteSemestralAtivo: Boolean?,
    val perfis: List<PerfilBackup>
)

data class PerfilBackup(
    val nome: String,
    val localizacao: String,
    val criadoEm: Long,
    val itens: List<ItemBackup>,
    val documentos: List<DocumentoBackup>,
    val checklist: List<ChecklistBackup>,
    val contatos: List<ContatoBackup>
)

data class ItemBackup(
    val nome: String,
    val categoria: String,
    val quantidade: Double,
    val unidade: String,
    val validadeEpochDay: Long?,
    val local: String,
    val observacao: String,
    val falta: Boolean,
    val aRepor: Boolean,
    val chaveChecklist: String?,
    /** Forma de consumo opcional (vazio = não informada). */
    val formaConsumo: String = "",
    /** Foto em Base64 (JPEG já reduzido pelo app); nula quando o item não tem foto. */
    val fotoBase64: String?
)

/** Contato pessoal de emergência incluído no backup. */
data class ContatoBackup(
    val nome: String,
    val telefone: String,
    val parentesco: String,
    val observacao: String
)

data class DocumentoBackup(
    val titulo: String,
    val tipo: String,
    val uri: String,
    val categoria: String,
    val observacao: String,
    val arquivoLocal: Boolean
)

data class ChecklistBackup(
    val chave: String,
    val nome: String,
    val categoria: String,
    val quantidadeBase: Double,
    val unidade: String,
    val porPessoa: Boolean,
    /** Dica curta exibida junto da sugestão. */
    val dica: String = "",
    val ordem: Int,
    val personalizado: Boolean
)

/**
 * Exporta e importa backups JSON 100% locais.
 *
 * Regras de segurança aplicadas:
 * - limite de tamanho do texto (12 MB, por causa das fotos em Base64) e de registros por coleção;
 * - validação de tipos, campos obrigatórios, faixas numéricas e formato de data;
 * - nenhum caminho de arquivo vem do JSON (nada de path traversal);
 * - a importação só grava depois de validar todo o documento;
 * - a substituição de dados exige confirmação explícita do usuário na interface.
 */
class BackupRepository(
    private val db: AppDatabase,
    private val preferencias: ArmazenamentoConfiguracoes,
    /** Contexto usado apenas para ler/gravar as fotos no diretório privado do app. */
    private val contexto: android.content.Context? = null
) {

    suspend fun exportarJson(): String = withContext(Dispatchers.IO) {
        val perfis = db.perfilDao().listarTodos()
        val itens = db.itemDao().listarTodos().groupBy { it.perfilId }
        val documentos = db.documentoDao().listarTodos().groupBy { it.perfilId }
        val checklist = db.checklistDao().listarTodos().groupBy { it.perfilId }
        val contatos = db.contatoDao().listarTodos().groupBy { it.perfilId }
        val config = preferencias.configuracoes.first()
        val appContext = contexto ?: throw IllegalStateException("Contexto ausente para o backup.")

        val raiz = JSONObject()
        raiz.put("aplicativo", "Mochila de Emergência")
        raiz.put("versaoFormato", VERSAO_FORMATO)
        raiz.put("exportadoEm", System.currentTimeMillis())
        raiz.put(
            "configuracoes",
            JSONObject().apply {
                put("antecedenciaDias", config.antecedenciaDias)
                put("notificacoesAtivas", config.notificacoesAtivas)
                put("pessoasCasa", config.pessoasCasa)
                put("tema", config.tema.name)
                put("lembreteSemestralAtivo", config.lembreteSemestralAtivo)
            }
        )

        val listaPerfis = JSONArray()
        perfis.forEach { perfil ->
            val objPerfil = JSONObject()
            objPerfil.put("nome", perfil.nome)
            objPerfil.put("localizacao", perfil.localizacao)
            objPerfil.put("criadoEm", perfil.criadoEm)

            val listaItens = JSONArray()
            itens[perfil.id].orEmpty().forEach { item ->
                listaItens.put(
                    JSONObject().apply {
                        put("nome", item.nome)
                        put("categoria", item.categoria)
                        put("quantidade", item.quantidade)
                        put("unidade", item.unidade)
                        put(
                            "validade",
                            item.validadeEpochDay?.let { LocalDate.ofEpochDay(it).toString() }
                                ?: JSONObject.NULL
                        )
                        put("local", item.local)
                        put("observacao", item.observacao)
                        put("falta", item.marcadoComoFalta)
                        put("aRepor", item.aRepor)
                        put("chaveChecklist", item.chaveChecklist ?: JSONObject.NULL)
                        put("formaConsumo", item.formaConsumo)
                        val foto = Fotos.paraBase64(appContext, item.fotoArquivo)
                        put("fotoBase64", foto ?: JSONObject.NULL)
                    }
                )
            }
            objPerfil.put("itens", listaItens)

            val listaDocumentos = JSONArray()
            documentos[perfil.id].orEmpty().forEach { documento ->
                listaDocumentos.put(
                    JSONObject().apply {
                        put("titulo", documento.titulo)
                        put("tipo", documento.tipo)
                        put("uri", documento.uri)
                        put("categoria", documento.categoria)
                        put("observacao", documento.observacao)
                        put("arquivoLocal", documento.arquivoLocal)
                    }
                )
            }
            objPerfil.put("documentos", listaDocumentos)

            val listaChecklist = JSONArray()
            checklist[perfil.id].orEmpty().forEach { sugestao ->
                listaChecklist.put(
                    JSONObject().apply {
                        put("chave", sugestao.chave)
                        put("nome", sugestao.nome)
                        put("categoria", sugestao.categoria)
                        put("quantidadeBase", sugestao.quantidadeBase)
                        put("unidade", sugestao.unidade)
                        put("porPessoa", sugestao.porPessoa)
                        put("dica", sugestao.dica)
                        put("ordem", sugestao.ordem)
                        put("personalizado", sugestao.personalizado)
                    }
                )
            }
            objPerfil.put("checklist", listaChecklist)

            val listaContatos = JSONArray()
            contatos[perfil.id].orEmpty().forEach { contato ->
                listaContatos.put(
                    JSONObject().apply {
                        put("nome", contato.nome)
                        put("telefone", contato.telefone)
                        put("parentesco", contato.parentesco)
                        put("observacao", contato.observacao)
                    }
                )
            }
            objPerfil.put("contatos", listaContatos)

            listaPerfis.put(objPerfil)
        }
        raiz.put("perfis", listaPerfis)

        val texto = raiz.toString(2)
        // Não adianta entregar um arquivo que o próprio app recusaria na importação.
        if (texto.length > TAMANHO_MAXIMO_CARACTERES) {
            throw BackupInvalidoException(
                "O backup passou de ${TAMANHO_MAXIMO_BYTES / (1024 * 1024)} MB (as fotos ocupam " +
                    "muito espaço). Remova algumas fotos e tente de novo."
            )
        }
        texto
    }

    /** Valida o texto do backup. Não grava nada. */
    fun validar(json: String): BackupValidado {
        if (json.isBlank()) throw BackupInvalidoException("O arquivo de backup está vazio.")
        if (json.length > TAMANHO_MAXIMO_CARACTERES) {
            throw BackupInvalidoException(
                "O arquivo de backup é grande demais (limite de " +
                    "${TAMANHO_MAXIMO_BYTES / (1024 * 1024)} MB)."
            )
        }
        return try {
            val raiz = JSONObject(json)
            val aplicativo = if (raiz.has("aplicativo")) raiz.getString("aplicativo") else ""
            if (aplicativo.isNotEmpty() && !aplicativo.startsWith("Mochila")) {
                throw BackupInvalidoException("Este arquivo não parece ser um backup do app.")
            }
            val versao = if (raiz.has("versaoFormato")) raiz.getInt("versaoFormato") else 1
            if (versao > VERSAO_FORMATO) {
                throw BackupInvalidoException(
                    "Backup criado por uma versão mais nova do app (formato $versao)."
                )
            }
            if (versao < 1) throw BackupInvalidoException("Versão de formato inválida.")

            val config = raiz.optJSONObject("configuracoes")
            val antecedencia = config?.let {
                if (it.has("antecedenciaDias")) {
                    it.getInt("antecedenciaDias").takeIf { d -> d in listOf(30, 60, 90) }
                } else null
            }
            val notificacoes = config?.let {
                if (it.has("notificacoesAtivas")) it.getBoolean("notificacoesAtivas") else null
            }
            val pessoas = config?.let {
                if (it.has("pessoasCasa")) {
                    it.getInt("pessoasCasa").coerceIn(1, 20)
                } else null
            }
            val tema = config?.let {
                if (it.has("tema")) Tema.dePersistido(it.getString("tema")) else null
            }
            val lembreteSemestral = config?.let {
                if (it.has("lembreteSemestralAtivo")) {
                    it.getBoolean("lembreteSemestralAtivo")
                } else null
            }

            val listaPerfis = raiz.optJSONArray("perfis")
                ?: throw BackupInvalidoException("Backup sem a lista de perfis.")
            if (listaPerfis.length() > MAXIMO_PERFIS) {
                throw BackupInvalidoException("Backup com perfis demais (limite $MAXIMO_PERFIS).")
            }
            if (listaPerfis.length() == 0) {
                throw BackupInvalidoException("O backup não contém nenhum perfil.")
            }

            var totalItens = 0
            val perfis = buildList {
                for (i in 0 until listaPerfis.length()) {
                    val objPerfil = listaPerfis.optJSONObject(i)
                        ?: throw BackupInvalidoException("Perfil em formato inválido.")

                    val nome = exigirTexto(objPerfil, "nome", 60, obrigatorio = true)
                    val localizacao = opcionalTexto(objPerfil, "localizacao", 120)
                    val criadoEm = opcionalLong(objPerfil, "criadoEm") ?: System.currentTimeMillis()

                    val itensJson = objPerfil.optJSONArray("itens") ?: JSONArray()
                    if (itensJson.length() > MAXIMO_ITENS_POR_PERFIL) {
                        throw BackupInvalidoException("Perfil com itens demais.")
                    }
                    totalItens += itensJson.length()
                    if (totalItens > MAXIMO_ITENS_TOTAL) {
                        throw BackupInvalidoException("Backup com itens demais.")
                    }

                    val itens = buildList {
                        for (j in 0 until itensJson.length()) {
                            val objItem = itensJson.optJSONObject(j)
                                ?: throw BackupInvalidoException("Item em formato inválido.")
                            add(lerItem(objItem))
                        }
                    }

                    val documentosJson = objPerfil.optJSONArray("documentos") ?: JSONArray()
                    if (documentosJson.length() > MAXIMO_DOCUMENTOS_POR_PERFIL) {
                        throw BackupInvalidoException("Perfil com documentos demais.")
                    }
                    val documentos = buildList {
                        for (j in 0 until documentosJson.length()) {
                            val objDoc = documentosJson.optJSONObject(j)
                                ?: throw BackupInvalidoException("Documento em formato inválido.")
                            add(lerDocumento(objDoc))
                        }
                    }

                    val checklistJson = objPerfil.optJSONArray("checklist") ?: JSONArray()
                    if (checklistJson.length() > MAXIMO_CHECKLIST_POR_PERFIL) {
                        throw BackupInvalidoException("Perfil com sugestões demais no checklist.")
                    }
                    val sugestoes = buildList {
                        for (j in 0 until checklistJson.length()) {
                            val objSugestao = checklistJson.optJSONObject(j)
                                ?: throw BackupInvalidoException("Sugestão em formato inválido.")
                            add(lerSugestao(objSugestao))
                        }
                    }

                    val contatosJson = objPerfil.optJSONArray("contatos") ?: JSONArray()
                    if (contatosJson.length() > MAXIMO_CONTATOS_POR_PERFIL) {
                        throw BackupInvalidoException("Perfil com contatos demais.")
                    }
                    val listaContatos = buildList {
                        for (j in 0 until contatosJson.length()) {
                            val objContato = contatosJson.optJSONObject(j)
                                ?: throw BackupInvalidoException("Contato em formato inválido.")
                            add(lerContato(objContato))
                        }
                    }

                    add(
                        PerfilBackup(
                            nome = nome,
                            localizacao = localizacao,
                            criadoEm = criadoEm,
                            itens = itens,
                            documentos = documentos,
                            checklist = sugestoes,
                            contatos = listaContatos
                        )
                    )
                }
            }

            BackupValidado(
                versaoFormato = versao,
                exportadoEm = raiz.optLong("exportadoEm", System.currentTimeMillis()),
                antecedenciaDias = antecedencia,
                notificacoesAtivas = notificacoes,
                pessoasCasa = pessoas,
                tema = tema,
                lembreteSemestralAtivo = lembreteSemestral,
                perfis = perfis
            )
        } catch (erro: BackupInvalidoException) {
            throw erro
        } catch (erro: Exception) {
            throw BackupInvalidoException(
                "JSON inválido ou incompleto. Nenhum dado foi alterado."
            )
        }
    }

    /**
     * Importa o backup validado.
     * @param substituir true apaga os dados atuais (após confirmação na UI);
     *                   false mescla os perfis importados com os existentes.
     */
    suspend fun importar(backup: BackupValidado, substituir: Boolean): ResumoImportacao =
        withContext(Dispatchers.IO) {
            var totalItens = 0
            var totalDocumentos = 0
            var totalSugestoes = 0
            var totalContatos = 0

            db.withTransaction {
                if (substituir) {
                    // Remove os arquivos de foto dos itens que serão apagados.
                    contexto?.let { ctx ->
                        db.itemDao().listarTodos().forEach { item ->
                            Fotos.excluir(ctx, item.fotoArquivo)
                        }
                    }
                    db.contatoDao().excluirTodos()
                    db.checklistDao().excluirTodos()
                    db.documentoDao().excluirTodos()
                    db.itemDao().excluirTodos()
                    db.perfilDao().excluirTodos()
                }

                backup.perfis.forEach { perfil ->
                    val novoId = db.perfilDao().inserir(
                        PerfilEntity(
                            nome = perfil.nome,
                            localizacao = perfil.localizacao,
                            criadoEm = perfil.criadoEm
                        )
                    )

                    val itens = perfil.itens.map { item ->
                        // A foto do JSON é recriada com nome novo gerado pelo app (nunca o do arquivo).
                        val fotoImportada = item.fotoBase64?.let { texto ->
                            contexto?.let { ctx -> Fotos.deBase64(ctx, texto) }
                        }
                        ItemEntity(
                            perfilId = novoId,
                            nome = item.nome,
                            categoria = Categoria.dePersistido(item.categoria).name,
                            quantidade = item.quantidade,
                            unidade = item.unidade,
                            validadeEpochDay = item.validadeEpochDay,
                            local = item.local,
                            observacao = item.observacao,
                            marcadoComoFalta = item.falta,
                            aRepor = item.aRepor,
                            fotoArquivo = fotoImportada,
                            chaveChecklist = item.chaveChecklist
                        )
                    }
                    if (itens.isNotEmpty()) db.itemDao().inserirTodos(itens)
                    totalItens += itens.size

                    val documentos = perfil.documentos.map { documento ->
                        DocumentoEntity(
                            perfilId = novoId,
                            titulo = documento.titulo,
                            tipo = TipoDocumento.dePersistido(documento.tipo).name,
                            uri = documento.uri,
                            categoria = documento.categoria,
                            observacao = documento.observacao,
                            arquivoLocal = documento.arquivoLocal
                        )
                    }
                    if (documentos.isNotEmpty()) db.documentoDao().inserirTodos(documentos)
                    totalDocumentos += documentos.size

                    val sugestoes = perfil.checklist.map { sugestao ->
                        ChecklistEntity(
                            perfilId = novoId,
                            chave = sugestao.chave,
                            nome = sugestao.nome,
                            categoria = Categoria.dePersistido(sugestao.categoria).name,
                            quantidadeBase = sugestao.quantidadeBase,
                            unidade = sugestao.unidade,
                            porPessoa = sugestao.porPessoa,
                            dica = sugestao.dica,
                            ordem = sugestao.ordem,
                            personalizado = sugestao.personalizado
                        )
                    }
                    if (sugestoes.isNotEmpty()) db.checklistDao().inserirTodos(sugestoes)
                    totalSugestoes += sugestoes.size

                    val contatos = perfil.contatos.map { contato ->
                        ContatoEntity(
                            perfilId = novoId,
                            nome = contato.nome,
                            telefone = contato.telefone,
                            parentesco = contato.parentesco,
                            observacao = contato.observacao
                        )
                    }
                    if (contatos.isNotEmpty()) db.contatoDao().inserirTodos(contatos)
                    totalContatos += contatos.size
                }

                preferencias.aplicarConfiguracoesImportadas(
                    antecedenciaDias = backup.antecedenciaDias,
                    notificacoesAtivas = backup.notificacoesAtivas,
                    pessoasCasa = backup.pessoasCasa,
                    tema = backup.tema,
                    lembreteSemestralAtivo = backup.lembreteSemestralAtivo
                )

                if (substituir) {
                    val primeiro = db.perfilDao().listarTodos().firstOrNull()
                    if (primeiro != null) preferencias.definirPerfilAtivo(primeiro.id)
                }
            }

            ResumoImportacao(
                perfis = backup.perfis.size,
                itens = totalItens,
                documentos = totalDocumentos,
                sugestoes = totalSugestoes,
                contatos = totalContatos,
                substituiuDados = substituir
            )
        }

    // ------------------------- leitura de cada registro -------------------------

    private fun lerItem(obj: JSONObject): ItemBackup {
        val nome = exigirTexto(obj, "nome", 120, obrigatorio = true)
        val quantidade = exigirNumero(obj, "quantidade", padrao = 1.0)
        val validade = if (obj.has("validade") && !obj.isNull("validade")) {
            val texto = obj.getString("validade")
            val data = try {
                LocalDate.parse(texto)
            } catch (erro: DateTimeParseException) {
                throw BackupInvalidoException("Data de validade inválida no backup: $texto")
            }
            val epochDay = data.toEpochDay()
            if (epochDay < ItemRepository.VALIDADE_MINIMA ||
                epochDay > ItemRepository.VALIDADE_MAXIMA
            ) {
                throw BackupInvalidoException("Data de validade fora da faixa aceita.")
            }
            epochDay
        } else {
            null
        }
        val chaveChecklist = if (obj.has("chaveChecklist") && !obj.isNull("chaveChecklist")) {
            obj.getString("chaveChecklist").take(64)
        } else {
            null
        }
        return ItemBackup(
            nome = nome,
            categoria = Categoria.dePersistido(
                if (obj.has("categoria")) obj.getString("categoria") else null
            ).name,
            quantidade = quantidade.coerceIn(0.01, 999999.0),
            unidade = opcionalTexto(obj, "unidade", 12).ifBlank { "un" },
            validadeEpochDay = validade,
            local = opcionalTexto(obj, "local", 80),
            observacao = opcionalTexto(obj, "observacao", 500),
            falta = obj.optBoolean("falta", false),
            aRepor = obj.optBoolean("aRepor", false),
            chaveChecklist = chaveChecklist,
            formaConsumo = lerFormaConsumo(obj),
            fotoBase64 = lerFoto(obj)
        )
    }

    private fun lerDocumento(obj: JSONObject): DocumentoBackup {
        val titulo = exigirTexto(obj, "titulo", 120, obrigatorio = true)
        val uri = exigirTexto(obj, "uri", 2048, obrigatorio = true)
        val tipo = TipoDocumento.dePersistido(
            if (obj.has("tipo")) obj.getString("tipo") else null
        )
        val local = obj.optBoolean("arquivoLocal", tipo != TipoDocumento.LINK)
        if (tipo == TipoDocumento.LINK) {
            val minusculo = uri.lowercase()
            if (!minusculo.startsWith("http://") && !minusculo.startsWith("https://")) {
                throw BackupInvalidoException(
                    "Documento \"$titulo\" tem link inválido (use http:// ou https://)."
                )
            }
        } else if (!uri.startsWith("content://")) {
            throw BackupInvalidoException(
                "Documento \"$titulo\" precisa apontar para um arquivo local do Android."
            )
        }
        return DocumentoBackup(
            titulo = titulo,
            tipo = tipo.name,
            uri = uri,
            categoria = opcionalTexto(obj, "categoria", 40),
            observacao = opcionalTexto(obj, "observacao", 1000),
            arquivoLocal = local
        )
    }

    private fun lerSugestao(obj: JSONObject): ChecklistBackup {
        val nome = exigirTexto(obj, "nome", 80, obrigatorio = true)
        val chave = opcionalTexto(obj, "chave", 64)
            .ifBlank { "chave_${nome.hashCode()}_${obj.optInt("ordem", 0)}" }
        return ChecklistBackup(
            chave = chave,
            nome = nome,
            categoria = Categoria.dePersistido(
                if (obj.has("categoria")) obj.getString("categoria") else null
            ).name,
            quantidadeBase = exigirNumero(obj, "quantidadeBase", padrao = 1.0)
                .coerceIn(0.01, 999999.0),
            unidade = opcionalTexto(obj, "unidade", 12).ifBlank { "un" },
            porPessoa = obj.optBoolean("porPessoa", false),
            dica = opcionalTexto(obj, "dica", 160),
            ordem = obj.optInt("ordem", 0).coerceIn(0, 10_000),
            personalizado = obj.optBoolean("personalizado", true)
        )
    }

    /** Forma de consumo opcional: aceita apenas valores conhecidos; qualquer outro vira vazio. */
    private fun lerFormaConsumo(obj: JSONObject): String {
        val texto = opcionalTexto(obj, "formaConsumo", 20)
        if (texto.isBlank()) return ""
        val forma = FormaConsumo.dePersistido(texto)
        return FormaConsumo.persistir(forma)
    }

    private fun lerContato(obj: JSONObject): ContatoBackup {
        val nome = exigirTexto(obj, "nome", 80, obrigatorio = true)
        val telefoneBruto = exigirTexto(obj, "telefone", 24, obrigatorio = true)
        val telefone = telefoneBruto.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
        if (telefone.isBlank() || telefone.none { it.isDigit() }) {
            throw BackupInvalidoException("Contato \"$nome\" tem telefone inválido.")
        }
        return ContatoBackup(
            nome = nome,
            telefone = telefone,
            parentesco = opcionalTexto(obj, "parentesco", 40),
            observacao = opcionalTexto(obj, "observacao", 200)
        )
    }

    /** Lê a foto em Base64 do item, respeitando o limite por foto. */
    private fun lerFoto(obj: JSONObject): String? {
        if (!obj.has("fotoBase64") || obj.isNull("fotoBase64")) return null
        val texto = obj.get("fotoBase64")
        if (texto !is String) throw BackupInvalidoException("Foto em formato inválido no backup.")
        if (texto.length > MAXIMO_CARACTERES_FOTO) {
            throw BackupInvalidoException("Uma das fotos do backup é grande demais.")
        }
        return texto.ifBlank { null }
    }

    // ------------------------------ utilidades ------------------------------

    private fun exigirTexto(
        obj: JSONObject,
        campo: String,
        maximo: Int,
        obrigatorio: Boolean
    ): String {
        val valor = if (obj.has(campo) && !obj.isNull(campo)) obj.get(campo) else null
        if (valor == null) {
            if (obrigatorio) throw BackupInvalidoException("Campo obrigatório ausente: $campo.")
            return ""
        }
        if (valor !is String) throw BackupInvalidoException("O campo $campo deve ser texto.")
        val limpo = sanearTexto(valor, maximo)
        if (obrigatorio && limpo.isBlank()) {
            throw BackupInvalidoException("O campo $campo não pode ficar vazio.")
        }
        return limpo
    }

    private fun opcionalTexto(obj: JSONObject, campo: String, maximo: Int): String =
        exigirTexto(obj, campo, maximo, obrigatorio = false)

    private fun exigirNumero(obj: JSONObject, campo: String, padrao: Double): Double {
        if (!obj.has(campo) || obj.isNull(campo)) return padrao
        val valor = obj.get(campo)
        if (valor !is Number) throw BackupInvalidoException("O campo $campo deve ser numérico.")
        val numero = valor.toDouble()
        if (!numero.isFinite()) throw BackupInvalidoException("O campo $campo é inválido.")
        return numero
    }

    private fun opcionalLong(obj: JSONObject, campo: String): Long? {
        if (!obj.has(campo) || obj.isNull(campo)) return null
        val valor = obj.get(campo)
        if (valor !is Number) throw BackupInvalidoException("O campo $campo deve ser numérico.")
        return valor.toLong()
    }

    /** Remove caracteres de controle e limita o tamanho do texto importado. */
    private fun sanearTexto(texto: String, maximo: Int): String =
        texto.filter { caractere ->
            !caractere.isISOControl() || caractere == '\n' || caractere == '\t'
        }.trim().take(maximo)

    companion object {
        const val VERSAO_FORMATO = 3
        /** Limite do arquivo JSON (as fotos em Base64 tornam o backup maior que antes). */
        const val TAMANHO_MAXIMO_BYTES = 12 * 1024 * 1024
        const val TAMANHO_MAXIMO_CARACTERES = TAMANHO_MAXIMO_BYTES
        const val MAXIMO_PERFIS = 50
        const val MAXIMO_ITENS_POR_PERFIL = 2_000
        const val MAXIMO_ITENS_TOTAL = 10_000
        const val MAXIMO_DOCUMENTOS_POR_PERFIL = 500
        const val MAXIMO_CHECKLIST_POR_PERFIL = 300
        const val MAXIMO_CONTATOS_POR_PERFIL = 200
        /** ~450 KB em Base64 por foto (o app grava fotos de até ~400 KB). */
        const val MAXIMO_CARACTERES_FOTO = 620_000
    }
}
