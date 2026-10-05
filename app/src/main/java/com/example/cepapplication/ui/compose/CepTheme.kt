package com.example.cepapplication.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.example.cepapplication.R

@Composable
fun CepTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val primaryBlue = colorResource(R.color.primary_color)
    val accentYellow = colorResource(R.color.primary_text_color)
    val secondaryBlue = colorResource(R.color.secondary_color)

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF8CCBEE),
            onPrimary = Color(0xFF00344F),
            primaryContainer = primaryBlue,
            onPrimaryContainer = accentYellow,
            secondary = secondaryBlue,
            onSecondary = Color.Black,
            background = Color(0xFF121212),
            onBackground = Color(0xFFF5F5F5),
            surface = Color(0xFF121212),
            onSurface = Color(0xFFF5F5F5),
            surfaceVariant = Color(0xFF2B3235),
            onSurfaceVariant = Color(0xFFE4E9EC),
        )
    } else {
        lightColorScheme(
            primary = primaryBlue,
            onPrimary = accentYellow,
            secondary = secondaryBlue,
            onSecondary = Color.Black,
            background = Color.White,
            onBackground = Color(0xFF1B1B1B),
            surface = Color.White,
            onSurface = Color(0xFF1B1B1B),
            surfaceVariant = Color(0xFFE3E8EB),
            onSurfaceVariant = Color(0xFF37474F),
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
