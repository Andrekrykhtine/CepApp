package com.example.cepapplication.testing

import android.content.Context
import androidx.room.Room
import com.example.cepapplication.data.local.AddressDatabase
import java.util.UUID

class IsolatedRoomDatabase private constructor(
    private val context: Context,
    private val name: String,
    var database: AddressDatabase,
    private val beforeClose: () -> Unit,
) : AutoCloseable {
    override fun close() {
        beforeClose()
        database.close()
        check(context.deleteDatabase(name)) { "Não foi possível remover o banco de teste $name" }
    }

    fun reopen() {
        beforeClose()
        database.close()
        database = Room.databaseBuilder(context, AddressDatabase::class.java, name).build()
    }

    companion object {
        fun create(context: Context, beforeClose: () -> Unit = {}): IsolatedRoomDatabase {
            val name = "addresses-test-${UUID.randomUUID()}.db"
            val database = Room.databaseBuilder(
                context.applicationContext,
                AddressDatabase::class.java,
                name,
            ).build()
            return IsolatedRoomDatabase(context.applicationContext, name, database, beforeClose)
        }
    }
}
