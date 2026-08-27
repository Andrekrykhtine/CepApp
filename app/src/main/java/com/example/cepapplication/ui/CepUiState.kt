package com.example.cepapplication.ui

data class CepUiState(
    val inputZipCode: String = "",
    val savedZipCode: String? = null,
    val isZipCodeInvalid: Boolean = false
)
