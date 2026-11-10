package br.com.mochiladeemergencia.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.mochiladeemergencia.app.domain.CardPrimeirosSocorros
import br.com.mochiladeemergencia.app.domain.PrimeirosSocorros
import br.com.mochiladeemergencia.app.ui.icones.Icones
import br.com.mochiladeemergencia.app.util.Telefone

/**
 * Primeiros socorros — conteúdo informativo e offline.
 * O aviso de que o conteúdo não substitui treinamento/atendimento profissional aparece
 * fixo no topo e repetido em cada card. A discagem usa ACTION_DIAL (o usuário confirma).
 */
@Composable
fun PrimeirosSocorrosScreen(
    onSalvarLink: () -> Unit,
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
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Aviso importante: ${PrimeirosSocorros.AVISO}" }
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Filled.Warning, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(PrimeirosSocorros.AVISO, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { Telefone.discar(contexto, "192") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icones.Telefone, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ligar 192")
                }
                OutlinedButton(
                    onClick = { Telefone.discar(contexto, "193") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icones.Telefone, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ligar 193")
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onSalvarLink,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Salvar link de primeiros socorros") }
        }

        items(items = PrimeirosSocorros.CARDS, key = { it.titulo }) { card ->
            CardPrimeirosSocorrosExpansivel(
                card = card,
                onLigar = { numero ->
                    if (!Telefone.discar(contexto, numero)) {
                        onMensagem("Nenhum aplicativo de telefone encontrado neste aparelho.")
                    }
                }
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Fonte: ${PrimeirosSocorros.FONTE}. Conteúdo informativo, sem " +
                        "instruções de combate a incêndio ou procedimentos invasivos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun CardPrimeirosSocorrosExpansivel(
    card: CardPrimeirosSocorros,
    onLigar: (String) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expandido = !expandido }
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icones.Cruz,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = card.titulo,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expandido) {
                        Icons.Filled.KeyboardArrowUp
                    } else {
                        Icons.Filled.KeyboardArrowDown
                    },
                    contentDescription = if (expandido) {
                        "Recolher ${card.titulo}"
                    } else {
                        "Expandir ${card.titulo}"
                    }
                )
            }

            AnimatedVisibility(visible = expandido) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = PrimeirosSocorros.AVISO,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )

                    card.sinal?.let { sinal ->
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Sinal: $sinal",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Text("O que fazer", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    card.passos.forEachIndexed { indice, passo ->
                        Row(modifier = Modifier.padding(vertical = 3.dp)) {
                            Text(
                                text = "${indice + 1}.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(22.dp)
                            )
                            Text(text = passo, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Não faça",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(card.naoFaca, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { onLigar(card.numeroDestaque) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icones.Telefone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Ligar ${card.numeroDestaque}")
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Fonte: ${PrimeirosSocorros.FONTE}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
