package com.example.cepapplication.data

import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CepDatabaseTest {
    private lateinit var database: CepDatabase

    @Before
    fun createDatabase() {
        val context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, CepDatabase::class.java).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun savesAndReadsTheSingleZipCodeRow() = runBlocking {
        database.cepDao().saveZipCode(SavedZipCodeEntity(zipCode = "12345678"))

        assertEquals("12345678", database.cepDao().getSavedZipCode())
    }
}
