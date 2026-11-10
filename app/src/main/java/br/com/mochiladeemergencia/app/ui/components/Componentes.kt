package br.com.mochiladeemergencia.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Warning
import br.com.mochiladeemergencia.app.domain.model.StatusItem
import br.com.mochiladeemergencia.app.ui.icones.Icones
import br.com.mochiladeemergencia.app.ui.theme.LocalCoresStatus

/** Cartão de indicador do painel inicial. */
@Composable
fun CartaoEstatistica(
    valor: String,
    rotulo: String,
    icone: ImageVector,
    corContainer: Color,
    corConteudo: Color,
    descricaoAcessivel: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .heightIn(min = 104.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = descricaoAcessivel },
        shape = RoundedCornerShape(20.dp),
        color = corContainer,
        contentColor = corConteudo,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Icon(imageVector = icone, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(text = valor, style = MaterialTheme.typography.headlineSmall)
            Text(text = rotulo, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Selo colorido com o status do item: verde (tenho), amarelo (vencendo), vermelho (vencido), cinza (falta). */
@Composable
fun SeloStatus(status: StatusItem, modifier: Modifier = Modifier) {
    val cores = LocalCoresStatus.current
    val icone = when (status) {
        StatusItem.TENHO -> Icons.Filled.Check
        StatusItem.VENCENDO -> Icons.Filled.Warning
        StatusItem.VENCIDO -> Icons.Filled.Warning
        StatusItem.FALTA -> Icones.Menos
        StatusItem.REPOR -> Icones.Repor
    }
    Surface(
        modifier = modifier.semantics { contentDescription = "Status: ${status.rotulo}" },
        color = cores.container(status).copy(alpha = 0.65f),
        contentColor = cores.conteudo(status),
        border = BorderStroke(1.dp, cores.conteudo(status).copy(alpha = 0.45f)),
        shape = RoundedCornerShape(50)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(icone, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(status.rotulo, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Estado vazio explicativo, com ícone, título, descrição e ação opcional. */
@Composable
fun EstadoVazio(
    icone: ImageVector,
    titulo: String,
    descricao: String,
    modifier: Modifier = Modifier,
    acao: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    MaterialTheme.colorScheme.secondaryContainer,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = descricao,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (acao != null) {
            Spacer(Modifier.height(16.dp))
            acao()
        }
    }
}

/** Diálogo de confirmação obrigatório para exclusões e importações destrutivas. */
@Composable
fun DialogoConfirmacao(
    titulo: String,
    texto: String,
    rotuloConfirmar: String = "Confirmar",
    rotuloCancelar: String = "Cancelar",
    destrutivo: Boolean = false,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text(
                    text = rotuloConfirmar,
                    color = if (destrutivo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(rotuloCancelar) }
        }
    )
}

/** Cartão de seção com título opcional. */
@Composable
fun CartaoSecao(
    modifier: Modifier = Modifier,
    titulo: String? = null,
    conteudo: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (titulo != null) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
            }
            conteudo()
        }
    }
}

/** Botão de data com ação de limpar (usado na validade opcional). */
@Composable
fun BotaoData(
    rotulo: String,
    dataFormatada: String?,
    descricaoAcessivel: String,
    onEscolherData: () -> Unit,
    modifier: Modifier = Modifier,
    onLimparData: (() -> Unit)? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = onEscolherData,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp)
                .semantics { contentDescription = descricaoAcessivel }
        ) {
            Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = dataFormatada ?: rotulo,
                style = MaterialTheme.typography.bodyLarge,
                color = if (dataFormatada != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        if (onLimparData != null && dataFormatada != null) {
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onLimparData) { Text("Limpar") }
        }
    }
}

/** Linha com espaço entre dois lados. */
@Composable
fun LinhaEspacada(conteudo: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        content = { conteudo() }
    )
}

/**
 * Miniatura da foto do item (carregada do armazenamento privado do app, já reduzida).
 * Quando não há foto, mostra um marcador neutro com ícone de câmera.
 */
@Composable
fun MiniaturaFoto(
    nomeArquivo: String?,
    descricao: String,
    modifier: Modifier = Modifier,
    tamanho: Dp = 48.dp
) {
    val contexto = LocalContext.current
    // A decodificação acontece em background: antes ela rodava durante a composição e
    // travava a rolagem da lista (um JPEG por item, na thread principal).
    val bitmap by produceState<Bitmap?>(initialValue = null, nomeArquivo) {
        value = if (nomeArquivo == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                br.com.mochiladeemergencia.app.util.Fotos.miniatura(contexto, nomeArquivo, lado = 160)
            }
        }
    }
    Surface(
        modifier = modifier
            .size(tamanho)
            .semantics { contentDescription = descricao },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        val imagem = bitmap
        if (imagem != null) {
            Image(
                bitmap = imagem.asImageBitmap(),
                contentDescription = descricao,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icones.Camera,
                    contentDescription = null,
                    modifier = Modifier.size(tamanho * 0.5f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
