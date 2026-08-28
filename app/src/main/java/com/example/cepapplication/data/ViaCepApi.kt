package com.example.cepapplication.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path

interface ViaCepApi {
    @GET("ws/{cep}/json/")
    suspend fun findAddress(@Path("cep") zipCode: String): ViaCepResponse
}

data class ViaCepResponse(
    val cep: String? = null,
    val logradouro: String? = null,
    val complemento: String? = null,
    val bairro: String? = null,
    val localidade: String? = null,
    val uf: String? = null,
    val estado: String? = null,
    @SerializedName("erro") val hasError: Boolean? = null,
) {
    fun toAddress(): Address? {
        if (hasError == true || cep.isNullOrBlank()) return null

        return Address(
            zipCode = cep,
            street = logradouro.orEmpty(),
            complement = complemento.orEmpty(),
            neighborhood = bairro.orEmpty(),
            city = localidade.orEmpty(),
            stateAbbreviation = uf.orEmpty(),
            state = estado.orEmpty(),
        )
    }
}
