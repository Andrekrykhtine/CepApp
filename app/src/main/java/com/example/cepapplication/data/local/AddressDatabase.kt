package com.example.cepapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AddressEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AddressDatabase : RoomDatabase() {
    abstract fun addressDao(): AddressDao

    companion object {
        private const val DATABASE_NAME = "addresses.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE addresses_room (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        zip_code TEXT NOT NULL,
                        street TEXT NOT NULL,
                        complement TEXT NOT NULL,
                        neighborhood TEXT NOT NULL,
                        city TEXT NOT NULL,
                        state_abbreviation TEXT NOT NULL,
                        state TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    INSERT INTO addresses_room (
                        id, zip_code, street, complement, neighborhood,
                        city, state_abbreviation, state
                    )
                    SELECT
                        id, zip_code, street, complement, neighborhood,
                        city, state_abbreviation, state
                    FROM addresses
                    """.trimIndent(),
                )
                database.execSQL("DROP TABLE addresses")
                database.execSQL("ALTER TABLE addresses_room RENAME TO addresses")
                database.execSQL(
                    "CREATE UNIQUE INDEX index_addresses_zip_code ON addresses(zip_code)",
                )
            }
        }

        fun create(context: Context): AddressDatabase = Room.databaseBuilder(
            context.applicationContext,
            AddressDatabase::class.java,
            DATABASE_NAME,
        ).addMigrations(MIGRATION_1_2).build()
    }
}
