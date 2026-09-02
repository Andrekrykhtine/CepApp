package com.example.cepapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AddressDao {
    @Query("SELECT * FROM addresses WHERE zip_code = :zipCode LIMIT 1")
    suspend fun findByZipCode(zipCode: String): AddressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(address: AddressEntity)

    @Query("SELECT * FROM addresses ORDER BY saved_at_epoch_millis DESC, id DESC")
    fun observeAll(): Flow<List<AddressEntity>>
}
