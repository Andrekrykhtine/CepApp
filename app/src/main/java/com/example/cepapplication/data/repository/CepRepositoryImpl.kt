package com.example.cepapplication.data.repository

import com.example.cepapplication.data.local.CepLocalDataSource
import com.example.cepapplication.data.remote.CepRemoteDataSource
import com.example.cepapplication.domain.model.Address
import com.example.cepapplication.domain.repository.CepRepository
import kotlinx.coroutines.flow.Flow

class CepRepositoryImpl(
    private val localDataSource: CepLocalDataSource,
    private val remoteDataSource: CepRemoteDataSource,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : CepRepository {
    override suspend fun getAddress(zipCode: String): Address? {
        localDataSource.findAndRecordConsultation(zipCode)?.let { return it }

        val remoteAddress = remoteDataSource.findByZipCode(zipCode) ?: return null
        return localDataSource.saveAndRecordConsultation(remoteAddress, currentTimeMillis())
    }

    override fun observeSavedAddresses(): Flow<List<Address>> = localDataSource.observeAll()
}
