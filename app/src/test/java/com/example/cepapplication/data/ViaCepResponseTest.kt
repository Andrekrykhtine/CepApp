package com.example.cepapplication.data

import com.example.cepapplication.domain.model.Address
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AddressDtoTest {
    @Test
    fun `maps a successful response to an address`() {
        val response = AddressDto(
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
                zipCode = "01001000",
                street = "Praça da Sé",
                complement = "lado ímpar",
                neighborhood = "Sé",
                city = "São Paulo",
                stateAbbreviation = "SP",
                state = "São Paulo",
            ),
            response.toDomain(),
        )
    }

    @Test
    fun `does not map a response marked as not found`() {
        assertNull(AddressDto(hasError = true).toDomain())
    }
}
