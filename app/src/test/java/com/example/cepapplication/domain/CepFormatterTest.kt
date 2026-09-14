package com.example.cepapplication.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CepFormatterTest {

    @Test
    fun `formats eight digits with zip code mask`() {
        assertEquals("12345-678", CepFormatter.format("12345678"))
    }

    @Test
    fun `removes non numeric characters before formatting`() {
        assertEquals("12345-678", CepFormatter.format("12.345 abc 678"))
    }

    @Test
    fun `validates only values with eight digits`() {
        assertTrue(CepFormatter.isValid("12345-678"))
        assertFalse(CepFormatter.isValid("12345-67"))
    }
}
