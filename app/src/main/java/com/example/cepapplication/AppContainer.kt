package com.example.cepapplication

import android.content.Context
import com.example.cepapplication.data.ViaCepService
import com.example.cepapplication.data.local.AddressDatabase
import com.example.cepapplication.data.local.RoomCepLocalDataSource
import com.example.cepapplication.data.remote.CepRemoteDataSource
import com.example.cepapplication.data.remote.RetrofitCepRemoteDataSource
import com.example.cepapplication.data.repository.CepRepositoryImpl
import com.example.cepapplication.domain.usecase.GetAddressByCepUseCase
import com.example.cepapplication.domain.usecase.GetSavedAddressesUseCase
import com.example.cepapplication.ui.CepViewModelFactory

class AppContainer(
    context: Context,
    databaseFactory: (Context) -> AddressDatabase = AddressDatabase::create,
    remoteDataSourceFactory: () -> CepRemoteDataSource = {
        RetrofitCepRemoteDataSource(ViaCepService.api)
    },
) {
    private val database = databaseFactory(context)
    private val repository = CepRepositoryImpl(
        localDataSource = RoomCepLocalDataSource(database.addressDao()),
        remoteDataSource = remoteDataSourceFactory(),
    )

    val cepViewModelFactory = CepViewModelFactory(
        getAddressByCep = GetAddressByCepUseCase(repository),
        getSavedAddresses = GetSavedAddressesUseCase(repository),
    )
}
