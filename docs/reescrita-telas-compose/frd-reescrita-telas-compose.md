# FRD — Reescrita das telas em Jetpack Compose

Data: 17/09/2026  
Status: escopo confirmado pelo usuário; implementação ainda não iniciada.

## 1. Visão geral

Reescrever a interface do aplicativo de consulta de CEP em Jetpack Compose. A entrega abrange as telas de pesquisa e de CEPs armazenados, a barra superior e a navegação entre elas. A aparência e o fluxo atuais devem ser preservados, com as mudanças de feedback e restauração de entrada definidas neste documento.

O aplicativo continua consultando endereços por CEP, usando primeiro os dados locais e recorrendo à ViaCEP somente para CEPs ainda não armazenados. A reescrita da interface não altera essas regras nem os dados existentes. Este FRD especifica o comportamento da nova interface de forma independente da implementação atual em XML, Fragments e RecyclerView.

## 2. Atores

| Ator | Papel |
| --- | --- |
| Usuário do aplicativo | Digitar e consultar um CEP, ver o resultado e navegar pela lista de CEPs armazenados. |
| Sistema Android | Recriar a tela ou encerrar e restaurar a tarefa, quando houver estado de tarefa recuperável. |

Não há autenticação ou papéis com permissões diferentes.

## 3. Requisitos funcionais

| ID | Condição de disparo e comportamento | Resultado esperado |
| --- | --- | --- |
| RF-001 | Ao abrir o aplicativo, apresentar a tela de pesquisa. | Exibir barra superior com “Busca CEP”, campo de CEP, botão “Consultar e salvar” e acesso a “CEPs armazenados”. Havendo último endereço consultado no armazenamento, apresentá-lo; sem registro, não mostrar o cartão de resultado. |
| RF-002 | Ao digitar ou colar no campo, aplicar a máscara de CEP. | Remover caracteres não numéricos, limitar a oito dígitos e apresentar `00000-000` conforme a digitação. Usar entrada numérica no teclado virtual. |
| RF-003 | Ao acionar “Consultar e salvar”, validar e normalizar o CEP antes de buscar dados. | CEP válido segue para consulta; CEP incompleto produz o Toast “Digite um CEP válido”, sem leitura para essa consulta no banco ou na API. |
| RF-004 | Ao consultar um CEP válido, usar a regra vigente de dados. | CEP local retorna sem expiração e sem acesso remoto; CEP ausente localmente é consultado na ViaCEP e salvo automaticamente após sucesso. Manter um registro por CEP e atualizar a recência em toda consulta bem-sucedida. |
| RF-005 | Enquanto uma consulta estiver em andamento, mostrar progresso. | Campo de CEP e botão “Consultar e salvar” ficam desabilitados até a conclusão. |
| RF-006 | Quando a consulta terminar com sucesso, mostrar o endereço. | Exibir o cartão “Último CEP consultado” com os campos da seção 5, limpar o campo de entrada, liberar os controles e mostrar o Toast “Endereço disponível.” |
| RF-007 | Quando a consulta terminar com falha, mostrar o erro correspondente. | Manter o CEP informado, liberar os controles e preservar o endereço anterior visível, se houver. Mostrar a mensagem da seção 7 somente por Toast, sem erro próprio no campo. O resultado preservado continua identificado pelo CEP ao qual pertence. |
| RF-008 | Ao acionar “CEPs armazenados”, abrir a lista. | Exibir a barra superior com “CEPs armazenados” e ação de voltar. Apresentar um cartão por CEP, ordenado da última consulta bem-sucedida para a mais antiga; refletir mudanças do armazenamento local. |
| RF-009 | Quando a lista carregada não contiver registros, mostrar o estado vazio. | Exibir “Nenhum CEP armazenado.” e não apresentar o título ou cartões da lista. Não declarar lista vazia antes de concluir a leitura. |
| RF-010 | Ao voltar da lista para a pesquisa, preservar o estado da pesquisa. | Manter o CEP parcialmente digitado, o último endereço visível e o estado estável da tela. Abrir ou fechar a lista não constitui uma nova consulta e não muda a recência. |
| RF-011 | Ao girar o aparelho, preservar o estado da interface. | Manter a entrada parcial, o endereço visível e uma consulta que já esteja em andamento; não repetir Toast já apresentado nem iniciar outra consulta apenas pela recriação da tela. |
| RF-012 | Se o sistema encerrar e depois restaurar a tarefa com estado recuperável, recuperar a entrada parcial. | Reapresentar o CEP digitado antes do encerramento. Em abertura de nova sessão sem estado de tarefa recuperável, a entrada começa vazia e o último endereço é recuperado do armazenamento conforme RF-001. |
| RF-013 | Ao receber um novo resultado da lista local enquanto a tela de lista estiver aberta, atualizá-la. | Mostrar a ordem e os endereços atuais sem ação manual de atualização. Uma falha de leitura apresenta o Toast genérico da seção 7, sem afirmar que a lista está vazia. |
| RF-014 | Ao renderizar endereços em qualquer tela, apresentar os mesmos dados. | Usar CEP formatado e os seis campos textuais da seção 5; mostrar “Não informado” para cada campo textual vazio. |

## 4. Regras de negócio

- **RN-001 — Identidade do CEP:** o CEP normalizado de oito dígitos identifica o registro; a máscara é apenas apresentação.
- **RN-002 — Prioridade local:** um CEP armazenado satisfaz a consulta independentemente da idade, sem acesso remoto e sem aviso de cache.
- **RN-003 — Consulta remota:** a ViaCEP é usada somente após confirmar que o CEP não existe localmente. Sucesso remoto inclui salvamento automático; falha local não equivale a CEP ausente.
- **RN-004 — Recência:** cada consulta bem-sucedida, inclusive local, move o CEP para o início da lista. Abertura do app, restauração do último endereço, navegação e exibição da lista não alteram essa ordem.
- **RN-005 — Falha:** uma falha não salva o CEP solicitado, não altera a recência e não substitui o endereço anterior. O cartão preservado não representa uma resposta para o CEP que falhou.
- **RN-006 — Feedback:** sucesso, validação, CEP inexistente e demais falhas usam Toast. Cada ocorrência é apresentada uma vez; recomposição, retorno à tela ou recriação não repetem um Toast já consumido.
- **RN-007 — Dados existentes:** reescrever a UI não apaga, duplica nem recria os endereços armazenados. A lista continua contendo um registro por CEP.
- **RN-008 — Uma consulta por vez:** durante a consulta, os controles de entrada permanecem bloqueados. A restauração inicial atrasada não pode sobrescrever o estado de uma pesquisa iniciada depois dela.

## 5. Dados de entrada e apresentação

### Entrada

| Campo | Tipo e origem | Obrigatório | Validação e apresentação |
| --- | --- | --- | --- |
| CEP | Texto digitado ou colado pelo usuário | Sim, para consultar | Remover não numéricos e limitar a oito dígitos na máscara; exigir exatamente oito dígitos após normalização para consultar. Preservar zeros iniciais. |

A consulta ocorre apenas ao acionar “Consultar e salvar”, nunca automaticamente ao completar oito dígitos. A entrada parcial tem as regras de preservação e restauração de RF-010 a RF-012.

### Endereço exibido na pesquisa e em cada cartão da lista

| Campo | Apresentação |
| --- | --- |
| CEP | Máscara `00000-000`. |
| Logradouro | Valor armazenado ou “Não informado”. |
| Complemento | Valor armazenado ou “Não informado”. |
| Bairro | Valor armazenado ou “Não informado”. |
| Localidade | Valor armazenado ou “Não informado”. |
| UF | Valor armazenado ou “Não informado”. |
| Estado | Valor armazenado ou “Não informado”. |

A recência é persistida para ordenação e restauração do último endereço, mas não aparece como campo visual.

## 6. Estados e transições da interface

| Estado atual | Evento ou condição | Estado seguinte | Efeito visível |
| --- | --- | --- | --- |
| Pesquisa inicial | Sem endereço armazenado e sem estado de tarefa recuperável | Pesquisa sem resultado | Campo vazio; cartão de resultado oculto. |
| Pesquisa inicial | Último endereço armazenado encontrado | Pesquisa com resultado | Mostrar endereço, sem Toast de sucesso e sem alterar recência. |
| Pesquisa inicial | Tarefa restaurada com entrada parcial | Pesquisa com entrada restaurada | Mostrar entrada parcial; recuperar também o último endereço disponível sem iniciar consulta. |
| Pesquisa estável | Usuário edita CEP | Pesquisa estável | Atualizar máscara e entrada visível; não consultar. |
| Pesquisa estável | Acionar consulta inválida | Pesquisa com erro | Toast de validação; preservar entrada e resultado anterior. |
| Pesquisa estável | Acionar consulta válida | Consulta em andamento | Mostrar progresso e bloquear campo e botão. |
| Consulta em andamento | Consulta local ou remota concluída com sucesso | Pesquisa com resultado | Mostrar endereço, limpar entrada e emitir Toast de sucesso uma vez. |
| Consulta em andamento | CEP não encontrado ou falha | Pesquisa com erro | Mostrar Toast correspondente, preservar entrada e resultado anterior. |
| Pesquisa estável | Abrir lista | Lista em carregamento ou carregada | Mostrar registros locais, sem alterar a pesquisa ou a recência. |
| Lista carregada | Armazenamento emite mudança | Lista atualizada | Atualizar cartões e ordem. |
| Lista carregada | Leitura falha | Lista com falha de leitura | Toast genérico; não tratar falha como lista vazia. |
| Lista | Voltar pela barra superior ou pelo sistema | Pesquisa estável | Restaurar a apresentação anterior da pesquisa. |

O endereço anterior pode permanecer visível durante uma nova consulta e após seu erro. A apresentação de um Toast é um evento transitório; o endereço, a entrada e os estados de carregamento ou erro são dados da tela.

## 7. Casos limite e tratamento de erros

| Cenário | Comportamento esperado | Toast |
| --- | --- | --- |
| CEP vazio ou incompleto | Não consultar dados; manter entrada para correção. | “Digite um CEP válido”. |
| Colagem com letras ou mais de oito dígitos | Remover caracteres não numéricos, limitar a oito dígitos, formatar e validar o resultado. | Somente se o resultado for incompleto. |
| CEP novo inexistente na ViaCEP | Não salvar nem alterar recência; manter resultado anterior. | “CEP não encontrado”. |
| CEP novo com falha de conexão ou timeout | Não salvar; manter entrada e resultado anterior. | “Não foi possível consultar o CEP. Verifique sua conexão e tente novamente.” |
| CEP novo com HTTP 500 ou outra falha HTTP | Não salvar; manter entrada e resultado anterior. | “Não foi possível concluir a consulta. Tente novamente.” |
| Falha de leitura ou gravação local na consulta | Não declarar sucesso nem armazenamento confirmado; permitir nova tentativa. | “Não foi possível concluir a consulta. Tente novamente.” |
| Falha de leitura da lista | Não apresentar um estado vazio falso; permitir nova leitura ao retornar ou reabrir a tela. | “Não foi possível concluir a consulta. Tente novamente.” |
| Falha ao restaurar o último endereço | Manter a pesquisa disponível e não apresentar endereço não confirmado. | “Não foi possível concluir a consulta. Tente novamente.” |
| CEP local antigo ou ausência de internet | Retornar o endereço local e atualizar sua recência. | “Endereço disponível.” |
| Campos textuais vazios no endereço | Aceitar o endereço e mostrar “Não informado” em cada campo vazio. | “Endereço disponível.” somente quando decorrer de uma consulta. |
| Restauração do último endereço ou retorno da lista | Mostrar os dados sem registrar nova consulta. | Nenhum. |
| Recriação após Toast já exibido | Preservar o estado da tela sem reapresentar o evento consumido. | Nenhum Toast repetido. |

## 8. Aparência e navegação

- Manter a estrutura visual atual: barra superior, campo e botões centralizados na pesquisa, cartão do último endereço e cartões individuais na lista.
- Manter os títulos, rótulos, textos, espaçamentos e cores existentes como referência visual. Preservar os modos claro e escuro, garantindo legibilidade de textos e controles em ambos.
- A barra superior mostra “Busca CEP” na pesquisa e “CEPs armazenados” na lista. Na lista, a ação de voltar da barra e o botão Voltar do sistema retornam à pesquisa.
- A lista apresenta o título “CEPs consultados” quando houver registros. Sem registros, usa o estado vazio de RF-009.
- A lista é apenas para visualização; seus cartões não iniciam nova consulta nem mudam a recência.
- O conteúdo deve continuar acessível em telas com altura reduzida ou teclado aberto, sem esconder permanentemente a ação de consultar ou de abrir a lista.

## 9. Fora do escopo

- Pesquisa por nome, cidade ou logradouro; novos campos ou novas telas.
- Reformulação visual, novos fluxos, edição, exclusão, atualização manual ou sincronização dos endereços.
- Alteração do provedor ViaCEP, das regras de cache, recência, unicidade ou persistência.
- Expiração de dados, atualização remota de CEP já salvo e aviso de cache.
- Exigir que uma entrada parcial sobreviva à abertura de uma sessão inteiramente nova sem estado de tarefa recuperável.
- Material didático separado do desenvolvimento. As explicações serão dadas conforme as etapas do projeto avançarem.

## 10. Premissas e dependências

- A base funcional é o FRD aprovado de consulta de CEP, de 07/09/2026. As mudanças explícitas neste documento prevalecem apenas para a apresentação do feedback e a restauração da entrada parcial.
- O armazenamento existente deve continuar acessível após a reescrita. A interface recebe o estado e solicita ações à camada de apresentação; não acessa diretamente Room ou ViaCEP.
- A restauração da entrada parcial após encerramento pelo sistema depende de o Android oferecer estado de tarefa recuperável. A abertura de uma sessão nova sem esse estado segue RF-001 com campo vazio.
- O projeto mantém duas telas e uma consulta do usuário por vez. A validação automatizada do projeto é executada localmente, sem aparelho, emulador ou ADB.
- Durante a evolução técnica, o usuário quer aprender os conceitos de Compose aplicados ao projeto, incluindo UI declarativa, composables, `remember`, `mutableStateOf`, recomposição, `Column`, `Row`, `Box` e `LazyColumn`. A configuração e as mudanças serão explicadas quando forem planejadas e implementadas. A escrita de Pull Requests pelo método STAR (Situação, Tarefa, Ação, Resultado) será trabalhada com um exemplo da mudança realizada.

## 11. Questões em aberto

Não há decisão funcional bloqueante. Bibliotecas, versões, estrutura de componentes, mecanismo de navegação e estratégia técnica de restauração pertencem ao planejamento de implementação, desde que cumpram os comportamentos acima.

## 12. Critérios de aceite

1. A pesquisa, a lista, a barra superior e a navegação usam Compose e mantêm a organização visual e os textos aprovados nos modos claro e escuro.
2. A máscara remove não numéricos, limita a oito dígitos, preserva zeros iniciais e a consulta só ocorre pelo botão.
3. CEP inválido e inexistente geram os Toasts correspondentes, sem mensagem de erro no campo e sem leitura ou escrita indevida.
4. Durante consulta válida, progresso é visível e campo e botão ficam desabilitados; sucesso exibe endereço, limpa entrada e emite o Toast de sucesso uma vez.
5. Uma falha de rede, HTTP ou armazenamento mantém a entrada e o endereço anterior com seu CEP original, emite o Toast correspondente e não altera a recência.
6. CEP local é mostrado sem rede e sem expiração; cada sucesso de consulta atualiza a recência e mantém um único registro por CEP.
7. A lista reflete os registros locais por recência, mostra todos os campos e “Não informado” quando necessário; lista carregada vazia mostra a mensagem existente.
8. Voltar da lista ou girar o aparelho preserva a entrada parcial e o endereço visível, sem nova consulta, alteração de recência ou repetição de Toast.
9. Após restauração da tarefa pelo sistema com estado recuperável, a entrada parcial reaparece. Em nova sessão sem esse estado, o campo começa vazio e o último endereço salvo pode ser restaurado.
10. A aparência permanece legível nos modos claro e escuro e o conteúdo pode ser alcançado com teclado aberto ou altura de tela reduzida.
