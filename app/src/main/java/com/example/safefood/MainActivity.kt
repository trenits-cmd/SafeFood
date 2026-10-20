package com.example.safefood

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.safefood.ui.SafeFoodViewModel
import com.example.safefood.ui.navigation.MainScreen
import com.example.safefood.ui.theme.SafeFoodTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SafeFoodTheme {
                val viewModel: SafeFoodViewModel = viewModel()
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
