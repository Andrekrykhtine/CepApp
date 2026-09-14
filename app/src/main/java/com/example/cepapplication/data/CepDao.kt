package com.example.cepapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CepDao {
    @Query("SELECT zip_code FROM saved_zip_code WHERE id = 1 LIMIT 1")
    suspend fun getSavedZipCode(): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveZipCode(entity: SavedZipCodeEntity)
}
