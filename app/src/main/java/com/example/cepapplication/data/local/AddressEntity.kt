package com.example.cepapplication.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.cepapplication.domain.model.Address

@Entity(
    tableName = "addresses",
    indices = [Index(value = ["zip_code"], unique = true)],
)
data class AddressEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "zip_code")
    val zipCode: String,
    val street: String,
    val complement: String,
    val neighborhood: String,
    val city: String,
    @ColumnInfo(name = "state_abbreviation")
    val stateAbbreviation: String,
    val state: String,
)

internal fun AddressEntity.toDomain(): Address = Address(
    zipCode = zipCode,
    street = street,
    complement = complement,
    neighborhood = neighborhood,
    city = city,
    stateAbbreviation = stateAbbreviation,
    state = state,
)

internal fun Address.toEntity(): AddressEntity = AddressEntity(
    zipCode = zipCode,
    street = street,
    complement = complement,
    neighborhood = neighborhood,
    city = city,
    stateAbbreviation = stateAbbreviation,
    state = state,
)
