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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.mochiladeemergencia.app.data.local.entity.ContatoEntity
import br.com.mochiladeemergencia.app.data.repository.ContatosEmergenciaFixos
import br.com.mochiladeemergencia.app.ui.components.DialogoConfirmacao
import br.com.mochiladeemergencia.app.ui.components.EstadoVazio
import br.com.mochiladeemergencia.app.ui.icones.Icones
import br.com.mochiladeemergencia.app.ui.viewmodel.ContatosViewModel
import br.com.mochiladeemergencia.app.util.Telefone

/**
 * Contatos de emergência: números públicos fixos + contatos pessoais do perfil ativo.
 * Toda ligação passa por ACTION_DIAL — o app nunca disca sozinho (sem CALL_PHONE).
 */
@Composable
fun ContatosScreen(viewModel: ContatosViewModel, onMensagem: (String) -> Unit) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val mensagem by viewModel.mensagem.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    var contatoEmEdicao by remember { mutableStateOf<ContatoEntity?>(null) }
    var mostrarNovo by remember { mutableStateOf(false) }
    var contatoParaExcluir by remember { mutableStateOf<ContatoEntity?>(null) }

    LaunchedEffect(mensagem) {
        mensagem?.let {
            snackbar.showSnackbar(it)
            viewModel.consumirMensagem()
        }
    }

    fun ligar(numero: String) {
        if (!Telefone.discar(contexto, numero)) {
            onMensagem("Nenhum aplicativo de telefone encontrado neste aparelho.")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Filled.Warning, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Em emergência grave ligue imediatamente para o SAMU (192) ou " +
                                "Bombeiros (193). O app apenas abre o discador — a chamada é " +
                                "feita por você.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item { Text("Números de emergência", style = MaterialTheme.typography.titleMedium) }

            items(items = ContatosEmergenciaFixos.LISTA, key = { it.numero }) { contato ->
                CartaoContatoFixo(
                    nome = contato.nome,
                    numero = contato.numero,
                    descricao = contato.descricao,
                    onLigar = { ligar(contato.numero) }
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Contatos pessoais (${estado.perfilNome.ifBlank { "Casa" }})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (estado.vazio) {
                item {
                    EstadoVazio(
                        icone = Icones.Contatos,
                        titulo = "Nenhum contato pessoal",
                        descricao = "Cadastre telefones de familiares, vizinhos ou do posto de saúde " +
                            "para ter tudo à mão offline.",
                        acao = { Button(onClick = { mostrarNovo = true }) { Text("Adicionar contato") } }
                    )
                }
            } else {
                items(items = estado.contatos, key = { it.id }) { contato ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
                            Text(contato.nome, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (contato.parentesco.isNotBlank()) {
                                    "${contato.parentesco} · ${contato.telefone}"
                                } else {
                                    contato.telefone
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (contato.observacao.isNotBlank()) {
                                Text(contato.observacao, style = MaterialTheme.typography.bodySmall)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(onClick = { ligar(contato.telefone) }) {
                                    Icon(
                                        Icones.Telefone,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Ligar")
                                }
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { contatoEmEdicao = contato }) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "Editar ${contato.nome}",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(onClick = { contatoParaExcluir = contato }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Excluir ${contato.nome}",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { mostrarNovo = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Adicionar contato pessoal")
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

    if (mostrarNovo || contatoEmEdicao != null) {
        val editando = contatoEmEdicao
        DialogoContato(
            contato = editando,
            onSalvar = { nome, telefone, parentesco, observacao ->
                viewModel.salvar(editando?.id ?: 0L, nome, telefone, parentesco, observacao)
                mostrarNovo = false
                contatoEmEdicao = null
            },
            onFechar = {
                mostrarNovo = false
                contatoEmEdicao = null
            }
        )
    }

    contatoParaExcluir?.let { contato ->
        DialogoConfirmacao(
            titulo = "Excluir contato?",
            texto = "\"${contato.nome}\" será removido da lista.",
            rotuloConfirmar = "Excluir",
            destrutivo = true,
            onConfirmar = {
                viewModel.excluir(contato)
                contatoParaExcluir = null
            },
            onCancelar = { contatoParaExcluir = null }
        )
    }
}

@Composable
private fun CartaoContatoFixo(
    nome: String,
    numero: String,
    descricao: String,
    onLigar: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(nome, style = MaterialTheme.typography.titleMedium)
                Text(numero, style = MaterialTheme.typography.headlineSmall)
                Text(descricao, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onLigar) {
                Icon(Icones.Telefone, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Ligar")
            }
        }
    }
}

@Composable
private fun DialogoContato(
    contato: ContatoEntity?,
    onSalvar: (String, String, String, String) -> Unit,
    onFechar: () -> Unit
) {
    var nome by remember { mutableStateOf(contato?.nome ?: "") }
    var telefone by remember { mutableStateOf(contato?.telefone ?: "") }
    var parentesco by remember { mutableStateOf(contato?.parentesco ?: "") }
    var observacao by remember { mutableStateOf(contato?.observacao ?: "") }

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text(if (contato == null) "Novo contato" else "Editar contato") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it.take(80) },
                    label = { Text("Nome *") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = telefone,
                    onValueChange = { telefone = it.take(24) },
                    label = { Text("Telefone *") },
                    placeholder = { Text("(21) 99999-9999") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                OutlinedTextField(
                    value = parentesco,
                    onValueChange = { parentesco = it.take(40) },
                    label = { Text("Parentesco / relação") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = observacao,
                    onValueChange = { observacao = it.take(200) },
                    label = { Text("Observação") }
                )
                Text(
                    text = "O número é usado apenas para abrir o discador do aparelho.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSalvar(nome, telefone, parentesco, observacao) }) {
                Text("Salvar")
            }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } }
    )
}
