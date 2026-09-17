# Hardening — T-004: falhas locais, remotas e cancelamento

Data: 07/09/2026.

## Escopo executado

A T-004 tornou explícitos os contratos de falha da consulta de CEP, sem alterar
o fluxo local-primeiro da T-003 nem antecipar o estado renderizável das tarefas
T-005 e T-007.

- Falhas da fonte Room são encapsuladas em `LocalStorageException`, com a causa
  original preservada.
- Cancelamentos de coroutine são relançados pela fonte local e continuam a
  atravessar repositório e caso de uso sem virar erro de negócio.
- A UI diferencia somente conexão/timeout remoto (`IOException`) das demais
  falhas: armazenamento e HTTP usam a mensagem genérica existente.
- A validação centralizada existente permanece anterior ao acesso ao repositório;
  inexistência continua sendo `CepNotFoundException`.

Não foram incluídos mutex, serialização, releitura defensiva, novas mensagens,
novos estados, telas ou validações adicionais.

## Diferença encontrada e enforcement aplicado

| Regra | Antes | Depois |
| --- | --- | --- |
| Falha de leitura/gravação local | A exceção do DAO era repassada sem tipo. Uma `IOException` local podia ser apresentada como falha de rede. | A fonte Room encapsula falhas não canceladas em `LocalStorageException`, que não herda de `IOException`. |
| Observação local | Falhas da `Flow` não tinham contrato identificado. | Falhas não canceladas do fluxo são encapsuladas; cancelamento é preservado. |
| Cancelamento | O caso de uso o relançava, mas não havia cobertura nas três operações do repositório/fonte local. | Testes comprovam passagem sem conversão no hit local, chamada remota e salvamento local, além das operações da fonte Room. |
| Mensagem de erro | A UI mapeava `IOException` para rede sem distinguir origem local. | `LocalStorageException`, HTTP e outras falhas usam `error_unexpected`; `IOException` remoto continua em `error_network`. |

## Arquivos alterados

- `app/src/main/java/com/example/cepapplication/data/local/LocalStorageException.kt`: novo tipo de falha local que preserva a causa.
- `app/src/main/java/com/example/cepapplication/data/local/RoomCepLocalDataSource.kt`: encapsulamento das operações de hit, salvamento e observação, com relançamento de `CancellationException`.
- `app/src/main/java/com/example/cepapplication/SearchFragment.kt`: mapeamento explícito de falha local para a mensagem genérica existente.
- `app/src/test/java/com/example/cepapplication/data/local/RoomCepLocalDataSourceTest.kt`: cobertura de encapsulamento, causa, hit e cancelamento com DAO fake.
- `app/src/test/java/com/example/cepapplication/data/repository/CepRepositoryImplTest.kt`: cobertura de cancelamento nas etapas local, remota e de salvamento.
- `app/src/test/java/com/example/cepapplication/domain/usecase/GetAddressByCepUseCaseTest.kt`: cobertura de preservação do tipo de falha não cancelada, além dos casos já existentes de entrada inválida, ausência e cancelamento.

## Rastreabilidade

- RF-002/RN-001/RN-002: a entrada inválida continua retornando `InvalidCepException` antes do repositório; CEP válido é normalizado como texto, preservando zeros.
- RF-009/RN-008: T-004 mapeia as causas corretas; a preservação renderizável da entrada/endereço anterior é responsabilidade das T-005/T-007 e não foi antecipada.
- RN-004: erro local não é convertido em ausência e interrompe a consulta antes da rede.
- RN-011: `IOException` remoto no miss recebe mensagem de rede; HTTP e falha de armazenamento recebem mensagem genérica; nenhum desses caminhos produz sucesso.
- EC-001, EC-003, EC-006 a EC-009 e EC-013: cobertos nesta tarefa conforme a separação de responsabilidades do Blueprint; cenários completos de UI permanecem para T-007/T-010.

## Verificações executadas

| Comando | Resultado |
| --- | --- |
| `./gradlew.bat testDebugUnitTest` | Aprovado — reexecução final com `BUILD SUCCESSFUL` em 3 s; 28 tarefas, 2 executadas. A primeira execução precisou ocorrer fora do sandbox porque o ambiente isolado retornou `Acesso negado` ao wrapper. |
| `./gradlew.bat assembleDebug` | Aprovado — reexecução final com `BUILD SUCCESSFUL` em 1 s; 38 tarefas atualizadas. |
| `git diff --check` | Aprovado — nenhuma falha de whitespace; o Git apenas informou avisos de conversão LF→CRLF em arquivos já modificados no worktree. |

Não foram usados dispositivo, emulador, ADB ou testes instrumentados, em conformidade com `AGENTS.md`. O teste da fonte local usa o adaptador Room com um `AddressDao` fake para validar o contrato de encapsulamento; a integração de falha real de escrita em SQLite/Room foi removida da validação instrumentada do projeto e permanece sem execução neste ambiente. A T-010 continuará responsável pelos cenários completos de UI→persistência.

## O que não foi alterado

- `GetAddressByCepUseCase.kt` já satisfazia validação antes do repositório, normalização, conversão de ausência e propagação de cancelamento; não foi reescrito.
- `CepRepositoryImpl.kt` já interrompia em falha local/remota/de salvamento e não fazia fallback; não foi modificado.
- `CepUiState.kt`, `CepViewModel.kt` e layouts não foram alterados: entrada, resultado anterior e feedback consumível pertencem às T-005/T-007.
- `docs/consulta-cep/hardening-consulta-cep.md` histórico foi preservado sem modificação.
