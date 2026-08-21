package com.example.cepapplication.domain

object CepFormatter {
    private const val ZIP_CODE_DIGIT_COUNT = 8
    private const val ZIP_CODE_PREFIX_LENGTH = 5

    fun normalize(value: String): String =
        value.filter(Char::isDigit).take(ZIP_CODE_DIGIT_COUNT)

    fun format(value: String): String {
        val numbers = normalize(value)
        return if (numbers.length > ZIP_CODE_PREFIX_LENGTH) {
            "${numbers.substring(0, ZIP_CODE_PREFIX_LENGTH)}-${numbers.substring(ZIP_CODE_PREFIX_LENGTH)}"
        } else {
            numbers
        }
    }

    fun isValid(value: String): Boolean =
        value.filter(Char::isDigit).length == ZIP_CODE_DIGIT_COUNT
}
