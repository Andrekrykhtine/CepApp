package com.example.cepapplication.data

import com.google.gson.annotations.SerializedName
import com.example.cepapplication.domain.model.Address
import com.example.cepapplication.domain.util.CepFormatter
import retrofit2.http.GET
import retrofit2.http.Path

interface ViaCepApi {
    @GET("ws/{cep}/json/")
    suspend fun findAddress(@Path("cep") zipCode: String): AddressDto
}

data class AddressDto(
    val cep: String? = null,
    val logradouro: String? = null,
    val complemento: String? = null,
    val bairro: String? = null,
    val localidade: String? = null,
    val uf: String? = null,
    val estado: String? = null,
    @SerializedName("erro") val hasError: Boolean? = null,
) {
    fun toDomain(): Address? {
        if (hasError == true || cep.isNullOrBlank()) return null

        return Address(
            zipCode = CepFormatter.normalize(cep),
            street = logradouro.orEmpty(),
            complement = complemento.orEmpty(),
            neighborhood = bairro.orEmpty(),
            city = localidade.orEmpty(),
            stateAbbreviation = uf.orEmpty(),
            state = estado.orEmpty(),
        )
    }
}
