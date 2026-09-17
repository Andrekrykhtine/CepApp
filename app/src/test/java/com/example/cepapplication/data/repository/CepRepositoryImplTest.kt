package com.example.cepapplication.data.repository

import com.example.cepapplication.data.local.CepLocalDataSource
import com.example.cepapplication.data.remote.CepRemoteDataSource
import com.example.cepapplication.domain.model.Address
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import kotlin.coroutines.cancellation.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class CepRepositoryImplTest {
    @Test
    fun `returns local address of any age and records consultation without calling remote`() = runBlocking {
        val localAddress = address(street = "Local")
        val local = FakeLocalDataSource(localResult = localAddress)
        val remote = FakeRemoteDataSource(address(street = "Remoto"))

        val result = repository(local, remote).getAddress("01001000")

        assertEquals(localAddress, result)
        assertEquals(listOf("01001000"), local.findAndRecordCalls)
        assertEquals(emptyList<SavedAddress>(), local.saved)
        assertEquals(0, remote.calls)
    }

    @Test
    fun `calls remote and saves response before returning on local miss`() = runBlocking {
        val remoteAddress = address()
        val operations = mutableListOf<String>()
        val local = FakeLocalDataSource(events = operations)
        val remote = FakeRemoteDataSource(remoteAddress, events = operations)

        val result = repository(local, remote).getAddress("01001000")

        assertEquals(remoteAddress, result)
        assertEquals(listOf("01001000"), local.findAndRecordCalls)
        assertEquals(1, remote.calls)
        assertEquals(listOf(SavedAddress(remoteAddress, NOW)), local.saved)
        assertEquals(listOf("local", "remote", "save"), operations)
    }

    @Test
    fun `does not save when remote source does not find address`() = runBlocking {
        val local = FakeLocalDataSource()

        val result = repository(local, FakeRemoteDataSource(null)).getAddress("01001000")

        assertNull(result)
        assertEquals(emptyList<SavedAddress>(), local.saved)
    }

    @Test
    fun `does not call remote when local read fails`() = runBlocking {
        val localFailure = IllegalStateException("falha local")
        val local = FakeLocalDataSource(localError = localFailure)
        val remote = FakeRemoteDataSource(address())

        val thrown = expectFailure { repository(local, remote).getAddress("01001000") }

        assertEquals(localFailure, thrown)
        assertEquals(0, remote.calls)
        assertEquals(emptyList<SavedAddress>(), local.saved)
    }

    @Test
    fun `does not return remote address when saving it fails`() = runBlocking {
        val saveFailure = IllegalStateException("falha de salvamento")
        val remoteAddress = address()
        val local = FakeLocalDataSource(saveError = saveFailure)
        val remote = FakeRemoteDataSource(remoteAddress)

        val thrown = expectFailure { repository(local, remote).getAddress("01001000") }

        assertEquals(saveFailure, thrown)
        assertEquals(1, remote.calls)
        assertEquals(listOf(SavedAddress(remoteAddress, NOW)), local.saveAttempts)
        assertEquals(emptyList<SavedAddress>(), local.saved)
    }

    @Test
    fun `propagates remote failure without local fallback`() = runBlocking {
        val remoteFailure = IOException("sem conexão")
        val local = FakeLocalDataSource()
        val remote = FakeRemoteDataSource(error = remoteFailure)

        val thrown = expectFailure { repository(local, remote).getAddress("01001000") }

        assertEquals(remoteFailure, thrown)
        assertEquals(1, remote.calls)
        assertEquals(emptyList<SavedAddress>(), local.saved)
    }

    @Test
    fun `propagates remote http failure without saving`() = runBlocking {
        val remoteFailure = HttpException(
            Response.error<Any>(500, "falha".toResponseBody("text/plain".toMediaType())),
        )
        val local = FakeLocalDataSource()

        val thrown = expectFailure {
            repository(local, FakeRemoteDataSource(error = remoteFailure)).getAddress("01001000")
        }

        assertEquals(remoteFailure, thrown)
        assertEquals(emptyList<SavedAddress>(), local.saved)
    }

    @Test(expected = CancellationException::class)
    fun `propagates cancellation from local lookup without calling remote`() = runBlocking<Unit> {
        val local = FakeLocalDataSource(localError = CancellationException("cancelado"))
        val remote = FakeRemoteDataSource(address())

        repository(local, remote).getAddress("01001000")
    }

    @Test(expected = CancellationException::class)
    fun `propagates cancellation from remote lookup without saving`() = runBlocking<Unit> {
        val local = FakeLocalDataSource()
        val remote = FakeRemoteDataSource(error = CancellationException("cancelado"))

        repository(local, remote).getAddress("01001000")
    }

    @Test(expected = CancellationException::class)
    fun `propagates cancellation from local save without returning success`() = runBlocking<Unit> {
        val local = FakeLocalDataSource(saveError = CancellationException("cancelado"))

        repository(local, FakeRemoteDataSource(address())).getAddress("01001000")
    }

    private class FakeLocalDataSource(
        private val localResult: Address? = null,
        private val localError: Throwable? = null,
        private val saveError: Throwable? = null,
        private val events: MutableList<String> = mutableListOf(),
    ) : CepLocalDataSource {
        val findAndRecordCalls = mutableListOf<String>()
        val saveAttempts = mutableListOf<SavedAddress>()
        val saved = mutableListOf<SavedAddress>()

        override suspend fun findAndRecordConsultation(zipCode: String): Address? {
            findAndRecordCalls += zipCode
            events += "local"
            localError?.let { throw it }
            return localResult
        }

        override suspend fun saveAndRecordConsultation(address: Address, savedAtEpochMillis: Long): Address {
            val savedAddress = SavedAddress(address, savedAtEpochMillis)
            saveAttempts += savedAddress
            events += "save"
            saveError?.let { throw it }
            saved += savedAddress
            return address
        }

        override fun observeAll(): Flow<List<Address>> = flowOf(emptyList())
    }

    private class FakeRemoteDataSource(
        private val result: Address? = null,
        private val error: Throwable? = null,
        private val events: MutableList<String> = mutableListOf(),
    ) : CepRemoteDataSource {
        var calls: Int = 0

        override suspend fun findByZipCode(zipCode: String): Address? {
            calls += 1
            events += "remote"
            error?.let { throw it }
            return result
        }
    }

    private fun repository(
        local: CepLocalDataSource,
        remote: CepRemoteDataSource,
    ) = CepRepositoryImpl(
        localDataSource = local,
        remoteDataSource = remote,
        currentTimeMillis = { NOW },
    )

    private suspend fun expectFailure(block: suspend () -> Unit): Throwable =
        runCatching { block() }.exceptionOrNull()
            ?: throw AssertionError("A operação deveria falhar")

    private data class SavedAddress(
        val address: Address,
        val savedAtEpochMillis: Long,
    )

    private fun address(street: String = "Praça da Sé") = Address(
        zipCode = "01001000",
        street = street,
        complement = "lado ímpar",
        neighborhood = "Sé",
        city = "São Paulo",
        stateAbbreviation = "SP",
        state = "São Paulo",
    )

    private companion object {
        const val NOW = 100_000L
    }
}
