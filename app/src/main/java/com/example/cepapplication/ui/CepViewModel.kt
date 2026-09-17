package com.example.cepapplication.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cepapplication.domain.model.Address
import com.example.cepapplication.domain.usecase.GetAddressByCepUseCase
import com.example.cepapplication.domain.usecase.GetSavedAddressesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CepViewModel(
    private val getAddressByCep: GetAddressByCepUseCase,
    private val getSavedAddresses: GetSavedAddressesUseCase,
) : ViewModel() {
    private val _screenState = MutableStateFlow(CepScreenState())
    val screenState: StateFlow<CepScreenState> = _screenState.asStateFlow()

    // Mantido enquanto SearchFragment ainda consome o estado funcional diretamente.
    val uiState: StateFlow<CepUiState> = screenState
        .map { state -> state.status }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = CepUiState.Idle,
        )

    private var nextFeedbackId = 0L

    private val _savedAddressesState = MutableStateFlow(SavedAddressesState())
    val savedAddressesState: StateFlow<SavedAddressesState> = _savedAddressesState.asStateFlow()

    val savedAddresses: StateFlow<List<Address>> = savedAddressesState
        .map { state -> state.addresses }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList(),
        )

    private var observationJob: Job? = null
    private var restorationJob: Job? = null
    private var restorationVersion = 0L

    init {
        observeSavedAddresses()
    }

    fun observeSavedAddresses() {
        if (observationJob?.isActive == true) return
        observationJob = viewModelScope.launch {
            getSavedAddresses()
                .catch { error ->
                    if (error is CancellationException) throw error
                    _savedAddressesState.value = _savedAddressesState.value.copy(
                        readError = error,
                        errorFeedbackId = ++nextFeedbackId,
                    )
                }
                .collect { addresses ->
                    _savedAddressesState.value = SavedAddressesState(
                        addresses = addresses,
                        hasLoaded = true,
                    )
                }
        }
    }

    fun search(rawZipCode: String) {
        invalidateRestoration()
        viewModelScope.launch {
            _screenState.value = _screenState.value.copy(
                status = CepUiState.Loading,
                input = rawZipCode,
                feedback = null,
            )
            getAddressByCep(rawZipCode).fold(
                onSuccess = { address ->
                    _screenState.value = _screenState.value.copy(
                        status = CepUiState.Success(address),
                        input = "",
                        lastSuccessfulAddress = address,
                        feedback = CepFeedback(
                            id = ++nextFeedbackId,
                            type = CepFeedback.Type.LookupCompleted,
                        ),
                    )
                },
                onFailure = { error ->
                    _screenState.value = _screenState.value.copy(
                        status = CepUiState.Error(error),
                        input = rawZipCode,
                        feedback = failureFeedback(error),
                    )
                },
            )
        }
    }

    fun updateInput(input: String) {
        _screenState.value = _screenState.value.copy(input = input)
    }

    fun consumeSavedAddressesError(id: Long) {
        if (_savedAddressesState.value.errorFeedbackId == id) {
            _savedAddressesState.value = _savedAddressesState.value.copy(errorFeedbackId = null)
        }
    }

    private fun failureFeedback(error: Throwable) = CepFeedback(
        id = ++nextFeedbackId,
        type = CepFeedback.Type.LookupFailed,
        cause = error,
    )

    fun consumeFeedback(feedbackId: Long) {
        if (_screenState.value.feedback?.id == feedbackId) {
            _screenState.value = _screenState.value.copy(feedback = null)
        }
    }

    fun loadLatestAddress() {
        if (_screenState.value.status != CepUiState.Idle) return

        val version = ++restorationVersion
        restorationJob?.cancel()
        restorationJob = viewModelScope.launch {
            try {
                val address = getSavedAddresses().first().firstOrNull()
                if (address != null && isCurrentRestoration(version)) {
                    _screenState.value = _screenState.value.copy(
                        status = CepUiState.Success(address),
                        lastSuccessfulAddress = address,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (isCurrentRestoration(version)) {
                    _screenState.value = _screenState.value.copy(
                        status = CepUiState.Error(error),
                        feedback = failureFeedback(error),
                    )
                }
            }
        }
    }

    private fun invalidateRestoration() {
        restorationVersion += 1
        restorationJob?.cancel()
        restorationJob = null
    }

    private fun isCurrentRestoration(version: Long): Boolean =
        restorationVersion == version && _screenState.value.status == CepUiState.Idle
}

class CepViewModelFactory(
    private val getAddressByCep: GetAddressByCepUseCase,
    private val getSavedAddresses: GetSavedAddressesUseCase,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(CepViewModel::class.java)) {
            "ViewModel não suportado: ${modelClass.name}"
        }
        return CepViewModel(getAddressByCep, getSavedAddresses) as T
    }
}
