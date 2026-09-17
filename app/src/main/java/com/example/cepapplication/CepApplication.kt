package com.example.cepapplication

import android.app.Application

open class CepApplication : Application() {
    open val container: AppContainer by lazy { AppContainer(this) }
}
