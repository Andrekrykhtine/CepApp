package com.example.cepapplication

import org.junit.Assert.assertEquals
import org.junit.Test

class ZipCodeFormatterTest {
    @Test
    fun `formats a complete zip code`() {
        assertEquals("01001-000", formatZipCode("01001000"))
    }

    @Test
    fun `ignores non numeric characters and limits length`() {
        assertEquals("01001-000", formatZipCode("01.001-0009"))
    }

    @Test
    fun `keeps an incomplete zip code without separator`() {
        assertEquals("01001", formatZipCode("01001"))
    }
}
