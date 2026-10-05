package com.example.cepapplication.ui.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.hasScrollAction
import androidx.compose.ui.test.assertHasScrollAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.example.cepapplication.domain.model.Address
import com.example.cepapplication.ui.CepScreenState
import com.example.cepapplication.ui.CepUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun inputIsMaskedThroughCallbackAndSearchOnlyRunsAfterOneClick() {
        var state by mutableStateOf(CepScreenState())
        var searchClicks = 0
        var savedClicks = 0

        composeRule.setContent {
            CepTheme {
                SearchScreen(
                    state = state,
                    onInputChange = { value -> state = state.copy(input = value) },
                    onSearch = { searchClicks++ },
                    onOpenSaved = { savedClicks++ },
                )
            }
        }

        assertEquals(0, searchClicks)
        composeRule.onNode(hasSetTextAction()).performTextInput("abc010010009")
        assertEquals("01001-000", state.input)
        assertEquals(0, searchClicks)

        composeRule.onNodeWithText("Consultar e salvar").performClick()
        assertEquals(1, searchClicks)
        assertEquals(0, savedClicks)
    }

    @Test
    fun loadingDisablesOnlyInputAndSearchAndShowsProgress() {
        val state = CepScreenState(
            status = CepUiState.Loading,
            input = "01001-000",
            lastSuccessfulAddress = address(),
        )

        composeRule.setContent {
            CepTheme {
                SearchScreen(
                    state = state,
                    onInputChange = {},
                    onSearch = {},
                    onOpenSaved = {},
                )
            }
        }

        composeRule.onNode(hasSetTextAction()).assertIsNotEnabled()
        composeRule.onNodeWithText("Consultar e salvar").assertIsNotEnabled()
        composeRule.onNodeWithText("CEPs armazenados").assertIsEnabled()
        composeRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
        composeRule.onNodeWithText("Último CEP consultado").assertIsDisplayed()
    }

    @Test
    fun successAndErrorKeepThePreviousResultVisible() {
        val successState = CepScreenState(
            status = CepUiState.Success(address()),
            lastSuccessfulAddress = address(),
        )
        composeRule.setContent {
            CepTheme {
                SearchScreen(
                    state = successState,
                    onInputChange = {},
                    onSearch = {},
                    onOpenSaved = {},
                )
            }
        }
        composeRule.onNodeWithText("Último CEP consultado").assertIsDisplayed()
        composeRule.onNodeWithText("CEP: 01001-000").assertIsDisplayed()

        val errorState = successState.copy(
            status = CepUiState.Error(IllegalStateException("falha")),
            input = "20000-000",
        )
        composeRule.setContent {
            CepTheme {
                SearchScreen(
                    state = errorState,
                    onInputChange = {},
                    onSearch = {},
                    onOpenSaved = {},
                )
            }
        }
        composeRule.onNodeWithText("20000-000").assertIsDisplayed()
        composeRule.onNodeWithText("CEP: 01001-000").assertIsDisplayed()
    }

    @Test
    fun contentUsesScrollableColumnForReducedHeight() {
        composeRule.setContent {
            CepTheme {
                androidx.compose.foundation.layout.Box(modifier = Modifier.size(320.dp, 240.dp)) {
                    SearchScreen(
                        state = CepScreenState(lastSuccessfulAddress = address()),
                        onInputChange = {},
                        onSearch = {},
                        onOpenSaved = {},
                    )
                }
            }
        }

        composeRule.onNode(hasScrollAction()).assertHasScrollAction()
        composeRule.onNodeWithText("CEPs armazenados").performScrollTo().assertIsDisplayed()
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
