# Relatório de revisão — Consulta de CEP

Data: 07/09/2026. Referência: HEAD `9f57911`, incluindo as alterações locais existentes no início da revisão. Revisão solicitada contra o FRD e o Blueprint de consulta de CEP, usando a skill `feature-review`. A referência a carrinho de compras não se aplica a este escopo.

## Situação após as correções aprovadas

Em 07/09/2026, o usuário aprovou R-01–R-07. As correções foram aplicadas e a execução final concluiu **80 testes sem falhas e assembleDebug**. A matriz histórica abaixo descreve a revisão anterior às correções; a matriz atual e o inventário executado estão em [test-report-consulta-cep.md](test-report-consulta-cep.md). Localizações dos findings referem-se à árvore revisada antes dos ajustes.

| Finding | Situação atual |
| --- | --- |
| R-01 | Corrigido: falha da restauração tratada; cancelamento/invalidação preservados |
| R-02 | Corrigido: espera pelos estados reais e fechamento de composições; seis testes passaram |
| R-03 | Corrigido: observação reiniciável após erro ao reabrir, com feedback consumível |
| R-04 | Cobertura local acrescentada: migrações, índice, reabertura, rollback e cancelamento |
| R-05 | Cobertura de UI/JSON/nova tentativa acrescentada; limitações explícitas no relatório atual |
| R-06 | Corrigido: Toast de falha consumível por id |
| R-07 | Documentação ativa atualizada e registros históricos preservados |

O teste de UI também revelou truncamento da colagem antes da máscara pelo maxLength XML; corrigido dentro de RF-001/RN-002, sem mudar escopo.

## Resumo histórico da revisão inicial

- **7 findings: 2 críticos, 3 altos e 2 médios.** A classificação segue a skill; teste falhando é crítico para o aceite, sem significar necessariamente defeito funcional no aplicativo.
- Conformidade com o Blueprint: **parcial/média**, principalmente por tratamento incompleto de leituras e cobertura de aceite incompleta.
- Conformidade com o FRD: **parcial/média**. O caminho principal local/remoto está implementado; falhas de restauração/observação impedem aprovação integral.
- Cobertura: **parcial**. 59 testes executados, 53 passaram e 6 falharam.
- Segurança: **sem vulnerabilidades concretas identificadas na inspeção estática deste fluxo**. Isso não equivale a auditoria dinâmica ou certificação.
- `assembleDebug` concluiu; a execução conjunta terminou com erro por causa dos testes.
- Nenhuma correção de produção ou teste foi aplicada nesta revisão. O único artefato escrito deliberadamente é este relatório; Gradle produziu seus artefatos de build/teste.

Prevalecem as decisões posteriores do FRD e do AGENTS.md: testes locais, sem ADB/emulador, sem proteções/testes de consultas simultâneas. Referências antigas a testes instrumentados no Blueprint não autorizam reintroduzi-los.

## Findings

### R-01 — [CRÍTICO] Falha de restauração escapa da coroutine

**Eixo:** FRD / Blueprint / Qualidade.

**Localização:** `app/src/main/java/com/example/cepapplication/ui/CepViewModel.kt:114–122`.

**Referências:** RF-009, RF-010; seção 7 do FRD; T-006, EC-009.

`loadLatestAddress()` executa `getSavedAddresses().first()` em `viewModelScope.launch` sem tratar falhas. Se Room falhar ao abrir/ler o banco na entrada da tela, a exceção escapa da coroutine e pode encerrar o app, em vez de apresentar a mensagem genérica e manter a pesquisa disponível. O `catch` da observação iniciada no `init` pertence a outra coleta e não protege esta leitura.

**Correção proposta:** capturar falhas da restauração, propagar `CancellationException` e publicar o erro somente se a restauração ainda for válida. Manter a proteção contra leitura atrasada e não alterar recência. Adicionar teste de falha inicial e de falha atrasada após pesquisa. Evidência estática; não foi injetada falha no app nesta revisão. A semântica de exceções de `launch` está documentada em [Kotlin — tratamento de exceções](https://kotlinlang.org/docs/exception-handling.html).

### R-02 — [CRÍTICO] Seis testes de fluxo falham na execução atual

**Eixo:** Qualidade / Blueprint.

**Localização:** `app/src/test/java/com/example/cepapplication/ConsultaCepFlowTest.kt:55–180`; `testing/IsolatedRoomDatabase.kt:20–28`.

**Referências:** T-009, T-010, T-011; CA-02, CA-04, CA-05, CA-06, CA-07, CA-08.

Os testes de miss, restauração após A/B/A, HTTP, inexistência, dados parciais e conclusão após Loading falharam. As mensagens registram `Loading` onde se esperava `Success`/`Error`, e `Idle` onde se esperava restauração.

O código usa `advanceUntilIdle()` como garantia de conclusão, mas Room foi criado com executores próprios, fora do scheduler controlado pelo teste. A leitura estática e os estados observados indicam falta de sincronização como causa provável; isso ainda precisa ser confirmado ao corrigir e reexecutar. Os ViewModels também não são encerrados explicitamente antes de fechar/remover os bancos das fixtures.

**Correção proposta:** sincronizar os testes com a conclusão real das operações/estados, com limites de tempo, e encerrar os ViewModels/coletores antes do fechamento das fixtures. Não usar sleeps nem remover asserts para obter aprovação. O cenário HTTP deve concluir A antes de iniciar B, preservando a regra de uma consulta por vez. Reexecutar a classe e a suíte completa. Os resultados atuais não demonstram que a consulta de produção fica permanentemente em Loading.

### R-03 — [ALTO] Observação da lista termina após falha e não se recupera na reabertura

**Eixo:** FRD / Blueprint.

**Localização:** `app/src/main/java/com/example/cepapplication/ui/CepViewModel.kt:53–67`; `SavedAddressesFragment.kt:39–54`.

**Referências:** RF-011; T-006 e T-008.

O `catch` registra `readError`, mas encerra a coleta iniciada no `init`. Reabrir a tela apenas volta a coletar o StateFlow que mantém o último valor; não reinicia a leitura do banco. Uma falha transitória pode deixar a lista congelada pelo restante da vida do ViewModel, inclusive depois de novas consultas bem-sucedidas. Além disso, `SavedAddressesFragment` ignora `readError`, deixando a falha inicial sem feedback.

**Correção proposta:** permitir reiniciar a observação ao reabrir a tela após falha, preservando dados anteriores e usando a mensagem genérica existente. Evitar retentativas periódicas e coletores duplicados. Testar emissão A, falha, reabertura e emissão B, sem rede/escritas provocadas pela navegação. O teste atual de preservação após erro valida somente o valor retido, não a recuperação.

### R-04 — [ALTO] Migrações e garantias de persistência perderam cobertura executável

**Eixo:** Qualidade / Blueprint.

**Localização:** `app/src/test/java/com/example/cepapplication/testing/IsolatedRoomDatabase.kt:20–28`; `data/local/RoomCepLocalDataSourceTest.kt:82–113`; `ConsultaCepFlowTest.kt:93–110`.

**Referências:** RF-005, RF-006, RF-010, RF-011; RN-005–RN-007; T-001, T-002, T-010.

Não há testes locais de migração. A fixture cria somente banco novo, sem registrar migrações. O teste local da fonte Room usa DAO falso e substitui os métodos transacionais, portanto não comprova rollback. A/B/A utiliza Room real, mas a chamada de restauração usa o mesmo repositório e banco aberto; não testa reabertura do arquivo persistido. Também faltam testes de conflito do índice único e falha/cancelamento antes do commit com verificação do banco.

**Correção proposta:** portar a cobertura relevante para testes locais Robolectric já disponíveis: fixtures legadas v1/v2/v3 abertas em v4 com migrações reais; dados/ids/índice/ordem preservados; reabertura do mesmo arquivo; conflito de unicidade; rollback de gravação e recência. Usar banco exclusivo dos testes e falhas controladas. A exclusão de testes instrumentados não excluiu essas garantias do aceite. Não foi identificado defeito concreto na SQL de migração pela leitura; o finding é ausência de evidência automatizada importante.

### R-05 — [ALTO] Testes chamados de fluxo não exercitam as telas e vários aceites visuais

**Eixo:** Qualidade / Blueprint.

**Localização:** `app/src/test/java/com/example/cepapplication/ConsultaCepFlowTest.kt:34–238`; `data/ViaCepResponseTest.kt:8–78`.

**Referências:** RF-001, RF-007–RF-012; T-007–T-010; EC-015.

`ConsultaCepFlowTest` instancia o ViewModel diretamente. Não abre Activity/Fragments, não verifica botão/campo desabilitados, progresso, Toast, máscara no EditText, sete campos renderizados, “Não informado”, navegação ou recriação da View. O teste cujo nome menciona nova tentativa não provoca erro nem tenta novamente. O cenário chamado HTTP lança `IllegalStateException("HTTP 500")`; existe cobertura separada com `HttpException` no adaptador/repositório, mas não valida a mensagem da UI. Os testes DTO constroem objetos diretamente e não verificam a desserialização Gson do JSON/`erro` prevista em T-009.

**Correção proposta:** acrescentar testes locais com Robolectric das telas e dos cenários ausentes, preservando Room/remoto controlados onde necessário. Cobrir recriação e feedback único, navegação sem escrita, nova tentativa real, HTTP com tipo correto e Gson com payloads representativos. Não exigir acesso à ViaCEP pública. Ajustar nomes/documentação para distinguir integração sem UI de fluxo completo.

### R-06 — [MÉDIO] Erro em Toast é repetido a cada edição e retomada da tela

**Eixo:** Qualidade / Blueprint.

**Localização:** `app/src/main/java/com/example/cepapplication/SearchFragment.kt:83–90`; `ui/CepViewModel.kt:99–100`.

**Referências:** RF-009; T-005, T-007; seção 4.2 do Blueprint.

Após IOException/HTTP/falha local, o estado permanece Error. Cada edição chama `updateInput`, gera um novo estado e executa `showLookupError` novamente. Retomar a coleta após navegação também repete o Toast sem nova consulta. O mecanismo consumível existe apenas para sucesso.

**Correção proposta:** tornar o Toast de falha um feedback consumível por identificador, preservando o estado Error e os erros de campo renderizáveis. Testar falha seguida de edição, retomada da tela e nova tentativa; cada falha efetiva deve notificar uma vez.

### R-07 — [MÉDIO] Documentação ativa mistura planejamento antigo, conclusão e estratégia retirada

**Eixo:** Blueprint / Qualidade.

**Localização:** `docs/consulta-cep/blueprint-consulta-cep.md`, status e T-001/T-002/T-007–T-011; `README.md`, “Estado da implementação”; `frd-consulta-cep.md`, status.

**Referências:** T-011, T-012; decisão de testes de 07/09/2026.

O Blueprint declara implementação concluída até T-010, mas ainda prevê arquivos `androidTest` e permite cobertura instrumentada complementar em T-011. Isso conflita com a decisão posterior de manter somente testes locais. A evidência atual também mudou: o build foi possível e a suíte apresentou falhas reais, enquanto os resumos ainda citam exclusivamente bloqueio do SDK.

**Correção proposta:** atualizar os caminhos/estratégia e status ativos com o resultado desta revisão, sem marcar tarefas com aceites faltantes como integralmente concluídas. Acrescentar nova seção de validação datada, preservando integralmente o resultado histórico da T-011. Não reescrever a tentativa antiga como sucesso atual.

## Matriz de cobertura

“Parcial” em testes indica cenário incompleto, não necessariamente defeito funcional. Aprovação refere-se apenas aos asserts executados, sem inferir cobertura adicional.

| RF/RN | Implementado? | Testado? | Observação |
| --- | --- | --- | --- |
| RF-001 | Sim, por inspeção | Parcial | Formatter passa; falta EditText/colagem real |
| RF-002 | Sim | Sim | Inválido não chama repositório/dados da consulta |
| RF-003 | Sim | Parcial | Hit sem remoto passa; cenário “antigo” não grava data antiga real |
| RF-004 | Sim, por inspeção | Parcial | Repositório/adaptador passam; fluxo integrado falha |
| RF-005 | Sim, por inspeção | Parcial | Sem testes de migração/reabertura/índice |
| RF-006 | Sim, por inspeção | Parcial | A/B/A existe; faltam rollback e reabertura |
| RF-007 | Sim, por inspeção | Parcial | Estado Loading coberto; widgets não |
| RF-008 | Sim, por inspeção | Parcial | ViewModel passa isolado; fluxo falha; renderização ausente |
| RF-009 | Parcial | Parcial | Restauração sem catch; Toast repetido |
| RF-010 | Parcial | Parcial | Proteção atrasada passa; erro inicial e reabertura faltam |
| RF-011 | Parcial | Parcial | Observação não reinicia após erro |
| RF-012 | Sim, por inspeção | Parcial | DTO passa; sem assert de “Não informado” na tela |
| RN-001 | Sim, por inspeção | Parcial | Normalização passa; índice não exercitado em conflito |
| RN-002 | Sim, por inspeção | Parcial | Formatter passa; integração com filtros do EditText não exercitada |
| RN-003 | Sim | Parcial | Não há TTL; idade real não variada nos testes |
| RN-004 | Sim | Sim | Falha local impede remoto |
| RN-005 | Sim | Parcial | Save antes de retorno; falta falha transacional real |
| RN-006 | Sim, por inspeção | Parcial | Leituras sem escrita cobertas com doubles |
| RN-007 | Sim, por inspeção | Parcial | Recência/índice no código; falta reabertura/conflito |
| RN-008 | Sim, por inspeção | Parcial | ViewModel preserva A; UI não exercitada |
| RN-009 | Sim, por inspeção | Parcial | Complementares vazios aceitos; apresentação não exercitada |
| RN-010 | Sim, por inspeção | Não diretamente | Sem aviso de cache nos recursos/renderização |
| RN-011 | Sim, por inspeção | Parcial | HttpException passa em camadas; cenário integrado falha |

## Cobertura das tarefas

| Tarefa | Avaliação |
| --- | --- |
| T-001 | Schema v4 e migrações presentes; validação legada ausente |
| T-002 | Transações/ordenação presentes; rollback/reabertura sem cobertura |
| T-003 | Local primeiro e remoção de TTL conformes; testes isolados passam |
| T-004 | Distinção de falhas/cancelamento na consulta conforme; restauração em R-01 |
| T-005 | Estado/entrada/resultado anterior conformes; feedback de erro em R-06 |
| T-006 | Proteção contra restauração atrasada presente; recuperação incompleta |
| T-007 | Tela integrada ao estado; cobertura de UI e feedback pendentes |
| T-008 | Ordenação recebida preservada; recuperação da observação pendente |
| T-009 | Room isolado e remoto controlado presentes; sincronização/JSON incompletos |
| T-010 | Integração parcial; seis falhas, sem percurso das telas |
| T-011 | Build debug concluído; suíte reprovada na execução atual |
| T-012 | Sem TTL no README; estratégia/status precisam da atualização R-07 |

## O que está correto

- Retrofit usa HTTPS, GET `ws/{cep}/json/`, Gson e método suspenso. Contrato compatível com a [documentação oficial ViaCEP](https://viacep.com.br/): CEP com oito dígitos, JSON e indicação de inexistência por `erro`.
- Caso de uso valida antes do repositório; domínio não importa Android/Retrofit/Room.
- Repositório retorna localmente sem TTL/fallback e acessa remoto somente após miss; só retorna sucesso remoto após salvamento.
- DAO mantém índice único, ABORT e transações locais; não mantém transação durante a rede. A recência ordinal independe do relógio e o hit não regrava o endereço.
- Migração 3→4 materializa ids e preserva a ordem legada; todas as migrações estão registradas, sem fallback destrutivo.
- Cancelamento é propagado na fonte local/caso de uso; entrada e endereço anterior vivem no ViewModel; restauração é invalidada antes da pesquisa.
- Fragments coletam com ciclo de vida da View; mensagens ficam em recursos. Não foram introduzidas proteções de consultas simultâneas nem funcionalidades fora do escopo.
- Consultas SQL usam parâmetros para valores externos; URL base é fixa; não foram encontrados logs de CEP/endereço nem credenciais neste fluxo. Autenticação/autorização não se aplicam ao escopo aprovado.

## Verificações efetivamente executadas

Ambiente observado: Windows 11 amd64; Gradle Wrapper 9.5.0; Launcher JVM JetBrains 25.0.2; critério do Daemon JVM Java 21 informado por `gradlew.bat --version`. AGP 9.3.1, Room 2.8.4, Retrofit 3.0.0 e SDKs conforme configuração local; nenhuma versão foi alterada.

| Comando | Resultado atual |
| --- | --- |
| `.\gradlew.bat testDebugUnitTest` e `.\gradlew.bat assembleDebug`, na permissão padrão | Ambos terminaram com “Acesso negado”, saída 1 |
| `.\gradlew.bat testDebugUnitTest assembleDebug`, com permissão ampliada autorizada pelo ambiente | `assembleDebug` concluiu; 59 testes, 6 falhas; execução conjunta saída 1, 52 segundos |
| `.\gradlew.bat --version`, com permissão ampliada | Saída 0; versões acima |
| `git diff --check` | Saída 0; avisos LF/CRLF, sem erros de whitespace |

| Classe | Executados | Falhas |
| --- | ---: | ---: |
| ConsultaCepFlowTest | 11 | 6 |
| AddressDtoTest | 4 | 0 |
| RoomCepLocalDataSourceTest | 7 | 0 |
| RetrofitCepRemoteDataSourceTest | 4 | 0 |
| CepRepositoryImplTest | 10 | 0 |
| GetAddressByCepUseCaseTest | 5 | 0 |
| TestInfrastructureTest | 2 | 0 |
| CepViewModelTest | 10 | 0 |
| ZipCodeFormatterTest | 6 | 0 |

Evidências geradas: `app/build/reports/tests/testDebugUnitTest/index.html` e XMLs em `app/build/test-results/testDebugUnitTest/`. Os resultados acima são desta execução; não substituem os registros históricos. Não houve aparelho, emulador, ADB ou teste instrumentado. A documentação pública foi consultada, mas não foi realizado teste de transporte HTTP real nem teste de disponibilidade da API.

## Plano de correções para aprovação

1. **R-01/R-03:** tratar falha da restauração e recuperar observação ao reabrir; testes de erro, cancelamento e invalidação atrasada.
2. **R-02:** tornar os testes com Room determinísticos e encerrar suas composições; confirmar a causa das seis falhas sem relaxar critérios.
3. **R-04/R-05:** completar evidências locais de migração, transações, reabertura, telas, JSON e nova tentativa.
4. **R-06:** aplicar feedback consumível também aos Toasts de falha e verificar ausência de repetição.
5. **R-07:** atualizar documentação ativa e registrar nova validação. Executar testes afetados, suíte completa, build se houver mudança de produção e `git diff --check`.

Os itens críticos e altos são recomendados para correção. Os itens médios R-06/R-07 ficam explicitados para decisão de prioridade. Este plano foi posteriormente aprovado e executado; ver a situação atual no início do relatório.

## Revisão complementar — cards dos CEPs armazenados (17/09/2026)

Escopo desta revisão: migração da apresentação de `SavedAddressesFragment` para `RecyclerView`, com um `MaterialCardView` por endereço. Os resultados históricos acima não validam esta mudança posterior.

### Resumo e evidência

- **Erros de compilação confirmados:** nenhum. `testDebugUnitTest --stacktrace` parou antes da compilação, na conexão do cliente Gradle com o daemon: `Unable to establish loopback connection`, causado por `java.net.SocketException: Invalid argument: connect` em `java.nio.channels.Selector.open`.
- **Findings de código:** dois pontos de qualidade a corrigir/verificar, ambos de severidade média. Conformidade observável com RF-011 e RF-012: parcial até a execução dos testes e do build.
- **Segurança:** nenhuma nova superfície de entrada ou persistência foi adicionada pela mudança de apresentação.
- `git diff --check` retornou saída 0. O APK e o relatório de testes existentes são anteriores a esta mudança e não servem como validação atual.

### Findings

#### RV-01 — [MÉDIO] A lista vazia permanece sobre a mensagem de vazio

**Eixo:** FRD / Qualidade. **Localização:** `app/src/main/res/layout/fragment_saved_addresses.xml`, `app/src/main/java/com/example/cepapplication/SavedAddressesFragment.kt`. **Referência:** RF-011; T-008.

O `RecyclerView` ocupa todo o `FrameLayout` e continua visível quando `state.hasLoaded && state.addresses.isEmpty()`. A mensagem de vazio é um irmão desenhado antes dele. Como a lista não tem fundo opaco, o texto pode aparecer, mas a hierarquia e a área de toque/acessibilidade ainda contêm uma lista vazia cobrindo a mensagem. Alternar a visibilidade do `RecyclerView` junto com a do título e da mensagem; verificar também o estado inicial ainda não carregado.

#### RV-02 — [MÉDIO] Teste dos cards depende de um ViewHolder já materializado

**Eixo:** Qualidade / Cobertura. **Localização:** `app/src/test/java/com/example/cepapplication/ConsultaCepUiTest.kt`, teste `navegacaoListaReativaRecriacaoDaViewSemEscritaOuFeedbackRepetido`. **Referência:** RF-011, RF-012; T-008.

O teste espera `findViewHolderForAdapterPosition(0)`, que pode permanecer `null` se o Robolectric não executar layout/medição da lista no cenário. Também valida o conteúdo somente do primeiro card, embora espere dois itens. Fazer o layout explicitamente quando necessário e verificar conteúdo e card de A e B após a atualização assíncrona do `ListAdapter`, mantendo a afirmação sobre a ordem e a ausência de escrita ao abrir a tela.

### Matriz de cobertura deste escopo

| Regra | Implementação observada | Evidência de teste atual |
| --- | --- | --- |
| RF-011 / RN-006 / RN-007 — lista reativa, ordenada, sem escrita ao abrir | `ListAdapter` recebe a ordem do estado e usa CEP no `DiffUtil` | Teste atualizado, porém não executado após a mudança |
| RF-011 — vazio confirmado | Mensagem depende de `hasLoaded` e lista vazia | Teste existe; sobreposição da lista requer ajuste |
| RF-012 — sete campos e “Não informado” | Item reutiliza `formatAddress` | Teste verifica somente o primeiro card; execução pendente |

### O que está correto

`SavedAddressesAdapter` recebe os registros já ordenados, usa o CEP como identidade e compara os sete campos para atualização. Cada item tem seu próprio `MaterialCardView`. O Fragment mantém coleta vinculada ao ciclo de vida e preserva o tratamento de erro. Não há mudança no Room nem na recência ao abrir a lista.

### Plano de correção

1. Alternar a visibilidade do `RecyclerView` para mostrar apenas lista ou mensagem de vazio, conforme `SavedAddressesState`.
2. Tornar o teste de UI determinístico quanto ao layout e conferir os dois cards, incluindo campos vazios formatados.
3. Resolver a falha de loopback do ambiente Gradle sem alterar as regras do aplicativo; executar `testDebugUnitTest` e `assembleDebug`. Só então classificar eventuais diagnósticos do compilador ou falhas de teste como erros confirmados do código.
