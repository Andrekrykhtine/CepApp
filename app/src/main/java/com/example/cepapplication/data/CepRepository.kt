package com.example.cepapplication.data

interface CepRepository {
    suspend fun getSavedZipCode(): String?

    suspend fun saveZipCode(zipCode: String)
}
