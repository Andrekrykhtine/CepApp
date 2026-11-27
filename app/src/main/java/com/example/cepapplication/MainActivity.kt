package com.example.cepapplication

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.cepapplication.ui.CepViewModel
import com.example.cepapplication.ui.compose.CepApp
import com.example.cepapplication.ui.compose.CepTheme

class MainActivity : AppCompatActivity() {

    val cepViewModel: CepViewModel by viewModels {
        (application as CepApplication).container.cepViewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CepTheme {
                CepApp(viewModel = cepViewModel)
            }
        }
    }
}
