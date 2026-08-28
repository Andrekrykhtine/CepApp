package com.example.cepapplication

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.cepapplication.data.Address
import com.example.cepapplication.data.AddressRepository
import com.example.cepapplication.data.AddressStore
import com.example.cepapplication.data.ViaCepService
import com.example.cepapplication.databinding.ActivityMainBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private companion object {
        const val PREFS_NAME = "app_data"
    }

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val addressStore by lazy {
        AddressStore(getSharedPreferences(PREFS_NAME, MODE_PRIVATE))
    }

    private val addressRepository by lazy {
        AddressRepository(ViaCepService.api)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarMain)
        setupZipCodeMask()
        displaySavedAddress(addressStore.load())

        binding.btnSave.setOnClickListener {
            lookupAndSaveZipCode()
        }
    }

    private fun setupZipCodeMask() {
        var isUpdating = false

        binding.edtZipCode.doAfterTextChanged { editable ->
            if (isUpdating) return@doAfterTextChanged

            val currentText = editable?.toString().orEmpty()
            val formattedText = formatZipCode(currentText)

            if (currentText != formattedText) {
                isUpdating = true
                binding.edtZipCode.setText(formattedText)
                binding.edtZipCode.setSelection(formattedText.length)
                isUpdating = false
            }
        }
    }

    private fun lookupAndSaveZipCode() {
        val zipCode = binding.edtZipCode.text.toString().filter(Char::isDigit)

        if (zipCode.length != 8) {
            binding.edtZipCode.error = getString(R.string.error_invalid_zip_code)
            return
        }

        lifecycleScope.launch {
            setLoading(true)
            try {
                val address = addressRepository.findAddress(zipCode)
                if (address == null) {
                    binding.edtZipCode.error = getString(R.string.error_zip_code_not_found)
                    return@launch
                }

                addressStore.save(address)
                displaySavedAddress(address)
                binding.edtZipCode.text.clear()
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.toast_zip_code_saved),
                    Toast.LENGTH_SHORT,
                ).show()
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                showLookupError()
            } catch (error: Exception) {
                showLookupError()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnSave.isEnabled = !isLoading
        binding.edtZipCode.isEnabled = !isLoading
        binding.progressLookup.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showLookupError() {
        Toast.makeText(this, getString(R.string.error_network), Toast.LENGTH_LONG).show()
    }

    private fun displaySavedAddress(address: Address?) {
        binding.cardSavedAddress.visibility = if (address == null) View.GONE else View.VISIBLE
        if (address == null) return

        binding.txtSavedAddress.text = listOf(
            getString(R.string.cep_salvo_text, formatZipCode(address.zipCode)),
            getString(R.string.street_text, displayValue(address.street)),
            getString(R.string.complement_text, displayValue(address.complement)),
            getString(R.string.neighborhood_text, displayValue(address.neighborhood)),
            getString(R.string.city_text, displayValue(address.city)),
            getString(R.string.state_abbreviation_text, displayValue(address.stateAbbreviation)),
            getString(R.string.state_text, displayValue(address.state)),
        ).joinToString(separator = "\n")
    }

    private fun displayValue(value: String): String =
        value.ifBlank { getString(R.string.not_informed) }
}

internal fun formatZipCode(value: String): String {
    val numbers = value.filter(Char::isDigit).take(8)
    return if (numbers.length > 5) {
        "${numbers.substring(0, 5)}-${numbers.substring(5)}"
    } else {
        numbers
    }
}
