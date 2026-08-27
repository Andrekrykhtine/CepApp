---
name: code-structure-explainer
description: Analisa e explica a estrutura, as responsabilidades e os fluxos reais de um projeto de software. Use para onboarding, compreensão arquitetural, navegação pelo código ou entendimento de uma feature; não use como revisão formal nem como autorização para alterar o projeto.
---

# Code Structure Explainer

Construa um modelo mental claro do projeto a partir de evidências no repositório. Explique somente o que for relevante ao pedido e adapte a profundidade ao tamanho do código e ao conhecimento do usuário.

## Princípios

- Comece pelo contexto geral antes de entrar em arquivos, classes ou métodos.
- Confirme arquitetura, tecnologias e fluxos pelo uso real no código; dependências e nomes de pastas são apenas indícios.
- Prefira um caminho representativo de ponta a ponta a um inventário exaustivo de arquivos.
- Separe fatos encontrados, inferências arquiteturais e sugestões de melhoria quando essa distinção for necessária.
- Explique conceitos apenas quando ajudarem a compreender o projeto concreto.
- Não transforme uma solicitação de explicação em revisão formal, refatoração ou edição de código.
- Aponte problemas somente quando houver evidência e explique seu impacto sem impor preferências arquiteturais.

## Leitura inicial

Antes da análise, determine o escopo pedido: projeto inteiro, módulo, camada, feature, classe ou fluxo.

Quando o repositório estiver disponível:

1. Leia as instruções aplicáveis do repositório, como `AGENTS.md`.
2. Identifique módulos, stack e entry points pelos arquivos de configuração relevantes.
3. Examine a árvore somente até localizar os componentes relacionados ao escopo.
4. Leia o código que participa do fluxo e seus testes correspondentes.
5. Consulte documentação relacionada, verificando se ainda corresponde ao código atual.

Use esta precedência quando as fontes divergirem:

```text
código e configuração atuais
↓
testes atuais
↓
instruções e documentação arquitetural vigente
↓
documentos históricos e README
```

Se houver alterações locais, considere que a estrutura pode estar em transição e evite tratar documentos antigos como estado atual.

## Profundidade adaptativa

Escolha o menor nível que responda bem ao usuário:

- **Macro:** módulos, camadas, entry points e direção das dependências.
- **Feature:** evento inicial, componentes atravessados, persistência ou integração e caminho de retorno.
- **Código:** responsabilidades de classes e métodos específicos.

Não desça ao nível de métodos ou linhas sem necessidade. Em projetos grandes, entenda primeiro o mapa global e depois acompanhe verticalmente apenas a feature relevante.

## Análise estrutural

Identifique, conforme aplicável:

- módulos e suas responsabilidades;
- organização por camada ou por feature;
- pontos de entrada;
- componentes centrais e quem os instancia;
- direção das dependências e interfaces que formam fronteiras;
- localização das regras de negócio;
- estado, eventos e operações assíncronas;
- fontes de dados e transformações relevantes;
- testes que comprovam o comportamento explicado.

Nomeie um padrão arquitetural somente depois de confirmar responsabilidades, dependências, criação de objetos e fluxo de dados. Se a implementação for parcial ou híbrida, diga isso diretamente.

## Rastreamento de uma feature

Quando o pedido envolver comportamento, reconstruir um fluxo real costuma ser a explicação mais útil. Mostre apenas os componentes existentes, por exemplo:

```text
ação ou entrada
↓
componente de interface
↓
orquestração e estado
↓
regra de negócio
↓
fronteira de dados
↓
persistência ou integração
```

Depois descreva como o resultado volta à interface e quais representações dos dados mudam no caminho. Omita etapas que não existam; não invente Use Cases, Data Sources, DTOs, mappers ou outras camadas para completar um diagrama idealizado.

Quando ajudar o usuário a trabalhar no código, indique de forma concisa:

- onde começar a alteração pretendida;
- quais consumidores ou dependências verificar em seguida;
- onde o comportamento é testado.

## Projetos Android e Kotlin

Em projetos Android, ajuste a leitura ao que estiver realmente presente:

- use os arquivos Gradle para confirmar módulos, toolchain e dependências relevantes;
- use o Manifest para localizar a inicialização, componentes registrados, permissões e deep links;
- diferencie Views XML e Compose e siga o mecanismo de navegação efetivamente usado;
- conecte Activity, Fragment ou Composable ao ViewModel, estado e eventos quando existirem;
- mostre como dependências são criadas, seja por factory, composição manual ou framework de DI;
- conecte repositórios às fontes reais, como Room, DataStore, rede ou SDK externo;
- explique `StateFlow`, `SharedFlow`, coroutines e lifecycle no contexto das propriedades e chamadas reais;
- diferencie testes locais, instrumentados e de UI somente quando forem relevantes ao fluxo.

Não procure nem explique todas as tecnologias Android possíveis. Aprofunde apenas mecanismos encontrados e importantes para o pedido.

## Evidência e precisão

Para cada afirmação importante, seja capaz de apontar o arquivo, símbolo, configuração ou teste que a sustenta. Use linguagem calibrada:

- **Encontrado:** comportamento ou relação explícita no código.
- **Inferência:** interpretação sustentada por múltiplos indícios.
- **Sugestão:** alternativa de melhoria, separada da descrição atual.

Não atribua rótulos como MVVM, MVI, Clean Architecture, UDF, offline-first ou single source of truth apenas por convenção de nomes.

## Resposta

Comece pelo resultado: o modelo mental mais importante para o usuário. Organize a resposta de forma adaptativa, usando somente as seções necessárias entre:

- visão geral;
- mapa estrutural simplificado;
- responsabilidades e dependências;
- fluxo da feature ou dos dados;
- testes e pontos de atenção;
- ordem recomendada de leitura ou locais de alteração.

Use um diagrama textual apenas quando ele tornar relações ou sequência mais claras do que a prosa. Cite caminhos e símbolos reais. Evite repetir o mesmo fluxo em mapa, lista e texto.

Para uma pergunta estreita, responda diretamente sem produzir uma análise completa do projeto. Para onboarding amplo, termine com poucos arquivos ou classes em uma ordem de leitura justificada.
