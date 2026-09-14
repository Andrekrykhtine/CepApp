# Relatório de revisão — qualidade e Clean Code

## Escopo e limitações

Revisão estática do aplicativo Android completo, incluindo código Kotlin, layouts, recursos, configuração Gradle e testes. O repositório não contém FRD nem Blueprint técnico; por isso, conformidade funcional com requisitos formais não pôde ser avaliada.

## Resumo

- Total: 8 findings — 1 alto, 3 médios e 4 baixos.
- Clean Code: razoável para o tamanho atual, mas com baixa testabilidade e responsabilidades concentradas na Activity.
- Cobertura de testes: insuficiente. O único teste local é o exemplo `2 + 2`; o teste instrumentado apenas verifica o package name.
- Android Lint: 0 erros e 9 avisos.
- Segurança: nenhum risco crítico identificado no escopo analisado.
- FRD/Blueprint: não disponíveis; conformidade não avaliada.

## Findings

### [ALTO] Não há testes dos comportamentos reais

**Eixo:** Qualidade/testes  
**Localização:** `app/src/test/java/com/example/cepapplication/ExampleUnitTest.kt:12`; `app/src/androidTest/java/com/example/cepapplication/ExampleInstrumentedTest.kt:17`  
**Problema:** os testes gerados pelo template não exercitam formatação, validação, persistência, restauração nem interação da tela. Uma regressão nesses fluxos continuaria produzindo uma build verde.  
**Recomendação:** extrair formatação e validação para componentes Kotlin puros e testar entradas vazias, CEP parcial, oito dígitos, caracteres não numéricos, colagem com mais de oito dígitos e limites. Adicionar testes de UI para CEP inválido, salvamento válido e restauração do valor salvo.

### [MÉDIO] A Activity concentra UI, formatação, validação e persistência

**Eixo:** Qualidade/estrutura  
**Localização:** `app/src/main/java/com/example/cepapplication/MainActivity.kt:11`  
**Problema:** `MainActivity` configura a interface, aplica máscara, valida CEP, lê/escreve `SharedPreferences` e monta mensagens. Hoje o arquivo ainda é pequeno, mas essa concentração torna a lógica difícil de testar e aumenta o custo de qualquer evolução.  
**Recomendação:** extrair primeiro apenas os limites que trazem benefício imediato: um `CepFormatter`/`CepValidator` puro e um pequeno repositório para persistência. Se a tela ganhar consulta de API ou mais estado, adotar `ViewModel` e expor estado de UI. Evitar criar camadas adicionais sem necessidade.

### [MÉDIO] A máscara descarta entrada excedente silenciosamente

**Eixo:** Qualidade/comportamento  
**Localização:** `app/src/main/java/com/example/cepapplication/MainActivity.kt:127`  
**Problema:** `take(8)` remove dígitos além do limite. Ao colar um valor maior, o usuário vê um CEP aparentemente válido e pode salvar um valor diferente do fornecido, sem aviso. Como a máscara já truncou a entrada, `salvarCep()` não consegue detectar o excesso.  
**Recomendação:** definir explicitamente a regra: rejeitar excesso com erro, ou impedir a digitação por filtro sem transformar silenciosamente um valor colado. A validação deve receber a entrada original e a formatação deve ocorrer somente após a validação ou por um componente que preserve a intenção de edição.

### [MÉDIO] Textos visíveis estão hardcoded no Kotlin

**Eixo:** Qualidade/manutenibilidade  
**Localização:** `app/src/main/java/com/example/cepapplication/MainActivity.kt:107` e `:122`  
**Problema:** as mensagens de erro e sucesso não estão em `strings.xml`, ao contrário dos demais textos. Isso fragmenta a fonte dos textos e impede localização consistente.  
**Recomendação:** criar recursos como `cep_invalido_error` e `cep_salvo_success` e acessá-los com `getString(...)`.

### [BAIXO] Números de domínio estão espalhados como literais

**Eixo:** Qualidade/legibilidade  
**Localização:** `app/src/main/java/com/example/cepapplication/MainActivity.kt:106`, `:130` e `:132`  
**Problema:** os valores `8` e `5` representam regras do formato do CEP, mas seu significado depende do contexto.  
**Recomendação:** centralizar as regras no formatter/validator (`CEP_DIGIT_COUNT`, `CEP_PREFIX_LENGTH`) ou encapsulá-las em um tipo `Cep`.

### [BAIXO] Há recursos não utilizados e configuração de autofill indefinida

**Eixo:** Qualidade/recursos  
**Localização:** `colors.xml:3`, `:4`, `:8`; `dimens.xml:4`, `:13`; `activity_main.xml:55`  
**Problema:** o Lint apontou cinco recursos sem uso e ausência de decisão explícita sobre autofill no campo de CEP. Recursos mortos geram ruído e confundem manutenção.  
**Recomendação:** remover os recursos realmente desnecessários e marcar o campo com `android:importantForAutofill="no"` se CEP não deve ser preenchido pelo serviço, ou escolher um hint apropriado conforme a experiência desejada.

### [BAIXO] Nomenclatura de recursos é inconsistente

**Eixo:** Qualidade/convenções  
**Localização:** `app/src/main/res/values/strings.xml:7`  
**Problema:** `cepSalvo_text` mistura camelCase e snake_case, enquanto recursos Android normalmente usam snake_case. IDs genéricos como `text_main` e `btn_text` descrevem o tipo/posição, não a intenção.  
**Recomendação:** preferir nomes semânticos e consistentes, por exemplo `saved_cep_text`, `cep_label`, `save_cep_button` e `cep_hint`.

### [BAIXO] Documentação e comentários ainda são de template ou redundantes

**Eixo:** Qualidade/documentação  
**Localização:** `README.md:1`; `activity_main.xml:2`; arquivos de teste  
**Problema:** o README contém somente o título; comentários no layout repetem o XML; testes e regras de backup mantêm conteúdo de template. Isso aumenta ruído sem explicar decisões reais.  
**Recomendação:** documentar objetivo, requisitos, execução e decisões importantes no README; remover comentários que apenas traduzem a linha seguinte; substituir testes de exemplo; definir conscientemente a política de backup antes de produção.

## Evidências de verificação

- `gradlew.bat test`: sucesso; 1 teste, 0 falhas, 0 erros, 0 ignorados. O teste não cobre lógica da aplicação.
- `gradlew.bat lint`: sucesso; 0 erros e 9 avisos.
- Avisos adicionais do Lint: `targetSdk` abaixo do SDK mais recente disponível no ambiente e versões mais novas de AppCompat e ConstraintLayout. Atualizações devem ser feitas com teste de compatibilidade, não apenas para silenciar avisos.
- Testes instrumentados não foram executados porque não havia execução em dispositivo/emulador neste escopo.

## O que está correto

- Métodos curtos e com nomes que comunicam intenção.
- Constantes privadas para nome e chave de `SharedPreferences`.
- View Binding habilitado e acesso ao binding encapsulado.
- Uma única função concentra a regra atual de formatação, evitando duplicação.
- Validação impede salvar CEP com quantidade diferente de oito dígitos.
- A maior parte de cores, dimensões e textos já está em recursos Android.
- Lint e compilação das variantes analisadas não apresentam erros.

## Ordem sugerida de melhoria

1. Extrair formatter/validator puros e criar testes unitários significativos.
2. Corrigir a regra de entrada excedente e cobrir colagem/edição por testes.
3. Mover textos hardcoded para recursos e padronizar nomes.
4. Adicionar testes de UI para erro, sucesso e restauração.
5. Remover recursos e comentários mortos; ampliar o README.
6. Só então avaliar `ViewModel`/repositório, proporcionalmente ao crescimento da tela.
