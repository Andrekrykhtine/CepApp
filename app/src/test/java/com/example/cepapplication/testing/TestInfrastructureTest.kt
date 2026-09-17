package com.example.cepapplication.testing

import com.example.cepapplication.data.local.RoomCepLocalDataSource
import com.example.cepapplication.data.repository.CepRepositoryImpl
import com.example.cepapplication.domain.model.Address
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TestInfrastructureTest {
    @Test
    fun `real Room database is isolated between test compositions`() = runBlocking {
        IsolatedRoomDatabase.create(RuntimeEnvironment.getApplication()).use { first ->
            val repository = repository(first, ControlledCepRemoteDataSource(result = address()))

            assertEquals(address(), repository.getAddress("01001000"))
            assertEquals(listOf(address()), repository.observeSavedAddresses().first())
        }

        IsolatedRoomDatabase.create(RuntimeEnvironment.getApplication()).use { second ->
            val repository = repository(second, ControlledCepRemoteDataSource())

            assertTrue(repository.observeSavedAddresses().first().isEmpty())
        }
    }

    @Test
    fun `controlled remote waits for explicit coroutine release without sleep`() = runBlocking {
        val remote = ControlledCepRemoteDataSource(result = address())
        val gate = remote.delayResponses()
        IsolatedRoomDatabase.create(RuntimeEnvironment.getApplication()).use { fixture ->
            val lookup = async { repository(fixture, remote).getAddress("01001000") }

            while (remote.requestedZipCodes.isEmpty()) kotlinx.coroutines.yield()
            assertFalse(lookup.isCompleted)

            gate.complete(Unit)

            assertEquals(address(), lookup.await())
            assertEquals(listOf("01001000"), remote.requestedZipCodes)
        }
    }

    private fun repository(
        fixture: IsolatedRoomDatabase,
        remote: ControlledCepRemoteDataSource,
    ) = CepRepositoryImpl(
        localDataSource = RoomCepLocalDataSource(fixture.database.addressDao()),
        remoteDataSource = remote,
        currentTimeMillis = { 100L },
    )

    private fun address() = Address(
        zipCode = "01001000",
        street = "Praça da Sé",
        complement = "lado ímpar",
        neighborhood = "Sé",
        city = "São Paulo",
        stateAbbreviation = "SP",
        state = "São Paulo",
    )
}
