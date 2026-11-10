package br.com.mochiladeemergencia.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.mochiladeemergencia.app.domain.StatusCalculadora
import br.com.mochiladeemergencia.app.domain.model.Categoria
import br.com.mochiladeemergencia.app.domain.model.StatusItem
import br.com.mochiladeemergencia.app.ui.components.EstadoVazio
import br.com.mochiladeemergencia.app.ui.components.MiniaturaFoto
import br.com.mochiladeemergencia.app.ui.components.SeloStatus
import br.com.mochiladeemergencia.app.ui.icones.Icones
import br.com.mochiladeemergencia.app.ui.viewmodel.ConferirViewModel
import br.com.mochiladeemergencia.app.util.Formatos

/**
 * Auditoria item a item: "Tenho", "Falta" ou "Substituir".
 * Mostra progresso durante a conferência e um resumo ao final.
 */
@Composable
fun ConferirScreen(
    viewModel: ConferirViewModel,
    onVoltarParaMochila: () -> Unit,
    onAbrirRepor: () -> Unit
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.iniciar() }

    if (estado.vazio) {
        EstadoVazio(
            icone = Icones.Conferir,
            titulo = "Nada para conferir",
            descricao = "Adicione itens à mochila para poder fazer a conferência.",
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (estado.concluido) "Conferência concluída" else "Conferindo a mochila",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (estado.concluido) {
                        "${estado.total} itens revisados no perfil ${estado.perfilNome.ifBlank { "Casa" }}."
                    } else {
                        "Item ${estado.indiceAtual + 1} de ${estado.total} · " +
                            "perfil ${estado.perfilNome.ifBlank { "Casa" }}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                val progresso = if (estado.concluido) 1f else estado.progresso
                LinearProgressIndicator(
                    progress = { progresso },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${(progresso * 100).toInt()}% concluído",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (estado.concluido) {
            ResumoConferencia(estado, onVoltarParaMochila, onAbrirRepor, viewModel::reiniciar)
        } else {
            estado.atual?.let { atual ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MiniaturaFoto(
                                nomeArquivo = atual.item.fotoArquivo,
                                descricao = "Foto de ${atual.item.nome}",
                                tamanho = 56.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(atual.item.nome, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = Formatos.quantidadeComUnidade(
                                        atual.item.quantidade,
                                        atual.item.unidade
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = Categoria.dePersistido(atual.item.categoria).rotulo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (atual.item.local.isNotBlank()) {
                                "Local: ${atual.item.local}"
                            } else {
                                "Local não informado"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Validade: " +
                                (Formatos.data(atual.item.validadeEpochDay).ifBlank { "sem validade" }) +
                                " · " + StatusCalculadora.descreverDias(atual.diasRestantes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        SeloStatus(atual.status)
                    }
                }

                Text(
                    text = "Este item está na mochila, em bom estado?",
                    style = MaterialTheme.typography.titleMedium
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.responder(StatusItem.TENHO) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Tenho")
                    }
                    OutlinedButton(
                        onClick = { viewModel.responder(StatusItem.FALTA) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icones.Menos, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Falta")
                    }
                    OutlinedButton(
                        onClick = { viewModel.responder(StatusItem.REPOR) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icones.Repor, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Substituir (vai para Repor)")
                    }
                }
                Text(
                    text = "Cada resposta é salva na hora. Se você sair, pode continuar depois.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } ?: Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun ResumoConferencia(
    estado: br.com.mochiladeemergencia.app.ui.viewmodel.ConferirUiState,
    onVoltarParaMochila: () -> Unit,
    onAbrirRepor: () -> Unit,
    onReiniciar: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LinhaResumo("Tenho", estado.contagemTenho, StatusItem.TENHO)
            HorizontalDivider()
            LinhaResumo("Falta", estado.contagemFalta, StatusItem.FALTA)
            HorizontalDivider()
            LinhaResumo("Substituir (foram para Repor)", estado.contagemSubstituir, StatusItem.REPOR)
            Spacer(Modifier.height(8.dp))
            Text(
                text = when {
                    estado.contagemFalta == 0 && estado.contagemSubstituir == 0 ->
                        "Tudo certo! Sua mochila está completa."
                    estado.contagemSubstituir > 0 ->
                        "Há itens para repor. Abra a lista Repor para registrar as compras."
                    else ->
                        "Há itens faltando. Marque como falta para lembrar de repor."
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    Button(
        onClick = onAbrirRepor,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) { Text("Abrir lista Repor (${estado.contagemSubstituir})") }

    OutlinedButton(
        onClick = onVoltarParaMochila,
        modifier = Modifier.fillMaxWidth()
    ) { Text("Voltar para Minha Mochila") }

    OutlinedButton(
        onClick = onReiniciar,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Conferir novamente")
    }
}

@Composable
private fun LinhaResumo(rotulo: String, valor: Int, status: StatusItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rotulo,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = valor.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(10.dp))
        SeloStatus(status)
    }
}
