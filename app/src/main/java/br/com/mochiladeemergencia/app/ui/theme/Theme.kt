package br.com.mochiladeemergencia.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import br.com.mochiladeemergencia.app.domain.model.StatusItem
import br.com.mochiladeemergencia.app.domain.model.Tema

/** Cores de status com contraste AA nos temas claro e escuro. */
@Immutable
data class CoresStatus(
    val tenho: Color,
    val sobreTenho: Color,
    val vencendo: Color,
    val sobreVencendo: Color,
    val vencido: Color,
    val sobreVencido: Color,
    val falta: Color,
    val sobreFalta: Color,
    val repor: Color,
    val sobreRepor: Color
) {
    fun container(status: StatusItem): Color = when (status) {
        StatusItem.TENHO -> tenho
        StatusItem.VENCENDO -> vencendo
        StatusItem.VENCIDO -> vencido
        StatusItem.FALTA -> falta
        StatusItem.REPOR -> repor
    }

    fun conteudo(status: StatusItem): Color = when (status) {
        StatusItem.TENHO -> sobreTenho
        StatusItem.VENCENDO -> sobreVencendo
        StatusItem.VENCIDO -> sobreVencido
        StatusItem.FALTA -> sobreFalta
        StatusItem.REPOR -> sobreRepor
    }
}

private val CoresStatusClaro = CoresStatus(
    tenho = Color(0xFFC6EDD5),
    sobreTenho = Color(0xFF0A4D2C),
    vencendo = Color(0xFFFFEFC2),
    sobreVencendo = Color(0xFF5C4300),
    vencido = Color(0xFFFFDAD6),
    sobreVencido = Color(0xFF7F1D18),
    falta = Color(0xFFE6E9E6),
    sobreFalta = Color(0xFF3A403C),
    repor = Color(0xFFD7E7F7),
    sobreRepor = Color(0xFF123A5C)
)

private val CoresStatusEscuro = CoresStatus(
    tenho = Color(0xFF153722),
    sobreTenho = Color(0xFFA9F2C7),
    vencendo = Color(0xFF3B2F00),
    sobreVencendo = Color(0xFFFFE08A),
    vencido = Color(0xFF4E1512),
    sobreVencido = Color(0xFFFFB4AB),
    falta = Color(0xFF2A2F2B),
    sobreFalta = Color(0xFFD3D8D3),
    repor = Color(0xFF13293D),
    sobreRepor = Color(0xFFBBD9F5)
)

val LocalCoresStatus = staticCompositionLocalOf { CoresStatusClaro }

private val EsquemaClaro = lightColorScheme(
    primary = Color(0xFF1F6F4A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB4EECD),
    onPrimaryContainer = Color(0xFF00210F),
    secondary = Color(0xFF4F6354),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD2E8D6),
    onSecondaryContainer = Color(0xFF0C1F13),
    tertiary = Color(0xFF3B6470),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBFE9F5),
    onTertiaryContainer = Color(0xFF001F26),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBFDF8),
    onBackground = Color(0xFF191C19),
    surface = Color(0xFFFBFDF8),
    onSurface = Color(0xFF191C19),
    surfaceVariant = Color(0xFFDCE5DB),
    onSurfaceVariant = Color(0xFF404943),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F8F2),
    surfaceContainer = Color(0xFFEFF3EC),
    surfaceContainerHigh = Color(0xFFE9EDE6),
    surfaceContainerHighest = Color(0xFFE3E8E0),
    outline = Color(0xFF707972),
    outlineVariant = Color(0xFFC0C9C1)
)

private val EsquemaEscuro = darkColorScheme(
    primary = Color(0xFF8CD5A9),
    onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF155232),
    onPrimaryContainer = Color(0xFFA8F2C6),
    secondary = Color(0xFFB6CCB9),
    onSecondary = Color(0xFF213527),
    secondaryContainer = Color(0xFF374B3D),
    onSecondaryContainer = Color(0xFFD2E8D6),
    tertiary = Color(0xFFA3CDDA),
    onTertiary = Color(0xFF053541),
    tertiaryContainer = Color(0xFF224C58),
    onTertiaryContainer = Color(0xFFBFE9F5),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF11140F),
    onBackground = Color(0xFFE1E3DD),
    surface = Color(0xFF11140F),
    onSurface = Color(0xFFE1E3DD),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFC0C9C1),
    surfaceContainerLowest = Color(0xFF0C0F0B),
    surfaceContainerLow = Color(0xFF191D18),
    surfaceContainer = Color(0xFF1D211C),
    surfaceContainerHigh = Color(0xFF272B26),
    surfaceContainerHighest = Color(0xFF323631),
    outline = Color(0xFF8A938C),
    outlineVariant = Color(0xFF404943)
)

/** Tipografia com pesos claros e foco em legibilidade; respeita a escala de fonte do sistema. */
private val Tipografia = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp)
)

@Composable
fun TemaMochila(tema: Tema, conteudo: @Composable () -> Unit) {
    val escuroDoSistema = isSystemInDarkTheme()
    val escuro = when (tema) {
        Tema.CLARO -> false
        Tema.ESCURO -> true
        Tema.SISTEMA -> escuroDoSistema
    }
    CompositionLocalProvider(
        LocalCoresStatus provides if (escuro) CoresStatusEscuro else CoresStatusClaro
    ) {
        MaterialTheme(
            colorScheme = if (escuro) EsquemaEscuro else EsquemaClaro,
            typography = Tipografia,
            content = conteudo
        )
    }
}
