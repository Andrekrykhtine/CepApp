# `MainActivity.kt` explicado linha a linha

Arquivo analisado: [`MainActivity.kt`](../app/src/main/java/com/example/cepapplication/MainActivity.kt).

Esta classe controla a única tela do aplicativo: aplica a máscara `00000-000`, valida se há oito dígitos, persiste o CEP no armazenamento local e o mostra na tela.

## Linhas 1 a 9 — pacote e importações

| Linha | Código | Explicação |
| --- | --- | --- |
| 1 | `package com.example.cepapplication` | Define o pacote Kotlin ao qual este arquivo pertence. |
| 2 | *(vazia)* | Separa visualmente a declaração do pacote das importações. |
| 3 | `import android.os.Bundle` | Importa `Bundle`, objeto que pode conter o estado anterior da Activity. |
| 4 | `import android.view.View` | Importa `View`, usado para os valores de visibilidade `VISIBLE` e `GONE`. |
| 5 | `import android.widget.Toast` | Importa o componente de mensagem breve exibida na parte inferior da tela. |
| 6 | `import androidx.appcompat.app.AppCompatActivity` | Importa a classe base compatível usada pela tela principal. |
| 7 | `import androidx.core.content.edit` | Importa a extensão Kotlin que simplifica a edição de `SharedPreferences`. |
| 8 | `import androidx.core.widget.doAfterTextChanged` | Importa a extensão chamada depois que o texto do campo é alterado. |
| 9 | `import com.example.cepapplication.databinding.ActivityMainBinding` | Importa a classe gerada pelo View Binding para acessar as Views de `activity_main.xml` com segurança de tipos. |

## Linhas 11 a 24 — classe e dependências locais

| Linha | Código | Explicação |
| --- | --- | --- |
| 11 | `class MainActivity : AppCompatActivity() {` | Declara a tela inicial do app. A herança de `AppCompatActivity` dá acesso ao ciclo de vida e aos recursos de Activity. |
| 12 | *(vazia)* | Separa a abertura da classe dos seus membros. |
| 13 | `private companion object {` | Cria uma área estática privada: suas constantes pertencem à classe, não a uma instância específica da tela. |
| 14 | `const val PREFS_NAME = "app_data"` | Define o nome do arquivo de preferências locais. |
| 15 | `const val ZIP_CODE_KEY = "saved_zip_code"` | Define a chave usada para gravar e recuperar o CEP nesse arquivo. |
| 16 | `}` | Fecha o `companion object`. |
| 18 | `private val binding by lazy {` | Declara a referência ao layout. `lazy` só a cria no primeiro acesso. |
| 19 | `ActivityMainBinding.inflate(layoutInflater)` | Infla o XML `activity_main.xml` e retorna objetos para suas Views, como `btnSave` e `edtZipCode`. |
| 20 | `}` | Fecha o bloco que inicializa o binding. |
| 22 | `private val sharedPrefs by lazy {` | Declara o acesso às preferências persistentes; ele também será criado somente quando necessário. |
| 23 | `getSharedPreferences(PREFS_NAME, MODE_PRIVATE)` | Abre as preferências de nome `app_data`. `MODE_PRIVATE` limita o acesso ao próprio aplicativo. |
| 24 | `}` | Fecha o bloco de inicialização de `sharedPrefs`. |

## Linhas 26 a 36 — inicialização da tela

| Linha | Código | Explicação |
| --- | --- | --- |
| 26 | `override fun onCreate(savedInstanceState: Bundle?) {` | Sobrescreve o primeiro método de ciclo de vida chamado ao criar a tela. |
| 27 | `super.onCreate(savedInstanceState)` | Executa a inicialização padrão da classe pai e repassa qualquer estado salvo. |
| 28 | `setContentView(binding.root)` | Define a raiz do layout ligado ao View Binding como conteúdo visível da Activity. |
| 29 | `setSupportActionBar(binding.toolbarMain)` | Registra a barra `toolbarMain` do XML como a barra superior da Activity. |
| 30 | `setupZipCodeMask()` | Instala o ouvinte que formata o CEP enquanto o usuário digita. |
| 31 | `loadZipCode()` | Carrega e apresenta o CEP persistido em uma execução anterior, se houver. |
| 32 | *(vazia)* | Separa as inicializações do registro do clique do botão. |
| 33 | `binding.btnSave.setOnClickListener {` | Define o que acontece quando o botão Salvar recebe um toque. |
| 34 | `saveZipCode()` | Chama o método que valida e persiste o valor digitado. |
| 35 | `}` | Fecha o listener de clique. |
| 36 | `}` | Fecha o método `onCreate`. |

## Linhas 38 a 54 — máscara do CEP

| Linha | Código | Explicação |
| --- | --- | --- |
| 38 | `private fun setupZipCodeMask() {` | Declara o método que configura a formatação automática do campo. |
| 39 | `var isUpdating = false` | Cria uma trava para distinguir uma alteração feita pelo código de uma digitação do usuário. |
| 41 | `binding.edtZipCode.doAfterTextChanged { editable ->` | Registra uma função executada após toda mudança no campo; `editable` contém o texto resultante. |
| 42 | `if (isUpdating) return@doAfterTextChanged` | Se a mudança foi feita pela própria máscara, sai do listener para impedir recursão infinita. |
| 44 | `val currentText = editable?.toString().orEmpty()` | Converte o texto atual em `String`; se `editable` for nulo, usa uma string vazia. |
| 45 | `val formattedText = formatZipCode(currentText)` | Calcula como o texto deveria ficar segundo a regra de CEP. |
| 47 | `if (currentText != formattedText) {` | Só altera a View quando a versão formatada for diferente da atual. |
| 48 | `isUpdating = true` | Ativa a trava antes de modificar programaticamente o conteúdo do campo. |
| 49 | `binding.edtZipCode.setText(formattedText)` | Substitui o texto do campo pelo valor contendo apenas dígitos e, quando cabível, hífen. |
| 50 | `binding.edtZipCode.setSelection(formattedText.length)` | Posiciona o cursor no fim do texto recém-formatado. |
| 51 | `isUpdating = false` | Desativa a trava para permitir futuras alterações do usuário. |
| 52 | `}` | Fecha a condição que atualiza o campo. |
| 53 | `}` | Fecha o listener de texto. |
| 54 | `}` | Fecha o método de configuração da máscara. |

## Linhas 56 a 69 — carregamento e apresentação

| Linha | Código | Explicação |
| --- | --- | --- |
| 56 | `private fun loadZipCode() {` | Declara o método que restaura o último CEP salvo. |
| 57 | `val savedZipCode = sharedPrefs.getString(ZIP_CODE_KEY, "").orEmpty()` | Busca o valor na preferência. Se a chave não existir ou o resultado for nulo, utiliza texto vazio. |
| 58 | `val formattedZipCode = formatZipCode(savedZipCode)` | Garante que mesmo um valor persistido apareça no padrão `00000-000`. |
| 59 | `displaySavedZipCode(formattedZipCode)` | Atualiza a tela com o CEP restaurado. |
| 60 | `}` | Fecha `loadZipCode`. |
| 62 | `private fun displaySavedZipCode(zipCode: String) {` | Declara o método responsável por mostrar ou ocultar o texto do CEP salvo. |
| 63 | `binding.txtSavedZipCode.visibility = if (shouldDisplaySavedZipCode(zipCode)) {` | Decide a visibilidade com base em uma regra extraída para uma função testável. |
| 64 | `View.VISIBLE` | Caso haja CEP, o TextView aparece e ocupa espaço no layout. |
| 65 | `} else {` | Inicia o caso em que não há CEP. |
| 66 | `View.GONE` | Esconde o TextView e remove o espaço que ele ocuparia. |
| 67 | `}` | Fecha a expressão condicional de visibilidade. |
| 68 | `binding.txtSavedZipCode.text = getString(R.string.cep_salvo_text, zipCode)` | Resolve o recurso `CEP Salvo: %1$s`, substitui `%1$s` pelo CEP e define o texto da View. |
| 69 | `}` | Fecha `displaySavedZipCode`. |

## Linhas 71 a 87 — validação e salvamento

| Linha | Código | Explicação |
| --- | --- | --- |
| 71 | `private fun saveZipCode() {` | Declara o método chamado pelo botão Salvar. |
| 72 | `val zipCode = binding.edtZipCode.text.toString().filter { it.isDigit() }` | Lê o campo e remove todos os caracteres não numéricos — inclusive o hífen da máscara. |
| 74 | `if (zipCode.length != 8) {` | Confere se há exatamente os oito dígitos exigidos para um CEP. |
| 75 | `binding.edtZipCode.error = getString(R.string.error_invalid_zip_code)` | Mostra a mensagem de erro associada ao próprio campo. |
| 76 | `return` | Interrompe o método, portanto nenhum valor inválido é salvo. |
| 77 | `}` | Fecha a validação. |
| 79 | `sharedPrefs.edit {` | Abre uma transação de edição das preferências com a extensão Kotlin. Ao fechar o bloco, a alteração é aplicada. |
| 80 | `putString(ZIP_CODE_KEY, zipCode)` | Grava os oito dígitos puros sob a chave `saved_zip_code`. |
| 81 | `}` | Fecha e aplica a edição. |
| 83 | `displaySavedZipCode(formatZipCode(zipCode))` | Formata o CEP e atualiza imediatamente o texto visível na tela. |
| 84 | `binding.edtZipCode.text.clear()` | Limpa o campo de entrada após um salvamento bem-sucedido. |
| 86 | `Toast.makeText(this, getString(R.string.toast_zip_code_saved), Toast.LENGTH_SHORT).show()` | Cria e exibe por pouco tempo a mensagem de confirmação. `this` é a Activity usada como contexto. |
| 87 | `}` | Fecha `saveZipCode`. |

## Linhas 89 a 99 — formatação e regra testável

| Linha | Código | Explicação |
| --- | --- | --- |
| 89 | `private fun formatZipCode(value: String): String {` | Declara uma função privada que recebe qualquer texto e devolve um CEP formatado. |
| 90 | `val numbers = value.filter { it.isDigit() }.take(8)` | Mantém somente dígitos e limita o resultado a oito caracteres. |
| 91 | `return if (numbers.length > 5) {` | Se existirem pelo menos seis dígitos, retorna a versão com hífen depois dos cinco primeiros. |
| 92 | `"${numbers.substring(0, 5)}-${numbers.substring(5)}"` | Monta o formato `00000-000`: primeiros cinco dígitos, hífen e o restante. |
| 93 | `} else {` | Trata valores de até cinco dígitos. |
| 94 | `numbers` | Retorna os números sem hífen, pois o CEP ainda não chegou à posição do separador. |
| 95 | `}` | Fecha a condição. |
| 96 | `}` | Fecha `formatZipCode`. |
| 97 | `}` | Fecha a classe `MainActivity`. |
| 98 | *(vazia)* | Separa a classe da função de nível superior. |
| 99 | `internal fun shouldDisplaySavedZipCode(zipCode: String): Boolean = zipCode.isNotBlank()` | Declara uma função acessível dentro do módulo. Ela retorna `true` quando o CEP contém algo além de espaços; assim pode ser testada sem instanciar a Activity. |

## Resumo do fluxo

1. A Activity cria o layout e configura a toolbar, a máscara e o clique do botão.
2. O CEP salvo anteriormente é lido de `SharedPreferences` e mostrado, se existir.
3. Ao digitar, o campo remove caracteres inválidos e adiciona o hífen após o quinto dígito.
4. Ao salvar, o app exige oito números, persiste o valor localmente e atualiza a interface.
