package br.com.mochiladeemergencia.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.mochiladeemergencia.app.data.local.entity.PerfilEntity
import br.com.mochiladeemergencia.app.ui.components.DialogoConfirmacao
import br.com.mochiladeemergencia.app.ui.viewmodel.ConfiguracoesViewModel

@Composable
fun PerfisScreen(viewModel: ConfiguracoesViewModel) {
    val perfis by viewModel.perfis.collectAsStateWithLifecycle()
    val perfilAtivo by viewModel.perfilAtivo.collectAsStateWithLifecycle()

    var mostrarNovoPerfil by remember { mutableStateOf(false) }
    var perfilParaRenomear by remember { mutableStateOf<PerfilEntity?>(null) }
    var perfilParaExcluir by remember { mutableStateOf<PerfilEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Text(
                    text = "Cada perfil tem a sua própria mochila, o seu checklist e os seus " +
                        "documentos. Use perfis para separar a casa, a família ou outro local.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        items(items = perfis, key = { it.id }) { perfil ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (perfil.id == perfilAtivo?.id) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.ativarPerfil(perfil.id) }
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(perfil.nome, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (perfil.id == perfilAtivo?.id) "Perfil ativo" else "Toque para ativar",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (perfil.id == perfilAtivo?.id) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Perfil ativo",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = { perfilParaRenomear = perfil }) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Renomear perfil ${perfil.nome}",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = { perfilParaExcluir = perfil }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Excluir perfil ${perfil.nome}",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = { mostrarNovoPerfil = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Novo perfil")
            }
        }
    }

    if (mostrarNovoPerfil) {
        DialogoNomePerfil(
            titulo = "Novo perfil",
            valorInicial = "",
            onSalvar = { nome ->
                viewModel.criarPerfil(nome) { }
                mostrarNovoPerfil = false
            },
            onFechar = { mostrarNovoPerfil = false }
        )
    }

    perfilParaRenomear?.let { perfil ->
        DialogoNomePerfil(
            titulo = "Renomear perfil",
            valorInicial = perfil.nome,
            onSalvar = { nome ->
                viewModel.renomearPerfil(perfil, nome)
                perfilParaRenomear = null
            },
            onFechar = { perfilParaRenomear = null }
        )
    }

    perfilParaExcluir?.let { perfil ->
        DialogoConfirmacao(
            titulo = "Excluir perfil?",
            texto = "Todos os itens, o checklist e os documentos de \"${perfil.nome}\" serão " +
                "apagados do aparelho. Essa ação não pode ser desfeita.",
            rotuloConfirmar = "Excluir",
            destrutivo = true,
            onConfirmar = {
                viewModel.excluirPerfil(perfil)
                perfilParaExcluir = null
            },
            onCancelar = { perfilParaExcluir = null }
        )
    }
}

@Composable
private fun DialogoNomePerfil(
    titulo: String,
    valorInicial: String,
    onSalvar: (String) -> Unit,
    onFechar: () -> Unit
) {
    var nome by remember { mutableStateOf(valorInicial) }
    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text(titulo) },
        text = {
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it.take(60) },
                label = { Text("Nome do perfil") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onSalvar(nome) }) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onFechar) { Text("Cancelar") }
        }
    )
}
