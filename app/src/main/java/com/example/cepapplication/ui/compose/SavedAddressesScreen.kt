package com.example.cepapplication.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cepapplication.R
import com.example.cepapplication.ui.SavedAddressesState

@Composable
fun SavedAddressesScreen(
    state: SavedAddressesState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 32.dp),
    ) {
        if (state.addresses.isNotEmpty()) {
            Text(
                text = stringResource(R.string.saved_address_title),
                modifier = Modifier.padding(horizontal = 24.dp),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(
                    items = state.addresses,
                    key = { address -> address.zipCode },
                ) { address ->
                    AddressCard(address = address)
                }
            }
        } else if (state.hasLoaded && state.readError == null) {
            Text(
                text = stringResource(R.string.no_stored_zip_codes),
                modifier = Modifier.padding(horizontal = 24.dp),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
            )
        }
    }
}
