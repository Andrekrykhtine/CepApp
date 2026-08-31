package com.example.cepapplication.data.local

import com.example.cepapplication.domain.model.Address

class RoomCepLocalDataSource(
    private val addressDao: AddressDao,
) : CepLocalDataSource {
    override suspend fun findByZipCode(zipCode: String): Address? =
        addressDao.findByZipCode(zipCode)?.toDomain()

    override suspend fun save(address: Address) {
        addressDao.save(address.toEntity())
    }

    override suspend fun findAll(): List<Address> = addressDao.findAll().map { it.toDomain() }
}
