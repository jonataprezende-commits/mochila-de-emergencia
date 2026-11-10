package br.com.mochiladeemergencia.app.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mochiladeemergencia.app.data.local.entity.ItemEntity
import br.com.mochiladeemergencia.app.di.ServicosLocais
import br.com.mochiladeemergencia.app.domain.model.Categoria
import br.com.mochiladeemergencia.app.domain.model.FormaConsumo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

@Immutable
data class ItemFormUiState(
    val carregando: Boolean = true,
    val itemId: Long = 0L,
    val perfilId: Long = 0L,
    val nome: String = "",
    val categoria: Categoria = Categoria.AGUA_E_BEBIDAS,
    val quantidade: String = "1",
    val unidade: String = "un",
    val validadeEpochDay: Long? = null,
    val local: String = "",
    val observacao: String = "",
    val falta: Boolean = false,
    /** Forma de consumo (opcional) — só faz sentido em itens de comida/bebida. */
    val formaConsumo: FormaConsumo = FormaConsumo.NAO_INFORMADO,
    val fotoArquivo: String? = null,
    /** true quando a foto mudou nesta edição (para apagar a antiga só ao salvar). */
    val fotoAlterada: Boolean = false,
    val erroNome: String? = null,
    val erroQuantidade: String? = null,
    val erroSalvar: String? = null,
    val salvo: Boolean = false
) {
    val editando: Boolean get() = itemId > 0L
}

/** Cadastro e edição de item, com validação dos campos obrigatórios. */
class ItemFormViewModel(private val servicos: ServicosLocais) : ViewModel() {

    private val _estado = MutableStateFlow(ItemFormUiState())
    val estado: StateFlow<ItemFormUiState> = _estado.asStateFlow()

    private var itemOriginal: ItemEntity? = null

    fun carregar(itemId: Long) {
        if (!_estado.value.carregando && _estado.value.itemId == itemId && itemId != 0L) return
        viewModelScope.launch {
            val perfil = fluxoPerfilAtivo(servicos).first()
            if (itemId > 0L) {
                val item = servicos.itemRepository.buscar(itemId)
                itemOriginal = item
                if (item != null) {
                    _estado.value = ItemFormUiState(
                        carregando = false,
                        itemId = item.id,
                        perfilId = item.perfilId,
                        nome = item.nome,
                        categoria = Categoria.dePersistido(item.categoria),
                        quantidade = br.com.mochiladeemergencia.app.util.Formatos.quantidade(item.quantidade),
                        unidade = item.unidade,
                        validadeEpochDay = item.validadeEpochDay,
                        local = item.local,
                        observacao = item.observacao,
                        falta = item.marcadoComoFalta,
                        formaConsumo = FormaConsumo.dePersistido(item.formaConsumo),
                        fotoArquivo = item.fotoArquivo
                    )
                    return@launch
                }
            }
            _estado.value = ItemFormUiState(
                carregando = false,
                perfilId = perfil?.id ?: 0L,
                quantidade = "1",
                unidade = "un"
            )
        }
    }

    fun definirNome(valor: String) =
        _estado.update { it.copy(nome = valor, erroNome = null) }

    fun definirCategoria(valor: Categoria) = _estado.update { it.copy(categoria = valor) }

    fun definirQuantidade(valor: String) {
        val filtrado = valor.filter { caractere -> caractere.isDigit() || caractere == ',' || caractere == '.' }
        _estado.update { it.copy(quantidade = filtrado, erroQuantidade = null) }
    }

    fun definirUnidade(valor: String) = _estado.update { it.copy(unidade = valor.take(12)) }

    fun definirValidade(epochDay: Long?) = _estado.update { it.copy(validadeEpochDay = epochDay) }

    fun definirLocal(valor: String) = _estado.update { it.copy(local = valor.take(80)) }

    fun definirObservacao(valor: String) = _estado.update { it.copy(observacao = valor.take(500)) }

    fun definirFalta(valor: Boolean) = _estado.update { it.copy(falta = valor) }

    fun definirFormaConsumo(valor: FormaConsumo) =
        _estado.update { it.copy(formaConsumo = valor) }

    /** Salva a foto escolhida na galeria (Photo Picker) no armazenamento interno. */
    fun definirFotoDeUri(uri: android.net.Uri) {
        viewModelScope.launch {
            val nome = withContext(Dispatchers.IO) {
                br.com.mochiladeemergencia.app.util.Fotos.salvarDeUri(servicos.contexto, uri)
            }
            if (nome != null) {
                _estado.update { it.copy(fotoArquivo = nome, fotoAlterada = true, erroSalvar = null) }
            } else {
                _estado.update { it.copy(erroSalvar = "Não foi possível usar esta imagem.") }
            }
        }
    }

    /** Salva a foto tirada com a câmera do sistema. */
    fun definirFotoDeCamera(arquivoTemporario: java.io.File) {
        viewModelScope.launch {
            val nome = withContext(Dispatchers.IO) {
                br.com.mochiladeemergencia.app.util.Fotos.salvarDeArquivo(
                    servicos.contexto,
                    arquivoTemporario
                )
            }
            arquivoTemporario.delete()
            if (nome != null) {
                _estado.update { it.copy(fotoArquivo = nome, fotoAlterada = true, erroSalvar = null) }
            } else {
                _estado.update { it.copy(erroSalvar = "Não foi possível salvar a foto.") }
            }
        }
    }

    fun removerFoto() {
        _estado.update { it.copy(fotoArquivo = null, fotoAlterada = true) }
    }

    fun salvar() {
        val estadoAtual = _estado.value
        val nome = estadoAtual.nome.trim()
        val quantidade = estadoAtual.quantidade.replace(",", ".").toDoubleOrNull()

        val erroNome = if (nome.isBlank()) "Informe o nome do item." else null
        val erroQuantidade = when {
            estadoAtual.quantidade.isBlank() -> "Informe a quantidade."
            quantidade == null -> "Quantidade inválida."
            quantidade <= 0.0 -> "A quantidade deve ser maior que zero."
            else -> null
        }
        if (erroNome != null || erroQuantidade != null) {
            _estado.update { it.copy(erroNome = erroNome, erroQuantidade = erroQuantidade) }
            return
        }

        viewModelScope.launch {
            try {
                val perfilId = if (estadoAtual.perfilId > 0L) {
                    estadoAtual.perfilId
                } else {
                    fluxoPerfilAtivo(servicos).first()?.id ?: 0L
                }
                if (perfilId <= 0L) {
                    _estado.update { it.copy(erroSalvar = "Nenhum perfil ativo encontrado.") }
                    return@launch
                }
                val base = itemOriginal ?: ItemEntity(perfilId = perfilId, nome = nome)
                val item = base.copy(
                    perfilId = perfilId,
                    nome = nome,
                    categoria = estadoAtual.categoria.name,
                    quantidade = quantidade ?: 1.0,
                    unidade = estadoAtual.unidade.trim().ifBlank { "un" },
                    validadeEpochDay = estadoAtual.validadeEpochDay,
                    local = estadoAtual.local,
                    observacao = estadoAtual.observacao,
                    marcadoComoFalta = estadoAtual.falta,
                    formaConsumo = FormaConsumo.persistir(estadoAtual.formaConsumo),
                    fotoArquivo = estadoAtual.fotoArquivo
                )
                servicos.itemRepository.salvar(item)
                // A foto antiga só é apagada depois de salvar com sucesso.
                val fotoAntiga = itemOriginal?.fotoArquivo
                if (estadoAtual.fotoAlterada && fotoAntiga != null && fotoAntiga != estadoAtual.fotoArquivo) {
                    withContext(Dispatchers.IO) {
                        br.com.mochiladeemergencia.app.util.Fotos.excluir(servicos.contexto, fotoAntiga)
                    }
                }
                _estado.update { it.copy(salvo = true, erroSalvar = null) }
            } catch (erro: IllegalArgumentException) {
                _estado.update { it.copy(erroSalvar = erro.message) }
            } catch (erro: Exception) {
                _estado.update {
                    it.copy(erroSalvar = "Não foi possível salvar o item. Tente novamente.")
                }
            }
        }
    }
}
