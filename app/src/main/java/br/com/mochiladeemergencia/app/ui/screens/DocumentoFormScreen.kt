package br.com.mochiladeemergencia.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.mochiladeemergencia.app.ui.icones.Icones
import br.com.mochiladeemergencia.app.domain.model.TipoDocumento
import br.com.mochiladeemergencia.app.ui.viewmodel.DocumentoFormViewModel
import br.com.mochiladeemergencia.app.util.IntentsDocumento

@Composable
fun DocumentoFormScreen(
    viewModel: DocumentoFormViewModel,
    documentoId: Long,
    onVoltar: () -> Unit
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current

    val seletorArquivo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // Mantém o acesso ao arquivo escolhido mesmo após reiniciar o app.
                contexto.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Alguns provedores não oferecem permissão persistente; seguimos com a URI atual.
            }
            viewModel.definirArquivo(uri.toString(), IntentsDocumento.nomeExibicao(contexto, uri))
        }
    }

    LaunchedEffect(documentoId) { viewModel.carregar(documentoId) }
    LaunchedEffect(estado.salvo) { if (estado.salvo) onVoltar() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (estado.editando) "Editar documento" else "Novo documento",
            style = MaterialTheme.typography.titleLarge
        )

        OutlinedTextField(
            value = estado.titulo,
            onValueChange = viewModel::definirTitulo,
            label = { Text("Título *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = estado.erroTitulo != null,
            supportingText = { estado.erroTitulo?.let { Text(it) } }
        )

        Text("Tipo", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TipoDocumento.entries.take(3).forEach { tipo ->
                FilterChip(
                    selected = estado.tipo == tipo,
                    onClick = { viewModel.definirTipo(tipo) },
                    label = { Text(tipo.rotulo) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TipoDocumento.entries.drop(3).forEach { tipo ->
                FilterChip(
                    selected = estado.tipo == tipo,
                    onClick = { viewModel.definirTipo(tipo) },
                    label = { Text(tipo.rotulo) }
                )
            }
        }

        if (estado.tipo == TipoDocumento.LINK) {
            OutlinedTextField(
                value = estado.uri,
                onValueChange = viewModel::definirUri,
                label = { Text("Link (http:// ou https://) *") },
                placeholder = { Text("https://exemplo.org") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = estado.erroUri != null,
                supportingText = { estado.erroUri?.let { Text(it) } }
            )
            Text(
                text = "O link abre no navegador ou app escolhido por você. " +
                    "O app não acessa a internet sozinho.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            OutlinedButton(
                onClick = { seletorArquivo.launch(arrayOf(mimeDoTipo(estado.tipo))) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icones.Upload, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (estado.nomeArquivo.isNotBlank()) estado.nomeArquivo
                    else "Escolher arquivo no aparelho"
                )
            }
            if (estado.erroUri != null) {
                Text(text = estado.erroUri!!, color = MaterialTheme.colorScheme.error)
            }
            Text(
                text = "Arquivos salvos assim ficam disponíveis offline. " +
                    "Se o arquivo for movido ou apagado, será preciso escolhê-lo novamente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = estado.categoria,
            onValueChange = viewModel::definirCategoria,
            label = { Text("Categoria (opcional)") },
            placeholder = { Text("Ex.: contatos, saúde, escola") },
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
            Text(if (estado.editando) "Salvar alterações" else "Salvar documento")
        }
        Text(
            text = "Você é responsável pelos conteúdos que salvar. O app não verifica " +
                "nem distribui esses materiais.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
    }
}

private fun mimeDoTipo(tipo: TipoDocumento): String = when (tipo) {
    TipoDocumento.PDF -> "application/pdf"
    TipoDocumento.IMAGEM -> "image/*"
    TipoDocumento.VIDEO -> "video/*"
    TipoDocumento.TEXTO -> "text/*"
    TipoDocumento.LINK -> "*/*"
}
