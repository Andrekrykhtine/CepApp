package com.example.cepapplication.testing

import com.example.cepapplication.data.remote.CepRemoteDataSource
import com.example.cepapplication.domain.model.Address
import kotlinx.coroutines.CompletableDeferred

class ControlledCepRemoteDataSource(
    var result: Address? = null,
    var failure: Throwable? = null,
) : CepRemoteDataSource {
    val requestedZipCodes = mutableListOf<String>()
    private var responseGate: CompletableDeferred<Unit>? = null

    fun delayResponses(): CompletableDeferred<Unit> = CompletableDeferred<Unit>().also {
        responseGate = it
    }

    override suspend fun findByZipCode(zipCode: String): Address? {
        requestedZipCodes += zipCode
        responseGate?.await()
        failure?.let { throw it }
        return result
    }
}
