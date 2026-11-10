package br.com.mochiladeemergencia.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.com.mochiladeemergencia.app.domain.StatusCalculadora
import br.com.mochiladeemergencia.app.domain.model.Tema
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Preferências do usuário (DataStore). Nada é enviado para fora do aparelho. */
data class Configuracoes(
    val antecedenciaDias: Int = StatusCalculadora.ANTECEDENCIA_PADRAO,
    val notificacoesAtivas: Boolean = true,
    val pessoasCasa: Int = 1,
    val tema: Tema = Tema.SISTEMA,
    /** 0L indica "ainda não escolhido": o app assume o primeiro perfil disponível. */
    val perfilAtivoId: Long = 0L,
    /** Lembrete semestral de revisão da mochila (padrão: ativado). */
    val lembreteSemestralAtivo: Boolean = true,
    /** Controle interno de anti-duplicação das notificações. */
    val ultimoAvisoDia: Long = -1L,
    val ultimoAvisoHash: Int = 0
) {
    companion object {
        const val PESSOAS_MINIMO = 1
        const val PESSOAS_MAXIMO = 20
    }
}

/** Contrato de armazenamento das configurações (permite testes com implementação em memória). */
interface ArmazenamentoConfiguracoes {
    val configuracoes: Flow<Configuracoes>
    suspend fun aplicarConfiguracoesImportadas(
        antecedenciaDias: Int?,
        notificacoesAtivas: Boolean?,
        pessoasCasa: Int?,
        tema: Tema?,
        lembreteSemestralAtivo: Boolean? = null
    )
    suspend fun definirPerfilAtivo(perfilId: Long)
}

private val Context.dataStoreDaMochila: DataStore<Preferences> by preferencesDataStore(
    name = "mochila_config"
)

class PreferenciasRepository(private val context: Context) : ArmazenamentoConfiguracoes {

    private object Chaves {
        val ANTECEDENCIA = intPreferencesKey("antecedencia_dias")
        val NOTIFICACOES = booleanPreferencesKey("notificacoes_ativas")
        val PESSOAS = intPreferencesKey("pessoas_casa")
        val TEMA = stringPreferencesKey("tema")
        val PERFIL_ATIVO = longPreferencesKey("perfil_ativo_id")
        val LEMBRETE_SEMESTRAL = booleanPreferencesKey("lembrete_semestral")
        val ULTIMO_AVISO_DIA = longPreferencesKey("ultimo_aviso_dia")
        val ULTIMO_AVISO_HASH = intPreferencesKey("ultimo_aviso_hash")
    }

    override val configuracoes: Flow<Configuracoes> = context.dataStoreDaMochila.data
        .catch { erro ->
            // Em caso de leitura corrompida, usa os padrões em vez de derrubar o app.
            if (erro is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw erro
        }
        .map { prefs ->
            Configuracoes(
                antecedenciaDias = prefs[Chaves.ANTECEDENCIA]
                    ?.takeIf { it in StatusCalculadora.OPCOES_ANTECEDENCIA }
                    ?: StatusCalculadora.ANTECEDENCIA_PADRAO,
                notificacoesAtivas = prefs[Chaves.NOTIFICACOES] ?: true,
                pessoasCasa = (prefs[Chaves.PESSOAS] ?: 1)
                    .coerceIn(Configuracoes.PESSOAS_MINIMO, Configuracoes.PESSOAS_MAXIMO),
                tema = Tema.dePersistido(prefs[Chaves.TEMA]),
                perfilAtivoId = prefs[Chaves.PERFIL_ATIVO] ?: 0L,
                lembreteSemestralAtivo = prefs[Chaves.LEMBRETE_SEMESTRAL] ?: true,
                ultimoAvisoDia = prefs[Chaves.ULTIMO_AVISO_DIA] ?: -1L,
                ultimoAvisoHash = prefs[Chaves.ULTIMO_AVISO_HASH] ?: 0
            )
        }

    suspend fun definirAntecedencia(dias: Int) {
        val valor = if (dias in StatusCalculadora.OPCOES_ANTECEDENCIA) {
            dias
        } else {
            StatusCalculadora.ANTECEDENCIA_PADRAO
        }
        context.dataStoreDaMochila.edit { it[Chaves.ANTECEDENCIA] = valor }
    }

    suspend fun definirNotificacoes(ativas: Boolean) {
        context.dataStoreDaMochila.edit { it[Chaves.NOTIFICACOES] = ativas }
    }

    suspend fun definirLembreteSemestral(ativo: Boolean) {
        context.dataStoreDaMochila.edit { it[Chaves.LEMBRETE_SEMESTRAL] = ativo }
    }

    suspend fun definirPessoasCasa(pessoas: Int) {
        context.dataStoreDaMochila.edit {
            it[Chaves.PESSOAS] = pessoas.coerceIn(
                Configuracoes.PESSOAS_MINIMO,
                Configuracoes.PESSOAS_MAXIMO
            )
        }
    }

    suspend fun definirTema(tema: Tema) {
        context.dataStoreDaMochila.edit { it[Chaves.TEMA] = tema.name }
    }

    override suspend fun definirPerfilAtivo(perfilId: Long) {
        context.dataStoreDaMochila.edit { it[Chaves.PERFIL_ATIVO] = perfilId }
    }

    /** Registra que um aviso foi emitido hoje (para não repetir no mesmo dia). */
    suspend fun registrarAviso(diaEpoch: Long, hashConteudo: Int) {
        context.dataStoreDaMochila.edit {
            it[Chaves.ULTIMO_AVISO_DIA] = diaEpoch
            it[Chaves.ULTIMO_AVISO_HASH] = hashConteudo
        }
    }

    /** Aplica configurações vindas de um backup, validando cada valor. */
    override suspend fun aplicarConfiguracoesImportadas(
        antecedenciaDias: Int?,
        notificacoesAtivas: Boolean?,
        pessoasCasa: Int?,
        tema: Tema?,
        lembreteSemestralAtivo: Boolean?
    ) {
        context.dataStoreDaMochila.edit { prefs ->
            if (antecedenciaDias != null && antecedenciaDias in StatusCalculadora.OPCOES_ANTECEDENCIA) {
                prefs[Chaves.ANTECEDENCIA] = antecedenciaDias
            }
            if (notificacoesAtivas != null) prefs[Chaves.NOTIFICACOES] = notificacoesAtivas
            if (pessoasCasa != null) {
                prefs[Chaves.PESSOAS] = pessoasCasa.coerceIn(
                    Configuracoes.PESSOAS_MINIMO,
                    Configuracoes.PESSOAS_MAXIMO
                )
            }
            if (tema != null) prefs[Chaves.TEMA] = tema.name
            if (lembreteSemestralAtivo != null) {
                prefs[Chaves.LEMBRETE_SEMESTRAL] = lembreteSemestralAtivo
            }
        }
    }
}
