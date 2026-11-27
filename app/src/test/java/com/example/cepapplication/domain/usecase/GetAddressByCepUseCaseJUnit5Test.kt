package com.example.cepapplication.domain.usecase

import com.example.cepapplication.domain.model.Address
import com.example.cepapplication.domain.repository.CepRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.IOException

@DisplayName("GetAddressByCepUseCase - Testes Unitários com JUnit 5 e MockK na JVM")
class GetAddressByCepUseCaseJUnit5Test {

    private lateinit var repository: CepRepository
    private lateinit var useCase: GetAddressByCepUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk()
        useCase = GetAddressByCepUseCase(repository)
    }

    @Test
    @DisplayName("Deve normalizar o CEP válido e retornar sucesso quando encontrado no repositório")
    fun `normalizes valid zip code and returns success`() = runTest {
        val expectedAddress = Address(
            zipCode = "01001-000",
            street = "Praça da Sé",
            neighborhood = "Sé",
            city = "São Paulo",
            state = "SP",
        )
        coEvery { repository.getAddress("01001000") } returns expectedAddress

        val result = useCase("01001-000")

        assertTrue(result.isSuccess)
        assertEquals(expectedAddress, result.getOrNull())
        coVerify(exactly = 1) { repository.getAddress("01001000") }
    }

    @Test
    @DisplayName("Deve rejeitar CEP inválido antes de acessar o repositório")
    fun `rejects invalid zip code without calling repository`() = runTest {
        val result = useCase("01001-00")

        assertTrue(result.isFailure)
        assertInstanceOf(InvalidCepException::class.java, result.exceptionOrNull())
        coVerify(exactly = 0) { repository.getAddress(any()) }
    }

    @Test
    @DisplayName("Deve retornar falha de CepNotFoundException quando o endereço não for localizado")
    fun `returns failure when zip code is not found in repository`() = runTest {
        coEvery { repository.getAddress("01001000") } returns null

        val result = useCase("01001000")

        assertTrue(result.isFailure)
        assertInstanceOf(CepNotFoundException::class.java, result.exceptionOrNull())
        coVerify(exactly = 1) { repository.getAddress("01001000") }
    }

    @Test
    @DisplayName("Deve propagar falhas de repositório sem alterar seu tipo")
    fun `propagates repository exception without altering type`() = runTest {
        val networkError = IOException("Falha de conexão remota")
        coEvery { repository.getAddress("01001000") } throws networkError

        val result = useCase("01001000")

        assertTrue(result.isFailure)
        assertEquals(networkError, result.exceptionOrNull())
        coVerify(exactly = 1) { repository.getAddress("01001000") }
    }
}
