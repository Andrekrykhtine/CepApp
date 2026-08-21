# AGENTS.md

## Visao geral do projeto

O **Cep Application** e um aplicativo Android de modulo unico para entrada, validacao, formatacao e persistencia local do ultimo CEP informado pelo usuario. A interface aplica a mascara `00000-000`, rejeita valores incompletos, salva os oito digitos normalizados no Room Database e restaura o valor formatado nas proximas execucoes.

Este arquivo se aplica a todo o repositorio. Preserve as decisoes abaixo ao alterar o projeto.

## Stack vigente

- Android Gradle Plugin 9.3.1 e Gradle Wrapper 9.5.0.
- Gradle Kotlin DSL e catalogo de versoes em `gradle/libs.versions.toml`.
- JVM do Gradle 21; compatibilidade de fonte e bytecode Java 11.
- Android `compileSdk` 37, `targetSdk` 36 e `minSdk` 24.
- Kotlin integrado ao toolchain Android, com `kotlin.code.style=official`.
- UI tradicional com layouts XML, Material Components, AppCompat, ConstraintLayout e ViewBinding.
- Estado de tela com AndroidX ViewModel, `StateFlow` e coroutines ligadas ao ciclo de vida.
- Persistencia local com Room Database encapsulado por uma interface de repositorio.
- Testes locais com JUnit 4.13.2; infraestrutura de testes instrumentados com AndroidX JUnit e Espresso.

## Estrutura e responsabilidades

```text
CepApplication/
|-- app/
|   |-- build.gradle.kts                 # Configuracao do modulo Android
|   `-- src/
|       |-- main/
|       |   |-- AndroidManifest.xml
|       |   |-- java/com/example/cepapplication/
|       |   |   |-- MainActivity.kt     # Composicao e renderizacao da UI
|       |   |   |-- data/               # Contratos e implementacoes de persistencia
|       |   |   |-- domain/             # Regras puras de dominio
|       |   |   `-- ui/                 # Estado e ViewModels
|       |   `-- res/                    # Layouts, strings, temas e demais recursos
|       `-- test/java/...               # Testes unitarios locais, espelhando os pacotes
|-- docs/                               # Relatorios e documentacao tecnica
|-- gradle/libs.versions.toml           # Versoes e aliases de dependencias
|-- build.gradle.kts                    # Plugins compartilhados do projeto
`-- settings.gradle.kts                 # Modulos e repositorios permitidos
```

Mantenha as responsabilidades atuais:

- `MainActivity` conecta views, eventos e ciclo de vida; nao deve conter regras de dominio nem acesso direto a persistencia.
- `ui` concentra o estado imutavel da tela e a orquestracao no `ViewModel`.
- `domain` contem logica Kotlin pura, deterministica e independente do Android.
- `data` expoe contratos de repositorio e isola APIs de armazenamento Android.
- `res` e a fonte para textos visiveis, cores, dimensoes, temas e layouts.

## Convencoes obrigatorias

- Escreva codigo Kotlin seguindo o estilo oficial e a formatacao existente: quatro espacos, tipos e classes em `PascalCase`, funcoes e propriedades em `camelCase` e constantes em `UPPER_SNAKE_CASE`.
- Use nomes de recursos Android em `lower_snake_case`, descritivos pela intencao e nao apenas pelo tipo visual.
- Coloque todo texto exibido ao usuario em `res/values/strings.xml`; nao use strings visiveis hardcoded em Kotlin ou XML.
- Centralize versoes e aliases de novas dependencias em `gradle/libs.versions.toml`.
- Preserve o namespace raiz `com.example.cepapplication` e organize novos arquivos pela responsabilidade (`data`, `domain` ou `ui`).
- Modele o estado de tela com `data class` imutavel e exponha fluxos somente leitura. Mutacoes pertencem ao `ViewModel`.
- Injete dependencias por construtor. Para dependencias do `ViewModel`, use uma `ViewModelProvider.Factory` enquanto o projeto nao adotar deliberadamente uma biblioteca de injecao.
- Prefira funcoes puras para formatacao e validacao. Regras de CEP devem permanecer centralizadas no dominio e nao ser duplicadas na Activity ou no layout.
- Use ViewBinding para acessar views; nao introduza `findViewById`.
- Colete fluxos respeitando o ciclo de vida, com `repeatOnLifecycle` ou mecanismo AndroidX equivalente.
- Testes locais ficam em `app/src/test/java` no pacote correspondente. Nomeie os metodos de teste como frases de comportamento entre crases e cubra sucesso, falha e casos-limite relevantes.
- Commits existentes seguem o formato `<tipo>: <descricao>` (por exemplo, `feat:`, `fix:` e `docs:`); mantenha esse padrao ao criar commits solicitados.

## Decisoes arquiteturais a preservar

- **Um unico modulo `app`:** o tamanho atual nao justifica modularizacao adicional. Nao crie novos modulos sem uma necessidade concreta aprovada.
- **Layouts XML em vez de Compose:** a tela vigente usa ViewBinding e recursos XML. Nao migre para Jetpack Compose de forma incidental.
- **Separacao leve por camadas:** Activity, ViewModel, dominio puro e repositorio fornecem testabilidade sem impor uma arquitetura excessiva. Evite adicionar use cases, frameworks ou camadas vazias sem ganho demonstravel.
- **Fonte unica de estado na UI:** `CepUiState` e o estado renderizado, exposto pelo `CepViewModel` via `StateFlow`.
- **Persistencia simples e local:** `RoomCepRepository` persiste o unico CEP em uma tabela de linha unica. Nao substitua por outro banco de dados, DataStore ou servico remoto sem requisito explicito.
- **CEP persistido normalizado:** grave somente os oito digitos; aplique a mascara apenas na entrada e na exibicao.
- **Repositorio como fronteira:** codigo de UI e dominio nao deve depender diretamente de Room.
- **Dependencias somente de `google()` e `mavenCentral()`:** `settings.gradle.kts` proibe repositorios declarados em modulos.

## Regras para agentes

Antes de editar:

1. Leia `README.md`, os arquivos Gradle relevantes e o codigo da area afetada.
2. Verifique `git status --short` e preserve alteracoes preexistentes do usuario.
3. Consulte a documentacao em `docs/` quando a mudanca tocar uma regra ja registrada.

Ao implementar:

- Faca a menor alteracao coerente com o pedido e com a arquitetura vigente.
- Atualize ou adicione testes para toda alteracao observavel de regra, estado ou persistencia.
- Mantenha a logica Android fora dos testes unitarios locais sempre que uma abstracao ou funcao pura for suficiente.
- Reutilize recursos e componentes existentes antes de criar duplicacoes.
- Atualize a documentacao quando comandos, estrutura ou comportamento documentado mudarem.
- Nao altere IDs de recursos, package name, SDKs, dependencias ou formatos persistidos sem considerar compatibilidade e atualizar todos os consumidores.

Nao faca:

- Nao reverta, apague nem reformate alteracoes fora do escopo.
- Nao edite artefatos gerados em `build/` ou `.gradle/`.
- Nao adicione bibliotecas, plugins, repositorios Maven ou ferramentas arquiteturais sem necessidade clara.
- Nao mova regra de negocio para Activity, XML ou implementacoes de persistencia.
- Nao introduza chamadas de rede, permissoes Android, analytics ou coleta de dados sem requisito explicito.
- Nao considere a tarefa concluida se os testes ou o build relevante estiverem falhando; informe claramente qualquer bloqueio ambiental.

## Execucao local

Pre-requisitos:

- Android Studio compativel com AGP 9.3.1 e Android SDK instalado.
- JDK 21 para o daemon do Gradle.
- Android SDK 37 para compilacao.
- Dispositivo ou emulador com Android 7.0 (API 24) ou superior para executar o app.

No Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

Em macOS ou Linux:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

Para uma verificacao local proporcional a maioria das mudancas, execute os testes unitarios e o build de debug:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Mudancas de interface ou integracao Android tambem devem ser verificadas em emulador/dispositivo. Quando existirem testes instrumentados aplicaveis, execute `connectedDebugAndroidTest` com um dispositivo conectado.
