package br.com.mochiladeemergencia.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.mochiladeemergencia.app.ui.icones.Icones
import br.com.mochiladeemergencia.app.BuildConfig
import br.com.mochiladeemergencia.app.data.prefs.Configuracoes
import br.com.mochiladeemergencia.app.domain.StatusCalculadora
import br.com.mochiladeemergencia.app.domain.model.Tema
import br.com.mochiladeemergencia.app.ui.components.CartaoSecao
import br.com.mochiladeemergencia.app.ui.viewmodel.ConfiguracoesViewModel
import br.com.mochiladeemergencia.app.util.Notificacoes

@Composable
fun ConfiguracoesScreen(
    viewModel: ConfiguracoesViewModel,
    onAbrirPerfis: () -> Unit,
    onAbrirSobre: () -> Unit,
    onMensagem: (String) -> Unit
) {
    val config by viewModel.configuracoes.collectAsStateWithLifecycle()
    val perfilAtivo by viewModel.perfilAtivo.collectAsStateWithLifecycle()
    val ocupado by viewModel.ocupado.collectAsStateWithLifecycle()
    val importacaoPendente by viewModel.importacaoPendente.collectAsStateWithLifecycle()
    val contexto = LocalContext.current

    val exportador = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) viewModel.exportarPara(uri)
    }

    val importador = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) viewModel.prepararImportacao(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (ocupado) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Processando…", style = MaterialTheme.typography.bodyMedium)
            }
        }

        CartaoSecao(titulo = "Alertas de validade") {
            Text(
                text = "Avisar com antecedência de:",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusCalculadora.OPCOES_ANTECEDENCIA.forEach { dias ->
                    FilterChip(
                        selected = config.antecedenciaDias == dias,
                        onClick = { viewModel.definirAntecedencia(dias) },
                        label = { Text("$dias dias") }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Além do prazo escolhido, o app também avisa em 30, 7, 1 e 0 dias. " +
                    "Itens sem data de validade não geram avisos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Notifications, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Notificações", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Um resumo por dia, quando houver itens perto de vencer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = config.notificacoesAtivas,
                    onCheckedChange = viewModel::definirNotificacoes
                )
            }

            val permissaoOk = Notificacoes.permissaoConcedida(contexto)
            if (!permissaoOk) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "A permissão de notificações está desativada no sistema. " +
                        "O app continua funcionando e mostra os avisos no painel.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                TextButton(onClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName)
                    }
                    runCatching { contexto.startActivity(intent) }
                }) { Text("Abrir configurações de notificação") }
            }

            Spacer(Modifier.height(4.dp))
            OutlinedButton(onClick = viewModel::verificarValidadesAgora) {
                Text("Verificar validades agora")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icones.Repor, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Lembrete semestral", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "A cada 6 meses avisamos para revisar a mochila — até itens sem validade " +
                            "vencem ou enferrujam (pilha, repelente aberto, isqueiro, lona ressecada).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = config.lembreteSemestralAtivo,
                    onCheckedChange = viewModel::definirLembreteSemestral
                )
            }
        }

        CartaoSecao(titulo = "Casa") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pessoas na casa", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Usado apenas para sugerir quantidades no checklist.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = {
                        viewModel.definirPessoas(
                            (config.pessoasCasa - 1).coerceAtLeast(Configuracoes.PESSOAS_MINIMO)
                        )
                    }
                ) {
                    Icon(Icones.Menos, contentDescription = "Diminuir número de pessoas")
                }
                Text(
                    text = config.pessoasCasa.toString(),
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(
                    onClick = {
                        viewModel.definirPessoas(
                            (config.pessoasCasa + 1).coerceAtMost(Configuracoes.PESSOAS_MAXIMO)
                        )
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Aumentar número de pessoas")
                }
            }
        }

        CartaoSecao(titulo = "Aparência") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tema.entries.forEach { tema ->
                    FilterChip(
                        selected = config.tema == tema,
                        onClick = { viewModel.definirTema(tema) },
                        label = { Text(tema.rotulo) }
                    )
                }
            }
        }

        CartaoSecao(titulo = "Perfis e família") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Person, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Perfil ativo: ${perfilAtivo?.nome ?: "Casa"}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Crie perfis para separar mochilas e documentos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onAbrirPerfis) { Text("Gerenciar perfis") }
        }

        CartaoSecao(titulo = "Backup local (JSON)") {
            Text(
                text = "O backup inclui perfis, itens, documentos e configurações. " +
                    "Ele fica no local que você escolher — nada é enviado para a internet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { exportador.launch(viewModel.nomeArquivoBackup()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icones.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Exportar backup")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = viewModel::compartilharBackup,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Compartilhar backup")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    importador.launch(arrayOf("application/json", "text/plain", "*/*"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icones.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Importar backup")
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "A importação valida o arquivo antes de tocar nos seus dados. " +
                    "Arquivos de documentos de outro aparelho podem precisar ser escolhidos novamente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        CartaoSecao(titulo = "Sobre") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mochila de Emergência", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Versão ${BuildConfig.VERSION_NAME} · gratuito, offline e independente",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onAbrirSobre) { Text("Ver avisos e detalhes") }
        }

        Spacer(Modifier.height(24.dp))
    }

    importacaoPendente?.let { pendente ->
        AlertDialog(
            onDismissRequest = viewModel::cancelarImportacao,
            title = { Text("Backup válido") },
            text = {
                Text(
                    "Encontramos ${pendente.perfis} perfil(is), ${pendente.itens} item(ns), " +
                        "${pendente.documentos} documento(s), ${pendente.sugestoes} sugestão(ões) " +
                        "de checklist e ${pendente.contatos} contato(s).\n\n" +
                        "As fotos dos itens também são restauradas.\n\nEscolha como importar:\n" +
                        "• Mesclar: mantém os dados atuais e acrescenta os do arquivo.\n" +
                        "• Substituir: apaga os dados atuais do app (não pode ser desfeito)."
                )
            },
            confirmButton = {
                Row {
                    TextButton(onClick = { viewModel.confirmarImportacao(true) }) {
                        Text("Substituir", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { viewModel.confirmarImportacao(false) }) {
                        Text("Mesclar")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelarImportacao) { Text("Cancelar") }
            }
        )
    }
}
