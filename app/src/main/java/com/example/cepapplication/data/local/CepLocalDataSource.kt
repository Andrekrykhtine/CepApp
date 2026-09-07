package com.example.cepapplication.data.local

import com.example.cepapplication.domain.model.Address
import kotlinx.coroutines.flow.Flow

data class CachedAddress(
    val address: Address,
    val savedAtEpochMillis: Long,
)

interface CepLocalDataSource {
    suspend fun findAndRecordConsultation(zipCode: String): Address?

    suspend fun saveAndRecordConsultation(address: Address, savedAtEpochMillis: Long): Address

    suspend fun findByZipCode(zipCode: String): CachedAddress?

    suspend fun save(address: Address, savedAtEpochMillis: Long)

    fun observeAll(): Flow<List<Address>>
}
