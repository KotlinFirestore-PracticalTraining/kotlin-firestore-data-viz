package com.example.kotlin_firestore_data_viz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.kotlin_firestore_data_viz.navigation.AppNavHost
import com.example.kotlin_firestore_data_viz.ui.theme.KotlinfirestoredatavizTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KotlinfirestoredatavizTheme  {
                AppNavHost()
            }
        }
    }
}
