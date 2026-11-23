package com.example.cepapplication.di

import com.example.cepapplication.domain.repository.CepRepository
import com.example.cepapplication.domain.usecase.GetAddressByCepUseCase
import com.example.cepapplication.domain.usecase.GetSavedAddressesUseCase
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.koin.core.context.stopKoin
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@DisplayName("Koin DI - Teste de Definição e Resolução de Módulos com JUnit 5")
class KoinModuleJUnit5Test {

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    @DisplayName("Deve conter todas as definições de módulos do aplicativo")
    fun `verifies modules declaration structure`() {
        assertNotNull(dataModule)
        assertNotNull(useCaseModule)
        assertNotNull(viewModelModule)
        assertNotNull(appModules)
    }

    @Test
    @DisplayName("Deve resolver casos de uso a partir do container Koin com repositório fornecido")
    fun `resolves use cases from koin container with mocked repository`() {
        val testModule = module {
            single<CepRepository> { mockk(relaxed = true) }
        }

        val koinApp = koinApplication {
            modules(testModule, useCaseModule)
        }

        val getAddressByCep = koinApp.koin.get<GetAddressByCepUseCase>()
        val getSavedAddresses = koinApp.koin.get<GetSavedAddressesUseCase>()

        assertNotNull(getAddressByCep)
        assertNotNull(getSavedAddresses)
    }
}
