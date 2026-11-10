package br.com.mochiladeemergencia.app.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mochiladeemergencia.app.data.local.entity.ItemEntity
import br.com.mochiladeemergencia.app.di.ServicosLocais
import br.com.mochiladeemergencia.app.domain.model.Categoria
import br.com.mochiladeemergencia.app.domain.model.StatusItem
import br.com.mochiladeemergencia.app.util.Formatos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

@Immutable
data class MochilaUiState(
    val carregando: Boolean = true,
    val perfilId: Long = 0L,
    val perfilNome: String = "",
    val itens: List<ItemComStatus> = emptyList(),
    val totalSemFiltro: Int = 0,
    val filtros: FiltrosMochila = FiltrosMochila(),
    val categoriasDisponiveis: List<Categoria> = emptyList()
) {
    val semItens: Boolean get() = totalSemFiltro == 0
    val semResultados: Boolean get() = totalSemFiltro > 0 && itens.isEmpty()
}

/** Lista da mochila com busca e filtros por categoria/status. */
@OptIn(ExperimentalCoroutinesApi::class)
class MochilaViewModel(private val servicos: ServicosLocais) : ViewModel() {

    private val perfilAtivo = fluxoPerfilAtivo(servicos)
    private val configuracoes = servicos.preferencias.configuracoes
    private val filtros = MutableStateFlow(FiltrosMochila())

    private val itensDoPerfil = perfilAtivo.flatMapLatest { perfil ->
        if (perfil == null) flowOf(emptyList())
        else servicos.itemRepository.observar(perfil.id)
    }

    val estado: StateFlow<MochilaUiState> =
        combine(perfilAtivo, itensDoPerfil, filtros, configuracoes) { perfil, itens, filtro, config ->
            val hoje = LocalDate.now()
            val comStatus = itens.map { it.comStatus(config.antecedenciaDias, hoje) }
            val buscaNormalizada = Formatos.normalizarBusca(filtro.busca)
            val filtrados = comStatus.filter { itemComStatus ->
                val combinaBusca = buscaNormalizada.isBlank() ||
                    Formatos.normalizarBusca(itemComStatus.item.nome).contains(buscaNormalizada)
                val combinaCategoria = filtro.categoria == null ||
                    itemComStatus.item.categoria == filtro.categoria.name
                val combinaStatus = filtro.status == null || itemComStatus.status == filtro.status
                combinaBusca && combinaCategoria && combinaStatus
            }
            MochilaUiState(
                carregando = false,
                perfilId = perfil?.id ?: 0L,
                perfilNome = perfil?.nome ?: "",
                itens = filtrados,
                totalSemFiltro = comStatus.size,
                filtros = filtro,
                categoriasDisponiveis = comStatus.map { Categoria.dePersistido(it.item.categoria) }
                    .distinct()
                    .sortedBy { it.rotulo }
            )
        }
            // O cálculo de status/filtros sai da thread principal (listas grandes).
            .flowOn(Dispatchers.Default)
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = MochilaUiState()
            )

    fun definirBusca(texto: String) = filtros.update { it.copy(busca = texto) }

    fun alternarCategoria(categoria: Categoria) = filtros.update {
        it.copy(categoria = if (it.categoria == categoria) null else categoria)
    }

    fun alternarStatus(status: StatusItem) = filtros.update {
        it.copy(status = if (it.status == status) null else status)
    }

    fun limparFiltros() {
        filtros.value = FiltrosMochila()
    }

    fun excluir(item: ItemEntity) {
        viewModelScope.launch {
            // Remove também o arquivo da foto para não deixar lixo no armazenamento interno.
            br.com.mochiladeemergencia.app.util.Fotos.excluir(servicos.contexto, item.fotoArquivo)
            servicos.itemRepository.excluir(item)
        }
    }

    fun marcarComoConsumido(item: ItemEntity) {
        viewModelScope.launch { servicos.itemRepository.marcarComoConsumido(item) }
    }

    fun marcarComoReposto(item: ItemEntity) {
        viewModelScope.launch { servicos.itemRepository.marcarComoReposto(item) }
    }
}
