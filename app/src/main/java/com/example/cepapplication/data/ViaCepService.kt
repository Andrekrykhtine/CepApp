package com.example.cepapplication.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ViaCepService {
    private const val BASE_URL = "https://viacep.com.br/"

    val api: ViaCepApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ViaCepApi::class.java)
    }
}
