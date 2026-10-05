# Blueprint técnico — Reescrita das telas em Jetpack Compose

Data: 17/09/2026  
Status: proposta para revisão e aprovação; nenhuma tarefa de implementação foi executada.

## 1. Contexto e contrato funcional

### 1.1 Objetivo

Substituir por Jetpack Compose toda a interface visível do app de CEP: pesquisa, lista de CEPs armazenados, barra superior e navegação. Preservar a aparência, os textos e o fluxo existentes, exceto as duas alterações expressamente aprovadas: todo feedback transitório será Toast, sem erro no campo; e a entrada parcial sobreviverá à restauração da tarefa pelo Android quando houver estado recuperável.

O app continua aceitando apenas CEP. A máscara remove não numéricos, limita a oito dígitos, preserva zeros e mostra `00000-000`. A consulta só ocorre no botão. CEP local é retornado sem expiração, sem rede e sem aviso; miss local chama ViaCEP e salva o sucesso. Toda consulta bem-sucedida registra recência, mantém unicidade e ordena a lista do mais recente ao mais antigo. Falha não muda histórico nem apaga o endereço anterior. Restauração do último endereço e navegação não registram consulta. Uma restauração inicial atrasada não sobrescreve pesquisa iniciada depois dela.

### 1.2 Arquitetura encontrada

| Camada | Código atual | Responsabilidade preservada |
| --- | --- | --- |
| Entrada Android | `MainActivity.kt`, hoje `AppCompatActivity` + ViewBinding + `NavHostFragment` | Ponto único de entrada e proprietário do `CepViewModel` compartilhado. |
| Apresentação | `SearchFragment.kt`, `SavedAddressesFragment.kt`, `SavedAddressesAdapter.kt`, XML e `AddressFormatting.kt` | Entrada, renderização, navegação e Toasts; serão substituídos por Compose. |
| Estado | `ui/CepViewModel.kt`, `ui/CepUiState.kt` | `StateFlow<CepScreenState>` e `StateFlow<SavedAddressesState>`; consulta, observação, restauração e feedback com IDs. |
| Domínio | `domain/model/Address.kt`, `domain/util/CepFormatter.kt`, casos de uso e contrato do repositório | Entidade, formatação/validação, casos de uso; sem dependência de Compose. |
| Dados | `data/repository/CepRepositoryImpl.kt`, Room e Retrofit | Local primeiro, salvamento, recência e ViaCEP; sem mudança de regra ou esquema. |
| Composição | `AppContainer.kt`, `CepApplication.kt` | Injeção manual e fábrica do ViewModel, adaptada apenas se necessária ao estado salvável. |

Projeto de um módulo `:app`, Kotlin, Gradle Wrapper 9.7.1, AGP 9.3.2, `compileSdk` 37, `minSdk` 24, `targetSdk` 36, Java 11, Room 2.8.4, Lifecycle 2.10.0, Navigation 2.10.0 e KSP 2.3.10. O AGP 9 já fornece suporte Kotlin embutido; não aplicar `org.jetbrains.kotlin.android` por hábito. O projeto usa catálogo `gradle/libs.versions.toml`, testes JUnit/Robolectric em `app/src/test` e não mantém testes instrumentados. [Suporte Kotlin no AGP 9](https://developer.android.com/build/migrate-to-built-in-kotlin).

### 1.3 Premissas técnicas e limites

- Manter a única Activity e o mesmo `applicationId`, banco Room e migrações. Não há alteração de schema, seed nem dado remoto.
- Adotar Navigation Compose 2.10.0, alinhada à versão Navigation já declarada. Usar duas rotas sem argumentos: `search` (inicial) e `saved_addresses`; a ação de voltar remove a rota da lista. Isso substitui o grafo de Fragments e conserva o histórico de navegação.
- Usar o `CepViewModel` no escopo da Activity, não criar um ViewModel por destino. O estado compartilhado conserva entrada e resultado ao voltar e durante rotação.
- Guardar a entrada de CEP no `SavedStateHandle` do ViewModel. Esse valor simples é o estado mínimo a recuperar depois de encerramento do processo pelo sistema; `Address` e a lista são reconstruídos de Room. Não manter uma segunda cópia editável da entrada em `rememberSaveable`.
- Usar Compose Material 3 e recursos de strings/cores existentes. `remember` serve apenas a estado local de apresentação, como scroll/navegação; a entrada e o resultado de negócio vêm do ViewModel. `mutableStateOf` poderá ser demonstrado em um exemplo didático de estado local, sem criar fonte concorrente de verdade para o CEP.
- Não introduzir mutex, serialização do repositório, guarda adicional contra consultas simultâneas ou releitura defensiva antes de salvar. Campo e botão continuam bloqueados durante a consulta; não mudar o contrato de unicidade e atomicidade existente.
- Executar apenas testes locais pelo Gradle Wrapper. Não planejar ADB, emulador, aparelho ou `connectedDebugAndroidTest`.

## 2. Modelos de dados e mapeamento

### 2.1 Modelos existentes e alteração mínima

| Modelo/campo | Tipo e padrão | Origem, validação e uso | Requisitos |
| --- | --- | --- | --- |
| `CepScreenState.status` | `CepUiState`, inicial `Idle` | `Idle`, `Loading`, `Success(Address)` ou `Error(Throwable)`; controla progresso, bloqueio e resultado. | RF-001, RF-003, RF-005–RF-007, RF-011; RN-005, RN-008 |
| `CepScreenState.input` | `String`, inicial `""` | Valor mascarado apresentado no campo e entregue à consulta. Espelhar no `SavedStateHandle` após edição, falha e limpeza no sucesso. | RF-002, RF-003, RF-006, RF-007, RF-010–RF-012; RN-001 |
| `CepScreenState.lastSuccessfulAddress` | `Address?`, inicial `null` | Resultado da consulta ou restauração da última consulta persistida; permanece durante `Loading` e `Error`. | RF-001, RF-006, RF-007, RF-010–RF-012, RF-014; RN-005 |
| `CepScreenState.feedback` | `CepFeedback?`, inicial `null` | `id`, `type` e possível `cause`. Evento transitório; consumir após Toast, sem persistir no estado da tarefa. | RF-003, RF-006, RF-007, RF-011; RN-006 |
| `SavedAddressesState.addresses` | `List<Address>`, inicial vazia | Fluxo local já ordenado por recência; renderizar em `LazyColumn` com chave do CEP. | RF-008, RF-009, RF-013, RF-014; RN-004, RN-007 |
| `SavedAddressesState.hasLoaded` | `Boolean`, inicial `false` | Distingue lista carregada vazia de carregamento/falha inicial. | RF-009, RF-013 |
| `SavedAddressesState.readError` e `errorFeedbackId` | `Throwable?` / `Long?` | Preservar dados anteriores após falha e apresentar Toast genérico uma vez por ID. | RF-013; RN-006 |
| `Address.zipCode` | `String`, oito dígitos | Identidade única; apresentação com máscara. | RF-004, RF-008, RF-014; RN-001, RN-007 |
| `Address.street`, `complement`, `neighborhood`, `city`, `stateAbbreviation`, `state` | `String` cada | Se vazio, mostrar “Não informado”; manter os rótulos dos recursos atuais. | RF-006, RF-008, RF-014 |
| Ordem de consulta local | Persistida no Room existente | Não expor nem alterar seu schema; determina lista e último endereço. | RF-001, RF-004, RF-008, RF-010; RN-004 |

O `SavedStateHandle` armazena somente `input` (`String`, chave estável como `cep_input`), com valor padrão vazio. O ViewModel continua sendo a fonte de verdade em execução: inicializa `CepScreenState.input` com o valor restaurado e escreve no handle toda vez que a entrada muda. Depois de sucesso, limpa ambos. Após falha, conserva ambos. Na criação de uma sessão sem bundle salvo, o valor é vazio. Não persistir `Loading`, exceções, feedback ou endereço no handle: após morte do processo, a operação anterior não é retomada automaticamente e o último endereço é lido de Room.

### 2.2 Relações e validações

- `Address` não ganha campos. `AddressEntity` e o banco versão 4 permanecem intactos.
- `CepFormatter.format` e `GetAddressByCepUseCase` seguem responsáveis por máscara/normalização e validação central; a UI não consulta DAO nem Retrofit. Colagem excessiva continua truncada na máscara antes de disparar consulta.
- Reutilizar `AddressFormatting.kt` para produzir as sete linhas com recursos de strings em ambos os cartões. O composable recebe `Address` e usa o `Context` atual apenas para essa formatação de apresentação, evitando duas implementações divergentes. Nenhuma operação de dados é feita pelo cartão.

## 3. Componentes e módulos

| Componente/arquivo | Ação planejada | Contrato principal |
| --- | --- | --- |
| `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts` | Acrescentar Compose BOM, plugin do compilador compatível, Material 3, Activity Compose, Lifecycle Compose, Navigation Compose e dependências de teste local. Habilitar `buildFeatures.compose`. | Compilar UI Compose sem atualizar ferramentas não relacionadas. |
| `ui/CepViewModel.kt`, `AppContainer.kt` | Integrar `SavedStateHandle` à fábrica com `CreationExtras`; inicializar/sincronizar `input`, mantendo consulta, recência e proteção da restauração atrasada. | RF-010–RF-012, RN-008. |
| `ui/compose/CepTheme.kt` (novo) | Esquemas claro/escuro a partir das cores existentes, com contraste legível. | RF-001, RF-008, critérios visuais. |
| `ui/compose/AddressCard.kt` (novo) | Cartão reutilizável para resultado e lista, com as sete linhas e “Não informado”. | RF-006, RF-008, RF-014. |
| `ui/compose/SearchScreen.kt` (novo) | Composable sem acesso a dados: campo, máscara via callback, botões, progresso, cartão, rolagem. | RF-001–RF-007, RF-010–RF-012. |
| `ui/compose/SavedAddressesScreen.kt` (novo) | Composable de lista: título, `LazyColumn`, estado vazio/carregamento e cartões. | RF-008, RF-009, RF-013, RF-014. |
| `ui/compose/CepApp.kt` (novo), `MainActivity.kt` | `setContent`, tema, barra superior, `NavHost`, coleta dos StateFlows e entrega de eventos ao ViewModel. | RF-001, RF-008, RF-010, RF-011, RN-006. |
| `app/src/main/res/values/strings.xml`, cores/temas | Reusar textos; acrescentar apenas descrições de acessibilidade se necessárias. Manter tema Android sem ActionBar para janela. | Mensagens e aparência. |
| `SearchFragment.kt`, `SavedAddressesFragment.kt`, `SavedAddressesAdapter.kt`, quatro layouts XML e `main_navigation.xml` | Remover após a substituição funcionar e os testes serem migrados. Limpar imports e dependências de ViewBinding, Navigation Fragment, RecyclerView e ConstraintLayout se não houver uso restante. | Evitar duas implementações ativas. |
| `app/src/test/...` | Adaptar os testes ligados a Views/Fragments e acrescentar verificação local de Compose, estado salvável e fluxo com Room real/remoto controlado. | Todos os critérios de aceite. |
| `README.md`, documentação desta feature | Descrever a UI real e registrar evidências de validação sem reescrever relatórios históricos. | Entrega e aprendizado. |

## 4. Interfaces

| Interface | Entrada | Saída e comportamento | Erros |
| --- | --- | --- | --- |
| `SearchScreen(state, onInputChange, onSearch, onOpenSaved)` | `CepScreenState`, `(String) -> Unit` para edição e callbacks `() -> Unit` para ações | Renderiza `Column` rolável; campo numérico usa valor `state.input`; ao editar chama `formatZipCode` e envia ao ViewModel; botão consulta apenas por clique. `Row` alinha botão e progresso; `Box`/cartão compõe resultado. | Não cria Toast nem consulta dados dentro do composable. Não apresenta `isError` ou texto de erro no campo. |
| `SavedAddressesScreen(state)` | `SavedAddressesState` | `LazyColumn` com `items(..., key = { it.zipCode })`; título só com registros, mensagem vazia só após `hasLoaded`; cartões não clicáveis. | Se `readError` ocorrer, mantém registros anteriores e não finge lista vazia. |
| `AddressCard(address, title?)` | `Address` e título opcional | CEP formatado e seis campos textuais com rótulos existentes; valor vazio vira “Não informado”. | Não dispara consulta ou escrita. |
| `CepApp(viewModel)` | ViewModel único, recursos e tema | Coleta `screenState`/`savedAddressesState` com ciclo de vida, hospeda `search` e `saved_addresses`, seleciona título da barra pelo destino, navega uma vez por clique e usa `popBackStack`/`navigateUp` para voltar. | Eventos de feedback são consumidos por ID após apresentação, fora da renderização de composables. |
| `CepViewModel.updateInput(value)` / `search(value)` | CEP mascarado | Atualiza estado e handle; consulta usa caso de uso existente; sucesso limpa entrada, falha preserva. | `InvalidCepException`, `CepNotFoundException`, `IOException` e demais causas são classificados pela apresentação nos Toasts existentes. Cancelamento se propaga. |
| `CepViewModel.loadLatestAddress()` | Sem parâmetro | Leitura local do último endereço sem consulta nem alteração de recência. Invocar na entrada da Activity, não a cada recomposição ou retorno de rota. | Falha gera feedback genérico; leitura atrasada não sobrescreve pesquisa nova. |

Rotas são identificadores internos sem CEP como argumento. A Activity obtém o ViewModel pela fábrica manual e o passa ao composable raiz; destinos recebem estados e callbacks, sem chamar repositório. O botão Voltar do Android usa a pilha do `NavHost`; o ícone de voltar da barra chama a mesma operação. `navigate` para a lista deve evitar empilhar cópias com cliques repetidos, sem acrescentar guardas de consulta.

## 5. Gestão de estado, eventos e ciclo de vida

### 5.1 Fonte de verdade

`CepScreenState` permanece no `StateFlow` do ViewModel. `collectAsStateWithLifecycle` converte os fluxos em estado observável pelo Compose. A recomposição deve apenas refletir valores: nenhuma chamada à API, leitura de Room, Toast, navegação ou atualização de recência pode ocorrer no corpo de um composable. `SavedStateHandle` restaura a entrada simples após morte do processo pelo sistema; Room restaura o último endereço. `remember`/`rememberSaveable` ficam restritos a estado visual local, sem duplicar `input`. [Estado e coleta com ciclo de vida](https://developer.android.com/develop/ui/compose/state), [estado salvável](https://developer.android.com/topic/libraries/architecture/saving-states).

### 5.2 Transições

| Estado/ação | Guarda e operação | Resultado |
| --- | --- | --- |
| Criação sem bundle | `input = ""`; iniciar `loadLatestAddress()` uma vez na raiz | Endereço salvo aparece sem Toast; se não houver, cartão oculto. |
| Criação com bundle recuperável | `input` do handle; iniciar restauração local do endereço | Entrada parcial reaparece; sem nova consulta. |
| Editar campo | Formatar e limitar; `updateInput` sincroniza `StateFlow` e handle | Recomposição do campo, sem consulta. |
| Consultar entrada inválida | Caso de uso valida antes de acessar dados | `Error`, `LookupFailed`, Toast de CEP inválido; entrada/resultado preservados. |
| Consultar entrada válida | `Loading`; bloquear campo/botão e exibir progresso | Resultado anterior pode continuar visível. |
| Sucesso local/remoto | Caso de uso/repositório confirma consulta e persistência | `Success`, endereço novo, entrada/handle vazios, Toast uma vez. |
| Falha local/remota | Caso de uso retorna causa | `Error`, entrada e resultado anterior preservados; Toast correspondente. |
| Abrir/fechar lista | Mudar rota apenas | Estado do ViewModel e recência preservados. |
| Lista recebe emissão | Atualizar `SavedAddressesState` | `LazyColumn` reflete ordem/valores atuais. |
| Rotação | Activity recriada, ViewModel retido | Campo, endereço e consulta em andamento continuam; evento consumido não reaparece. |
| Morte e restauração do processo | ViewModel novo com `SavedStateHandle`, leitura de Room | Entrada parcial e último endereço; não retomar automaticamente consulta antiga nem reapresentar Toast. |

### 5.3 Feedback exatamente por ocorrência na UI

Manter os IDs de `CepFeedback` e `errorFeedbackId` da lista. Na raiz Compose, coletar eventos enquanto o ciclo de vida estiver `STARTED`, apresentar Toast e chamar `consumeFeedback(id)` ou `consumeSavedAddressesError(id)` após a apresentação. Uma nova coleta encontra `null`, por isso não repete Toast ao voltar, girar ou recompor. Não usar `LaunchedEffect(state.status)` para Toast: `Error` persistente não é evento. Não persistir feedback já apresentado no handle. Mensagens:

| Causa | Recurso existente |
| --- | --- |
| Sucesso de consulta | `toast_zip_code_saved` (“Endereço disponível.”) |
| `InvalidCepException` | `error_invalid_zip_code` (“Digite um CEP válido”) |
| `CepNotFoundException` | `error_zip_code_not_found` (“CEP não encontrado”) |
| `IOException` | `error_network` (“Não foi possível consultar o CEP. Verifique sua conexão e tente novamente.”) |
| HTTP, Room, erro da lista/restauração e demais falhas | `error_unexpected` (“Não foi possível concluir a consulta. Tente novamente.”) |

Não apresentar erro inline no campo. O estado `Error` pode continuar no ViewModel para descrever a última tentativa, mas não causa Toast por si só.

## 6. Algoritmos e regras existentes

### 6.1 Entrada e consulta (RN-001–RN-005, RN-008)

```text
onInputChange(texto):
    visivel = CepFormatter.format(texto)  // dígitos, no máximo 8, máscara
    viewModel.updateInput(visivel)         // StateFlow + SavedStateHandle

onConsultar():
    viewModel.search(estado.input)
    caso de uso valida 8 dígitos antes de consultar dados
    repositório: encontrar local e registrar recência atomicamente
    se miss confirmado: consultar ViaCEP; se sucesso, salvar e registrar recência
    sucesso: mostrar endereço, limpar entrada/handle, gerar feedback de sucesso
    falha: conservar entrada/handle e endereço anterior, gerar feedback de falha
```

O fluxo de dados acima já está implementado nas camadas de domínio/dados. As tarefas de UI devem reutilizá-lo, sem duplicar validação de negócio nem alterar Room/Retrofit. A chamada a `format` na borda visual preserva o truncamento antes da validação. `CancellationException` não vira Toast.

### 6.2 Lista e restauração (RN-004, RN-006, RN-007)

```text
na criação da raiz:
    solicitar restauração local se o estado funcional ainda é Idle
    apresentar o endereço restaurado sem marcar nova consulta e sem Toast de sucesso

na tela da lista:
    observar SavedAddressesState
    se addresses não vazia: título + cartões na ordem recebida
    senão se hasLoaded e sem erro: mensagem de lista vazia
    senão: não declarar vazio
    se errorFeedbackId novo: Toast genérico e consumir esse ID
```

A lista não chama `search`, não modifica ordem e não oferece clique para consultar. Reabrir depois de falha aciona o mecanismo existente `observeSavedAddresses()` para retomar leitura, sem repetição de Toast antigo.

## 7. Plano de implementação por fases

Cada tarefa abaixo é uma unidade de entrega para execução futura, sujeita à aprovação do usuário. Os testes indicados são locais em `app/src/test`; “integração” significa integração local com Robolectric, Room isolado e remoto controlado. Nenhuma tarefa prevê teste instrumentado.

### Fase A — Preparação e estado

#### T-001 — Configurar Compose e a infraestrutura de teste local

- **O que:** adicionar `buildFeatures.compose = true`, alias do plugin `org.jetbrains.kotlin.plugin.compose` no catálogo/raiz/módulo e dependências mínimas: Compose BOM estável fixada, Material 3, Activity Compose, Lifecycle Runtime Compose e Navigation Compose na versão Navigation atual. Adicionar `testImplementation(platform(BOM))` e `testImplementation(ui-test-junit4)` para testes locais, mais `debugImplementation(ui-test-manifest)` se exigido pelo host de teste. Não adicionar `androidTestImplementation` nem atualizar AGP, SDK, Room ou KSP sem necessidade concreta.
- **Onde:** `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts`.
- **Decisão de versão:** a documentação oficial consultada em 17/09/2026 indica BOM estável `2026.08.00`, compatível com `compileSdk 37`/AGP 9. A versão do plugin Compose Compiler deve corresponder à versão Kotlin efetivamente usada pelo AGP/KSP no projeto; verificar a resolução antes de fixá-la. Não adicionar o plugin Kotlin Android, já coberto pelo AGP 9. [Setup oficial](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler), [BOM](https://developer.android.com/develop/ui/compose/bom), [compiler Kotlin](https://kotlinlang.org/docs/compose-compiler-migration-guide.html).
- **Dependências:** nenhuma.
- **Aceite:** sincronização Gradle e compilação de um composable mínimo sem interferir na UI XML ainda ativa; dependências resolvidas de forma determinística.
- **Testes:** build `assembleDebug` e execução de um teste Compose mínimo sob Robolectric para validar o host local. Não criar teste superficial permanente apenas para espelhar configuração; o teste pode evoluir para os cenários de T-007.
- **Complexidade:** média, pela compatibilidade de plugin/AGP/KSP.
- **Aprendizado:** explicar BOM, plugin de compilação, `@Composable` e diferença entre descrever a UI e inflar XML.

#### T-002 — Persistir a entrada parcial no estado salvável

- **O que:** adicionar `SavedStateHandle` ao `CepViewModel`, inicializar `CepScreenState.input` pelo handle e sincronizar edições, falhas e limpeza no sucesso. Adaptar `CepViewModelFactory` para criar o handle via `CreationExtras` e preservar a composição manual em `AppContainer`. Conservar `loadLatestAddress()`, cancelamento e proteção contra restauração atrasada. Não persistir feedback nem endereço no handle.
- **Onde:** `ui/CepViewModel.kt`, `AppContainer.kt` se a fábrica exigir adaptação, `app/build.gradle.kts` apenas para dependência explícita de saved state se faltar.
- **Dependências:** T-001 apenas se a verificação conjunta for usada; a lógica é independente de Compose.
- **Aceite:** CEP parcial sobrevive a recriação/estado de tarefa recuperável; sucesso limpa handle; erro o conserva; sessão sem estado começa vazia. RF-010–RF-012, RN-005, RN-008.
- **Testes unitários:** ampliar `ui/CepViewModelTest.kt` para handle preenchido/vazio, edição, sucesso, falha e restauração atrasada. **Integração local:** simular save/restore do estado da Activity com novo ViewModel em T-008. **E2E local:** coberto em T-008.
- **Complexidade:** média.
- **Aprendizado:** comparar `remember`, `rememberSaveable` e `SavedStateHandle`; demonstrar por que o CEP editável tem uma fonte de verdade.

### Fase B — Componentes visuais

#### T-003 — Definir tema Compose e cartão de endereço compartilhado

- **O que:** criar `CepTheme` claro/escuro com azul `#00416B`, amarelo `#FFD400` e secundário `#0083CA`, respeitando recursos existentes; definir superfícies e cores de texto legíveis em ambos os modos. Criar cartão reutilizável com as sete linhas na ordem atual e “Não informado” para vazios. Preservar margens de 8/16/24/32 dp e textos de 14/18/20 sp como referência, ajustando só o necessário para legibilidade. Reutilizar `Context.formatAddress` de `AddressFormatting.kt` para o texto dos dois cartões.
- **Onde:** novos `ui/compose/CepTheme.kt`, `ui/compose/AddressCard.kt`; possivelmente `AddressFormatting.kt`, `res/values/colors.xml`, `res/values-night/themes.xml`, `res/values/strings.xml` se houver necessidade justificada.
- **Dependências:** T-001.
- **Aceite:** cartão de pesquisa e cartão de lista mostram o mesmo endereço, CEP mascarado, seis campos e valores substitutos; tema claro/escuro mantém identidade visual e texto legível. RF-001, RF-006, RF-008, RF-014.
- **Testes locais:** teste de formatação compartilhada para todos os campos vazios e preenchidos; teste Compose/Robolectric de semântica e sete linhas em tema claro/escuro. Conferência visual por preview, se disponível, como complemento sem substituir teste local.
- **Complexidade:** média.
- **Aprendizado:** `@Composable`, parâmetros, `Column`, `Row`, `Box`, `MaterialTheme` e recomposição de um cartão quando `Address` muda.

#### T-004 — Construir a tela de pesquisa declarativa

- **O que:** criar `SearchScreen` recebendo estado e callbacks, com rótulo, campo numérico, botão de consulta, progresso, cartão de resultado e botão da lista. Usar `Column` rolável para altura pequena/teclado, `Row` para consulta/progresso e cartão de T-003. Aplicar máscara no callback sem ciclo de atualizações; valor exibido vem do ViewModel. Não usar `isError`, supporting text ou Toast dentro do corpo da tela. Manter resultado anterior em `Loading`/`Error` e desabilitar somente campo/botão de consulta durante `Loading`, conforme FRD.
- **Onde:** novo `ui/compose/SearchScreen.kt`; recursos de strings somente se necessários.
- **Dependências:** T-002, T-003.
- **Aceite:** RF-001–RF-007, RF-010–RF-012, RF-014; máscara, zeros, truncamento, bloqueio, progresso, limpeza e resultado anterior fiéis ao FRD.
- **Testes locais:** Compose/Robolectric para texto digitado/colado, clique único de consulta, ausência de consulta automática, estado Loading, sucesso/erro e rolagem em altura reduzida. Testar callbacks com estado controlado; fluxo com banco fica em T-008.
- **Complexidade:** alta.
- **Aprendizado:** fluxo unidirecional, `mutableStateOf` em exemplo local isolado, diferença entre estado local e `StateFlow`, e por que recomposição não deve iniciar a consulta.

#### T-005 — Construir a lista declarativa

- **O que:** criar `SavedAddressesScreen` com título condicional, estado vazio somente quando `hasLoaded` e sem erro, `LazyColumn` de cartões de T-003 com chave pelo CEP e visualização apenas. Preservar a ordem recebida e atualização automática do fluxo. Sem busca, clique, exclusão ou salvamento na lista.
- **Onde:** novo `ui/compose/SavedAddressesScreen.kt`.
- **Dependências:** T-003.
- **Aceite:** RF-008, RF-009, RF-013, RF-014, RN-004, RN-007; falha de leitura não vira estado vazio falso.
- **Testes locais:** Compose/Robolectric de lista não carregada, vazia carregada, dois cartões em ordem A/B, emissão de nova ordem B/A e estado com erro após dados prévios. Integração Room em T-008.
- **Complexidade:** média.
- **Aprendizado:** `LazyColumn` versus `Column`, chaves estáveis e recomposição da lista quando o fluxo emite nova ordem.

### Fase C — Navegação e feedback

#### T-006 — Substituir a Activity, barra superior e navegação

- **O que:** fazer `MainActivity` chamar `setContent { CepTheme { CepApp(...) } }`, conservando ViewModel no escopo da Activity. Criar `Scaffold` e barra superior Compose com títulos e cores atuais; `NavHost` com duas rotas e volta pela barra/sistema. Coletar `screenState` e `savedAddressesState` com ciclo de vida. Chamar `loadLatestAddress()` uma vez por criação da raiz, jamais no corpo recomponível ou em cada retorno da lista. Retomar `observeSavedAddresses()` ao reabrir a lista depois de falha. Observar IDs de feedback em efeito vinculado ao ciclo de vida, mostrar Toast e consumir cada ID; invalid/inexistente usam Toast, sem erro inline. Ajustar insets de barra/teclado para manter ações alcançáveis.
- **Onde:** `MainActivity.kt`, novo `ui/compose/CepApp.kt`, eventualmente `res/values/themes.xml` e `AndroidManifest.xml` apenas se necessário para janela/insets; não alterar `windowSoftInputMode=adjustResize` sem motivo validado.
- **Dependências:** T-002, T-004, T-005.
- **Aceite:** RF-001, RF-003, RF-006–RF-013; RN-006, RN-008. Barra, voltar, estados e Toasts funcionam em ambas as telas; recomposição, volta e rotação não repetem evento.
- **Testes locais:** Compose/Robolectric para abrir lista, voltar por ícone e sistema, título por rota, preservação de entrada/endereço, Toast inválido/inexistente/sucesso/falha uma vez. Cobrir readError da lista e restauração. Integração completa em T-008.
- **Complexidade:** alta.
- **Aprendizado:** `NavHost`, `Scaffold`, `collectAsStateWithLifecycle` e efeito para Toast; demonstrar a diferença entre renderizar estado e executar efeito.

### Fase D — Migração da cobertura e remoção do legado

#### T-007 — Migrar os testes da UI antiga para Compose local

- **O que:** reescrever `ConsultaCepUiTest.kt`, que hoje procura IDs de `EditText`, `Button`, `RecyclerView`, `MaterialCardView` e `NavHostFragment`, para interagir por semântica Compose e observar Toast via Robolectric. Usar `createAndroidComposeRule<MainActivity>` ou host equivalente validado em T-001 sem chamar `setContent` duas vezes na Activity. Usar tags apenas onde textos/semântica não identificarem univocamente elementos. Preservar os testes de domínio, repositório e Room existentes.
- **Onde:** `app/src/test/java/com/example/cepapplication/ConsultaCepUiTest.kt`, novos testes de componentes em `app/src/test/java/com/example/cepapplication/ui/compose/`, `app/build.gradle.kts` para dependências do host local.
- **Dependências:** T-006.
- **Aceite:** nenhum teste de UI exige IDs dos layouts XML; suíte local cobre máscara, estados, mensagens, sete campos, navegação e feedback sem aparelho/ADB. RF-001–RF-014 e RN-006.
- **Testes planejados:** esta tarefa é a própria migração; rodar os testes locais focados e confirmar que testes de ViewModel/Room não regrediram. Não adicionar testes instrumentados para contornar limitação do host.
- **Complexidade:** alta, especialmente na configuração Robolectric/Compose.

#### T-008 — Verificar os fluxos completos locais e restauração

- **O que:** ampliar os testes de fluxo com `MainActivity` Compose, Room real isolado e remoto controlado. Exercitar A/B/A, cache local sem rede, recência, HTTP 500, CEP inexistente, IOException, falha de escrita/leitura, lista reativa, navegação, rotação e novo ViewModel com bundle recuperável. Verificar que as transações/endereços existentes permanecem. Incluir teste de altura reduzida/teclado por configuração local na medida suportada pelo Robolectric, com inspeção de semântica e capacidade de rolagem; não afirmar fidelidade de pixels ou sistema real.
- **Onde:** `ConsultaCepFlowTest.kt`, `ConsultaCepUiTest.kt`, `ui/CepViewModelTest.kt` e fixtures existentes quando necessário.
- **Dependências:** T-007.
- **Aceite:** os dez critérios da seção 12 do FRD estão cobertos por testes locais ou por revisão visual documentada para contraste/insets. Recriação da Activity preserva consulta ativa; morte simulada do processo recupera entrada parcial e endereço persistido sem Toast antigo nem nova consulta. RF-001–RF-014, RN-001–RN-008.
- **Testes:** integração local com Room/remote doubles e UI Compose; unitários de handle/feedback; executar `testDebugUnitTest`. Não usar ViaCEP pública, aparelho ou ADB.
- **Complexidade:** alta.

#### T-009 — Remover a interface legada e dependências sem uso

- **O que:** após a suíte Compose passar, remover Fragments, Adapter, layouts `activity_main.xml`, `fragment_search.xml`, `fragment_saved_addresses.xml`, `item_saved_address.xml` e `navigation/main_navigation.xml`. Desabilitar ViewBinding; retirar dependências de Navigation Fragment/UI, RecyclerView e ConstraintLayout se nenhum outro código as usar. Manter AppCompat/Material Views somente se ainda sustentarem o tema base; caso contrário, limpar gradualmente sem mudar a identidade visual. Atualizar referências no manifest/resources/testes para não apontar a IDs antigos. Não apagar banco, esquema ou migrações.
- **Onde:** arquivos legados acima, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `MainActivity.kt`, recursos e testes.
- **Dependências:** T-008.
- **Aceite:** nenhum caminho ativo infla XML ou usa Fragments/RecyclerView; `assembleDebug` e suíte local passam, dados persistidos continuam disponíveis. RF-001, RF-008; RN-007.
- **Testes:** `testDebugUnitTest`, `assembleDebug`, busca estática de referências a classes/IDs removidos. Migrações Room existentes devem continuar passando.
- **Complexidade:** média.

### Fase E — Entrega e documentação

#### T-010 — Revisão visual e documental orientada pelo FRD

- **O que:** conferir as duas telas em claro/escuro, texto legível, rolagem com teclado e altura reduzida, títulos e espaçamentos contra os layouts originais. Registrar limites dos testes Robolectric. Atualizar README para a nova UI sem alterar relatórios históricos de validação. Registrar o que foi implementado e efetivamente verificado em documento de implementação/validação desta pasta. Preparar um exemplo de descrição de PR por STAR com **Situação**, **Tarefa**, **Ação**, **Resultado** baseado nos resultados reais, sem alegar teste não executado.
- **Onde:** `README.md`, `docs/reescrita-telas-compose/` e, apenas para correções visuais encontradas, os composables/tema.
- **Dependências:** T-009.
- **Aceite:** documentação ativa corresponde ao código; não resta alegação de XML/Fragments como UI ativa; evidências e limitações são datadas. Critério visual do FRD e objetivo de aprendizagem.
- **Testes:** revisão documental, `git diff --check`; se houver ajuste de código, repetir os testes afetados e `assembleDebug`.
- **Complexidade:** média.
- **Aprendizado:** construir o STAR com evidência: situação inicial, tarefa aprovada, ações realmente executadas e resultados medidos.

#### T-011 — Validação final e matriz de aceite

- **O que:** executar pelo Wrapper `.\gradlew.bat testDebugUnitTest` e `.\gradlew.bat assembleDebug` no PowerShell, conferir migração/Room e auditoria estática das referências legadas. Preencher uma matriz de aceite com evidência por RF/RN/edge case, apontar limitações locais e pendências reais. Não classificar falha de ambiente como teste aprovado; não transferir resultados históricos para esta execução.
- **Onde:** `docs/reescrita-telas-compose/validacao-reescrita-telas-compose.md` (novo), correções pontuais em arquivos afetados se os comandos falharem.
- **Dependências:** T-010.
- **Aceite:** resultado de cada comando documentado, todos os critérios funcionais rastreados e nenhuma regressão conhecida deixada sem registro.
- **Testes:** suíte local completa e build debug; sem dispositivo/emulador.
- **Complexidade:** média.

## 8. Rastreabilidade e autovalidação do plano

### 8.1 Requisitos e regras

| ID | Tarefas que implementam/verificam |
| --- | --- |
| RF-001 | T-003, T-004, T-006, T-008, T-010 |
| RF-002 | T-004, T-007, T-008 |
| RF-003 | T-004, T-006, T-007, T-008 |
| RF-004 | T-006, T-008, T-009 |
| RF-005 | T-004, T-007, T-008 |
| RF-006 | T-003, T-004, T-006, T-008 |
| RF-007 | T-004, T-006, T-008 |
| RF-008 | T-003, T-005, T-006, T-008, T-010 |
| RF-009 | T-005, T-007, T-008 |
| RF-010 | T-002, T-006, T-008 |
| RF-011 | T-002, T-006, T-008 |
| RF-012 | T-002, T-006, T-008 |
| RF-013 | T-005, T-006, T-008 |
| RF-014 | T-003, T-005, T-007, T-008 |
| RN-001 | T-004, T-008 |
| RN-002 | T-008, T-009 |
| RN-003 | T-008, T-009 |
| RN-004 | T-005, T-006, T-008 |
| RN-005 | T-002, T-004, T-008 |
| RN-006 | T-006, T-007, T-008 |
| RN-007 | T-005, T-008, T-009 |
| RN-008 | T-002, T-004, T-006, T-008 |

### 8.2 Casos limite do FRD

| Caso | Tratamento e teste |
| --- | --- |
| CEP vazio/incompleto | T-004/T-006: Toast inválido, nenhum acesso para a consulta; T-007/T-008 verificam. |
| Colagem com letras/excesso | T-004 usa formatador existente antes de enviar ao estado; T-007 cobre truncamento e zeros. |
| CEP novo inexistente | T-006 mapeia `CepNotFoundException` a Toast; T-008 verifica banco/recência intactos. |
| Rede/timeout | T-006 mapeia `IOException`; T-008 verifica resultado e entrada preservados. |
| HTTP 500/outra falha HTTP | T-006 usa mensagem genérica; T-008 testa HTTP 500 com CEP novo. |
| Falha de leitura/gravação local na consulta | T-006 usa mensagem genérica; T-008 verifica ausência de falso sucesso e histórico preservado. |
| Falha de leitura da lista | T-005 preserva dados e não mostra vazio falso; T-006 emite Toast; T-008 cobre nova abertura. |
| Falha na restauração do último endereço | T-006 emite Toast e mantém pesquisa disponível; T-008 testa. |
| CEP local antigo/offline | T-008 garante retorno local, Toast de sucesso e recência sem rede. |
| Campos vazios | T-003 exibe “Não informado”; T-007/T-008 cobrem as sete linhas. |
| Restauração/retorno sem consulta | T-002/T-006 não iniciam `search`; T-008 verifica recência/Toast. |
| Recriação após Toast | T-006 consome ID; T-008 testa rotação/retorno e ausência de repetição. |

### 8.3 Verificação de completude

- **Dados/estados:** seção 2 cobre CEP, sete campos de endereço, recência, `Idle`/`Loading`/`Success`/`Error`, lista não carregada/vazia/populada/falha e feedback transitório. A seção 5 relaciona cada evento às transições.
- **Dependências:** T-001 → T-003/T-002 → T-004/T-005 → T-006 → T-007 → T-008 → T-009 → T-010 → T-011. Não há ciclo. T-002 pode avançar em paralelo com T-003 após T-001, mas cada tarefa só é concluída com seu aceite.
- **Testes:** cada mudança de comportamento tem teste unitário ou de integração local. Aparência e insets exigem revisão visual complementar; Robolectric não prova fidelidade de pixels em hardware.
- **Banco/integrações:** nenhuma migração ou seed; manter `AppContainer`, Room, Retrofit e testes de migração existentes. Não há CI configurado a alterar neste escopo.
- **Lacuna técnica controlada:** a versão exata do plugin Compose Compiler depende da resolução Kotlin efetiva do AGP/KSP; T-001 a verifica antes de fixar. Essa verificação não autoriza atualização geral de ferramentas.

## 9. Riscos e pontos de atenção

1. **Compatibilidade do compilador:** Compose Compiler e Kotlin devem ser compatíveis, enquanto AGP 9 fornece Kotlin embutido. Fixar versão sem verificar a resolução pode quebrar o build. T-001 trata isso antes de migrar telas.
2. **Restauração do processo:** `SavedStateHandle` atende ao caso em que Android preserva o estado da tarefa; ele não transforma uma nova sessão em retomada de digitação. Não confundir `recreate()` com simulação de processo morto nos testes.
3. **Feedback duplicado:** Compose pode recompor muitas vezes. Toast deve ser acionado pelo ID pendente em efeito com ciclo de vida e consumido, jamais pela presença de `Error` na renderização.
4. **Teste local de UI:** as APIs de teste Compose são frequentemente documentadas para testes instrumentados, mas o projeto exige JVM local. Validar o host Robolectric na T-001 e manter os testes em `src/test`; a documentação do Android considera Robolectric adequado para comportamento de UI Compose, com fidelidade limitada para pixels/insets. [Estratégias Robolectric](https://developer.android.com/training/testing/local-tests/robolectric).
5. **Aparência em tema escuro:** o XML atual força texto preto em alguns cartões. A migração deve manter a identidade de cor com `onSurface` legível, sem copiar combinações ilegíveis.
6. **Migração de testes:** os testes atuais dependem de IDs View, Adapter e grafo Fragment. Remover XML antes de portar a suíte destrói cobertura. A remoção vem após T-008.
7. **Efeito da navegação durante carregamento:** o botão de lista não é bloqueado pela regra existente. Um Toast emitido enquanto a lista estiver visível deve ser consumido pela raiz, e o resultado permanecerá no ViewModel para aparecer quando o usuário voltar, sem segunda consulta.

## 10. Fora do escopo técnico

- Alterar banco, migrações, TTL, política local primeiro, provedor ViaCEP ou APIs de domínio.
- Criar telas extras, buscar por nome/endereço, permitir editar/excluir/atualizar manualmente ou marcar favoritos.
- Acrescentar proteções para consultas simultâneas além do bloqueio de campo/botão já aprovado.
- Adicionar testes instrumentados, ADB, emulador ou dispositivo à validação automatizada.
- Persistir dados de entrada em DataStore/Room para uma nova sessão; o contrato é apenas restauração de tarefa com estado recuperável.
- Produzir material didático separado: cada tarefa indica o conceito a explicar durante a implementação, e T-010 inclui o exemplo STAR baseado em evidência real.

## 11. Condição para iniciar a implementação

Este arquivo é apenas o plano. Após revisão e aprovação pelo usuário, executar as tarefas de forma progressiva, informando para cada uma o que mudou, o conceito de Compose envolvido, os testes realmente executados e qualquer limite observado. Nenhum resultado futuro está presumido neste Blueprint.
