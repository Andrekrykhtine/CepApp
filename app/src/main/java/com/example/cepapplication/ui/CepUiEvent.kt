package com.example.cepapplication.ui

sealed interface CepUiEvent {
    data object ZipCodeSaved : CepUiEvent
}
