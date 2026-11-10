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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.mochiladeemergencia.app.data.repository.ContatosEmergenciaFixos
import br.com.mochiladeemergencia.app.domain.PrimeirosSocorros
import br.com.mochiladeemergencia.app.ui.icones.Icones
import br.com.mochiladeemergencia.app.util.Telefone

/** Aba Emergência: atalho para primeiros socorros, contatos e discagem rápida. */
@Composable
fun EmergenciaScreen(
    onAbrirPrimeirosSocorros: () -> Unit,
    onAbrirContatos: () -> Unit,
    onAbrirDocumentos: () -> Unit,
    onMensagem: (String) -> Unit
) {
    val contexto = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    Text(PrimeirosSocorros.AVISO, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (!Telefone.discar(contexto, "192")) {
                            onMensagem("Nenhum aplicativo de telefone encontrado.")
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Icon(Icones.Telefone, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("SAMU 192")
                }
                OutlinedButton(
                    onClick = {
                        if (!Telefone.discar(contexto, "193")) {
                            onMensagem("Nenhum aplicativo de telefone encontrado.")
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Icon(Icones.Telefone, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Bombeiros 193")
                }
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    if (!Telefone.discar(contexto, "199")) {
                        onMensagem("Nenhum aplicativo de telefone encontrado.")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icones.Telefone, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Defesa Civil 199")
            }
        }

        item {
            CartaoAtalho(
                titulo = "Primeiros socorros",
                descricao = "Engasgo, RCP, queimadura, desmaio, convulsão e hemorragia — " +
                    "conteúdo informativo, disponível offline.",
                icone = Icones.Cruz,
                onClick = onAbrirPrimeirosSocorros
            )
        }
        item {
            CartaoAtalho(
                titulo = "Contatos de emergência",
                descricao = "SAMU, Bombeiros, Polícia, Defesa Civil e seus contatos pessoais.",
                icone = Icones.Contatos,
                onClick = onAbrirContatos
            )
        }
        item {
            CartaoAtalho(
                titulo = "Documentos úteis",
                descricao = "Links e arquivos que você salvou (laudos, receitas, contatos).",
                icone = Icones.Documento,
                onClick = onAbrirDocumentos
            )
        }

        item {
            Text("Ligar rápido", style = MaterialTheme.typography.titleMedium)
        }

        items(items = ContatosEmergenciaFixos.LISTA, key = { it.numero }) { contato ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (!Telefone.discar(contexto, contato.numero)) {
                            onMensagem("Nenhum aplicativo de telefone encontrado.")
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icones.Telefone, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(contato.nome, style = MaterialTheme.typography.titleMedium)
                        Text(
                            contato.descricao,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(contato.numero, style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp)) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Todos os números abrem o discador do aparelho (o app não faz " +
                            "chamadas e não pede permissão de telefone).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CartaoAtalho(
    titulo: String,
    descricao: String,
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icone,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Text(
                    descricao,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
