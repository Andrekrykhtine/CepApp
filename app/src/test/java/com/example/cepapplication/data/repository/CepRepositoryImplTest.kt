package com.example.cepapplication.data.repository

import com.example.cepapplication.data.local.CepLocalDataSource
import com.example.cepapplication.data.remote.CepRemoteDataSource
import com.example.cepapplication.domain.model.Address
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CepRepositoryImplTest {
    @Test
    fun `returns local address without calling remote source on cache hit`() = runBlocking {
        val cached = address()
        val local = FakeLocalDataSource(cached)
        val remote = FakeRemoteDataSource(address(street = "Remoto"))

        val result = CepRepositoryImpl(local, remote).getAddress("01001000")

        assertEquals(cached, result)
        assertEquals(0, remote.calls)
        assertEquals(emptyList<Address>(), local.saved)
    }

    @Test
    fun `calls remote source and saves response on cache miss`() = runBlocking {
        val remoteAddress = address()
        val local = FakeLocalDataSource(null)
        val remote = FakeRemoteDataSource(remoteAddress)

        val result = CepRepositoryImpl(local, remote).getAddress("01001000")

        assertEquals(remoteAddress, result)
        assertEquals(1, remote.calls)
        assertEquals(listOf(remoteAddress), local.saved)
    }

    @Test
    fun `does not save when remote source does not find address`() = runBlocking {
        val local = FakeLocalDataSource(null)

        val result = CepRepositoryImpl(local, FakeRemoteDataSource(null))
            .getAddress("01001000")

        assertNull(result)
        assertEquals(emptyList<Address>(), local.saved)
    }

    private class FakeLocalDataSource(
        private val cached: Address?,
    ) : CepLocalDataSource {
        val saved = mutableListOf<Address>()

        override suspend fun findByZipCode(zipCode: String): Address? = cached

        override suspend fun save(address: Address) {
            saved += address
        }

        override suspend fun findAll(): List<Address> = saved
    }

    private class FakeRemoteDataSource(
        private val result: Address?,
    ) : CepRemoteDataSource {
        var calls: Int = 0

        override suspend fun findByZipCode(zipCode: String): Address? {
            calls += 1
            return result
        }
    }

    private fun address(street: String = "Praça da Sé") = Address(
        zipCode = "01001000",
        street = street,
        complement = "lado ímpar",
        neighborhood = "Sé",
        city = "São Paulo",
        stateAbbreviation = "SP",
        state = "São Paulo",
    )
}
