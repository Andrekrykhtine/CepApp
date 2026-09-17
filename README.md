# Cep Application

![Version](https://img.shields.io/badge/version-1.0.0-blue)
![License](https://img.shields.io/badge/license-MIT-green)

## 📋 Sobre

O **Cep Application** é um aplicativo Android para consultar endereços pela ViaCEP, armazená-los localmente e visualizá-los posteriormente. O projeto separa interface, regras de negócio e acesso a dados, com armazenamento local permanente para endereços já consultados e funcionamento offline.

## 📑 Índice

- [Sobre](#-sobre)
- [Funcionalidades](#-funcionalidades)
- [Tecnologias](#-tecnologias)
- [Pré-requisitos](#-pré-requisitos)
- [Instalação](#-instalação)
- [Como usar](#-como-usar)
- [Estrutura de pastas](#-estrutura-de-pastas)
- [Licença](#-licença)
- [Autores](#-autores)

## ✨ Funcionalidades

- **Máscara Automática**: Formatação em tempo real do CEP (00000-000) durante a digitação.
- **Consulta ViaCEP**: Busca os dados completos do endereço pela internet.
- **Persistência Local**: Utiliza Room para manter os endereços entre sessões.
- **Consulta local prioritária**: CEPs já armazenados são retornados localmente, sem expiração e sem chamada à ViaCEP.
- **Recência persistente**: Toda consulta bem-sucedida, local ou remota, move o CEP para o início da lista e permite restaurá-lo entre sessões.
- **Funcionamento offline**: Consultas de CEPs já armazenados não dependem de conexão.
- **Lista Reativa**: A tela de CEPs armazenados acompanha automaticamente as mudanças do banco.
- **Validação de Entrada**: Bloqueio de salvamento para CEPs incompletos ou inválidos.
- **Interface Moderna**: Construída com `Material Design` e `ViewBinding`.

## ✅ Estado da implementação

O comportamento vigente segue o FRD de 07/09/2026: não há TTL de 24 horas, o armazenamento local tem prioridade e a recência representa a última consulta bem-sucedida. A migração de dados legados e os fluxos de persistência são mantidos conforme a implementação atual.

Após as correções da revisão, em 07/09/2026, **80 testes locais passaram e o build debug concluiu** pelo Gradle Wrapper. A suíte inclui Room real, migrações e telas com Robolectric, sem ADB ou emulador. Evidências em [relatório de testes](docs/consulta-cep/test-report-consulta-cep.md) e [validação](docs/consulta-cep/validacao-consulta-cep.md). As tentativas anteriores bloqueadas por ambiente foram preservadas como histórico. Essa execução não certifica a disponibilidade da ViaCEP pública nem o comportamento em hardware.

## 🛠️ Tecnologias

- **Kotlin**: Linguagem de programação moderna e concisa.
- **Android SDK**: Ferramentas e APIs para desenvolvimento Android.
- **ViewBinding**: Integração segura entre código e layout XML.
- **Material Components**: Design de interface seguindo padrões modernos.
- **Navigation Component**: Navegação entre pesquisa e endereços armazenados.
- **ViewModel e StateFlow**: Estado da interface e atualizações reativas.
- **Retrofit**: Integração com a ViaCEP.
- **Room**: Banco de dados local e migrações de schema.
- **Coroutines**: Execução assíncrona das consultas.

## 📦 Pré-requisitos

Para rodar o projeto, você precisará de:

- **Android Studio** (Versão Ladybug | 2024.2.1 ou superior).
- **JDK 11** ou superior.
- Dispositivo físico ou emulador com **Android 7.0 (API 24)** ou superior.

## 🚀 Instalação

```bash
# Clone o repositório
git clone https://github.com/usuario/cep-application.git

# Entre na pasta do projeto
cd cep-application

# Realize o build via terminal (opcional)
./gradlew assembleDebug
```

## 💻 Como usar

1. Abra o projeto no **Android Studio**.
2. Execute o app em um emulador ou dispositivo real.
3. No campo de texto, insira os 8 dígitos de um CEP.
4. Clique em **Consultar e salvar**.
5. O endereço aparecerá na tela e ficará disponível na seção **CEPs armazenados**.

## 📁 Estrutura de pastas

```text
CepApplication/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/cepapplication/
│   │   │   │   ├── ui/                   # ViewModel e estados da interface
│   │   │   │   ├── domain/               # Modelos, regras e contratos
│   │   │   │   ├── data/                 # Room, Retrofit e repositório
│   │   │   │   ├── MainActivity.kt       # Navegação e ViewModel compartilhado
│   │   │   │   ├── SearchFragment.kt     # Consulta de CEP
│   │   │   │   └── SavedAddressesFragment.kt
│   │   │   └── res/
│   │   │       ├── layout/
│   │   │       │   ├── activity_main.xml
│   │   │       │   ├── fragment_search.xml
│   │   │       │   └── fragment_saved_addresses.xml
│   │   │       └── values/               # Recursos de cores, strings e dimensões
│   └── build.gradle.kts                  # Configurações do módulo app
├── build.gradle.kts                      # Configurações do projeto nível raiz
└── settings.gradle.kts                   # Configurações de módulos e repositórios
```

## ✍️ Autores

- **Andre Krykhtine Peres** - *Desenvolvedor*
