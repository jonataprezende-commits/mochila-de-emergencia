package br.com.mochiladeemergencia.app.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mochiladeemergencia.app.data.local.entity.ContatoEntity
import br.com.mochiladeemergencia.app.di.ServicosLocais
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class ContatosUiState(
    val carregando: Boolean = true,
    val perfilId: Long = 0L,
    val perfilNome: String = "",
    val contatos: List<ContatoEntity> = emptyList()
) {
    val vazio: Boolean get() = !carregando && contatos.isEmpty()
}

/** Contatos pessoais de emergência por perfil. */
@OptIn(ExperimentalCoroutinesApi::class)
class ContatosViewModel(private val servicos: ServicosLocais) : ViewModel() {

    private val perfilAtivo = fluxoPerfilAtivo(servicos)
    private val _mensagem = MutableStateFlow<String?>(null)
    val mensagem: StateFlow<String?> = _mensagem.asStateFlow()

    private val contatosDoPerfil = perfilAtivo.flatMapLatest { perfil ->
        if (perfil == null) flowOf(emptyList())
        else servicos.contatoRepository.observar(perfil.id)
    }

    val estado: StateFlow<ContatosUiState> =
        combine(perfilAtivo, contatosDoPerfil) { perfil, contatos ->
            ContatosUiState(
                carregando = false,
                perfilId = perfil?.id ?: 0L,
                perfilNome = perfil?.nome ?: "",
                contatos = contatos
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ContatosUiState()
        )

    fun salvar(
        contatoId: Long,
        nome: String,
        telefone: String,
        parentesco: String,
        observacao: String
    ) {
        viewModelScope.launch {
            try {
                val perfilId = estado.value.perfilId
                if (perfilId <= 0L) {
                    _mensagem.value = "Nenhum perfil ativo encontrado."
                    return@launch
                }
                val base = if (contatoId > 0L) {
                    servicos.contatoRepository.buscar(contatoId)
                        ?: ContatoEntity(perfilId = perfilId, nome = nome, telefone = telefone)
                } else {
                    ContatoEntity(perfilId = perfilId, nome = nome, telefone = telefone)
                }
                servicos.contatoRepository.salvar(
                    base.copy(
                        perfilId = perfilId,
                        nome = nome,
                        telefone = telefone,
                        parentesco = parentesco,
                        observacao = observacao
                    )
                )
                _mensagem.value = if (contatoId > 0L) "Contato atualizado." else "Contato salvo."
            } catch (erro: IllegalArgumentException) {
                _mensagem.value = erro.message
            } catch (erro: Exception) {
                _mensagem.value = "Não foi possível salvar o contato."
            }
        }
    }

    fun excluir(contato: ContatoEntity) {
        viewModelScope.launch {
            servicos.contatoRepository.excluir(contato)
            _mensagem.value = "Contato removido."
        }
    }

    fun consumirMensagem() {
        _mensagem.value = null
    }
}
