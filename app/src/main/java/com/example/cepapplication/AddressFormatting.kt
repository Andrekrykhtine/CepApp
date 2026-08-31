package com.example.cepapplication

import android.content.Context
import com.example.cepapplication.domain.model.Address
import com.example.cepapplication.domain.util.CepFormatter

internal fun Context.formatAddress(address: Address): String = listOf(
    getString(R.string.cep_salvo_text, formatZipCode(address.zipCode)),
    getString(R.string.street_text, displayValue(address.street)),
    getString(R.string.complement_text, displayValue(address.complement)),
    getString(R.string.neighborhood_text, displayValue(address.neighborhood)),
    getString(R.string.city_text, displayValue(address.city)),
    getString(R.string.state_abbreviation_text, displayValue(address.stateAbbreviation)),
    getString(R.string.state_text, displayValue(address.state)),
).joinToString(separator = "\n")

private fun Context.displayValue(value: String): String =
    value.ifBlank { getString(R.string.not_informed) }

internal fun formatZipCode(value: String): String {
    return CepFormatter.format(value)
}
