package com.example.kotlin_firestore_data_viz.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.PhotoFilter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.*
import com.example.kotlin_firestore_data_viz.screens.ColorAnalysisScreen
import com.example.kotlin_firestore_data_viz.screens.DataVizScreen
import com.example.kotlin_firestore_data_viz.screens.ImageTransformScreen
import com.example.kotlin_firestore_data_viz.screens.ImageDetectorScreen
import com.example.kotlin_firestore_data_viz.screens.ImageEditorScreen



sealed class Screen(val route: String, val icon: ImageVector, val title: String) {
    object DataViz        : Screen("data_viz",        Icons.Filled.BarChart,    "Charts" )
    object ColorAnalysis  : Screen("color_analysis",  Icons.Filled.ColorLens,   "Colors" )
    object ImageTransform : Screen("image_transform", Icons.Filled.PhotoFilter, "Transform")
    object Detector       : Screen("image_detector",        Icons.Filled.Search, "Detector")
    object ImageEditor    : Screen("image_editor", Icons.Filled.PhotoFilter, "Editor")
}

private val allScreens = listOf(
    Screen.DataViz,
    Screen.ColorAnalysis,
    Screen.ImageTransform,
    Screen.Detector,
    Screen.ImageEditor
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val current = navController.currentBackStackEntryAsState().value?.destination?.route
                allScreens.forEach { screen ->
                    NavigationBarItem(
                        icon      = { Icon(screen.icon, contentDescription = screen.title) },
                        label     = { Text(screen.title) },
                        selected  = (current == screen.route),
                        onClick   = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState   = true
                            }
                        }
                    )
                }
            }
        }
    ) { inner ->
        NavHost(
            navController,
            startDestination = Screen.DataViz.route,
            modifier = Modifier.padding(inner)
        ) {
            composable(Screen.DataViz.route) {
                DataVizScreen()
            }
            composable(Screen.ColorAnalysis.route) {
                ColorAnalysisScreen()
            }
            composable(Screen.ImageTransform.route) {
                ImageTransformScreen()
            }
            composable(Screen.Detector.route) {
                ImageDetectorScreen()
            }
            composable(Screen.ImageEditor.route) {
                ImageEditorScreen()
            }

        }
    }
}
