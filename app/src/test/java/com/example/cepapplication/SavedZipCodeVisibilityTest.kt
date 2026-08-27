package com.example.cepapplication

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedZipCodeVisibilityTest {

    @Test
    fun `saved zip code is hidden when value is empty`() {
        assertFalse(shouldDisplaySavedZipCode(""))
    }

    @Test
    fun `saved zip code is shown when value exists`() {
        assertTrue(shouldDisplaySavedZipCode("12345-678"))
    }
}
