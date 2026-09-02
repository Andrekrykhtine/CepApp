package com.example.cepapplication.data.local

import com.example.cepapplication.domain.model.Address
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomCepLocalDataSource(
    private val addressDao: AddressDao,
) : CepLocalDataSource {
    override suspend fun findByZipCode(zipCode: String): CachedAddress? =
        addressDao.findByZipCode(zipCode)?.let { entity ->
            CachedAddress(
                address = entity.toDomain(),
                savedAtEpochMillis = entity.savedAtEpochMillis,
            )
        }

    override suspend fun save(address: Address, savedAtEpochMillis: Long) {
        addressDao.save(address.toEntity(savedAtEpochMillis))
    }

    override fun observeAll(): Flow<List<Address>> =
        addressDao.observeAll().map { entities -> entities.map(AddressEntity::toDomain) }
}
