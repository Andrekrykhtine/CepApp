package com.example.cepapplication

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cepapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private companion object {
        const val NOME_PREFERENCIAS = "dados_app"
        const val CHAVE_CEP = "cep_salvo"
    }

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val preferencias by lazy {
        getSharedPreferences(NOME_PREFERENCIAS, MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(binding.root)

        binding.tbPrincipal.title = "Busca CEP"
        binding.tbPrincipal.setTitleTextColor(getColor(android.R.color.white))
        setSupportActionBar(binding.tbPrincipal)

        binding.btnSalvar.setOnClickListener {
            val cep = binding.etCep.text.toString()

            preferencias.edit()
                .putString(CHAVE_CEP, cep)
                .apply()
        }

        val cepSalvo = preferencias.getString(CHAVE_CEP, null)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}