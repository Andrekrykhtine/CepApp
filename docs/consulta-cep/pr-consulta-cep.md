# Conecta consulta de CEP à API pública ViaCEP

## Descrição da PR — STAR

### Situação

O aplicativo precisava buscar informações complementares de endereço a partir do CEP informado pelo usuário.

### Tarefa

Integrar uma API REST pública utilizando Retrofit e Coroutines, mantendo o fluxo assíncrono e a separação entre UI, domínio e dados.

> **Observação:** a ViaCEP consulta CEPs, não nomes. Por isso, este documento considera o CEP digitado como entrada da consulta.

### Ação

- Configurado o cliente Retrofit para comunicação com a [ViaCEP](https://viacep.com.br/).
- Criado o serviço responsável pela consulta de CEP.
- Implementada a conversão da resposta da API para o modelo de domínio.
- Adicionado tratamento para CEP inexistente, erros HTTP e falhas de rede.
- Mantido o acesso assíncrono por meio de Coroutines.
- Integrada a consulta ao fluxo existente de ViewModel e repositório.
- Preservado o resultado anterior quando uma nova consulta falha.

### Resultado

- O app consulta a ViaCEP para CEPs não encontrados localmente.
- Respostas válidas são exibidas e armazenadas para consultas futuras.
- CEPs já salvos podem ser consultados sem nova chamada à API.
- Erros de rede ou HTTP não interrompem o fluxo nem apagam o resultado anterior.
- A integração permanece testável por meio de testes unitários com API controlada.

## Validação

- Testes unitários executados com API remota controlada.
- Cenários de sucesso, CEP inexistente, erro HTTP e falha de rede cobertos.
- Nenhuma consulta depende da disponibilidade da ViaCEP pública durante os testes.
