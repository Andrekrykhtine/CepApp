package com.example.cepapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.cepapplication.databinding.FragmentSearchBinding
import com.example.cepapplication.data.local.LocalStorageException
import com.example.cepapplication.domain.model.Address
import com.example.cepapplication.domain.usecase.CepNotFoundException
import com.example.cepapplication.domain.usecase.InvalidCepException
import com.example.cepapplication.ui.CepScreenState
import com.example.cepapplication.ui.CepFeedback
import com.example.cepapplication.ui.CepUiState
import kotlinx.coroutines.launch
import java.io.IOException

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: CepViewModel by activityViewModels {
        (requireActivity().application as CepApplication).container.cepViewModelFactory
    }

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
        observeUiState()
        viewModel.loadLatestAddress()

        binding.btnSave.setOnClickListener {
            viewModel.search(binding.edtZipCode.text.toString())
        }
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
            viewModel.updateInput(formattedText)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.screenState.collect(::renderScreenState)
            }
        }
    }

    private fun renderScreenState(state: CepScreenState) {
        renderInput(state.input)
        displayLatestAddress(state.lastSuccessfulAddress)
        setLoading(state.status is CepUiState.Loading)

        binding.edtZipCode.error = when ((state.status as? CepUiState.Error)?.cause) {
            is InvalidCepException -> getString(R.string.error_invalid_zip_code)
            is CepNotFoundException -> getString(R.string.error_zip_code_not_found)
            else -> null
        }

        state.feedback?.let { feedback ->
            when (feedback.type) {
                CepFeedback.Type.LookupCompleted -> Toast.makeText(
                    requireContext(), R.string.toast_zip_code_saved, Toast.LENGTH_SHORT,
                ).show()
                CepFeedback.Type.LookupFailed -> showLookupError(requireNotNull(feedback.cause))
            }
            viewModel.consumeFeedback(feedback.id)
        }
    }

    private fun renderInput(input: String) {
        if (binding.edtZipCode.text.toString() != input) {
            binding.edtZipCode.setText(input)
            binding.edtZipCode.setSelection(input.length)
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

    private fun showLookupError(error: Throwable) {
        when (error) {
            is InvalidCepException -> binding.edtZipCode.error =
                getString(R.string.error_invalid_zip_code)
            is CepNotFoundException -> binding.edtZipCode.error =
                getString(R.string.error_zip_code_not_found)
            is LocalStorageException -> Toast.makeText(
                requireContext(),
                R.string.error_unexpected,
                Toast.LENGTH_LONG,
            ).show()
            is IOException -> Toast.makeText(
                requireContext(),
                R.string.error_network,
                Toast.LENGTH_LONG,
            ).show()
            else -> Toast.makeText(
                requireContext(),
                R.string.error_unexpected,
                Toast.LENGTH_LONG,
            ).show()
        }
    }
}
