package com.example.cepapplication.ui.compose

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import com.example.cepapplication.R
import com.example.cepapplication.domain.model.Address
import com.example.cepapplication.domain.repository.CepRepository
import com.example.cepapplication.domain.usecase.GetAddressByCepUseCase
import com.example.cepapplication.domain.usecase.GetSavedAddressesUseCase
import com.example.cepapplication.ui.CepViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
class CepAppTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var repository: FakeCepRepository
    private lateinit var viewModel: CepViewModel

    @Before
    fun setUp() {
        ShadowToast.reset()
    }

    @Test
    fun routesShowTitlesAndToolbarBackPreservesInput() {
        launch()

        composeRule.onNodeWithText("Busca CEP").assertIsDisplayed()
        composeRule.onNode(hasSetTextAction()).performTextInput("01001")
        composeRule.onNodeWithText("CEPs armazenados").performClick()

        composeRule.onNodeWithText("CEPs armazenados").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Voltar").performClick()
        composeRule.onNodeWithText("Busca CEP").assertIsDisplayed()
        composeRule.onNode(hasSetTextAction()).assertTextEquals("01001-")
    }

    @Test
    fun restorationShowsLatestAddressWithoutLookupOrSuccessToast() {
        launch(savedAddresses = flowOfAddress(address()))
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Último CEP consultado").assertIsDisplayed()
        assertEquals(0, repository.lookupCalls)
        assertEquals(0, ShadowToast.shownToastCount())

        composeRule.onNodeWithText("CEPs armazenados").performClick()
        composeRule.onNodeWithContentDescription("Voltar").performClick()
        assertEquals(0, repository.lookupCalls)
        assertEquals(0, ShadowToast.shownToastCount())
    }

    @Test
    fun recoverableTaskRestoresPartialInputWithoutStartingLookup() {
        launch(savedInput = "01001-")
        composeRule.waitForIdle()

        composeRule.onNode(hasSetTextAction()).assertTextEquals("01001-")
        assertEquals(0, repository.lookupCalls)
        assertEquals(0, ShadowToast.shownToastCount())
    }

    @Test
    fun eachLookupFeedbackIsShownOnceAndInvalidInputIsOnlyToast() {
        launch(lookup = address())
        composeRule.onNode(hasSetTextAction()).performTextInput("123")
        composeRule.onNodeWithText("Consultar e salvar").performClick()
        waitForToast(R.string.error_invalid_zip_code)
        assertEquals(1, ShadowToast.shownToastCount())
        composeRule.waitForIdle()
        assertEquals(1, ShadowToast.shownToastCount())
        composeRule.onNodeWithText("Digite um CEP válido").assertDoesNotExist()

        composeRule.onNode(hasSetTextAction()).performTextClearance()
        composeRule.onNode(hasSetTextAction()).performTextInput("01001000")
        composeRule.onNodeWithText("Consultar e salvar").performClick()
        waitForToast(R.string.toast_zip_code_saved)
        assertEquals(2, ShadowToast.shownToastCount())
        composeRule.waitForIdle()
        assertEquals(2, ShadowToast.shownToastCount())
    }

    @Test
    fun notFoundUsesToastAndDoesNotShowInlineError() {
        launch(lookup = null)
        composeRule.onNode(hasSetTextAction()).performTextInput("01001000")
        composeRule.onNodeWithText("Consultar e salvar").performClick()

        waitForToast(R.string.error_zip_code_not_found)
        composeRule.onNodeWithText("CEP não encontrado").assertDoesNotExist()
        assertEquals("01001-000", viewModel.screenState.value.input)
    }

    @Test
    fun listReadErrorKeepsPreviousCardsAndShowsOneGenericToast() {
        launch(savedAddresses = flow {
            emit(listOf(address()))
            throw IllegalStateException("falha de leitura")
        })
        composeRule.waitForIdle()
        ShadowToast.reset()

        composeRule.onNodeWithText("CEPs armazenados").performClick()
        waitForToast(R.string.error_unexpected)

        composeRule.onNodeWithText("CEPs consultados").assertIsDisplayed()
        composeRule.onNodeWithText("CEP: 01001-000").assertIsDisplayed()
        composeRule.onNodeWithText("Nenhum CEP armazenado.").assertDoesNotExist()
        assertEquals(1, ShadowToast.shownToastCount())
        composeRule.waitForIdle()
        assertEquals(1, ShadowToast.shownToastCount())
    }

    private fun launch(
        savedAddresses: Flow<List<Address>> = flow { emit(emptyList()) },
        lookup: Address? = null,
        savedInput: String? = null,
    ) {
        repository = FakeCepRepository(savedAddresses, lookup)
        viewModel = CepViewModel(
            getAddressByCep = GetAddressByCepUseCase(repository),
            getSavedAddresses = GetSavedAddressesUseCase(repository),
            savedStateHandle = SavedStateHandle(
                savedInput?.let { mapOf("cep_input" to it) } ?: emptyMap(),
            ),
        )
        composeRule.setContent {
            CepTheme {
                CepApp(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()
    }

    private fun waitForToast(stringRes: Int) {
        val expected = RuntimeEnvironment.getApplication().getString(stringRes)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            ShadowToast.getTextOfLatestToast() == expected
        }
    }

    private fun address() = Address(
        zipCode = "01001000",
        street = "Praça da Sé",
        complement = "",
        neighborhood = "Sé",
        city = "São Paulo",
        stateAbbreviation = "SP",
        state = "São Paulo",
    )

    private fun flowOfAddress(address: Address): Flow<List<Address>> = flow {
        emit(listOf(address))
    }

    private class FakeCepRepository(
        private val savedAddresses: Flow<List<Address>>,
        private val lookup: Address?,
    ) : CepRepository {
        var lookupCalls = 0

        override suspend fun getAddress(zipCode: String): Address? {
            lookupCalls++
            return lookup?.takeIf { it.zipCode == zipCode }
        }

        override fun observeSavedAddresses(): Flow<List<Address>> = savedAddresses
    }
}
