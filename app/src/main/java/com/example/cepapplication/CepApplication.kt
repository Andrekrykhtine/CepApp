package com.example.cepapplication

import android.app.Application
import com.example.cepapplication.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

open class CepApplication : Application() {
    open val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        initKoin()
    }

    protected open fun initKoin() {
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidLogger(Level.ERROR)
                androidContext(this@CepApplication)
                modules(appModules)
            }
        }
    }
}
