package com.example.cepapplication.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CepDatabaseTest {
    private lateinit var database: CepDatabase

    @Before
    fun createDatabase() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
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
