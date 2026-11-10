package br.com.mochiladeemergencia.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.mochiladeemergencia.app.ui.icones.Icones
import br.com.mochiladeemergencia.app.data.local.entity.ItemEntity
import br.com.mochiladeemergencia.app.domain.StatusCalculadora
import br.com.mochiladeemergencia.app.domain.model.Categoria
import br.com.mochiladeemergencia.app.domain.model.FormaConsumo
import br.com.mochiladeemergencia.app.domain.model.StatusItem
import br.com.mochiladeemergencia.app.ui.components.DialogoConfirmacao
import br.com.mochiladeemergencia.app.ui.components.EstadoVazio
import br.com.mochiladeemergencia.app.ui.components.MiniaturaFoto
import br.com.mochiladeemergencia.app.ui.components.SeloStatus
import br.com.mochiladeemergencia.app.ui.theme.LocalCoresStatus
import br.com.mochiladeemergencia.app.ui.viewmodel.ItemComStatus
import br.com.mochiladeemergencia.app.ui.viewmodel.MochilaViewModel
import br.com.mochiladeemergencia.app.util.Formatos

@Composable
fun MochilaScreen(
    viewModel: MochilaViewModel,
    onEditar: (Long) -> Unit,
    onAdicionar: () -> Unit,
    onAbrirChecklist: () -> Unit
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var itemParaExcluir by remember { mutableStateOf<ItemEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = estado.filtros.busca,
            onValueChange = viewModel::definirBusca,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            label = { Text("Buscar item") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (estado.filtros.busca.isNotBlank()) {
                    IconButton(onClick = { viewModel.definirBusca("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Limpar busca")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categorias = listOf<Categoria?>(null) + estado.categoriasDisponiveis
            items(items = categorias, key = { it?.name ?: "todas" }) { categoria ->
                FilterChip(
                    selected = estado.filtros.categoria == categoria,
                    onClick = {
                        if (categoria == null) viewModel.limparFiltros()
                        else viewModel.alternarCategoria(categoria)
                    },
                    label = { Text(categoria?.rotulo ?: "Todas") }
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = StatusItem.entries.toList(), key = { it.name }) { status ->
                FilterChip(
                    selected = estado.filtros.status == status,
                    onClick = { viewModel.alternarStatus(status) },
                    label = { Text(status.rotulo) }
                )
            }
            item {
                FilterChip(
                    selected = estado.filtros.vazio,
                    onClick = viewModel::limparFiltros,
                    label = { Text("Limpar filtros") }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        when {
            estado.carregando -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            estado.semItens -> {
                EstadoVazio(
                    icone = Icones.Mochila,
                    titulo = "Nenhum item cadastrado",
                    descricao = "Adicione itens manualmente ou use o checklist com sugestões prontas para começar.",
                    acao = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Button(onClick = onAdicionar) { Text("Adicionar item") }
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = onAbrirChecklist) { Text("Usar checklist") }
                        }
                    }
                )
            }

            estado.semResultados -> {
                EstadoVazio(
                    icone = Icons.Filled.Search,
                    titulo = "Nenhum resultado",
                    descricao = "Nenhum item corresponde à busca e aos filtros escolhidos.",
                    acao = {
                        Button(onClick = viewModel::limparFiltros) { Text("Limpar filtros") }
                    }
                )
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "${estado.itens.size} de ${estado.totalSemFiltro} itens",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(
                        items = estado.itens,
                        key = { it.item.id },
                        contentType = { "item" }
                    ) { item ->
                        CartaoItemMochila(
                            itemComStatus = item,
                            onEditar = { onEditar(item.item.id) },
                            onExcluir = { itemParaExcluir = item.item },
                            onAlternarFalta = {
                                if (item.item.marcadoComoFalta) {
                                    viewModel.marcarComoReposto(item.item)
                                } else {
                                    viewModel.marcarComoConsumido(item.item)
                                }
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }

    itemParaExcluir?.let { item ->
        DialogoConfirmacao(
            titulo = "Excluir item?",
            texto = "O item \"${item.nome}\" será removido da mochila. Essa ação não pode ser desfeita.",
            rotuloConfirmar = "Excluir",
            destrutivo = true,
            onConfirmar = {
                viewModel.excluir(item)
                itemParaExcluir = null
            },
            onCancelar = { itemParaExcluir = null }
        )
    }
}

@Composable
private fun CartaoItemMochila(
    itemComStatus: ItemComStatus,
    onEditar: () -> Unit,
    onExcluir: () -> Unit,
    onAlternarFalta: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cores = LocalCoresStatus.current
    val item = itemComStatus.item
    val status = itemComStatus.status
    val categoria = Categoria.dePersistido(item.categoria)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = cores.container(status),
            contentColor = cores.conteudo(status)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 14.dp, bottom = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiniaturaFoto(
                    nomeArquivo = item.fotoArquivo,
                    descricao = if (item.fotoArquivo != null) {
                        "Foto de ${item.nome}"
                    } else {
                        "${item.nome} sem foto"
                    },
                    tamanho = 44.dp
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.nome, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = categoria.rotulo,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                SeloStatus(status)
                Spacer(Modifier.width(6.dp))
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = buildString {
                    append(Formatos.quantidadeComUnidade(item.quantidade, item.unidade))
                    val validade = Formatos.data(item.validadeEpochDay)
                    if (validade.isNotBlank()) {
                        append(" · Validade: $validade")
                    } else {
                        append(" · Sem validade")
                    }
                    if (item.formaConsumo.isNotBlank()) {
                        append(" · ${FormaConsumo.dePersistido(item.formaConsumo).rotulo}")
                    }
                    if (item.local.isNotBlank()) append(" · Local: ${item.local}")
                },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = StatusCalculadora.descreverDias(itemComStatus.diasRestantes),
                style = MaterialTheme.typography.bodySmall
            )
            if (item.observacao.isNotBlank()) {
                Text(text = item.observacao, style = MaterialTheme.typography.bodySmall)
            }

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onAlternarFalta) {
                    Icon(
                        imageVector = if (item.marcadoComoFalta) Icons.Filled.Refresh else Icones.Repor,
                        contentDescription = if (item.marcadoComoFalta) {
                            "Marcar ${item.nome} como disponível"
                        } else {
                            "Marcar ${item.nome} como consumido ou substituído (vai para Repor)"
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = onEditar) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Editar ${item.nome}",
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = onExcluir) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Excluir ${item.nome}",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
