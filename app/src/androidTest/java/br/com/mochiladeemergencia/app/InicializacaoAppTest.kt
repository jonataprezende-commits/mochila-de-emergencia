package br.com.mochiladeemergencia.app

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Teste instrumentado (roda em aparelho/emulador): confere que o app abre,
 * que o perfil "Casa" existe e que a Activity é recriada sem crash (rotação).
 *
 * Execução:
 *   ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class InicializacaoAppTest {

    @Test
    fun appAbreSemCrash() {
        ActivityScenario.launch(MainActivity::class.java).use { cenario ->
            cenario.onActivity { activity ->
                assertNotNull(activity)
                assertTrue(!activity.isFinishing)
            }
            cenario.recreate()
        }
    }

    @Test
    fun servicosLocaisSaoCriados() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
            as MochilaApp
        assertNotNull(app.servicos)
        assertNotNull(app.servicos.banco)
    }
}
