package com.example.cepapplication.ui.compose

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cepapplication.R
import com.example.cepapplication.data.local.LocalStorageException
import com.example.cepapplication.domain.usecase.CepNotFoundException
import com.example.cepapplication.domain.usecase.InvalidCepException
import com.example.cepapplication.ui.CepFeedback
import com.example.cepapplication.ui.CepViewModel
import java.io.IOException

private const val SEARCH_ROUTE = "search"
private const val SAVED_ADDRESSES_ROUTE = "saved_addresses"

@Composable
fun CepApp(
    viewModel: CepViewModel,
    modifier: Modifier = Modifier,
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val savedAddressesState by viewModel.savedAddressesState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: SEARCH_ROUTE

    LaunchedEffect(Unit) {
        viewModel.loadLatestAddress()
    }

    LaunchedEffect(screenState.feedback?.id) {
        val feedback = screenState.feedback ?: return@LaunchedEffect
        Toast.makeText(
            context,
            feedbackMessage(feedback),
            if (feedback.type == CepFeedback.Type.LookupCompleted) {
                Toast.LENGTH_SHORT
            } else {
                Toast.LENGTH_LONG
            },
        ).show()
        viewModel.consumeFeedback(feedback.id)
    }

    LaunchedEffect(savedAddressesState.errorFeedbackId) {
        val feedbackId = savedAddressesState.errorFeedbackId ?: return@LaunchedEffect
        Toast.makeText(context, R.string.error_unexpected, Toast.LENGTH_LONG).show()
        viewModel.consumeSavedAddressesError(feedbackId)
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute == SAVED_ADDRESSES_ROUTE) {
            viewModel.observeSavedAddresses()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CepTopAppBar(
                route = currentRoute,
                onBack = { navController.popBackStack() },
            )
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = SEARCH_ROUTE,
            modifier = Modifier.padding(contentPadding),
        ) {
            composable(SEARCH_ROUTE) {
                SearchScreen(
                    state = screenState,
                    onInputChange = viewModel::updateInput,
                    onSearch = { viewModel.search(screenState.input) },
                    onOpenSaved = {
                        navController.navigate(SAVED_ADDRESSES_ROUTE) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(SAVED_ADDRESSES_ROUTE) {
                SavedAddressesScreen(state = savedAddressesState)
            }
        }
    }
}

@Composable
private fun CepTopAppBar(
    route: String,
    onBack: () -> Unit,
) {
    val title = when (route) {
        SAVED_ADDRESSES_ROUTE -> stringResource(R.string.stored_zip_codes_title)
        else -> stringResource(R.string.toolbar_title)
    }

    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (route == SAVED_ADDRESSES_ROUTE) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.navigate_back),
                    )
                }
            }
        },
    )
}

private fun feedbackMessage(feedback: CepFeedback): Int = when (feedback.type) {
    CepFeedback.Type.LookupCompleted -> R.string.toast_zip_code_saved
    CepFeedback.Type.LookupFailed -> when (feedback.cause) {
        is InvalidCepException -> R.string.error_invalid_zip_code
        is CepNotFoundException -> R.string.error_zip_code_not_found
        is IOException -> R.string.error_network
        is LocalStorageException -> R.string.error_unexpected
        else -> R.string.error_unexpected
    }
}
