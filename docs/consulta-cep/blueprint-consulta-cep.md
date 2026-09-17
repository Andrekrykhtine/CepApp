# Blueprint Técnico — Consulta e armazenamento de CEP

Data: 07/09/2026.

Status atual: correções R-01–R-07 implementadas e validadas em 07/09/2026: 80 testes locais passaram e assembleDebug concluiu. Evidências em test-report-consulta-cep.md e na nova seção de validacao-consulta-cep.md. Os registros anteriores de T-011/T-012 permanecem históricos.

Referência de escopo: FRD de consulta de CEP, incluindo a decisão de 07/09/2026 que exclui proteções para consultas simultâneas. A referência inicial a “Carrinho de Compras” não corresponde à funcionalidade: o usuário confirmou a manutenção do escopo do FRD de CEP.

Este documento mantém o desenho técnico e os critérios de aceite. A implementação e as correções foram autorizadas em conversa; os resultados efetivos estão nos relatórios datados, não nos pseudocódigos.

## 1. Contexto

### 1.1 Objetivo e atores

O usuário digita ou cola um CEP, aciona “Consultar e salvar”, visualiza o endereço e pode abrir a lista de CEPs armazenados. Room é consultado primeiro. Um endereço existente é suficiente independentemente da idade, inclusive sem internet. Somente um CEP ausente localmente é pesquisado na ViaCEP; o sucesso remoto inclui salvamento automático.

Cada consulta bem-sucedida, local ou remota, move o CEP para o início do histórico. Abrir telas e restaurar o endereço inicial são leituras e não contam como consulta. O histórico possui um registro por CEP, persistido entre sessões. Não há autenticação ou perfis; os atores são o usuário e a ViaCEP.

### 1.2 Requisitos funcionais incorporados

| ID | Comportamento obrigatório |
| --- | --- |
| RF-001 | Durante digitação/colagem, remover caracteres não numéricos, limitar a oito dígitos e aplicar a máscara progressiva `00000-000`. |
| RF-002 | Ao acionar o botão, validar e normalizar antes de consultar dados. Entrada incompleta produz “Digite um CEP válido”, sem acesso ao banco/API para essa consulta. |
| RF-003 | Procurar primeiro no armazenamento local; se encontrado, retornar independentemente da idade, sem API. |
| RF-004 | Consultar ViaCEP somente para CEP ausente localmente; salvar automaticamente o endereço encontrado. |
| RF-005 | Preservar um único registro persistente por CEP normalizado, inclusive em reconsultas e entradas com máscara. |
| RF-006 | Registrar recência em todo sucesso, inclusive local. Falhas não alteram recência. |
| RF-007 | Durante a consulta, mostrar progresso e desabilitar campo e botão até a conclusão. |
| RF-008 | No sucesso, exibir os sete campos do endereço, limpar entrada e liberar controles. |
| RF-009 | No erro, mostrar a mensagem correspondente, liberar controles e preservar entrada e endereço anterior, se houver. |
| RF-010 | Ao abrir, recuperar localmente o último consultado, inclusive após reconsulta local; sem registros, não mostrar endereço. |
| RF-011 | Mostrar lista local reativa, ordenada da última consulta mais recente à mais antiga. Lista vazia mostra “Nenhum CEP armazenado.” |
| RF-012 | Aceitar campos complementares vazios e mostrar “Não informado” em cada campo textual vazio. |

### 1.3 Regras de negócio incorporadas

| ID | Regra obrigatória |
| --- | --- |
| RN-001 | O CEP normalizado de oito dígitos é a identidade; máscara é apresentação. Preservar zeros iniciais. |
| RN-002 | Preservar remoção de não numéricos e truncamento da máscara; não criar rejeição de colagens por letras/excesso. Validar centralmente o valor efetivamente submetido. |
| RN-003 | Endereço salvo não expira, não é excluído e não é atualizado remotamente por idade. |
| RN-004 | API somente após ausência local confirmada; erro de leitura não equivale a ausência. |
| RN-005 | Sucesso remoto inclui armazenamento, sem ação ou confirmação separada de salvar. |
| RN-006 | Todo sucesso atualiza a ordem. Abertura, restauração e visualização da lista não atualizam recência. |
| RN-007 | Reconsulta move o registro ao início sem criar outra entrada. |
| RN-008 | Falha não apaga/substitui o resultado anterior nem o identifica como resposta ao CEP que falhou. |
| RN-009 | Campos complementares vazios não invalidam endereço encontrado; apresentar “Não informado”. |
| RN-010 | Não mostrar origem local, idade, validade ou aviso de desatualização. |
| RN-011 | CEP salvo impede chamada remota e, portanto, HTTP 500 nesse fluxo. HTTP 500 para CEP novo produz erro e preserva apenas o resultado anterior visível. |

Decisão adicional incorporada: o fluxo não contempla consultas simultâneas do usuário. Não adicionar mutex, serialização no repositório, guardas extras no ViewModel, releitura defensiva antes de inserir ou testes desse cenário. Permanecem RF-007, unicidade, atomicidade da persistência e proteção contra restauração inicial atrasada sobrescrever uma pesquisa iniciada depois dela.

### 1.4 Arquitetura real e diferença em relação à imagem

```text
MainActivity: navegação e ViewModel compartilhado
  SearchFragment / SavedAddressesFragment
                  ↓ eventos / ↑ StateFlow
             CepViewModel
                  ↓
  GetAddressByCepUseCase / GetSavedAddressesUseCase
                  ↓
          CepRepository (domínio)
                  ↓
       CepRepositoryImpl (dados)
          ↓                  ↓ somente no miss
 RoomCepLocalDataSource   RetrofitCepRemoteDataSource
          ↓                  ↓
      AddressDao          ViaCepApi / AddressDto
          ↓                  ↓
   Room: addresses.db       ViaCEP
```

A imagem posiciona a interface do repositório na camada de dados e simplifica a UI como MainActivity. O projeto real mantém o contrato no domínio e usa Fragments. Preservar essa organização, os layouts XML, ViewBinding e a composição manual de dependências em AppContainer/CepApplication. UI não acessa DAO ou Retrofit diretamente; domínio não importa Android, Room ou Retrofit.

### 1.5 Stack verificada nos arquivos do projeto

| Item | Configuração observada |
| --- | --- |
| Linguagem | Kotlin; nenhuma versão independente de plugin Kotlin declarada no catálogo consultado |
| Android Gradle Plugin / Wrapper | 9.3.1 / Gradle 9.5.0 |
| SDK | compileSdk 37, targetSdk 36, minSdk 24 |
| Compatibilidade Java do módulo | Source/target 11; isso não determina por si só o JDK exigido pelo Gradle/AGP |
| Room / KSP | 2.8.4 / 2.3.10 |
| Retrofit e converter Gson | 3.0.0 |
| Coroutines Android / testes | 1.11.0 / 1.10.2 |
| Lifecycle / Navigation | 2.10.0 / 2.10.0 |
| AppCompat / Material | 1.7.1 / 1.14.0 |
| JUnit / Robolectric | 4.13.2 / 4.16 |
| UI | Activity, Fragments, XML e ViewBinding |
| Banco atual | `addresses.db`, versão 4, migrações 1→2, 2→3 e 3→4, `exportSchema = false` |

Não atualizar dependências, SDK, JDK ou ferramentas apenas para acompanhar versões. Os valores acima são leitura da configuração local, não comprovação de build aprovado. Não se exige exportação retroativa de schemas inexistentes para executar os testes de migração: usar fixtures SQLite e validar a abertura pelo Room real.

### 1.6 Baseline anterior à implementação e mudança aprovada

| Situação observada | Destino |
| --- | --- |
| Repositório verifica TTL de 24 horas e usa cache vencido após IOException. | Remover TTL e fallback; hit local nunca chama API. |
| Fonte local expõe `CachedAddress` com timestamp. | Expor operações que retornam endereço após registrar recência. |
| DAO ordena por `saved_at_epoch_millis DESC, id DESC`. | Ordenar por sequência persistente de consulta. |
| DAO usa inserção com REPLACE. | Inserção de CEP novo sem substituição silenciosa; hit atualiza só recência. |
| ViewModel armazena somente Idle/Loading/Success/Error. | Manter também entrada e endereço anterior para reconstruir a View. |
| Fragment usa `wasLoading` para limpar entrada e emitir feedback. | Limpeza por sucesso no ViewModel; feedback de conclusão independente de observar Loading. |
| Restauração verifica Idle apenas antes da suspensão. | Invalidar restauração se pesquisa começar e conferir antes de publicar. |
| Testes exigem atualização/fallback de cache vencido. | Substituir por retorno local permanente, recência e falhas sem escrita. |
| README descreve TTL e fallback. | Atualizar depois de implementar e validar. |

## 2. Modelos de dados

### 2.1 Entrada e entidade de domínio

A consulta é disparada somente pelo botão, nunca ao completar oito dígitos. A UI entrega a entrada após a máscara. O caso de uso valida exatamente oito dígitos após normalização. Manter `CepFormatter`; não introduzir outra regra de colagem ou converter CEP em número.

| Campo em Address | DTO ViaCEP | Coluna Room | Tipo/nulabilidade e apresentação | Origem |
| --- | --- | --- | --- | --- |
| `zipCode` | `cep` | `zip_code` | String não nula; oito dígitos; máscara na exibição | RF-001, RF-002, RF-005, RF-008; RN-001 |
| `street` | `logradouro` | `street` | String não nula; vazio permitido | RF-008, RF-012; RN-009 |
| `complement` | `complemento` | `complement` | String não nula; vazio permitido | RF-008, RF-012; RN-009 |
| `neighborhood` | `bairro` | `neighborhood` | String não nula; vazio permitido | RF-008, RF-012; RN-009 |
| `city` | `localidade` | `city` | String não nula; vazio permitido | RF-008, RF-012; RN-009 |
| `stateAbbreviation` | `uf` | `state_abbreviation` | String não nula; vazio permitido | RF-008, RF-012; RN-009 |
| `state` | `estado` | `state` | String não nula; vazio permitido | RF-008, RF-012; RN-009 |

No DTO, os campos textuais já são nullable com padrão null; `hasError: Boolean?`, mapeado de `erro`, sinaliza inexistência. Preservar o mapeamento atual: `erro == true` ou CEP ausente/vazio retornam ausência; campos complementares nulos tornam-se string vazia. Não acrescentar validação remota nova nesta iteração. Na UI, `ifBlank` aplica “Não informado”; não persistir esse texto substituto.

Address permanece com os sete campos existentes, obrigatórios no construtor, sem campos de cache ou recência. Não há relacionamentos com outras entidades.

### 2.2 Metadados de AddressEntity

| Propriedade / coluna | Tipo e padrão | Finalidade / requisito |
| --- | --- | --- |
| `id` / `id` | Long, PK autogerada; Kotlin 0 para nova entidade | Identidade interna preservada na migração; RF-005 |
| `savedAtEpochMillis` / `saved_at_epoch_millis` | Long não nulo; legado pode ser 0 | Metadado de salvamento preservado; premissa de compatibilidade |
| `lastConsultationOrder` / `last_consultation_order` | Long não nulo; Kotlin 0 e `@ColumnInfo(defaultValue = "0")` | Sequência de última consulta; RF-006, RF-010, RF-011; RN-006 |

Tabela `addresses`; manter índice único existente `index_addresses_zip_code` sobre `zip_code`. Os campos textuais existentes são NOT NULL, sem novos defaults. O campo de recência tem default compatível entre criação nova e migração; gravações normais sempre atribuem sequência positiva. Ajustar o mapeador `toEntity` para receber timestamp e sequência. `toDomain` não expõe metadados.

Recência será um ordinal, não uma data: próximo valor = máximo persistido + 1, ou 1 quando vazio. Isso evita empates de milissegundos e dependência de mudanças de relógio. `saved_at_epoch_millis` não participa da ordenação após a migração e não muda em cache hit. Em novo salvamento, obter o timestamp após o retorno remoto, junto à preparação da escrita.

Ordenação única de referência:

```sql
SELECT * FROM addresses
ORDER BY last_consultation_order DESC, id DESC;
```

O desempate por id torna a consulta determinística; a sequência calculada normalmente já distingue os sucessos. Não há tabela adicional de histórico ou contador. O cálculo e a escrita ocorrem na mesma transação para atomicidade, sem mecanismo adicional de consultas simultâneas.

### 2.3 Migração 3→4

1. Acrescentar `last_consultation_order INTEGER NOT NULL DEFAULT 0`.
2. Ler os ids por `saved_at_epoch_millis ASC, id ASC` e materializar essa lista antes de atualizar os registros.
3. Para cada id, atribuir a posição crescente, iniciando em 1, com parâmetros SQL vinculados.
4. Manter os demais campos e o índice único intactos.
5. Registrar MIGRATION_3_4 junto às migrações anteriores, com versão final 4.

A ordenação decrescente por sequência reproduz a ordenação legada decrescente por timestamp/id. Banco vazio continua vazio; timestamps zero ou empatados usam id. Não interpretar os valores atribuídos como datas de consultas recuperadas. A consulta mais recente anterior à mudança não pode ser reconstruída se não foi registrada.

Não usar funções SQL de janela dependentes de SQLite mais recente que o suportado pelo minSdk. Não apagar/recriar o banco para contornar incompatibilidade nem usar fallback destrutivo. As migrações 1→2 e 2→3 existentes continuam compondo os caminhos completos.

## 3. Componentes e módulos

Todos os caminhos desta seção são relativos à raiz do repositório. Prefixo de fontes principais: `app/src/main/java/com/example/cepapplication/`.

| Arquivo sob o prefixo | Responsabilidade planejada | RF/RN |
| --- | --- | --- |
| `domain/model/Address.kt` | Preservar entidade pura e campos | RF-008, RF-012 |
| `domain/util/CepFormatter.kt` | Preservar normalização, validação e máscara | RF-001, RF-002; RN-001, RN-002 |
| `domain/repository/CepRepository.kt` | Preservar contrato no domínio | RF-003–RF-006, RF-011 |
| `domain/usecase/GetAddressByCepUseCase.kt` | Validar antes dos dados; resultado tipado por exceção; cancelamento | RF-002, RF-009 |
| `domain/usecase/GetSavedAddressesUseCase.kt` | Leitura reativa sem escrita | RF-010, RF-011; RN-006 |
| `data/local/AddressEntity.kt` | Novo metadado e mapeamento | RF-005, RF-006 |
| `data/local/AddressDatabase.kt` | Versão 4 e migração | RF-005, RF-006 e compatibilidade |
| `data/local/AddressDao.kt` | Transações de hit/insert e ordenação | RF-003, RF-005, RF-006, RF-010, RF-011 |
| `data/local/CepLocalDataSource.kt` | Contratos locais sem CachedAddress/TTL | RF-003–RF-006 |
| `data/local/RoomCepLocalDataSource.kt` | Adaptar DAO, mapear dados e encapsular falhas locais | RF-003–RF-006, RF-009 |
| `data/local/LocalStorageException.kt` (novo) | Distinguir falha local de conexão remota, preservando causa | RF-009; RN-004 |
| `data/repository/CepRepositoryImpl.kt` | Local primeiro; remoto e save somente após miss | RN-003–RN-007, RN-011 |
| `data/remote/CepRemoteDataSource.kt` e `RetrofitCepRemoteDataSource.kt` | Preservar fonte suspensa e adaptador ViaCEP | RF-004, RF-012 |
| `data/ViaCepApi.kt` e `data/ViaCepService.kt` | Preservar endpoint, DTO e Retrofit | RF-004, RF-012 |
| `ui/CepUiState.kt` e `ui/CepViewModel.kt` | Estado de tela, conclusão e restauração | RF-007–RF-010; RN-008 |
| `SearchFragment.kt` | Eventos, máscara, renderização e feedback | RF-001, RF-007–RF-009, RF-012 |
| `SavedAddressesFragment.kt` | Lista, vazio confirmado e observação com ciclo de vida | RF-011 |
| `MainActivity.kt` | Preservar navegação e ViewModel compartilhado | RF-010, RF-011 |
| `AppContainer.kt` e `CepApplication.kt` | Composição manual; ajuste mínimo para contratos/testes | Suporte à integração |
| `AddressFormatting.kt` | Preservar campos, máscara e “Não informado” | RF-008, RF-012 |

Preservar `app/src/main/res/layout/fragment_search.xml`, `fragment_saved_addresses.xml`, `activity_main.xml`, navegação em `app/src/main/res/navigation/main_navigation.xml` e textos em `app/src/main/res/values/strings.xml`. Não planejar alteração visual. Atualizar README somente ao concluir a implementação; manter relatório histórico intacto.

## 4. Interfaces

### 4.1 Contratos de domínio e dados

Assinaturas de referência:

```kotlin
// Domínio: contratos existentes preservados
suspend fun CepRepository.getAddress(zipCode: String): Address?
fun CepRepository.observeSavedAddresses(): Flow<List<Address>>
suspend operator fun GetAddressByCepUseCase.invoke(rawZipCode: String): Result<Address>
operator fun GetSavedAddressesUseCase.invoke(): Flow<List<Address>>

// Fonte local: contratos de destino
suspend fun findAndRecordConsultation(zipCode: String): Address?
suspend fun saveAndRecordConsultation(address: Address, savedAtEpochMillis: Long): Address
fun observeAll(): Flow<List<Address>>

// Fonte remota: preservar
suspend fun findByZipCode(zipCode: String): Address?
```

As primeiras assinaturas descrevem membros existentes, não instruem convertê-los em funções de extensão.

| Interface | Entrada e validação | Saída/efeitos | Falhas |
| --- | --- | --- | --- |
| Caso de uso de consulta | Texto efetivamente submetido; exatamente oito dígitos após normalização | Result com Address; sem dados para entrada inválida | InvalidCepException, CepNotFoundException ou causa de falha; CancellationException é lançada |
| Repositório de consulta | CEP normalizado pelo caso de uso | Address após escrita confirmada; null se remoto não encontrou | Propaga falhas locais/remotas e cancelamento |
| Consulta local transacional | CEP normalizado | Hit: atualiza só recência e retorna Address; miss: null, sem escrita | LocalStorageException; cancelamento propagado |
| Salvamento local transacional | Address e timestamp de salvamento | Insere endereço/recência juntos e retorna Address após commit | LocalStorageException; conflito de unicidade falha sem REPLACE; cancelamento propagado |
| Observação local/repositório/caso de uso | Sem entrada | Lista ordenada reativa; nenhuma escrita | Falha de leitura identificada, nunca convertida em ausência confirmada |
| Fonte remota | CEP normalizado | DTO convertido em Address ou null | HTTP, conexão, timeout, desserialização e cancelamento |

Endpoint existente: base `https://viacep.com.br/`, GET `ws/{cep}/json/`, conversor Gson. Este contrato foi lido do código do projeto; não houve nova consulta ao serviço para elaborar o Blueprint.

### 4.2 Interface de apresentação

`search` recebe a entrada atual e invoca o caso de uso. `loadLatestAddress` lê a lista ordenada para restauração. Atualização de entrada é um evento explícito encaminhado pelo Fragment ao ViewModel. Leitores externos não recebem MutableStateFlow.

O estado funcional permanece CepUiState com Idle/Loading/Success/Error. Para agrupar os dados renderizáveis de forma consistente, adotar um estado de tela `CepScreenState` no arquivo de estado existente, contendo `status: CepUiState`, `input: String`, `lastSuccessfulAddress: Address?` e feedback pendente opcional. Esse agrupamento é interno, não uma nova tela ou um novo estado de negócio. A UI consome um StateFlow desse conjunto; adaptar os testes/consumidores existentes ao contrato final.

Feedback pendente contém id de evento e tipo/cause necessário à apresentação. O Fragment confirma consumo pelo id após exibir o Toast. Feedback de sucesso é gerado só pela conclusão de consulta, nunca por restauração. Erros de campo são derivados do estado/causa e podem ser renderizados novamente. Essa separação impede repetir Toast ao recolher o mesmo estado e não depende de observar Loading. Não é exigida entrega exatamente uma vez através de encerramento do processo.

Para a lista, manter os últimos registros recebidos e distinguir “ainda não carregou”, leitura concluída e falha como metadados internos de leitura. Só uma emissão vazia bem-sucedida confirma a mensagem de vazio. Em falha, manter a lista anterior e apresentar o erro genérico existente. Nova abertura pode iniciar nova leitura local; não criar polling ou retentativa periódica. Isso não muda os quatro estados funcionais de consulta nem a recência.

### 4.3 Mensagens e canais existentes

| Situação | Recurso | Texto / canal |
| --- | --- | --- |
| Entrada inválida | `error_invalid_zip_code` | “Digite um CEP válido” no campo |
| Inexistência | `error_zip_code_not_found` | “CEP não encontrado” no campo |
| Conexão/timeout remoto | `error_network` | “Não foi possível consultar o CEP. Verifique sua conexão e tente novamente.” em Toast |
| HTTP, armazenamento ou outra falha | `error_unexpected` | “Não foi possível concluir a consulta. Tente novamente.” em Toast |
| Consulta concluída | `toast_zip_code_saved` | “Endereço disponível.” em Toast |
| Lista vazia confirmada | `no_stored_zip_codes` | “Nenhum CEP armazenado.” |
| Campo textual vazio | `not_informed` | “Não informado” |

Conservar os títulos e o CEP do endereço anterior visível após falha; não renderizá-lo com o CEP solicitado que falhou. Não adicionar texto de origem local/idade. LocalStorageException não deve herdar de IOException, para não cair no mapeamento de conexão existente. Encapsular causas de armazenamento, preservando CancellationException antes desse tratamento.

## 5. Gestão de estado

### 5.1 Estados e transições

| Estado/evento | Próximo estado | Entrada / resultado / persistência |
| --- | --- | --- |
| Idle, abertura sem registros | Idle | Sem endereço; nenhuma escrita |
| Idle, restauração válida com registro | Success | Exibir último; não limpar entrada, notificar sucesso ou alterar recência |
| Idle/Success/Error, consulta inválida | Error | Preservar entrada e endereço anterior; zero acesso a dados para a consulta |
| Idle/Success/Error, consulta válida | Loading | Preservar entrada/endereço; progresso e controles bloqueados |
| Loading, hit com recência confirmada | Success | Exibir endereço, limpar entrada, liberar controles, feedback de conclusão |
| Loading, miss remoto com salvamento confirmado | Success | Mesmo efeito visual; endereço/recência persistidos juntos |
| Loading, ausência ou falha | Error | Manter entrada/endereço anterior; liberar controles; histórico sem alteração por essa falha |
| Estado estável, abrir lista | Sem alteração da consulta | Observar localmente, sem atualizar recência |
| Pesquisa iniciada, restauração pendente conclui | Preservar estado da pesquisa | Descartar publicação da restauração atrasada |

Uma emissão transitória de Loading antes da validação é permitida pelo FRD, mas não autoriza acesso a dados. A renderização e limpeza devem continuar corretas caso StateFlow omita esse estado intermediário em consulta rápida.

### 5.2 Restauração, ciclo de vida e cancelamento

Manter referência ao Job de restauração e um marcador de invalidação específico dessa leitura. Iniciar restauração apenas na condição inicial; antes de pesquisar, invalidar/cancelar a leitura pendente. Antes de publicar uma restauração, conferir a condição inicial e a validade do marcador. Mesmo uma pesquisa inválida iniciada posteriormente impede que a restauração troque seu Error por Success.

Essa proteção aplica-se exclusivamente à leitura automática de abertura. Não acrescentar mutex, fila, Job guard, contador de requisições ou política de “última pesquisa vence” para pesquisas simultâneas.

Coletar estados com `viewLifecycleOwner` e `repeatOnLifecycle`. O último endereço e a entrada vivem no ViewModel compartilhado; a View pode ser reconstruída sem depender de texto remanescente no widget. Restauração após encerramento do app é feita a partir do banco; não se exige persistir erro ou entrada não consultada entre sessões.

CancellationException deve atravessar fonte local, remoto e caso de uso sem virar falha de negócio. Não suprimir cancelamento para forçar gravação. Ao cancelar antes do commit, a transação não deixa dados parciais. Commit já confirmado é o marco de sucesso da camada de dados; cancelamento posterior não apaga uma consulta já persistida. Ao encerrar o ViewModel, não emitir Toast de falha nem criar novo estado de negócio.

## 6. Lógica de negócio

### 6.1 Validação — RF-001/RF-002; RN-001/RN-002

```text
UI aplica máscara existente: remover não numéricos, take(8), inserir hífen
botão submete o valor efetivo
caso de uso:
    se CepFormatter.isValid(entrada) for falso:
        retornar failure(InvalidCepException), sem repositório
    normalizado = CepFormatter.normalize(entrada)
    tentar repository.getAddress(normalizado)
    null -> failure(CepNotFoundException)
    endereço -> success(endereço)
    CancellationException -> lançar novamente
    outra falha -> failure(causa)
```

Não truncar silenciosamente uma entrada direta do caso de uso com mais de oito dígitos; o truncamento é o comportamento da máscara da UI. Não mudar as regras atuais de CepFormatter nesta tarefa.

### 6.2 Consulta coordenada — RN-003/RN-004/RN-005/RN-011

```text
local = localSource.findAndRecordConsultation(cep)
se local != null:
    retornar local

remoto = remoteSource.findByZipCode(cep)
se remoto == null:
    retornar null

retornar localSource.saveAndRecordConsultation(remoto, currentTimeMillis())
```

Não há tratamento de idade, catch de IOException para fallback, mutex ou releitura do CEP antes de inserir. Exceção local interrompe o fluxo; somente null confirma miss. A UI recebe sucesso remoto apenas após armazenamento.

### 6.3 Hit e recência — RN-006/RN-007

```text
transação Room:
    entity = buscar por zip_code
    se ausente: retornar null sem escrita
    próxima = (MAX(last_consultation_order) ou 0) + 1
    atualizar last_consultation_order do id encontrado
    retornar entity.toDomain()
```

Os dados do endereço, id e timestamp de salvamento não mudam. A transação deve falhar integralmente se a atualização falhar. O cálculo de MAX não é uma segunda busca defensiva do CEP; serve somente para atribuir recência.

### 6.4 Novo salvamento — RN-001/RN-005/RN-007

```text
transação Room:
    próxima = (MAX(last_consultation_order) ou 0) + 1
    construir entidade com endereço, timestamp de salvamento e próxima
    inserir com conflito ABORT, preservando índice único
    retornar endereço
```

Não fazer REPLACE, upsert ou findByZipCode adicional para tratar inserções concorrentes. A unicidade permanece uma garantia de banco; falha de inserção propaga erro, sem sucesso parcial. Hit e miss não compartilham uma transação aberta durante rede.

### 6.5 Leituras e apresentação — RN-006/RN-008/RN-009/RN-010

```text
lista = observar DAO ordenado por sequência DESC e id DESC
restauração = primeiro item da primeira emissão, se existir e ainda for válida
não gravar em nenhuma dessas leituras

ao iniciar consulta: preservar último endereço
ao concluir com sucesso: trocar último endereço, limpar entrada, feedback
ao falhar: preservar último endereço e entrada, apresentar causa
para cada campo textual em branco: exibir "Não informado"
CEP apresentado é sempre o do endereço renderizado
```

## 7. Tarefas de implementação

As tarefas abaixo preservam o desenho e os critérios aprovados; o estado atual é registrado na seção 11.2. Caminhos reais existentes e caminhos de novos artefatos estão explicitados. Em cada tarefa, executar os testes específicos alterados; a T-011 consolida os comandos completos. Não adicionar testes de consultas simultâneas.

### Fase 1 — Persistência e recência

#### T-001 — Evoluir schema Room e preservar dados

- **O que:** implementar o schema da seção 2 e migração 3→4; preservar migrações anteriores, dados, ids e índice. Materializar ids antes de atribuir ordinais. Manter defaults compatíveis com criação nova. Expor migrações como internal para uso dos testes, sem copiar a lógica.
- **Onde:** `app/src/main/java/com/example/cepapplication/data/local/AddressEntity.kt` e `AddressDatabase.kt`; novo `app/src/test/java/com/example/cepapplication/data/local/AddressDatabaseMigrationTest.kt`.
- **Dependências:** nenhuma.
- **Aceite:** instalação nova e bancos 1, 2 e 3 abrem em v4; conteúdo/id/índice preservados; ordem legada reproduzida; zero timestamps suportados. RF-005, RF-006, RF-010, RF-011; compatibilidade.
- **Unitários:** não substituem validação de schema; nenhum teste artificial de SQL em string.
- **Integração:** fixtures SQLite por versão, bancos vazios/povoados, datas iguais/zero/distintas; abrir com Room real e verificar conteúdo, versão e unicidade. Bancos de teste isolados.
- **E2E:** preservação após atualização compõe a validação integrada da T-010; migração é verificada por testes locais com Room real sob Robolectric.
- **Complexidade:** alta.

#### T-002 — Operações locais atômicas e ordenação

- **O que:** implementar transações das seções 6.3/6.4, novos contratos locais e ordenação comum; contratos antigos foram removidos na integração com T-003. Inserção ABORT; hit altera só recência. Não incluir releitura defensiva.
- **Onde:** `app/src/main/java/com/example/cepapplication/data/local/AddressDao.kt`, `CepLocalDataSource.kt`, `RoomCepLocalDataSource.kt` e mapeadores de `AddressEntity.kt`; `app/src/test/java/com/example/cepapplication/data/local/RoomPersistenceTest.kt` (Room real) e `RoomCepLocalDataSourceTest.kt` (encapsulamento de falhas).
- **Dependências:** T-001.
- **Aceite:** A/B/reconsulta de A resulta em dois registros e A primeiro; miss não escreve; observação não escreve; falha não deixa endereço/recência parcial. RF-003, RF-005, RF-006, RF-010, RF-011; RN-003, RN-006, RN-007.
- **Unitários:** mapeamento de endereço sem exposição de metadados, se alterado de forma relevante.
- **Integração:** Room real, primeira sequência, incremento, reabertura, unicidade, preservação dos demais campos e emissões ordenadas. Para falha de escrita, usar mecanismo restrito ao banco de teste, como trigger de aborto, e conferir rollback. Não alterar produção para simular falha.
- **E2E:** cenários de histórico e falhas na T-010.
- **Complexidade:** alta.

#### T-003 — Remover TTL/fallback e integrar local primeiro

- **O que:** implementar seção 6.2; remover cacheTtlMillis, constante de TTL, cálculo de idade, fallback, CachedAddress e assinaturas locais antigas. Ajustar composição e doubles. Preservar relógio apenas para timestamp de novo salvamento.
- **Onde:** `app/src/main/java/com/example/cepapplication/data/repository/CepRepositoryImpl.kt`, `AppContainer.kt`, contratos locais; `app/src/test/java/com/example/cepapplication/data/repository/CepRepositoryImplTest.kt`.
- **Dependências:** T-002.
- **Aceite:** hit de qualquer idade chama remoto zero vezes; miss encontrado salva antes de retornar; null remoto não salva; falha local não chama API. RF-003–RF-006; RN-003–RN-007, RN-011.
- **Unitários:** substituir testes de atualização por TTL e fallback vencido; hit antigo/zero/futuro, miss, inexistência, erros de leitura/recência/save. Doubles devem distinguir leitura, escrita de recência e salvamento.
- **Integração:** integração completa com Room na T-010; contratos de persistência cobertos em T-002.
- **E2E:** T-010 comprova UI→persistência.
- **Complexidade:** média.

### Fase 2 — Erros e estado

#### T-004 — Distinguir falhas locais, remotas e cancelamento

- **O que:** encapsular falhas locais em LocalStorageException preservando causa e cancelamento; manter validação e erros existentes no caso de uso. Mapear armazenamento/HTTP para mensagem genérica e conexão remota para mensagem de rede.
- **Onde:** novo `app/src/main/java/com/example/cepapplication/data/local/LocalStorageException.kt`, fonte local, `domain/usecase/GetAddressByCepUseCase.kt`, `SearchFragment.kt`; testes existentes `app/src/test/java/com/example/cepapplication/domain/usecase/GetAddressByCepUseCaseTest.kt` e de repositório.
- **Dependências:** T-003.
- **Aceite:** inválido não acessa repositório; IOException local não aparece como rede; HTTP/armazenamento não produzem sucesso; CancellationException atravessa as camadas. RF-002, RF-009; RN-004, RN-008, RN-011; EC-013.
- **Unitários:** tipos de erro, invalidação sem dados, inexistência, timeout remoto, HTTP e cancelamento nas três operações suspensas.
- **Integração:** falha real de escrita e encapsulamento na fonte Room; adaptador remoto em T-009.
- **E2E:** mensagens e preservação na T-007/T-010.
- **Complexidade:** média.

#### T-005 — Estado renderizável com entrada e resultado anterior

- **O que:** implementar CepScreenState da seção 4.2 mantendo CepUiState funcional; publicar estado consistente, limpar entrada no sucesso e identificar feedback de conclusão. Não adicionar guarda de pesquisa simultânea.
- **Onde:** `app/src/main/java/com/example/cepapplication/ui/CepUiState.kt`, `CepViewModel.kt`; `app/src/test/java/com/example/cepapplication/ui/CepViewModelTest.kt`.
- **Dependências:** T-004.
- **Aceite:** sucesso limpa entrada; erro preserva entrada/endereço; Loading mantém endereço anterior; sucesso rápido independe de emissão intermediária observada. RF-007–RF-009; RN-008.
- **Unitários:** sucesso local imediato e remoto suspenso, erro inicial, sucesso A/falha B, nova tentativa, feedback consumido e nova coleta.
- **Integração:** reconstrução da View usa o mesmo estado, verificada em T-007.
- **E2E:** T-010.
- **Complexidade:** média.

#### T-006 — Restauração e observação sem efeitos no histórico

- **O que:** implementar proteção específica da restauração da seção 5.2, leitura do primeiro endereço e metadados de leitura da lista. Preservar dados em erro; não converter erro em lista vazia; permitir nova leitura ao reabrir sem retentativa periódica.
- **Onde:** `app/src/main/java/com/example/cepapplication/ui/CepViewModel.kt`, `ui/CepUiState.kt` se necessário ao modelo de leitura; `domain/usecase/GetSavedAddressesUseCase.kt` (preservar contrato); testes de ViewModel.
- **Dependências:** T-005.
- **Aceite:** restauração usa recência atual; não escreve/limpa entrada/emite sucesso; resultado atrasado não substitui pesquisa, mesmo inválida ou com erro; lista preservada se leitura falhar. RF-009–RF-011; RN-006; CA-11.
- **Unitários:** flows controlados para ausência, restauração normal, erro de leitura, pesquisa antes da conclusão e reabertura. Conferir ausência de chamadas de escrita/rede.
- **Integração:** recência persistente está em T-002; usar essa ordem sem transformação adicional.
- **E2E:** reabertura e restauração atrasada em T-010.
- **Complexidade:** média.

### Fase 3 — Telas existentes

#### T-007 — Integrar estado à tela de pesquisa

- **O que:** encaminhar entrada ao ViewModel sem loop de máscara; renderizar endereço anterior em Loading/Error; manter controles/progresso; substituir wasLoading por estado de entrada e feedback consumível. Preservar textos/formatadores.
- **Onde:** `app/src/main/java/com/example/cepapplication/SearchFragment.kt`, `AddressFormatting.kt`; recursos existentes em `app/src/main/res/values/strings.xml`; novo `app/src/test/java/com/example/cepapplication/ConsultaCepUiTest.kt`.
- **Dependências:** T-005, T-006, T-009 (infraestrutura para concluir os testes de tela).
- **Aceite:** máscara/truncamento/zeros preservados; sucesso rápido limpa; falha conserva A identificado como A; controles seguem Loading; cada campo vazio mostra “Não informado”; feedback não repete na restauração. RF-001, RF-007–RF-009, RF-012; RN-002, RN-008–RN-010.
- **Unitários:** preservar/ampliar casos pertinentes de `app/src/test/java/com/example/cepapplication/ZipCodeFormatterTest.kt`; não testar novamente a implementação literal da máscara sem cenário funcional.
- **Integração/UI:** coleta com ciclo de vida, destruição/recriação da View com ViewModel vivo, feedback, erro de campo e Toast.
- **E2E:** fluxos correspondentes em T-010.
- **Complexidade:** média.

#### T-008 — Lista reativa e navegação sem escrita

- **O que:** apresentar ordem recebida e vazio confirmado; manter dados anteriores diante de falha de leitura; preservar navegação/coleta e o fluxo de apresentação existente.
- **Onde:** `app/src/main/java/com/example/cepapplication/SavedAddressesFragment.kt` e integração com ViewModel; novo `app/src/test/java/com/example/cepapplication/ConsultaCepUiTest.kt`.
- **Dependências:** T-006, T-007, T-009.
- **Aceite:** A reconsultado aparece primeiro, lista reage às mudanças locais e abrir/fechar não altera ordem; mensagem de vazio somente após leitura vazia confirmada. RF-010, RF-011; RN-006, RN-007.
- **Unitários:** sem nova lógica de ordenação na UI; já testada em T-002/T-006.
- **Integração/UI:** vazio, lista com A/B, reconsulta, erro de leitura e navegação.
- **E2E:** T-010 verifica ausência de chamadas remotas/escritas pela navegação.
- **Complexidade:** baixa.

### Fase 4 — Validação integrada

#### T-009 — Infraestrutura determinística de testes

- **O que:** disponibilizar substituições mínimas na composição manual para banco de teste e fonte remota controlada. Não introduzir framework de DI ou configuração de teste na UI do produto. Usar Room real isolado, A/B e cenários de erro. Prover atrasos controlados por coroutine, sem sleeps frágeis.
- **Onde:** `app/src/main/java/com/example/cepapplication/AppContainer.kt`, `CepApplication.kt` somente no necessário à composição; helpers novos em `app/src/test/java/com/example/cepapplication/testing/`; `app/build.gradle.kts` somente se dependência de teste for indispensável. Testes do adaptador em `app/src/test/java/com/example/cepapplication/data/remote/RetrofitCepRemoteDataSourceTest.kt` (novo), além de `data/ViaCepResponseTest.kt` existente.
- **Dependências:** T-004. Pode ser executada antes de T-007/T-008 para suportar seus testes; não depende das telas ajustadas.
- **Aceite:** sem ViaCEP pública obrigatória, sem modificar banco de uso do app, contagem de rede verificável e dados isolados entre testes. Exercitar adaptador real com ViaCepApi substituível para sucesso/ausência/IOException/HttpException; manter testes JSON/DTO para conversão.
- **Unitários:** adaptador, DTO parcial/inexistente e doubles com comportamento relevante.
- **Integração:** composição com Room real; criar/fechar e remover somente arquivos exclusivos dos testes. Usar Robolectric/JUnit existentes, sem aparelho, emulador, ADB ou testes instrumentados.
- **E2E:** infraestrutura usada em T-007, T-008 e T-010. Uma API controlada não comprova disponibilidade da ViaCEP pública nem transporte HTTP real; registrar essa limitação.
- **Complexidade:** média.

#### T-010 — Cenários completos de aceite

- **O que:** percorrer UI→ViewModel→casos de uso→repositório→Room com remoto controlado, verificando apresentação, chamadas e estado persistido.
- **Onde:** novo `app/src/test/java/com/example/cepapplication/ConsultaCepFlowTest.kt` e helpers de T-009.
- **Dependências:** T-007, T-008, T-009.
- **Aceite:** todos os CA-01–CA-11 da seção 8.3, mais falha de leitura/gravação e dados parciais, com asserts de banco e rede. Não basta conferir texto de tela.
- **Unitários:** reutilizar evidências das tarefas de negócio, sem duplicar sua implementação nos testes.
- **Integração:** reabrir banco persistido com nova instância de repositório/ViewModel para recência entre sessões; conferir endereços migrados com consultas locais sem API.
- **E2E:** entrada inválida; miss encontrado; hit antigo/offline; A/B/A sem duplicação; falha de rede/HTTP 500 para B com A visível; inexistência; vazios; Loading; abertura de lista/restauração sem escrita; restauração atrasada; falha ao salvar/registrar recência; nova tentativa. Distinguir recriação da View de nova sessão nos testes.
- **Complexidade:** alta.

#### T-011 — Verificações finais e evidências

- **O que:** executar comandos completos, corrigir falhas do escopo e produzir relatório com resultados reais, ambiente e limitações.
- **Onde:** Gradle Wrapper; novo `docs/consulta-cep/validacao-consulta-cep.md`.
- **Dependências:** T-010.
- **Aceite:** testes unitários e build aprovados. Toda cobertura automatizada deve executar localmente; não usar aparelho, emulador, ADB ou testes instrumentados. Não tratar problema histórico de loopback como resultado atual.
- **Unitários:** executar `testDebugUnitTest` pelo Wrapper, conforme os comandos PowerShell abaixo.
- **Integração/E2E:** cobrir os cenários por testes automatizados que possam ser executados sem aparelho/emulador. Registrar as classes e cenários efetivamente executados.
- **Build/documentação:** `assembleDebug` e `git diff --check`. Reexecutar após correção somente verificações pertinentes e a consolidação necessária.
- **Complexidade:** média.

Comandos previstos no PowerShell:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
git diff --check
```

O relatório deverá conter data, revisão/estado testado, versões efetivas de execução, comandos, resultados, cenários cobertos e pendências. Não incluir credenciais ou conteúdo sensível do ambiente.

### Fase 5 — Documentação e entrega

#### T-012 — Atualizar documentação ativa

- **O que:** substituir TTL/fallback no README por comportamento real, explicar recência local/remota e migração legada, atualizar status de implementação com evidências. Preservar histórico e decisões aprovadas; nenhuma nova regra funcional.
- **Onde:** `README.md`, `docs/consulta-cep/frd-consulta-cep.md`, este Blueprint e relatório de T-011; conferir coerência com `AGENTS.md` sem reabrir a decisão de simultaneidade. Não reescrever `hardening-consulta-cep.md` como validação atual.
- **Dependências:** T-011.
- **Aceite:** documentação distingue implementado, testado e pendente; não declara build aprovado por badge preexistente. RF-003, RF-006, RF-010, RF-011 e diferenças de implementação do FRD.
- **Unitários/integração/E2E:** não se aplicam a alteração apenas documental; citar evidências de T-011.
- **Verificação:** consistência, caminhos e `git diff --check`; sem novo build para edição exclusivamente documental.
- **Complexidade:** baixa.

### 7.1 Sequenciamento e dependências

```text
T-001 → T-002 → T-003 → T-004
T-004 → T-005 → T-006
T-004 → T-009
T-005 + T-006 + T-009 → T-007
T-006 + T-007 + T-009 → T-008
T-007 + T-008 + T-009 → T-010 → T-011 → T-012
```

Ordem linear recomendada, para disponibilizar os helpers antes dos testes de tela: T-001, T-002, T-003, T-004, T-009, T-005, T-006, T-007, T-008, T-010, T-011, T-012. As dependências de T-007/T-008 incluem T-009 para permitir concluir seus testes. Dependências representam ordem de trabalho, não autorização para agentes paralelos.

## 8. Matriz de rastreabilidade e autovalidação

### 8.1 Requisitos e regras → tarefas

| ID | Tarefas | Evidência esperada |
| --- | --- | --- |
| RF-001 | T-007, T-010 | Máscara/truncamento/colagem preservados |
| RF-002 | T-004, T-007, T-010 | Inválido sem repositório e sem dados |
| RF-003 | T-002, T-003, T-010 | Hit de qualquer idade, remoto zero |
| RF-004 | T-003, T-004, T-009, T-010 | Miss com chamada remota e save confirmado |
| RF-005 | T-001, T-002, T-003, T-010 | Um registro por CEP entre sessões |
| RF-006 | T-001, T-002, T-003, T-010 | A/B/A muda topo, falhas não mudam |
| RF-007 | T-005, T-007, T-010 | Progresso e controles até conclusão |
| RF-008 | T-005, T-007, T-010 | Sete campos e entrada limpa |
| RF-009 | T-004, T-005, T-006, T-007, T-010 | Erro correto, entrada/endereço preservados |
| RF-010 | T-001, T-002, T-006, T-010 | Restauração do último local sem escrita |
| RF-011 | T-002, T-006, T-008, T-010 | Lista ordenada reativa e vazio confirmado |
| RF-012 | T-007, T-009, T-010 | Sucesso parcial com “Não informado” |
| RN-001 | T-001, T-002, T-004, T-007 | Texto normalizado, índice único, zeros |
| RN-002 | T-004, T-007, T-010 | Colagem normalizada pela UI e validação central |
| RN-003 | T-002, T-003, T-010 | Sem TTL, atualização ou exclusão por idade |
| RN-004 | T-003, T-004, T-010 | Erro local não chama remoto |
| RN-005 | T-002, T-003, T-010 | Sucesso somente após persistência |
| RN-006 | T-002, T-006, T-008, T-010 | Consulta altera recência; leituras não |
| RN-007 | T-002, T-003, T-008, T-010 | Reconsulta mantém cardinalidade e move registro |
| RN-008 | T-004, T-005, T-007, T-010 | A continua identificado como A após erro de B |
| RN-009 | T-007, T-009, T-010 | Dados complementares vazios aceitos |
| RN-010 | T-007, T-008, T-010 | Nenhum aviso de cache/idade/origem |
| RN-011 | T-003, T-004, T-009, T-010 | HTTP 500 no miss; hit sem chamada |
| Decisão de simultaneidade | T-002, T-003, T-005, T-010 | Nenhuma proteção/teste de consultas simultâneas; RF-007 mantido |
| Compatibilidade legada | T-001, T-002, T-010 | Migração íntegra e ordem legada preservada |
| Documentação vigente | T-011, T-012 | Evidências atuais separadas do histórico |

### 8.2 Casos de borda → tratamento e testes

IDs EC são rótulos de rastreabilidade deste Blueprint, não novas regras do FRD.

| ID / cenário | Tratamento | Tarefas |
| --- | --- | --- |
| EC-001 Vazio/incompleto | Erro de campo; nenhum acesso a dados para consultar | T-004, T-007, T-010 |
| EC-002 Letras/excesso na colagem | Máscara remove/trunca; validar valor resultante | T-007, T-010 |
| EC-003 Zeros iniciais | String em UI/domínio/DTO/banco | T-002, T-004, T-007 |
| EC-004 Antigo/sem data útil | Hit sem idade; migração fornece ordem inicial | T-001, T-002, T-003, T-010 |
| EC-005 Local offline | Zero remoto; recência confirmada | T-003, T-010 |
| EC-006 `erro: true` | Ausência, mensagem de inexistência, sem escrita | T-003, T-004, T-009, T-010 |
| EC-007 Conexão/timeout de CEP novo | Mensagem de rede; preservar resultado/histórico | T-004, T-007, T-009, T-010 |
| EC-008 HTTP 500/outra falha HTTP | Mensagem genérica, sem registro novo | T-003, T-004, T-009, T-010 |
| EC-009 Leitura/gravação local falha | Não converter em miss/sucesso; rollback e mensagem genérica | T-002, T-004, T-006, T-010 |
| EC-010 Complementares vazios | Aceitar e apresentar substituto por campo | T-007, T-009, T-010 |
| EC-011 Reconsulta | Atualizar só recência e manter um registro | T-002, T-003, T-008, T-010 |
| EC-012 Nova tentativa | Botão liberado; entrada preservada ou corrigida | T-005, T-007, T-010 |
| EC-013 Cancelamento | Propagar sem erro de negócio; transação incompleta não deixa escrita parcial | T-002, T-004, T-005 |
| EC-014 Restauração atrasada | Não publicar após início de pesquisa | T-006, T-010 |
| EC-015 Sucesso rápido / recriação da View | Estado mantém entrada/endereço; limpeza independe de observar Loading | T-005, T-007, T-010 |

EC-014 corresponde ao aceite adicional do FRD revisado. EC-015 é verificação técnica dos comportamentos de UI já exigidos, sem novo escopo funcional.

### 8.3 Critérios de aceite completos

| ID | Cenário de aprovação | Tarefas |
| --- | --- | --- |
| CA-01 | CEP inválido após máscara mostra validação sem buscar dados | T-004, T-007, T-010 |
| CA-02 | CEP novo encontrado gera um registro, exibe campos e limpa entrada | T-002, T-003, T-007, T-010 |
| CA-03 | CEP salvo há mais de 24 horas retorna sem ViaCEP | T-003, T-010 |
| CA-04 | B no topo; consultar A localmente mantém dois registros, move A e restaura A após reabrir | T-002, T-006, T-008, T-010 |
| CA-05 | A visível; B novo falha por rede/HTTP 500; mostrar erro, manter A e não salvar B | T-004, T-005, T-007, T-010 |
| CA-06 | CEP inexistente mostra “CEP não encontrado” e não salva | T-003, T-004, T-009, T-010 |
| CA-07 | Campos vazios mostram “Não informado” e consulta conclui | T-007, T-009, T-010 |
| CA-08 | Durante consulta, controles bloqueados/progresso; conclusão libera | T-005, T-007, T-010 |
| CA-09 | Consulta local funciona offline, sem aviso de cache ou expiração | T-003, T-007, T-010 |
| CA-10 | Abrir lista/restaurar não muda ordem | T-006, T-008, T-010 |
| CA-11 | Restauração concluída após pesquisa iniciada não sobrescreve essa pesquisa | T-006, T-010 |

### 8.4 Resultado da autovalidação documental

- Os 12 RFs e 11 RNs possuem tarefas e evidências previstas.
- Os sete campos de endereço, a entrada CEP, os quatro estados funcionais e a recência persistente estão representados.
- Todos os 13 cenários da seção de erros do FRD estão mapeados em EC-001–EC-013; restauração atrasada e riscos de renderização estão explicitados.
- As dependências têm ordem executável sem ciclos; T-009 antecede a validação completa das telas.
- Migração, atomicidade, cancelamento, falha de leitura e preservação de histórico têm tratamento explícito.
- Não há mutex, serialização de consultas, guarda extra contra consultas simultâneas ou releitura defensiva antes do insert no desenho final.
- Nenhuma tarefa funcional depende de acesso à ViaCEP pública para seus testes automatizados.
- Não são necessários seeds volumosos, novas telas, alterações de CI/CD ou infraestrutura de deploy para atender o FRD.
- Esta autovalidação verifica o plano. Não comprova build, execução de testes ou comportamento do aplicativo.

## 9. Riscos e pontos de atenção

| Ponto | Tratamento definido |
| --- | --- |
| Schema Room novo divergir da migração | Default SQL/Kotlin explícito; abrir todos os caminhos com Room nos testes |
| Ausência de schemas exportados históricos | Fixtures por schema legado compatível com migrações existentes; não inventar histórico recuperado |
| Datas legadas não representam consultas reais | Preservar ordem anterior, documentando a limitação |
| Relógio regressar ou consultas terem mesmo timestamp | Recência usa ordinal independente do relógio |
| MAX+1 e rank da migração | Operações locais de custo proporcional aos dados; migração materializa ids. Não adicionar otimização ou infraestrutura sem necessidade observada |
| Falha no update de hit ou insert remoto | Transações e erro local identificado; nenhum sucesso antes de commit |
| Cancelamento próximo ao commit | Não prometer desfazer transação já confirmada; propagar cancelamento e testar rollback antes do commit |
| StateFlow omitir Loading em retorno rápido | Limpeza controlada pelo sucesso no estado, não por observação da transição |
| View recriada perder endereço anterior | Dados renderizáveis no ViewModel; restauração entre sessões pelo Room |
| Restauração atrasada | Invalidação exclusiva dessa leitura e conferência antes de publicar |
| Erro de observação aparecer como vazio | Preservar últimos dados e distinguir leitura vazia confirmada de erro |
| Versões locais e runtime de build | Verificar com Wrapper na implementação; não inferir JDK de execução só de sourceCompatibility |
| Estratégia local de testes | Room, migrações e telas são exercitados por Robolectric/JUnit; registrar limitações de transporte público e hardware |
| Rede controlada nos E2E | Demonstra lógica/integração interna; não certifica disponibilidade pública nem transporte real da ViaCEP |

Não há decisão funcional bloqueante remanescente. Detalhes de mecânica de testes e organização de helpers podem ser ajustados sem alterar contratos, escopo ou resultados obrigatórios. Qualquer impedimento que exija mudança funcional deve ser apresentado ao usuário antes de alterar o escopo.

## 10. Fora do escopo técnico

Todos os requisitos funcionais do FRD revisado estão contemplados; nenhum foi adiado deliberadamente. Permanecem excluídos:

- Pesquisa por nome, cidade, logradouro ou descoberta de CEP por endereço.
- Mudança de provedor/API, autenticação, perfis ou permissões adicionais.
- Novas telas, reformulação visual, novos campos como IBGE/DDD/região.
- TTL, atualização remota de CEP salvo, atualização em segundo plano e avisos de cache/idade/origem.
- Botões de atualizar/excluir/editar/salvar separado, favoritos e sincronização entre dispositivos.
- Histórico com uma linha por tentativa.
- Mutex, serialização no repositório, guardas adicionais para pesquisas simultâneas, segunda leitura defensiva antes de salvar e testes de consultas simultâneas.
- Framework de DI, mudança de stack, atualização geral de dependências, seeds volumosos e novos pipelines de deploy.
- Alterar regras funcionais fora do FRD aprovado.

## 11. Registro desta entrega documental

O Blueprint foi consolidado após revisão da decomposição e das tarefas em conversa. FRD e AGENTS.md já haviam sido ajustados para a decisão sobre consultas simultâneas; essas alterações anteriores foram preservadas.

Verificações desta entrega: revisão de rastreabilidade, estados, modelos, erros, dependências e caminhos; verificação de whitespace com `git diff --check` e conferência explícita do novo arquivo. Nenhum build Android, teste unitário ou instrumentado foi executado para esta edição documental.

### 11.1 Registro da T-012 — Atualização da documentação ativa

A T-012 foi executada em 07/09/2026. O README passou a descrever retorno local sem expiração, recência persistente e funcionamento offline; o FRD passou a distinguir comportamento implementado de validação pendente; e este Blueprint passou a registrar o estado real das tarefas. A T-011 permanece como evidência histórica de tentativa de validação: `testDebugUnitTest` e `assembleDebug` não chegaram à compilação por falta de acesso de leitura ao Android SDK, enquanto `git diff --check` passou. O relatório da T-011 não foi reescrito como se houvesse uma nova execução.

### 11.2 Correções da revisão — 07/09/2026

Após aprovação de R-01–R-07, foram tratados erros da restauração, recuperação da observação ao reabrir e feedback consumível de falha. A guarda do Job de observação evita duplicar coletores de leitura; não protege consultas simultâneas do usuário.

T-001/T-002 têm testes locais de migração v1/v2/v3→v4, reabertura, unicidade, ordem, rollback e cancelamento antes do commit. T-007/T-008/T-010 possuem integração das telas com Room e remoto controlado em ConsultaCepUiTest. ConsultaCepFlowTest aguarda estados finais com prazo real e encerra ViewModels antes de fechar bancos; A/B/A agora reabre o arquivo persistido.

A colagem de letras/excesso revelou truncamento anterior à máscara pelo maxLength XML. Esse limite bruto foi removido: a máscara continua removendo não numéricos e limitando a oito dígitos, conforme RF-001/RN-002. Não há alteração de escopo ou layout visual.

T-011: 80 testes passaram e assembleDebug concluiu na mesma execução. T-012: documentação ativa e relatórios atualizados, preservando as tentativas históricas. A validação usa Robolectric, sem transporte HTTP real ou hardware; não certifica disponibilidade da ViaCEP. Evidências e matriz atual em test-report-consulta-cep.md.
