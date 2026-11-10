package br.com.mochiladeemergencia.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import br.com.mochiladeemergencia.app.ui.navigation.TelaPrincipal
import br.com.mochiladeemergencia.app.ui.viewmodel.FabricaViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val servicos = (application as MochilaApp).servicos

        setContent {
            val fabrica = remember { FabricaViewModel(servicos) }
            TelaPrincipal(servicos = servicos, fabrica = fabrica)
        }
    }
}
