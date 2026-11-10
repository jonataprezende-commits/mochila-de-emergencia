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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.mochiladeemergencia.app.data.local.entity.ItemEntity
import br.com.mochiladeemergencia.app.domain.model.Categoria
import br.com.mochiladeemergencia.app.ui.components.DialogoConfirmacao
import br.com.mochiladeemergencia.app.ui.components.EstadoVazio
import br.com.mochiladeemergencia.app.ui.components.MiniaturaFoto
import br.com.mochiladeemergencia.app.ui.icones.Icones
import br.com.mochiladeemergencia.app.ui.viewmodel.ItemComStatus
import br.com.mochiladeemergencia.app.ui.viewmodel.ReporViewModel
import br.com.mochiladeemergencia.app.util.Formatos

/** Lista de itens consumidos/substituídos que precisam ser repostos. */
@Composable
fun ReporScreen(
    viewModel: ReporViewModel,
    onIrParaMochila: () -> Unit,
    onEditarItem: (Long) -> Unit
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val mensagem by viewModel.mensagem.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var itemParaExcluir by remember { mutableStateOf<ItemEntity?>(null) }

    LaunchedEffect(mensagem) {
        mensagem?.let {
            snackbar.showSnackbar(it)
            viewModel.consumirMensagem()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (estado.vazio) {
            EstadoVazio(
                icone = Icones.Repor,
                titulo = "Nada para repor",
                descricao = "Quando você marcar um item como consumido ou substituído " +
                    "(na mochila ou na conferência), ele aparece aqui para ser comprado de novo.",
                modifier = Modifier.fillMaxSize(),
                acao = { Button(onClick = onIrParaMochila) { Text("Ir para Minha Mochila") } }
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Text(
                            text = "${estado.total} item(ns) para repor no perfil " +
                                estado.perfilNome.ifBlank { "Casa" } +
                                ". Ao repor, o item volta para a mochila com status \"Tenho\".",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
                items(
                    items = estado.itens,
                    key = { it.item.id },
                    contentType = { "item" }
                ) { item ->
                    CartaoRepor(
                        itemComStatus = item,
                        onRepor = { viewModel.repor(item.item.id) },
                        onEditar = { onEditarItem(item.item.id) },
                        onExcluir = { itemParaExcluir = item.item },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }

    itemParaExcluir?.let { item ->
        DialogoConfirmacao(
            titulo = "Excluir item?",
            texto = "\"${item.nome}\" será removido definitivamente da mochila.",
            rotuloConfirmar = "Excluir",
            destrutivo = true,
            onConfirmar = {
                viewModel.excluir(item.id)
                itemParaExcluir = null
            },
            onCancelar = { itemParaExcluir = null }
        )
    }
}

@Composable
private fun CartaoRepor(
    itemComStatus: ItemComStatus,
    onRepor: () -> Unit,
    onEditar: () -> Unit,
    onExcluir: () -> Unit,
    modifier: Modifier = Modifier
) {
    val item = itemComStatus.item
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiniaturaFoto(
                    nomeArquivo = item.fotoArquivo,
                    descricao = "Foto de ${item.nome}",
                    tamanho = 48.dp
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.nome, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${Categoria.dePersistido(item.categoria).rotulo} · " +
                            Formatos.quantidadeComUnidade(item.quantidade, item.unidade),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.local.isNotBlank()) {
                        Text(
                            text = "Local: ${item.local}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onRepor) {
                    Icon(Icones.Repor, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Repor")
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onEditar) { Text("Editar") }
                IconButton(onClick = onExcluir) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Excluir ${item.nome}",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
