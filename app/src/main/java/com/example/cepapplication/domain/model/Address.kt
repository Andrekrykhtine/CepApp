package com.example.cepapplication.domain.model

data class Address(
    val zipCode: String,
    val street: String,
    val complement: String,
    val neighborhood: String,
    val city: String,
    val stateAbbreviation: String,
    val state: String,
)
