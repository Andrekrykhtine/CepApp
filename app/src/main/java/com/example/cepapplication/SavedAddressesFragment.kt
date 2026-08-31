package com.example.cepapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.cepapplication.databinding.FragmentSavedAddressesBinding
import kotlinx.coroutines.launch

class SavedAddressesFragment : Fragment() {
    private var _binding: FragmentSavedAddressesBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel get() = (requireActivity() as MainActivity).cepViewModel

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
        loadSavedAddresses()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadSavedAddresses() {
        viewModel.loadSavedAddresses()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.savedAddresses.collect { addresses ->
                    val hasAddresses = addresses.isNotEmpty()
                    binding.cardSavedAddresses.visibility =
                        if (hasAddresses) View.VISIBLE else View.GONE
                    binding.txtEmptyAddresses.visibility =
                        if (hasAddresses) View.GONE else View.VISIBLE
                    binding.txtSavedAddresses.text =
                        addresses.joinToString(separator = "\n\n") { address ->
                            requireContext().formatAddress(address)
                        }
                }
            }
        }
    }
}
