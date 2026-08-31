package com.example.cepapplication.domain.repository

import com.example.cepapplication.domain.model.Address

interface CepRepository {
    suspend fun getAddress(zipCode: String): Address?

    suspend fun getSavedAddresses(): List<Address>
}
