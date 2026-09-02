package com.example.cepapplication.domain.usecase

import com.example.cepapplication.domain.model.Address
import com.example.cepapplication.domain.repository.CepRepository
import kotlinx.coroutines.flow.Flow

class GetSavedAddressesUseCase(
    private val repository: CepRepository,
) {
    operator fun invoke(): Flow<List<Address>> = repository.observeSavedAddresses()
}
