# Revisão visual e documental — reescrita das telas em Compose

Data: 17/09/2026
Tarefa: T-010 — Revisão visual e documental orientada pelo FRD
Escopo: revisão estática da UI Compose e atualização da documentação ativa.
Estado: concluída somente no escopo documental; T-011 não foi executada.

## Referências e estado observado

Foram consultados:

- [Blueprint da reescrita](blueprint-reescrita-telas-compose.md), especialmente T-010, a seção visual e os limites de validação;
- [FRD da reescrita](frd-reescrita-telas-compose.md), especialmente RF-001, RF-005–RF-014, seção 8 e critério de aceite 10;
- `AGENTS.md`, `README.md` e o código atual de `MainActivity`, `CepApplication` e `ui/compose`;
- os testes locais Compose em `app/src/test/java/com/example/cepapplication/ui/compose/`.

O worktree já apresentava a migração Compose, a remoção dos arquivos legados e alterações de testes/configuração anteriores a esta tarefa. Essas alterações foram preservadas. Nesta T-010 não foram alterados Kotlin, testes, banco, dependências, configuração funcional ou os relatórios de `docs/consulta-cep/`.

A dependência indicada no Blueprint é T-009. O estado do código é compatível com essa etapa porque a Activity ativa usa Compose e não referencia os Fragments, Adapter ou layouts removidos. O aceite completo de T-009 não foi revalidado por build nesta tarefa.

## Revisão visual orientada pelo FRD

### Estrutura, telas e navegação

`MainActivity` mantém uma única Activity e chama `setContent`. `CepTheme` envolve `CepApp`, que coleta os `StateFlow`s do `CepViewModel`, usa `Scaffold` com `TopAppBar` e hospeda um `NavHost` com as rotas `search` e `saved_addresses`.

- A tela `SearchScreen` apresenta o título “CEP”, campo mascarado, botão “Consultar e salvar”, progresso durante `Loading`, cartão do último endereço e botão “CEPs armazenados”.
- A tela `SavedAddressesScreen` apresenta “CEPs consultados” apenas quando há itens, renderiza cartões na ordem recebida e mostra “Nenhum CEP armazenado.” somente depois de uma leitura vazia concluída.
- A barra superior usa “Busca CEP” na pesquisa e “CEPs armazenados” na lista. A lista tem ação de voltar; a navegação não inicia consulta nem altera recência.
- `AddressCard` é compartilhado pelas duas telas e usa a mesma formatação dos sete campos, incluindo “Não informado”.

Esse desenho atende à organização visual prevista no FRD sem criar novas telas, campos ou ações de edição/exclusão.

### Claro, escuro e legibilidade

`CepTheme` define esquemas explícitos claro e escuro a partir das cores existentes. Os textos usam `onSurface` ou `onBackground` nos cartões e no estado vazio; títulos usam a cor primária. No modo escuro, o cartão não copia o texto preto do layout legado: usa `MaterialTheme.colorScheme.onSurface`, preservando a legibilidade prevista pelo Blueprint.

Na inspeção do código, foram observados:

- títulos de seção em 20sp e peso forte;
- título opcional do cartão em 18sp e negrito;
- linhas de endereço em 14sp com `lineHeight` de 18sp;
- botão, campo, progresso e ação de lista renderizados por componentes Material 3;
- descrições de acessibilidade para a ação de voltar e semântica de progresso.

`AddressCardTest` cobre a renderização com `darkTheme = true` e `darkTheme = false`, verifica as sete linhas e verifica os valores “Não informado”. Essa é evidência de composição/semântica local; não é medição de contraste por pixel nem certificação de todos os dispositivos, fontes ou escalas do sistema.

### Espaçamentos e relação com a referência anterior

Os valores observados preservam a escala existente de 8, 16, 24 e 32dp:

| Área | Implementação Compose observada |
| --- | --- |
| Pesquisa | padding vertical de 32dp, intervalos de 16dp e margens horizontais de 24dp |
| Campo e ações | `OutlinedTextField` e linha de consulta com 24dp nas laterais |
| Cartão | 24dp nas laterais, 16dp interno, 16dp entre cartões/abaixo do conteúdo |
| Lista | título com 24dp nas laterais, 32dp no topo e 16dp no final da `LazyColumn` |
| Tipografia | 14sp/18sp para conteúdo, 18sp para título de cartão e 20sp para títulos de seção |

A revisão confirma coerência estrutural com as dimensões e textos existentes. Ela não afirma fidelidade de pixels: a comparação foi feita por código, recursos e testes de semântica, sem captura de tela em dispositivo real.

### Altura reduzida, rolagem e teclado

`SearchScreen` combina `fillMaxSize`, `verticalScroll(rememberScrollState())` e `imePadding()`. O conteúdo vertical — inclusive o botão “CEPs armazenados” — pode ser alcançado quando a altura disponível é reduzida. O Manifesto mantém `android:windowSoftInputMode="adjustResize"`, complementando o comportamento esperado quando o teclado reduz a área útil.

O teste local `SearchScreenTest.contentUsesScrollableColumnForReducedHeight` monta a tela em 320×240dp, verifica a ação de rolagem e alcança o botão de lista com `performScrollTo`. Isso sustenta a revisão para altura reduzida.

Não há teste local que abra um IME real, meça insets de uma janela Android ou valide a posição final em hardware. Portanto, o registro é de suporte implementado por `imePadding`/`adjustResize`, não de validação visual do teclado em aparelho ou emulador.

### Estado e efeitos visuais

O corpo dos composables apenas descreve a UI a partir do estado. `rememberScrollState` conserva estado visual de rolagem, enquanto entrada, endereço e lista continuam vindo do ViewModel. Toasts, restauração e observação da lista ocorrem em `LaunchedEffect`, fora da renderização direta; o feedback é consumido por identificador. Assim, a revisão não encontrou Toast ou consulta disparados apenas por recomposição.

## Evidências desta tarefa

### Verificações executadas em 17/09/2026

| Comando/verificação | Resultado |
| --- | --- |
| Leitura do Blueprint, FRD, `AGENTS.md`, README, Activity, aplicação, Compose e testes locais | Concluída; caminhos e componentes usados nesta revisão foram conferidos. |
| Busca de referências legadas no README e na UI ativa | README foi corrigido; a UI ativa não referencia Fragments, ViewBinding, `NavHostFragment`, RecyclerView ou layouts XML. As menções históricas/planejadas permanecem somente nos documentos de referência. |
| Revisão dos testes Compose existentes | Confirmados testes de temas claro/escuro, sete linhas, estados da lista, Toasts e rolagem em altura reduzida; eles não foram reexecutados nesta tarefa. |
| `git diff --check` | Executado após as alterações documentais; saída 0, sem erro de whitespace. O Git emitiu apenas avisos de conversão LF/CRLF para arquivos já modificados no worktree. |

Não foram executados `testDebugUnitTest` nem `assembleDebug`, porque a T-010 alterou apenas documentação. Não foi executado T-011 e não foi criada a matriz final de aceite.

### Limitação do Gradle e do ambiente

Não se deve transformar histórico em validação atual da UI Compose. O registro histórico disponível em `docs/consulta-cep/validacao-consulta-cep.md` documenta tentativas bloqueadas antes da compilação por `Acesso negado` ao Android SDK. Esta T-010 também registra a limitação operacional de ambiente/loopback do Wrapper informada para a validação, sem tratá-la como diagnóstico de falha do código. Esses impedimentos não foram repetidos nesta tarefa.

O mesmo histórico registra uma execução posterior, em 07/09/2026, com 80 testes e `assembleDebug` concluído para o estado então validado da consulta de CEP. Esse resultado permanece histórico e não comprova a compilação atual da migração Compose, pois T-010 não executou Gradle.

## O que permanece não validado

- compilação e suíte de testes do estado atual da UI Compose nesta tarefa;
- comportamento em aparelho/emulador, fidelidade de pixels, insets reais, IME real, barras do sistema, fontes e escalas de acessibilidade;
- disponibilidade da ViaCEP pública e transporte HTTP real;
- encerramento real de processo pelo Android, além das simulações locais existentes;
- matriz final de requisitos/aceite, reservada à T-011.

Não foram alterados relatórios históricos em `docs/consulta-cep/`.

## Exemplo de descrição de PR pelo método STAR

### Situação

A interface ativa do aplicativo já havia sido migrada para Compose, mas o README ainda descrevia ViewBinding, Fragments e layouts XML como se fossem a UI em uso. A revisão visual precisava registrar, com data, o suporte implementado para temas, rolagem e teclado e separar evidência local de limitações de ambiente.

### Tarefa

Executar a T-010: revisar visualmente as duas telas conforme o FRD, atualizar a documentação ativa e preparar um resumo de PR baseado apenas no que foi observado e verificado.

### Ação

Foram conferidos `MainActivity`, `CepApp`, tema, cartões, telas Compose, recursos e testes locais. O README foi atualizado para descrever as duas rotas Compose e suas limitações. Este documento foi criado com a revisão de cores, tipografia, espaçamentos, rolagem, `imePadding`, `adjustResize`, Toasts e limites do Robolectric. O `git diff --check` foi executado. Não foram modificados código, testes, dependências, banco ou relatórios históricos, e não foram executados os comandos Gradle nesta tarefa.

### Resultado

A documentação ativa agora corresponde à UI Compose observada, sem alegar Fragments/ViewBinding como UI ativa. A revisão confirmou suporte de código para claro/escuro e rolagem em altura reduzida, com testes locais existentes para esses pontos. A compilação atual, o IME em hardware e a matriz final de aceite permanecem explicitamente pendentes; T-011 continua separada.

## Conceitos de Compose registrados

- `@Composable` descreve a UI a partir de estado, permitindo recomposição sem executar consulta ou Toast no corpo da tela.
- `MaterialTheme` e `colorScheme` centralizam as cores dos modos claro e escuro.
- `Scaffold` organiza a barra superior e o conteúdo; `NavHost` troca as duas rotas mantendo o ViewModel compartilhado.
- `rememberScrollState`, `verticalScroll` e `imePadding` tratam rolagem e espaço ocupado pelo teclado; `LazyColumn` renderiza a lista de endereços de forma apropriada para conteúdo potencialmente longo.
- `LaunchedEffect` hospeda efeitos pontuais, como Toast e observação, fora da descrição declarativa da tela.
