package com.example.cepapplication

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.cepapplication.databinding.ActivityMainBinding
import com.example.cepapplication.data.CepDatabase
import com.example.cepapplication.data.RoomCepRepository
import com.example.cepapplication.ui.CepUiEvent
import com.example.cepapplication.ui.CepUiState
import com.example.cepapplication.ui.CepViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var isUpdatingZipCode = false

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val viewModel: CepViewModel by viewModels {
        CepViewModel.Factory(
            RoomCepRepository(
                cepDao = CepDatabase.getInstance(applicationContext).cepDao()
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarMain)
        setupZipCodeMask()
        observeUiState()

        binding.btnSave.setOnClickListener {
            viewModel.saveZipCode()
        }
    }

    private fun setupZipCodeMask() {
        binding.edtZipCode.doAfterTextChanged { editable ->
            if (isUpdatingZipCode) return@doAfterTextChanged

            viewModel.onZipCodeChanged(editable?.toString().orEmpty())
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect(::render)
                }
                launch {
                    viewModel.events.collect(::handleEvent)
                }
            }
        }
    }

    private fun handleEvent(event: CepUiEvent) {
        when (event) {
            CepUiEvent.ZipCodeSaved -> Toast.makeText(
                this,
                getString(R.string.toast_zip_code_saved),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun render(state: CepUiState) {
        val currentInput = binding.edtZipCode.text.toString()
        if (currentInput != state.inputZipCode) {
            isUpdatingZipCode = true
            binding.edtZipCode.setText(state.inputZipCode)
            binding.edtZipCode.setSelection(state.inputZipCode.length)
            isUpdatingZipCode = false
        }

        val savedZipCode = state.savedZipCode
        binding.txtSavedZipCode.visibility = if (shouldDisplaySavedZipCode(savedZipCode.orEmpty())) {
            View.VISIBLE
        } else {
            View.GONE
        }
        binding.txtSavedZipCode.text = getString(R.string.cep_salvo_text, savedZipCode.orEmpty())
        binding.edtZipCode.error = if (state.isZipCodeInvalid) {
            getString(R.string.error_invalid_zip_code)
        } else {
            null
        }
    }
}

internal fun shouldDisplaySavedZipCode(zipCode: String): Boolean = zipCode.isNotBlank()
