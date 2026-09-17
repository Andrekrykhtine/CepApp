# Implementação T-001 — Evoluir schema Room e preservar dados

## Escopo

A T-001 adiciona a coluna persistente de recência ao endereço e migra o banco Room da versão 3 para a versão 4, preservando os dados existentes, os identificadores e o índice único de CEP.

## Alterações

- `AddressEntity` passou a declarar `last_consultation_order` como inteiro não nulo, com valor padrão `0`.
- `AddressDatabase` passou à versão 4; as migrações anteriores foram expostas como `internal` para os testes e foi adicionada a migração 3→4.
- A migração 3→4 ordena os registros legados por `saved_at_epoch_millis` e `id`, atribuindo ordinais iniciados em 1. Bancos vazios permanecem vazios e timestamps zero são aceitos.
- Foi criado teste instrumentado com fixtures das versões 1, 2 e 3, cobrindo instalação nova, bancos vazios e povoados, timestamps iguais/zero/distintos, conteúdo, IDs, reabertura, default da coluna e unicidade do CEP.

## Verificação

`git diff --check` foi executado sem erros de whitespace.

As tentativas de executar `testDebugUnitTest`, `assembleDebug` e os testes instrumentados ficaram limitadas pelo ambiente: o Gradle não conseguiu estabelecer a conexão loopback Java e não havia dispositivo/emulador ADB conectado. Portanto, a compilação e a execução dos testes instrumentados permanecem pendentes nesta sessão.

## Rastreabilidade

Atende à base de RF-005, RF-006, RF-010 e RF-011, além de RN-001 e da compatibilidade legada prevista no Blueprint. A atualização da recência durante consultas pertence à T-002.
