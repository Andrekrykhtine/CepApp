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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
    fun `keeps previous address during loading and clears input on success`() = runTest {
        val deferredAddress = CompletableDeferred<Address?>()
        val repository = FakeCepRepository(lookup = { _ -> deferredAddress.await() })
        val viewModel = createViewModel(repository)

        viewModel.search("01001-000")
        runCurrent()
        assertEquals(CepUiState.Loading, viewModel.screenState.value.status)
        assertEquals("01001-000", viewModel.screenState.value.input)
        assertEquals(null, viewModel.screenState.value.lastSuccessfulAddress)

        val address = address()
        deferredAddress.complete(address)
        advanceUntilIdle()
        assertEquals(CepUiState.Success(address), viewModel.screenState.value.status)
        assertEquals("", viewModel.screenState.value.input)
        assertEquals(address, viewModel.screenState.value.lastSuccessfulAddress)
        assertEquals(CepFeedback.Type.LookupCompleted, viewModel.screenState.value.feedback?.type)
    }

    @Test
    fun `moves to error with identifiable cause when zip code is invalid`() = runTest {
        val viewModel = createViewModel(FakeCepRepository(lookup = { _ -> address() }))

        viewModel.search("01001-00")
        advanceUntilIdle()

        val state = viewModel.screenState.value.status
        assertTrue(state is CepUiState.Error)
        assertTrue((state as CepUiState.Error).cause is InvalidCepException)
        assertEquals("01001-00", viewModel.screenState.value.input)
        assertEquals(null, viewModel.screenState.value.lastSuccessfulAddress)
    }

    @Test
    fun `preserves address and input when a later search fails`() = runTest {
        val firstAddress = address()
        val repository = FakeCepRepository(lookup = { zipCode ->
            if (zipCode == firstAddress.zipCode) firstAddress else throw IllegalStateException("Falha")
        })
        val viewModel = createViewModel(repository)

        viewModel.search("01001-000")
        advanceUntilIdle()
        viewModel.search("20000-000")
        advanceUntilIdle()

        assertTrue(viewModel.screenState.value.status is CepUiState.Error)
        assertEquals("20000-000", viewModel.screenState.value.input)
        assertEquals(firstAddress, viewModel.screenState.value.lastSuccessfulAddress)
    }

    @Test
    fun `consumes completion feedback only when the identifier matches`() = runTest {
        val viewModel = createViewModel(FakeCepRepository(lookup = { address() }))

        viewModel.search("01001-000")
        advanceUntilIdle()
        val feedback = requireNotNull(viewModel.screenState.value.feedback)

        viewModel.consumeFeedback(feedback.id + 1)
        assertEquals(feedback, viewModel.screenState.value.feedback)
        viewModel.consumeFeedback(feedback.id)
        assertEquals(null, viewModel.screenState.value.feedback)
    }

    @Test
    fun `keeps completed state available to a new collector`() = runTest {
        val viewModel = createViewModel(FakeCepRepository(lookup = { address() }))

        viewModel.search("01001-000")
        advanceUntilIdle()

        assertEquals(CepUiState.Success(address()), viewModel.screenState.value.status)
        assertEquals(address(), viewModel.screenState.value.lastSuccessfulAddress)
    }

    @Test
    fun `restores first saved address without performing a lookup or completion feedback`() = runTest {
        var lookupCalls = 0
        val restoredAddress = address()
        val viewModel = createViewModel(
            FakeCepRepository(
                lookup = { lookupCalls += 1; address() },
                storedAddresses = flowOf(listOf(restoredAddress)),
            ),
        )

        viewModel.loadLatestAddress()
        advanceUntilIdle()

        assertEquals(CepUiState.Success(restoredAddress), viewModel.screenState.value.status)
        assertEquals(restoredAddress, viewModel.screenState.value.lastSuccessfulAddress)
        assertEquals("", viewModel.screenState.value.input)
        assertEquals(null, viewModel.screenState.value.feedback)
        assertEquals(0, lookupCalls)
    }

    @Test
    fun `does not let delayed restoration replace a search state`() = runTest {
        val delayedAddresses = CompletableDeferred<List<Address>>()
        val viewModel = createViewModel(
            FakeCepRepository(
                lookup = { throw IllegalStateException("Falha da pesquisa") },
                storedAddresses = flow { emit(delayedAddresses.await()) },
            ),
        )

        viewModel.loadLatestAddress()
        runCurrent()
        viewModel.search("01001-00")
        advanceUntilIdle()
        delayedAddresses.complete(listOf(address()))
        advanceUntilIdle()

        assertTrue(viewModel.screenState.value.status is CepUiState.Error)
        assertTrue((viewModel.screenState.value.status as CepUiState.Error).cause is InvalidCepException)
        assertEquals(null, viewModel.screenState.value.lastSuccessfulAddress)
    }

    @Test
    fun `does not let delayed restoration replace a valid search loading state`() = runTest {
        val delayedAddresses = CompletableDeferred<List<Address>>()
        val delayedLookup = CompletableDeferred<Address?>()
        val viewModel = createViewModel(
            FakeCepRepository(
                lookup = { delayedLookup.await() },
                storedAddresses = flow { emit(delayedAddresses.await()) },
            ),
        )

        viewModel.loadLatestAddress()
        runCurrent()
        viewModel.search("01001-000")
        runCurrent()
        delayedAddresses.complete(listOf(address()))
        advanceUntilIdle()

        assertEquals(CepUiState.Loading, viewModel.screenState.value.status)
        delayedLookup.complete(address())
        advanceUntilIdle()
    }

    @Test
    fun `preserves observed addresses when a later read fails`() = runTest {
        val savedAddress = address()
        val readFailure = IllegalStateException("Falha de leitura")
        val viewModel = createViewModel(
            FakeCepRepository(
                lookup = { address() },
                storedAddresses = flow {
                    emit(listOf(savedAddress))
                    throw readFailure
                },
            ),
        )

        advanceUntilIdle()

        assertEquals(listOf(savedAddress), viewModel.savedAddressesState.value.addresses)
        assertTrue(viewModel.savedAddressesState.value.hasLoaded)
        assertEquals(readFailure, viewModel.savedAddressesState.value.readError)
    }

    @Test
    fun `allows a new restoration read after an initial empty result`() = runTest {
        val flows = mutableListOf<Flow<List<Address>>>(
            flowOf(emptyList()),
            flowOf(emptyList()),
            flowOf(listOf(address())),
        )
        val viewModel = createViewModel(
            FakeCepRepository(
                lookup = { address() },
                storedAddressesProvider = { flows.removeAt(0) },
            ),
        )

        advanceUntilIdle()
        viewModel.loadLatestAddress()
        advanceUntilIdle()
        assertEquals(CepUiState.Idle, viewModel.screenState.value.status)

        viewModel.loadLatestAddress()
        advanceUntilIdle()
        assertEquals(CepUiState.Success(address()), viewModel.screenState.value.status)
    }

    @Test
    fun falhaNaRestauracaoPublicaErroSemEscapar() = runTest {
        val failure = IllegalStateException("Banco indisponível")
        val model = createViewModel(FakeCepRepository(
            lookup = { address() }, storedAddresses = flow { throw failure },
        ))
        model.loadLatestAddress()
        advanceUntilIdle()
        assertEquals(CepUiState.Error(failure), model.screenState.value.status)
        assertEquals(failure, model.screenState.value.feedback?.cause)
        model.search("01001-000")
        advanceUntilIdle()
        assertEquals(CepUiState.Success(address()), model.screenState.value.status)
    }

    @Test
    fun reabrirListaRetomaLeituraAposFalhaSemConsultarEndereco() = runTest {
        var subscriptions = 0
        var lookups = 0
        val model = createViewModel(FakeCepRepository(
            lookup = { lookups++; address() },
            storedAddressesProvider = {
                subscriptions++
                if (subscriptions == 1) flow {
                    emit(listOf(address()))
                    throw IllegalStateException("Leitura interrompida")
                } else flowOf(emptyList())
            },
        ))
        advanceUntilIdle()
        assertEquals(listOf(address()), model.savedAddressesState.value.addresses)
        val event = requireNotNull(model.savedAddressesState.value.errorFeedbackId)
        model.consumeSavedAddressesError(event)
        assertEquals(null, model.savedAddressesState.value.errorFeedbackId)
        model.observeSavedAddresses()
        advanceUntilIdle()
        assertEquals(2, subscriptions)
        assertEquals(0, lookups)
        assertTrue(model.savedAddressesState.value.hasLoaded)
        assertEquals(emptyList<Address>(), model.savedAddressesState.value.addresses)
        assertEquals(null, model.savedAddressesState.value.readError)
    }

    @Test
    fun feedbackDeFalhaNaoRetornaAoEditarENovaTentativaTemNovoEvento() = runTest {
        val model = createViewModel(FakeCepRepository(lookup = { throw java.io.IOException() }))
        model.search("01001-000")
        advanceUntilIdle()
        val first = requireNotNull(model.screenState.value.feedback)
        model.consumeFeedback(first.id)
        model.updateInput("20000-000")
        assertEquals(null, model.screenState.value.feedback)
        model.search("20000-000")
        advanceUntilIdle()
        assertTrue(requireNotNull(model.screenState.value.feedback).id > first.id)
    }

    @Test
    fun cancelamentoDaRestauracaoNaoViraErro() = runTest {
        val model = createViewModel(FakeCepRepository(
            lookup = { address() },
            storedAddresses = flow { throw kotlinx.coroutines.CancellationException() },
        ))
        model.loadLatestAddress()
        advanceUntilIdle()
        assertEquals(CepUiState.Idle, model.screenState.value.status)
        assertEquals(null, model.screenState.value.feedback)
    }

    @Test
    fun falhaAtrasadaDaRestauracaoNaoSobrescrevePesquisa() = runTest {
        val release = CompletableDeferred<Unit>()
        val model = createViewModel(FakeCepRepository(
            lookup = { address() },
            storedAddresses = flow {
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { release.await() }
                throw IllegalStateException("Falha atrasada")
            },
        ))
        model.loadLatestAddress()
        runCurrent()
        model.search("01001-000")
        advanceUntilIdle()
        release.complete(Unit)
        advanceUntilIdle()
        assertEquals(CepUiState.Success(address()), model.screenState.value.status)
    }

    private fun createViewModel(repository: CepRepository) = CepViewModel(
        getAddressByCep = GetAddressByCepUseCase(repository),
        getSavedAddresses = GetSavedAddressesUseCase(repository),
    )

    private class FakeCepRepository(
        private val lookup: suspend (String) -> Address?,
        private val storedAddresses: Flow<List<Address>> =
            MutableStateFlow<List<Address>>(emptyList()).asStateFlow(),
        private val storedAddressesProvider: (() -> Flow<List<Address>>)? = null,
    ) : CepRepository {
        override suspend fun getAddress(zipCode: String): Address? = lookup(zipCode)

        override fun observeSavedAddresses(): Flow<List<Address>> =
            storedAddressesProvider?.invoke() ?: storedAddresses
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
