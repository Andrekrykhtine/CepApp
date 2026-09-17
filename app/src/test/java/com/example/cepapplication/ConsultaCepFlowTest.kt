package com.example.cepapplication

import com.example.cepapplication.data.local.CepLocalDataSource
import com.example.cepapplication.data.local.LocalStorageException
import com.example.cepapplication.data.local.RoomCepLocalDataSource
import com.example.cepapplication.data.repository.CepRepositoryImpl
import com.example.cepapplication.domain.model.Address
import com.example.cepapplication.domain.usecase.CepNotFoundException
import com.example.cepapplication.domain.usecase.GetAddressByCepUseCase
import com.example.cepapplication.domain.usecase.GetSavedAddressesUseCase
import com.example.cepapplication.testing.IsolatedRoomDatabase
import com.example.cepapplication.ui.CepUiState
import com.example.cepapplication.ui.CepViewModel
import com.example.cepapplication.ui.MainDispatcherRule
import java.io.IOException
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import org.junit.After
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConsultaCepFlowTest {
    private val models = mutableListOf<CepViewModel>()

    @After
    fun closeModels() { models.forEach { it.viewModelScope.cancel() } }

    private suspend fun CepViewModel.awaitConclusion() = withContext(Dispatchers.Default) {
        withTimeout(5_000) { screenState.first { it.status is CepUiState.Success || it.status is CepUiState.Error } }
    }

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `cep invalido nao acessa remoto nem banco e preserva entrada`() = runTest {
        val remote = FlowRemoteDataSource()
        val local = RecordingLocalDataSource()
        val viewModel = viewModel(CepRepositoryImpl(local, remote))

        viewModel.search("12.345-67")
        advanceUntilIdle()

        assertTrue(viewModel.screenState.value.status is CepUiState.Error)
        assertEquals("12.345-67", viewModel.screenState.value.input)
        assertEquals(0, local.findCalls)
        assertTrue(remote.requestedZipCodes.isEmpty())
    }

    @Test
    fun `miss remoto salva e sucesso limpa entrada`() = runTest {
        val address = address("01001000")
        IsolatedRoomDatabase.create(RuntimeEnvironment.getApplication(), beforeClose = ::closeModels).use { fixture ->
            val remote = FlowRemoteDataSource(addresses = mapOf(address.zipCode to address))
            val viewModel = viewModel(repository(fixture, remote))

            viewModel.search("01001-000")
            runCurrent()
            viewModel.awaitConclusion()

            assertEquals(CepUiState.Success(address), viewModel.screenState.value.status)
            assertEquals("", viewModel.screenState.value.input)
            assertEquals(listOf(address), repository(fixture, FlowRemoteDataSource()).observeSavedAddresses().first())
            assertEquals(listOf("01001000"), remote.requestedZipCodes)
        }
    }

    @Test
    fun `hit local antigo e offline nao acessa remoto e registra recencia`() = runTest {
        val old = address("01001000")
        val other = address("20000000", street = "Rua A")
        IsolatedRoomDatabase.create(RuntimeEnvironment.getApplication(), beforeClose = ::closeModels).use { fixture ->
            val firstRemote = FlowRemoteDataSource(addresses = mapOf(old.zipCode to old, other.zipCode to other))
            val repository = repository(fixture, firstRemote)
            repository.getAddress(old.zipCode)
            repository.getAddress(other.zipCode)

            val offline = FlowRemoteDataSource(failure = IOException("offline"))
            val offlineRepository = CepRepositoryImpl(
                localDataSource = RoomCepLocalDataSource(fixture.database.addressDao()),
                remoteDataSource = offline,
            )
            assertEquals(old, offlineRepository.getAddress(old.zipCode))
            assertTrue(offline.requestedZipCodes.isEmpty())
            assertEquals(listOf(old, other), repository.observeSavedAddresses().first())
        }
    }

    @Test
    fun `reconsulta A mantem dois registros move A ao topo e restaura em nova instancia`() = runTest {
        val a = address("01001000")
        val b = address("20000000", street = "Rua B")
        IsolatedRoomDatabase.create(RuntimeEnvironment.getApplication(), beforeClose = ::closeModels).use { fixture ->
            val remote = FlowRemoteDataSource(addresses = mapOf(a.zipCode to a, b.zipCode to b))
            val repository = repository(fixture, remote)
            repository.getAddress(a.zipCode)
            repository.getAddress(b.zipCode)
            repository.getAddress(a.zipCode)

            assertEquals(listOf(a, b), repository.observeSavedAddresses().first())
            fixture.reopen()
            val restored = viewModel(repository(fixture, remote))
            restored.loadLatestAddress()
            runCurrent()
            restored.awaitConclusion()
            assertEquals(CepUiState.Success(a), restored.screenState.value.status)
            assertEquals(listOf("01001000", "20000000"), remote.requestedZipCodes)
        }
    }

    @Test
    fun `falha HTTP preserva A e nao salva B`() = runTest {
        val a = address("01001000")
        IsolatedRoomDatabase.create(RuntimeEnvironment.getApplication(), beforeClose = ::closeModels).use { fixture ->
            val remote = FlowRemoteDataSource(addresses = mapOf(a.zipCode to a))
            val repository = repository(fixture, remote)
            val viewModel = viewModel(repository)
            viewModel.search(a.zipCode)
            runCurrent()
            viewModel.awaitConclusion()

            remote.failure = HttpException(Response.error<Any>(500, "falha".toResponseBody()))
            viewModel.search("20000-000")
            runCurrent()
            viewModel.awaitConclusion()

            assertTrue(viewModel.screenState.value.status is CepUiState.Error)
            assertEquals(a, viewModel.screenState.value.lastSuccessfulAddress)
            assertEquals(listOf(a), repository.observeSavedAddresses().first())
        }
    }

    @Test
    fun `cep inexistente nao cria registro`() = runTest {
        IsolatedRoomDatabase.create(RuntimeEnvironment.getApplication(), beforeClose = ::closeModels).use { fixture ->
            val remote = FlowRemoteDataSource(addresses = emptyMap())
            val repository = repository(fixture, remote)
            val viewModel = viewModel(repository)

            viewModel.search("99999-999")
            runCurrent()
            viewModel.awaitConclusion()

            val error = viewModel.screenState.value.status as CepUiState.Error
            assertTrue(error.cause is CepNotFoundException)
            assertTrue(repository.observeSavedAddresses().first().isEmpty())
        }
    }

    @Test
    fun `dados parciais concluem com endereco armazenado`() = runTest {
        val partial = address("01001000", complement = "", neighborhood = "")
        IsolatedRoomDatabase.create(RuntimeEnvironment.getApplication(), beforeClose = ::closeModels).use { fixture ->
            val viewModel = viewModel(
                repository(fixture, FlowRemoteDataSource(addresses = mapOf(partial.zipCode to partial))),
            )

            viewModel.search(partial.zipCode)
            runCurrent()
            viewModel.awaitConclusion()

            assertEquals(CepUiState.Success(partial), viewModel.screenState.value.status)
            assertEquals(partial, viewModel.screenState.value.lastSuccessfulAddress)
        }
    }

    @Test
    fun `loading aguarda resposta e conclui com entrada limpa`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val address = address("01001000")
        val remote = FlowRemoteDataSource(addresses = mapOf(address.zipCode to address), responseGate = gate)
        IsolatedRoomDatabase.create(RuntimeEnvironment.getApplication(), beforeClose = ::closeModels).use { fixture ->
            val viewModel = viewModel(repository(fixture, remote))
            viewModel.search("01001000")
            runCurrent()
            assertEquals(CepUiState.Loading, viewModel.screenState.value.status)
            assertEquals("01001000", viewModel.screenState.value.input)

            gate.complete(Unit)
            runCurrent()
            viewModel.awaitConclusion()
            assertEquals(CepUiState.Success(address), viewModel.screenState.value.status)
            assertEquals("", viewModel.screenState.value.input)
        }
    }

    @Test
    fun `falha de leitura ou gravacao local nao vira miss nem sucesso`() = runTest {
        val readFailure = LocalStorageException(IllegalStateException("read"))
        val local = RecordingLocalDataSource(readFailure = readFailure, writeFailure = readFailure)
        val remote = FlowRemoteDataSource(addresses = mapOf("01001000" to address("01001000")))
        val viewModel = viewModel(CepRepositoryImpl(local, remote))

        viewModel.search("01001000")
        advanceUntilIdle()

        assertTrue(viewModel.screenState.value.status is CepUiState.Error)
        assertTrue(remote.requestedZipCodes.isEmpty())
        assertEquals(0, local.saveCalls)
    }

    @Test
    fun `restauracao e leitura da lista nao fazem nova consulta nem escrita`() = runTest {
        val saved = address("01001000")
        val remote = FlowRemoteDataSource()
        val local = RecordingLocalDataSource(observedAddresses = flowOf(listOf(saved)))
        val viewModel = viewModel(CepRepositoryImpl(local, remote))

        viewModel.loadLatestAddress()
        advanceUntilIdle()
        viewModel.savedAddresses.first()

        assertEquals(CepUiState.Success(saved), viewModel.screenState.value.status)
        assertEquals(0, local.findCalls)
        assertEquals(0, local.saveCalls)
        assertTrue(remote.requestedZipCodes.isEmpty())
    }

    @Test
    fun `restauracao atrasada nao sobrescreve pesquisa iniciada depois`() = runTest {
        val delayedAddresses = CompletableDeferred<List<Address>>()
        val local = RecordingLocalDataSource(
            observedAddresses = flow { emit(delayedAddresses.await()) },
        )
        val remote = FlowRemoteDataSource()
        val viewModel = viewModel(CepRepositoryImpl(local, remote))

        viewModel.loadLatestAddress()
        runCurrent()
        viewModel.search("01001-00")
        advanceUntilIdle()
        delayedAddresses.complete(listOf(address("01001000")))
        advanceUntilIdle()

        assertTrue(viewModel.screenState.value.status is CepUiState.Error)
        assertEquals("01001-00", viewModel.screenState.value.input)
        assertTrue(remote.requestedZipCodes.isEmpty())
    }

    private fun viewModel(repository: com.example.cepapplication.domain.repository.CepRepository) = CepViewModel(
        GetAddressByCepUseCase(repository),
        GetSavedAddressesUseCase(repository),
    ).also { models += it }

    private fun repository(
        fixture: IsolatedRoomDatabase,
        remote: FlowRemoteDataSource,
    ) = CepRepositoryImpl(RoomCepLocalDataSource(fixture.database.addressDao()), remote, currentTimeMillis = { 0L })

    private fun address(
        zipCode: String,
        street: String = "Praça da Sé",
        complement: String = "lado ímpar",
        neighborhood: String = "Sé",
    ) = Address(zipCode, street, complement, neighborhood, "São Paulo", "SP", "São Paulo")

    private class FlowRemoteDataSource(
        private val addresses: Map<String, Address> = emptyMap(),
        var failure: Throwable? = null,
        private val responseGate: CompletableDeferred<Unit>? = null,
    ) : com.example.cepapplication.data.remote.CepRemoteDataSource {
        val requestedZipCodes = mutableListOf<String>()

        override suspend fun findByZipCode(zipCode: String): Address? {
            requestedZipCodes += zipCode
            responseGate?.await()
            failure?.let { throw it }
            return addresses[zipCode]
        }
    }

    private class RecordingLocalDataSource(
        private val observedAddresses: Flow<List<Address>> = flowOf(emptyList()),
        private val readFailure: Throwable? = null,
        private val writeFailure: Throwable? = null,
    ) : CepLocalDataSource {
        var findCalls = 0
        var saveCalls = 0

        override suspend fun findAndRecordConsultation(zipCode: String): Address? {
            findCalls++
            readFailure?.let { throw it }
            return null
        }

        override suspend fun saveAndRecordConsultation(address: Address, savedAtEpochMillis: Long): Address {
            saveCalls++
            writeFailure?.let { throw it }
            return address
        }

        override fun observeAll(): Flow<List<Address>> = observedAddresses
    }
}
