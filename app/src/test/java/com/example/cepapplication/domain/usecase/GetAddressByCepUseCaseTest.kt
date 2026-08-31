package com.example.cepapplication.domain.usecase

import com.example.cepapplication.domain.model.Address
import com.example.cepapplication.domain.repository.CepRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class GetAddressByCepUseCaseTest {
    @Test
    fun `normalizes a valid zip code and returns success`() = runBlocking {
        val address = address()
        val repository = FakeCepRepository(address)

        val result = GetAddressByCepUseCase(repository)("01001-000")

        assertEquals(address, result.getOrThrow())
        assertEquals("01001000", repository.requestedZipCode)
    }

    @Test
    fun `rejects invalid zip code before accessing repository`() = runBlocking {
        val repository = FakeCepRepository(address())

        val result = GetAddressByCepUseCase(repository)("01001-00")

        assertTrue(result.exceptionOrNull() is InvalidCepException)
        assertEquals(null, repository.requestedZipCode)
    }

    @Test
    fun `returns identifiable failure when address is not found`() = runBlocking {
        val result = GetAddressByCepUseCase(FakeCepRepository(null))("01001000")

        assertTrue(result.exceptionOrNull() is CepNotFoundException)
    }

    @Test(expected = CancellationException::class)
    fun `does not convert coroutine cancellation into result failure`() = runBlocking<Unit> {
        val repository = object : CepRepository {
            override suspend fun getAddress(zipCode: String): Address? {
                throw CancellationException("cancelado")
            }

            override suspend fun getSavedAddresses(): List<Address> = emptyList()
        }

        GetAddressByCepUseCase(repository)("01001000")
    }

    private class FakeCepRepository(
        private val result: Address?,
    ) : CepRepository {
        var requestedZipCode: String? = null

        override suspend fun getAddress(zipCode: String): Address? {
            requestedZipCode = zipCode
            return result
        }

        override suspend fun getSavedAddresses(): List<Address> = emptyList()
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
