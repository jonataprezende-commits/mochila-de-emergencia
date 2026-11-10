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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.core.content.FileProvider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.mochiladeemergencia.app.domain.model.Categoria
import br.com.mochiladeemergencia.app.domain.model.FormaConsumo
import br.com.mochiladeemergencia.app.domain.model.UNIDADES_PADRAO
import br.com.mochiladeemergencia.app.ui.components.BotaoData
import br.com.mochiladeemergencia.app.ui.components.MiniaturaFoto
import br.com.mochiladeemergencia.app.ui.icones.Icones
import br.com.mochiladeemergencia.app.util.Fotos
import java.io.File
import br.com.mochiladeemergencia.app.ui.viewmodel.ItemFormViewModel
import br.com.mochiladeemergencia.app.util.Formatos

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemFormScreen(
    viewModel: ItemFormViewModel,
    itemId: Long,
    onVoltar: () -> Unit
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var mostrarCalendario by remember { mutableStateOf(false) }
    val contexto = LocalContext.current

    // Photo Picker: não exige permissão ampla de mídia (nem READ_MEDIA_IMAGES).
    val seletorFoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) viewModel.definirFotoDeUri(uri)
    }

    // Arquivo temporário para a câmera do sistema (FileProvider, sem permissão de câmera).
    val arquivoCamera = remember { mutableStateOf<File?>(null) }
    val camera = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { sucesso: Boolean ->
        val arquivo = arquivoCamera.value
        if (sucesso && arquivo != null) {
            viewModel.definirFotoDeCamera(arquivo)
        } else {
            arquivo?.delete()
        }
        arquivoCamera.value = null
    }

    fun tirarFoto() {
        val arquivo = File(Fotos.pastaTemporaria(contexto), "camera_${System.currentTimeMillis()}.jpg")
        arquivoCamera.value = arquivo
        val uri = FileProvider.getUriForFile(
            contexto,
            "${contexto.packageName}.fileprovider",
            arquivo
        )
        camera.launch(uri)
    }

    LaunchedEffect(itemId) { viewModel.carregar(itemId) }
    LaunchedEffect(estado.salvo) { if (estado.salvo) onVoltar() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (estado.editando) "Editar item" else "Novo item da mochila",
            style = MaterialTheme.typography.titleLarge
        )

        OutlinedTextField(
            value = estado.nome,
            onValueChange = viewModel::definirNome,
            label = { Text("Nome do item *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = estado.erroNome != null,
            supportingText = { estado.erroNome?.let { Text(it) } }
        )

        // ------------- Foto opcional -------------
        Text("Foto do item (opcional)", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            MiniaturaFoto(
                nomeArquivo = estado.fotoArquivo,
                descricao = if (estado.fotoArquivo != null) {
                    "Foto atual do item"
                } else {
                    "Item sem foto"
                },
                tamanho = 72.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = {
                        seletorFoto.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icones.Galeria, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Escolher da galeria")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { tirarFoto() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icones.Camera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Tirar foto")
                }
                if (estado.fotoArquivo != null) {
                    TextButton(onClick = viewModel::removerFoto) { Text("Remover foto") }
                }
            }
        }
        Text(
            text = "A foto fica salva apenas no aparelho e é reduzida automaticamente.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        CampoSelecao(
            rotulo = "Categoria",
            valor = estado.categoria.rotulo,
            opcoes = Categoria.entries.map { it.rotulo },
            onSelecionar = { rotulo ->
                Categoria.entries.firstOrNull { it.rotulo == rotulo }?.let(viewModel::definirCategoria)
            }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = estado.quantidade,
                onValueChange = viewModel::definirQuantidade,
                label = { Text("Quantidade *") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = estado.erroQuantidade != null,
                supportingText = { estado.erroQuantidade?.let { Text(it) } }
            )
            OutlinedTextField(
                value = estado.unidade,
                onValueChange = viewModel::definirUnidade,
                label = { Text("Unidade") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        if (estado.categoria in Categoria.DE_ALIMENTO) {
            CampoSelecao(
                rotulo = "Forma de consumo (opcional)",
                valor = estado.formaConsumo.rotulo,
                opcoes = FormaConsumo.entries.map { it.rotulo },
                onSelecionar = { rotulo ->
                    FormaConsumo.entries.firstOrNull { it.rotulo == rotulo }
                        ?.let(viewModel::definirFormaConsumo)
                }
            )
            Text(
                text = "Ex.: abrir e comer, aquecer ou adicionar água. Ajuda a saber o que dá " +
                    "para consumir sem preparo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UNIDADES_PADRAO.take(6).forEach { unidade ->
                FilterChip(
                    selected = estado.unidade == unidade,
                    onClick = { viewModel.definirUnidade(unidade) },
                    label = { Text(unidade) }
                )
            }
        }

        BotaoData(
            rotulo = "Adicionar validade (opcional)",
            dataFormatada = Formatos.data(estado.validadeEpochDay).ifBlank { null },
            descricaoAcessivel = "Escolher data de validade do item",
            onEscolherData = { mostrarCalendario = true },
            onLimparData = { viewModel.definirValidade(null) }
        )
        Text(
            text = "Itens sem data de validade não geram alertas de vencimento.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = estado.local,
            onValueChange = viewModel::definirLocal,
            label = { Text("Local na mochila (opcional)") },
            placeholder = { Text("Ex.: bolso frontal") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = estado.observacao,
            onValueChange = viewModel::definirObservacao,
            label = { Text("Observação (opcional)") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )

        Text("Status do item", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !estado.falta,
                onClick = { viewModel.definirFalta(false) },
                label = { Text("Tenho") }
            )
            FilterChip(
                selected = estado.falta,
                onClick = { viewModel.definirFalta(true) },
                label = { Text("Falta") }
            )
        }

        estado.erroSalvar?.let { erro ->
            Text(text = erro, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = viewModel::salvar,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(if (estado.editando) "Salvar alterações" else "Adicionar à mochila")
        }
        Spacer(Modifier.height(24.dp))
    }

    if (mostrarCalendario) {
        val estadoCalendario = rememberDatePickerState(
            initialSelectedDateMillis = estado.validadeEpochDay?.let {
                Formatos.epochDayParaUtcMillis(it)
            }
        )
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoCalendario.selectedDateMillis?.let { millis ->
                        viewModel.definirValidade(Formatos.utcMillisParaEpochDay(millis))
                    }
                    mostrarCalendario = false
                }) { Text("Escolher") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = estadoCalendario)
        }
    }
}

@Composable
private fun CampoSelecao(
    rotulo: String,
    valor: String,
    opcoes: List<String>,
    onSelecionar: (String) -> Unit
) {
    var aberto by remember { mutableStateOf(false) }
    Column {
        Text(rotulo, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Box {
            OutlinedButton(
                onClick = { aberto = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(text = valor, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Abrir opções de $rotulo")
            }
            DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
                opcoes.forEach { opcao ->
                    DropdownMenuItem(
                        text = { Text(opcao) },
                        onClick = {
                            onSelecionar(opcao)
                            aberto = false
                        }
                    )
                }
            }
        }
    }
}
