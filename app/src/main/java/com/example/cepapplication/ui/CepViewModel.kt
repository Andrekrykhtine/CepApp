package com.example.cepapplication.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cepapplication.domain.model.Address
import com.example.cepapplication.domain.usecase.GetAddressByCepUseCase
import com.example.cepapplication.domain.usecase.GetSavedAddressesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CepViewModel(
    private val getAddressByCep: GetAddressByCepUseCase,
    private val getSavedAddresses: GetSavedAddressesUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<CepUiState>(CepUiState.Idle)
    val uiState: StateFlow<CepUiState> = _uiState.asStateFlow()

    private val _savedAddresses = MutableStateFlow<List<Address>>(emptyList())
    val savedAddresses: StateFlow<List<Address>> = _savedAddresses.asStateFlow()

    fun search(rawZipCode: String) {
        viewModelScope.launch {
            _uiState.value = CepUiState.Loading
            _uiState.value = getAddressByCep(rawZipCode).fold(
                onSuccess = CepUiState::Success,
                onFailure = CepUiState::Error,
            )
        }
    }

    fun loadSavedAddresses() {
        viewModelScope.launch {
            _savedAddresses.value = getSavedAddresses()
        }
    }

    fun loadLatestAddress() {
        if (_uiState.value != CepUiState.Idle) return

        viewModelScope.launch {
            getSavedAddresses().firstOrNull()?.let { address ->
                _uiState.value = CepUiState.Success(address)
            }
        }
    }
}

class CepViewModelFactory(
    private val getAddressByCep: GetAddressByCepUseCase,
    private val getSavedAddresses: GetSavedAddressesUseCase,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(CepViewModel::class.java)) {
            "ViewModel não suportado: ${modelClass.name}"
        }
        return CepViewModel(getAddressByCep, getSavedAddresses) as T
    }
}
