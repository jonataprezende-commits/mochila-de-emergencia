package br.com.mochiladeemergencia.app.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mochiladeemergencia.app.data.local.entity.ItemEntity
import br.com.mochiladeemergencia.app.di.ServicosLocais
import br.com.mochiladeemergencia.app.domain.model.Categoria
import br.com.mochiladeemergencia.app.domain.model.StatusItem
import br.com.mochiladeemergencia.app.util.Fotos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

// --------------------------------- Conferir ---------------------------------

@Immutable
data class ConferirUiState(
    val carregando: Boolean = true,
    val perfilNome: String = "",
    val itens: List<ItemComStatus> = emptyList(),
    val indiceAtual: Int = 0,
    val concluido: Boolean = false,
    val contagemTenho: Int = 0,
    val contagemFalta: Int = 0,
    val contagemSubstituir: Int = 0
) {
    val total: Int get() = itens.size
    val atual: ItemComStatus? get() = itens.getOrNull(indiceAtual)
    val progresso: Float
        get() = if (total == 0) 0f else (indiceAtual.toFloat() / total).coerceIn(0f, 1f)
    val vazio: Boolean get() = !carregando && total == 0
}

/**
 * Auditoria "Conferir Mochila": percorre os itens um a um aplicando Tenho / Falta / Substituir.
 * Cada resposta é gravada imediatamente no banco (rotacionar a tela não perde a auditoria).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConferirViewModel(private val servicos: ServicosLocais) : ViewModel() {

    private val perfilAtivo = fluxoPerfilAtivo(servicos)
    private val configuracoes = servicos.preferencias.configuracoes
    private val indice = MutableStateFlow(0)
    private val concluido = MutableStateFlow(false)
    private val respostas = MutableStateFlow(Contagens())

    private data class Contagens(val tenho: Int = 0, val falta: Int = 0, val substituir: Int = 0)

    private val itensDoPerfil = perfilAtivo.flatMapLatest { perfil ->
        if (perfil == null) flowOf(emptyList())
        else servicos.itemRepository.observar(perfil.id)
    }

    /** Lista fixa do início da auditoria: evita que o item "pule" ao mudar de status. */
    private val listaDaAuditoria = MutableStateFlow<List<ItemEntity>?>(null)

    /** Instantâneo do andamento; a lista congelada entra no fluxo para reemitir quando ela muda. */
    private data class Andamento(
        val indice: Int,
        val concluido: Boolean,
        val contagens: Contagens,
        val lista: List<ItemEntity>?
    )

    /** Agrupa o progresso da auditoria em um único fluxo (combine aceita até 5). */
    private val progresso = combine(indice, concluido, respostas, listaDaAuditoria) {
            indiceAtual, finalizado, contagens, lista ->
        Andamento(indiceAtual, finalizado, contagens, lista)
    }

    val estado: StateFlow<ConferirUiState> = combine(
        perfilAtivo,
        itensDoPerfil,
        configuracoes,
        progresso
    ) { perfil, itensBanco, config, andamento ->
        val indiceAtual = andamento.indice
        val finalizado = andamento.concluido
        val contagens = andamento.contagens
        // Congelada durante a auditoria: "Substituir" zera a validade e reordenaria a lista viva,
        // fazendo o usuário pular itens.
        val base = andamento.lista ?: itensBanco
        val hoje = LocalDate.now()
        ConferirUiState(
            carregando = false,
            perfilNome = perfil?.nome ?: "",
            itens = base.map { it.comStatus(config.antecedenciaDias, hoje) },
            indiceAtual = indiceAtual.coerceIn(0, base.size),
            concluido = finalizado,
            contagemTenho = contagens.tenho,
            contagemFalta = contagens.falta,
            contagemSubstituir = contagens.substituir
        )
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ConferirUiState()
    )

    /** Congela a lista atual para a auditoria desta sessão. */
    fun iniciar() {
        if (listaDaAuditoria.value != null) return
        viewModelScope.launch {
            indice.value = 0
            concluido.value = false
            respostas.value = Contagens()
            val perfil = perfilAtivo.first()
            listaDaAuditoria.value = perfil?.let { servicos.itemRepository.listar(it.id) } ?: emptyList()
        }
    }

    fun responder(resultado: StatusItem) {
        val atual = estado.value.atual ?: return
        viewModelScope.launch {
            servicos.itemRepository.aplicarConferencia(atual.item, resultado)
            respostas.update {
                when (resultado) {
                    StatusItem.TENHO -> it.copy(tenho = it.tenho + 1)
                    StatusItem.FALTA -> it.copy(falta = it.falta + 1)
                    else -> it.copy(substituir = it.substituir + 1)
                }
            }
            val proximo = indice.value + 1
            // Avança até o total para a barra chegar a 100% antes do resumo final.
            indice.value = proximo
            if (proximo >= estado.value.total) {
                concluido.value = true
            }
        }
    }

    /** Reinicia a auditoria com a lista atual do banco. */
    fun reiniciar() {
        listaDaAuditoria.value = null
        concluido.value = false
        indice.value = 0
        respostas.value = Contagens()
        iniciar()
    }
}

// ------------------------------- Repor ---------------------------------

@Immutable
data class ReporUiState(
    val carregando: Boolean = true,
    val perfilId: Long = 0L,
    val perfilNome: String = "",
    val itens: List<ItemComStatus> = emptyList(),
    val mensagem: String? = null
) {
    val total: Int get() = itens.size
    val vazio: Boolean get() = !carregando && itens.isEmpty()
}

/** Itens consumidos/substituídos que aguardam reposição. */
@OptIn(ExperimentalCoroutinesApi::class)
class ReporViewModel(private val servicos: ServicosLocais) : ViewModel() {

    private val perfilAtivo = fluxoPerfilAtivo(servicos)
    private val configuracoes = servicos.preferencias.configuracoes
    private val _mensagem = MutableStateFlow<String?>(null)
    val mensagem: StateFlow<String?> = _mensagem.asStateFlow()

    private val itensDoPerfil = perfilAtivo.flatMapLatest { perfil ->
        if (perfil == null) flowOf(emptyList())
        else servicos.itemRepository.observar(perfil.id)
    }

    val estado: StateFlow<ReporUiState> =
        combine(perfilAtivo, itensDoPerfil, configuracoes) { perfil, itens, config ->
            val hoje = LocalDate.now()
            ReporUiState(
                carregando = false,
                perfilId = perfil?.id ?: 0L,
                perfilNome = perfil?.nome ?: "",
                itens = itens.filter { it.aRepor }
                    .map { it.comStatus(config.antecedenciaDias, hoje) }
                    .sortedBy { Categoria.dePersistido(it.item.categoria).ordinal }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ReporUiState()
        )

    /** Devolve o item à mochila com status "Tenho". */
    fun repor(itemId: Long) {
        viewModelScope.launch {
            val item = servicos.itemRepository.buscar(itemId) ?: return@launch
            servicos.itemRepository.repor(item)
            _mensagem.value = "\"${item.nome}\" voltou para a mochila (Tenho)."
        }
    }

    /** Remove o item definitivamente (com confirmação na tela). */
    fun excluir(itemId: Long) {
        viewModelScope.launch {
            val item = servicos.itemRepository.buscar(itemId) ?: return@launch
            Fotos.excluir(servicos.contexto, item.fotoArquivo)
            servicos.itemRepository.excluir(item)
            _mensagem.value = "Item removido."
        }
    }

    fun consumirMensagem() {
        _mensagem.value = null
    }
}
