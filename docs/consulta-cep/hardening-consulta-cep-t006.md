# Hardening — T-006: restauração e observação sem efeitos no histórico

Data: 07/09/2026
Status: implementada e validada por testes unitários locais.

## Escopo e regras verificadas

Esta tarefa aplica RF-009, RF-010, RF-011 e RN-006 do FRD, além de EC-014 e CA-11 do Blueprint.

- A restauração automática lê somente o primeiro endereço da lista já ordenada; não consulta a ViaCEP, não escreve no banco, não altera a recência, não limpa a entrada e não produz feedback de conclusão.
- Uma pesquisa iniciada após a restauração ter começado invalida e cancela apenas essa leitura automática. Antes de publicar, a restauração ainda confirma que seu marcador é atual e que a tela continua em `Idle`.
- A proteção vale também para pesquisa inválida ou que termine em erro: uma restauração atrasada não troca esse estado por `Success`.
- A observação da lista distingue leitura ainda não concluída, lista recebida e falha de leitura. Em falha, preserva a última lista válida e não a interpreta como lista vazia.
- Uma nova abertura pode solicitar nova restauração enquanto a tela permanece inicial; não foi introduzido polling nem retentativa periódica.

Não foram adicionados mutex, fila, serialização, guardas para consultas simultâneas, novas telas ou alterações de recência.

## Arquivos alterados

- `app/src/main/java/com/example/cepapplication/ui/CepUiState.kt`: inclui `SavedAddressesState`, metadados internos da observação de lista.
- `app/src/main/java/com/example/cepapplication/ui/CepViewModel.kt`: adiciona observação que preserva dados em erro e proteção de versão/`Job` exclusiva da restauração inicial.
- `app/src/test/java/com/example/cepapplication/ui/CepViewModelTest.kt`: cobre restauração normal sem consulta, invalidação por pesquisa válida em carregamento e por pesquisa inválida, erro de leitura com lista preservada e nova leitura após resultado inicial vazio.

## Verificações executadas

| Comando | Resultado |
| --- | --- |
| `.\gradlew.bat testDebugUnitTest` | Aprovado — BUILD SUCCESSFUL, 28 tarefas; inclui `CepViewModelTest`. |
| `.\gradlew.bat assembleDebug` | Aprovado — BUILD SUCCESSFUL, 38 tarefas. |

O build reportou avisos preexistentes de nomes de parâmetros em migrações de `AddressDatabase.kt`; não pertencem ao escopo desta tarefa e não impediram a compilação.

## Limitações e próximos passos

Não foram executados testes instrumentados, ADB, emulador ou teste de transporte HTTP real, conforme a orientação vigente do projeto. A integração da renderização de lista e a validação ponta a ponta permanecem nas tarefas T-008 e T-010; a integração de tela de pesquisa permanece na T-007.
