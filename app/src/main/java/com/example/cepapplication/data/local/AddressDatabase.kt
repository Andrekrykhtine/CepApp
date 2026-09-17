package com.example.cepapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AddressEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class AddressDatabase : RoomDatabase() {
    abstract fun addressDao(): AddressDao

    companion object {
        private const val DATABASE_NAME = "addresses.db"

        internal val MIGRATION_1_2 = object : Migration(1, 2) {
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

        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE addresses " +
                        "ADD COLUMN saved_at_epoch_millis INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE addresses " +
                        "ADD COLUMN last_consultation_order INTEGER NOT NULL DEFAULT 0",
                )
                val ids = database.query(
                    "SELECT id FROM addresses ORDER BY saved_at_epoch_millis ASC, id ASC",
                ).use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) add(cursor.getLong(0))
                    }
                }
                ids.forEachIndexed { index, id ->
                    database.execSQL(
                        "UPDATE addresses SET last_consultation_order = ? WHERE id = ?",
                        arrayOf(index.toLong() + 1, id),
                    )
                }
            }
        }

        fun create(context: Context): AddressDatabase = Room.databaseBuilder(
            context.applicationContext,
            AddressDatabase::class.java,
            DATABASE_NAME,
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
    }
}
