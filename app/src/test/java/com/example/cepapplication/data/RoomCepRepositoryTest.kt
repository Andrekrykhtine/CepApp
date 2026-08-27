package com.example.cepapplication.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomCepRepositoryTest {

    @Test
    fun `reads zip code from Room when it exists`() = runTest {
        val dao = FakeCepDao("12345678")
        val repository = RoomCepRepository(dao)

        assertEquals("12345678", repository.getSavedZipCode())
    }

    @Test
    fun `returns null when Room has no saved zip code`() = runTest {
        val dao = FakeCepDao()
        val repository = RoomCepRepository(dao)

        assertNull(repository.getSavedZipCode())
    }

    @Test
    fun `saves zip code in Room`() = runTest {
        val dao = FakeCepDao()
        val repository = RoomCepRepository(dao)

        repository.saveZipCode("12345678")

        assertEquals("12345678", dao.zipCode)
    }

    private class FakeCepDao(
        var zipCode: String? = null
    ) : CepDao {
        override suspend fun getSavedZipCode(): String? = zipCode

        override suspend fun saveZipCode(entity: SavedZipCodeEntity) {
            zipCode = entity.zipCode
        }
    }

}
