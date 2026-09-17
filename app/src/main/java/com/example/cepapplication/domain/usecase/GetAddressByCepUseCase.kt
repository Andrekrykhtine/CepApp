package com.example.cepapplication.domain.usecase

import com.example.cepapplication.domain.model.Address
import com.example.cepapplication.domain.repository.CepRepository
import com.example.cepapplication.domain.util.CepFormatter
import kotlin.coroutines.cancellation.CancellationException

class GetAddressByCepUseCase(
    private val repository: CepRepository,
) {
    suspend operator fun invoke(rawZipCode: String): Result<Address> {
        if (!CepFormatter.isValid(rawZipCode)) {
            return Result.failure(InvalidCepException())
        }

        return try {
            Result.success(
                repository.getAddress(CepFormatter.normalize(rawZipCode))
                    ?: throw CepNotFoundException(),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }
}

class InvalidCepException : IllegalArgumentException("CEP deve conter exatamente 8 dígitos")

class CepNotFoundException : NoSuchElementException("CEP não encontrado")
