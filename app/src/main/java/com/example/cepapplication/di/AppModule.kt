package com.example.cepapplication.di

import androidx.lifecycle.SavedStateHandle
import com.example.cepapplication.data.ViaCepApi
import com.example.cepapplication.data.ViaCepService
import com.example.cepapplication.data.local.AddressDao
import com.example.cepapplication.data.local.AddressDatabase
import com.example.cepapplication.data.local.CepLocalDataSource
import com.example.cepapplication.data.local.RoomCepLocalDataSource
import com.example.cepapplication.data.remote.CepRemoteDataSource
import com.example.cepapplication.data.remote.RetrofitCepRemoteDataSource
import com.example.cepapplication.data.repository.CepRepositoryImpl
import com.example.cepapplication.domain.repository.CepRepository
import com.example.cepapplication.domain.usecase.GetAddressByCepUseCase
import com.example.cepapplication.domain.usecase.GetSavedAddressesUseCase
import com.example.cepapplication.ui.CepViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val dataModule: Module = module {
    single<AddressDatabase> { AddressDatabase.create(androidContext()) }
    single<AddressDao> { get<AddressDatabase>().addressDao() }
    single<ViaCepApi> { ViaCepService.api }
    single<CepLocalDataSource> { RoomCepLocalDataSource(get()) }
    single<CepRemoteDataSource> { RetrofitCepRemoteDataSource(get()) }
    single<CepRepository> {
        CepRepositoryImpl(
            localDataSource = get(),
            remoteDataSource = get(),
        )
    }
}

val useCaseModule: Module = module {
    factory { GetAddressByCepUseCase(get()) }
    factory { GetSavedAddressesUseCase(get()) }
}

val viewModelModule: Module = module {
    viewModel {
        CepViewModel(
            getAddressByCep = get(),
            getSavedAddresses = get(),
            savedStateHandle = getOrNull() ?: SavedStateHandle(),
        )
    }
}

val appModules: List<Module> = listOf(dataModule, useCaseModule, viewModelModule)
