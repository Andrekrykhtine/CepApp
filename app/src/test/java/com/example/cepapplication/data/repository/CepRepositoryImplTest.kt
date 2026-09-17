package com.example.cepapplication.data.repository

import com.example.cepapplication.data.local.CepLocalDataSource
import com.example.cepapplication.data.local.CachedAddress
import com.example.cepapplication.data.remote.CepRemoteDataSource
import com.example.cepapplication.domain.model.Address
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class CepRepositoryImplTest {
    @Test
    fun `returns local address without calling remote source on cache hit`() = runBlocking {
        val cached = address()
        val local = FakeLocalDataSource(CachedAddress(cached, SAVED_AT))
        val remote = FakeRemoteDataSource(address(street = "Remoto"))

        val result = repository(local, remote).getAddress("01001000")

        assertEquals(cached, result)
        assertEquals(0, remote.calls)
        assertEquals(emptyList<SavedAddress>(), local.saved)
    }

    @Test
    fun `calls remote source and saves response on cache miss`() = runBlocking {
        val remoteAddress = address()
        val local = FakeLocalDataSource(null)
        val remote = FakeRemoteDataSource(remoteAddress)

        val result = repository(local, remote).getAddress("01001000")

        assertEquals(remoteAddress, result)
        assertEquals(1, remote.calls)
        assertEquals(listOf(SavedAddress(remoteAddress, NOW)), local.saved)
    }

    @Test
    fun `does not save when remote source does not find address`() = runBlocking {
        val local = FakeLocalDataSource(null)

        val result = repository(local, FakeRemoteDataSource(null))
            .getAddress("01001000")

        assertNull(result)
        assertEquals(emptyList<SavedAddress>(), local.saved)
    }

    @Test
    fun `refreshes and replaces an expired cached address`() = runBlocking {
        val cached = address(street = "Antigo")
        val refreshed = address(street = "Atualizado")
        val local = FakeLocalDataSource(CachedAddress(cached, EXPIRED_AT))
        val remote = FakeRemoteDataSource(refreshed)

        val result = repository(local, remote).getAddress("01001000")

        assertEquals(refreshed, result)
        assertEquals(1, remote.calls)
        assertEquals(listOf(SavedAddress(refreshed, NOW)), local.saved)
    }

    @Test
    fun `falls back to expired cache when refresh fails offline`() = runBlocking {
        val cached = address(street = "Disponível offline")
        val local = FakeLocalDataSource(CachedAddress(cached, EXPIRED_AT))
        val remote = FakeRemoteDataSource(error = IOException("sem conexão"))

        val result = repository(local, remote).getAddress("01001000")

        assertEquals(cached, result)
        assertEquals(1, remote.calls)
        assertEquals(emptyList<SavedAddress>(), local.saved)
    }

    private class FakeLocalDataSource(
        private val cached: CachedAddress?,
    ) : CepLocalDataSource {
        val saved = mutableListOf<SavedAddress>()

        override suspend fun findAndRecordConsultation(zipCode: String): Address? =
            error("O repositório passará a usar este contrato na T-003")

        override suspend fun saveAndRecordConsultation(address: Address, savedAtEpochMillis: Long): Address =
            error("O repositório passará a usar este contrato na T-003")

        override suspend fun findByZipCode(zipCode: String): CachedAddress? = cached

        override suspend fun save(address: Address, savedAtEpochMillis: Long) {
            saved += SavedAddress(address, savedAtEpochMillis)
        }

        override fun observeAll(): Flow<List<Address>> =
            flowOf(saved.map(SavedAddress::address))
    }

    private class FakeRemoteDataSource(
        private val result: Address? = null,
        private val error: IOException? = null,
    ) : CepRemoteDataSource {
        var calls: Int = 0

        override suspend fun findByZipCode(zipCode: String): Address? {
            calls += 1
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
        cacheTtlMillis = CACHE_TTL,
        currentTimeMillis = { NOW },
    )

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
        const val CACHE_TTL = 1_000L
        const val SAVED_AT = NOW - CACHE_TTL
        const val EXPIRED_AT = SAVED_AT - 1L
    }
}
