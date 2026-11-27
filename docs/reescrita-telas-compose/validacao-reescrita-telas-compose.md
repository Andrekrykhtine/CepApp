# Validação final — reescrita das telas em Jetpack Compose

Data: 17/09/2026
Tarefa: T-011 — Validação final e matriz de aceite
Escopo: validação local, auditoria estática e rastreabilidade da migração Compose.
Status: concluída quanto às verificações possíveis; suíte Gradle e build bloqueados pelo ambiente.

## 1. Escopo e referências

Esta validação executa exclusivamente a T-011 do [Blueprint](blueprint-reescrita-telas-compose.md). A dependência indicada é a T-010, cuja revisão visual/documental foi consultada em [revisao-visual-documental-reescrita-telas-compose.md](revisao-visual-documental-reescrita-telas-compose.md). O comportamento funcional foi conferido contra o [FRD da reescrita](frd-reescrita-telas-compose.md), e a auditoria de Room/consulta também considerou `docs/consulta-cep/frd-consulta-cep.md`.

O worktree já continha alterações da migração Compose, da remoção dos arquivos de UI legados e da T-010 antes desta tarefa. Essas alterações foram preservadas. Esta T-011 criou somente este documento; não alterou produção, testes, configuração funcional nem relatórios de `docs/consulta-cep/`.

## 2. Comandos executados

Os comandos foram executados no PowerShell, a partir da raiz do checkout, usando o Gradle Wrapper. O bloqueio ocorreu antes de qualquer saída de Gradle, compilação ou execução de teste.

| Comando | Resultado real | Status da evidência |
| --- | --- | --- |
| `.\gradlew.bat testDebugUnitTest` | Código de saída `1`; saída: `Acesso negado.` | **bloqueado**; não comprova aprovação nem reprovação de testes |
| `.\gradlew.bat assembleDebug` | Código de saída `1`; saída: `Acesso negado.` | **bloqueado**; não comprova aprovação nem reprovação do build |
| `.\gradlew.bat --no-daemon testDebugUnitTest` | Código de saída `1`; saída: `Acesso negado.` | **bloqueado**; a alternativa não contornou o ambiente |
| `.\gradlew.bat --no-daemon assembleDebug` | Código de saída `1`; saída: `Acesso negado.` | **bloqueado**; a alternativa não contornou o ambiente |

Não houve erro de compilação, falha de teste, erro de SDK ou diagnóstico de loopback observável nessas execuções: o Wrapper foi impedido de prosseguir pela mensagem imediata de acesso negado. Resultados históricos de `docs/consulta-cep/` não foram usados como aprovação desta migração.

## 3. Auditoria estática da UI e da migração

### 3.1 Referências legadas ativas

Foi feita busca case-insensitive por `Fragment`, `Adapter`, `ViewBinding`, `NavHostFragment`, Navigation Fragment/UI, `RecyclerView`, `ConstraintLayout`, `R.layout`, `setContentView` e `findViewById` em `app/src/main`, `app/src/test`, Gradle, catálogo de versões e `README.md`.

Resultado: **nenhuma ocorrência ativa encontrada** nesses caminhos. Também foram conferidos os recursos:

- `app/src/main/res/layout`: diretório inexistente;
- `app/src/main/res/navigation`: diretório inexistente;
- os quatro layouts XML e `main_navigation.xml` removidos aparecem como alterações anteriores no `git status`, não como remoções executadas por esta T-011;
- a Activity ativa usa `setContent` e `CepApp`, e a navegação usa `navigation-compose` com `NavHost`;
- a lista ativa usa `LazyColumn`; não há uso de `RecyclerView` ou `ConstraintLayout` na UI ativa;
- não há dependência ativa de ViewBinding ou Navigation Fragment/UI no Gradle.

As ocorrências encontradas em documentos são referências históricas, de baseline ou de plano: por exemplo, o FRD original de consulta, o Blueprint, a documentação da T-010 e relatórios históricos ainda mencionam XML/Fragments para explicar o que foi substituído. Elas não são referências de execução e não foram alteradas.

### 3.2 Compose, estado e efeitos

A leitura estática confirmou os conceitos previstos no Blueprint:

- `MainActivity` mantém a única Activity e chama `setContent`;
- `CepTheme` envolve `CepApp`, com esquemas claro/escuro;
- `CepApp` usa `Scaffold`, `TopAppBar`, `NavHost` e duas rotas;
- `screenState` e `savedAddressesState` são coletados com `collectAsStateWithLifecycle`;
- `SearchScreen` usa `verticalScroll` e `imePadding`, e `SavedAddressesScreen` usa `LazyColumn`;
- Toasts, restauração e observação da lista são efeitos em `LaunchedEffect`, não ações no corpo recomponível;
- o CEP editável permanece no `CepViewModel` e é sincronizado com `SavedStateHandle`;
- a UI não acessa DAO ou Retrofit diretamente.

Há testes locais Compose/Robolectric no código-fonte para cartão, estados da lista, máscara, bloqueio em `Loading`, rolagem reduzida, Toasts e restauração. Eles não puderam ser executados nesta T-011 por causa do bloqueio do Wrapper.

### 3.3 Room, migrações e persistência

A auditoria somente leitura confirmou:

- `AppContainer` usa `AddressDatabase::create`, não o banco antigo de linha única;
- `AddressDatabase` está na versão 4, com `MIGRATION_1_2`, `MIGRATION_2_3` e `MIGRATION_3_4` registradas em `addMigrations`;
- a entidade ativa `addresses` mantém índice único para `zip_code` e a coluna `last_consultation_order`;
- o DAO ordena por recência e as operações de localizar/registrar e salvar/registrar continuam transacionais;
- `AddressDatabaseMigrationTest` cobre, no código-fonte, bancos v1/v2/v3, conteúdo/id/ordem, versão 4 e unicidade;
- `RoomPersistenceTest`, `RoomCepLocalDataSourceTest` e os testes de fluxo cobrem, no código-fonte, atomicidade, falhas locais, cancelamento, ordem e ausência de duplicidade;
- não houve alteração de schema, migração ou política de persistência nesta T-011;
- existe `app/schemas/com.example.cepapplication.data.CepDatabase/1.json`, associado ao `CepDatabase` v1 antigo, além do `AddressDatabase` ativo com `exportSchema = false`. O `CepDatabase`, `CepDao` e `SavedZipCodeEntity` permanecem no código e em teste isolado, mas não são usados pelo `AppContainer` ativo. Isso foi registrado como compatibilidade/legado de dados, não como parte da UI Compose, e não foi removido.

Esse resultado é uma conferência estática. A migração e a persistência não foram executadas nesta T-011 porque a suíte foi bloqueada antes de iniciar.

## 4. Inventário de evidências locais

O código contém 108 declarações `@Test` distribuídas entre domínio, repositório, Room, ViewModel, fluxo e Compose. Entre as evidências relevantes estão:

- `ConsultaCepFlowTest.kt`: miss remoto, cache local/offline, recência, HTTP/falha, persistência, lista e restauração do ViewModel;
- `ConsultaCepUiTest.kt` e `ui/compose/CepAppTest.kt`: Toasts, erro sem mensagem inline, preservação do resultado, lista e restauração;
- `ui/compose/SearchScreenTest.kt`: máscara por callback, consulta somente por clique, bloqueio/progresso, resultado anterior e altura reduzida;
- `ui/compose/SavedAddressesScreenTest.kt`: não declarar vazio antes da leitura, vazio carregado, ordem, recomposição e erro preservando dados;
- `ui/compose/AddressCardTest.kt` e `AddressFormattingTest.kt`: sete linhas, temas claro/escuro e “Não informado”;
- `AddressDatabaseMigrationTest.kt` e testes locais de Room: migrações, unicidade, recência e falhas transacionais.

Essas são evidências de cobertura existente no código e na documentação. Como os comandos Gradle foram bloqueados, nenhum desses testes é declarado aprovado nesta execução.

## 5. Matriz de aceite — requisitos funcionais

Legenda: **coberto/verificado** = evidência atual suficiente na verificação indicada; **parcialmente coberto** = há código/teste/documento relacionado, mas falta execução ou validação do ambiente; **não executado** = a verificação não foi realizada; **bloqueado** = dependia diretamente de uma execução impedida.

| ID | Evidência rastreada | Status |
| --- | --- | --- |
| RF-001 | `MainActivity.kt`, `ui/compose/CepApp.kt`, revisão T-010 e `CepAppTest` no código-fonte | **parcialmente coberto** — build/teste bloqueados |
| RF-002 | `SearchScreen.kt`, `CepFormatterTest.kt`, `ZipCodeFormatterTest.kt` e `SearchScreenTest` | **parcialmente coberto** — teste não executado |
| RF-003 | `CepViewModel.kt`, caso de uso de validação, `ConsultaCepUiTest` e fluxo local | **parcialmente coberto** — teste não executado |
| RF-004 | `CepRepositoryImpl`, `AddressDao`, `AddressDatabase`, testes de fluxo/Room e migração | **parcialmente coberto** — validação executável bloqueada |
| RF-005 | `SearchScreen.kt` e teste de `Loading` com campo/botão desabilitados e progresso | **parcialmente coberto** — teste não executado |
| RF-006 | `AddressCard.kt`, `CepApp.kt`, recursos de strings e testes de sucesso/Toast | **parcialmente coberto** — teste não executado |
| RF-007 | preservação no `CepViewModel`/`SearchScreen`, Toasts e teste de falha | **parcialmente coberto** — teste não executado |
| RF-008 | `NavHost`, `SavedAddressesScreen.kt`, `LazyColumn`, revisão T-010 e testes da lista | **parcialmente coberto** — teste não executado |
| RF-009 | estado `hasLoaded`/lista vazia em `SavedAddressesScreenTest` | **parcialmente coberto** — teste não executado |
| RF-010 | ViewModel compartilhado, navegação Compose e testes de preservação/fluxo | **parcialmente coberto** — sem teste Gradle atual |
| RF-011 | estado no ViewModel, feedback por ID e evidência de testes locais | **parcialmente coberto** — rotação real não executada |
| RF-012 | `SavedStateHandle`, testes de novo ViewModel e documentação do FRD | **parcialmente coberto** — restauração real do sistema não executada |
| RF-013 | observação local, `readError`, `SavedAddressesScreenTest` e `CepAppTest` | **parcialmente coberto** — teste não executado |
| RF-014 | `AddressFormatting.kt`, `AddressCard.kt`, sete linhas e “Não informado” nos testes | **parcialmente coberto** — teste não executado |

Nenhum RF é marcado como totalmente verificado porque a suíte e o build atuais não puderam ser iniciados.

## 6. Matriz de aceite — regras de negócio

| ID | Evidência rastreada | Status |
| --- | --- | --- |
| RN-001 | formatador, identidade `zip_code`, entidade/índice único e testes de máscara/Room | **parcialmente coberto** — teste não executado |
| RN-002 | repositório local primeiro, `AddressDao.findAndRecordConsultation`, fluxo offline/cache | **parcialmente coberto** — teste não executado |
| RN-003 | `CepRepositoryImpl`, fonte local/remota, salvamento transacional e testes de miss remoto | **parcialmente coberto** — teste não executado |
| RN-004 | `last_consultation_order`, ordenação DAO, navegação/restauração sem busca e testes de recência | **parcialmente coberto** — teste não executado |
| RN-005 | estados de erro no ViewModel, persistência atômica, fluxo HTTP/falhas e testes fonte | **parcialmente coberto** — teste não executado |
| RN-006 | `CepFeedback` por ID, `LaunchedEffect`, consumo após Toast e testes Compose | **parcialmente coberto** — teste não executado |
| RN-007 | banco ativo preservado, migrações v1→v4, índice único e auditoria sem mudança de schema | **parcialmente coberto** — migração não executada |
| RN-008 | bloqueio de campo/botão, invalidação da restauração atrasada e testes de estado | **parcialmente coberto** — teste não executado |

## 7. Matriz de aceite — casos-limite do FRD

| Caso-limite | Evidência rastreada | Status |
| --- | --- | --- |
| CEP vazio ou incompleto | validação no caso de uso, Toast de inválido e testes UI/ViewModel | **parcialmente coberto** — teste bloqueado |
| Colagem com letras ou excesso | `CepFormatter`, `SearchScreenTest` e testes do formatador | **parcialmente coberto** — teste bloqueado |
| CEP novo inexistente | `CepNotFoundException`, Toast e fluxo de ausência/recência | **parcialmente coberto** — teste bloqueado |
| Rede/timeout | mapeamento de `IOException`, preservação de estado e fluxo controlado | **parcialmente coberto** — teste bloqueado |
| HTTP 500/outra falha HTTP | feedback genérico, `ConsultaCepUiTest` e `ConsultaCepFlowTest` | **parcialmente coberto** — teste bloqueado |
| Falha de leitura/gravação local na consulta | `LocalStorageException`, testes de fonte local e atomicidade Room | **parcialmente coberto** — teste bloqueado |
| Falha de leitura da lista | `readError`, preservação de cartões, Toast genérico e testes Compose | **parcialmente coberto** — teste bloqueado |
| Falha na restauração do último endereço | `loadLatestAddress`, feedback genérico e testes de fluxo/UI | **parcialmente coberto** — teste bloqueado |
| CEP local antigo/offline | consulta local primeiro, recência no DAO e fluxo offline controlado | **parcialmente coberto** — teste bloqueado |
| Campos textuais vazios | `AddressFormatting.kt`, `AddressCardTest` e `AddressFormattingTest` | **parcialmente coberto** — teste bloqueado |
| Restauração do último endereço ou retorno da lista sem consulta | `LaunchedEffect(Unit)`, navegação sem `search` e testes de restauração | **parcialmente coberto** — teste bloqueado |
| Recriação após Toast | IDs consumíveis no ViewModel/Compose e testes de Toast único | **parcialmente coberto** — recriação Android real não executada |

## 8. Critérios de aceite consolidados

| Critério do FRD | Resultado desta T-011 |
| --- | --- |
| Pesquisa, lista, barra e navegação em Compose, com claro/escuro | **parcialmente coberto** por auditoria/revisão e testes-fonte; build bloqueado |
| Máscara, limite, zeros e consulta somente por botão | **parcialmente coberto** por código/testes-fonte; suíte bloqueada |
| Toasts de inválido/inexistente sem erro inline | **parcialmente coberto** por código/testes-fonte; suíte bloqueada |
| Loading bloqueia controles; sucesso mostra, limpa e notifica uma vez | **parcialmente coberto** por código/testes-fonte; suíte bloqueada |
| Falhas preservam entrada/endereço/recência | **parcialmente coberto** por código/testes-fonte; suíte bloqueada |
| Cache local sem expiração e recência única | **parcialmente coberto** por auditoria Room e testes-fonte; execução bloqueada |
| Lista por recência, campos e estado vazio | **parcialmente coberto** por código/revisão/testes-fonte; suíte bloqueada |
| Retorno/rotação preservam estado sem nova consulta/Toast | **parcialmente coberto**; rotação e suíte atuais não executadas |
| Restauração recuperável da entrada parcial | **parcialmente coberto** por `SavedStateHandle` e testes-fonte; Android real não executado |
| Legibilidade e acesso com teclado/altura reduzida | **parcialmente coberto** pela revisão T-010 e teste-fonte de rolagem; IME real/pixels não validados |

## 9. Limitações reais e pendências

- O ambiente bloqueou todas as quatro execuções do Wrapper com `Acesso negado.` e código 1. Não é possível afirmar que `testDebugUnitTest` passou ou que `assembleDebug` gerou um APK nesta execução.
- Não foram usados aparelho, emulador, ADB ou `connectedDebugAndroidTest`, conforme as instruções do projeto.
- Os testes locais Robolectric/Compose existem, mas não foram executados devido ao bloqueio anterior à inicialização do Gradle.
- Não houve validação de fidelidade de pixels, insets reais, fontes/escalas de dispositivo ou IME real.
- Não houve rotação real de Activity nem encerramento/restauração real de processo pelo Android; a evidência disponível é estática e de testes-fonte.
- Não foi consultada a ViaCEP pública. O remoto permanece representado por doubles/testes controlados no código-fonte, sem comprovar disponibilidade, rede ou transporte HTTP atual.
- A auditoria confirmou as migrações e a estrutura Room por leitura, mas não executou os testes de migração nem abriu um banco real nesta T-011.
- Nenhuma correção pontual foi aplicada: não foi identificada regressão objetiva que justificasse desvio do escopo, e o bloqueio do ambiente não deve ser mascarado por alteração de código.
- `docs/consulta-cep/` não foi alterado. Seus resultados históricos continuam históricos e não substituem esta validação.

## 10. Verificação final e entrega

O comando obrigatório de higiene foi executado após a criação deste documento:

```powershell
git diff --check
```

O único arquivo criado nesta T-011 é este documento. T-011 é a última tarefa do Blueprint; não há T-012 nem tarefa seguinte inventada. O aceite de compilação e execução permanece bloqueado até que o ambiente permita rodar o Wrapper sem o erro de acesso negado.
