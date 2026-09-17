package com.example.cepapplication.data.local

import androidx.room.Room
import androidx.room.withTransaction
import kotlinx.coroutines.launch
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import com.example.cepapplication.domain.model.Address
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class RoomPersistenceTest {
    private val context = RuntimeEnvironment.getApplication()
    private val databaseName = "local-t002-${UUID.randomUUID()}.db"
    private lateinit var database: AddressDatabase
    private lateinit var source: RoomCepLocalDataSource

    @Before
    fun openDatabase() {
        database = Room.databaseBuilder(context, AddressDatabase::class.java, databaseName).build()
        source = RoomCepLocalDataSource(database.addressDao())
    }

    @After
    fun closeDatabase() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun consultaLocalMoveAParaOTopoPreservandoDadosEReabertura() = runBlocking {
        val a = address("01001000")
        val b = address("20040002")
        assertEquals(a, source.saveAndRecordConsultation(a, 100))
        val original = database.addressDao().findByZipCode(a.zipCode)!!
        assertEquals(1L, original.lastConsultationOrder)
        source.saveAndRecordConsultation(b, 50)
        assertEquals(listOf(b, a), source.observeAll().first())
        assertEquals(a, source.findAndRecordConsultation(a.zipCode))
        val updated = database.addressDao().findByZipCode(a.zipCode)!!
        assertEquals(original.copy(lastConsultationOrder = 3), updated)
        assertEquals(listOf(a, b), source.observeAll().first())
        database.close()
        openDatabase()
        assertEquals(listOf(a, b), source.observeAll().first())
        assertEquals(3L, database.addressDao().maxConsultationOrder())
    }

    @Test
    fun missEObservacaoNaoEscrevem() = runBlocking {
        assertNull(source.findAndRecordConsultation("01001000"))
        assertTrue(source.observeAll().first().isEmpty())
        assertEquals(0L, database.addressDao().maxConsultationOrder())
        source.saveAndRecordConsultation(address("01001000"), 10)
        val before = database.addressDao().observeAll().first()
        repeat(2) { source.observeAll().first() }
        assertNull(source.findAndRecordConsultation("20040002"))
        assertEquals(before, database.addressDao().observeAll().first())
    }

    @Test
    fun duplicidadeNaoSubstituiEnderecoNemRecencia() = runBlocking {
        val a = address("01001000")
        source.saveAndRecordConsultation(a, 10)
        val before = database.addressDao().observeAll().first()
        expectWriteFailure { source.saveAndRecordConsultation(a.copy(street = "Outro"), 20) }
        assertEquals(before, database.addressDao().observeAll().first())
    }

    @Test
    fun falhaDeInsercaoReverteInclusiveEfeitosDoTrigger() = runBlocking {
        source.saveAndRecordConsultation(address("01001000"), 10)
        val before = database.addressDao().observeAll().first()
        database.openHelper.writableDatabase.execSQL("""
            CREATE TRIGGER fail_insert BEFORE INSERT ON addresses BEGIN
                UPDATE addresses SET last_consultation_order = 99;
                SELECT RAISE(ABORT, 'falha de teste');
            END
        """.trimIndent())
        expectWriteFailure { source.saveAndRecordConsultation(address("20040002"), 20) }
        assertEquals(before, database.addressDao().observeAll().first())
    }

    @Test
    fun falhaDeRecenciaReverteInclusiveEfeitosDoTrigger() = runBlocking {
        source.saveAndRecordConsultation(address("01001000"), 10)
        val before = database.addressDao().observeAll().first()
        database.openHelper.writableDatabase.execSQL("""
            CREATE TRIGGER fail_update BEFORE UPDATE OF last_consultation_order ON addresses BEGIN
                UPDATE addresses SET street = 'alteracao parcial';
                SELECT RAISE(ABORT, 'falha de teste');
            END
        """.trimIndent())
        expectWriteFailure { source.findAndRecordConsultation("01001000") }
        assertEquals(before, database.addressDao().observeAll().first())
    }

    @Test
    fun observacaoReageARecenciaEUsaIdComoDesempate() = runBlocking {
        val a = address("01001000")
        val b = address("20040002")
        source.saveAndRecordConsultation(a, 100)
        source.saveAndRecordConsultation(b, 10)
        database.openHelper.writableDatabase.execSQL("UPDATE addresses SET last_consultation_order = 1")
        assertEquals(listOf(b, a), source.observeAll().first())
        withTimeout(5_000) {
            val initialEmission = CompletableDeferred<Unit>()
            val emission = async {
                source.observeAll().onEach { initialEmission.complete(Unit) }
                    .first { it.firstOrNull() == a }
            }
            initialEmission.await()
            source.findAndRecordConsultation(a.zipCode)
            assertEquals(listOf(a, b), emission.await())
        }
    }

    @Test
    fun cancelamentoAntesDoCommitReverteEscrita() = runBlocking {
        source.saveAndRecordConsultation(address("01001000"), 0)
        val before = database.addressDao().observeAll().first()
        val written = CompletableDeferred<Unit>()
        val job = launch {
            database.withTransaction {
                database.addressDao().saveAndRecordConsultation(address("20040002"), 10)
                written.complete(Unit)
                kotlinx.coroutines.awaitCancellation()
            }
        }
        withTimeout(5_000) { written.await() }
        job.cancel()
        job.join()
        assertEquals(before, database.addressDao().observeAll().first())
    }

    private suspend fun expectWriteFailure(operation: suspend () -> Any?) {
        try {
            operation()
            fail("A escrita deveria falhar")
        } catch (expected: LocalStorageException) {
            // A falha real do SQLite deve atravessar a fonte local.
        }
    }

    private fun address(zipCode: String) = Address(
        zipCode = zipCode,
        street = "Praça da Sé",
        complement = "",
        neighborhood = "Sé",
        city = "São Paulo",
        stateAbbreviation = "SP",
        state = "São Paulo",
    )
}
