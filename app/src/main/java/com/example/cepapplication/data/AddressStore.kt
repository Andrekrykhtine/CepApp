package com.example.cepapplication.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AddressStore(context: Context) {
    private val applicationContext = context.applicationContext
    private val databaseHelper = AddressDatabaseHelper(applicationContext)

    init {
        migrateLegacyAddress()
    }

    fun save(address: Address) {
        val normalizedZipCode = address.zipCode.filter(Char::isDigit)
        require(normalizedZipCode.length == ZIP_CODE_LENGTH) { "CEP inválido" }

        val values = ContentValues().apply {
            put(COLUMN_ZIP_CODE, normalizedZipCode)
            put(COLUMN_STREET, address.street)
            put(COLUMN_COMPLEMENT, address.complement)
            put(COLUMN_NEIGHBORHOOD, address.neighborhood)
            put(COLUMN_CITY, address.city)
            put(COLUMN_STATE_ABBREVIATION, address.stateAbbreviation)
            put(COLUMN_STATE, address.state)
        }

        val rowId = databaseHelper.writableDatabase.insertWithOnConflict(
            TABLE_ADDRESSES,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE,
        )
        check(rowId != -1L) { "Não foi possível salvar o CEP" }
    }

    fun loadAll(): List<Address> = buildList {
        databaseHelper.readableDatabase.query(
            TABLE_ADDRESSES,
            COLUMNS,
            null,
            null,
            null,
            null,
            "$COLUMN_ID DESC",
        ).use { cursor ->
            val zipCodeIndex = cursor.getColumnIndexOrThrow(COLUMN_ZIP_CODE)
            val streetIndex = cursor.getColumnIndexOrThrow(COLUMN_STREET)
            val complementIndex = cursor.getColumnIndexOrThrow(COLUMN_COMPLEMENT)
            val neighborhoodIndex = cursor.getColumnIndexOrThrow(COLUMN_NEIGHBORHOOD)
            val cityIndex = cursor.getColumnIndexOrThrow(COLUMN_CITY)
            val stateAbbreviationIndex = cursor.getColumnIndexOrThrow(COLUMN_STATE_ABBREVIATION)
            val stateIndex = cursor.getColumnIndexOrThrow(COLUMN_STATE)

            while (cursor.moveToNext()) {
                add(
                    Address(
                        zipCode = cursor.getString(zipCodeIndex),
                        street = cursor.getString(streetIndex),
                        complement = cursor.getString(complementIndex),
                        neighborhood = cursor.getString(neighborhoodIndex),
                        city = cursor.getString(cityIndex),
                        stateAbbreviation = cursor.getString(stateAbbreviationIndex),
                        state = cursor.getString(stateIndex),
                    ),
                )
            }
        }
    }

    private fun migrateLegacyAddress() {
        val preferences = applicationContext.getSharedPreferences(LEGACY_PREFERENCES, Context.MODE_PRIVATE)
        val zipCode = preferences.getString(LEGACY_ZIP_CODE, null) ?: return
        val normalizedZipCode = zipCode.filter(Char::isDigit)
        if (normalizedZipCode.length != ZIP_CODE_LENGTH) return

        save(
            Address(
                zipCode = normalizedZipCode,
                street = preferences.getString(LEGACY_STREET, "").orEmpty(),
                complement = preferences.getString(LEGACY_COMPLEMENT, "").orEmpty(),
                neighborhood = preferences.getString(LEGACY_NEIGHBORHOOD, "").orEmpty(),
                city = preferences.getString(LEGACY_CITY, "").orEmpty(),
                stateAbbreviation = preferences.getString(LEGACY_STATE_ABBREVIATION, "").orEmpty(),
                state = preferences.getString(LEGACY_STATE, "").orEmpty(),
            ),
        )
        preferences.edit()
            .remove(LEGACY_ZIP_CODE)
            .remove(LEGACY_STREET)
            .remove(LEGACY_COMPLEMENT)
            .remove(LEGACY_NEIGHBORHOOD)
            .remove(LEGACY_CITY)
            .remove(LEGACY_STATE_ABBREVIATION)
            .remove(LEGACY_STATE)
            .apply()
    }

    private class AddressDatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onCreate(database: SQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE $TABLE_ADDRESSES (
                    $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_ZIP_CODE TEXT NOT NULL UNIQUE,
                    $COLUMN_STREET TEXT NOT NULL,
                    $COLUMN_COMPLEMENT TEXT NOT NULL,
                    $COLUMN_NEIGHBORHOOD TEXT NOT NULL,
                    $COLUMN_CITY TEXT NOT NULL,
                    $COLUMN_STATE_ABBREVIATION TEXT NOT NULL,
                    $COLUMN_STATE TEXT NOT NULL
                )
                """.trimIndent(),
            )
        }

        override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }

    private companion object {
        const val DATABASE_NAME = "addresses.db"
        const val DATABASE_VERSION = 1
        const val TABLE_ADDRESSES = "addresses"
        const val COLUMN_ID = "id"
        const val COLUMN_ZIP_CODE = "zip_code"
        const val COLUMN_STREET = "street"
        const val COLUMN_COMPLEMENT = "complement"
        const val COLUMN_NEIGHBORHOOD = "neighborhood"
        const val COLUMN_CITY = "city"
        const val COLUMN_STATE_ABBREVIATION = "state_abbreviation"
        const val COLUMN_STATE = "state"
        const val ZIP_CODE_LENGTH = 8
        const val LEGACY_PREFERENCES = "app_data"
        const val LEGACY_ZIP_CODE = "saved_zip_code"
        const val LEGACY_STREET = "saved_street"
        const val LEGACY_COMPLEMENT = "saved_complement"
        const val LEGACY_NEIGHBORHOOD = "saved_neighborhood"
        const val LEGACY_CITY = "saved_city"
        const val LEGACY_STATE_ABBREVIATION = "saved_state_abbreviation"
        const val LEGACY_STATE = "saved_state"

        val COLUMNS = arrayOf(
            COLUMN_ZIP_CODE,
            COLUMN_STREET,
            COLUMN_COMPLEMENT,
            COLUMN_NEIGHBORHOOD,
            COLUMN_CITY,
            COLUMN_STATE_ABBREVIATION,
            COLUMN_STATE,
        )
    }
}
