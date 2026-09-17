package com.example.cepapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cepapplication.databinding.FragmentSavedAddressesBinding
import com.example.cepapplication.ui.CepViewModel
import kotlinx.coroutines.launch

class SavedAddressesFragment : Fragment() {
    private var _binding: FragmentSavedAddressesBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: CepViewModel by activityViewModels {
        (requireActivity().application as CepApplication).container.cepViewModelFactory
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSavedAddressesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerSavedAddresses.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = SavedAddressesAdapter(requireContext())
        }
        observeSavedAddresses()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeSavedAddresses() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.observeSavedAddresses()
                viewModel.savedAddressesState.collect { state ->
                    val hasAddresses = state.addresses.isNotEmpty()
                    val showEmptyAddresses = state.hasLoaded && !hasAddresses
                    binding.txtSavedAddressesTitle.visibility =
                        if (hasAddresses) View.VISIBLE else View.GONE
                    binding.txtEmptyAddresses.visibility =
                        if (showEmptyAddresses) View.VISIBLE else View.GONE
                    (binding.recyclerSavedAddresses.adapter as SavedAddressesAdapter)
                        .submitList(state.addresses)
                    state.errorFeedbackId?.let { id ->
                        Toast.makeText(requireContext(), R.string.error_unexpected, Toast.LENGTH_LONG).show()
                        viewModel.consumeSavedAddressesError(id)
                    }
                }
            }
        }
    }
}
