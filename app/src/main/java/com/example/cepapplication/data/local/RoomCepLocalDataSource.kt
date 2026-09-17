package com.example.cepapplication.data.local

import com.example.cepapplication.domain.model.Address
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class RoomCepLocalDataSource(
    private val addressDao: AddressDao,
) : CepLocalDataSource {
    override suspend fun findAndRecordConsultation(zipCode: String): Address? = localOperation {
        addressDao.findAndRecordConsultation(zipCode)?.toDomain()
    }

    override suspend fun saveAndRecordConsultation(address: Address, savedAtEpochMillis: Long): Address =
        localOperation {
            addressDao.saveAndRecordConsultation(address, savedAtEpochMillis)
            address
        }

    override fun observeAll(): Flow<List<Address>> =
        addressDao.observeAll()
            .map { entities -> entities.map(AddressEntity::toDomain) }
            .catch { error -> throw error.asLocalStorageException() }

    private suspend fun <T> localOperation(operation: suspend () -> T): T = try {
        operation()
    } catch (error: Throwable) {
        throw error.asLocalStorageException()
    }

    private fun Throwable.asLocalStorageException(): Throwable = when (this) {
        is CancellationException -> this
        is LocalStorageException -> this
        else -> LocalStorageException(this)
    }
}
