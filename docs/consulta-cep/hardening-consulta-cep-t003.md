# Hardening — T-003: prioridade local sem TTL

Data: 07/09/2026.

## Escopo executado

A T-003 substituiu a coordenação baseada em TTL pelo fluxo local primeiro:

1. A fonte local executa `findAndRecordConsultation`.
2. Um endereço local retorna imediatamente, sem considerar idade e sem acessar a ViaCEP.
3. Apenas a ausência local (`null`) permite consultar a fonte remota.
4. Um endereço remoto é retornado somente após `saveAndRecordConsultation` concluir.
5. Ausência remota não grava dados; falhas locais/remotas atravessam a camada e não são convertidas em cache miss ou fallback.

Não foram adicionados mutex, serialização, releitura defensiva, estados de UI ou mensagens. O tratamento tipado de falhas locais e o cancelamento explícito permanecem no escopo da T-004.

## Diferença encontrada e enforcement aplicado

| Regra | Antes | Depois |
| --- | --- | --- |
| RN-003 | Registro local era considerado válido por 24 horas. | A idade não participa do contrato nem da decisão. |
| RN-004 | Após cache vencido, a rede podia ser chamada; falha local não estava distinguida neste fluxo. | A rede é chamada apenas após `null` local; exceção local interrompe a consulta. |
| RN-005 | O retorno remoto era salvo pelo contrato legado. | O retorno remoto depende da confirmação de `saveAndRecordConsultation`. |
| RN-006/RN-007 | O repositório não usava a operação de hit que registra recência. | Todo hit usa a operação transacional de hit da T-002. |
| RN-011 | `IOException` remoto fazia fallback para cache vencido. | Não existe fallback: CEP local nunca chega à rede; erro remoto no miss é propagado. |

## Arquivos alterados

- `app/src/main/java/com/example/cepapplication/data/repository/CepRepositoryImpl.kt`: removeu TTL, cálculo de idade e fallback; coordenou local → remoto → salvamento confirmado.
- `app/src/main/java/com/example/cepapplication/data/local/CepLocalDataSource.kt`: removeu `CachedAddress`, `findByZipCode` e `save` legados.
- `app/src/main/java/com/example/cepapplication/data/local/RoomCepLocalDataSource.kt`: removeu as implementações do contrato legado, preservando as operações transacionais e observação.
- `app/src/test/java/com/example/cepapplication/data/repository/CepRepositoryImplTest.kt`: substituiu cenários de expiração/fallback por hit sem rede, miss com salvamento, ausência remota e falhas local/remota/de salvamento.

## Verificações

- `git diff --check`: executado com sucesso, sem erro de whitespace.
- Busca por `CachedAddress`, TTL e assinaturas locais legadas: não restaram ocorrências no repositório/fonte local/testes; `AddressDao.findByZipCode` permanece por ser detalhe interno da transação Room, não o contrato legado.
- `./gradlew.bat testDebugUnitTest`: executado com sucesso após o wrapper remover `TEMP` e `TMP` apenas do processo do Gradle, evitando a falha de loopback do Java NIO.
- `./gradlew.bat assembleDebug`: executado com sucesso com o mesmo workaround temporário do wrapper.
- Testes instrumentados: removidos do projeto por decisão posterior do usuário; não há validação por aparelho, emulador, ADB ou `connectedDebugAndroidTest`.

Portanto, os testes unitários e o empacotamento debug foram validados nesta tarefa. Este relatório é atual e não altera o registro histórico em `hardening-consulta-cep.md`.
