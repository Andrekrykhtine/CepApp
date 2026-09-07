# Hardening T-002 — Operações locais atômicas e ordenação

Data: 07/09/2026.
Status: implementação concluída; compilação e validação instrumentada pendentes por impedimentos atuais do ambiente.

## Escopo e referências

Plano aprovado explicitamente pelo usuário com “sim”. Foram usados `blueprint-consulta-cep.md` (T-002 e seções 6.3/6.4), `frd-consulta-cep.md`, o `AGENTS.md` da raiz e a skill business-logic-hardening.

A T-001 já estava presente no checkout, com schema v4, migração e teste instrumentado. Suas alterações foram preservadas; o relatório anterior registra verificações pendentes. Não foi reimplementada nem considerada integralmente validada.

## Especificação e alterações

| Antes | Implementado na T-002 |
| --- | --- |
| Consulta local somente lê | Transação busca o CEP, calcula MAX + 1 e altera apenas a recência; miss não escreve |
| Inserção REPLACE sem ordinal | Transação calcula a sequência e insere endereço/timestamp/recência com ABORT |
| Ordem por timestamp | Ordem por last_consultation_order DESC, id DESC |
| Contratos locais legados | Novos contratos retornam Address depois da transação; contratos antigos preservados provisoriamente |

Arquivos relativos à raiz:

- `app/src/main/java/com/example/cepapplication/data/local/AddressDao.kt`: operações transacionais, cálculo de sequência, atualização de recência, ABORT e ordenação.
- `app/src/main/java/com/example/cepapplication/data/local/AddressEntity.kt`: mapeador recebe explicitamente timestamp e ordinal; a coluna preexistente da T-001 permanece intacta.
- `app/src/main/java/com/example/cepapplication/data/local/CepLocalDataSource.kt`: novos contratos.
- `app/src/main/java/com/example/cepapplication/data/local/RoomCepLocalDataSource.kt`: delegação transacional e mapeamento; save legado usa o salvamento atômico.
- `app/src/test/java/com/example/cepapplication/data/repository/CepRepositoryImplTest.kt`: double adaptado aos membros novos, que ainda não devem ser chamados pelo repositório legado.
- `app/src/androidTest/java/com/example/cepapplication/data/local/RoomCepLocalDataSourceTest.kt`: sete testes com Room/SQLite reais e banco exclusivo por teste.

Falhas de escrita e conflitos atravessam a fonte local e impedem retorno de sucesso. Cancelamento não é capturado nem convertido. A transação mantém cálculo e escrita juntos; leitura/observação não usa atualização. Não foram acrescentados mecanismos para consultas simultâneas ou releitura defensiva.

## Testes preparados

1. A/B/A: primeira sequência 1, incrementos, dois registros, A no topo, preservação de id/campos/timestamp e reabertura.
2. Miss em banco vazio/povoado e observações repetidas sem escrita.
3. Duplicidade com ABORT preservando todos os dados e recência anteriores.
4. Trigger restrito ao banco de teste provoca falha de inserção após efeito parcial; snapshot deve permanecer idêntico.
5. Trigger provoca falha de atualização da recência após efeito parcial; snapshot deve permanecer idêntico.
6. Ordem por id em empate e emissão reativa após reconsulta, sincronizada por coroutine sem sleeps.
7. Compatibilidade do salvamento legado e leitura CachedAddress, com ordinal persistido.

Os campos de domínio são comparados integralmente, incluindo CEP com zeros iniciais e complemento vazio. Os bancos de teste têm nomes UUID e são fechados/removidos sem usar `addresses.db`.

## Verificações efetivamente executadas

| Comando PowerShell na raiz | Resultado atual |
| --- | --- |
| `.\gradlew.bat testDebugUnitTest` | Exit 1: após execução permitida, falha ao iniciar daemon: `java.io.IOException: Unable to establish loopback connection` |
| `.\gradlew.bat assembleDebug` | Exit 1: mesma falha de loopback antes de compilar |
| `.\gradlew.bat connectedDebugAndroidTest` | Exit 1: mesma falha antes de executar instrumentação |
| `.\gradlew.bat --no-daemon '-Dorg.gradle.jvmargs=' testDebugUnitTest` | Exit 1: Gradle ainda requer daemon de uso único e apresenta a mesma falha |
| `& 'C:\Users\andre.peres\AppData\Local\Android\Sdk\platform-tools\adb.exe' devices -l` | Exit 0: lista sem dispositivos conectados |
| `git diff --check` | Exit 0: sem erros; somente avisos de conversão LF/CRLF |

As primeiras tentativas restritas retornaram “Acesso negado”; a repetição com permissão de execução alcançou o Gradle e confirmou o impedimento acima. Não houve rejeição da revisão automática. Não foram alterados JDK, SDK, dependências ou configuração funcional para contornar o ambiente.

Nenhum teste foi declarado aprovado: as suítes não chegaram a executar. Revisão estática e verificação de whitespace não comprovam compilação nem atomicidade em execução. A T-001 também continua sem validação executada nesta entrega.

## Rastreabilidade e limites

A implementação entrega a base local de RF-003, RF-005, RF-006, RF-010 e RF-011; RN-003, RN-006 e RN-007. A garantia de retorno local sem rede no fluxo completo depende da T-003.

Os contratos antigos e CachedAddress permanecem conforme a transição prevista no Blueprint. O repositório ainda usa TTL: uma tentativa legada de substituir CEP expirado agora falha por ABORT. Remover TTL/fallback e passar a registrar hits por meio do novo contrato é escopo da T-003. Encapsular falhas em LocalStorageException pertence à T-004. Não se afirma conformidade integral do app com o FRD nesta etapa intermediária.

T-003 pode prosseguir a partir dos contratos implementados, mediante seleção do usuário, com validação técnica da T-002 ainda pendente. E2E de UI/histórico é responsabilidade da T-010. Não há backend próprio; transporte HTTP real e disponibilidade pública da ViaCEP não foram verificados neste escopo local.

O relatório histórico `hardening-consulta-cep.md`, migrações e relatório T-001 foram preservados. Nenhuma tarefa seguinte foi executada.
