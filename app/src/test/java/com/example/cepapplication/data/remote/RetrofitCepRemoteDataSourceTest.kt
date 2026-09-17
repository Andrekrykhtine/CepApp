package com.example.cepapplication.data.remote

import com.example.cepapplication.data.AddressDto
import com.example.cepapplication.data.ViaCepApi
import com.example.cepapplication.domain.model.Address
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class RetrofitCepRemoteDataSourceTest {
    @Test
    fun `maps successful response from substitutable api`() = runBlocking {
        val api = FakeViaCepApi(
            result = AddressDto(
                cep = "01001-000",
                logradouro = "Praça da Sé",
                complemento = "lado ímpar",
                bairro = "Sé",
                localidade = "São Paulo",
                uf = "SP",
                estado = "São Paulo",
            ),
        )

        val result = RetrofitCepRemoteDataSource(api).findByZipCode("01001000")

        assertEquals(listOf("01001000"), api.requests)
        assertEquals(address(), result)
    }

    @Test
    fun `returns absence for api response marked as not found`() = runBlocking {
        val api = FakeViaCepApi(result = AddressDto(hasError = true))

        val result = RetrofitCepRemoteDataSource(api).findByZipCode("99999999")

        assertEquals(listOf("99999999"), api.requests)
        assertNull(result)
    }

    @Test
    fun `propagates connection failure from api`() = runBlocking {
        val failure = IOException("sem conexão")
        val api = FakeViaCepApi(error = failure)

        val thrown = expectFailure {
            RetrofitCepRemoteDataSource(api).findByZipCode("01001000")
        }

        assertSame(failure, thrown)
        assertEquals(listOf("01001000"), api.requests)
    }

    @Test
    fun `propagates http failure from api`() = runBlocking {
        val failure = HttpException(
            Response.error<Any>(500, "falha".toResponseBody("text/plain".toMediaType())),
        )
        val api = FakeViaCepApi(error = failure)

        val thrown = expectFailure {
            RetrofitCepRemoteDataSource(api).findByZipCode("01001000")
        }

        assertSame(failure, thrown)
        assertEquals(listOf("01001000"), api.requests)
    }

    private class FakeViaCepApi(
        private val result: AddressDto = AddressDto(),
        private val error: Throwable? = null,
    ) : ViaCepApi {
        val requests = mutableListOf<String>()

        override suspend fun findAddress(zipCode: String): AddressDto {
            requests += zipCode
            error?.let { throw it }
            return result
        }
    }

    private suspend fun expectFailure(block: suspend () -> Unit): Throwable =
        runCatching { block() }.exceptionOrNull()
            ?: throw AssertionError("A operação deveria falhar")

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
