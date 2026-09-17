package com.example.cepapplication.data.local

import com.example.cepapplication.domain.model.Address
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class RoomCepLocalDataSourceTest {
    @Test
    fun `wraps lookup failure as local storage exception and preserves cause`() = runBlocking {
        val failure = IllegalStateException("leitura indisponível")
        val source = RoomCepLocalDataSource(FakeAddressDao(lookupError = failure))

        val thrown = expectFailure { source.findAndRecordConsultation("01001000") }

        assertTrue(thrown is LocalStorageException)
        assertSame(failure, thrown.cause)
    }

    @Test
    fun `wraps save failure as local storage exception and preserves cause`() = runBlocking {
        val failure = IllegalStateException("escrita indisponível")
        val source = RoomCepLocalDataSource(FakeAddressDao(saveError = failure))

        val thrown = expectFailure { source.saveAndRecordConsultation(address(), 100L) }

        assertTrue(thrown is LocalStorageException)
        assertSame(failure, thrown.cause)
    }

    @Test
    fun `wraps observation failure as local storage exception and preserves cause`() = runBlocking {
        val failure = IllegalStateException("observação indisponível")
        val source = RoomCepLocalDataSource(
            FakeAddressDao(observedAddresses = flow { throw failure }),
        )

        val thrown = expectFailure { source.observeAll().collect {} }

        assertTrue(thrown is LocalStorageException)
        assertSame(failure, thrown.cause)
    }

    @Test(expected = CancellationException::class)
    fun `does not wrap cancellation from lookup`() = runBlocking<Unit> {
        RoomCepLocalDataSource(
            FakeAddressDao(lookupError = CancellationException("cancelado")),
        ).findAndRecordConsultation("01001000")
    }

    @Test(expected = CancellationException::class)
    fun `does not wrap cancellation from save`() = runBlocking<Unit> {
        RoomCepLocalDataSource(
            FakeAddressDao(saveError = CancellationException("cancelado")),
        ).saveAndRecordConsultation(address(), 100L)
    }

    @Test(expected = CancellationException::class)
    fun `does not wrap cancellation from observation`() = runBlocking<Unit> {
        RoomCepLocalDataSource(
            FakeAddressDao(
                observedAddresses = flow { throw CancellationException("cancelado") },
            ),
        ).observeAll().collect {}
    }

    @Test
    fun `maps local hit without exposing storage metadata`() = runBlocking {
        val source = RoomCepLocalDataSource(
            FakeAddressDao(lookupResult = entity()),
        )

        assertEquals(address(), source.findAndRecordConsultation("01001000"))
    }

    private class FakeAddressDao(
        private val lookupResult: AddressEntity? = null,
        private val lookupError: Throwable? = null,
        private val saveError: Throwable? = null,
        private val observedAddresses: Flow<List<AddressEntity>> = flowOf(emptyList()),
    ) : AddressDao {
        override suspend fun findByZipCode(zipCode: String): AddressEntity? = lookupResult

        override suspend fun save(address: AddressEntity) = Unit

        override suspend fun maxConsultationOrder(): Long = 0

        override suspend fun updateConsultationOrder(id: Long, order: Long) = Unit

        override suspend fun findAndRecordConsultation(zipCode: String): AddressEntity? {
            lookupError?.let { throw it }
            return lookupResult
        }

        override suspend fun saveAndRecordConsultation(address: Address, savedAtEpochMillis: Long) {
            saveError?.let { throw it }
        }

        override fun observeAll(): Flow<List<AddressEntity>> = observedAddresses
    }

    private suspend fun expectFailure(block: suspend () -> Unit): Throwable =
        runCatching { block() }.exceptionOrNull()
            ?: throw AssertionError("A operação deveria falhar")

    private fun address() = Address(
        zipCode = "01001000",
        street = "Praça da Sé",
        complement = "lado ímpar",
        neighborhood = "Sé",
        city = "São Paulo",
        stateAbbreviation = "SP",
        state = "São Paulo",
    )

    private fun entity() = AddressEntity(
        id = 7L,
        zipCode = "01001000",
        street = "Praça da Sé",
        complement = "lado ímpar",
        neighborhood = "Sé",
        city = "São Paulo",
        stateAbbreviation = "SP",
        state = "São Paulo",
        savedAtEpochMillis = 10L,
        lastConsultationOrder = 3L,
    )
}
