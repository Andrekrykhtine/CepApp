# FRD — Consulta e armazenamento de CEP

Data: 07/09/2026  
Status: escopo aprovado pelo usuário; documento gerado após brainstorming.  
Implementação: correções da revisão aprovadas e aplicadas na árvore de trabalho. Em 07/09/2026, 80 testes locais passaram e assembleDebug concluiu; ver test-report-consulta-cep.md e o registro posterior de validação.

## 1. Visão geral

O aplicativo permite consultar um endereço pelo CEP, armazenar o resultado no dispositivo e visualizar os endereços consultados posteriormente. Endereços já armazenados são consultados localmente, inclusive sem internet; somente CEPs ainda não armazenados são pesquisados na ViaCEP.

Este FRD consolida o fluxo completo existente e as mudanças aprovadas, preservando as telas de pesquisa e de CEPs armazenados. A expressão “nome digitado” do enunciado original foi esclarecida pelo usuário como **CEP digitado**; pesquisa por nome não faz parte desta funcionalidade.

### Referências e situação atual

- Projeto Android existente, analisado durante o brainstorming.
- Imagem fornecida pelo usuário: “Proposta de Arquitetura”, com UI, domínio, repositório, fonte remota e fonte local. A imagem é referência de alto nível; as decisões explícitas deste FRD detalham seu comportamento.
- [Documentação oficial ViaCEP](https://viacep.com.br/), consultada em 07/09/2026: consulta por CEP de oito dígitos, resposta JSON e sinalização de CEP inexistente pelo campo `erro`.
- `docs/consulta-cep/hardening-consulta-cep.md`: registro histórico da implementação, não comprovação de que as novas decisões já foram aplicadas.

O código analisado utiliza Retrofit, Coroutines, StateFlow e Room. O comportamento vigente **não possui expiração**: um CEP encontrado localmente é retornado sem acesso remoto, e a recuperação do último endereço considera a **última consulta bem-sucedida**, inclusive local. Essas decisões substituem a regra histórica de 24 horas descrita anteriormente no README e discutida inicialmente no brainstorming. A implementação e suas limitações de validação estão registradas em `docs/consulta-cep/validacao-consulta-cep.md`.

## 2. Atores

| Ator | Papel |
| --- | --- |
| Usuário do aplicativo | Digitar CEP, consultar endereço e visualizar endereços armazenados. |
| ViaCEP | Fornecer endereço para um CEP que ainda não exista no armazenamento local. |

Não há autenticação ou diferenciação de permissões neste fluxo.

## 3. Requisitos funcionais

| ID | Comportamento e condição de disparo | Resultado esperado |
| --- | --- | --- |
| RF-001 | Ao digitar ou colar no campo de CEP, aplicar a máscara existente. | Remover caracteres não numéricos, manter até oito dígitos e apresentar a máscara `00000-000` conforme a digitação. |
| RF-002 | Ao acionar “Consultar e salvar”, validar e normalizar a entrada. | Entrada válida segue para consulta; entrada incompleta recebe “Digite um CEP válido”, sem acesso ao banco ou à API para a consulta. |
| RF-003 | Ao consultar um CEP válido, procurar primeiro no armazenamento local. | Se encontrado, exibir o endereço salvo, independentemente de sua idade e sem chamada à API. |
| RF-004 | Se o CEP válido não estiver armazenado, consultar a ViaCEP. | Se encontrado, obter os dados do endereço e salvá-los automaticamente; falhas seguem a seção 7. |
| RF-005 | Ao concluir uma consulta bem-sucedida, preservar um único registro por CEP normalizado. | O endereço permanece disponível entre sessões, sem duplicação por máscara ou consultas repetidas. |
| RF-006 | Em toda consulta bem-sucedida, inclusive atendida localmente, registrar sua recência. | O CEP passa a ser o último consultado e ocupa o início da lista armazenada. Falhas não alteram essa recência. |
| RF-007 | Durante a consulta, mostrar carregamento. | Campo de CEP e botão “Consultar e salvar” ficam desabilitados até a conclusão. |
| RF-008 | Ao concluir com sucesso, renderizar o endereço. | Exibir os campos da seção 5, limpar o campo de entrada e liberar os controles. |
| RF-009 | Ao concluir com erro, apresentar a mensagem correspondente. | Liberar os controles, manter o CEP informado para correção ou nova tentativa e preservar o endereço anterior visível, se houver. |
| RF-010 | Ao abrir o aplicativo, recuperar a última consulta bem-sucedida armazenada. | Exibir o último endereço consultado, considerando também consultas anteriores atendidas pelo cache, sem chamada à API. Sem registros, não exibir endereço. |
| RF-011 | Ao acessar “CEPs armazenados”, apresentar os registros locais. | Ordenar pela última consulta bem-sucedida, da mais recente para a mais antiga, e refletir mudanças locais. Sem registros, exibir “Nenhum CEP armazenado.” |
| RF-012 | Ao exibir um endereço com campos textuais vazios, preservar a apresentação atual. | Mostrar “Não informado” em cada campo vazio; a ausência de campos complementares não impede o sucesso. |

## 4. Regras de negócio

- **RN-001 — Identidade:** o CEP normalizado de oito dígitos identifica o endereço armazenado. A máscara é apenas apresentação.
- **RN-002 — Entrada existente:** manter a remoção de caracteres não numéricos e o truncamento a oito dígitos na máscara da UI. Não introduzir rejeição de colagem por letras ou excesso de dígitos. O valor efetivamente enviado à consulta deve passar pela validação centralizada.
- **RN-003 — Prioridade local:** um endereço armazenado satisfaz a consulta do mesmo CEP. Sua idade não provoca expiração, exclusão ou atualização remota.
- **RN-004 — Acesso remoto:** chamar a ViaCEP somente quando não houver endereço local para o CEP solicitado. Falha ao ler o banco não equivale a ausência confirmada do registro.
- **RN-005 — Salvamento automático:** consulta remota bem-sucedida inclui armazenamento do endereço; não há ação separada de salvar ou confirmação adicional.
- **RN-006 — Recência:** cada consulta bem-sucedida, local ou remota, atualiza a ordem do CEP no histórico. Abrir o aplicativo ou visualizar a lista não constitui nova consulta.
- **RN-007 — Sem duplicação:** consultar novamente um CEP movimenta seu registro para o início, sem criar outra entrada na lista.
- **RN-008 — Resultado anterior:** uma falha não apaga nem substitui o endereço anteriormente apresentado e não o transforma em resposta para o CEP que falhou.
- **RN-009 — Dados parciais:** campos complementares vazios não tornam um endereço encontrado inválido; apresentar “Não informado”.
- **RN-010 — Sem aviso de cache:** não adicionar indicação de origem local, idade do registro ou aviso de desatualização.
- **RN-011 — Erro de servidor:** havendo endereço salvo para o CEP, ele será exibido antes de qualquer acesso remoto. Portanto, não ocorre HTTP 500 para esse CEP no fluxo normal. Para CEP novo, HTTP 500 produz erro e preserva apenas o resultado anterior da tela, quando existente.

## 5. Dados de entrada e saída

### Entrada

| Campo | Tipo | Obrigatório | Origem | Validação e apresentação |
| --- | --- | --- | --- | --- |
| CEP | Texto | Sim | Digitação ou colagem pelo usuário | Máscara atual; até oito dígitos na UI; exatamente oito dígitos após normalização para consultar. |

A consulta é disparada pelo botão “Consultar e salvar”, não automaticamente ao completar a digitação.

### Endereço apresentado e armazenado

| Campo | Apresentação |
| --- | --- |
| CEP | Máscara `00000-000`. |
| Logradouro | Texto retornado, ou “Não informado”. |
| Complemento | Texto retornado, ou “Não informado”. |
| Bairro | Texto retornado, ou “Não informado”. |
| Cidade/localidade | Texto retornado, ou “Não informado”. |
| UF | Texto retornado, ou “Não informado”. |
| Estado | Texto retornado, ou “Não informado”. |

A recência da última consulta bem-sucedida deve persistir entre sessões para sustentar a ordenação e a restauração. Não é necessário exibi-la nem definir aqui sua representação no banco.

## 6. Estados e transições

| Estado atual | Evento/condição | Resultado/estado seguinte | Efeito visível |
| --- | --- | --- | --- |
| Inicial (`Idle`) | Abertura sem endereços armazenados | `Idle` | Pesquisa disponível, sem endereço. |
| Inicial (`Idle`) | Restauração de endereço anterior | `Success` | Exibir o último consultado sem consulta remota e sem modificar sua recência. |
| `Idle`, `Success` ou `Error` | Acionar consulta com entrada inválida | `Error` | Erro no campo; preservar resultado anterior, se houver; não buscar dados. |
| `Idle`, `Success` ou `Error` | Acionar consulta válida | `Loading` | Exibir progresso e desabilitar campo e botão. |
| `Loading` | Endereço local encontrado e consulta concluída | `Success` | Exibir endereço, atualizar recência, limpar entrada e liberar controles. |
| `Loading` | Endereço ausente localmente e consulta remota concluída com armazenamento | `Success` | Exibir e armazenar endereço, atualizar recência, limpar entrada e liberar controles. |
| `Loading` | CEP não encontrado ou falha | `Error` | Mostrar erro, preservar entrada e resultado anterior, liberar controles. |
| Qualquer estado estável | Abrir lista de armazenados | Manter estado da consulta | Mostrar lista local; a navegação não altera recência. |

O último resultado visível pode coexistir com um erro de uma nova tentativa. Os nomes de estado refletem o projeto existente; a implementação pode preservar esse resultado separadamente sem criar novas telas. Uma eventual emissão transitória de `Loading` antes da validação não autoriza acesso a dados para CEP inválido.

O fluxo considera uma consulta do usuário por vez, mantendo o bloqueio de campo e botão de RF-007. A restauração inicial é uma leitura automática: se terminar após o início de uma pesquisa, não deve sobrescrever o estado dessa pesquisa.

## 7. Edge cases e tratamento de erros

| Cenário | Comportamento esperado | Mensagem existente |
| --- | --- | --- |
| CEP vazio ou incompleto | Não consultar banco/API; permitir correção. | “Digite um CEP válido”. |
| Colagem com letras ou mais de oito dígitos | Aplicar a máscara atual, removendo não numéricos e limitando a oito dígitos; validar o resultado. | Erro de CEP inválido apenas se o resultado for incompleto. |
| CEP com zeros iniciais | Preservar zeros; tratar como texto. | Sem mensagem adicional. |
| CEP armazenado há mais de 24 horas, ou sem data histórica útil | Usar endereço local, sem expiração nem chamada remota. | Sem aviso de cache. |
| Consulta local sem internet | Concluir normalmente e registrar a consulta. | Apresentação normal do endereço. |
| CEP novo e resposta ViaCEP com `erro: true` | Não armazenar resultado nem atualizar histórico; manter endereço anterior na tela. | “CEP não encontrado”. |
| CEP novo e falha de conexão/timeout | Apresentar falha, sem criar registro; manter resultado anterior. | “Não foi possível consultar o CEP. Verifique sua conexão e tente novamente.” |
| CEP novo e HTTP 500 ou outra falha HTTP | Apresentar falha, sem criar registro; manter resultado anterior. | “Não foi possível concluir a consulta. Tente novamente.” |
| Falha de leitura ou gravação local | Não declarar sucesso completo nem armazenamento confirmado; manter resultado anterior e permitir nova tentativa. | “Não foi possível concluir a consulta. Tente novamente.” |
| Campos complementares vazios | Aceitar endereço encontrado e apresentar campos vazios conforme RF-012. | “Não informado”. |
| Reconsulta de CEP armazenado | Não acessar API; manter um registro e movê-lo para o topo. | Apresentação normal do endereço. |
| Nova tentativa após erro | Permitir consulta pelo mesmo botão, com a entrada preservada ou corrigida. | Mensagem correspondente ao novo resultado. |
| Cancelamento de coroutine | Preservar a propagação de cancelamento existente; não convertê-lo em erro de CEP ou de rede. | Não criar mensagem de falha de negócio para cancelamento. |

Preservar as mensagens e os mecanismos de apresentação existentes. Não adicionar tentativas periódicas, aviso de cache ou ação de atualização.

## 8. Fora do escopo

- Pesquisa por nome de pessoa, cidade ou logradouro e descoberta de CEP por endereço.
- Troca de provedor ou integração com outra API.
- Novas telas, reformulação visual ou mudança do fluxo de navegação.
- Inclusão de campos como IBGE, DDD e região.
- Expiração de cache, atualização remota de CEP já salvo ou atualização em segundo plano.
- Avisos sobre origem, validade ou desatualização do endereço armazenado.
- Novos botões para atualizar, excluir, editar ou salvar separadamente; favoritos e sincronização entre dispositivos.
- Autenticação, perfis e permissões adicionais.
- Histórico com uma linha por tentativa: a lista continua contendo um registro por CEP.
- Proteções e testes específicos para consultas simultâneas: mutex, serialização no repositório, guardas adicionais no ViewModel e segunda leitura antes do salvamento motivada por inserções concorrentes. Essa exclusão não altera RF-007 nem as garantias de unicidade e atomicidade da persistência.

## 9. Premissas e dependências

- Manter o aplicativo Android existente e a estrutura de UI, domínio e dados apresentada no projeto/diagrama.
- Retrofit + Coroutines, StateFlow e Room são restrições técnicas herdadas do projeto e confirmadas no escopo; este FRD não determina o desenho detalhado das alterações.
- Preservar as telas XML, ViewBinding, Activity/Fragments e navegação existentes.
- Por decisão explícita do usuário em 07/09/2026, o app não contempla consultas simultâneas no fluxo de uso. O planejamento deve retirar as proteções propostas para esse cenário, preservando o bloqueio dos controles durante a consulta e o tratamento da restauração inicial atrasada.
- A consulta remota depende de conexão e da disponibilidade da ViaCEP. A consulta local depende do armazenamento do dispositivo.
- Preservar endereços existentes ao evoluir a persistência. A última consulta real anterior à implementação não pode ser reconstruída se não foi registrada; usar a ordenação legada como base inicial, passando a registrar corretamente as novas consultas. Essa é uma premissa de compatibilidade, não uma informação histórica recuperada.
- O armazenamento sem expiração pode manter um endereço antigo indefinidamente; esse comportamento resulta da decisão explícita de chamar a API somente para CEPs novos.
- Nenhuma alteração funcional foi executada como parte da geração deste documento.

## 10. Questões em aberto

Não há decisão funcional bloqueante para o fluxo aprovado. Detalhes de schema, migração, desempate determinístico de registros legados e implementação do registro de recência pertencem ao planejamento técnico e devem preservar os comportamentos acima.

## 11. Critérios de aceite

1. Dado um CEP inválido após a máscara, ao consultar, mostrar a validação sem buscar dados.
2. Dado um CEP novo encontrado na ViaCEP, ao consultar, armazenar um registro, exibir seus campos e limpar a entrada.
3. Dado um CEP armazenado há mais de 24 horas, ao consultar, retornar o registro local sem qualquer chamada à ViaCEP.
4. Dados os CEPs A e B armazenados, com B no topo, ao consultar A pelo cache, manter dois registros, mover A para o topo e restaurar A após reabrir o app.
5. Dado um endereço A visível, ao consultar um CEP B novo com falha de rede ou HTTP 500, mostrar o erro, manter A visível e não registrar B como sucesso.
6. Dado um CEP novo inexistente, mostrar “CEP não encontrado” e não armazená-lo.
7. Dado um endereço com campos complementares vazios, exibir “Não informado” e concluir a consulta.
8. Durante a consulta, manter os controles de entrada desabilitados e o progresso visível; liberá-los ao concluir.
9. Consultas atendidas localmente funcionam sem internet, sem aviso de cache e sem expiração.
10. Abrir a lista ou restaurar o endereço inicial não muda a ordem do histórico.
11. Uma restauração inicial que termine após o início de uma pesquisa não sobrescreve o estado dessa pesquisa.

## 12. Diferenças a implementar

| Aspecto | Código analisado | Comportamento aprovado |
| --- | --- | --- |
| Validade local | TTL de 24 horas, com tentativa remota após vencimento. | Sem TTL; retorno local sempre que o CEP existir. |
| Fallback | Reutilização do cache vencido em `IOException`. | Cache existente impede a chamada remota; fallback remoto para o mesmo CEP deixa de fazer parte do fluxo. |
| Recência | Data de salvamento/atualização remota. | Última consulta bem-sucedida, inclusive local. |
| Último endereço e lista | Ordenação por salvamento/atualização. | Ordenação persistente por consulta bem-sucedida. |
| Documentação anterior | README descreve validade de 24 horas. | Atualizar ao implementar, distinguindo documentação histórica e regra vigente. |

Os demais comportamentos aprovados devem ser preservados. As diferenças funcionais desta seção foram implementadas na árvore de trabalho; os testes locais e o build passaram na execução posterior às correções da revisão; a tentativa original da T-011 permanece registrada como histórica.

## 13. Registro de decisões posteriores

- **07/09/2026 — Planejamento do Blueprint:** o usuário esclareceu que não há consultas simultâneas no fluxo do app e solicitou retirar as proteções propostas para esse cenário. Foram excluídos do planejamento mutex/serialização, guardas adicionais para pesquisas concorrentes, segunda leitura defensiva antes do salvamento e testes de consultas simultâneas. Permanecem RF-007, a unicidade do CEP, a atomicidade do salvamento e da recência e a proteção contra restauração inicial atrasada. Esta revisão é documental e não representa implementação ou aprovação das demais propostas técnicas do Blueprint.
- **07/09/2026 — Estratégia de testes:** o usuário determinou a retirada dos testes instrumentados dependentes de ADB. O projeto não deve usar aparelho, emulador, ADB ou `connectedDebugAndroidTest` como parte da validação; a cobertura automatizada deve ser mantida em testes unitários locais.
- **07/09/2026 — Status da implementação:** a documentação ativa foi atualizada pela T-012 para refletir o comportamento sem TTL, a prioridade local e a recência por consulta bem-sucedida. A T-011 registra que os testes unitários e o build ainda precisam ser reexecutados em ambiente com acesso ao Android SDK.

- **07/09/2026 — Correções da revisão:** R-01–R-07 aprovadas pelo usuário e aplicadas; erros de restauração/observação tratados, feedback de falha consumível e cobertura local ampliada. Corrigido o truncamento da colagem antes da máscara, mantendo RF-001/RN-002. Resultado: 80 testes passaram e build debug concluído. O transporte público e hardware não foram exercitados.
