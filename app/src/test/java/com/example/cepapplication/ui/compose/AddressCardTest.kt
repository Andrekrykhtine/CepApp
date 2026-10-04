package com.example.cepapplication.ui.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.example.cepapplication.domain.model.Address
import com.example.cepapplication.formatAddress
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AddressCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun lightTheme_rendersTitleAndSevenFormattedLines() {
        val address = filledAddress()

        composeRule.setContent {
            CepTheme(darkTheme = false) {
                AddressCard(
                    address = address,
                    title = "Último CEP consultado",
                )
            }
        }

        composeRule.onNodeWithText("Último CEP consultado").assertIsDisplayed()
        assertAddressLines(address)
    }

    @Test
    fun darkTheme_rendersSevenLinesAndNotInformedValues() {
        val address = emptyFieldsAddress()

        composeRule.setContent {
            CepTheme(darkTheme = true) {
                AddressCard(address = address)
            }
        }

        assertAddressLines(address)
    }

    private fun assertAddressLines(address: Address) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val lines = context.formatAddress(address).lines()
        assertEquals(7, lines.size)

        lines.forEach { line ->
            composeRule.onNodeWithText(line).assertIsDisplayed()
        }
    }

    private fun filledAddress() = Address(
        zipCode = "01001000",
        street = "Praça da Sé",
        complement = "lado ímpar",
        neighborhood = "Sé",
        city = "São Paulo",
        stateAbbreviation = "SP",
        state = "São Paulo",
    )

    private fun emptyFieldsAddress() = Address(
        zipCode = "30140071",
        street = "",
        complement = "",
        neighborhood = "",
        city = "",
        stateAbbreviation = "",
        state = "",
    )
}
