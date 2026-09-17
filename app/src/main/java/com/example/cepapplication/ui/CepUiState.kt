package com.example.cepapplication.ui

import com.example.cepapplication.domain.model.Address

sealed interface CepUiState {
    data object Idle : CepUiState

    data object Loading : CepUiState

    data class Success(val address: Address) : CepUiState

    data class Error(val cause: Throwable) : CepUiState
}

data class CepScreenState(
    val status: CepUiState = CepUiState.Idle,
    val input: String = "",
    val lastSuccessfulAddress: Address? = null,
    val feedback: CepFeedback? = null,
)

data class SavedAddressesState(
    val addresses: List<Address> = emptyList(),
    val hasLoaded: Boolean = false,
    val readError: Throwable? = null,
    val errorFeedbackId: Long? = null,
)

data class CepFeedback(
    val id: Long,
    val type: Type,
    val cause: Throwable? = null,
) {
    enum class Type {
        LookupCompleted,
        LookupFailed,
    }
}
