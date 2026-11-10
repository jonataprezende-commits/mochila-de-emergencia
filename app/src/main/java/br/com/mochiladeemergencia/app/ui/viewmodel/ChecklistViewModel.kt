package br.com.mochiladeemergencia.app.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mochiladeemergencia.app.data.local.entity.ChecklistEntity
import br.com.mochiladeemergencia.app.di.ServicosLocais
import br.com.mochiladeemergencia.app.domain.model.Categoria
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Filtro de exibição do checklist. */
enum class FiltroChecklist(val rotulo: String) {
    TODOS("Tudo"),
    FALTANDO("Só o que falta"),
    ADICIONADOS("Já na mochila")
}

@Immutable
data class ChecklistComEstado(
    val sugestao: ChecklistEntity,
    val adicionado: Boolean,
    /** Dica de quantidade aproximada (só quando o ajuste por pessoas está ligado). */
    val dicaQuantidade: String? = null
)

/** Um grupo de sugestões da mesma categoria, na ordem de exibição. */
@Immutable
data class GrupoChecklist(
    val categoria: Categoria,
    val itens: List<ChecklistComEstado>
) {
    val total: Int get() = itens.size
    val adicionados: Int get() = itens.count { it.adicionado }
    val faltando: Int get() = total - adicionados
    val completo: Boolean get() = total > 0 && adicionados == total
}

@Immutable
data class ChecklistUiState(
    val carregando: Boolean = true,
    val perfilId: Long = 0L,
    val perfilNome: String = "",
    val grupos: List<GrupoChecklist> = emptyList(),
    val filtro: FiltroChecklist = FiltroChecklist.TODOS,
    val pessoasCasa: Int = 1,
    val ajustarPorPessoas: Boolean = false,
    /** Totais do checklist inteiro, independentes do filtro selecionado. */
    val total: Int = 0,
    val adicionados: Int = 0
) {
    val todosItens: List<ChecklistComEstado> get() = grupos.flatMap { it.itens }
    val faltando: Int get() = (total - adicionados).coerceAtLeast(0)
    val progresso: Float get() = if (total == 0) 0f else adicionados.toFloat() / total
    val checklistVazio: Boolean get() = grupos.isEmpty()
    val temDicas: Boolean get() = todosItens.any { it.sugestao.dica.isNotBlank() }
}

/**
 * Checklist editável por perfil, agrupado por categoria.
 *
 * As sugestões são apenas sugestões: qualquer uma pode ser editada, removida ou
 * substituída por itens personalizados.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChecklistViewModel(private val servicos: ServicosLocais) : ViewModel() {

    private val perfilAtivo = fluxoPerfilAtivo(servicos)
    private val configuracoes = servicos.preferencias.configuracoes
    private val ajustar = MutableStateFlow(false)
    private val filtro = MutableStateFlow(FiltroChecklist.TODOS)
    private val precisaSincronizar = MutableStateFlow(true)

    private val _mensagem = MutableStateFlow<String?>(null)
    val mensagem: StateFlow<String?> = _mensagem.asStateFlow()

    private val sugestoes = perfilAtivo.flatMapLatest { perfil ->
        if (perfil == null) {
            flowOf(emptyList())
        } else {
            if (precisaSincronizar.value) {
                precisaSincronizar.value = false
                // Sincroniza as sugestões padrão com a versão atual do app (idempotente).
                viewModelScope.launch { servicos.checklistRepository.sincronizarPadroes(perfil.id) }
            }
            servicos.checklistRepository.observar(perfil.id)
        }
    }

    private val itensDaMochila = perfilAtivo.flatMapLatest { perfil ->
        if (perfil == null) flowOf(emptyList())
        else servicos.itemRepository.observar(perfil.id)
    }

    val estado: StateFlow<ChecklistUiState> = combine(
        perfilAtivo,
        sugestoes,
        itensDaMochila,
        configuracoes,
        combine(ajustar, filtro) { ajustarPorPessoas, filtroAtual -> ajustarPorPessoas to filtroAtual }
    ) { perfil, listaSugestoes, itens, config, opcoes ->
        val (ajustarPorPessoas, filtroAtual) = opcoes
        if (perfil != null && listaSugestoes.isEmpty()) {
            // Primeiro acesso do perfil (ou perfil restaurado): recria o checklist padrão.
            viewModelScope.launch {
                servicos.checklistRepository.sincronizarPadroes(perfil.id)
                precisaSincronizar.value = false
            }
        }
        val chavesAdicionadas = itens.mapNotNull { it.chaveChecklist }.toSet()

        val comEstado = listaSugestoes.map { sugestao ->
            ChecklistComEstado(
                sugestao = sugestao,
                adicionado = sugestao.chave in chavesAdicionadas,
                dicaQuantidade = servicos.checklistRepository.dicaDeQuantidade(
                    sugestao = sugestao,
                    pessoasCasa = config.pessoasCasa,
                    ajustarPorPessoas = ajustarPorPessoas
                )
            )
        }.sortedWith(
            compareBy<ChecklistComEstado> { it.sugestao.personalizado }
                .thenBy { it.sugestao.ordem }
                .thenBy { it.sugestao.nome.lowercase() }
        )

        val gruposCompletos = comEstado
            .groupBy { Categoria.dePersistido(it.sugestao.categoria) }
            .toList()
            .sortedWith(
                compareBy<Pair<Categoria, List<ChecklistComEstado>>> {
                    val indice = Categoria.entries.indexOf(it.first)
                    if (indice < 0) Int.MAX_VALUE else indice
                }
            )
            .map { (categoria, itensDoGrupo) ->
                GrupoChecklist(categoria = categoria, itens = itensDoGrupo)
            }

        ChecklistUiState(
            carregando = false,
            perfilId = perfil?.id ?: 0L,
            perfilNome = perfil?.nome ?: "",
            grupos = gruposCompletos.map { grupo ->
                grupo.copy(
                    itens = grupo.itens.filter { item ->
                        when (filtroAtual) {
                            FiltroChecklist.TODOS -> true
                            FiltroChecklist.FALTANDO -> !item.adicionado
                            FiltroChecklist.ADICIONADOS -> item.adicionado
                        }
                    }
                )
            }.filter { it.itens.isNotEmpty() },
            filtro = filtroAtual,
            pessoasCasa = config.pessoasCasa,
            ajustarPorPessoas = ajustarPorPessoas,
            total = comEstado.size,
            adicionados = comEstado.count { it.adicionado }
        )
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChecklistUiState()
    )

    fun alternarAjustePorPessoas(valor: Boolean) {
        ajustar.value = valor
    }

    fun definirFiltro(valor: FiltroChecklist) {
        filtro.value = valor
    }

    fun adicionarAMochila(item: ChecklistComEstado) {
        viewModelScope.launch {
            val estadoAtual = estado.value
            if (estadoAtual.perfilId <= 0L) return@launch
            val adicionado = servicos.checklistRepository.adicionarAMochila(
                perfilId = estadoAtual.perfilId,
                sugestao = item.sugestao
            )
            _mensagem.value = if (adicionado) {
                "\"${item.sugestao.nome}\" adicionado à mochila. Ajuste a quantidade em Mochila."
            } else {
                "Este item já está na sua mochila."
            }
        }
    }

    fun criarSugestao(
        nome: String,
        categoria: Categoria,
        quantidadeBase: Double,
        unidade: String,
        dica: String = ""
    ) {
        val nomeLimpo = nome.trim()
        if (nomeLimpo.isBlank()) {
            _mensagem.value = "Informe o nome da sugestão."
            return
        }
        viewModelScope.launch {
            val perfilId = estado.value.perfilId
            if (perfilId <= 0L) return@launch
            servicos.checklistRepository.criarSugestao(
                perfilId = perfilId,
                nome = nomeLimpo,
                categoria = categoria,
                quantidadeBase = quantidadeBase.takeIf { it > 0.0 } ?: 1.0,
                unidade = unidade,
                dica = dica
            )
            _mensagem.value = "Sugestão adicionada ao checklist."
        }
    }

    fun atualizarSugestao(
        sugestao: ChecklistEntity,
        nome: String,
        categoria: Categoria,
        quantidadeBase: Double,
        unidade: String,
        dica: String
    ) {
        val nomeLimpo = nome.trim()
        if (nomeLimpo.isBlank()) {
            _mensagem.value = "Informe o nome da sugestão."
            return
        }
        viewModelScope.launch {
            servicos.checklistRepository.atualizar(
                sugestao.copy(
                    nome = nomeLimpo.take(80),
                    categoria = categoria.name,
                    quantidadeBase = quantidadeBase.takeIf { it > 0.0 } ?: 1.0,
                    unidade = unidade.trim().take(12).ifBlank { "un" },
                    dica = dica.trim().take(160)
                )
            )
            _mensagem.value = "Sugestão atualizada."
        }
    }

    fun excluirSugestao(sugestao: ChecklistEntity) {
        viewModelScope.launch {
            servicos.checklistRepository.excluir(sugestao)
            _mensagem.value = "Sugestão removida do checklist."
        }
    }

    fun restaurarPadroes() {
        viewModelScope.launch {
            val perfilId = estado.value.perfilId
            if (perfilId <= 0L) return@launch
            servicos.checklistRepository.restaurarPadroes(perfilId)
            _mensagem.value = "Checklist padrão restaurado."
        }
    }

    fun consumirMensagem() {
        _mensagem.value = null
    }
}
