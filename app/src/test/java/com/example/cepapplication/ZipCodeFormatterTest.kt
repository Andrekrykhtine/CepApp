package com.example.cepapplication

import com.example.cepapplication.domain.util.CepFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZipCodeFormatterTest {
    @Test
    fun `normalizes a formatted zip code`() {
        assertEquals("01001000", CepFormatter.normalize("01001-000"))
    }

    @Test
    fun `accepts exactly eight digits after normalization`() {
        assertTrue(CepFormatter.isValid("01001-000"))
        assertFalse(CepFormatter.isValid("01001-00"))
        assertFalse(CepFormatter.isValid("01001-0009"))
    }

    @Test
    fun `formats a complete zip code`() {
        assertEquals("01001-000", CepFormatter.format("01001000"))
    }

    @Test
    fun `ignores non numeric characters and limits length`() {
        assertEquals("01001-000", CepFormatter.format("01.001-0009"))
    }

    @Test
    fun `keeps an incomplete zip code without separator`() {
        assertEquals("01001", CepFormatter.format("01001"))
    }
}
