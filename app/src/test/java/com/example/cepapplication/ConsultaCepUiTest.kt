package com.example.cepapplication

import android.app.Application
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.example.cepapplication.data.remote.CepRemoteDataSource
import com.example.cepapplication.domain.model.Address
import com.example.cepapplication.testing.IsolatedRoomDatabase
import com.example.cepapplication.ui.CepUiState
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import retrofit2.HttpException
import retrofit2.Response

class UiTestApplication : CepApplication() {
    override val container: AppContainer by lazy { factory(this) }
    companion object {
        lateinit var factory: (Application) -> AppContainer
    }
}
@RunWith(RobolectricTestRunner::class)
@Config(application = UiTestApplication::class)
class ConsultaCepUiTest {
    private lateinit var fixture: IsolatedRoomDatabase
    private lateinit var controller: ActivityController<MainActivity>
    private lateinit var activity: MainActivity
    private val requests = mutableListOf<String>()
    private var failure: Throwable? = null
    private var gate: CompletableDeferred<Unit>? = null
    private val a = Address("01001000", "Praça da Sé", "", "Sé", "São Paulo", "SP", "São Paulo")
    private val b = Address("20040002", "Rua da Assembleia", "", "Centro", "Rio de Janeiro", "RJ", "Rio de Janeiro")
    private val remote = object : CepRemoteDataSource {
        override suspend fun findByZipCode(zipCode: String): Address? {
            requests += zipCode
            gate?.await()
            failure?.let { throw it }
            return listOf(a, b).find { it.zipCode == zipCode }
        }
    }

    @Before
    fun setup() {
        fixture = IsolatedRoomDatabase.create(RuntimeEnvironment.getApplication())
        UiTestApplication.factory = { context ->
            AppContainer(context, databaseFactory = { fixture.database }, remoteDataSourceFactory = { remote })
        }
        controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        activity = controller.get()
        awaitUi { activity.cepViewModel.savedAddressesState.value.hasLoaded }
        ShadowToast.reset()
    }

    @After
    fun cleanup() {
        if (::controller.isInitialized) controller.pause().stop().destroy()
        if (::fixture.isInitialized) fixture.close()
    }

    private fun <T : View> view(id: Int): T = activity.findViewById(id)
    private fun idle() { shadowOf(Looper.getMainLooper()).idle() }
    private fun awaitUi(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 10_000_000_000L
        while (true) {
            idle()
            if (condition()) return
            if (System.nanoTime() > deadline) fail("A UI não concluiu dentro do prazo")
            Thread.yield()
        }
    }
    private fun search(cep: String) {
        view<EditText>(R.id.edtZipCode).setText(cep)
        view<Button>(R.id.btnSave).performClick()
        idle()
    }
    private fun awaitSuccess() = awaitUi { activity.cepViewModel.screenState.value.status is CepUiState.Success }
    private fun awaitError() = awaitUi { activity.cepViewModel.screenState.value.status is CepUiState.Error }
    private fun rows() = runBlocking { fixture.database.addressDao().observeAll().first() }
    private fun openList() {
        view<Button>(R.id.btnStoredZipCodes).performClick()
        awaitUi { activity.findViewById<View>(R.id.recyclerSavedAddresses) != null }
    }
    private fun back() {
        val host = activity.supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        host.navController.popBackStack()
        awaitUi { activity.findViewById<View>(R.id.edtZipCode) != null }
    }

    @Test
    fun mascaraValidacaoLoadingSeteCamposELimpeza() {
        val input = view<EditText>(R.id.edtZipCode)
        input.text.replace(0, input.length(), "a00123b456789")
        assertEquals("00123-456", input.text.toString())
        input.setText("0100100099")
        assertEquals("01001-000", input.text.toString())
        search("123")
        assertEquals(activity.getString(R.string.error_invalid_zip_code), input.error.toString())
        assertTrue(requests.isEmpty())
        assertTrue(rows().isEmpty())

        gate = CompletableDeferred()
        search("01001000")
        awaitUi { requests.isNotEmpty() }
        assertFalse(input.isEnabled)
        assertFalse(view<Button>(R.id.btnSave).isEnabled)
        assertEquals(View.VISIBLE, view<View>(R.id.progressLookup).visibility)
        gate!!.complete(Unit)
        awaitSuccess()
        assertTrue(input.isEnabled)
        assertTrue(view<Button>(R.id.btnSave).isEnabled)
        assertEquals(View.GONE, view<View>(R.id.progressLookup).visibility)
        assertEquals("", input.text.toString())
        val text = view<TextView>(R.id.txtLastAddress).text.toString()
        assertEquals(activity.formatAddress(a), text)
        assertEquals(7, text.lines().size)
        assertTrue(text.contains("Complemento: Não informado"))
        assertEquals(listOf("01001000"), requests)
        assertEquals(listOf("01001000"), rows().map { it.zipCode })
    }

    @Test
    fun httpPreservaResultadoToastUnicoENovaTentativa() {
        search(a.zipCode)
        awaitSuccess()
        val before = rows()
        ShadowToast.reset()
        failure = HttpException(Response.error<Any>(500, "falha".toResponseBody()))
        search(b.zipCode)
        awaitError()
        assertEquals(activity.getString(R.string.error_unexpected), ShadowToast.getTextOfLatestToast())
        assertEquals(1, ShadowToast.shownToastCount())
        assertEquals("20040-002", view<EditText>(R.id.edtZipCode).text.toString())
        assertEquals(activity.formatAddress(a), view<TextView>(R.id.txtLastAddress).text.toString())
        assertEquals(before, rows())
        view<EditText>(R.id.edtZipCode).setText("2004000")
        idle()
        assertEquals(1, ShadowToast.shownToastCount())
        openList()
        back()
        assertEquals(1, ShadowToast.shownToastCount())
        failure = null
        search(b.zipCode)
        awaitSuccess()
        assertEquals(listOf(b.zipCode, a.zipCode), rows().map { it.zipCode })
    }

    @Test
    fun redeInexistenciaEConsultaLocalOffline() {
        search(a.zipCode)
        awaitSuccess()
        val before = rows()
        failure = IOException("offline")
        search(b.zipCode)
        awaitError()
        assertEquals(activity.getString(R.string.error_network), ShadowToast.getTextOfLatestToast())
        assertEquals(before, rows())
        search(a.zipCode)
        awaitSuccess()
        assertEquals(2, requests.size)
        failure = null
        search("99999999")
        awaitError()
        assertEquals(activity.getString(R.string.error_zip_code_not_found),
            view<EditText>(R.id.edtZipCode).error.toString())
        assertEquals(listOf(a.zipCode), rows().map { it.zipCode })
        assertEquals(activity.formatAddress(a), view<TextView>(R.id.txtLastAddress).text.toString())
    }

    @Test
    fun falhaAoSalvarPreservaBancoResultadoEPermiteNovaTentativa() {
        search(a.zipCode)
        awaitSuccess()
        val before = rows()
        fixture.database.openHelper.writableDatabase.execSQL(
            """CREATE TRIGGER fail_save BEFORE INSERT ON addresses BEGIN
                UPDATE addresses SET last_consultation_order = 99;
                SELECT RAISE(ABORT, 'falha controlada');
            END""",
        )
        search(b.zipCode)
        awaitError()
        assertEquals(activity.getString(R.string.error_unexpected), ShadowToast.getTextOfLatestToast())
        assertEquals(activity.formatAddress(a), view<TextView>(R.id.txtLastAddress).text.toString())
        assertEquals(before, rows())
        fixture.database.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_save")
        search(b.zipCode)
        awaitSuccess()
        assertEquals(listOf(b.zipCode, a.zipCode), rows().map { it.zipCode })
    }

    @Test
    fun navegacaoListaReativaRecriacaoDaViewSemEscritaOuFeedbackRepetido() {
        openList()
        assertEquals(View.VISIBLE, view<View>(R.id.txtEmptyAddresses).visibility)
        back()
        search(a.zipCode)
        awaitSuccess()
        search(b.zipCode)
        awaitSuccess()
        search(a.zipCode)
        awaitSuccess()
        awaitUi { activity.cepViewModel.savedAddressesState.value.addresses.firstOrNull() == a }
        val before = rows()
        val toastCount = ShadowToast.shownToastCount()
        openList()
        assertEquals(listOf(a.zipCode, b.zipCode), before.map { it.zipCode })
        val savedAddresses = view<RecyclerView>(R.id.recyclerSavedAddresses)
        awaitUi { savedAddresses.adapter!!.itemCount == 2 }
        assertEquals(listOf(a, b), (savedAddresses.adapter as SavedAddressesAdapter).currentList)
        awaitUi { savedAddresses.findViewHolderForAdapterPosition(0) != null }
        assertTrue(savedAddresses.findViewHolderForAdapterPosition(0)!!.itemView is MaterialCardView)
        assertEquals(activity.formatAddress(a),
            savedAddresses.findViewHolderForAdapterPosition(0)
                ?.itemView?.findViewById<TextView>(R.id.txtSavedAddress)?.text.toString())
        back()
        assertEquals(before, rows())
        assertEquals(2, requests.size)
        assertEquals(toastCount, ShadowToast.shownToastCount())
        assertEquals(activity.formatAddress(a), view<TextView>(R.id.txtLastAddress).text.toString())
        assertEquals("", view<EditText>(R.id.edtZipCode).text.toString())
    }
}
