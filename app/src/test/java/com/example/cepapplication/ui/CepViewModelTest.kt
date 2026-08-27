package com.example.cepapplication.ui

import com.example.cepapplication.data.CepRepository
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CepViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loads and formats the saved zip code`() = runTest {
        val viewModel = CepViewModel(FakeCepRepository("12345678"))
        advanceUntilIdle()

        assertEquals("12345-678", viewModel.uiState.value.savedZipCode)
    }

    @Test
    fun `invalid zip code exposes validation error and is not persisted`() = runTest {
        val repository = FakeCepRepository()
        val viewModel = CepViewModel(repository)

        viewModel.onZipCodeChanged("123")
        viewModel.saveZipCode()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isZipCodeInvalid)
        assertNull(repository.persistedZipCode)
    }

    @Test
    fun `valid zip code is persisted and reflected in the state`() = runTest {
        val repository = FakeCepRepository()
        val viewModel = CepViewModel(repository)
        val emittedEvent = async(start = CoroutineStart.UNDISPATCHED) {
            viewModel.events.first()
        }

        viewModel.onZipCodeChanged("12345678")
        viewModel.saveZipCode()
        advanceUntilIdle()

        assertEquals("12345678", repository.persistedZipCode)
        assertEquals("12345-678", viewModel.uiState.value.savedZipCode)
        assertEquals("", viewModel.uiState.value.inputZipCode)
        assertEquals(CepUiEvent.ZipCodeSaved, emittedEvent.await())
    }

    private class FakeCepRepository(
        initialZipCode: String? = null
    ) : CepRepository {
        var persistedZipCode: String? = initialZipCode
            private set

        override suspend fun getSavedZipCode(): String? = persistedZipCode

        override suspend fun saveZipCode(zipCode: String) {
            persistedZipCode = zipCode
        }
    }
}
