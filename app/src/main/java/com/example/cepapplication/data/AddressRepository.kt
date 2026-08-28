package com.example.cepapplication.data

class AddressRepository(private val api: ViaCepApi) {
    suspend fun findAddress(zipCode: String): Address? = api.findAddress(zipCode).toAddress()
}
