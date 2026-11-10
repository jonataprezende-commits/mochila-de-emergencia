package br.com.mochiladeemergencia.app.ui.screens

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.mochiladeemergencia.app.ui.icones.Icones
import br.com.mochiladeemergencia.app.data.local.entity.DocumentoEntity
import br.com.mochiladeemergencia.app.domain.model.TipoDocumento
import br.com.mochiladeemergencia.app.ui.components.DialogoConfirmacao
import br.com.mochiladeemergencia.app.ui.components.EstadoVazio
import br.com.mochiladeemergencia.app.ui.viewmodel.DocumentosViewModel
import br.com.mochiladeemergencia.app.util.IntentsDocumento

@Composable
fun DocumentosScreen(
    viewModel: DocumentosViewModel,
    onAdicionar: () -> Unit,
    onEditar: (Long) -> Unit,
    onMensagem: (String) -> Unit
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    var documentoParaExcluir by remember { mutableStateOf<DocumentoEntity?>(null) }

    if (!estado.carregando && estado.vazio) {
        EstadoVazio(
            icone = Icones.Documento,
            titulo = "Nenhum documento salvo",
            descricao = "Guarde links e arquivos úteis (PDF, imagens, vídeos e textos) para consultar " +
                "sem internet. Você escolhe o que salvar.",
            modifier = Modifier.fillMaxSize(),
            acao = { Button(onClick = onAdicionar) { Text("Adicionar documento") } }
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "Documentos do perfil ${estado.perfilNome.ifBlank { "Casa" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(items = estado.documentos, key = { it.id }, contentType = { "documento" }) { documento ->
                CartaoDocumento(
                    documento = documento,
                    onAbrir = {
                        val abriu = IntentsDocumento.abrir(contexto, documento)
                        if (!abriu) {
                            onMensagem(
                                "Nenhum app encontrado para abrir este documento. " +
                                    "Verifique se o arquivo ainda está disponível no aparelho."
                            )
                        }
                    },
                    onEditar = { onEditar(documento.id) },
                    onExcluir = { documentoParaExcluir = documento }
                )
            }
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Os links abrem no navegador e podem exigir internet. " +
                            "Arquivos escolhidos no aparelho ficam disponíveis offline. " +
                            "O app não baixa nenhum conteúdo de terceiros.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }

    documentoParaExcluir?.let { documento ->
        DialogoConfirmacao(
            titulo = "Excluir documento?",
            texto = "\"${documento.titulo}\" será removido da lista. " +
                "O arquivo original no aparelho não é apagado.",
            rotuloConfirmar = "Excluir",
            destrutivo = true,
            onConfirmar = {
                viewModel.excluir(documento)
                documentoParaExcluir = null
            },
            onCancelar = { documentoParaExcluir = null }
        )
    }
}

@Composable
private fun CartaoDocumento(
    documento: DocumentoEntity,
    onAbrir: () -> Unit,
    onEditar: () -> Unit,
    onExcluir: () -> Unit
) {
    val tipo = TipoDocumento.dePersistido(documento.tipo)
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = iconeDoTipo(tipo),
                    contentDescription = "Tipo: ${tipo.rotulo}",
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(documento.titulo, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = buildString {
                            append(tipo.rotulo)
                            if (documento.categoria.isNotBlank()) append(" · ${documento.categoria}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onAbrir) {
                    Icon(
                        Icones.AbrirExterno,
                        contentDescription = "Abrir ${documento.titulo}",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (documento.arquivoLocal) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Disponível offline",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (documento.observacao.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(documento.observacao, style = MaterialTheme.typography.bodyMedium)
            }

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onEditar) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Editar ${documento.titulo}",
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onExcluir) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Excluir ${documento.titulo}",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun iconeDoTipo(tipo: TipoDocumento): ImageVector = when (tipo) {
    TipoDocumento.LINK -> Icones.Link
    TipoDocumento.PDF -> Icones.DocumentoPdf
    TipoDocumento.IMAGEM -> Icones.Imagem
    TipoDocumento.VIDEO -> Icones.Video
    TipoDocumento.TEXTO -> Icones.Documento
}
