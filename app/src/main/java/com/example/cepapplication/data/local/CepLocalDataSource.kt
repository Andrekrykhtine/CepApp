package com.example.cepapplication.data.local

import com.example.cepapplication.domain.model.Address
import kotlinx.coroutines.flow.Flow

interface CepLocalDataSource {
    suspend fun findAndRecordConsultation(zipCode: String): Address?

    suspend fun saveAndRecordConsultation(address: Address, savedAtEpochMillis: Long): Address

    fun observeAll(): Flow<List<Address>>
}
