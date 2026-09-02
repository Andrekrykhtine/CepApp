package com.example.cepapplication.ui

import com.example.cepapplication.domain.model.Address
import com.example.cepapplication.domain.repository.CepRepository
import com.example.cepapplication.domain.usecase.GetAddressByCepUseCase
import com.example.cepapplication.domain.usecase.GetSavedAddressesUseCase
import com.example.cepapplication.domain.usecase.InvalidCepException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CepViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `moves from loading to success when address is found`() = runTest {
        val deferredAddress = CompletableDeferred<Address?>()
        val repository = FakeCepRepository { deferredAddress.await() }
        val viewModel = createViewModel(repository)

        viewModel.search("01001-000")
        runCurrent()
        assertEquals(CepUiState.Loading, viewModel.uiState.value)

        val address = address()
        deferredAddress.complete(address)
        advanceUntilIdle()
        assertEquals(CepUiState.Success(address), viewModel.uiState.value)
    }

    @Test
    fun `moves to error with identifiable cause when zip code is invalid`() = runTest {
        val viewModel = createViewModel(FakeCepRepository { address() })

        viewModel.search("01001-00")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CepUiState.Error)
        assertTrue((state as CepUiState.Error).cause is InvalidCepException)
    }

    private fun createViewModel(repository: CepRepository) = CepViewModel(
        getAddressByCep = GetAddressByCepUseCase(repository),
        getSavedAddresses = GetSavedAddressesUseCase(repository),
    )

    private class FakeCepRepository(
        private val lookup: suspend (String) -> Address?,
        private val storedAddresses: MutableStateFlow<List<Address>> = MutableStateFlow(emptyList()),
    ) : CepRepository {
        override suspend fun getAddress(zipCode: String): Address? = lookup(zipCode)

        override fun observeSavedAddresses(): Flow<List<Address>> = storedAddresses.asStateFlow()
    }

    private fun address() = Address(
        zipCode = "01001000",
        street = "Praça da Sé",
        complement = "lado ímpar",
        neighborhood = "Sé",
        city = "São Paulo",
        stateAbbreviation = "SP",
        state = "São Paulo",
    )
}
