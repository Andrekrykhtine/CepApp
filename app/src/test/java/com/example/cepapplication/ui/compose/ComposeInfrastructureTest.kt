package com.example.cepapplication.ui.compose

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ComposeInfrastructureTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun minimalComposableRendersOnLocalHost() {
        composeRule.setContent { MinimalContent() }

        composeRule.onNodeWithText("Compose ativo").assertIsDisplayed()
    }
}

@Composable
private fun MinimalContent() {
    Text("Compose ativo")
}
