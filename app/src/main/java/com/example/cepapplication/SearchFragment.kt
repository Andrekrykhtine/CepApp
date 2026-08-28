package com.example.cepapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.cepapplication.data.Address
import com.example.cepapplication.data.AddressRepository
import com.example.cepapplication.data.AddressStore
import com.example.cepapplication.data.ViaCepService
import com.example.cepapplication.databinding.FragmentSearchBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val addressStore by lazy { AddressStore(requireContext().applicationContext) }
    private val addressRepository by lazy { AddressRepository(ViaCepService.api) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupZipCodeMask()
        loadLatestAddress()

        binding.btnSave.setOnClickListener { lookupAndSaveZipCode() }
        binding.btnStoredZipCodes.setOnClickListener {
            findNavController().navigate(R.id.action_searchFragment_to_savedAddressesFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
        if (zipCode.length != ZIP_CODE_LENGTH) {
            binding.edtZipCode.error = getString(R.string.error_invalid_zip_code)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            try {
                val address = addressRepository.findAddress(zipCode)
                if (address == null) {
                    binding.edtZipCode.error = getString(R.string.error_zip_code_not_found)
                    return@launch
                }

                withContext(Dispatchers.IO) { addressStore.save(address) }
                displayLatestAddress(address)
                binding.edtZipCode.text.clear()
                Toast.makeText(
                    requireContext(),
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
                if (_binding != null) {
                    setLoading(false)
                }
            }
        }
    }

    private fun loadLatestAddress() {
        viewLifecycleOwner.lifecycleScope.launch {
            val address = withContext(Dispatchers.IO) { addressStore.loadLatest() }
            displayLatestAddress(address)
        }
    }

    private fun displayLatestAddress(address: Address?) {
        binding.cardLastAddress.visibility = if (address == null) View.GONE else View.VISIBLE
        if (address != null) {
            binding.txtLastAddress.text = requireContext().formatAddress(address)
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnSave.isEnabled = !isLoading
        binding.edtZipCode.isEnabled = !isLoading
        binding.progressLookup.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showLookupError() {
        Toast.makeText(requireContext(), R.string.error_network, Toast.LENGTH_LONG).show()
    }

    private companion object {
        const val ZIP_CODE_LENGTH = 8
    }
}
