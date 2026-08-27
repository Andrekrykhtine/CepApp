package com.example.cepapplication.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_zip_code")
data class SavedZipCodeEntity(
    @PrimaryKey
    val id: Int = SINGLE_ROW_ID,
    @ColumnInfo(name = "zip_code")
    val zipCode: String
) {
    companion object {
        const val SINGLE_ROW_ID = 1
    }
}
