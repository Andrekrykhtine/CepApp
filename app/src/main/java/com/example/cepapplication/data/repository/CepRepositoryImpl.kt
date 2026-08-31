package com.example.cepapplication.data.repository

import com.example.cepapplication.data.local.CepLocalDataSource
import com.example.cepapplication.data.remote.CepRemoteDataSource
import com.example.cepapplication.domain.model.Address
import com.example.cepapplication.domain.repository.CepRepository

class CepRepositoryImpl(
    private val localDataSource: CepLocalDataSource,
    private val remoteDataSource: CepRemoteDataSource,
) : CepRepository {
    override suspend fun getAddress(zipCode: String): Address? {
        localDataSource.findByZipCode(zipCode)?.let { return it }

        return remoteDataSource.findByZipCode(zipCode)?.also { address ->
            localDataSource.save(address)
        }
    }

    override suspend fun getSavedAddresses(): List<Address> = localDataSource.findAll()
}
