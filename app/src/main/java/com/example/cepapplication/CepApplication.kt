package com.example.cepapplication

import android.app.Application

class CepApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
