package com.example.cepapplication.data

class RoomCepRepository(
    private val cepDao: CepDao
) : CepRepository {

    override suspend fun getSavedZipCode(): String? = cepDao.getSavedZipCode()

    override suspend fun saveZipCode(zipCode: String) {
        cepDao.saveZipCode(SavedZipCodeEntity(zipCode = zipCode))
    }
}
