package com.example.cepapplication.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * Extensão do JUnit 5 para controle do [Dispatchers.Main] durante testes unitários na JVM.
 *
 * Esta extensão é a evolução e contraparte no JUnit 5 da regra [MainDispatcherRule] (JUnit 4).
 * Ela substitui o Main Dispatcher por um [TestDispatcher] antes de cada teste e o restaura ao final,
 * garantindo determinismo em testes de ViewModel e corrotinas.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherExtension(
    val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : BeforeEachCallback, AfterEachCallback {

    override fun beforeEach(context: ExtensionContext?) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun afterEach(context: ExtensionContext?) {
        Dispatchers.resetMain()
    }
}
