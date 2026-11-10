# Mochila de Emergência

[**⬇ Baixar o APK**](mochila-de-emergencia-debug.apk) · versão **1.2.3** (versionCode 6) · 13 MB · Android 8.0+

Aplicativo Android **gratuito, offline, sem login, sem anúncios e sem assinatura** para ajudar
famílias a montar, conferir, fotografar e manter uma mochila de emergência.

- Cadastro de itens com **data de validade**, **foto** (galeria ou câmera) e avisos antecipados
- Status **Tenho · Vencendo · Vencido · Falta · Repor**
- **Conferir Mochila**: auditoria item a item com progresso e resumo final
- **Repor**: lista do que foi consumido/substituído, com um toque para voltar à mochila
- **Checklist** com 54 sugestões editáveis, agrupadas por categoria, com dica em cada item
- **Documentos** (links e arquivos locais) para consulta offline
- **Emergência**: números que abrem o discador + **Primeiros Socorros** offline + **Contatos**
- **Múltiplos perfis** (Casa, família, outro local) com a **localização da mochila**
- **Lembrete semestral** para revisar a mochila
- **Backup e restauração em JSON** (inclui fotos e contatos)
- **Tema claro/escuro** e suporte a fonte grande

> ⚠️ **Aviso:** este app **não substitui** orientação profissional de emergência.
> Em caso de risco à vida, ligue imediatamente para o **SAMU (192)** ou os **Bombeiros (193)**.
> As orientações de primeiros socorros são **informativas**, não substituem treinamento prático
> nem atendimento médico, e o app **não** traz instruções de combate a incêndio.

---

## 1. Resumo técnico

| Item | Valor |
|---|---|
| Linguagem | Kotlin 2.2.21 |
| Interface | Jetpack Compose + Material 3 (BOM 2025.10.01) |
| Banco local | Room 2.8.3 (versão **3**, com migrações 1→2 e 2→3) |
| Preferências | DataStore Preferences 1.1.7 |
| Tarefas periódicas | WorkManager 2.10.5 |
| Navegação | Navigation Compose 2.9.6 |
| Min SDK | 26 (Android 8.0) |
| Target/Compile SDK | 36 (Android 16) |
| AGP / Gradle | 8.13.2 / 8.13 |
| JDK para build | 17 |
| Package | `br.com.mochiladeemergencia.app` |
| Versão | **1.2.3** (versionCode 6) |
| Nome exibido | Mochila de Emergência |

**Não há** backend, login, nuvem, anúncios, analytics, telemetria, bibliotecas pagas
ou permissão de INTERNET.

---

## 2. Telas

| Tela | Rota | O que faz |
|---|---|---|
| **Início** | `inicio` | Saudação e perfil ativo; **card grande e editável "Onde está a mochila"** (com o número de pessoas da casa); **dica "Gire o estoque"** (consumir o que está perto de vencer e repor com o novo); cards de **itens a repor**, **contatos de emergência**, total/faltando/vencendo/vencidos/documentos; próximos vencimentos; **atalhos** para Checklist, Conferir mochila, Primeiros socorros e Contatos; FAB "Adicionar item" |
| **Mochila** | `mochila` | Lista de itens com **miniatura da foto**, busca por nome, filtros por categoria e status (Tenho/Falta/Vencendo/Vencido/**Repor**), editar, excluir com confirmação, marcar como consumido e repor |
| **Item (cadastro/edição)** | `item_form?itemId=` | Nome, categoria (12 grupos), quantidade, unidade, validade opcional, **forma de consumo** (abrir e comer, aquecer, adicionar água), local, observação, status e **foto** (galeria pelo *Photo Picker* ou câmera) |
| **Conferir Mochila** | `conferir` | Percorre os itens um a um (**Tenho / Falta / Substituir**), barra de progresso e **resumo final** (quantos tenho, quantos faltam, quantos substituir); cada resposta é salva na hora |
| **Repor** | `repor` | Lista dos itens consumidos/substituídos; **"Repor"** devolve o item à mochila como **Tenho** |
| **Checklist** | `checklist` | **54 sugestões padrão agrupadas por categoria**, cada uma com nome curto e **dica** opcional; botão "Adicionar à minha mochila"; mostra o que já entrou e o que falta; filtros (tudo / só o que falta / já na mochila); sugestões personalizadas; dica de quantidade por pessoa (opcional, não altera nada) |
| **Documentos** | `documentos` | Links e arquivos locais, abrir em app externo, selo "Disponível offline" |
| **Documento (cadastro/edição)** | `doc_form?docId=` | Título, tipo (link/PDF/imagem/vídeo/texto), link ou arquivo, categoria, observação |
| **Emergência** | `emergencia` | Aviso fixo, botões **SAMU 192**, **Bombeiros 193** e **Defesa Civil 199**, atalhos para Primeiros socorros / Contatos / Documentos e a lista de **ligar rápido** (192, 193, 190, 199) |
| **Primeiros Socorros** | `primeiros_socorros` | 6 cards expansíveis **offline** (Engasgo, RCP, Queimadura, Desmaio, Convulsão, Hemorragia) com passos numerados, "Não faça", **Ligar 192** e fonte; aviso fixo no topo **e** em cada card; botão "Salvar link de primeiros socorros" leva a Documentos |
| **Contatos de Emergência** | `contatos` | Lista fixa com **Ligar** (192/193/190/199) e **contatos pessoais** (nome, telefone, parentesco, observação) — criar, editar e excluir com confirmação |
| **Perfis / Família** | `perfis` | Criar, renomear, excluir (com confirmação) e alternar o perfil ativo |
| **Ajustes** | `ajustes` | Antecedência (30/60/90), notificações, **lembrete semestral**, pessoas na casa, tema, backup, perfis e "Sobre" |
| **Sobre** | `sobre` | Avisos de segurança, privacidade, permissões e passo a passo |

Navegação inferior: **Início · Mochila · Documentos · Emergência · Ajustes**.

---

## 3. Como compilar

### 3.1 No Android Studio

1. Instale o **Android Studio** (versão com suporte a AGP 8.13) e o **JDK 17**.
2. `File → Open` e selecione a pasta do projeto (`MochilaDeEmergencia`).
3. Aguarde o *Gradle Sync* (as dependências vêm do Google Maven e do Maven Central).
4. Se o SDK 36 não estiver instalado: `Tools → SDK Manager → SDK Platforms → Android 16 (API 36)`
   e `SDK Tools → Android SDK Build-Tools 36`.
5. Rode o app com ▶ (`Shift+F10`) ou gere o APK em
   `Build → Build Bundle(s) / APK(s) → Build APK(s)`.

> O arquivo `local.properties` **não** é versionado. O Android Studio o cria automaticamente.
> No Linux/macOS, se for compilar por linha de comando, crie o arquivo com:
> `sdk.dir=/caminho/para/Android/Sdk`

### 3.2 Por linha de comando

```bash
# Linux/macOS
export JAVA_HOME=/caminho/para/jdk-17
cd MochilaDeEmergencia
./gradlew assembleDebug
```

```bat
:: Windows
set JAVA_HOME=C:\caminho\para\jdk-17
cd MochilaDeEmergencia
gradlew.bat assembleDebug
```

Saída: `app/build/outputs/apk/debug/app-debug.apk`

Outros comandos úteis:

```bash
./gradlew testDebugUnitTest   # testes unitários/integração em JVM (73 testes)
./gradlew lintDebug           # análise estática do Android Lint
./gradlew assembleRelease     # release NÃO assinado (sem chave no projeto)
```

### 3.3 Observações de build

- O projeto foi compilado e testado com **JDK 17**, **Gradle 8.13** e **AGP 8.13.2**.
- O build de debug inclui o sufixo `.debug` no `applicationId`, então o APK de debug
  pode conviver com uma futura versão de produção no mesmo aparelho.
- O `gradle.properties` já vem com ajustes de memória conservadores
  (`-Xmx896m`, compilação Kotlin *in-process*, `workers.max=1`) e o `build.gradle.kts`
  limita o JVM de testes a **512 MB** — build, lint e testes rodam até em máquinas com 2 GB de RAM.
  Em máquinas maiores, pode aumentar `org.gradle.jvmargs` (ex.: `-Xmx2048m`).

---

## 4. Como instalar o APK

O APK entregue é **debug** (assinado com a chave de debug do Android), instalável
em qualquer aparelho Android 8.0 ou superior.

### 4.1 Pelo aparelho (fontes desconhecidas)

1. Copie `mochila-de-emergencia-debug.apk` para o celular (cabo USB, e-mail, Drive etc.).
2. Abra o arquivo pelo gerenciador de arquivos.
3. O Android vai pedir para permitir a instalação: toque em **Configurações** e ative
   **"Instalar apps desconhecidos"** (ou "Fontes desconhecidas") **para o app que está abrindo o
   arquivo** (Arquivos, Chrome, WhatsApp…).
   - Caminho manual: `Configurações → Apps → Acesso especial → Instalar apps desconhecidos`.
4. Volte e toque em **Instalar**.
5. Abra o app **Mochila de Emergência** na gaveta de apps.

### 4.2 Via ADB (recomendado para testes)

```bash
adb devices                 # confirme que o aparelho aparece como "device"
adb install -r mochila-de-emergencia-debug.apk
adb shell am start -n br.com.mochiladeemergencia.app.debug/br.com.mochiladeemergencia.app.MainActivity
```

Para desinstalar: `adb uninstall br.com.mochiladeemergencia.app.debug`

### 4.3 Observações

- Na primeira abertura o app cria o perfil **"Casa"** e o checklist padrão.
- **Atualizando da versão anterior:** o banco migra de v1 para v2 automaticamente
  (ganha localização do perfil, status "Repor", fotos e contatos) — **nada é apagado**.
- No Android 13+ o sistema pergunta se você permite notificações. **Se você negar, o app continua
  funcionando** e os avisos de validade aparecem no painel Início.
- Não é necessário criar conta, conectar à internet ou conceder qualquer outra permissão.

---

## 5. Como usar (roteiro rápido)

1. **Checklist** → as sugestões estão agrupadas por categoria; toque em
   *Adicionar à minha mochila* no que fizer sentido (as dicas explicam cada item).
   Ligue *Mostrar dica por pessoas* para ver estimativas — nada é alterado automaticamente.
2. **Minha Mochila** → ajuste quantidade, local e **validade** de cada item.
   Itens sem validade não geram alertas de vencimento.
3. **Item → Adicionar foto** → escolha da galeria (*Photo Picker* do sistema) ou tire uma foto.
   A foto fica só no aparelho e aparece como miniatura na lista.
4. **Início** → toque no card grande e escreva **onde a mochila está** ("Armário da lavanderia").
5. **Conferir mochila** → a cada item responda **Tenho**, **Falta** ou **Substituir**.
   Ao final aparece o resumo da conferência.
6. **Consumiu ou venceu?** → no item, *Marcar como consumido/substituído*: ele sai da mochila e
   entra em **Repor**. Quando comprar de novo, toque em **Repor** para devolvê-lo como "Tenho".
7. **Ajustes** → antecedência do aviso (**30/60/90 dias**, padrão **60**), notificações,
   **lembrete semestral** (padrão ligado), número de pessoas da casa e tema.
8. **Documentos** → salve links úteis e arquivos (PDF, imagem, vídeo, texto) escolhidos por você.
9. **Emergência** → toque em **Ligar** para abrir o discador com o número (o app não disca):
   SAMU 192, Bombeiros 193, Defesa Civil 199, Polícia 190.
   **Primeiros socorros** funciona sem internet; **Contatos** guarda seus contatos pessoais.
10. **Perfis** → crie "Casa da praia", "Sítio" etc. Cada perfil tem mochila, checklist, documentos,
    contatos e localização próprios.
11. **Ajustes → Backup local** → exporte um JSON de vez em quando (inclui fotos e contatos).
    Para restaurar, use *Importar backup* (o app valida o arquivo e pergunta se você quer
    **mesclar** ou **substituir**).

---

## 6. Arquitetura

```
app/src/main/java/br/com/mochiladeemergencia/app/
├── MainActivity.kt              # Activity única (Compose)
├── MochilaApp.kt                # Application: serviços, canal de notificação, agendamentos
├── di/ServicosLocais.kt         # Injeção de dependências manual
├── domain/
│   ├── StatusCalculadora.kt     # regras puras de validade (testável em JVM)
│   ├── PrimeirosSocorros.kt     # aviso, fonte, lembrete e os 6 cards (texto fixo)
│   └── model/Modelos.kt         # Categoria, StatusItem, TipoDocumento, Tema
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt       # Room versão 3 + MIGRACAO_1_2 + MIGRACAO_2_3 + banco em memória
│   │   ├── entity/Entidades.kt  # Perfil, Item, Documento, Checklist, Contato
│   │   └── dao/Daos.kt          # DAOs com Flow + suspend
│   ├── prefs/PreferenciasRepository.kt   # DataStore + interface ArmazenamentoConfiguracoes
│   ├── repository/              # Perfil, Item, Documento, Checklist, Contato
│   └── backup/                  # BackupRepository (JSON v3) + ArquivoBackup (SAF/FileProvider)
├── work/
│   ├── RegraNotificacao.kt      # regras puras de aviso (30/7/1/0, anti-duplicação)
│   ├── ValidadeWorker.kt        # CoroutineWorker diário
│   ├── RevisaoSemestralWorker.kt# CoroutineWorker de 182 dias
│   └── AgendadorAvisos.kt       # diário às 9h + lembrete semestral
├── ui/
│   ├── navigation/AppMochila.kt # Scaffold, bottom bar (5 abas), NavHost, FAB
│   ├── screens/                 # Inicio, Mochila, ItemForm, Conferir, Repor, Checklist,
│   │                            # Documentos, DocumentoForm, Emergencia, PrimeirosSocorros,
│   │                            # Contatos, Perfis, Configuracoes, Sobre
│   ├── components/Componentes.kt# cards, selo de status, estado vazio, MiniaturaFoto
│   ├── viewmodel/               # um ViewModel por tela + FabricaViewModel
│   ├── icones/Icones.kt         # ícones vetoriais próprios (mantém o APK leve)
│   └── theme/Theme.kt           # cores, tipografia e cores de status (claro/escuro)
└── util/                        # Fotos (JPEG + EXIF), Telefone (ACTION_DIAL),
                                 # Formatos (datas pt-BR), Notificacoes, IntentsDocumento
```

- **UI → ViewModel → Repositório → Room/DataStore**, com `StateFlow` e `collectAsStateWithLifecycle`.
- Sem DI pesada: `ServicosLocais` instancia banco, repositórios e agendador uma única vez.
- Regras de negócio puras (`StatusCalculadora`, `RegraNotificacao`, `PrimeirosSocorros`)
  não dependem do Android e são cobertas por testes de JVM.

### Correção de dicas antigas (v1.2.3)

A sincronização do checklist **insere** o que falta e remove o que saiu do código, mas não
reescrevia o texto de uma sugestão que já estava gravada. Quem instalou a 1.2.0 continuava
vendo a dica antiga da Bíblia ("Se fizer sentido para você") mesmo depois de atualizar.

Agora, ao abrir o app e a cada sincronização, `ChecklistRepository.corrigirTextosAntigos()`
compara a dica gravada das sugestões **padrão** com a lista de textos obsoletos
(`ChecklistPadrao.DICAS_ANTIGAS`, identificados por padrão de texto — tolera "para/pra",
"você/você/vc" e pontuação) e reescreve para o texto atual:

- só mexe em sugestão **padrão** (as personalizadas do usuário não são tocadas);
- só mexe quando a dica gravada **contém** um texto obsoleto conhecido — qualquer outro
  texto escrito pelo usuário é preservado;
- é idempotente: sem nada para corrigir, não escreve no banco.

---

### Fluidez (v1.2.2)

Nada de trabalho pesado na thread principal e nada de recriar estado ao trocar de aba:

| Onde | O que mudou |
|---|---|
| Miniaturas das fotos | decodificação do JPEG em `Dispatchers.IO` (`produceState`), com cache LRU de 8 MB e `RGB_565` |
| Listas | `LazyColumn` com `contentType` e `animateItem()`; cartões recebem `modifier` (sem recompor a lista inteira) |
| Troca de aba | ViewModels de Início, Mochila e Documentos vivem na `MainActivity` — a aba volta na hora, sem "carregando" |
| Cálculos | status/contagens em `Dispatchers.Default` + `distinctUntilChanged` (não recalcula nem reabre o banco à toa) |
| Inicialização do app | canal de notificação, `WorkManager` e *seed* do banco em `Dispatchers.IO` |
| Transições | fade + slide curtos no `NavHost` (180/220/120 ms) |
| Estado de UI | `@Immutable` nas classes de estado (Compose pula recomposições) |

### Modelo de dados (Room, versão 3)

| Entidade | Campos principais |
|---|---|
| `PerfilEntity` | `id`, `nome`, **`localizacao`**, `criadoEm` |
| `ItemEntity` | `id`, `perfilId` (FK cascade), `nome`, `categoria`, `quantidade`, `unidade`, `validadeEpochDay?`, `local`, `observacao`, `marcadoComoFalta`, `aRepor`, `fotoArquivo?`, `chaveChecklist?`, **`formaConsumo`** |
| `DocumentoEntity` | `id`, `perfilId` (FK cascade), `titulo`, `tipo`, `uri`, `categoria`, `observacao`, `arquivoLocal` |
| `ChecklistEntity` | `id`, `perfilId` (FK cascade), `chave` (única por perfil), `nome`, `categoria`, `quantidadeBase` (referência da dica), `unidade`, `porPessoa`, **`dica`**, `ordem`, `personalizado` |
| **`ContatoEntity`** | `id`, `perfilId` (FK cascade), `nome`, `telefone`, `parentesco`, `observacao`, `criadoEm` |

As migrações **1 → 2** e **2 → 3** são explícitas (colunas novas, tabela de contatos e
renomeação das categorias antigas); os schemas 1, 2 e 3 estão exportados em `app/schemas/`
e há teste automatizado de cada migração.

---

## 7. Regras de negócio

### 7.0 Checklist por categoria

- **54 sugestões** em 11 categorias (Água e bebidas, Comida de emergência, Utensílios,
  Iluminação e comunicação, Ferramentas e abrigo, Higiene, Saúde e primeiros socorros,
  Documentos e valores, Vestuário, Família e necessidades especiais, Conforto e apoio emocional).
- Cada sugestão tem **nome curto**, categoria e **dica opcional** (ex.: *"Prefira fósforos à
  prova d'água"*). Todas são **editáveis** e podem ser removidas; o usuário também cria as suas.
- **Nenhuma quantidade é imposta.** Ao adicionar à mochila, o item entra com quantidade **1** e a
  unidade sugerida; o ajuste fino é feito na tela Mochila. Se o usuário ligar *"Mostrar dica por
  pessoas"*, o app apenas **exibe** uma estimativa (ex.: *"Para 3 pessoas: cerca de 18 L"*) — não
  grava nada.
- O checklist é **sincronizado** a cada abertura: sugestões novas entram, sugestões que saíram do
  app somem e as edições do usuário são preservadas.
- O app **não sugere**: alimentos que precisam de geladeira ou cozimento elaborado, latas
  amassadas/estufadas, armas/munição/itens de caça, instruções de fabricação de fogo ou explosivos,
  procedimentos invasivos, nem medicamentos específicos (só "remédios de uso contínuo" e "cópia de
  receita").

### 7.1 Status calculado (precedência)

1. **Repor** — item consumido/substituído, aguardando reposição (azul)
2. **Vencido** — a data de validade já passou (vermelho)
3. **Vencendo** — faltam de 0 a *antecedência* dias (amarelo)
4. **Falta** — o usuário marcou o item como faltando (cinza)
5. **Tenho** — caso contrário (verde)

Itens **sem validade** nunca ficam "Vencendo"/"Vencido" e nunca geram notificação de validade.
"Marcar como consumido/substituído" coloca o item em **Repor** e limpa a validade antiga;
**Repor** devolve o item como **Tenho** (a validade pode ser informada de novo).

### 7.2 Notificações

- **Validade**: `ValidadeWorker` roda **uma vez por dia** (agendado para as 9h locais).
  Marcos de aviso: **antecedência configurada (30/60/90)** + **30, 7, 1 e 0 dias**.
  Uma **única notificação resumida** por dia; não repete no mesmo dia (controle em DataStore).
  Canal: **"Validade dos itens"**.
- **Revisão semestral**: `RevisaoSemestralWorker` a cada **182 dias**, canal próprio
  **"Revisão da mochila"**, texto fixo pedindo para revisar pilhas, repelente, isqueiro e lona.
  Pode ser desligado em **Ajustes → Lembrete semestral** (padrão: ligado).
- Permissão `POST_NOTIFICATIONS` (Android 13+): se negada, o app segue funcionando e os avisos
  aparecem no painel.
- Ambos usam WorkManager: sem serviço em primeiro plano, sem INTERNET.

### 7.3 Backup JSON (formato 3)

Exportação (`ACTION_CREATE_DOCUMENT` ou compartilhamento via `FileProvider`) e importação com
**validação completa antes de gravar**:

```json
{
  "aplicativo": "Mochila de Emergência",
  "versaoFormato": 3,
  "exportadoEm": 1794367200000,
  "configuracoes": {
    "antecedenciaDias": 60,
    "notificacoesAtivas": true,
    "pessoasCasa": 2,
    "tema": "SISTEMA",
    "lembreteSemestralAtivo": true
  },
  "perfis": [
    {
      "nome": "Casa",
      "localizacao": "Armário da lavanderia",
      "criadoEm": 1794367200000,
      "itens": [
        {
          "nome": "Água potável (3 dias)",
          "categoria": "AGUA_E_ALIMENTOS",
          "quantidade": 12.0,
          "unidade": "L",
          "validade": "2027-01-10",
          "local": "Bolso lateral",
          "observacao": "",
          "falta": false,
          "aRepor": false,
          "chaveChecklist": "agua",
          "formaConsumo": "ABRIR_E_COMER",
          "fotoBase64": null
        }
      ],
      "documentos": [
        {
          "titulo": "Defesa Civil",
          "tipo": "LINK",
          "uri": "https://www.gov.br/defesacivil",
          "categoria": "Contatos",
          "observacao": "",
          "arquivoLocal": false
        }
      ],
      "checklist": [
        {
          "chave": "agua",
          "nome": "Água potável (3 dias)",
          "categoria": "AGUA_E_ALIMENTOS",
          "quantidadeBase": 6.0,
          "unidade": "L",
          "porPessoa": true,
          "dica": "2 litros por pessoa por dia; 6 litros por pessoa para 72 horas",
          "ordem": 0,
          "personalizado": false
        }
      ],
      "contatos": [
        {
          "nome": "Vizinha Ana",
          "telefone": "+5521999999999",
          "parentesco": "Vizinha",
          "observacao": "Tem chave de reserva"
        }
      ]
    }
  ]
}
```

Limites aplicados na importação: **12 MB**, 50 perfis, 2.000 itens por perfil (10.000 no total),
500 documentos, 300 sugestões e **200 contatos** por perfil; **620.000 caracteres** por foto
em Base64. Campos validados: tipo, obrigatoriedade, faixa numérica, data ISO (`AAAA-MM-DD`),
link `http/https` ou `content://`, telefone (só dígitos e `+ * #`, máx. 24 caracteres).
As fotos do backup são **regravadas com nome novo** (o nome vindo do JSON é ignorado), o que
elimina qualquer risco de *path traversal*. A exportação recusa gerar um arquivo acima do limite.

---

## 8. Testes

### 8.1 Testes automatizados (JVM — rápidos, sem emulador)

```bash
./gradlew testDebugUnitTest
```

**73 testes, todos passando:**

| Classe | Testes | Cobre |
|---|---|---|
| `StatusCalculadoraTest` | 13 | status Tenho/Falta/Vencendo/Vencido, limites de antecedência (30/60/90), dias até vencer, marcos de aviso, textos em pt-BR |
| `RegraNotificacaoTest` | 11 | avisos somente nos marcos 30/7/1/0 + antecedência, itens sem validade, itens vencidos, agrupamento da mensagem, não repetir no mesmo dia |
| `BackupRepositoriosTest` | 15 | Room em memória: CRUD de itens, validação, cascata, documentos, exportar→validar→importar (mesclar e substituir), JSON inválido sem alterar dados, limites, checklist |
| `NovosFluxosTest` | 14 | precedência de **Repor**, consumido → Repor, `repor()` → Tenho, conferência (Tenho/Falta/Substituir), localização do perfil, **foto salva + miniatura + nomes inválidos recusados**, **foto no backup (Base64)**, contatos (CRUD, validação, cascata, backup), conteúdo dos 6 cards, constantes do lembrete semestral |
| `ChecklistCategoriasTest` | 17 | conteúdo das 54 sugestões (categorias, nomes curtos, dicas), **itens proibidos**, dica de fósforos/água, **CVV removido**, quantidade não imposta, dica por pessoa, sugestão personalizada, sincronização e restauração, categorias antigas, forma de consumo |
| `MigracaoBancoTest` | 3 | migração **v1 → v2**, migração **v2 → v3** (dica, forma de consumo, renomeação de categorias) e criação de banco novo |

Relatório HTML: `app/build/reports/tests/testDebugUnitTest/index.html`

### 8.2 Teste instrumentado (requer aparelho/emulador)

```bash
./gradlew connectedDebugAndroidTest
```

`InicializacaoAppTest` abre a `MainActivity`, confere que os serviços locais existem
e recria a Activity (rotação).

### 8.3 Roteiro de teste manual sugerido

1. Cadastrar item **com** validade e **sem** validade; adicionar **foto** (galeria e câmera).
2. Editar, excluir (confirmar o diálogo) e conferir que a foto sai do armazenamento.
3. Buscar por nome e filtrar por categoria/status (incluindo **Repor**).
4. Definir a **localização da mochila** no Início e reabrir o app.
5. Marcar um item como **consumido** → conferir que ele aparece em **Repor** → tocar em **Repor**.
6. Fazer uma **conferência** completa (Tenho/Falta/Substituir) e conferir o resumo final.
7. Ajustar antecedência para 30 e 90 e conferir o card "Vencendo em N dias".
8. Alterar a data do aparelho (ou usar `adb shell cmd jobscheduler run -f
   br.com.mochiladeemergencia.app.debug 1`) para testar os avisos 30/7/1/0.
9. Negar a permissão de notificação e confirmar que o painel continua mostrando avisos.
10. Criar um segundo perfil, alternar e conferir o isolamento (itens, fotos, contatos, local).
11. Em **Contatos**, criar/editar/excluir um contato pessoal e tocar em **Ligar**.
12. Em **Primeiros socorros**, expandir os 6 cards e conferir o aviso no topo e em cada card.
13. **Checklist**: conferir os grupos por categoria, usar o filtro "Só o que falta", criar uma
    sugestão personalizada com dica e testar "Restaurar checklist padrão".
14. **Item de comida**: escolher a **forma de consumo** e conferir que aparece na lista da mochila.
15. Salvar link e arquivo local em Documentos e abrir com app externo.
16. Exportar backup **com fotos e contatos**, alterar dados, importar **mesclando** e **substituindo**.
17. Tentar importar um arquivo de texto qualquer: a mensagem de erro deve aparecer e
    **nenhum dado** pode ser apagado.
18. Girar a tela, trocar o tema, **aumentar bastante a fonte do sistema** e conferir que os rótulos
    da barra inferior (em especial "Documentos") continuam em uma linha.
19. Desligar o **lembrete semestral** em Ajustes e confirmar que o trabalho é cancelado.

---

## 9. Privacidade, permissões e segurança

| Permissão | Origem | Justificativa |
|---|---|---|
| `POST_NOTIFICATIONS` | app | avisos de validade e lembrete semestral no Android 13+ (opcional) |
| `WAKE_LOCK` | WorkManager | execução dos trabalhos periódicos |
| `RECEIVE_BOOT_COMPLETED` | WorkManager | reagendar a verificação após reiniciar o aparelho |
| `FOREGROUND_SERVICE` | WorkManager | declarada pela biblioteca (o app não usa serviços em primeiro plano) |
| `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | androidx.core | permissão **privada** do próprio app |

- **Sem** `INTERNET`, `CALL_PHONE`, `CAMERA`, `READ_MEDIA_*` ou `READ/WRITE_EXTERNAL_STORAGE`
  — confirmado no APK final (`aapt2 dump permissions`).
- **Câmera e galeria sem permissões:** galeria pelo **Photo Picker** do sistema e câmera por
  `ACTION_IMAGE_CAPTURE` com `FileProvider` (o app não lê sua galeria nem pede acesso a ela).
- **Ligações sem permissão de telefone:** todos os botões usam `ACTION_DIAL`, que abre o discador
  com o número preenchido — **quem disca é o usuário**.
- **Fotos** ficam no armazenamento interno do app, recomprimidas em JPEG (máx. 1024 px),
  com nome gerado por UUID; metadados EXIF (inclusive GPS) são descartados na recomposição.
- **Sem** WebView, sem analytics, sem SDKs de anúncio, sem telemetria.
- Backup automático do Android **desativado** (`allowBackup="false"` + `data_extraction_rules`).
- `FileProvider` **não exportado**, expondo somente `cache/exports/`, `cache/fotos_temp/` e
  `files/fotos/`.
- Exportado no APK: apenas a `MainActivity` (launcher).
- Logs: apenas a categoria do erro (`ValidadeWorker`), nunca dados do usuário.
- Nenhuma chave, senha ou segredo no código.
- O app declara apenas as **`<queries>`** necessárias (abrir `https`, abrir discador com `tel`
  e usar `IMAGE_CAPTURE`).

Detalhes completos na [revisão de bugs e segurança](RELATORIO-REVISAO.md).

---

## 10. Limitações conhecidas

- Os **documentos** guardam a URI escolhida (`content://`). Se o arquivo for movido/apagado ou o
  app que o fornece for desinstalado, será preciso escolhê-lo novamente. URIs de outro aparelho
  (após backup) normalmente precisam ser reescolhidas.
- **Links** exigem internet e abrem no navegador/app externo escolhido pelo usuário.
- O aviso diário e o lembrete semestral dependem do WorkManager e das otimizações de bateria do
  fabricante; em aparelhos muito agressivos podem atrasar. O painel sempre mostra o estado atual.
- **Primeiros socorros** são **informativos**: não substituem treinamento prático nem atendimento
  médico. Não há orientação de combate a incêndio nem procedimentos invasivos.
- As **fotos** ocupam ~400 KB por item no armazenamento interno; um backup com muitas fotos pode
  chegar ao limite de 12 MB (a exportação avisa e orienta a remover fotos).
- Não há sincronização entre aparelhos (por escolha de projeto). Use o backup JSON.
- Não há gráfico/tela de estatísticas históricas: o app mostra o estado atual da mochila.
- As **dicas de quantidade** do checklist são estimativas (2 L de água por pessoa/dia; 6 L por
  pessoa para 72 h) e não substituem o bom senso: ajuste na tela Mochila conforme a sua casa.
- O APK entregue é **debug**; não há chave de assinatura de produção no projeto.
- Interface em **português do Brasil** apenas (os textos de UI estão no código, não em resources).

---

## 11. Versões das dependências

```
AGP 8.13.2 · Kotlin 2.2.21 · KSP 2.2.21-2.0.5 · Gradle 8.13
Compose BOM 2025.10.01 · Material3 · Navigation Compose 2.9.6
Room 2.8.3 · DataStore 1.1.7 · WorkManager 2.10.5 · Activity Compose 1.11.0
Lifecycle 2.9.4 · Core KTX 1.17.0 · Desugar JDK libs 2.1.5
Testes: JUnit 4.13.2 · Robolectric 4.16 · AndroidX Test 1.7.0/1.3.0
```

---

## 12. Licença e responsabilidade

Projeto independente, sem vínculo com órgãos públicos ou empresas.
Você é responsável pelos conteúdos (links e arquivos) que salvar no app.
Este aplicativo **não substitui** orientação profissional de emergência.
Em qualquer emergência grave, ligue **192 (SAMU)** ou **193 (Bombeiros)**.
