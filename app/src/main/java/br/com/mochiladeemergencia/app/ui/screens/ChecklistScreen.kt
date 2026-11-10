package br.com.mochiladeemergencia.app.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.mochiladeemergencia.app.data.local.entity.ChecklistEntity
import br.com.mochiladeemergencia.app.domain.model.Categoria
import br.com.mochiladeemergencia.app.domain.model.StatusItem
import br.com.mochiladeemergencia.app.ui.components.DialogoConfirmacao
import br.com.mochiladeemergencia.app.ui.components.SeloStatus
import br.com.mochiladeemergencia.app.ui.icones.Icones
import br.com.mochiladeemergencia.app.ui.viewmodel.ChecklistComEstado
import br.com.mochiladeemergencia.app.ui.viewmodel.ChecklistUiState
import br.com.mochiladeemergencia.app.ui.viewmodel.ChecklistViewModel
import br.com.mochiladeemergencia.app.ui.viewmodel.FiltroChecklist
import br.com.mochiladeemergencia.app.ui.viewmodel.GrupoChecklist

/**
 * Checklist por categoria: sugestões padrão editáveis, itens personalizados e o que já
 * está na mochila. Nenhuma quantidade é imposta — as dicas são apenas informativas.
 */
@Composable
fun ChecklistScreen(vm: ChecklistViewModel) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val mensagem by vm.mensagem.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var mostrarNovaSugestao by remember { mutableStateOf(false) }
    var sugestaoParaExcluir by remember { mutableStateOf<ChecklistEntity?>(null) }
    var sugestaoParaEditar by remember { mutableStateOf<ChecklistEntity?>(null) }
    var confirmarRestaurar by remember { mutableStateOf(false) }

    LaunchedEffect(mensagem) {
        mensagem?.let {
            snackbar.showSnackbar(it)
            vm.consumirMensagem()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "cabecalho") {
                CabecalhoChecklist(
                    estado = estado,
                    onAlternarAjuste = vm::alternarAjustePorPessoas
                )
            }

            item(key = "filtros") {
                FiltrosChecklist(estado.filtro, vm::definirFiltro)
            }

            if (estado.checklistVazio && !estado.carregando) {
                item(key = "vazio") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (estado.filtro == FiltroChecklist.TODOS) {
                                    "Nenhuma sugestão no checklist."
                                } else {
                                    "Nada neste filtro."
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (estado.filtro == FiltroChecklist.TODOS) {
                                    "Crie uma sugestão personalizada ou restaure o checklist padrão."
                                } else {
                                    "Troque o filtro para ver as outras sugestões."
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            estado.grupos.forEach { grupo ->
                item(key = "grupo_${grupo.categoria.name}") {
                    CabecalhoGrupo(grupo)
                }
                items(
                    items = grupo.itens,
                    key = { it.sugestao.id },
                    contentType = { "sugestao" }
                ) { item ->
                    CartaoSugestao(
                        item = item,
                        onAdicionar = { vm.adicionarAMochila(item) },
                        onEditar = { sugestaoParaEditar = item.sugestao },
                        onExcluir = { sugestaoParaExcluir = item.sugestao },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            item(key = "acoes") {
                Column {
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { mostrarNovaSugestao = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Criar sugestão personalizada")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { confirmarRestaurar = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Restaurar checklist padrão") }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Estas são sugestões, não imposições: edite ou remova o que quiser. " +
                            "As quantidades ficam na tela Mochila.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

    if (mostrarNovaSugestao) {
        DialogoSugestao(
            titulo = "Nova sugestão",
            nomeInicial = "",
            categoriaInicial = Categoria.OUTROS,
            dicaInicial = "",
            unidadeInicial = "un",
            onSalvar = { nome, categoria, unidade, dica ->
                vm.criarSugestao(nome, categoria, 1.0, unidade, dica)
                mostrarNovaSugestao = false
            },
            onFechar = { mostrarNovaSugestao = false }
        )
    }

    sugestaoParaEditar?.let { sugestao ->
        DialogoSugestao(
            titulo = "Editar sugestão",
            nomeInicial = sugestao.nome,
            categoriaInicial = Categoria.dePersistido(sugestao.categoria),
            dicaInicial = sugestao.dica,
            unidadeInicial = sugestao.unidade,
            onSalvar = { nome, categoria, unidade, dica ->
                vm.atualizarSugestao(
                    sugestao = sugestao,
                    nome = nome,
                    categoria = categoria,
                    quantidadeBase = sugestao.quantidadeBase,
                    unidade = unidade,
                    dica = dica
                )
                sugestaoParaEditar = null
            },
            onFechar = { sugestaoParaEditar = null }
        )
    }

    sugestaoParaExcluir?.let { sugestao ->
        DialogoConfirmacao(
            titulo = "Remover sugestão?",
            texto = "\"${sugestao.nome}\" sairá do checklist. Os itens já adicionados à mochila continuam lá.",
            rotuloConfirmar = "Remover",
            destrutivo = true,
            onConfirmar = {
                vm.excluirSugestao(sugestao)
                sugestaoParaExcluir = null
            },
            onCancelar = { sugestaoParaExcluir = null }
        )
    }

    if (confirmarRestaurar) {
        DialogoConfirmacao(
            titulo = "Restaurar checklist padrão?",
            texto = "As sugestões padrão voltam ao estado original. Suas sugestões personalizadas " +
                "são mantidas e as edições feitas nos padrões são desfeitas.",
            rotuloConfirmar = "Restaurar",
            onConfirmar = {
                vm.restaurarPadroes()
                confirmarRestaurar = false
            },
            onCancelar = { confirmarRestaurar = false }
        )
    }
}

@Composable
private fun CabecalhoChecklist(estado: ChecklistUiState, onAlternarAjuste: (Boolean) -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Checklist de ${estado.perfilNome.ifBlank { "Casa" }}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${estado.adicionados} de ${estado.total} sugestões já estão na mochila · " +
                    "${estado.faltando} falta(m)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { estado.progresso },
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mostrar dica por pessoas", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Só uma dica aproximada para ${estado.pessoasCasa} pessoa(s). " +
                            "Nenhuma quantidade é alterada automaticamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = estado.ajustarPorPessoas,
                    onCheckedChange = onAlternarAjuste
                )
            }
        }
    }
}

@Composable
private fun FiltrosChecklist(atual: FiltroChecklist, onFiltrar: (FiltroChecklist) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FiltroChecklist.entries.forEach { filtro ->
            FilterChip(
                selected = atual == filtro,
                onClick = { onFiltrar(filtro) },
                label = { Text(filtro.rotulo) }
            )
        }
    }
}

@Composable
private fun CabecalhoGrupo(grupo: GrupoChecklist) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = grupo.categoria.rotulo,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (grupo.completo) "completo" else "${grupo.adicionados}/${grupo.total}",
            style = MaterialTheme.typography.labelMedium,
            color = if (grupo.completo) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CartaoSugestao(
    item: ChecklistComEstado,
    onAdicionar: () -> Unit,
    onEditar: () -> Unit,
    onExcluir: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sugestao = item.sugestao
    val categoria = Categoria.dePersistido(sugestao.categoria)
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(sugestao.nome, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = categoria.rotulo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.adicionado) {
                    SeloStatus(StatusItem.TENHO)
                }
            }

            if (sugestao.dica.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icones.Dica,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = sugestao.dica,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            item.dicaQuantidade?.let { dica ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = dica,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.adicionado) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Já está na mochila",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Button(onClick = onAdicionar) {
                        Text("Adicionar à minha mochila")
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEditar) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Editar sugestão ${sugestao.nome}",
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onExcluir) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Remover sugestão ${sugestao.nome}",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogoSugestao(
    titulo: String,
    nomeInicial: String,
    categoriaInicial: Categoria,
    dicaInicial: String,
    unidadeInicial: String,
    onSalvar: (String, Categoria, String, String) -> Unit,
    onFechar: () -> Unit
) {
    var nome by remember { mutableStateOf(nomeInicial) }
    var categoria by remember { mutableStateOf(categoriaInicial) }
    var dica by remember { mutableStateOf(dicaInicial) }
    var unidade by remember { mutableStateOf(unidadeInicial) }
    var menuCategoriaAberto by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text(titulo) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it.take(80) },
                    label = { Text("Nome *") },
                    placeholder = { Text("Ex.: pilha AA extra") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = dica,
                    onValueChange = { dica = it.take(160) },
                    label = { Text("Dica (opcional)") },
                    placeholder = { Text("Ex.: prefira fósforos à prova d'água") },
                    maxLines = 3
                )
                OutlinedTextField(
                    value = unidade,
                    onValueChange = { unidade = it.take(12) },
                    label = { Text("Unidade sugerida") },
                    singleLine = true
                )
                Text(
                    text = "A quantidade é definida por você na tela Mochila.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box {
                    OutlinedButton(
                        onClick = { menuCategoriaAberto = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(categoria.rotulo, modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Filled.ArrowDropDown,
                            contentDescription = "Escolher categoria"
                        )
                    }
                    DropdownMenu(
                        expanded = menuCategoriaAberto,
                        onDismissRequest = { menuCategoriaAberto = false }
                    ) {
                        Categoria.entries.forEach { opcao ->
                            DropdownMenuItem(
                                text = { Text(opcao.rotulo) },
                                onClick = {
                                    categoria = opcao
                                    menuCategoriaAberto = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSalvar(nome, categoria, unidade, dica) }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onFechar) { Text("Cancelar") }
        }
    )
}
