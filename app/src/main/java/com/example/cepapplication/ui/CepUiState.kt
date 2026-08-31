package com.example.cepapplication.ui

import com.example.cepapplication.domain.model.Address

sealed interface CepUiState {
    data object Idle : CepUiState

    data object Loading : CepUiState

    data class Success(val address: Address) : CepUiState

    data class Error(val cause: Throwable) : CepUiState
}
