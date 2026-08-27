package com.example.cepapplication.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cepapplication.data.CepRepository
import com.example.cepapplication.domain.CepFormatter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CepViewModel(
    private val repository: CepRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CepUiState())
    val uiState: StateFlow<CepUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<CepUiEvent>()
    val events: SharedFlow<CepUiEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(
                    savedZipCode = repository.getSavedZipCode()?.let(CepFormatter::format)
                )
            }
        }
    }

    fun onZipCodeChanged(value: String) {
        _uiState.update { currentState ->
            currentState.copy(
                inputZipCode = CepFormatter.format(value),
                isZipCodeInvalid = false
            )
        }
    }

    fun saveZipCode() {
        val currentInput = _uiState.value.inputZipCode
        if (!CepFormatter.isValid(currentInput)) {
            _uiState.update { currentState ->
                currentState.copy(isZipCodeInvalid = true)
            }
            return
        }

        val normalizedZipCode = CepFormatter.normalize(currentInput)
        viewModelScope.launch {
            repository.saveZipCode(normalizedZipCode)
            _uiState.update { currentState ->
                currentState.copy(
                    inputZipCode = "",
                    savedZipCode = CepFormatter.format(normalizedZipCode),
                    isZipCodeInvalid = false
                )
            }
            _events.emit(CepUiEvent.ZipCodeSaved)
        }
    }

    class Factory(
        private val repository: CepRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(CepViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return CepViewModel(repository) as T
        }
    }
}
