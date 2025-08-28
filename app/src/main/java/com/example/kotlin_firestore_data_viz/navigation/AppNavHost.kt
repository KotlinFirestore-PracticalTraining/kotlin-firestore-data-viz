package com.example.kotlin_firestore_data_viz.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.kotlin_firestore_data_viz.screens.*

sealed class Screen(val route: String, val icon: ImageVector, val title: String) {
    object DataViz : Screen("data_viz", Icons.Filled.BarChart, "Charts")
    object ColorAnalysis : Screen("color_analysis", Icons.Filled.ColorLens, "Colors")
    object Detector : Screen("image_detector", Icons.Filled.Search, "Detector")
    object ImageEditor : Screen("image_editor", Icons.Filled.PhotoFilter, "Editor")
    object FoodSection : Screen("food_section", Icons.Filled.EmojiFoodBeverage, "Food")
}

private val allScreens = listOf(
    Screen.DataViz,
    Screen.ColorAnalysis,
    Screen.Detector,
    Screen.ImageEditor,
    Screen.FoodSection
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                allScreens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.DataViz.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.DataViz.route) { DataVizScreen() }
            composable(Screen.ColorAnalysis.route) { ColorAnalysisScreen() }
            composable(Screen.Detector.route) { ImageDetectorScreen() }
            composable(Screen.ImageEditor.route) { ImageEditorScreen() }
            composable(Screen.FoodSection.route) { FoodSectionScreen(navController) }

            // QR/Barcode Scanner Flow
            composable("qrScanner") {
                QrScannerScreen(navController = navController)
            }
            composable("nutritionAnalysis/{barcode}") { backStackEntry ->
                val barcode = backStackEntry.arguments?.getString("barcode")
                NutritionAnalysisScreen(
                    navController = navController,
                    barcode = barcode
                )
            }

            // Other screens
            composable("eCodeExplainer") { ECodeExplainerScreen(navController) }
            composable("ingredientChecker") { IngredientCheckerScreen(navController) }
            composable("foodComparison") { FoodComparisonScreen(navController) }
            composable("labelScanner") { LabelScannerScreen(navController) }
        }
    }
}