# Hardening — Visibilidade do CEP salvo

## Regra implementada

- Quando não existe CEP salvo, o texto "CEP Salvo:" não deve aparecer.
- Quando existe CEP salvo, o texto deve ser exibido com o valor formatado.

## Alterações

- `MainActivity.kt`: `displaySavedZipCode` alterna a visibilidade do componente entre `GONE` e `VISIBLE` de acordo com a existência do CEP.
- `activity_main.xml`: o componente inicia oculto para não exibir conteúdo vazio durante a abertura da tela.
- `SavedZipCodeVisibilityTest.kt`: cobre os cenários sem CEP e com CEP.

## Verificação

Comando executado:

```text
.\gradlew.bat testDebugUnitTest assembleDebug
```

Resultado: `BUILD SUCCESSFUL`.

## Gaps da especificação

Nenhum gap identificado para a regra solicitada.

## Fora do escopo

Não foram alteradas a máscara, a validação, a persistência nem a formatação do CEP.
