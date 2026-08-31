package com.example.cepapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AddressDao {
    @Query("SELECT * FROM addresses WHERE zip_code = :zipCode LIMIT 1")
    suspend fun findByZipCode(zipCode: String): AddressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(address: AddressEntity)

    @Query("SELECT * FROM addresses ORDER BY id DESC")
    suspend fun findAll(): List<AddressEntity>
}
