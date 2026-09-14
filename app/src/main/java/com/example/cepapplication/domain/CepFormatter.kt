package com.example.cepapplication.domain

object CepFormatter {
    fun normalize(value: String): String =
        value.filter(Char::isDigit).take(8)

    fun format(value: String): String {
        val numbers = normalize(value)
        return if (numbers.length > 5) {
            "${numbers.substring(0, 5)}-${numbers.substring(5)}"
        } else {
            numbers
        }
    }

    fun isValid(value: String): Boolean =
        value.filter(Char::isDigit).length == 8
}
