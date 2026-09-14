package com.example.cepapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SavedZipCodeEntity::class],
    version = 1,
    exportSchema = true
)
abstract class CepDatabase : RoomDatabase() {
    abstract fun cepDao(): CepDao

    companion object {
        private const val DATABASE_NAME = "cep.db"

        @Volatile
        private var instance: CepDatabase? = null

        fun getInstance(context: Context): CepDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CepDatabase::class.java,
                    DATABASE_NAME
                ).build().also { instance = it }
            }
    }
}
