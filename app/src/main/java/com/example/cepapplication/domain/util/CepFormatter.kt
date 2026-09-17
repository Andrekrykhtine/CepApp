package com.example.cepapplication.domain.util

object CepFormatter {
    private const val CEP_LENGTH = 8

    fun normalize(value: String): String = value.filter(Char::isDigit)

    fun isValid(value: String): Boolean = normalize(value).length == CEP_LENGTH

    fun format(value: String): String {
        val normalized = normalize(value).take(CEP_LENGTH)
        return if (normalized.length > 5) {
            "${normalized.substring(0, 5)}-${normalized.substring(5)}"
        } else {
            normalized
        }
    }
}
