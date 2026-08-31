package com.example.cepapplication.domain.usecase

import com.example.cepapplication.domain.model.Address
import com.example.cepapplication.domain.repository.CepRepository

class GetSavedAddressesUseCase(
    private val repository: CepRepository,
) {
    suspend operator fun invoke(): List<Address> = repository.getSavedAddresses()
}
