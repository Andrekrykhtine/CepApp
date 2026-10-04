package com.example.cepapplication.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cepapplication.R
import com.example.cepapplication.formatZipCode
import com.example.cepapplication.ui.CepScreenState
import com.example.cepapplication.ui.CepUiState

@Composable
fun SearchScreen(
    state: CepScreenState,
    onInputChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLoading = state.status is CepUiState.Loading
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .imePadding()
            .padding(vertical = 32.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = stringResource(R.string.text_main),
            modifier = Modifier.padding(horizontal = 24.dp),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.input,
            onValueChange = { value -> onInputChange(formatZipCode(value)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            enabled = !isLoading,
            label = { Text(stringResource(R.string.text_main)) },
            placeholder = { Text(stringResource(R.string.hint_text)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onSearch,
                enabled = !isLoading,
            ) {
                Text(stringResource(R.string.btn_text))
            }

            if (isLoading) {
                Spacer(modifier = Modifier.size(8.dp))
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .semantics {
                            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                        },
                    strokeWidth = 3.dp,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        state.lastSuccessfulAddress?.let { address ->
            AddressCard(
                address = address,
                title = stringResource(R.string.last_address_title),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onOpenSaved,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 24.dp),
        ) {
            Text(stringResource(R.string.stored_zip_codes_button))
        }
    }
}
