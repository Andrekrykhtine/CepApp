# Migra telas de consulta de CEP para Jetpack Compose

## S — Situação

A interface usava Fragments, layouts XML, ViewBinding e RecyclerView. Era necessário migrar as duas telas para Compose preservando o fluxo aprovado de consulta por CEP, o armazenamento local e o histórico por recência.

## T — Tarefa

Reescrever pesquisa, lista, tema e navegação com Jetpack Compose, manter a separação entre UI, domínio e dados e acrescentar cobertura unitária local e documentação de aceite. Esta PR reúne as alterações da branch `etapa5` em relação a `main`, incluindo os ajustes de dependências presentes no checkout.

## A — Ação

- Habilita Compose e seu plugin de compilação; adiciona BOM, Material 3, ícones, Activity, Lifecycle e Navigation Compose, além da infraestrutura de testes Compose/Robolectric.
- Atualiza o Gradle Wrapper de 9.7.1 para 9.8.0 e ajusta suas propriedades; declara explicitamente Compose Runtime 1.12.1 e Material 3 1.4.0 no catálogo e no módulo app.
- Substitui os Fragments, adapter, quatro layouts XML e grafo de navegação XML por `CepApp`, `SearchScreen`, `SavedAddressesScreen`, `AddressCard` e `CepTheme`, hospedados pela `MainActivity`.
- Implementa navegação entre pesquisa e lista, temas claro/escuro, coleta de estado vinculada ao ciclo de vida, máscara do CEP, progresso, bloqueio dos controles, Toasts e lista reativa com `LazyColumn`.
- Mantém o resultado anterior após falha e a limpeza da entrada após sucesso. O cartão apresenta os campos do endereço com tratamento para valores não informados.
- Persiste a entrada parcial no `SavedStateHandle` e adapta a fábrica do ViewModel para restauração de estado. Mantém a recuperação do endereço pelo Room e a proteção contra restauração inicial atrasada.
- Adiciona ou adapta testes locais de componentes Compose, navegação, feedback, formatação, fluxo de consulta e restauração do ViewModel.
- Atualiza README, adiciona FRD, blueprint, revisão visual/documental e matriz de validação da migração, e direciona a configuração do agente executor para as tarefas Compose.
- Marca o uso experimental da barra Material 3 e inclui este arquivo como descrição da PR no método STAR.

## R — Resultado

A UI ativa passa a usar Compose nas duas telas, com ViewModel compartilhado e entrada parcial recuperável. A migração preserva a política existente de consulta local sem expiração, busca remota apenas para CEP ausente, salvamento automático e histórico por última consulta bem-sucedida. Esta branch não altera o schema Room nem as regras do repositório.

### Validação atual — 05/10/2026

- `testDebugUnitTest` e `assembleDebug`: as tentativas iniciais foram bloqueadas por acesso negado ao arquivo de lock do cache do Gradle.
- Nova tentativa autorizada pelo Wrapper, com `testDebugUnitTest assembleDebug`: falhou com `java.io.IOException: Unable to establish loopback connection`, antes de comprovar execução dos testes ou geração do APK.
- `git diff --check`: executado para conferir a higiene das alterações.
- Testes e build permanecem sem aprovação nesta entrega. Não foram usados aparelho, emulador, ADB ou testes instrumentados.
- Os relatórios de setembro permanecem como evidências históricas; não substituem a validação desta PR. A fidelidade visual em dispositivo, o teclado real e a restauração de processo pelo Android continuam sem validação atual.

### Referências

- [FRD](frd-reescrita-telas-compose.md)
- [Blueprint](blueprint-reescrita-telas-compose.md)
- [Revisão visual/documental](revisao-visual-documental-reescrita-telas-compose.md)
- [Validação histórica da migração](validacao-reescrita-telas-compose.md)
