package com.example.cepapplication.ui

import androidx.lifecycle.SavedStateHandle
import com.example.cepapplication.domain.model.Address
import com.example.cepapplication.domain.usecase.GetAddressByCepUseCase
import com.example.cepapplication.domain.usecase.GetSavedAddressesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * Testes unitários do [CepViewModel] demonstrando:
 * 1. Execução rápida na JVM sem necessidade de emulador ou instrumentação Android.
 * 2. Uso do JUnit 5 com anotações modernas (@Test, @DisplayName, @BeforeEach, @RegisterExtension).
 * 3. Uso do MockK para criação de mocks e asserções de chamadas suspensas (coEvery, coVerify).
 * 4. Estratégias de teste para fluxos assíncronos e controle de tempo virtual com [runTest],
 *    [runCurrent], [advanceUntilIdle] e [MainDispatcherExtension].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("CepViewModel - Estratégias de Teste com JUnit 5, MockK, runTest e MainDispatcherExtension")
class CepViewModelJUnit5Test {

    @JvmField
    @RegisterExtension
    val mainDispatcherExtension = MainDispatcherExtension()

    private val getAddressByCep: GetAddressByCepUseCase = mockk()
    private val getSavedAddresses: GetSavedAddressesUseCase = mockk()

    @BeforeEach
    fun setUp() {
        every { getSavedAddresses() } returns flowOf(emptyList())
    }

    @Test
    @DisplayName("Deve iniciar com entrada vazia quando o SavedStateHandle não contiver estado prévio")
    fun `starts with empty input when no saved state exists`() = runTest {
        val handle = SavedStateHandle()
        val viewModel = CepViewModel(getAddressByCep, getSavedAddresses, handle)

        assertEquals("", viewModel.screenState.value.input)
        assertNull(handle.get<String>("cep_input"))
    }

    @Test
    @DisplayName("Deve atualizar a entrada digitada e sincronizar com SavedStateHandle sem iniciar busca")
    fun `updates input and synchronizes with handle without triggering search`() = runTest {
        val handle = SavedStateHandle()
        val viewModel = CepViewModel(getAddressByCep, getSavedAddresses, handle)

        viewModel.updateInput("01001-000")

        assertEquals("01001-000", viewModel.screenState.value.input)
        assertEquals("01001-000", handle.get<String>("cep_input"))
        coVerify(exactly = 0) { getAddressByCep(any()) }
    }

    @Test
    @DisplayName("Deve emitir estado Loading e em seguida Success ao realizar consulta com sucesso")
    fun `emits Loading and then Success upon successful lookup`() = runTest {
        val expectedAddress = Address(
            zipCode = "01001-000",
            street = "Praça da Sé",
            neighborhood = "Sé",
            city = "São Paulo",
            state = "SP",
        )
        coEvery { getAddressByCep("01001-000") } returns Result.success(expectedAddress)

        val handle = SavedStateHandle()
        val viewModel = CepViewModel(getAddressByCep, getSavedAddresses, handle)

        viewModel.search("01001-000")
        runCurrent()

        assertEquals(CepUiState.Loading, viewModel.screenState.value.status)
        assertEquals("01001-000", viewModel.screenState.value.input)

        advanceUntilIdle()

        assertEquals(CepUiState.Success(expectedAddress), viewModel.screenState.value.status)
        assertEquals("", viewModel.screenState.value.input)
        assertEquals("", handle.get<String>("cep_input"))
        assertEquals(expectedAddress, viewModel.screenState.value.lastSuccessfulAddress)
        coVerify(exactly = 1) { getAddressByCep("01001-000") }
    }

    @Test
    @DisplayName("Deve emitir estado Error e preservar endereço anterior quando a consulta falhar")
    fun `emits Error and preserves state when lookup fails`() = runTest {
        val error = RuntimeException("Erro de comunicação")
        coEvery { getAddressByCep("01001-000") } returns Result.failure(error)

        val handle = SavedStateHandle()
        val viewModel = CepViewModel(getAddressByCep, getSavedAddresses, handle)

        viewModel.search("01001-000")
        advanceUntilIdle()

        assertTrue(viewModel.screenState.value.status is CepUiState.Error)
        assertEquals("01001-000", viewModel.screenState.value.input)
        assertNull(viewModel.screenState.value.lastSuccessfulAddress)
        coVerify(exactly = 1) { getAddressByCep("01001-000") }
    }
}
