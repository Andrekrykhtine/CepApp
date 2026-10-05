package com.example.cepapplication

import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.hasScrollAction
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertHasScrollAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.fetchSemanticsNodes
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.cepapplication.data.local.RoomCepLocalDataSource
import com.example.cepapplication.data.remote.CepRemoteDataSource
import com.example.cepapplication.data.repository.CepRepositoryImpl
import com.example.cepapplication.domain.model.Address
import com.example.cepapplication.domain.usecase.GetAddressByCepUseCase
import com.example.cepapplication.domain.usecase.GetSavedAddressesUseCase
import com.example.cepapplication.testing.IsolatedRoomDatabase
import com.example.cepapplication.ui.CepUiState
import com.example.cepapplication.ui.CepViewModel
import com.example.cepapplication.ui.compose.CepApp
import com.example.cepapplication.ui.compose.CepTheme
import com.example.cepapplication.ui.compose.SearchScreen
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowToast
import retrofit2.HttpException
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
class ConsultaCepUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var fixture: IsolatedRoomDatabase
    private lateinit var viewModel: CepViewModel
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
        val repository = CepRepositoryImpl(
            localDataSource = RoomCepLocalDataSource(fixture.database.addressDao()),
            remoteDataSource = remote,
        )
        viewModel = CepViewModel(
            getAddressByCep = GetAddressByCepUseCase(repository),
            getSavedAddresses = GetSavedAddressesUseCase(repository),
            savedStateHandle = SavedStateHandle(),
        )
        composeRule.setContent {
            CepTheme {
                CepApp(viewModel = viewModel)
            }
        }
        awaitUi { viewModel.savedAddressesState.value.hasLoaded }
        ShadowToast.reset()
    }

    @After
    fun cleanup() {
        if (::viewModel.isInitialized) viewModel.viewModelScope.cancel()
        if (::fixture.isInitialized) fixture.close()
        requests.clear()
        failure = null
        gate = null
    }

    private fun idle() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun awaitUi(condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            idle()
            condition()
        }
    }

    private fun input() = composeRule.onNode(hasSetTextAction())

    private fun search(cep: String) {
        input().performTextClearance()
        input().performTextInput(cep)
        composeRule.onNodeWithText("Consultar e salvar").performClick()
        idle()
    }

    private fun awaitSuccess() {
        awaitUi { viewModel.screenState.value.status is CepUiState.Success }
        composeRule.waitForIdle()
    }

    private fun awaitError() {
        awaitUi { viewModel.screenState.value.status is CepUiState.Error }
        composeRule.waitForIdle()
    }

    private fun awaitToast(stringRes: Int) {
        val expected = RuntimeEnvironment.getApplication().getString(stringRes)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            ShadowToast.getTextOfLatestToast() == expected
        }
    }

    private fun rows() = runBlocking { fixture.database.addressDao().observeAll().first() }

    private fun openList() {
        composeRule.onNodeWithText("CEPs armazenados").performClick()
        composeRule.onNodeWithText("CEPs armazenados").assertIsDisplayed()
    }

    private fun back() {
        composeRule.onNodeWithContentDescription("Voltar").performClick()
        composeRule.onNodeWithText("Busca CEP").assertIsDisplayed()
    }

    @Test
    fun mascaraValidacaoLoadingSeteCamposELimpeza() {
        input().performTextInput("a00123b456789")
        input().assertTextEquals("00123-456")
        input().performTextClearance()
        input().performTextInput("0100100099")
        input().assertTextEquals("01001-000")

        search("123")
        awaitToast(R.string.error_invalid_zip_code)
        input().assertTextEquals("123")
        composeRule.onNodeWithText("Digite um CEP válido").assertDoesNotExist()
        assertTrue(requests.isEmpty())
        assertTrue(rows().isEmpty())

        gate = CompletableDeferred()
        search("01001000")
        awaitUi { requests.isNotEmpty() }
        input().assertIsNotEnabled()
        composeRule.onNodeWithText("Consultar e salvar").assertIsNotEnabled()
        composeRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
        gate!!.complete(Unit)
        awaitSuccess()
        input().assertIsEnabled()
        composeRule.onNodeWithText("Consultar e salvar").assertIsEnabled()
        input().assertTextEquals("")
        assertEquals(7, activityAddressLines(a).size)
        activityAddressLines(a).forEach { line ->
            composeRule.onNodeWithText(line).assertIsDisplayed()
        }
        awaitToast(R.string.toast_zip_code_saved)
        assertEquals(1, ShadowToast.shownToastCount())
        assertEquals(listOf("01001000"), requests)
        assertEquals(listOf("01001000"), rows().map { it.zipCode })
    }

    @Test
    fun httpPreservaResultadoToastUnicoENovaTentativa() {
        search(a.zipCode)
        awaitSuccess()
        awaitToast(R.string.toast_zip_code_saved)
        val before = rows()
        ShadowToast.reset()
        failure = HttpException(Response.error<Any>(500, "falha".toResponseBody()))
        search(b.zipCode)
        awaitError()
        awaitToast(R.string.error_unexpected)
        assertEquals(1, ShadowToast.shownToastCount())
        input().assertTextEquals("20040-002")
        activityAddressLines(a).forEach { line ->
            composeRule.onNodeWithText(line).assertIsDisplayed()
        }
        assertEquals(before, rows())
        input().performTextClearance()
        input().performTextInput("2004000")
        idle()
        assertEquals(1, ShadowToast.shownToastCount())
        openList()
        back()
        assertEquals(1, ShadowToast.shownToastCount())
        input().assertTextEquals("20040-00")
        failure = null
        search(b.zipCode)
        awaitSuccess()
        awaitToast(R.string.toast_zip_code_saved)
        assertEquals(listOf(b.zipCode, a.zipCode), rows().map { it.zipCode })
    }

    @Test
    fun redeInexistenciaEConsultaLocalOffline() {
        search(a.zipCode)
        awaitSuccess()
        awaitToast(R.string.toast_zip_code_saved)
        val before = rows()
        failure = IOException("offline")
        search(b.zipCode)
        awaitError()
        awaitToast(R.string.error_network)
        assertEquals(before, rows())
        search(a.zipCode)
        awaitSuccess()
        awaitToast(R.string.toast_zip_code_saved)
        assertEquals(2, requests.size)
        failure = null
        search("99999999")
        awaitError()
        awaitToast(R.string.error_zip_code_not_found)
        input().assertTextEquals("99999-999")
        composeRule.onNodeWithText("CEP não encontrado").assertDoesNotExist()
        assertEquals(listOf(a.zipCode), rows().map { it.zipCode })
        activityAddressLines(a).forEach { line ->
            composeRule.onNodeWithText(line).assertIsDisplayed()
        }
    }

    @Test
    fun falhaAoSalvarPreservaBancoResultadoEPermiteNovaTentativa() {
        search(a.zipCode)
        awaitSuccess()
        awaitToast(R.string.toast_zip_code_saved)
        val before = rows()
        fixture.database.openHelper.writableDatabase.execSQL(
            """CREATE TRIGGER fail_save BEFORE INSERT ON addresses BEGIN
                UPDATE addresses SET last_consultation_order = 99;
                SELECT RAISE(ABORT, 'falha controlada');
            END""",
        )
        search(b.zipCode)
        awaitError()
        awaitToast(R.string.error_unexpected)
        activityAddressLines(a).forEach { line ->
            composeRule.onNodeWithText(line).assertIsDisplayed()
        }
        assertEquals(before, rows())
        fixture.database.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_save")
        search(b.zipCode)
        awaitSuccess()
        awaitToast(R.string.toast_zip_code_saved)
        assertEquals(listOf(b.zipCode, a.zipCode), rows().map { it.zipCode })
    }

    @Test
    fun navegacaoListaReativaRecriacaoDaViewSemEscritaOuFeedbackRepetido() {
        openList()
        composeRule.onNodeWithText("Nenhum CEP armazenado.").assertIsDisplayed()
        viewModel.search(a.zipCode)
        awaitSuccess()
        awaitToast(R.string.toast_zip_code_saved)
        awaitUi { viewModel.savedAddressesState.value.addresses == listOf(a) }
        activityAddressLines(a).forEach { line ->
            composeRule.onNodeWithText(line).assertIsDisplayed()
        }

        viewModel.search(b.zipCode)
        awaitSuccess()
        awaitToast(R.string.toast_zip_code_saved)
        awaitUi { viewModel.savedAddressesState.value.addresses == listOf(b, a) }
        assertZipCodesInOrder(b, a)

        back()
        search(a.zipCode)
        awaitSuccess()
        awaitToast(R.string.toast_zip_code_saved)
        awaitUi { viewModel.savedAddressesState.value.addresses.firstOrNull() == a }
        val before = rows()
        val toastCount = ShadowToast.shownToastCount()
        openList()
        assertEquals(listOf(a.zipCode, b.zipCode), before.map { it.zipCode })
        assertZipCodesInOrder(a, b)
        activityAddressLines(a).forEach { line ->
            composeRule.onNodeWithText(line).assertIsDisplayed()
        }
        back()
        assertEquals(before, rows())
        assertEquals(2, requests.size)
        assertEquals(toastCount, ShadowToast.shownToastCount())
        activityAddressLines(a).forEach { line ->
            composeRule.onNodeWithText(line).assertIsDisplayed()
        }
        input().assertTextEquals("")
    }

    @Test
    fun novoViewModelNaComposicaoRestauraEstadoSemToastOuNovaConsulta() {
        search(a.zipCode)
        awaitSuccess()
        awaitToast(R.string.toast_zip_code_saved)
        viewModel.updateInput("20040-00")
        val requestsBeforeRecreation = requests.toList()

        viewModel.viewModelScope.cancel()
        ShadowToast.reset()
        viewModel = createViewModel(SavedStateHandle(mapOf("cep_input" to "20040-00")))
        composeRule.setContent {
            CepTheme {
                CepApp(viewModel = viewModel)
            }
        }

        awaitUi {
            viewModel.savedAddressesState.value.hasLoaded &&
                viewModel.screenState.value.lastSuccessfulAddress == a
        }
        composeRule.onNode(hasSetTextAction()).assertTextEquals("20040-00")
        activityAddressLines(a).forEach { line ->
            composeRule.onNodeWithText(line).assertIsDisplayed()
        }
        assertEquals(requestsBeforeRecreation, requests)
        assertEquals(0, ShadowToast.shownToastCount())
    }

    @Test
    fun alturaReduzidaMantemRolagemEAcaoDeListaAlcancavel() {
        composeRule.setContent {
            CepTheme {
                Box(modifier = Modifier.size(320.dp, 240.dp)) {
                    SearchScreen(
                        state = com.example.cepapplication.ui.CepScreenState(lastSuccessfulAddress = a),
                        onInputChange = {},
                        onSearch = {},
                        onOpenSaved = {},
                    )
                }
            }
        }

        composeRule.onNode(hasScrollAction()).assertHasScrollAction()
        composeRule.onNodeWithText("CEPs armazenados").performScrollTo().assertIsDisplayed()
    }

    private fun createViewModel(handle: SavedStateHandle): CepViewModel {
        val repository = CepRepositoryImpl(
            localDataSource = RoomCepLocalDataSource(fixture.database.addressDao()),
            remoteDataSource = remote,
        )
        return CepViewModel(
            getAddressByCep = GetAddressByCepUseCase(repository),
            getSavedAddresses = GetSavedAddressesUseCase(repository),
            savedStateHandle = handle,
        )
    }

    private fun activityAddressLines(address: Address): List<String> =
        RuntimeEnvironment.getApplication().formatAddress(address).lines()

    private fun assertZipCodesInOrder(first: Address, second: Address) {
        val firstNode = zipCodeNodes(first).single()
        val secondNode = zipCodeNodes(second).single()
        assertTrue(
            "Os cartões não estão na ordem recebida",
            firstNode.boundsInRoot.top < secondNode.boundsInRoot.top,
        )
    }

    private fun zipCodeNodes(address: Address) =
        composeRule
            .onAllNodesWithText("CEP: ${formatZipCode(address.zipCode)}", useUnmergedTree = true)
            .fetchSemanticsNodes()
}
