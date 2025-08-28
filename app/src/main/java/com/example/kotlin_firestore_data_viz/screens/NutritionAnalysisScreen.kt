package com.example.kotlin_firestore_data_viz.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.kotlin_firestore_data_viz.viewmodels.FoodState
import com.example.kotlin_firestore_data_viz.viewmodels.OpenFoodFactsViewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionAnalysisScreen(
    navController: NavController,
    barcode: String?
) {
    val context = LocalContext.current
    val viewModel: OpenFoodFactsViewModel = viewModel()
    val foodState by viewModel.foodData.collectAsState()

    LaunchedEffect(barcode) {
        if (barcode != null) {
            viewModel.fetchProductByBarcode(barcode)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Ravintoarvot / Nutrition Facts") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when (foodState) {
                is FoodState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is FoodState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Tuotetta ei löytynyt\nProduct not found",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (barcode != null) {
                                    viewModel.fetchProductByBarcode(barcode)
                                }
                            },
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text("Yritä uudelleen / Try Again")
                        }

                        OutlinedButton(
                            onClick = {
                                val url = "https://world.openfoodfacts.org/add-new"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text("Lisää tuote / Add Product")
                        }
                    }
                }

                is FoodState.Success -> {
                    val foodItem = (foodState as FoodState.Success)

                    // Nutrition data for pie chart (filter out zero values)
                    val nutritionData = mutableListOf<PieEntry>().apply {
                        foodItem.protein?.takeIf { it > 0f }?.let { add(PieEntry(it, "Proteiini/Protein")) }
                        foodItem.carbs?.takeIf { it > 0f }?.let { add(PieEntry(it, "Hiilihydraatit/Carbs")) }
                        foodItem.fat?.takeIf { it > 0f }?.let { add(PieEntry(it, "Rasva/Fat")) }
                        foodItem.sugar?.takeIf { it > 0f }?.let { add(PieEntry(it, "Sokeri/Sugar")) }
                        foodItem.fiber?.takeIf { it > 0f }?.let { add(PieEntry(it, "Kuitu/Fiber")) }
                    }

                    Column {
                        // Product info
                        Text(
                            text = foodItem.name,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        foodItem.brand?.let { brand ->
                            Text(
                                text = brand,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        // Highlight Finnish products
                        if (foodItem.origin?.contains("Finland", ignoreCase = true) == true ||
                            foodItem.brand?.contains("Suomi", ignoreCase = true) == true) {
                            Text(
                                text = "🇫🇮 Suomalainen tuote / Finnish product",
                                color = Color.Blue,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        // Pie Chart (only show if we have data)
                        if (nutritionData.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .padding(vertical = 16.dp)
                            ) {
                                AndroidView(
                                    factory = { context ->
                                        PieChart(context).apply {
                                            // Basic configuration
                                            setUsePercentValues(false)
                                            description.isEnabled = false
                                            setDrawEntryLabels(true)
                                            setEntryLabelColor(android.graphics.Color.BLACK) // Using Android Color
                                            setEntryLabelTextSize(12f)

                                            // Visual customization
                                            setDrawHoleEnabled(true)
                                            holeRadius = 50f
                                            transparentCircleRadius = 55f
                                            setHoleColor(android.graphics.Color.TRANSPARENT) // Using Android Color

                                            // Data setup
                                            val dataSet = PieDataSet(nutritionData, "").apply {
                                                colors = ColorTemplate.MATERIAL_COLORS.toList()
                                                valueTextSize = 14f
                                                valueTextColor = android.graphics.Color.WHITE // Using Android Color
                                                setDrawValues(true)
                                            }

                                            data = PieData(dataSet).apply {
                                                setValueFormatter(object : com.github.mikephil.charting.formatter.ValueFormatter() {
                                                    override fun getFormattedValue(value: Float): String {
                                                        return "%.1fg".format(value)
                                                    }
                                                })
                                            }

                                            // Animation
                                            animateY(1000)
                                            invalidate()
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Nutrition details
                        Column(modifier = Modifier.padding(16.dp)) {
                            foodItem.energy?.let {
                                NutritionRow("Energia / Energy", "${it} kcal")
                            }
                            foodItem.protein?.let {
                                NutritionRow("Proteiini / Protein", "${it}g")
                            }
                            foodItem.carbs?.let {
                                NutritionRow("Hiilihydraatit / Carbs", "${it}g")
                            }
                            foodItem.fat?.let {
                                NutritionRow("Rasva / Fat", "${it}g")
                            }
                            foodItem.sugar?.let {
                                NutritionRow("Sokeri / Sugar", "${it}g")
                            }
                            foodItem.fiber?.let {
                                NutritionRow("Kuitu / Fiber", "${it}g")
                            }
                            foodItem.salt?.let {
                                NutritionRow("Suola / Salt", "${it}g")
                            }
                            foodItem.servingSize?.let {
                                NutritionRow("Annoskoko / Serving Size", it)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NutritionRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}