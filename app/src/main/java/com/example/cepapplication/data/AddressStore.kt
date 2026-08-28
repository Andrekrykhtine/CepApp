package com.example.cepapplication.data

import android.content.SharedPreferences
import androidx.core.content.edit

class AddressStore(private val preferences: SharedPreferences) {
    fun save(address: Address) {
        preferences.edit {
            putString(KEY_ZIP_CODE, address.zipCode)
            putString(KEY_STREET, address.street)
            putString(KEY_COMPLEMENT, address.complement)
            putString(KEY_NEIGHBORHOOD, address.neighborhood)
            putString(KEY_CITY, address.city)
            putString(KEY_STATE_ABBREVIATION, address.stateAbbreviation)
            putString(KEY_STATE, address.state)
        }
    }

    fun load(): Address? {
        val zipCode = preferences.getString(KEY_ZIP_CODE, null) ?: return null
        return Address(
            zipCode = zipCode,
            street = preferences.getString(KEY_STREET, "").orEmpty(),
            complement = preferences.getString(KEY_COMPLEMENT, "").orEmpty(),
            neighborhood = preferences.getString(KEY_NEIGHBORHOOD, "").orEmpty(),
            city = preferences.getString(KEY_CITY, "").orEmpty(),
            stateAbbreviation = preferences.getString(KEY_STATE_ABBREVIATION, "").orEmpty(),
            state = preferences.getString(KEY_STATE, "").orEmpty(),
        )
    }

    private companion object {
        const val KEY_ZIP_CODE = "saved_zip_code"
        const val KEY_STREET = "saved_street"
        const val KEY_COMPLEMENT = "saved_complement"
        const val KEY_NEIGHBORHOOD = "saved_neighborhood"
        const val KEY_CITY = "saved_city"
        const val KEY_STATE_ABBREVIATION = "saved_state_abbreviation"
        const val KEY_STATE = "saved_state"
    }
}
