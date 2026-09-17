package com.example.cepapplication

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cepapplication.databinding.ItemSavedAddressBinding
import com.example.cepapplication.domain.model.Address

class SavedAddressesAdapter(
    private val context: Context,
) : ListAdapter<Address, SavedAddressesAdapter.ViewHolder>(AddressDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSavedAddressBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemSavedAddressBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(address: Address) {
            binding.txtSavedAddress.text = context.formatAddress(address)
        }
    }

}

private object AddressDiffCallback : DiffUtil.ItemCallback<Address>() {
    override fun areItemsTheSame(oldItem: Address, newItem: Address): Boolean =
        oldItem.zipCode == newItem.zipCode

    override fun areContentsTheSame(oldItem: Address, newItem: Address): Boolean =
        oldItem == newItem
}
