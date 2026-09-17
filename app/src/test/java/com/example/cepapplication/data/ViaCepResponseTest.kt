package com.example.cepapplication.data

import com.example.cepapplication.domain.model.Address
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AddressDtoTest {
    @Test
    fun desserializaJsonRealComErroNulosECamposAusentes() {
        val gson = com.google.gson.Gson()
        assertNull(gson.fromJson("""{"erro": true}""", AddressDto::class.java).toDomain())
        assertNull(gson.fromJson("""{"erro": "true"}""", AddressDto::class.java).toDomain())
        val result = gson.fromJson(
            """{"cep":"01001-000","logradouro":null,"localidade":"São Paulo","uf":"SP"}""",
            AddressDto::class.java,
        ).toDomain()!!
        assertEquals("01001000", result.zipCode)
        assertEquals("", result.street)
        assertEquals("", result.complement)
        assertEquals("São Paulo", result.city)
    }

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

    @Test
    fun `maps partial response keeping complementary fields empty`() {
        val response = AddressDto(
            cep = "01001-000",
            localidade = "São Paulo",
            uf = "SP",
        )

        assertEquals(
            Address(
                zipCode = "01001000",
                street = "",
                complement = "",
                neighborhood = "",
                city = "São Paulo",
                stateAbbreviation = "SP",
                state = "",
            ),
            response.toDomain(),
        )
    }

    @Test
    fun `does not map a response without a zip code`() {
        assertNull(AddressDto(localidade = "São Paulo").toDomain())
    }
}
