# Validação da consulta de CEP

## Identificação

- Data: 07/09/2026
- Tarefa: T-011 — Verificações finais e evidências
- Revisão observada: `9f57911`
- Estado: alterações locais das tarefas anteriores presentes na árvore de trabalho; nenhuma alteração funcional foi feita nesta tarefa.

## Ambiente

- Sistema: Windows 11 amd64
- Java: OpenJDK Temurin 17.0.20
- Gradle Wrapper: Gradle 9.5.0
- Android SDK configurado: `C:\Users\andre.peres\AppData\Local\Android\Sdk`

## Comandos e resultados

| Comando | Resultado |
| --- | --- |
| `.\gradlew.bat testDebugUnitTest` | **Não executado até as tarefas**: o script terminou imediatamente com “Acesso negado”. |
| Wrapper direto com `testDebugUnitTest` | **Falhou por ambiente** antes da compilação. O Android Gradle Plugin não conseguiu ler `platforms\android-37.0\package.xml` nem calcular o hash de `licenses\android-sdk-license`; ambos retornaram “Acesso negado”. |
| Wrapper direto com `assembleDebug` | **Falhou por ambiente** antes da compilação. O Android Gradle Plugin não conseguiu ler `platforms\android-37.0\package.xml` e `.knownPackages`; o acesso foi negado. |
| `git diff --check` | **Passou**, código de saída 0. Foram emitidos apenas avisos de conversão LF/CRLF. |

O Wrapper direto foi usado apenas como diagnóstico equivalente porque `gradlew.bat` contém uma alteração local que zera `TEMP` e `TMP`. Essa alteração não foi revertida.

## Cobertura prevista e efetivamente executada

O conjunto local contém testes para formatter, DTO ViaCEP, casos de uso, ViewModel, repositório, Room isolado, adaptador remoto controlado e o fluxo `ConsultaCepFlowTest`. Entretanto, `testDebugUnitTest` não conseguiu iniciar a execução devido ao bloqueio de leitura do Android SDK. Portanto, não há resultado atual que permita declarar esses cenários aprovados.

Não foram executados aparelho, emulador, ADB ou `connectedDebugAndroidTest`, conforme as instruções do projeto.

## Pendências

- Reexecutar `testDebugUnitTest` e `assembleDebug` em ambiente com permissão de leitura para o Android SDK configurado.
- Após uma execução bem-sucedida, atualizar este relatório com os resultados reais dos testes e do build.

O problema encontrado é de acesso ao ambiente externo do SDK, não uma falha de compilação ou de teste atribuível ao código; essa distinção permanece sem confirmação até a reexecução autorizada.

## Nota posterior — T-012

Em 07/09/2026, a T-012 atualizou a documentação ativa com base neste registro, sem alterar os resultados acima. Os testes unitários e o build continuam pendentes de reexecução em ambiente com acesso ao Android SDK; `git diff --check` permanece como a única verificação efetivamente registrada como aprovada nesta validação.

## Nova execução — correções da revisão aprovadas, 07/09/2026

Esta seção é posterior à T-011/T-012 e não altera os resultados históricos acima.

Após aprovação de R-01–R-07, foram corrigidos restauração, recuperação da lista, feedback de erro, sincronização de testes e colagem no EditText. Foram portados testes de migração/Room para execução local e acrescentada cobertura das telas.

- Estado validado: HEAD 9f57911 mais alterações locais anteriores e correções desta revisão.
- Comando: `.\gradlew.bat testDebugUnitTest assembleDebug`, com permissão ampliada aceita pelo ambiente.
- Resultado: **saída 0; 80 testes passaram, zero falhas; assembleDebug concluído**. BUILD SUCCESSFUL em 31 segundos.
- Gradle: Wrapper 9.5.0; Launcher JVM JetBrains 25.0.2; critério do daemon Java 21, conforme --version consultado na revisão. Nenhuma ferramenta/dependência foi atualizada.
- Evidências: app/build/reports/tests/testDebugUnitTest/index.html, XMLs em app/build/test-results/testDebugUnitTest/ e APK em app/build/outputs/apk/debug/app-debug.apk.
- A primeira recompilação incremental apresentou referências não resolvidas a formatadores existentes; --rerun-tasks recompilou com sucesso sem alterar esses formatadores. Ajustes nas novas fixtures e o bug da colagem foram concluídos antes da execução final.
- git diff --check e verificação de whitespace dos arquivos novos integram a revisão final.
- Inventário das 12 classes, 80 cenários e matriz de requisitos em [test-report-consulta-cep.md](test-report-consulta-cep.md).

Não houve aparelho, emulador, ADB ou teste instrumentado. As telas/migrações rodam em Robolectric. Não foi testado transporte HTTP público, disponibilidade ViaCEP ou hardware. Não há pendência conhecida de R-01–R-07; as limitações acima delimitam a evidência obtida.
