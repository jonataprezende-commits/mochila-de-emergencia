package br.com.mochiladeemergencia.app.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mochiladeemergencia.app.data.local.entity.ChecklistEntity
import br.com.mochiladeemergencia.app.data.local.entity.ContatoEntity
import br.com.mochiladeemergencia.app.data.local.entity.DocumentoEntity
import br.com.mochiladeemergencia.app.data.local.entity.ItemEntity
import br.com.mochiladeemergencia.app.data.local.entity.PerfilEntity
import br.com.mochiladeemergencia.app.data.prefs.Configuracoes
import br.com.mochiladeemergencia.app.di.ServicosLocais
import br.com.mochiladeemergencia.app.domain.StatusCalculadora
import br.com.mochiladeemergencia.app.domain.model.StatusItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@Immutable
data class InicioUiState(
    val carregando: Boolean = true,
    val nomePerfil: String = "",
    val localizacao: String = "",
    val totalItens: Int = 0,
    val faltando: Int = 0,
    val aRepor: Int = 0,
    val contatos: Int = 0,
    val vencendo: Int = 0,
    val vencidos: Int = 0,
    val documentos: Int = 0,
    val antecedenciaDias: Int = StatusCalculadora.ANTECEDENCIA_PADRAO,
    val notificacoesAtivas: Boolean = true,
    val pessoasCasa: Int = 1,
    val checklistTotal: Int = 0,
    val checklistAdicionados: Int = 0,
    val proximosVencimentos: List<ItemComStatus> = emptyList()
) {
    val mochilaVazia: Boolean get() = totalItens == 0
    val percentualChecklist: Float
        get() = if (checklistTotal == 0) 0f
        else (checklistAdicionados.toFloat() / checklistTotal).coerceIn(0f, 1f)
}

/** Dados do painel inicial. */
@OptIn(ExperimentalCoroutinesApi::class)
class InicioViewModel(private val servicos: ServicosLocais) : ViewModel() {

    private val perfilAtivo = fluxoPerfilAtivo(servicos)
    private val configuracoes = servicos.preferencias.configuracoes

    private val itens = perfilAtivo.flatMapLatest { perfil ->
        if (perfil == null) flowOf(emptyList())
        else servicos.itemRepository.observar(perfil.id)
    }

    private val documentos = perfilAtivo.flatMapLatest { perfil ->
        if (perfil == null) flowOf(emptyList())
        else servicos.documentoRepository.observar(perfil.id)
    }

    private val sugestoes = perfilAtivo.flatMapLatest { perfil ->
        if (perfil == null) flowOf(emptyList())
        else servicos.checklistRepository.observar(perfil.id)
    }

    private val contatos = perfilAtivo.flatMapLatest { perfil ->
        if (perfil == null) flowOf(emptyList())
        else servicos.contatoRepository.observar(perfil.id)
    }

    // `combine` aceita até 5 fluxos: sugestões e contatos são agrupados em um par.
    private val checklistEContatos = combine(sugestoes, contatos) { listaSugestoes, listaContatos ->
        listaSugestoes to listaContatos
    }

    val estado: StateFlow<InicioUiState> = combine(
        perfilAtivo,
        itens,
        documentos,
        checklistEContatos,
        configuracoes
    ) { perfil, listaItens, listaDocs, sugestoesEContatos, config ->
        calcular(
            perfil = perfil,
            itens = listaItens,
            documentos = listaDocs,
            sugestoes = sugestoesEContatos.first,
            contatos = sugestoesEContatos.second,
            config = config
        )
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InicioUiState()
    )

    /** Salva a localização da mochila (editável no card de destaque do painel). */
    fun salvarLocalizacao(texto: String) {
        viewModelScope.launch {
            val perfil = perfilAtivo.first() ?: return@launch
            servicos.perfilRepository.definirLocalizacao(perfil, texto)
        }
    }

    private fun calcular(
        perfil: PerfilEntity?,
        itens: List<ItemEntity>,
        documentos: List<DocumentoEntity>,
        sugestoes: List<ChecklistEntity>,
        contatos: List<ContatoEntity>,
        config: Configuracoes
    ): InicioUiState {
        val hoje = LocalDate.now()
        val comStatus = itens.map { it.comStatus(config.antecedenciaDias, hoje) }
        val chavesNaMochila = itens.mapNotNull { it.chaveChecklist }.toSet()
        return InicioUiState(
            carregando = false,
            nomePerfil = perfil?.nome ?: "",
            localizacao = perfil?.localizacao ?: "",
            totalItens = comStatus.size,
            faltando = comStatus.count { it.item.marcadoComoFalta },
            aRepor = comStatus.count { it.item.aRepor },
            contatos = contatos.size,
            vencendo = comStatus.count { it.status == StatusItem.VENCENDO },
            vencidos = comStatus.count { it.status == StatusItem.VENCIDO },
            documentos = documentos.size,
            antecedenciaDias = config.antecedenciaDias,
            notificacoesAtivas = config.notificacoesAtivas,
            pessoasCasa = config.pessoasCasa,
            checklistTotal = sugestoes.size,
            checklistAdicionados = sugestoes.count { it.chave in chavesNaMochila },
            proximosVencimentos = comStatus
                .filter { it.item.validadeEpochDay != null && !it.item.aRepor }
                .sortedBy { it.item.validadeEpochDay }
                .take(5)
        )
    }
}
