package br.com.mochiladeemergencia.app.ui.screens

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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import br.com.mochiladeemergencia.app.ui.icones.Icones
import br.com.mochiladeemergencia.app.BuildConfig
import br.com.mochiladeemergencia.app.ui.components.CartaoSecao

@Composable
fun SobreScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icones.Mochila,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Mochila de Emergência", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Versão ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        CartaoSecao(titulo = "O que é este app") {
            Text(
                text = "Um aplicativo gratuito, offline e independente para ajudar famílias a " +
                    "montar, conferir e manter uma mochila de emergência. Sem login, sem anúncios, " +
                    "sem assinatura e sem coleta de dados pessoais.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Aviso de segurança", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Este app não substitui orientação profissional de emergência. " +
                        "Em caso de risco à vida, procure imediatamente os serviços de emergência " +
                        "da sua região.\n\n" +
                        "As orientações de primeiros socorros do app são informativas e não " +
                        "substituem treinamento prático nem atendimento médico profissional. " +
                        "Em qualquer emergência grave, ligue imediatamente para o SAMU (192) ou " +
                        "Bombeiros (193).\n\n" +
                        "O app não traz instruções de combate a incêndio nem procedimentos " +
                        "invasivos, e não faz chamadas: os botões apenas abrem o discador do " +
                        "aparelho. Você é responsável pelos conteúdos que adicionar.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        CartaoSecao(titulo = "Privacidade e permissões") {
            Text(
                text = "• Todos os dados ficam no seu aparelho, em um banco local.\n" +
                    "• Nenhuma permissão de internet é usada: o app não acessa a rede.\n" +
                    "• Permissão de notificações (Android 13+): usada apenas para os avisos de " +
                    "validade e para o lembrete semestral.\n" +
                    "• Câmera e galeria funcionam sem permissões de mídia (Photo Picker do " +
                    "sistema e câmera via app externo).\n" +
                    "• As fotos dos itens ficam no armazenamento privado do app.\n" +
                    "• Nenhuma permissão de telefone: os contatos apenas abrem o discador.\n" +
                    "• O backup é manual e fica onde você escolher salvar.\n" +
                    "• Sem analytics, telemetria, anúncios ou bibliotecas de rastreamento.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        CartaoSecao(titulo = "Como usar em 5 passos") {
            Text(
                text = "1. Abra o Checklist e adicione as sugestões básicas à sua mochila.\n" +
                    "2. Ajuste quantidades, locais e datas de validade na tela Minha Mochila.\n" +
                    "3. Salve documentos e links úteis na tela Documentos.\n" +
                    "4. Confira a mochila de tempos em tempos com a auditoria da tela de " +
                    "Conferir Mochila.\n" +
                    "5. Faça um backup de vez em quando em Ajustes → Backup local.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Text(
            text = "Desenvolvido como projeto independente. Código aberto para conferência: " +
                "veja o README do projeto.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )
        Spacer(Modifier.height(24.dp))
    }
}
