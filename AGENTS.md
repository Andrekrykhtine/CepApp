# Orientações para trabalhar neste projeto

## Contexto e referência funcional

Aplicativo Android em Kotlin para consultar endereços por CEP na ViaCEP, armazená-los no dispositivo e listar os CEPs consultados.

- Leia `docs/consulta-cep/frd-consulta-cep.md` antes de alterar o fluxo de consulta ou persistência. Esse documento contém o escopo aprovado em 07/09/2026.
- O FRD descreve o comportamento desejado; sua existência não significa que todas as mudanças estejam implementadas.
- `docs/consulta-cep/hardening-consulta-cep.md` é um registro histórico. O README e o código ainda podem descrever o TTL de 24 horas, substituído pela decisão do FRD.
- Instruções explícitas posteriores do usuário prevalecem. Registre mudanças de escopo nos documentos correspondentes, sem transformar sugestões em requisitos aprovados.
- Comunique-se e escreva documentação do projeto em português.

## Regras aprovadas que devem orientar mudanças

- Entrada por CEP; não implementar pesquisa por nome ou endereço.
- Preservar as telas e o fluxo existentes, incluindo máscara, limpeza da entrada após sucesso e resultado anterior visível após falha.
- Validar/normalizar antes de consultar dados.
- Consultar Room primeiro. CEP existente deve retornar localmente, sem expiração, sem acesso remoto e sem aviso de cache.
- Consultar ViaCEP somente para CEP ausente localmente e salvar automaticamente o sucesso, sem duplicar o CEP.
- Registrar a última consulta bem-sucedida também em cache hit. Essa recência governa a lista e o endereço restaurado ao abrir o app.
- Abrir telas ou restaurar o último endereço não conta como nova consulta.
- HTTP 500 para CEP novo deve produzir erro e preservar o resultado anterior visível. Não apresentar esse resultado anterior como se pertencesse ao CEP que falhou.
- Não acrescentar novas telas, campos, exclusão, atualização manual, expiração ou atualização em segundo plano sem alteração explícita do escopo.

## Organização do código

Fontes principais em `app/src/main/java/com/example/cepapplication/`:

- `MainActivity.kt`, `SearchFragment.kt`, `SavedAddressesFragment.kt`: navegação, eventos e renderização da UI.
- `ui/`: ViewModel e estado da consulta via StateFlow.
- `domain/`: entidade `Address`, contratos de repositório, casos de uso e validação/formatação de CEP. Manter livre de Android, Retrofit e Room.
- `data/remote/` e `data/ViaCepApi.kt`, `data/ViaCepService.kt`: acesso à ViaCEP com Retrofit e funções suspensas.
- `data/local/`: Room, DAO, entidade, migrações e fonte local.
- `data/repository/`: coordenação do acesso local/remoto conforme o FRD.
- `AppContainer.kt` e `CepApplication.kt`: composição manual de dependências existente.
- `app/src/main/res/`: layouts XML, navegação e textos. Manter mensagens em recursos de strings.

Preservar a separação entre UI, domínio e dados. A UI não deve acessar Retrofit ou DAO diretamente. Manter o contrato do repositório no domínio, conforme o código existente. Não introduzir framework de injeção ou reestruturar a stack sem necessidade do trabalho solicitado.

## Persistência e execução assíncrona

- Preservar dados existentes e a unicidade do CEP normalizado.
- Ao alterar o schema Room, fornecer migração compatível; não apagar o banco nem adotar migração destrutiva para contornar incompatibilidade.
- Não confundir data de obtenção remota com a última consulta bem-sucedida. O formato de persistência pode ser definido na implementação, respeitando o FRD.
- Manter operações assíncronas com Coroutines, coleta vinculada ao ciclo de vida e propagação de cancelamento. Não converter `CancellationException` em falha de negócio.
- Preservar o bloqueio de campo/botão durante a consulta (RF-007). Por decisão do usuário em 07/09/2026, o fluxo do app não contempla consultas simultâneas: não acrescentar mutex, serialização no repositório, guardas adicionais no ViewModel, segunda leitura antes do salvamento ou testes específicos para proteger contra consultas simultâneas.
- Manter a unicidade do CEP e a atomicidade do salvamento/registro de recência. A exclusão de proteções para consultas simultâneas não elimina essas garantias de persistência.
- Impedir que a restauração inicial atrasada sobrescreva o estado de uma pesquisa iniciada depois dela. Essa leitura automática não constitui uma segunda consulta do usuário.

## Validação das alterações

Use o Gradle Wrapper do repositório. No PowerShell, conforme o escopo da alteração:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Quando necessário para verificar banco, migração ou UI e houver dispositivo/emulador disponível:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

- Testes unitários existentes ficam em `app/src/test/`; instrumentados em `app/src/androidTest/`.
- Para mudanças de cache e histórico, verificar retorno local sem rede independentemente da idade, cache miss com salvamento, ausência de duplicatas, recência em consultas locais e preservação de histórico após falhas.
- Atualizar testes que imponham o antigo TTL quando essa regra for removida. Não usar testes obsoletos para reintroduzir expiração.
- Mudanças apenas documentais dispensam build Android; revisar consistência, caminhos e `git diff --check`.
- Informar quais verificações foram efetivamente executadas e suas limitações. O relatório histórico registra problema de loopback no Gradle; isso não prova que o ambiente atual falha nem que o build está aprovado.
- Não atualizar dependências, SDK ou ferramentas apenas para acompanhar versões recentes. Consultar os arquivos Gradle e o catálogo `gradle/libs.versions.toml` para a configuração vigente.

## Entrega e documentação

- Fazer alterações proporcionais ao pedido e preservar modificações do usuário.
- Ao implementar o FRD, atualizar documentação ativa e informar o que foi implementado, testado ou permaneceu pendente.
- Não reescrever resultados de validação históricos como se fossem verificações atuais.
- Manter documentos desta funcionalidade em `docs/consulta-cep/`.
