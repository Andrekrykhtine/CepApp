package com.example.cepapplication.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ViaCepResponseTest {
    @Test
    fun `maps a successful response to an address`() {
        val response = ViaCepResponse(
            cep = "01001-000",
            logradouro = "Praça da Sé",
            complemento = "lado ímpar",
            bairro = "Sé",
            localidade = "São Paulo",
            uf = "SP",
            estado = "São Paulo",
        )

        assertEquals(
            Address(
                zipCode = "01001-000",
                street = "Praça da Sé",
                complement = "lado ímpar",
                neighborhood = "Sé",
                city = "São Paulo",
                stateAbbreviation = "SP",
                state = "São Paulo",
            ),
            response.toAddress(),
        )
    }

    @Test
    fun `does not map a response marked as not found`() {
        assertNull(ViaCepResponse(hasError = true).toAddress())
    }
}
