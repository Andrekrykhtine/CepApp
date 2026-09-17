package com.example.cepapplication.data.remote

import com.example.cepapplication.domain.model.Address

interface CepRemoteDataSource {
    suspend fun findByZipCode(zipCode: String): Address?
}
