# Relatório de testes — Consulta de CEP

Data: 07/09/2026. Estado: HEAD 9f57911 mais alterações locais anteriores e correções R-01–R-07 aprovadas em conversa. Esta execução não é o resultado histórico da T-011.

## Resumo

- **80 testes locais: 80 passaram, zero falhas, zero ignorados.**
- Unitários de domínio, estado, mapeamento e adaptadores: 52.
- Integração de fluxo/persistência/infraestrutura, incluindo Room real: 23.
- Fluxos de UI com Activity/Fragments, Room e remoto controlado sob Robolectric: 5.
- Testes de endpoint HTTP real: não executados; o aplicativo consome a API, não oferece endpoint próprio.
- Build debug concluído. Nenhum aparelho, emulador, ADB ou teste instrumentado.
- Dados, dependências, SDK e ferramentas de produção preservados; nenhuma atualização de stack.

## Plano aprovado e dados

O mapa de cenários, cobertura anterior e lacunas está em review-consulta-cep.md, com 7 findings. O usuário aprovou as correções R-01–R-07 e o plano de cobertura local. Não foram necessários seeds volumosos nem bibliotecas novas.

Fixtures usam CEPs com zeros iniciais, Praça da Sé/São Paulo, Rua da Assembleia/Rio de Janeiro, campos vazios e endereços legados com ids não consecutivos e datas zero/iguais/distintas. Bancos SQLite exclusivos têm nomes UUID, são fechados e removidos ao fim. O teste de reabertura conserva o arquivo entre instâncias antes da remoção final.

IsolatedRoomDatabase permite fechar ViewModels antes do banco e reabrir o arquivo. Testes de integração aguardam o estado final real com timeout de cinco segundos, sem assumir que advanceUntilIdle conclui executores Room. UI drena o Looper e aguarda a condição com prazo de dez segundos, sem sleeps. Gates de coroutine controlam atrasos remotos.

Os doubles manuais existentes substituem repositório/DAO para falhas isoladas e API/remoto para sucesso, ausência, IOException e HttpException. Room real valida esquema, SQL e transações. Triggers exclusivos de teste abortam insert/update após efeitos parciais; os testes conferem rollback. Cancelamento é disparado após uma escrita dentro da transação e antes de commit.

A composição manual foi preservada: CepApplication permite sobrescrever container em uma Application exclusiva do teste. Nenhuma configuração de teste aparece nas telas do produto. Versões usadas: JUnit 4.13.2, Robolectric 4.16, Room 2.8.4 e Retrofit 3.0.0, conforme o catálogo existente.

## Execuções e resultados

1. A primeira compilação incremental após editar Fragments falhou por referências a formatadores existentes. Reexecução com --rerun-tasks recompilou sem mudar os formatadores e passou. Não foi necessário alterar dependências.
2. Testes afetados de ViewModel, fluxo e DTO passaram após sincronização e correções.
3. As fixtures de migração/persistência portadas exigiram adaptação de getApplication e fechamento explícito do Room; após ajustes, os testes de migração, persistência e UI passaram.
4. O teste adicional de colagem no EditText revelou truncamento pelo maxLength antes da normalização. A remoção do limite bruto, mantendo o limite de oito dígitos da máscara, corrigiu o caso. Falha de salvamento via UI também foi exercitada.
5. Execução final: `.\gradlew.bat testDebugUnitTest assembleDebug`, **saída 0, BUILD SUCCESSFUL em 31 segundos**; 48 tarefas acionáveis, 15 executadas e 33 atualizadas. Permissão ampliada já autorizada pelo ambiente foi necessária para o Wrapper.
6. git diff --check e verificação dos novos arquivos completam a validação documental.

Ambiente observado na revisão: Windows 11 amd64, Wrapper 9.5.0, Launcher JVM JetBrains 25.0.2 e critério de Daemon JVM Java 21. As versões não foram alteradas durante as correções.

## Resultado por classe

| Classe | Testes | Falhas | Escopo |
| --- | ---: | ---: | --- |
| ConsultaCepFlowTest | 11 | 0 | Integração |
| ConsultaCepUiTest | 5 | 0 | UI local |
| AddressDtoTest | 5 | 0 | Unitário |
| AddressDatabaseMigrationTest | 3 | 0 | Integração |
| RoomCepLocalDataSourceTest | 7 | 0 | Unitário |
| RoomPersistenceTest | 7 | 0 | Integração |
| RetrofitCepRemoteDataSourceTest | 4 | 0 | Unitário |
| CepRepositoryImplTest | 10 | 0 | Unitário |
| GetAddressByCepUseCaseTest | 5 | 0 | Unitário |
| TestInfrastructureTest | 2 | 0 | Integração |
| CepViewModelTest | 15 | 0 | Unitário |
| ZipCodeFormatterTest | 6 | 0 | Unitário |

## Cobertura por requisito

| Requisito | Evidência principal atual | Limite |
| --- | --- | --- |
| RF-001 / RN-002 | Formatter e colagem real com letras/excesso/zeros no EditText | Teclado físico não exercitado |
| RF-002 | Caso de uso/fluxo/UI inválido sem busca ou registro | Restauração é leitura independente |
| RF-003 / RN-003 | Hit offline sem API; timestamp zero real e CEP migrado | Não há TTL |
| RF-004 / RN-004 | Miss/save, falha de leitura sem API, adaptador Retrofit substituível | Sem transporte público |
| RF-005 / RN-001 | Índice único, máscara, duplicação rejeitada, migração e reabertura | Fixtures de schema legado |
| RF-006 / RN-006 / RN-007 | A/B/A, recência persistida, observação sem escrita e rollback | Sem consultas simultâneas, por escopo |
| RF-007 | Gate remoto e asserts de controles/progresso na UI | Robolectric |
| RF-008 | Sucesso rápido, limpeza e sete linhas/campos exibidos | Robolectric |
| RF-009 / RN-008 | HTTP/rede/save falhando com A preservado; input e nova tentativa; restauração com erro | Mensagens existentes |
| RF-010 | Restauração local, reabertura real, invalidação atrasada e falha/cancelamento | Encerramento do processo real não simulado |
| RF-011 | Lista vazia, ordem A/B/A, navegação sem escrita; recuperação de Flow no ViewModel | Erro/reabertura do observador testado com Flow controlado |
| RF-012 / RN-009 | DTO parcial/JSON nulo e complemento “Não informado” na UI | Conversão comum ifBlank usada nos seis campos textuais |
| RN-005 | Save antes de sucesso; trigger aborta e preserva histórico; nova tentativa via UI | Banco exclusivo de teste |
| RN-010 | Texto renderizado igual aos sete campos aprovados; hit offline sem indicação de cache | Sem teste de hardware |
| RN-011 | HttpException 500 em adaptador/repositório/UI; hit impede API | Não chama ViaCEP pública |
| EC-013 | Propagação de cancelamento e rollback antes de commit | Commit confirmado não é desfeito |
| CA-11 / EC-014 | Restauração atrasada normal e com falha não sobrescreve pesquisa | Flow controlado |
| EC-015 | Estado persiste em nova coleta; navegação destrói/recria View sem novo Toast | Sem morte de processo |

## Correções e bugs verificados

- R-01: restauração captura falhas locais e preserva cancelamento e invalidação.
- R-02: os seis testes anteriormente falhos passaram com espera real e encerramento das composições; o HTTP usa HttpException.
- R-03: reabertura permite reiniciar leitura após falha, conserva últimos dados e mostra mensagem consumível.
- R-04: cobertura local de migração, índice, ordem, reabertura, rollback e cancelamento foi restaurada.
- R-05: acrescentados JSON/Gson, UI, nova tentativa, navegação, Loading e falha de salvamento.
- R-06: Toast de falha consumido por id não reaparece ao editar/navegar; nova falha gera novo evento.
- R-07: documentação ativa atualizada, com histórico preservado.
- Defeito adicional de R-05/RF-001: maxLength cortava colagem antes de remover letras; corrigido e coberto no EditText real sob Robolectric.

## Arquivos de teste

- ConsultaCepFlowTest.kt: espera de resultado, fechamento de ViewModels, HTTP tipado, timestamp zero e reabertura real.
- ConsultaCepUiTest.kt: Application de teste, Activity/Fragments reais, Room e remoto controlado.
- testing/IsolatedRoomDatabase.kt: isolamento, fechamento e reabertura.
- data/local/AddressDatabaseMigrationTest.kt: fixtures legadas v1/v2/v3, criação nova, ids/conteúdo/ordem/índice e hit offline migrado.
- data/local/RoomPersistenceTest.kt: transações reais, observação, recência, reabertura, unicidade e rollback.
- data/ViaCepResponseTest.kt: desserialização Gson, incluindo erro booleano/textual e campos ausentes/nulos.
- ui/CepViewModelTest.kt: falha de restauração, cancelamento, falha atrasada, recuperação da lista e feedback consumível.
- As demais classes existentes foram preservadas e executadas na suíte final.

Os arquivos estão em app/src/test/java/com/example/cepapplication/. Não foram restaurados arquivos androidTest.

## Limitações e cenários não exercitados

Não houve transporte HTTP real, disponibilidade pública da ViaCEP, hardware, morte real de processo, matriz de todos os SDKs, avaliação visual humana ou testes instrumentados. A reabertura persistente usa Room/SQLite real dentro de Robolectric; fixtures legadas representam os schemas históricos do repositório. Testes de simultaneidade continuam fora do escopo. As evidências não prometem comportamento de fontes externas arbitrariamente inválidas além do contrato aprovado.

## Inventário executado

Cada cenário abaixo foi executado por testDebugUnitTest na execução final e passou. Alguns testes iteram múltiplas versões de schema.

### ConsultaCepFlowTest

- Passou: miss remoto salva e sucesso limpa entrada
- Passou: reconsulta A mantem dois registros move A ao topo e restaura em nova instancia
- Passou: falha de leitura ou gravacao local nao vira miss nem sucesso
- Passou: cep inexistente nao cria registro
- Passou: falha HTTP preserva A e nao salva B
- Passou: restauracao atrasada nao sobrescreve pesquisa iniciada depois
- Passou: dados parciais concluem com endereco armazenado
- Passou: cep invalido nao acessa remoto nem banco e preserva entrada
- Passou: hit local antigo e offline nao acessa remoto e registra recencia
- Passou: loading aguarda resposta e conclui com entrada limpa
- Passou: restauracao e leitura da lista nao fazem nova consulta nem escrita

### ConsultaCepUiTest

- Passou: mascaraValidacaoLoadingSeteCamposELimpeza
- Passou: falhaAoSalvarPreservaBancoResultadoEPermiteNovaTentativa
- Passou: httpPreservaResultadoToastUnicoENovaTentativa
- Passou: navegacaoListaReativaRecriacaoDaViewSemEscritaOuFeedbackRepetido
- Passou: redeInexistenciaEConsultaLocalOffline

### AddressDtoTest

- Passou: maps a successful response to an address
- Passou: does not map a response marked as not found
- Passou: does not map a response without a zip code
- Passou: maps partial response keeping complementary fields empty
- Passou: desserializaJsonRealComErroNulosECamposAusentes

### AddressDatabaseMigrationTest

- Passou: populatedLegacyDatabasesPreserveContentIdsAndLegacyOrder
- Passou: emptyLegacyDatabasesMigrateToVersionFour
- Passou: freshDatabaseHasCompatibleDefaultAndUniqueZipCode

### RoomCepLocalDataSourceTest

- Passou: does not wrap cancellation from save
- Passou: maps local hit without exposing storage metadata
- Passou: does not wrap cancellation from lookup
- Passou: does not wrap cancellation from observation
- Passou: wraps observation failure as local storage exception and preserves cause
- Passou: wraps save failure as local storage exception and preserves cause
- Passou: wraps lookup failure as local storage exception and preserves cause

### RoomPersistenceTest

- Passou: duplicidadeNaoSubstituiEnderecoNemRecencia
- Passou: cancelamentoAntesDoCommitReverteEscrita
- Passou: missEObservacaoNaoEscrevem
- Passou: consultaLocalMoveAParaOTopoPreservandoDadosEReabertura
- Passou: falhaDeInsercaoReverteInclusiveEfeitosDoTrigger
- Passou: falhaDeRecenciaReverteInclusiveEfeitosDoTrigger
- Passou: observacaoReageARecenciaEUsaIdComoDesempate

### RetrofitCepRemoteDataSourceTest

- Passou: propagates http failure from api
- Passou: maps successful response from substitutable api
- Passou: returns absence for api response marked as not found
- Passou: propagates connection failure from api

### CepRepositoryImplTest

- Passou: does not return remote address when saving it fails
- Passou: returns local address of any age and records consultation without calling remote
- Passou: does not save when remote source does not find address
- Passou: propagates remote http failure without saving
- Passou: propagates cancellation from local save without returning success
- Passou: calls remote and saves response before returning on local miss
- Passou: propagates remote failure without local fallback
- Passou: propagates cancellation from local lookup without calling remote
- Passou: does not call remote when local read fails
- Passou: propagates cancellation from remote lookup without saving

### GetAddressByCepUseCaseTest

- Passou: normalizes a valid zip code and returns success
- Passou: does not convert coroutine cancellation into result failure
- Passou: returns identifiable failure when address is not found
- Passou: rejects invalid zip code before accessing repository
- Passou: returns repository failures without changing their type

### TestInfrastructureTest

- Passou: controlled remote waits for explicit coroutine release without sleep
- Passou: real Room database is isolated between test compositions

### CepViewModelTest

- Passou: feedbackDeFalhaNaoRetornaAoEditarENovaTentativaTemNovoEvento
- Passou: moves to error with identifiable cause when zip code is invalid
- Passou: consumes completion feedback only when the identifier matches
- Passou: restores first saved address without performing a lookup or completion feedback
- Passou: falhaNaRestauracaoPublicaErroSemEscapar
- Passou: preserves address and input when a later search fails
- Passou: does not let delayed restoration replace a valid search loading state
- Passou: cancelamentoDaRestauracaoNaoViraErro
- Passou: preserves observed addresses when a later read fails
- Passou: allows a new restoration read after an initial empty result
- Passou: reabrirListaRetomaLeituraAposFalhaSemConsultarEndereco
- Passou: does not let delayed restoration replace a search state
- Passou: keeps previous address during loading and clears input on success
- Passou: keeps completed state available to a new collector
- Passou: falhaAtrasadaDaRestauracaoNaoSobrescrevePesquisa

### ZipCodeFormatterTest

- Passou: accepts exactly eight digits after normalization
- Passou: normalizes a formatted zip code
- Passou: ignores non numeric characters and limits length
- Passou: keeps an incomplete zip code without separator
- Passou: formats a complete zip code
- Passou: formats pasted text with letters excess digits and leading zeroes
