package com.example.kotlin_firestore_data_viz.screens

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FoodSectionScreen(navController: NavController) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Button for QR Scanner (simplified version)
        Button(
            onClick = { navController.navigate("qrScanner") },
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Scan Food QR Code")
        }

        // Button for Nutrition Pie Chart
        Button(
            onClick = { navController.navigate("nutritionAnalysis") },
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Nutrition Analysis (Pie Chart)")
        }

        // Button for E-Code Explanation
        Button(
            onClick = { navController.navigate("eCodeExplainer") },
            modifier = Modifier.padding(8.dp)
        ) {
            Text("E-Code Explanations")
        }

        // Button for Blacklisted Ingredients
        Button(
            onClick = { navController.navigate("ingredientChecker") },
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Check Blacklisted Ingredients")
        }

        // Button for Food Comparison
        Button(
            onClick = { navController.navigate("foodComparison") },
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Compare Similar Foods")
        }
    }
}