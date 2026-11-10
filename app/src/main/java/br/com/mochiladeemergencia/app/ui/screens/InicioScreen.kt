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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.mochiladeemergencia.app.domain.StatusCalculadora
import br.com.mochiladeemergencia.app.ui.components.CartaoEstatistica
import br.com.mochiladeemergencia.app.ui.components.EstadoVazio
import br.com.mochiladeemergencia.app.ui.components.SeloStatus
import br.com.mochiladeemergencia.app.ui.icones.Icones
import br.com.mochiladeemergencia.app.ui.theme.LocalCoresStatus
import br.com.mochiladeemergencia.app.ui.viewmodel.InicioViewModel
import br.com.mochiladeemergencia.app.ui.viewmodel.ItemComStatus
import br.com.mochiladeemergencia.app.util.Formatos

@Composable
fun InicioScreen(
    viewModel: InicioViewModel,
    onAdicionarItem: () -> Unit,
    onAbrirChecklist: () -> Unit,
    onAbrirPerfis: () -> Unit,
    onAbrirDocumentos: () -> Unit,
    onAbrirMochila: () -> Unit,
    onAbrirAjustes: () -> Unit,
    onAbrirConferir: () -> Unit,
    onAbrirRepor: () -> Unit,
    onAbrirPrimeirosSocorros: () -> Unit,
    onAbrirContatos: () -> Unit,
    onMensagem: (String) -> Unit
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val cores = LocalCoresStatus.current
    var editandoLocalizacao by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(text = "Olá!", style = MaterialTheme.typography.headlineSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Perfil ativo: ",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = estado.nomePerfil.ifBlank { "Casa" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = onAbrirPerfis) { Text("Trocar") }
                }
            }
        }

        // -------- Card grande em destaque: localização da mochila --------
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { editandoLocalizacao = true }
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icones.Localizacao,
                        contentDescription = "Localização da mochila",
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Onde está a mochila",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = estado.localizacao.ifBlank { "Toque para definir o local" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (estado.pessoasCasa == 1) {
                                "1 pessoa na casa"
                            } else {
                                "${estado.pessoasCasa} pessoas na casa"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Editar localização da mochila",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // -------- Dica fixa: girar o estoque --------
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icones.Dica, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Gire o estoque", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Consuma primeiro o que está perto de vencer e reponha com o " +
                                "produto novo. Assim nada vence dentro da mochila.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (estado.vencendo > 0 || estado.vencidos > 0) {
                            TextButton(
                                onClick = onAbrirMochila,
                                contentPadding = PaddingValues(horizontal = 0.dp)
                            ) {
                                Text("Ver o que está perto de vencer (${estado.vencendo + estado.vencidos})")
                            }
                        }
                        TextButton(
                            onClick = onAbrirRepor,
                            contentPadding = PaddingValues(horizontal = 0.dp)
                        ) { Text("Ver itens a repor") }
                    }
                }
            }
        }

        if (!estado.notificacoesAtivas) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icones.NotificacoesOff, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Notificações desativadas", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Os avisos de validade continuam aparecendo aqui no painel.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = onAbrirAjustes) { Text("Ajustar notificações") }
                    }
                }
            }
        }

        // -------- Atalhos rápidos --------
        item {
            Text("Atalhos", style = MaterialTheme.typography.titleMedium)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AtalhoRapido(
                    rotulo = "Checklist",
                    icone = Icones.Checklist,
                    onClick = onAbrirChecklist,
                    modifier = Modifier.weight(1f)
                )
                AtalhoRapido(
                    rotulo = "Conferir mochila",
                    icone = Icones.Conferir,
                    onClick = onAbrirConferir,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AtalhoRapido(
                    rotulo = "Primeiros socorros",
                    icone = Icones.Cruz,
                    onClick = onAbrirPrimeirosSocorros,
                    modifier = Modifier.weight(1f)
                )
                AtalhoRapido(
                    rotulo = "Contatos",
                    icone = Icones.Contatos,
                    onClick = onAbrirContatos,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // -------- Cards de estatística --------
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CartaoEstatistica(
                    valor = estado.totalItens.toString(),
                    rotulo = "Itens na mochila",
                    icone = Icones.Mochila,
                    corContainer = MaterialTheme.colorScheme.surfaceContainerHigh,
                    corConteudo = MaterialTheme.colorScheme.onSurface,
                    descricaoAcessivel = "${estado.totalItens} itens na mochila",
                    modifier = Modifier.weight(1f),
                    onClick = onAbrirMochila
                )
                CartaoEstatistica(
                    valor = estado.faltando.toString(),
                    rotulo = "Itens faltando",
                    icone = Icons.Filled.Warning,
                    corContainer = cores.falta,
                    corConteudo = cores.sobreFalta,
                    descricaoAcessivel = "${estado.faltando} itens faltando",
                    modifier = Modifier.weight(1f),
                    onClick = onAbrirMochila
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CartaoEstatistica(
                    valor = estado.aRepor.toString(),
                    rotulo = "Itens a repor",
                    icone = Icones.Repor,
                    corContainer = cores.repor,
                    corConteudo = cores.sobreRepor,
                    descricaoAcessivel = "${estado.aRepor} itens a repor",
                    modifier = Modifier.weight(1f),
                    onClick = onAbrirRepor
                )
                CartaoEstatistica(
                    valor = estado.contatos.toString(),
                    rotulo = "Contatos de emergência",
                    icone = Icones.Contatos,
                    corContainer = MaterialTheme.colorScheme.secondaryContainer,
                    corConteudo = MaterialTheme.colorScheme.onSecondaryContainer,
                    descricaoAcessivel = "${estado.contatos} contatos de emergência",
                    modifier = Modifier.weight(1f),
                    onClick = onAbrirContatos
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CartaoEstatistica(
                    valor = estado.vencendo.toString(),
                    rotulo = "Vencendo em ${estado.antecedenciaDias} dias",
                    icone = Icons.Filled.DateRange,
                    corContainer = cores.vencendo,
                    corConteudo = cores.sobreVencendo,
                    descricaoAcessivel = "${estado.vencendo} itens vencendo em até " +
                        "${estado.antecedenciaDias} dias",
                    modifier = Modifier.weight(1f),
                    onClick = onAbrirMochila
                )
                CartaoEstatistica(
                    valor = estado.vencidos.toString(),
                    rotulo = "Itens vencidos",
                    icone = Icons.Filled.Warning,
                    corContainer = cores.vencido,
                    corConteudo = cores.sobreVencido,
                    descricaoAcessivel = "${estado.vencidos} itens vencidos",
                    modifier = Modifier.weight(1f),
                    onClick = onAbrirMochila
                )
            }
        }

        item {
            CartaoEstatistica(
                valor = estado.documentos.toString(),
                rotulo = "Documentos salvos",
                icone = Icones.Documento,
                corContainer = MaterialTheme.colorScheme.surfaceContainerHigh,
                corConteudo = MaterialTheme.colorScheme.onSurface,
                descricaoAcessivel = "${estado.documentos} documentos salvos",
                modifier = Modifier.fillMaxWidth(),
                onClick = onAbrirDocumentos
            )
        }

        if (estado.aRepor > 0) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = cores.repor,
                        contentColor = cores.sobreRepor
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icones.Repor, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${estado.aRepor} item(ns) precisam ser repostos",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Abra a lista Repor e marque o que já foi comprado.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(onClick = onAbrirRepor) { Text("Abrir") }
                    }
                }
            }
        }

        // -------- Checklist --------
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icones.Checklist, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Checklist", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${estado.checklistAdicionados} de ${estado.checklistTotal} " +
                            "sugestões já estão na mochila",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { estado.percentualChecklist },
                        modifier = Modifier.fillMaxWidth(),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onAbrirChecklist) { Text("Abrir checklist") }
                        TextButton(onClick = onAbrirConferir) { Text("Conferir mochila") }
                    }
                }
            }
        }

        item {
            Text(
                text = "Próximos vencimentos",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (estado.proximosVencimentos.isEmpty() && !estado.carregando) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Nenhum item com data de validade.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = onAdicionarItem) { Text("Adicionar item com validade") }
                    }
                }
            }
        }

        items(items = estado.proximosVencimentos, key = { it.item.id }) { item ->
            LinhaProximoVencimento(item)
        }

        if (estado.mochilaVazia && !estado.carregando) {
            item {
                EstadoVazio(
                    icone = Icones.Mochila,
                    titulo = "Sua mochila está vazia",
                    descricao = "Comece pelo checklist com as sugestões básicas ou adicione seus próprios itens.",
                    acao = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Button(onClick = onAbrirChecklist) { Text("Ver checklist") }
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = onAdicionarItem) { Text("Adicionar item") }
                        }
                    }
                )
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Este app não substitui orientação profissional de emergência. " +
                        "Em caso de risco, procure os serviços de emergência da sua região.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }

    if (editandoLocalizacao) {
        var texto by remember { mutableStateOf(estado.localizacao) }
        AlertDialog(
            onDismissRequest = { editandoLocalizacao = false },
            title = { Text("Localização da mochila") },
            text = {
                Column {
                    Text(
                        "Descreva onde a mochila fica guardada para que todos da casa saibam encontrá-la.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { texto = it.take(120) },
                        label = { Text("Ex.: Armário da entrada") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.salvarLocalizacao(texto)
                    editandoLocalizacao = false
                    onMensagem("Localização da mochila atualizada.")
                }) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { editandoLocalizacao = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun AtalhoRapido(
    rotulo: String,
    icone: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .heightIn(min = 76.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icone, contentDescription = rotulo, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(6.dp))
            Text(rotulo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun LinhaProximoVencimento(item: ItemComStatus) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.item.nome, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${Formatos.quantidadeComUnidade(item.item.quantidade, item.item.unidade)} · " +
                        formatarValidade(item.item.validadeEpochDay),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = StatusCalculadora.descreverDias(item.diasRestantes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SeloStatus(item.status)
        }
    }
}

private fun formatarValidade(epochDay: Long?): String {
    val data = Formatos.data(epochDay)
    return if (data.isBlank()) "sem validade" else "val. $data"
}
