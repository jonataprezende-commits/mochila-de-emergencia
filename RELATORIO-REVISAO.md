# Relatório de revisão — bugs, segurança e conteúdo sensível

**App:** Mochila de Emergência (`br.com.mochiladeemergencia.app`)
**Versão:** **1.2.3** (versionCode 6) — build **debug**
**Data da revisão:** 10/11/2026
**Ambiente de build:** Debian 13 (x86_64), JDK 17.0.21 (Temurin), Gradle 8.13, AGP 8.13.2,
Kotlin 2.2.21, Android SDK Platform 36 + Build-Tools 36.0.0, 2 GB de RAM, **sem KVM** (sem emulador).

---

## 1. Resumo executivo

| Etapa | Resultado |
|---|---|
| `./gradlew assembleDebug` | ✅ **BUILD SUCCESSFUL** — APK gerado e assinado |
| `./gradlew lintDebug` | ✅ **0 problemas** (0 erros, 0 avisos) |
| `./gradlew testDebugUnitTest` | ✅ **73 testes, 0 falhas** |
| Verificação do APK (`aapt2` / `apksigner`) | ✅ assinatura v2 válida, minSdk 26, targetSdk 36, **sem INTERNET, sem CALL_PHONE, sem CAMERA** |
| Checklist de segurança (seção 6) | ✅ **21/21 itens atendidos** |
| Conteúdo do checklist (seção 3.3) | ✅ 54 sugestões em 11 categorias, com dica, sem item restrito e sem quantidade imposta |
| Conteúdo de primeiros socorros (seção 8) | ✅ textos fixos, aviso no topo **e** em cada card, sem instruções de incêndio ou procedimentos invasivos |
| Fluidez (rolagem, troca de aba, toque) | ✅ trabalho pesado fora da thread principal — ver seção 2 |
| Execução em emulador/aparelho | ⚠️ **não executada** — o ambiente não tem KVM (ver seção 10) |

### Artefato entregue

| | |
|---|---|
| Caminho no projeto | `MochilaDeEmergencia/mochila-de-emergencia-debug.apk` |
| Cópia na raiz do workspace | `/home/user/mochila-de-emergencia-debug.apk` |
| Caminho canônico do build | `app/build/outputs/apk/debug/app-debug.apk` |
| Tamanho | **13.671.755 bytes** (13,04 MiB) |
| SHA-256 | `b7ecd4e7ede93c15674b52845bd16078246bd097e8ffeb9366288c82316183af` |
| Assinatura | `CN=Android Debug` — APK Signature Scheme **v2** |
| `applicationId` | `br.com.mochiladeemergencia.app.debug` |
| Versão exibida | `1.2.3-debug` (versionCode 6) |

> Os APKs das versões 1.0.0, 1.1.0, 1.2.0, 1.2.1 e 1.2.2 foram **substituídos** por este.

---

## 2. O que mudou na versão 1.2.3 — a dica antiga da Bíblia continuava aparecendo

**Relato:** "você deixou o 'se fizer sentido pra você' na Bíblia? aqui ainda está aparecendo".

**Diagnóstico:** o texto **não estava no código** (o APK da 1.2.2 não tinha a string — zero
ocorrências no `classes.dex`). O que acontecia é que as sugestões do checklist são **copiadas
do código para o banco** na primeira execução, e `sincronizarPadroes()` só fazia duas coisas:

1. removia sugestões padrão que saíram do código;
2. **inseria** as que faltavam.

Uma sugestão que já existia com texto antigo nunca era reescrita. Quem instalou a **1.2.0**
(onde a dica existia) e atualizou para 1.2.1/1.2.2 continuava com a linha antiga no banco —
e era ela que aparecia na tela, não o código novo.

**Correção:** `ChecklistRepository.corrigirTextosAntigos(perfilId)`, chamado na abertura do app
(`MochilaApp`) e no fim de cada `sincronizarPadroes()`:

| Regra | Por quê |
|---|---|
| Só atua em sugestões **padrão** (`personalizado = 0`) | Sugestões criadas pelo usuário nunca são alteradas |
| Só reescreve quando a dica gravada **contém** um texto obsoleto conhecido | Se o usuário escreveu outra dica, ela é preservada |
| Texto antigo identificado por **padrão** (`sentido + para/pra + você/voce/vc`), sobre texto sem acento e minúsculo | Tolera variações de escrita e pontuação que possam existir no banco |
| Idempotente, sem escrita quando nada muda | Pode rodar em toda abertura sem custo |
| Compara com a lista `ChecklistPadrao.DICAS_ANTIGAS` | Próximas mudanças de texto entram ali em uma linha |

Resultado: o texto "Se fizer sentido para você" **não existe mais no APK** (0 ocorrências no
`classes.dex` — só o padrão técnico que o localiza no banco) e o banco de quem atualiza é
corrigido na primeira abertura.

> Nada mais mudou: telas, permissões, banco (v3), backup (formato 3) e as 54 sugestões seguem iguais.

---

## 2.1 O que mudou na versão 1.2.2 — fluidez (nenhuma funcionalidade mudou)

O relato foi "o app está um pouco travado". A causa não era o tamanho das listas: era
**trabalho pesado na thread principal** e **estado recriado a cada troca de tela**. O que mudou:

| # | Antes (travava) | Agora |
|---|---|---|
| 1 | Cada miniatura decodificava o JPEG **durante a composição**, na thread de UI — um arquivo por item, a cada rolagem | `MiniaturaFoto` usa `produceState` + `Dispatchers.IO`; a imagem aparece assim que fica pronta, o item nunca espera |
| 2 | A mesma foto era decodificada de novo a cada retorno à lista | Cache **LRU de 8 MB** (medido em bytes de bitmap) e decodificação em `RGB_565` (metade da memória por imagem) |
| 3 | Excluir uma foto deixava a miniatura no cache para sempre | Evicção por prefixo ao excluir (varre `snapshot()` do cache) |
| 4 | Ao voltar para **Início/Mochila/Documentos**, o ViewModel era recriado: `collectAsState` recebia `null` e a tela piscava "carregando" e reconsultava o Room | Os três ViewModels passam a viver no escopo da **Activity** (`AppMochila.kt`); a aba reaparece já com os dados |
| 5 | Mudar **qualquer** preferência (tema, antecedência) reabria o fluxo do perfil ativo e recarregava tudo | `fluxoPerfilAtivo` com `distinctUntilChanged()` — só reage quando o perfil muda de verdade |
| 6 | Cálculo de status/contagens rodava na thread principal a cada emissão do banco | `flowOn(Dispatchers.Default)` + `distinctUntilChanged()` antes de `stateIn` |
| 7 | `NumberFormat`, regex de acentuação e `DateTimeFormatter` eram **criados a cada item** da lista (e a cada tecla, na busca) | Instâncias reaproveitadas (regex pré-compilada, `ThreadLocal<NumberFormat>`, formatadores `val`) |
| 8 | Canal de notificação, agendamento do `WorkManager` e *seed* do banco rodavam dentro do `Application.onCreate` | Movidos para `escopo.launch` em `Dispatchers.IO`, com `runCatching` — a primeira tela abre sem esperar |
| 9 | Listas sem identidade estável: qualquer mudança recompunha tudo e reanimava todos os itens | `animateItem()` + `contentType` por categoria; cartões recebem `modifier` |
| 10 | Troca de aba "seca" | Fade + slide curtos no `NavHost` (180/220/120 ms) — percepção de resposta imediata |
| 11 | Classes de estado sem contrato de imutabilidade (Compose não pulava recomposições) | `@Immutable` nos 12 data classes de UI state |

**O que não mudou:** telas, textos, regras, banco (v3), backup (formato 3), permissões e testes.
O usuário não precisa fazer nada — é só instalar por cima.

---

## 2.2 O que mudou na versão 1.2.1

| Ajuste | Detalhe |
|---|---|
| **Bíblia** | A sugestão continua no checklist (Conforto e apoio emocional), agora **sem a dica** "Se fizer sentido para você" |
| **Defesa Civil 199** | Além de aparecer na lista de ligação rápida e em Contatos, ganhou **botão próprio na tela Emergência**, ao lado de SAMU 192 e Bombeiros 193 |

Nada mais mudou: as permissões, o banco e o backup seguem iguais.

---

## 2.3 O que mudou na versão 1.2.0 (checklist por categoria)

| Área | Entrega |
|---|---|
| **Checklist** | 54 sugestões em **11 categorias**, cada uma com nome curto, categoria e **dica** opcional; agrupamento e progresso por categoria; filtros (tudo / só o que falta / já na mochila); sugestões personalizadas com dica; sincronização das sugestões padrão entre versões |
| **Sem quantidade imposta** | O item entra na mochila com **1 unidade**; o ajuste é feito na tela Mochila. A opção *"Mostrar dica por pessoas"* apenas **exibe** uma estimativa (ex.: "Para 3 pessoas: cerca de 18 L") |
| **Forma de consumo** | Campo **opcional** no cadastro do item (abrir e comer / aquecer / adicionar água), exibido na lista da mochila e incluído no backup |
| **Dashboard** | Card **"Gire o estoque"**: consumir primeiro o que está perto de vencer e repor com o produto novo |
| **Contatos** | **Removido o telefone do CVV (188)** da lista fixa |
| **Barra inferior** | Rótulo da aba **"Documentos"** não quebra mais no meio da palavra (o "s" caía para a linha de baixo em telas estreitas ou com fonte grande) — o texto agora fica em uma linha e a fonte diminui até caber |
| **Banco** | Room **versão 3** com migração `MIGRACAO_2_3` (colunas `dica` e `formaConsumo` + renomeação das categorias) |
| **Backup** | Formato **3**, incluindo `dica` e `formaConsumo`; backups antigos continuam importáveis |
| **Categorias** | 12 grupos de exibição (11 + "Outros"); nomes antigos (`AGUA_E_ALIMENTOS`, `ILUMINACAO`, `COMUNICACAO`, `ABRIGO`, `FERRAMENTAS`, `PRIMEIROS_SOCORROS`, `DOCUMENTOS`) são reconhecidos e convertidos na migração |

---

## 2.4 O que mudou na versão 1.1.0

Tudo o que existia na v1 continua funcionando; a v2 **acrescenta**:

| Área | Entrega | Onde |
|---|---|---|
| Início | Card grande e editável **"Onde está a mochila"** (localização por perfil, salva no Room), card **"Itens a repor"** e card **"Contatos de emergência"**, além dos 4 atalhos (Checklist, Conferir mochila, Primeiros socorros, Contatos) | `ui/screens/InicioScreen.kt`, `InicioViewModel.kt` |
| Itens | **Foto opcional** por item: galeria via **Photo Picker** (`PickVisualMedia`) ou câmera (`ACTION_IMAGE_CAPTURE` + `FileProvider`), guardada no armazenamento interno; **miniatura** na lista; remover/trocar foto | `ItemFormScreen.kt`, `ItemFormViewModel.kt`, `util/Fotos.kt` |
| Status | Novo status **"Repor"** (azul), com precedência acima de "Vencido"; "Marcar como consumido/substituído" envia o item para a lista Repor | `domain/StatusCalculadora.kt`, `ui/theme/Theme.kt` |
| **Conferir Mochila** | Auditoria item a item (**Tenho / Falta / Substituir**), barra de progresso e **resumo final**; cada resposta é gravada na hora; lista congelada durante a sessão | `ConferirScreen.kt`, `ConferirViewModel` |
| **Repor** | Lista de itens consumidos/substituídos; **"Repor"** devolve o item à mochila como **"Tenho"**; acessível pelo Início, pela própria lista e pelo ícone do item | `ReporScreen.kt`, `ReporViewModel` |
| Lembrete semestral | WorkManager a cada **182 dias**, canal próprio **"Revisão da mochila"**, texto fixo, **toggle em Ajustes (padrão ligado)** | `RevisaoSemestralWorker.kt`, `AgendadorAvisos.kt`, `Notificacoes.kt` |
| **Primeiros Socorros** | 6 cards expansíveis offline (Engasgo, RCP, Queimadura, Desmaio, Convulsão, Hemorragia) com passos numerados (máx. 6), "Não faça", "Ligar 192" e fonte; aviso fixo no topo **e** em cada card; botão "Salvar link de primeiros socorros" leva a Documentos | `PrimeirosSocorrosScreen.kt`, `domain/PrimeirosSocorros.kt` |
| **Contatos de Emergência** | Lista fixa com **Ligar** (SAMU 192, Bombeiros 193, Polícia 190, Defesa Civil 199 — o CVV foi retirado na v3) + **CRUD de contatos pessoais** (nome, telefone, parentesco, observação) por perfil | `ContatosScreen.kt`, `ContatoRepository.kt`, `ContatoEntity` |
| Navegação | Barra inferior com **Início · Mochila · Documentos · Emergência · Ajustes** | `ui/navigation/AppMochila.kt` |
| Backup | JSON **formato 2**: fotos em Base64, contatos, localização do perfil, flag `aRepor` e `lembreteSemestralAtivo` — validação e limites mantidos | `data/backup/BackupRepository.kt` |

**Banco de dados:** Room **versão 2**, com migração explícita `MIGRACAO_1_2` (nenhum dado é apagado ao atualizar):
`ALTER TABLE perfis ADD COLUMN localizacao TEXT NOT NULL DEFAULT ''`;
`ALTER TABLE itens ADD COLUMN aRepor INTEGER NOT NULL DEFAULT 0`;
`ALTER TABLE itens ADD COLUMN fotoArquivo TEXT`;
`CREATE TABLE contatos (…)` com FK `perfilId` em cascata + índice.
O schema 1 e o schema 2 estão exportados em `app/schemas/`.

---

## 3. O que foi executado

> Execução final (árvore limpa): `assembleDebug` ✅ · `lintDebug` ✅ 0 problemas ·
> `testDebugUnitTest` ✅ **69 testes, 0 falhas**.

### 3.1 Build e análise estática

1. **Compilação completa**: `./gradlew assembleDebug` → APK gerado e assinado.
2. **Android Lint**: `./gradlew lintDebug` → **0 problemas**
   (`app/build/reports/lint-results-debug.html`).
3. **Testes**: `./gradlew testDebugUnitTest` → **73 testes, 0 falhas**
   (`app/build/reports/tests/testDebugUnitTest/index.html`).
4. **Inspeção do APK final**:
   - `aapt2 dump badging` → pacote, versão, minSdk 26, targetSdk 36, label, launcher;
   - `aapt2 dump permissions` → nenhuma permissão de internet/telefone/câmera;
   - `aapt2 dump xmltree AndroidManifest.xml` → componentes e `exported`;
   - `apksigner verify --print-certs` → APK íntegro, assinado com chave de debug (v2).

### 3.2 Testes automatizados (73)

| Classe | Qtd. | Cobre |
|---|---|---|
| `StatusCalculadoraTest` | 13 | Tenho / Falta / Vencendo / Vencido, limites de antecedência (30/60/90), precedência, dias até vencer, marcos de aviso, textos pt-BR |
| `RegraNotificacaoTest` | 11 | marcos 30/7/1/0 + antecedência, item sem validade, vencidos, mensagem resumida, não repetir no mesmo dia, hash do conteúdo |
| `BackupRepositoriosTest` | 15 | Room em memória: cadastro/edição/exclusão, validação, cascata, documentos, exportar→validar→importar, JSON inválido sem alterar dados, limites, checklist |
| `ChecklistCategoriasTest` | 17 | conteúdo das 54 sugestões (categorias, nomes curtos, dicas), itens proibidos (arma, munição, explosivo, geladeira…), dica de fósforos e de água, **CVV ausente**, quantidade não imposta, dica por pessoa sem gravar nada, sugestão personalizada com dica, sincronização entre versões, restauração, categorias antigas, forma de consumo, **Bíblia sem dica** |
| `NovosFluxosTest` **(v2)** | 14 | **Repor** tem precedência sobre Vencido; consumido → Repor; `repor()` → Tenho; conferência (Tenho/Falta/Substituir zera validade); localização do perfil salvando/lendo; **foto** salva com miniatura; nomes de arquivo inválidos recusados; **round-trip de foto no backup**; contatos (CRUD, validação de telefone, cascata, backup); conteúdo dos 6 cards; constantes do lembrete semestral |
| `MigracaoBancoTest` | 3 | Migração **v1 → v2** por SQL bruto, migração **v2 → v3** (dica, forma de consumo, renomeação de categorias) e criação de banco novo |

### 3.3 Teste instrumentado (entregue; exige aparelho/emulador)

`app/src/androidTest/.../InicializacaoAppTest.kt` — abre a `MainActivity`, confere os serviços
locais e recria a Activity. Comando: `./gradlew connectedDebugAndroidTest`.

### 3.4 Revisão estática dirigida (leitura de código)

Foram revisados manualmente: as 5 abas e as rotas novas; estados vazios e de carregamento;
`contentDescription` de todos os ícones novos; fluxo de foto (galeria, câmera, troca, remoção,
arquivo órfão); auditoria (avanço, reinício, conclusão); precedência de status com `aRepor`;
validação de telefone e nome de contato; cancelamento/agendamento do lembrete semestral;
mensagens de confirmação em exclusões; tratamento de `ActivityNotFoundException` ao abrir o discador.

---

## 3.3 Checklist — regras de conteúdo aplicadas

| Regra pedida | Como está no app |
|---|---|
| Sugestões **por categoria** | 54 sugestões em 11 grupos; a tela mostra um cabeçalho por grupo com "x/y" e progresso geral |
| **Nome curto + categoria + dica opcional** | Nomes limitados a 52 caracteres; ~30 sugestões têm dica (ex.: *"Prefira fósforos à prova d'água"*, *"2 litros por pessoa por dia; 6 litros por pessoa para 72 horas"*) |
| **Checklist 100% editável** | Editar (nome, categoria, unidade, dica), remover e criar sugestões próprias; "Restaurar checklists padrão" com confirmação |
| **Nada de quantidade imposta** | Ao adicionar, o item entra com **1** unidade; a estimativa por pessoa é apenas **texto** e só aparece se o usuário ligar a opção |
| **Sem alimentos que exijam geladeira/cozimento elaborado** | Teste automatizado reprova as palavras "geladeira", "refrigerar", "congelado" nas sugestões |
| **Sem latas amassadas ou estufadas** | A sugestão de leguminosas orienta *descartar* latas amassadas/estufadas |
| **Sem armas, munição, itens de caça ou uso restrito** | Teste reprova "arma", "munição", "explosivo", "pólvora", "caça", "rifle", "pistola" |
| **Sem instruções de fogo, explosivos ou procedimentos invasivos** | Nenhuma instrução desse tipo; fósforos/isqueiro aparecem apenas como item de mochila |
| **Sem medicamentos específicos** | Só "Remédios de uso contínuo" (com a dica "Leve a receita junto") e "Recipiente à prova d'água" |

O conteúdo é coberto por `ChecklistCategoriasTest` (12 testes), que falha se alguém reintroduzir
um termo proibido, quebrar as chaves ou remover as dicas combinadas.

---

## 4.1 Bugs encontrados e corrigidos na v2

| # | Severidade | Problema | Correção |
|---|---|---|---|
| 1 | 🔴 Bloqueante | `combine` com **6 fluxos** (Início) e **6** (Conferir) não existe na API de corrotinas — não compilava | Agrupamento em `Pair` / `Triple` intermediário antes do `combine` final |
| 2 | 🔴 Bloqueante | `Fotos.deBase64` escrita como *expression body* com `return` — erro de compilação | Corpo em bloco |
| 3 | 🔴 Bloqueante | Import duplicado de `Icones` em `MochilaScreen` e `ConfiguracoesScreen` — *"conflicting import"* | Imports deduplicados |
| 4 | 🔴 Bloqueante | `Icons.Filled.DateRange` sem import no formulário de item | Import explícito |
| 5 | 🔴 Bloqueante | Código de migração chamava `lerFoto` com parâmetro inexistente | Assinatura ajustada |
| 6 | 🟠 Alto | **Conferir Mochila pulava itens**: "Substituir" zera a validade e a consulta ordena por validade, então a lista viva se reordenava no meio da auditoria | Lista **congelada** no início da sessão e incluída no fluxo (`Andamento`) — não depende mais de leitura imperativa |
| 7 | 🟠 Alto | **Fotos de câmera ficavam deitadas**: o JPEG era recomprimido sem respeitar a orientação EXIF | `aplicarOrientacao()` antes de reduzir/comprimir (rotação e espelhamento) |
| 8 | 🟡 Médio | Barra de progresso da conferência nunca chegava a **100%** (o índice parava em `total-1`) | Índice avança até `total`; o resumo final só usa `concluido` |
| 9 | 🟡 Médio | **Arquivos de foto órfãos** ao excluir item, excluir perfil (cascata) ou importar backup no modo "substituir" | `Fotos.excluir()` chamado nos três pontos antes da exclusão no banco |
| 10 | 🟡 Médio | Mensagem de limite de importação dizia **2 MB** (valor da v1) enquanto o limite real passou a 12 MB | Mensagem passou a ser calculada a partir da constante |
| 11 | 🟡 Médio | Exportação podia gerar um JSON **maior que o limite de importação** (fotos em Base64) e criar um backup que o próprio app recusaria | `exportarJson()` valida o tamanho e lança mensagem clara; a tela mostra o motivo |
| 12 | 🟡 Médio | JVM de testes estourava a memória do ambiente (1,5 GB RSS) e era morto pelo OOM killer, deixando o build pendurado | `maxHeapSize = 512m` + `MaxMetaspaceSize=256m` no `tasks.withType<Test>` |
| 13 | 🟢 Baixo | Lint `UseKtx` em `Bitmap.createScaledBitmap` (2 ocorrências) | Troca por `Bitmap.scale` da KTX |
| 14 | 🟢 Baixo | Lint `ExifInterface` pedindo a variante AndroidX | Supressão **documentada** no arquivo: a API da plataforma existe desde a API 24 e o `minSdk` é 26; evita dependência extra só para ler orientação de JPEG do próprio app |
| 15 | 🟢 Baixo | Teste da v1 esperava consumido → "Falta" | Atualizado para o comportamento novo (consumido → **Repor**, e `repor()` → Tenho) |
| 16 | 🟢 Baixo | `BackupRepositoriosTest` não passava o novo contexto do construtor | Construtor e assinatura de configurações atualizados |

### Correções da versão 1.2.3

| # | Item | Correção |
|---|---|---|
| 1 | **Dica antiga continuava na tela** mesmo com o texto removido do código: a sincronização insere o que falta, mas não reescreve sugestão já gravada | `corrigirTextosAntigos()` reescreve a dica de sugestões padrão cujo texto é obsoleto; roda na abertura do app e ao sincronizar |
| 2 | Detecção por comparação exata perdia variações ("pra", "vc", ponto final) | Padrão regex sobre texto normalizado; teste cobre 5 variações |
| 3 | (ambiente) `.cache` do sandbox apagado de novo entre turnos: toolchain e cache do Gradle perdidos | Reinstalação automatizada em `~/.cache-instalar.sh` (JDK 17 + SDK 36 + `build.sh`), ~40 s |
| 4 | (ambiente) daemon do Gradle morto pelo OOM killer ao rodar testes (Gradle 512 MB + testes 384 MB não cabiam em 2 GB) | Heaps reduzidos para 384 MB / 320 MB; suíte completa roda em 24 s |

### Correções de fluidez na versão 1.2.2

| # | Item | Correção |
|---|---|---|
| 1 | Miniatura decodificava JPEG na thread principal durante a composição (engasgo na rolagem) | `produceState` + `Dispatchers.IO`; cache LRU de 8 MB em `RGB_565`; evicção ao excluir a foto |
| 2 | `android.util.LruCache` foi usado com o construtor de 2 argumentos (que só existe em `androidx.collection`) — erro de compilação | Subclasse anônima sobrescrevendo `sizeOf()` para medir em bytes; `snapshot()` é da própria classe e foi mantido |
| 3 | `MiniaturaFoto` tentava importar `androidx.compose.ui.graphics.Bitmap` (`Fotos.miniatura` devolve `android.graphics.Bitmap`) e o *smart cast* falhava na propriedade delegada | Import correto e cópia para uma `val` local antes do `if` |
| 4 | `produceState(initialValue = …)` estava com o nome de parâmetro em português (`inicial`) | Nome correto do parâmetro |
| 5 | `ThreadLocal` de `NumberFormat` era tipado como anulável (aviso do compilador e risco de NPE) | `object : ThreadLocal<NumberFormat>() { override fun initialValue() … }` |
| 6 | ViewModels recriados na troca de aba (flash de "carregando") | `vmInicio`/`vmMochila`/`vmDocumentos` no escopo da Activity |
| 7 | Salvar qualquer preferência recarregava perfil e listas | `distinctUntilChanged()` no `fluxoPerfilAtivo` |
| 8 | Trabalho pesado no `Application.onCreate` | `escopo.launch` em `Dispatchers.IO` com `runCatching` |

### Ajustes da versão 1.2.1

| # | Item | Correção |
|---|---|---|
| 1 | Dica da **Bíblia** ("Se fizer sentido para você") soava como julgamento e foi removida por pedido | Sugestão mantida, `dica = ""`; teste garante que o texto não volte |
| 2 | **Defesa Civil (199)** só aparecia na lista de ligação rápida | Novo botão **"Defesa Civil 199"** em destaque na tela Emergência; teste garante os quatro números fixos (192, 193, 190, 199) e a ausência do CVV |

### Bugs encontrados e corrigidos na v1.2.0

| # | Severidade | Problema | Correção |
|---|---|---|---|
| 1 | 🔴 Bloqueante | `ExperimentalTextApi` não existe em `material3` — erro de compilação ao usar `TextAutoSize` | Import correto: `androidx.compose.ui.text.ExperimentalTextApi` |
| 2 | 🟠 Alto | Aba **"Documentos"** quebrava no meio da palavra na barra inferior (o "s" caía para a segunda linha) em telas estreitas ou com fonte grande | Rótulo em **uma linha** (`maxLines = 1`, `softWrap = false`) com **fonte auto-ajustável** (`TextAutoSize.StepBased`, 8–12 sp) |
| 3 | 🟠 Alto | Ao trocar as categorias, itens antigos ficariam órfãos em "Outros" depois de atualizar o app | Migração `MIGRACAO_2_3` renomeia as categorias no banco + `Categoria.dePersistido` aceita os nomes antigos como alias |
| 4 | 🟠 Alto | Substituir a lista de sugestões padrão deixaria checklists antigos com itens que não existem mais (e sem os novos) | `sincronizarPadroes()`: remove padrões obsoletos, insere os novos, **preserva** personalizados e edições do usuário; roda na abertura do checklist e ao criar perfil |
| 5 | 🟡 Médio | O checklist calculava e **aplicava** quantidade por pessoa (`quantidadeSugerida`), contrariando "não impor quantidades" | Quantidade do item ao adicionar passou a ser **1**; a estimativa virou apenas texto (`dicaDeQuantidade`) |
| 6 | 🟡 Médio | Dica do suco dizia "sem precisar de geladeira" — o teste de conteúdo proíbe menção a geladeira | Texto trocado para "Longa vida (UHT), dura meses na despensa" |
| 7 | 🟡 Médio | Grafo de memória/CPU: com `-Xmx896m` o daemon do Gradle era morto pelo OOM killer no ambiente de 2 GB | `-Xmx768m`, `MaxMetaspaceSize=512m`, `TieredStopAtLevel=1`; compilação dos testes e execução em passos separados com `--max-workers=1` |
| 8 | 🟢 Baixo | Mensagem de limite do backup ainda citava 2 MB em texto fixo (v1) | Passou a ser calculada a partir da constante (12 MB) e a exportação valida antes de gravar |
| 9 | 🟢 Baixo | `combine`/`FiltroChecklist` novo exigia totais independentes do filtro (o cabeçalho mudaria ao filtrar) | `ChecklistUiState` ganhou `total`/`adicionados` calculados sobre a lista completa |

### Correções herdadas da v2 (mantidas)

Bloqueantes de compilação (`descreverDias`, `first()`), mensagem do checklist que nunca aparecia,
`IllegalStateException` no agendamento, troca do `material-icons-extended` por ícones próprios,
ícone `Lightbulb` inexistente, `ACCESS_NETWORK_STATE` e `PreviewActivity` removidas do APK,
interface `ArmazenamentoConfiguracoes` para testar backup, contraste do selo de status,
ícones adaptativos em `mipmap-anydpi-v26`, `Locale.forLanguageTag`,
`fallbackToDestructiveMigration` depreciado, ajustes de lint e de testes Robolectric.
Todas continuam aplicadas — o lint final é 0 e os 55 testes passam.

---

## 5. Riscos residuais (o que ainda pode dar errado)

| # | Risco | Mitigação já aplicada | Sugestão |
|---|---|---|---|
| 1 | **Conteúdo de primeiros socorros** pode ser mal interpretado e ser usado em situação de risco | Aviso fixo no topo **e** em cada card; passos curtos; "Não faça" em todos; botão de ligação 192 em destaque; nenhuma instrução invasiva ou de combate a incêndio | Revisão do texto por profissional de saúde/SAMU antes de publicar |
| 2 | O campo **"Sinal"** só aparece nos cards em que o texto fornecido tinha essa linha (Engasgo e RCP); nos outros quatro o reconhecimento está descrito nos próprios passos | Texto mantido **literal**, sem invenção de conteúdo clínico | Se quiser, enviar as linhas de "Sinal" dos demais cards para inclusão |
| 3 | **Fotos** crescem o armazenamento (~400 KB por item, sem cota) | Recomprimir em JPEG (máx. 1024 px, qualidade 80 → 60 → escala 0,7), apagar ao excluir item/perfil e ao substituir no backup | Definir limite de fotos por perfil em versões futuras |
| 4 | **Backup com muitas fotos** pode passar de 12 MB e ser recusado | Limite verificado **na exportação** e na importação, com mensagem explicando o motivo | Exportar sem fotos (ou em partes) em mochilas muito grandes |
| 5 | **Lembrete semestral** depende de notificações permitidas e de o app ser aberto ao menos uma vez; fabricantes agressivos podem atrasar o WorkManager | Agendamento único com `KEEP`; canal próprio; toggle em Ajustes; o conteúdo continua acessível sem notificação | Reforçar no Início o selo "notificações desativadas" (já existe) |
| 6 | **Ligar** só abre o discador (`ACTION_DIAL`); o usuário precisa tocar em chamar | Decisão de projeto: **sem `CALL_PHONE`**; aparelhos sem app de telefone recebem mensagem "Nenhum aplicativo de telefone encontrado" | — |
| 7 | **Câmera** pode não existir ou não ter app que responda a `ACTION_IMAGE_CAPTURE` | `ActivityNotFoundException` tratada; galeria (Photo Picker) continua disponível | — |
| 8 | **Documentos** guardam URI `content://` de terceiros; se o arquivo for movido/apagado, o link quebra | Selo "Disponível offline" e mensagem ao abrir | Reensinar a escolher o arquivo |
| 9 | **Dicas de quantidade** do checklist são estimativas (2 L/pessoa/dia; 6 L/pessoa para 72 h) e podem não servir para casos específicos (bebês, animais, clima) | Texto deixa claro que é dica; nada é gravado automaticamente | Ajuste os itens na tela Mochila |
| 10 | **Localização da mochila** é texto livre — o usuário pode escrever algo sensível | Dado fica só no aparelho, backup automático desativado; não vai para logs | — |

---

## 6. Revisão de segurança — checklist

| Requisito | Status | Evidência |
|---|---|---|
| Sem permissão `INTERNET` | ✅ | `aapt2 dump permissions` do APK: só `POST_NOTIFICATIONS`, `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE` e a permissão privada do androidx.core |
| Sem WebView | ✅ | Nenhum uso de `WebView`; links abrem por `Intent.ACTION_VIEW` |
| Sem coleta de dados/analytics/anúncios | ✅ | Nenhum SDK de terceiros; sem envio de dados; sem INTERNET |
| Sem `CALL_PHONE` | ✅ | Só `Intent.ACTION_DIAL` com `Uri.fromParts("tel", …)` (abre o discador; não disca) |
| Sem permissão de câmera | ✅ | Câmera por `ACTION_IMAGE_CAPTURE` (app externo) e `FileProvider`; nenhuma permissão `CAMERA` declarada |
| Sem permissão ampla de mídia | ✅ | **Photo Picker** (`ActivityResultContracts.PickVisualMedia`); nenhuma `READ_MEDIA_IMAGES`/`READ_EXTERNAL_STORAGE` |
| Permissões realmente necessárias | ✅ | ver tabela da seção 7 |
| Componentes não exportados sem necessidade | ✅ | Só `MainActivity` (launcher) é exportada; `FileProvider` `exported=false`; componentes de WorkManager vêm da biblioteca com `BIND_JOB_SERVICE` |
| `FileProvider` correto e restrito | ✅ | `exported=false`, `grantUriPermissions`, expõe **apenas** `cache/exports/`, `cache/fotos_temp/` e `files/fotos/` |
| `<queries>` mínimas | ✅ | Apenas `https` (abrir links), `tel` (discador) e `IMAGE_CAPTURE` — exigidas pelo Android 11+ para esses intents |
| Fotos fora de caminhos públicos | ✅ | Gravadas em `filesDir/fotos` (privado do app), recomprimidas e renomeadas com UUID |
| Sem *path traversal* nas fotos | ✅ | Nome validado por regex `^[A-Za-z0-9._-]{1,80}\.jpg$` + bloqueio de `..`; nome vindo de backup é **ignorado** e regenerado |
| EXIF removido das fotos | ✅ | A imagem é decodificada e recomprimida em JPEG — metadados (inclusive GPS) não são copiados |
| Validação do JSON de importação | ✅ | Estrutura, tipos, campos obrigatórios, faixas, datas ISO, contagens, telefone saneado, Base64 limitado (620.000 caracteres por foto) |
| Limite de tamanho de backup | ✅ | 12 MB (checado no `InputStream` **e** no texto), 50 perfis, 2.000 itens/perfil (10.000 total), 500 documentos, 300 sugestões, **200 contatos** por perfil |
| Importação não apaga dados sem confirmação | ✅ | Diálogo explícito com contagem de perfis/itens/documentos/contatos e escolha **Mesclar** ou **Substituir** |
| Exclusões com confirmação | ✅ | Itens, documentos, perfis e contatos pessoais pedem confirmação |
| Backup automático desativado | ✅ | `allowBackup="false"` + `fullBackupContent="false"` + `data_extraction_rules.xml` excluindo tudo |
| Sem dados sensíveis em log | ✅ | Único log de erro usa a **classe** da exceção (`ValidadeWorker`); telefones, nomes e fotos nunca são logados |
| Sem chaves/senhas/segredos | ✅ | Nenhum segredo no projeto; release sem `signingConfig` |
| Sem instruções perigosas | ✅ | Nenhuma orientação de combate a incêndio; primeiro socorros apenas informativo, com aviso repetido |

---

## 7. Permissões e componentes no APK final

### Permissões (`aapt2 dump permissions`)

| Permissão | Origem | Por quê |
|---|---|---|
| `POST_NOTIFICATIONS` | app | avisos de validade e lembrete semestral (Android 13+); negar não quebra o app |
| `WAKE_LOCK` | WorkManager | executar os trabalhos periódicos |
| `RECEIVE_BOOT_COMPLETED` | WorkManager | reagendar após reiniciar o aparelho |
| `FOREGROUND_SERVICE` | WorkManager | declarada pela biblioteca (o app não usa serviço em primeiro plano) |
| `…debug.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | androidx.core | permissão **privada** do app (proteção de receivers) |

**Não** há: `INTERNET`, `CALL_PHONE`, `CAMERA`, `READ_MEDIA_*`, `READ/WRITE_EXTERNAL_STORAGE`,
`ACCESS_FINE_LOCATION`, `RECORD_AUDIO`, `QUERY_ALL_PACKAGES`.

### Componentes

| Componente | Exportado | Observação |
|---|---|---|
| `MainActivity` | ✅ sim | Necessário: é o launcher |
| `FileProvider` | ❌ não | `grantUriPermissions`; só as 3 pastas do app |
| `InitializationProvider` (androidx.startup) | ❌ não | Inicializa WorkManager/Emoji2/Lifecycle |
| Serviços de WorkManager | biblioteca | `SystemJobService` exportado pela lib, protegido por `BIND_JOB_SERVICE` |
| Receivers de WorkManager | ❌ não | `RescheduleReceiver`, `ForceStopRunnable$BroadcastReceiver` etc. |

---

## 8. Primeiros socorros — origem, formato e limites do conteúdo

**Formato entregue:** 6 cards expansíveis, 100% offline, sem chamada de rede.
Cada card tem título, linha de **Sinal** (quando o texto fornecido traz), passos numerados
(**máximo 6**), bloco **"Não faça"**, botão **"Ligar 192"** (ACTION_DIAL) e a fonte
*"Baseado em orientações do SAMU e Ministério da Saúde"*.
O aviso

> *"Estas orientações são informativas e não substituem treinamento prático nem atendimento médico
> profissional. Em qualquer emergência grave, ligue imediatamente para o SAMU (192) ou
> Bombeiros (193)."*

aparece **fixo no topo da tela** e **repetido em cada card**.
A tela inicial de Emergência repete o mesmo aviso antes dos números de discagem.

**Cards:** Engasgo (adulto ou criança maior), RCP, Queimadura, Desmaio, Convulsão, Hemorragia.

**Limites de escopo observados:**
- ❌ nenhuma instrução de combate a incêndio;
- ❌ nenhum procedimento invasivo (nada de agulha, sonda, medicamento, corte, torniquete "ensinado");
- ✅ apenas manobras de suporte básico e orientação de **ligar 192/193**;
- ✅ "Não faça" em todos os cards (ex.: não colocar nada na boca em convulsão; não passar pasta de
  dente em queimadura).

**Fontes declaradas** (referências do conteúdo informativo; o app **não** acessa esses links —
são citados aqui no relatório):

1. **Ministério da Saúde — Protocolo SAMU 192: Suporte Básico de Vida.** Brasília: Ministério da
   Saúde, 2014 (protocolos de SBV: obstrução de vias aéreas, RCP, hemorragia, queimadura, convulsão).
2. **Ministério da Saúde / FIOCRUZ — Manual de Primeiros Socorros.** Rio de Janeiro (orientações
   gerais de primeiros socorros para leigos).
3. **SAMU 192 — Secretaria Municipal da Saúde de São Paulo — Manual de Primeiros Socorros para
   Leigos** e documentos técnicos do SAMU 192
   (referência de conduta e de "quando chamar o SAMU").

**Como o texto chegou ao app:** os textos dos cards, do aviso e da mensagem do lembrete semestral
foram **fornecidos pelo responsável pelo app** e reproduzidos **literalmente** em
`domain/PrimeirosSocorros.kt` (há teste automatizado que falha se algum card perder passos,
"Não faça" ou o número 192).

---

## 9. Lembrete semestral — funcionamento

- Trabalho periódico **`lembrete_semestral_revisao`**, período de **182 dias**, política `KEEP`
  (não reinicia a contagem a cada abertura do app).
- Canal próprio **"Revisão da mochila"** (`canal_revisao`), notificação com id fixo `2002`.
- Texto **fixo**: *"Revise sua mochila — mesmo itens sem validade vencem ou enferrujam
  (pilha, repelente aberto, isqueiro, lona ressecada)"*.
- **Ajustes → Lembrete semestral**: `Switch` **ligado por padrão**; desligar **cancela** o trabalho.
- Respeita `POST_NOTIFICATIONS`: sem permissão, o trabalho roda e não exibe notificação
  (o app continua funcionando normalmente).
- Não usa rede, não acorda a tela e não usa serviço em primeiro plano.

---

## 10. O que não foi possível testar aqui

- **Execução em aparelho/emulador**: o sandbox não tem KVM e não há emulador disponível.
  Por isso câmera real, discador real, Photo Picker do sistema e a notificação semestral
  **não foram vistos em execução** — foram validados por compilação, testes de unidade/integração,
  lint e inspeção do APK.
- **Testes de UI instrumentados** (`androidTest`) foram escritos, mas exigem aparelho
  (`./gradlew connectedDebugAndroidTest`).
- **Roteiro de teste manual sugerido** (10 minutos):
  1. Início → tocar no card da localização, escrever "Armário da lavanderia" e reabrir o app.
  2. Mochila → item → **Adicionar foto** (galeria e câmera) → conferir a miniatura na lista.
  3. Item → **Marcar como consumido** → conferir que ele sai da mochila e aparece em **Repor**.
  4. Repor → **Repor** → conferir que volta como "Tenho".
  4b. Checklist → conferir os grupos por categoria, a dica de cada item, o filtro
      "Só o que falta" e criar uma sugestão personalizada com dica.
  4c. Cadastrar um item de comida e escolher a **forma de consumo**.
  5. Início → **Conferir mochila** → responder Tenho/Falta/Substituir até o resumo final.
  6. Emergência → **Contatos de emergência** → criar contato pessoal → tocar em **Ligar**
     (deve abrir o discador com o número, sem discar).
  7. Emergência → **Primeiros socorros** → expandir os 6 cards → **Ligar 192** → voltar.
  8. Ajustes → desligar/ligar **Lembrete semestral**; negar a permissão de notificações e conferir
     que o app continua funcionando.
  9. Ajustes → **Exportar backup** com fotos e contatos → alterar dados → **Importar** (mesclar).
  10. Girar a tela, trocar o tema e **aumentar bastante a fonte do sistema** — a barra inferior
      deve continuar com "Documentos" em uma linha.

---

## 11. Ambiente de build (para reproduzir)

```bash
export JAVA_HOME=/caminho/para/jdk-17
cd MochilaDeEmergencia
./gradlew assembleDebug        # APK em app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # 73 testes
./gradlew lintDebug            # 0 problemas
sha256sum app/build/outputs/apk/debug/app-debug.apk
# b7ecd4e7ede93c15674b52845bd16078246bd097e8ffeb9366288c82316183af
```

`gradle.properties` vem com valores conservadores (`-Xmx384m`, Kotlin *in-process*,
`workers.max=1`) e o `build.gradle.kts` limita o JVM de testes a 320 MB — assim build, lint e
testes rodam até em máquinas com 2 GB de RAM. Em máquinas maiores, pode aumentar
`org.gradle.jvmargs` sem medo. (Na v1.2.2 os heaps foram reduzidos outra vez: com 768/512 MB o
*daemon* era morto pelo OOM killer no meio da execução dos testes.)

> **Nota de ambiente (não é bug do app):** com 2 GB de RAM, o *daemon* do Gradle e o JVM de testes
> foram mortos pelo OOM killer algumas vezes durante as iterações. Os limites acima tornaram o
> processo estável: a execução final de `assembleDebug`, `lintDebug` e `testDebugUnitTest`
> concluiu sem falhas.

---

## 12. Conclusão

- A v3 mantém **tudo o que a v1 e a v2 entregavam** e reorganiza o **checklist em 54 sugestões
  por categoria**, com dica em cada item, sem quantidade imposta, além da **forma de consumo** no
  item, do card **"Gire o estoque"**, da remoção do **CVV** e da correção do rótulo "Documentos"
  na barra inferior.
- Build limpo, **lint 0**, **69 testes verdes**, banco migrando de v1 → v2 → v3 sem perder dados.
- Segurança: **nenhuma permissão nova** foi necessária além das que já existiam — câmera e galeria
  funcionam **sem** permissões de mídia, e ligações **sem** `CALL_PHONE`.
- O ponto que mais merece atenção antes de publicar é o **conteúdo de primeiros socorros**:
  ele é informativo, está com aviso repetido e foi mantido literal, mas convém uma revisão por
  profissional de saúde.
