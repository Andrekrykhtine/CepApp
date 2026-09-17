# Infraestrutura determinística de testes — T-009

Data: 07/09/2026.

## Escopo executado

Esta tarefa disponibiliza infraestrutura de testes JVM local para a consulta de
CEP. Não há aparelho, emulador, ADB, teste instrumentado nem acesso à ViaCEP
pública nesta evidência.

- A composição manual em `AppContainer` aceita fábricas de banco e de fonte
  remota, mantendo os valores de produção como padrão.
- `IsolatedRoomDatabase` cria um banco Room real, com nome exclusivo por teste,
  fecha a instância e remove apenas esse arquivo exclusivo.
- `ControlledCepRemoteDataSource` conta CEPs solicitados e pode suspender a
  resposta com `CompletableDeferred`; o teste libera a operação explicitamente,
  sem `sleep` frágil.
- Robolectric permite executar a integração Room no conjunto `src/test`.
- O adaptador Retrofit é exercitado com `ViaCepApi` substituível para sucesso,
  ausência (`erro: true`), `IOException` e `HttpException` 500. Os testes de
  DTO cobrem resposta parcial e ausência de CEP.

## Arquivos alterados

- `app/build.gradle.kts` e `gradle/libs.versions.toml`: Robolectric e recursos
  Android para testes JVM locais.
- `app/src/main/java/com/example/cepapplication/AppContainer.kt`: fábricas
  opcionais na composição manual, com produção preservada como padrão.
- `app/src/test/java/com/example/cepapplication/testing/ControlledCepRemoteDataSource.kt`:
  double remoto controlável.
- `app/src/test/java/com/example/cepapplication/testing/IsolatedRoomDatabase.kt`:
  ciclo de vida isolado do Room real.
- `app/src/test/java/com/example/cepapplication/testing/TestInfrastructureTest.kt`:
  isolamento do Room e atraso controlado por coroutine.
- `app/src/test/java/com/example/cepapplication/data/remote/RetrofitCepRemoteDataSourceTest.kt`:
  contrato do adaptador remoto.
- `app/src/test/java/com/example/cepapplication/data/ViaCepResponseTest.kt`:
  DTO parcial e resposta sem CEP.

## Evidências executadas

| Verificação | Resultado |
| --- | --- |
| `./gradlew.bat testDebugUnitTest --console=plain --no-configuration-cache` | Aprovado: 48 testes, 0 falhas e 0 erros. Inclui 2 testes Room/Robolectric, 4 testes do adaptador Retrofit e 4 de DTO. |
| `git diff --check` | Aprovado; sem erro de whitespace. O Git emitiu somente avisos de conversão LF→CRLF em arquivos já modificados. |

Uma primeira execução de `testDebugUnitTest` falhou antes da compilação porque
`org.robolectric:robolectric:4.13.2` não está publicado nos repositórios
configurados. A versão foi corrigida para `4.16`, e a execução acima foi a
evidência final.

## Rastreabilidade e limitações

- RF-004/RN-004/RN-005/RN-011 e EC-006 a EC-008: a infraestrutura permite
  controlar resposta, ausência e falhas da fonte remota, sem rede pública.
- RF-012/RN-009 e EC-010: o DTO aceita campos complementares vazios; a
  apresentação de “Não informado” continua responsabilidade da T-007.
- O banco usado no teste é Room real e isolado; a migração histórica e os
  cenários completos UI→persistência pertencem às tarefas T-010 e T-001/T-002.
- A API controlada não comprova disponibilidade pública da ViaCEP nem transporte
  HTTP real. Não foram executados testes instrumentados, conforme `AGENTS.md`.

T-009 desbloqueia a infraestrutura de testes das T-007 e T-008, sem autorizá-las
automaticamente.
