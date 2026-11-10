package br.com.mochiladeemergencia.app.ui.viewmodel

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mochiladeemergencia.app.data.backup.ArquivoBackup
import br.com.mochiladeemergencia.app.data.backup.BackupInvalidoException
import br.com.mochiladeemergencia.app.data.backup.BackupRepository
import br.com.mochiladeemergencia.app.data.backup.BackupValidado
import br.com.mochiladeemergencia.app.data.local.entity.PerfilEntity
import br.com.mochiladeemergencia.app.data.prefs.Configuracoes
import br.com.mochiladeemergencia.app.data.repository.ResultadoExclusaoPerfil
import br.com.mochiladeemergencia.app.di.ServicosLocais
import br.com.mochiladeemergencia.app.domain.model.Tema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Backup já validado, aguardando a confirmação do usuário para ser importado. */
data class ImportacaoPendente(
    val backup: BackupValidado,
    val perfis: Int,
    val itens: Int,
    val documentos: Int,
    val sugestoes: Int,
    val contatos: Int
)

/** Configurações, perfis/família e backup. */
class ConfiguracoesViewModel(private val servicos: ServicosLocais) : ViewModel() {

    val configuracoes: StateFlow<Configuracoes> = servicos.preferencias.configuracoes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Configuracoes()
    )

    val perfis: StateFlow<List<PerfilEntity>> = servicos.perfilRepository.perfis.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val perfilAtivo: StateFlow<PerfilEntity?> =
        combine(perfis, configuracoes) { lista, config ->
            lista.firstOrNull { it.id == config.perfilAtivoId } ?: lista.firstOrNull()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val _mensagem = MutableStateFlow<String?>(null)
    val mensagem: StateFlow<String?> = _mensagem.asStateFlow()

    private val _ocupado = MutableStateFlow(false)
    val ocupado: StateFlow<Boolean> = _ocupado.asStateFlow()

    private val _importacaoPendente = MutableStateFlow<ImportacaoPendente?>(null)
    val importacaoPendente: StateFlow<ImportacaoPendente?> = _importacaoPendente.asStateFlow()

    // ------------------------------ preferências ------------------------------

    fun definirAntecedencia(dias: Int) {
        viewModelScope.launch {
            servicos.preferencias.definirAntecedencia(dias)
            servicos.agendadorAvisos.agendarVerificacaoDiaria()
        }
    }

    fun definirNotificacoes(ativas: Boolean) {
        viewModelScope.launch {
            servicos.preferencias.definirNotificacoes(ativas)
            if (ativas) {
                servicos.agendadorAvisos.agendarVerificacaoDiaria()
                servicos.agendadorAvisos.verificarAgora()
            }
        }
    }

    fun definirLembreteSemestral(ativo: Boolean) {
        viewModelScope.launch {
            servicos.preferencias.definirLembreteSemestral(ativo)
            servicos.agendadorAvisos.configurarLembreteSemestral(ativo)
            _mensagem.value = if (ativo) {
                "Lembrete semestral ativado."
            } else {
                "Lembrete semestral desativado."
            }
        }
    }

    fun definirPessoas(pessoas: Int) {
        viewModelScope.launch { servicos.preferencias.definirPessoasCasa(pessoas) }
    }

    fun definirTema(tema: Tema) {
        viewModelScope.launch { servicos.preferencias.definirTema(tema) }
    }

    fun verificarValidadesAgora() {
        servicos.agendadorAvisos.verificarAgora()
        _mensagem.value = "Verificação de validade solicitada."
    }

    // --------------------------------- perfis ---------------------------------

    fun ativarPerfil(perfilId: Long) {
        viewModelScope.launch { servicos.preferencias.definirPerfilAtivo(perfilId) }
    }

    fun criarPerfil(nome: String, aoConcluir: (Long) -> Unit = {}) {
        val limpo = nome.trim()
        if (limpo.isBlank()) {
            _mensagem.value = "Informe o nome do perfil."
            return
        }
        viewModelScope.launch {
            val id = servicos.perfilRepository.criar(limpo)
            servicos.preferencias.definirPerfilAtivo(id)
            _mensagem.value = "Perfil \"$limpo\" criado."
            aoConcluir(id)
        }
    }

    fun renomearPerfil(perfil: PerfilEntity, novoNome: String, aoConcluir: () -> Unit = {}) {
        val limpo = novoNome.trim()
        if (limpo.isBlank()) {
            _mensagem.value = "Informe o nome do perfil."
            return
        }
        viewModelScope.launch {
            servicos.perfilRepository.renomear(perfil, limpo)
            _mensagem.value = "Perfil renomeado."
            aoConcluir()
        }
    }

    fun excluirPerfil(perfil: PerfilEntity) {
        viewModelScope.launch {
            // Antes da exclusão em cascata no banco, apaga as fotos dos itens do perfil.
            servicos.itemRepository.listar(perfil.id).forEach { item ->
                br.com.mochiladeemergencia.app.util.Fotos.excluir(servicos.contexto, item.fotoArquivo)
            }
            when (val resultado = servicos.perfilRepository.excluir(perfil)) {
                is ResultadoExclusaoPerfil.UltimoPerfil -> {
                    _mensagem.value = "Não é possível excluir o único perfil existente."
                }
                is ResultadoExclusaoPerfil.Sucesso -> {
                    val config = servicos.preferencias.configuracoes.first()
                    if (config.perfilAtivoId == perfil.id) {
                        resultado.novoPerfilAtivoId?.let {
                            servicos.preferencias.definirPerfilAtivo(it)
                        }
                    }
                    _mensagem.value = "Perfil \"${perfil.nome}\" excluído."
                }
            }
        }
    }

    // --------------------------------- backup ---------------------------------

    fun exportarPara(uri: Uri) {
        viewModelScope.launch {
            _ocupado.value = true
            try {
                val json = servicos.backupRepository.exportarJson()
                ArquivoBackup.salvarEmUri(servicos.contexto, uri, json)
                _mensagem.value = "Backup exportado com sucesso."
            } catch (erro: BackupInvalidoException) {
                // Ex.: backup maior que o limite por causa das fotos.
                _mensagem.value = erro.message ?: "Não foi possível exportar o backup."
            } catch (erro: Exception) {
                _mensagem.value = "Não foi possível exportar o backup. Tente novamente."
            } finally {
                _ocupado.value = false
            }
        }
    }

    fun compartilharBackup() {
        viewModelScope.launch {
            _ocupado.value = true
            try {
                val json = servicos.backupRepository.exportarJson()
                val intent = ArquivoBackup.criarIntentCompartilhar(servicos.contexto, json)
                val chooser = Intent.createChooser(intent, "Compartilhar backup").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                servicos.contexto.startActivity(chooser)
            } catch (erro: BackupInvalidoException) {
                _mensagem.value = erro.message ?: "Não foi possível compartilhar o backup."
            } catch (erro: Exception) {
                _mensagem.value = "Não foi possível compartilhar o backup."
            } finally {
                _ocupado.value = false
            }
        }
    }

    /** Lê e valida o arquivo escolhido. Nada é gravado antes da confirmação. */
    fun prepararImportacao(uri: Uri) {
        viewModelScope.launch {
            _ocupado.value = true
            try {
                val texto = ArquivoBackup.lerTexto(
                    servicos.contexto,
                    uri,
                    BackupRepository.TAMANHO_MAXIMO_BYTES
                )
                val backup = withContext(Dispatchers.Default) {
                    servicos.backupRepository.validar(texto)
                }
                _importacaoPendente.value = ImportacaoPendente(
                    backup = backup,
                    perfis = backup.perfis.size,
                    itens = backup.perfis.sumOf { it.itens.size },
                    documentos = backup.perfis.sumOf { it.documentos.size },
                    contatos = backup.perfis.sumOf { it.contatos.size },
                    sugestoes = backup.perfis.sumOf { it.checklist.size }
                )
            } catch (erro: BackupInvalidoException) {
                _mensagem.value = erro.message ?: "Backup inválido."
            } catch (erro: Exception) {
                _mensagem.value = "Não foi possível ler o arquivo escolhido."
            } finally {
                _ocupado.value = false
            }
        }
    }

    /**
     * Executa a importação. [substituir] só chega como true depois de uma confirmação
     * explícita do usuário na tela.
     */
    fun confirmarImportacao(substituir: Boolean) {
        val pendente = _importacaoPendente.value ?: return
        _importacaoPendente.value = null
        viewModelScope.launch {
            _ocupado.value = true
            try {
                val resumo = servicos.backupRepository.importar(pendente.backup, substituir)
                servicos.agendadorAvisos.agendarVerificacaoDiaria()
                _mensagem.value = if (resumo.substituiuDados) {
                    "Backup importado: ${resumo.perfis} perfil(is), ${resumo.itens} item(ns), " +
                        "${resumo.documentos} documento(s), ${resumo.contatos} contato(s)."
                } else {
                    "Backup mesclado: ${resumo.perfis} perfil(is), ${resumo.itens} item(ns), " +
                        "${resumo.documentos} documento(s), ${resumo.contatos} contato(s)."
                }
            } catch (erro: Exception) {
                _mensagem.value = "Falha ao importar o backup. Nenhum dado foi apagado."
            } finally {
                _ocupado.value = false
            }
        }
    }

    fun cancelarImportacao() {
        _importacaoPendente.value = null
    }

    fun consumirMensagem() {
        _mensagem.value = null
    }

    fun nomeArquivoBackup(): String = ArquivoBackup.nomeArquivoSugerido()
}
