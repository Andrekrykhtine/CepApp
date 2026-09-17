package com.example.cepapplication.data.local

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class AddressDatabaseMigrationTest {
    private val context = RuntimeEnvironment.getApplication()
    private val databaseNames = mutableListOf<String>()

    @After
    fun removeTestDatabases() {
        databaseNames.forEach { context.deleteDatabase(it) }
    }

    @Test
    fun freshDatabaseHasCompatibleDefaultAndUniqueZipCode() {
        val name = newDatabaseName()
        openRoom(name).use { room ->
            val database = room.openHelper.writableDatabase
            assertSchema(database)
            assertEquals(emptyList<StoredAddress>(), readAddresses(database))
            database.execSQL(
                "INSERT INTO addresses (zip_code, street, complement, neighborhood, city, " +
                    "state_abbreviation, state, saved_at_epoch_millis) " +
                    "VALUES ('01001000', '', '', '', 'São Paulo', 'SP', 'São Paulo', 0)",
            )
            assertEquals(0L, readAddresses(database).single().order)
            assertUniqueZipCode(database)
        }
        openRoom(name).use { room ->
            assertEquals("01001000", readAddresses(room.openHelper.writableDatabase).single().zipCode)
        }
    }

    @Test
    fun emptyLegacyDatabasesMigrateToVersionFour() {
        for (version in 1..3) {
            val name = newDatabaseName()
            createLegacyDatabase(name, version, emptyList())
            openRoom(name).use { room ->
                val database = room.openHelper.writableDatabase
                assertSchema(database)
                assertEquals(emptyList<StoredAddress>(), readAddresses(database))
            }
        }
    }

    @Test
    fun populatedLegacyDatabasesPreserveContentIdsAndLegacyOrder() {
        for (version in 1..3) {
            val name = newDatabaseName()
            // Datas empatadas e zero, ids não consecutivos e inserção fora da ordem.
            val original = listOf(
                StoredAddress(9, "01001000", "Praça da Sé", "", "Sé", "São Paulo", "SP", "São Paulo", 300, 0),
                StoredAddress(2, "20040002", "Rua da Assembleia", "lado par", "Centro", "Rio de Janeiro", "RJ", "Rio de Janeiro", 0, 0),
                StoredAddress(12, "30140071", "", "", "", "Belo Horizonte", "MG", "Minas Gerais", 300, 0),
                StoredAddress(5, "70040900", "Praça dos Três Poderes", "", "", "Brasília", "DF", "Distrito Federal", 100, 0),
            ).map { if (version < 3) it.copy(savedAt = 0) else it }
            createLegacyDatabase(name, version, original)
            val expected = original.sortedWith(compareBy<StoredAddress> { it.savedAt }.thenBy { it.id })
                .mapIndexed { index, address -> address.copy(order = index.toLong() + 1) }
                .reversed()
            openRoom(name).use { room ->
                val database = room.openHelper.writableDatabase
                assertSchema(database)
                assertEquals(expected, readAddresses(database))
                assertUniqueZipCode(database)
                assertEquals(expected, readAddresses(database))
            }
            openRoom(name).use { room ->
                assertEquals(expected, readAddresses(room.openHelper.writableDatabase))
                kotlinx.coroutines.runBlocking {
                    var remoteCalls = 0
                    val repository = com.example.cepapplication.data.repository.CepRepositoryImpl(
                        RoomCepLocalDataSource(room.addressDao()),
                        object : com.example.cepapplication.data.remote.CepRemoteDataSource {
                            override suspend fun findByZipCode(zipCode: String): com.example.cepapplication.domain.model.Address? {
                                remoteCalls++
                                throw java.io.IOException("offline")
                            }
                        },
                    )
                    val cep = expected.last().zipCode
                    assertEquals(cep, repository.getAddress(cep)?.zipCode)
                    assertEquals(0, remoteCalls)
                    assertEquals(cep, readAddresses(room.openHelper.writableDatabase).first().zipCode)
                    assertEquals(expected.size, readAddresses(room.openHelper.writableDatabase).size)
                }
            }
        }
    }

    private fun newDatabaseName(): String = "migration-t001-${UUID.randomUUID()}.db".also {
        databaseNames.add(it)
    }

    private fun openRoom(name: String): AddressDatabase = Room.databaseBuilder(
        context, AddressDatabase::class.java, name,
    ).addMigrations(
        AddressDatabase.MIGRATION_1_2,
        AddressDatabase.MIGRATION_2_3,
        AddressDatabase.MIGRATION_3_4,
    ).build()

    private fun createLegacyDatabase(name: String, version: Int, addresses: List<StoredAddress>) {
        // Fixtures do schema legado, sem executar ou duplicar a lógica das migrações.
        context.openOrCreateDatabase(name, 0, null).use { database ->
            val timestampColumn = if (version >= 3) {
                ", saved_at_epoch_millis INTEGER NOT NULL DEFAULT 0"
            } else ""
            database.execSQL(
                "CREATE TABLE addresses (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "zip_code TEXT NOT NULL, street TEXT NOT NULL, complement TEXT NOT NULL, " +
                    "neighborhood TEXT NOT NULL, city TEXT NOT NULL, " +
                    "state_abbreviation TEXT NOT NULL, state TEXT NOT NULL$timestampColumn)",
            )
            if (version >= 2) {
                database.execSQL("CREATE UNIQUE INDEX index_addresses_zip_code ON addresses(zip_code)")
            }
            addresses.forEach { insertLegacy(database, version, it) }
            database.version = version
        }
    }

    private fun insertLegacy(database: SQLiteDatabase, version: Int, address: StoredAddress) {
        val values = mutableListOf<Any>(address.id, address.zipCode, address.street, address.complement,
            address.neighborhood, address.city, address.uf, address.state)
        if (version >= 3) values.add(address.savedAt)
        val timestampColumn = if (version >= 3) ", saved_at_epoch_millis" else ""
        database.execSQL(
            "INSERT INTO addresses (id, zip_code, street, complement, neighborhood, city, " +
                "state_abbreviation, state$timestampColumn) VALUES (${values.joinToString { "?" }})",
            values.toTypedArray(),
        )
    }

    private fun assertSchema(database: SupportSQLiteDatabase) {
        assertEquals(4, database.version)
        database.query("PRAGMA table_info(addresses)").use { cursor ->
            var found = false
            while (cursor.moveToNext()) {
                if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == "last_consultation_order") {
                    found = true
                    assertEquals("INTEGER", cursor.getString(cursor.getColumnIndexOrThrow("type")))
                    assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("notnull")))
                    assertEquals("0", cursor.getString(cursor.getColumnIndexOrThrow("dflt_value")))
                }
            }
            assertTrue("Coluna de recência ausente", found)
        }
        database.query("PRAGMA index_list(addresses)").use { cursor ->
            var found = false
            while (cursor.moveToNext()) {
                if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == "index_addresses_zip_code") {
                    found = true
                    assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("unique")))
                }
            }
            assertTrue("Índice único de CEP ausente", found)
        }
    }

    private fun assertUniqueZipCode(database: SupportSQLiteDatabase) {
        try {
            database.execSQL(
                "INSERT INTO addresses (zip_code, street, complement, neighborhood, city, " +
                    "state_abbreviation, state, saved_at_epoch_millis) " +
                    "SELECT zip_code, street, complement, neighborhood, city, " +
                    "state_abbreviation, state, saved_at_epoch_millis FROM addresses LIMIT 1",
            )
            fail("CEP duplicado deveria ser rejeitado")
        } catch (_: SQLiteConstraintException) {
            // A restrição deve rejeitar a inserção, preservando o registro original.
        }
    }

    private fun readAddresses(database: SupportSQLiteDatabase): List<StoredAddress> = database.query(
        "SELECT id, zip_code, street, complement, neighborhood, city, state_abbreviation, state, " +
            "saved_at_epoch_millis, last_consultation_order FROM addresses " +
            "ORDER BY last_consultation_order DESC, id DESC",
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(
                StoredAddress(cursor.getLong(0), cursor.getString(1), cursor.getString(2),
                    cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6),
                    cursor.getString(7), cursor.getLong(8), cursor.getLong(9)),
            )
        }
    }

    private data class StoredAddress(
        val id: Long,
        val zipCode: String,
        val street: String,
        val complement: String,
        val neighborhood: String,
        val city: String,
        val uf: String,
        val state: String,
        val savedAt: Long,
        val order: Long,
    )
}

private inline fun <T> AddressDatabase.use(block: (AddressDatabase) -> T): T =
    try { block(this) } finally { close() }
