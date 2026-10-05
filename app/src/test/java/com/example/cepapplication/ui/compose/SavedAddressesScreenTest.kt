package com.example.cepapplication.ui.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.fetchSemanticsNodes
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.example.cepapplication.domain.model.Address
import com.example.cepapplication.ui.SavedAddressesState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SavedAddressesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun notLoaded_doesNotShowTitleOrEmptyMessage() {
        setContent(SavedAddressesState())

        composeRule.onNodeWithText("CEPs consultados").assertDoesNotExist()
        composeRule.onNodeWithText("Nenhum CEP armazenado.").assertDoesNotExist()
    }

    @Test
    fun loadedWithoutAddresses_showsOnlyEmptyMessage() {
        setContent(SavedAddressesState(hasLoaded = true))

        composeRule.onNodeWithText("Nenhum CEP armazenado.").assertIsDisplayed()
        composeRule.onNodeWithText("CEPs consultados").assertDoesNotExist()
    }

    @Test
    fun populatedState_rendersTwoCardsInReceivedOrder() {
        val first = address("01001000", "Primeiro")
        val second = address("20000000", "Segundo")
        setContent(
            SavedAddressesState(
                addresses = listOf(first, second),
                hasLoaded = true,
            ),
        )

        composeRule.onNodeWithText("CEPs consultados").assertIsDisplayed()
        assertZipCodesInOrder(first, second)
        composeRule.onNodeWithText("Nenhum CEP armazenado.").assertDoesNotExist()
    }

    @Test
    fun changingState_recomposesCardsInNewOrder() {
        val first = address("01001000", "Primeiro")
        val second = address("20000000", "Segundo")
        var state by mutableStateOf(
            SavedAddressesState(
                addresses = listOf(first, second),
                hasLoaded = true,
            ),
        )

        composeRule.setContent {
            CepTheme {
                SavedAddressesScreen(state = state)
            }
        }
        assertZipCodesInOrder(first, second)

        state = state.copy(addresses = listOf(second, first))
        composeRule.waitForIdle()

        assertZipCodesInOrder(second, first)
    }

    @Test
    fun readErrorAfterPreviousData_keepsCardsAndDoesNotShowEmptyMessage() {
        val previous = address("01001000", "Anterior")
        setContent(
            SavedAddressesState(
                addresses = listOf(previous),
                hasLoaded = true,
                readError = IllegalStateException("falha de leitura"),
            ),
        )

        composeRule.onNodeWithText("CEPs consultados").assertIsDisplayed()
        composeRule.onNodeWithText("CEP: 01001-000").assertIsDisplayed()
        composeRule.onNodeWithText("Nenhum CEP armazenado.").assertDoesNotExist()
    }

    private fun setContent(state: SavedAddressesState) {
        composeRule.setContent {
            CepTheme {
                SavedAddressesScreen(state = state)
            }
        }
    }

    private fun assertZipCodesInOrder(first: Address, second: Address) {
        val firstNode = zipCodeNodes(first).single()
        val secondNode = zipCodeNodes(second).single()

        assertTrue(
            "Os cartões não estão na ordem recebida",
            firstNode.boundsInRoot.top < secondNode.boundsInRoot.top,
        )
    }

    private fun zipCodeNodes(address: Address) =
        composeRule
            .onAllNodesWithText("CEP: ${formattedZipCode(address.zipCode)}", useUnmergedTree = true)
            .fetchSemanticsNodes()

    private fun formattedZipCode(zipCode: String) =
        "${zipCode.take(5)}-${zipCode.drop(5)}"

    private fun address(zipCode: String, street: String) = Address(
        zipCode = zipCode,
        street = street,
        complement = "Complemento",
        neighborhood = "Bairro",
        city = "Cidade",
        stateAbbreviation = "SP",
        state = "São Paulo",
    )
}
