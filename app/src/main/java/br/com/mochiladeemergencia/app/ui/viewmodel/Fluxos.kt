package br.com.mochiladeemergencia.app.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.com.mochiladeemergencia.app.data.local.entity.ItemEntity
import br.com.mochiladeemergencia.app.data.local.entity.PerfilEntity
import br.com.mochiladeemergencia.app.di.ServicosLocais
import br.com.mochiladeemergencia.app.domain.StatusCalculadora
import br.com.mochiladeemergencia.app.domain.model.Categoria
import br.com.mochiladeemergencia.app.domain.model.StatusItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** Item já com o status calculado conforme a antecedência configurada. */
@Immutable
data class ItemComStatus(
    val item: ItemEntity,
    val status: StatusItem,
    val diasRestantes: Int?
)

/** Filtros aplicados na tela Minha Mochila. */
data class FiltrosMochila(
    val busca: String = "",
    val categoria: Categoria? = null,
    val status: StatusItem? = null
) {
    val vazio: Boolean get() = busca.isBlank() && categoria == null && status == null
}

/**
 * Perfil ativo: o escolhido nas preferências ou, na falta dele, o primeiro cadastrado.
 *
 * `distinctUntilChanged` é essencial para a fluidez: sem ele, mudar qualquer preferência
 * (tema, notificações, número de pessoas) fazia o perfil "reemitir" e todos os
 * `flatMapLatest` dos ViewModels reabriam as consultas do Room do zero.
 */
fun fluxoPerfilAtivo(servicos: ServicosLocais): Flow<PerfilEntity?> =
    combine(servicos.perfilRepository.perfis, servicos.preferencias.configuracoes) { perfis, config ->
        perfis.firstOrNull { it.id == config.perfilAtivoId } ?: perfis.firstOrNull()
    }.distinctUntilChanged()

fun ItemEntity.comStatus(antecedenciaDias: Int, hoje: java.time.LocalDate = java.time.LocalDate.now()) =
    ItemComStatus(
        item = this,
        status = StatusCalculadora.status(
            validadeEpochDay = validadeEpochDay,
            marcadoComoFalta = marcadoComoFalta,
            antecedenciaDias = antecedenciaDias,
            hoje = hoje,
            aRepor = aRepor
        ),
        diasRestantes = StatusCalculadora.diasAteVencer(validadeEpochDay, hoje)
    )

/** Fábrica simples de ViewModels, ligada ao [ServicosLocais]. */
class FabricaViewModel(private val servicos: ServicosLocais) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(InicioViewModel::class.java) -> InicioViewModel(servicos) as T
        modelClass.isAssignableFrom(MochilaViewModel::class.java) -> MochilaViewModel(servicos) as T
        modelClass.isAssignableFrom(ItemFormViewModel::class.java) -> ItemFormViewModel(servicos) as T
        modelClass.isAssignableFrom(ChecklistViewModel::class.java) -> ChecklistViewModel(servicos) as T
        modelClass.isAssignableFrom(DocumentosViewModel::class.java) -> DocumentosViewModel(servicos) as T
        modelClass.isAssignableFrom(DocumentoFormViewModel::class.java) -> DocumentoFormViewModel(servicos) as T
        modelClass.isAssignableFrom(ConfiguracoesViewModel::class.java) -> ConfiguracoesViewModel(servicos) as T
        modelClass.isAssignableFrom(ConferirViewModel::class.java) -> ConferirViewModel(servicos) as T
        modelClass.isAssignableFrom(ReporViewModel::class.java) -> ReporViewModel(servicos) as T
        modelClass.isAssignableFrom(ContatosViewModel::class.java) -> ContatosViewModel(servicos) as T
        else -> throw IllegalArgumentException("ViewModel não registrado: ${modelClass.name}")
    }
}
