package com.example.cepapplication.data.local

import com.example.cepapplication.domain.model.Address

interface CepLocalDataSource {
    suspend fun findByZipCode(zipCode: String): Address?

    suspend fun save(address: Address)

    suspend fun findAll(): List<Address>
}
