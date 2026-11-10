package br.com.mochiladeemergencia.app.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mochiladeemergencia.app.data.local.entity.DocumentoEntity
import br.com.mochiladeemergencia.app.di.ServicosLocais
import br.com.mochiladeemergencia.app.domain.model.TipoDocumento
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class DocumentosUiState(
    val carregando: Boolean = true,
    val perfilId: Long = 0L,
    val perfilNome: String = "",
    val documentos: List<DocumentoEntity> = emptyList()
) {
    val vazio: Boolean get() = documentos.isEmpty()
}

/** Lista de documentos do perfil ativo. */
@OptIn(ExperimentalCoroutinesApi::class)
class DocumentosViewModel(private val servicos: ServicosLocais) : ViewModel() {

    private val perfilAtivo = fluxoPerfilAtivo(servicos)

    private val documentos = perfilAtivo.flatMapLatest { perfil ->
        if (perfil == null) flowOf(emptyList())
        else servicos.documentoRepository.observar(perfil.id)
    }

    val estado: StateFlow<DocumentosUiState> =
        combine(perfilAtivo, documentos) { perfil, lista ->
            DocumentosUiState(
                carregando = false,
                perfilId = perfil?.id ?: 0L,
                perfilNome = perfil?.nome ?: "",
                documentos = lista
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DocumentosUiState()
        )

    fun excluir(documento: DocumentoEntity) {
        viewModelScope.launch { servicos.documentoRepository.excluir(documento) }
    }
}

@Immutable
data class DocumentoFormUiState(
    val carregando: Boolean = true,
    val documentoId: Long = 0L,
    val perfilId: Long = 0L,
    val titulo: String = "",
    val tipo: TipoDocumento = TipoDocumento.LINK,
    val uri: String = "",
    val categoria: String = "",
    val observacao: String = "",
    val nomeArquivo: String = "",
    val erroTitulo: String? = null,
    val erroUri: String? = null,
    val erroSalvar: String? = null,
    val salvo: Boolean = false
) {
    val editando: Boolean get() = documentoId > 0L
    val esperaArquivo: Boolean get() = tipo != TipoDocumento.LINK
}

/** Cadastro/edição de documento (link ou arquivo local escolhido pelo usuário). */
class DocumentoFormViewModel(private val servicos: ServicosLocais) : ViewModel() {

    private val _estado = MutableStateFlow(DocumentoFormUiState())
    val estado: StateFlow<DocumentoFormUiState> = _estado.asStateFlow()

    private var original: DocumentoEntity? = null

    fun carregar(documentoId: Long) {
        viewModelScope.launch {
            val perfil = fluxoPerfilAtivo(servicos).first()
            if (documentoId > 0L) {
                val documento = servicos.documentoRepository.buscar(documentoId)
                original = documento
                if (documento != null) {
                    _estado.value = DocumentoFormUiState(
                        carregando = false,
                        documentoId = documento.id,
                        perfilId = documento.perfilId,
                        titulo = documento.titulo,
                        tipo = TipoDocumento.dePersistido(documento.tipo),
                        uri = documento.uri,
                        categoria = documento.categoria,
                        observacao = documento.observacao,
                        nomeArquivo = if (documento.arquivoLocal) "Arquivo selecionado" else ""
                    )
                    return@launch
                }
            }
            _estado.value = DocumentoFormUiState(
                carregando = false,
                perfilId = perfil?.id ?: 0L
            )
        }
    }

    fun definirTitulo(valor: String) =
        _estado.update { it.copy(titulo = valor, erroTitulo = null) }

    fun definirTipo(tipo: TipoDocumento) = _estado.update {
        // Ao trocar entre link e arquivo, o destino anterior não faz sentido.
        val limpar = (it.tipo == TipoDocumento.LINK) != (tipo == TipoDocumento.LINK)
        it.copy(tipo = tipo, uri = if (limpar) "" else it.uri, erroUri = null)
    }

    fun definirUri(valor: String) =
        _estado.update { it.copy(uri = valor.trim(), erroUri = null) }

    /** Guarda a URI de um arquivo escolhido no seletor do Android. */
    fun definirArquivo(uri: String, nomeExibicao: String) =
        _estado.update {
            it.copy(uri = uri, nomeArquivo = nomeExibicao, erroUri = null)
        }

    fun definirCategoria(valor: String) = _estado.update { it.copy(categoria = valor.take(40)) }

    fun definirObservacao(valor: String) = _estado.update { it.copy(observacao = valor.take(1000)) }

    fun salvar() {
        val atual = _estado.value
        val titulo = atual.titulo.trim()
        val uri = atual.uri.trim()

        val erroTitulo = if (titulo.isBlank()) "Informe o título do documento." else null
        val erroUri = when {
            uri.isBlank() -> if (atual.esperaArquivo) {
                "Escolha um arquivo no aparelho."
            } else {
                "Informe o link do documento."
            }
            atual.tipo == TipoDocumento.LINK &&
                !uri.lowercase().startsWith("http://") &&
                !uri.lowercase().startsWith("https://") ->
                "O link deve começar com http:// ou https://."
            atual.esperaArquivo && !uri.startsWith("content://") ->
                "Escolha um arquivo pelo seletor do Android."
            else -> null
        }
        if (erroTitulo != null || erroUri != null) {
            _estado.update { it.copy(erroTitulo = erroTitulo, erroUri = erroUri) }
            return
        }

        viewModelScope.launch {
            try {
                val perfilId = if (atual.perfilId > 0L) atual.perfilId
                else fluxoPerfilAtivo(servicos).first()?.id ?: 0L
                if (perfilId <= 0L) {
                    _estado.update { it.copy(erroSalvar = "Nenhum perfil ativo encontrado.") }
                    return@launch
                }
                val base = original ?: DocumentoEntity(perfilId = perfilId, titulo = titulo)
                servicos.documentoRepository.salvar(
                    base.copy(
                        perfilId = perfilId,
                        titulo = titulo,
                        tipo = atual.tipo.name,
                        uri = uri,
                        categoria = atual.categoria,
                        observacao = atual.observacao,
                        arquivoLocal = atual.esperaArquivo
                    )
                )
                _estado.update { it.copy(salvo = true, erroSalvar = null) }
            } catch (erro: IllegalArgumentException) {
                _estado.update { it.copy(erroSalvar = erro.message) }
            } catch (erro: Exception) {
                _estado.update {
                    it.copy(erroSalvar = "Não foi possível salvar o documento. Tente novamente.")
                }
            }
        }
    }
}
