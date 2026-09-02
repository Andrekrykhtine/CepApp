package com.example.cepapplication.domain.repository

import com.example.cepapplication.domain.model.Address
import kotlinx.coroutines.flow.Flow

interface CepRepository {
    suspend fun getAddress(zipCode: String): Address?

    fun observeSavedAddresses(): Flow<List<Address>>
}
