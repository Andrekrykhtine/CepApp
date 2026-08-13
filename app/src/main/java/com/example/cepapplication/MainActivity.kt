package com.example.cepapplication

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.example.cepapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private companion object {
        const val PREFS_NAME = "app_data"
        const val ZIP_CODE_KEY = "saved_zip_code"
    }

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val sharedPrefs by lazy {
        getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        setSupportActionBar(binding.toolbarMain)
        setupZipCodeMask()
        loadZipCode()

        binding.btnSave.setOnClickListener {
            saveZipCode()
        }
    }

    private fun setupZipCodeMask() {
        var isUpdating = false

        binding.edtZipCode.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }

            override fun afterTextChanged(editable: Editable?) {
                if (isUpdating) return

                val currentText = editable
                    ?.toString()
                    .orEmpty()

                val formattedText = formatZipCode(currentText)

                if (currentText != formattedText) {
                    isUpdating = true

                    binding.edtZipCode.setText(formattedText)
                    binding.edtZipCode.setSelection(formattedText.length)

                    isUpdating = false
                }
            }
        })
    }

    private fun loadZipCode() {
        val savedZipCode = sharedPrefs
            .getString(ZIP_CODE_KEY, "")
            .orEmpty()

        val formattedZipCode = formatZipCode(savedZipCode)
        displaySavedZipCode(formattedZipCode)
    }

    private fun displaySavedZipCode(zipCode: String) {
        binding.txtSavedZipCode.text =
            getString(R.string.cep_salvo_text, zipCode)
    }

    private fun saveZipCode() {
        val zipCode = binding.edtZipCode.text
            .toString()
            .filter { it.isDigit() }

        if (zipCode.length != 8) {
            binding.edtZipCode.error = getString(R.string.error_invalid_zip_code)
            return
        }

        sharedPrefs.edit {
            putString(ZIP_CODE_KEY, zipCode)
        }

        displaySavedZipCode(formatZipCode(zipCode))

        binding.edtZipCode.text.clear()

        Toast.makeText(
            this,
            getString(R.string.toast_zip_code_saved),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun formatZipCode(value: String): String {
        val numbers = value
            .filter { it.isDigit() }
            .take(8)

        return if (numbers.length > 5) {
            "${numbers.substring(0, 5)}-${numbers.substring(5)}"
        } else {
            numbers
        }
    }
}