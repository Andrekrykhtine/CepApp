package com.example.cepapplication.data.repository

import com.example.cepapplication.data.local.CepLocalDataSource
import com.example.cepapplication.data.remote.CepRemoteDataSource
import com.example.cepapplication.domain.model.Address
import com.example.cepapplication.domain.repository.CepRepository
import kotlinx.coroutines.flow.Flow
import java.io.IOException

class CepRepositoryImpl(
    private val localDataSource: CepLocalDataSource,
    private val remoteDataSource: CepRemoteDataSource,
    private val cacheTtlMillis: Long = DEFAULT_CACHE_TTL_MILLIS,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : CepRepository {
    override suspend fun getAddress(zipCode: String): Address? {
        val cached = localDataSource.findByZipCode(zipCode)
        val now = currentTimeMillis()
        if (cached != null) {
            val cacheAgeMillis = now - cached.savedAtEpochMillis
            if (cacheAgeMillis in 0L..cacheTtlMillis) {
                return cached.address
            }
        }

        return try {
            remoteDataSource.findByZipCode(zipCode)?.also { address ->
                localDataSource.save(address, now)
            }
        } catch (error: IOException) {
            cached?.address ?: throw error
        }
    }

    override fun observeSavedAddresses(): Flow<List<Address>> = localDataSource.observeAll()

    private companion object {
        const val DEFAULT_CACHE_TTL_MILLIS = 24L * 60L * 60L * 1_000L
    }
}
