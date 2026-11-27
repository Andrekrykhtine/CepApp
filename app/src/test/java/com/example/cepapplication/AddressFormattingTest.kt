package com.example.cepapplication

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.cepapplication.domain.model.Address
import org.junit.Assert.assertEquals
import org.junit.Test

class AddressFormattingTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun formatAddress_formatsCepAndAllAddressFieldsInOrder() {
        val address = Address(
            zipCode = "01001000",
            street = "Praça da Sé",
            complement = "lado ímpar",
            neighborhood = "Sé",
            city = "São Paulo",
            stateAbbreviation = "SP",
            state = "São Paulo",
        )

        assertEquals(
            listOf(
                "CEP: 01001-000",
                "Logradouro: Praça da Sé",
                "Complemento: lado ímpar",
                "Bairro: Sé",
                "Localidade: São Paulo",
                "UF: SP",
                "Estado: São Paulo",
            ),
            context.formatAddress(address).lines(),
        )
    }

    @Test
    fun formatAddress_usesNotInformedForEveryEmptyTextField() {
        val address = Address(
            zipCode = "30140071",
            street = "",
            complement = "",
            neighborhood = "",
            city = "",
            stateAbbreviation = "",
            state = "",
        )

        assertEquals(
            listOf(
                "CEP: 30140-071",
                "Logradouro: Não informado",
                "Complemento: Não informado",
                "Bairro: Não informado",
                "Localidade: Não informado",
                "UF: Não informado",
                "Estado: Não informado",
            ),
            context.formatAddress(address).lines(),
        )
    }
}
