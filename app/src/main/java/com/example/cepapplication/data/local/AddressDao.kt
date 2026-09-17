package com.example.cepapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.cepapplication.domain.model.Address
import kotlinx.coroutines.flow.Flow

@Dao
interface AddressDao {
    @Query("SELECT * FROM addresses WHERE zip_code = :zipCode LIMIT 1")
    suspend fun findByZipCode(zipCode: String): AddressEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun save(address: AddressEntity)

    @Query("SELECT COALESCE(MAX(last_consultation_order), 0) FROM addresses")
    suspend fun maxConsultationOrder(): Long

    @Query("UPDATE addresses SET last_consultation_order = :order WHERE id = :id")
    suspend fun updateConsultationOrder(id: Long, order: Long)

    @Transaction
    suspend fun findAndRecordConsultation(zipCode: String): AddressEntity? {
        val address = findByZipCode(zipCode) ?: return null
        val order = maxConsultationOrder() + 1
        updateConsultationOrder(address.id, order)
        return address.copy(lastConsultationOrder = order)
    }

    @Transaction
    suspend fun saveAndRecordConsultation(address: Address, savedAtEpochMillis: Long) {
        save(address.toEntity(savedAtEpochMillis, maxConsultationOrder() + 1))
    }

    @Query("SELECT * FROM addresses ORDER BY last_consultation_order DESC, id DESC")
    fun observeAll(): Flow<List<AddressEntity>>
}
