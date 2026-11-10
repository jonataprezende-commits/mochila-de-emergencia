package br.com.mochiladeemergencia.app.ui.navigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.mochiladeemergencia.app.di.ServicosLocais
import br.com.mochiladeemergencia.app.ui.icones.Icones
import br.com.mochiladeemergencia.app.ui.screens.ChecklistScreen
import br.com.mochiladeemergencia.app.ui.screens.ConferirScreen
import br.com.mochiladeemergencia.app.ui.screens.ConfiguracoesScreen
import br.com.mochiladeemergencia.app.ui.screens.ContatosScreen
import br.com.mochiladeemergencia.app.ui.screens.DocumentoFormScreen
import br.com.mochiladeemergencia.app.ui.screens.DocumentosScreen
import br.com.mochiladeemergencia.app.ui.screens.EmergenciaScreen
import br.com.mochiladeemergencia.app.ui.screens.InicioScreen
import br.com.mochiladeemergencia.app.ui.screens.ItemFormScreen
import br.com.mochiladeemergencia.app.ui.screens.MochilaScreen
import br.com.mochiladeemergencia.app.ui.screens.PerfisScreen
import br.com.mochiladeemergencia.app.ui.screens.PrimeirosSocorrosScreen
import br.com.mochiladeemergencia.app.ui.screens.ReporScreen
import br.com.mochiladeemergencia.app.ui.screens.SobreScreen
import br.com.mochiladeemergencia.app.ui.theme.TemaMochila
import br.com.mochiladeemergencia.app.ui.viewmodel.ConfiguracoesViewModel
import br.com.mochiladeemergencia.app.ui.viewmodel.DocumentosViewModel
import br.com.mochiladeemergencia.app.ui.viewmodel.InicioViewModel
import br.com.mochiladeemergencia.app.ui.viewmodel.MochilaViewModel
import br.com.mochiladeemergencia.app.ui.viewmodel.FabricaViewModel
import kotlinx.coroutines.launch

object Rotas {
    const val INICIO = "inicio"
    const val MOCHILA = "mochila"
    const val DOCUMENTOS = "documentos"
    const val EMERGENCIA = "emergencia"
    const val AJUSTES = "ajustes"
    const val CHECKLIST = "checklist"
    const val CONFERIR = "conferir"
    const val REPOR = "repor"
    const val PRIMEIROS_SOCORROS = "primeiros_socorros"
    const val CONTATOS = "contatos"
    const val PERFIS = "perfis"
    const val SOBRE = "sobre"
    const val ITEM_FORM = "item_form?itemId={itemId}"
    const val DOC_FORM = "doc_form?docId={docId}"

    fun itemForm(itemId: Long = 0L) = "item_form?itemId=$itemId"
    fun docForm(docId: Long = 0L) = "doc_form?docId=$docId"
}

private data class Aba(val rota: String, val rotulo: String, val icone: ImageVector)

private val abas = listOf(
    Aba(Rotas.INICIO, "Início", Icons.Filled.Home),
    Aba(Rotas.MOCHILA, "Mochila", Icones.Mochila),
    Aba(Rotas.DOCUMENTOS, "Documentos", Icones.Documento),
    Aba(Rotas.EMERGENCIA, "Emergência", Icones.Cruz),
    Aba(Rotas.AJUSTES, "Ajustes", Icons.Filled.Settings)
)

/**
 * Rótulo da barra inferior em **uma única linha**.
 *
 * Antes, "Documentos" quebrava no meio da palavra (o "s" caía para a linha de baixo)
 * em telas estreitas ou com fonte grande. Agora o texto não quebra: o tamanho da fonte
 * diminui automaticamente até caber.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
private fun RotuloAba(rotulo: String) {
    Text(
        text = rotulo,
        maxLines = 1,
        softWrap = false,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelMedium,
        autoSize = TextAutoSize.StepBased(
            minFontSize = 8.sp,
            maxFontSize = 12.sp,
            stepSize = 0.5.sp
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaPrincipal(servicos: ServicosLocais, fabrica: FabricaViewModel) {
    val contexto = LocalContext.current
    val vmConfig: ConfiguracoesViewModel = viewModel(factory = fabrica)
    val config by vmConfig.configuracoes.collectAsStateWithLifecycle()
    val mensagem by vmConfig.mensagem.collectAsStateWithLifecycle()

    // ViewModels das abas principais vivem enquanto a Activity viver: trocar de aba não
    // recria o ViewModel nem reabre as consultas (antes a tela piscava "carregando").
    val vmInicio: InicioViewModel = viewModel(factory = fabrica)
    val vmMochila: MochilaViewModel = viewModel(factory = fabrica)
    val vmDocumentos: DocumentosViewModel = viewModel(factory = fabrica)

    val navController = rememberNavController()
    val entradaAtual by navController.currentBackStackEntryAsState()
    val rotaAtual = entradaAtual?.destination?.route
    val snackbar = remember { SnackbarHostState() }
    val escopo = rememberCoroutineScope()

    val emTelaPrincipal = rotaAtual in abas.map { it.rota }
    val mostrarFabItem = rotaAtual == Rotas.INICIO || rotaAtual == Rotas.MOCHILA ||
        rotaAtual == Rotas.REPOR
    val mostrarFabDocumento = rotaAtual == Rotas.DOCUMENTOS

    // Permissão de notificação (Android 13+). Se negada, o app segue funcionando.
    val lancadorPermissao = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* negada ou concedida: os avisos também aparecem no painel */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            config.notificacoesAtivas &&
            !temPermissaoNotificacao(contexto)
        ) {
            lancadorPermissao.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(mensagem) {
        mensagem?.let {
            snackbar.showSnackbar(it)
            vmConfig.consumirMensagem()
        }
    }

    TemaMochila(tema = config.tema) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(tituloDaRota(rotaAtual)) },
                    navigationIcon = {
                        if (!emTelaPrincipal) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Voltar"
                                )
                            }
                        }
                    },
                    actions = {
                        if (rotaAtual == Rotas.INICIO) {
                            IconButton(onClick = { navController.navigate(Rotas.PERFIS) }) {
                                Icon(Icons.Filled.Person, contentDescription = "Perfis e família")
                            }
                        }
                        if (rotaAtual == Rotas.MOCHILA) {
                            IconButton(onClick = { navController.navigate(Rotas.CHECKLIST) }) {
                                Icon(Icones.Checklist, contentDescription = "Abrir checklist")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                if (emTelaPrincipal) {
                    NavigationBar {
                        abas.forEach { aba ->
                            val selecionada = entradaAtual?.destination?.hierarchy
                                ?.any { it.route == aba.rota } == true
                            NavigationBarItem(
                                selected = selecionada,
                                onClick = {
                                    navController.navigate(aba.rota) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(aba.icone, contentDescription = aba.rotulo) },
                                label = { RotuloAba(aba.rotulo) }
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (mostrarFabItem) {
                    ExtendedFloatingActionButton(
                        onClick = { navController.navigate(Rotas.itemForm()) },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text("Adicionar item") }
                    )
                } else if (mostrarFabDocumento) {
                    ExtendedFloatingActionButton(
                        onClick = { navController.navigate(Rotas.docForm()) },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text("Adicionar documento") }
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbar) }
        ) { espacamento ->
            NavHost(
                navController = navController,
                startDestination = Rotas.INICIO,
                modifier = Modifier.padding(espacamento),
                // Transições curtas: a tela entra deslizando de leve e some em fade.
                enterTransition = {
                    fadeIn(animationSpec = tween(180)) +
                        slideInHorizontally(
                            animationSpec = tween(220, easing = FastOutSlowInEasing),
                            initialOffsetX = { largura -> largura / 16 }
                        )
                },
                exitTransition = { fadeOut(animationSpec = tween(120)) },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(180)) +
                        slideInHorizontally(
                            animationSpec = tween(220, easing = FastOutSlowInEasing),
                            initialOffsetX = { largura -> -largura / 16 }
                        )
                },
                popExitTransition = { fadeOut(animationSpec = tween(120)) }
            ) {
                composable(Rotas.INICIO) {
                    InicioScreen(
                        viewModel = vmInicio,
                        onAdicionarItem = { navController.navigate(Rotas.itemForm()) },
                        onAbrirChecklist = { navController.navigate(Rotas.CHECKLIST) },
                        onAbrirPerfis = { navController.navigate(Rotas.PERFIS) },
                        onAbrirDocumentos = { navController.navegarAba(Rotas.DOCUMENTOS) },
                        onAbrirMochila = { navController.navegarAba(Rotas.MOCHILA) },
                        onAbrirAjustes = { navController.navegarAba(Rotas.AJUSTES) },
                        onAbrirConferir = { navController.navigate(Rotas.CONFERIR) },
                        onAbrirRepor = { navController.navigate(Rotas.REPOR) },
                        onAbrirPrimeirosSocorros = { navController.navigate(Rotas.PRIMEIROS_SOCORROS) },
                        onAbrirContatos = { navController.navigate(Rotas.CONTATOS) },
                        onMensagem = { texto -> escopo.launch { snackbar.showSnackbar(texto) } }
                    )
                }
                composable(Rotas.MOCHILA) {
                    MochilaScreen(
                        viewModel = vmMochila,
                        onEditar = { itemId -> navController.navigate(Rotas.itemForm(itemId)) },
                        onAdicionar = { navController.navigate(Rotas.itemForm()) },
                        onAbrirChecklist = { navController.navigate(Rotas.CHECKLIST) }
                    )
                }
                composable(Rotas.DOCUMENTOS) {
                    DocumentosScreen(
                        viewModel = vmDocumentos,
                        onAdicionar = { navController.navigate(Rotas.docForm()) },
                        onEditar = { docId -> navController.navigate(Rotas.docForm(docId)) },
                        onMensagem = { texto -> escopo.launch { snackbar.showSnackbar(texto) } }
                    )
                }
                composable(Rotas.EMERGENCIA) {
                    EmergenciaScreen(
                        onAbrirPrimeirosSocorros = { navController.navigate(Rotas.PRIMEIROS_SOCORROS) },
                        onAbrirContatos = { navController.navigate(Rotas.CONTATOS) },
                        onAbrirDocumentos = { navController.navegarAba(Rotas.DOCUMENTOS) },
                        onMensagem = { texto -> escopo.launch { snackbar.showSnackbar(texto) } }
                    )
                }
                composable(Rotas.AJUSTES) {
                    ConfiguracoesScreen(
                        viewModel = vmConfig,
                        onAbrirPerfis = { navController.navigate(Rotas.PERFIS) },
                        onAbrirSobre = { navController.navigate(Rotas.SOBRE) },
                        onMensagem = { texto -> escopo.launch { snackbar.showSnackbar(texto) } }
                    )
                }
                composable(Rotas.CHECKLIST) {
                    ChecklistScreen(vm = viewModel(factory = fabrica))
                }
                composable(Rotas.CONFERIR) {
                    ConferirScreen(
                        viewModel = viewModel(factory = fabrica),
                        onVoltarParaMochila = {
                            navController.navegarAba(Rotas.MOCHILA)
                        },
                        onAbrirRepor = { navController.navigate(Rotas.REPOR) }
                    )
                }
                composable(Rotas.REPOR) {
                    ReporScreen(
                        viewModel = viewModel(factory = fabrica),
                        onIrParaMochila = { navController.navegarAba(Rotas.MOCHILA) },
                        onEditarItem = { itemId -> navController.navigate(Rotas.itemForm(itemId)) }
                    )
                }
                composable(Rotas.PRIMEIROS_SOCORROS) {
                    PrimeirosSocorrosScreen(
                        onSalvarLink = { navController.navegarAba(Rotas.DOCUMENTOS) },
                        onMensagem = { texto -> escopo.launch { snackbar.showSnackbar(texto) } }
                    )
                }
                composable(Rotas.CONTATOS) {
                    ContatosScreen(
                        viewModel = viewModel(factory = fabrica),
                        onMensagem = { texto -> escopo.launch { snackbar.showSnackbar(texto) } }
                    )
                }
                composable(Rotas.PERFIS) {
                    PerfisScreen(viewModel = vmConfig)
                }
                composable(Rotas.SOBRE) {
                    SobreScreen()
                }
                composable(
                    route = Rotas.ITEM_FORM,
                    arguments = listOf(
                        navArgument("itemId") {
                            type = NavType.LongType
                            defaultValue = 0L
                        }
                    )
                ) { entrada ->
                    ItemFormScreen(
                        viewModel = viewModel(factory = fabrica),
                        itemId = entrada.arguments?.getLong("itemId") ?: 0L,
                        onVoltar = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Rotas.DOC_FORM,
                    arguments = listOf(
                        navArgument("docId") {
                            type = NavType.LongType
                            defaultValue = 0L
                        }
                    )
                ) { entrada ->
                    DocumentoFormScreen(
                        viewModel = viewModel(factory = fabrica),
                        documentoId = entrada.arguments?.getLong("docId") ?: 0L,
                        onVoltar = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

/** Navega para uma aba da barra inferior preservando o estado. */
private fun androidx.navigation.NavHostController.navegarAba(rota: String) {
    navigate(rota) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun temPermissaoNotificacao(contexto: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            contexto,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

private fun tituloDaRota(rota: String?): String = when {
    rota == Rotas.INICIO -> "Início"
    rota == Rotas.MOCHILA -> "Minha Mochila"
    rota == Rotas.DOCUMENTOS -> "Documentos"
    rota == Rotas.EMERGENCIA -> "Emergência"
    rota == Rotas.AJUSTES -> "Ajustes"
    rota == Rotas.CHECKLIST -> "Checklist"
    rota == Rotas.CONFERIR -> "Conferir mochila"
    rota == Rotas.REPOR -> "Repor itens"
    rota == Rotas.PRIMEIROS_SOCORROS -> "Primeiros socorros"
    rota == Rotas.CONTATOS -> "Contatos de emergência"
    rota == Rotas.PERFIS -> "Perfis / Família"
    rota == Rotas.SOBRE -> "Sobre o app"
    rota?.startsWith("item_form") == true -> "Item da mochila"
    rota?.startsWith("doc_form") == true -> "Documento"
    else -> "Mochila de Emergência"
}
