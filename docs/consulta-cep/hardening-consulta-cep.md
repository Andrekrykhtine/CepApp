# Hardening da consulta de CEP

Data: 31/08/2026

## Especificação aplicada

A implementação foi adequada ao fluxo do diagrama de arquitetura fornecido:

1. A UI envia o CEP informado para `CepViewModel.search`.
2. O ViewModel invoca `GetAddressByCepUseCase` com o valor bruto.
3. O caso de uso valida e normaliza o valor por meio de `CepFormatter`.
4. O caso de uso acessa o contrato `CepRepository` somente com o CEP normalizado.
5. `CepRepositoryImpl` consulta primeiro `CepLocalDataSource`.
6. Em cache miss, o repositório consulta `CepRemoteDataSource` e salva a resposta no Room.
7. O repositório devolve a entidade de domínio pura `Address`.
8. O caso de uso converte o resultado em `Result<Address>`, com falhas identificáveis.
9. O ViewModel publica `Loading`, `Success` ou `Error` no `StateFlow` observado pela UI.

## O que foi endurecido

- Validação centralizada: nenhum acesso ao repositório ocorre se o CEP não contiver exatamente oito dígitos após a normalização.
- Normalização centralizada: máscara e caracteres não numéricos são removidos antes da consulta.
- Erros de CEP inválido e CEP não encontrado são diferenciados por tipos próprios.
- Cancelamentos de coroutine continuam sendo propagados e não viram estado de erro.
- A UI não instancia nem acessa Retrofit, DAO, banco ou repositório.
- O repositório aplica cache-first: local, remoto em cache miss, persistência e retorno.
- O CEP formatado devolvido pela ViaCEP é normalizado antes de ser salvo, garantindo cache hit nas próximas consultas.
- O SQLite manual foi substituído por Room, com migração `1 -> 2` que preserva os registros existentes.
- O modelo `Address` foi movido para o domínio e não depende de Android, Retrofit ou Room.
- A tela de endereços armazenados foi preservada por decisão aprovada e passou a consumir o mesmo ViewModel/repositório.

## Arquivos modificados

- `gradle/libs.versions.toml` e `app/build.gradle.kts`: Room, KSP, ViewModel e coroutines-test.
- `CepApplication.kt` e `AppContainer.kt`: composição das dependências.
- `domain/model`, `domain/repository`, `domain/util` e `domain/usecase`: entidade, contrato, formatter e casos de uso.
- `data/local`: entidade, DAO, banco, migração e data source Room.
- `data/remote`: contrato e data source Retrofit; `AddressDto` separado do domínio.
- `data/repository/CepRepositoryImpl.kt`: estratégia cache-first.
- `ui/CepUiState.kt` e `ui/CepViewModel.kt`: estado e coordenação da consulta.
- `MainActivity.kt`, `SearchFragment.kt` e `SavedAddressesFragment.kt`: propriedade do ViewModel, envio de eventos e renderização do estado.
- Testes de formatter, DTO, caso de uso, repositório e ViewModel.

As classes antigas `Address` no pacote de dados, `AddressRepository` e `AddressStore` foram removidas porque suas responsabilidades foram redistribuídas pelas camadas especificadas.

## Testes e verificações

### Testes puros executados

Os fontes de domínio, os contratos dos data sources, o repositório e seus testes foram compilados diretamente com Kotlin 2.3.10. Em seguida, JUnit 4.13.2 executou:

- 5 testes de `CepFormatter`;
- 4 testes de `GetAddressByCepUseCase`;
- 3 testes de `CepRepositoryImpl`.

Resultado: `OK (12 tests)`.

Foram validados CEP completo, normalização, limite da máscara, entrada incompleta, entrada excedente, acesso ao repositório somente após validação, not-found, cancelamento, cache hit, cache miss, salvamento local e resposta remota ausente.

### Testes escritos e pendentes de execução Gradle

- 2 testes de `CepViewModel`, cobrindo `Loading -> Success` e `Loading -> Error`.
- 2 testes de mapeamento de `AddressDto`.

O comando `gradlew.bat --no-daemon testDebugUnitTest` não chegou à configuração do projeto. O Gradle 9.5 tentou iniciar um processo isolado e o ambiente recusou sua conexão loopback com `java.io.IOException: Unable to establish loopback connection`. O mesmo ocorreu com JDK 17, JDK 21, dentro e fora do sandbox. Portanto, não foi possível executar o build Android, o KSP/Room ou os testes de ViewModel neste ambiente.

### Verificações estáticas

- O domínio compilou isoladamente sem erros.
- Não restaram referências a `AddressStore`, `AddressRepository`, `ViaCepResponse` ou ao antigo `data.Address`.
- `git diff --check` não encontrou erros de whitespace; apenas avisos de conversão LF/CRLF do checkout Windows.

## Gaps da especificação

- O diagrama não define o estado inicial; foi usado `Idle` para evitar exibir loading, sucesso ou erro antes da primeira consulta.
- O diagrama cita `MainActivity`, enquanto o projeto já possuía fragments. A Activity é proprietária do ViewModel e os fragments compõem a camada de UI.
- A tela de CEPs armazenados não aparece no diagrama. Ela foi preservada conforme aprovação, usando `GetSavedAddressesUseCase` e o mesmo repositório.
- O diagrama não determina framework de injeção de dependências; foi usado um container manual pequeno.
- O diagrama não especifica mensagens ou taxonomia de falhas. Foram criadas somente as distinções necessárias para os comportamentos já existentes: inválido, não encontrado e erro de infraestrutura.

## O que não foi alterado

- Layouts XML, tema e fluxo de navegação, pois a especificação trata da arquitetura e não propõe mudanças visuais.
- Endpoint e configuração base da ViaCEP.
- Formato da apresentação do endereço.
- Banco `addresses.db` e dados existentes; a migração os preserva.
