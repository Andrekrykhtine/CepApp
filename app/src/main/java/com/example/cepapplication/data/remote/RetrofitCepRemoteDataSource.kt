package com.example.cepapplication.data.remote

import com.example.cepapplication.data.ViaCepApi
import com.example.cepapplication.domain.model.Address

class RetrofitCepRemoteDataSource(
    private val api: ViaCepApi,
) : CepRemoteDataSource {
    override suspend fun findByZipCode(zipCode: String): Address? =
        api.findAddress(zipCode).toDomain()
}
