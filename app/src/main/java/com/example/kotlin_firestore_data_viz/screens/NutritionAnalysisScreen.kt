package com.example.kotlin_firestore_data_viz.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionAnalysisScreen(
    navController: NavController,
    qrData: String? = null,
    barcode: String? = null
) {
    // Sample nutrition data - in a real app, you'd fetch this from an API
    val (nutritionData, productName) = remember(qrData, barcode) {
        when {
            qrData != null -> Pair(
                listOf(
                    PieEntry(28f, "Protein"),
                    PieEntry(42f, "Carbs"),
                    PieEntry(30f, "Fat"),
                    PieEntry(5f, "Fiber"),
                    PieEntry(3f, "Sugar")
                ),
                "Organic Protein Bar"
            )
            barcode != null -> Pair(
                listOf(
                    PieEntry(18f, "Protein"),
                    PieEntry(52f, "Carbs"),
                    PieEntry(30f, "Fat"),
                    PieEntry(8f, "Fiber"),
                    PieEntry(12f, "Sugar")
                ),
                "Granola Cereal"
            )
            else -> Pair(
                listOf(
                    PieEntry(25f, "Protein"),
                    PieEntry(35f, "Carbs"),
                    PieEntry(40f, "Fat")
                ),
                "Generic Food Item"
            )
        }
    }

    val totalCalories = remember(nutritionData) {
        nutritionData.sumOf { it.value.toInt() } * 4 // Simplified calculation
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Nutrition Analysis") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Product Information
            if (qrData != null || barcode != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = productName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ID: ${qrData ?: barcode}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Calories Summary
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    NutritionFactItem(value = "$totalCalories", label = "Calories")
                    Divider(
                        modifier = Modifier
                            .height(50.dp)
                            .width(1.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    NutritionFactItem(value = "${nutritionData.find { it.label == "Protein" }?.value?.toInt() ?: 0}g", label = "Protein")
                    Divider(
                        modifier = Modifier
                            .height(50.dp)
                            .width(1.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    NutritionFactItem(value = "${nutritionData.find { it.label == "Carbs" }?.value?.toInt() ?: 0}g", label = "Carbs")
                }
            }

            // Pie Chart
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(16.dp)
                ) {
                    AndroidView(
                        factory = { context ->
                            PieChart(context).apply {
                                configurePieChart()
                                setPieData(nutritionData)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Nutrition Details
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Nutrition Details",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    nutritionData.forEach { entry ->
                        NutritionDetailRow(
                            name = entry.label ?: "Unknown",
                            value = entry.value,
                            unit = "% DV"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NutritionFactItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun NutritionDetailRow(name: String, value: Float, unit: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "${value.toInt()}$unit",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun PieChart.configurePieChart() {
    setUsePercentValues(true)
    description.isEnabled = false
    setExtraOffsets(5f, 10f, 5f, 5f)
    dragDecelerationFrictionCoef = 0.95f

    isDrawHoleEnabled = true
    setHoleColor(Color.White.value.toInt())
    setTransparentCircleColor(Color.White.value.toInt())
    holeRadius = 58f
    transparentCircleRadius = 61f
    setDrawCenterText(true)
    centerText = "Nutrition\nBreakdown"

    rotationAngle = 0f
    isRotationEnabled = true
    isHighlightPerTapEnabled = true

    val legend = legend
    legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
    legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
    legend.orientation = Legend.LegendOrientation.HORIZONTAL
    legend.setDrawInside(false)
    legend.xEntrySpace = 7f
    legend.yEntrySpace = 0f
    legend.yOffset = 0f
}

private fun PieChart.setPieData(data: List<PieEntry>) {
    val dataSet = PieDataSet(data, "").apply {
        sliceSpace = 3f
        selectionShift = 5f
        colors = ColorTemplate.createColors(
            intArrayOf(
                0xFF4CAF50.toInt(),  // Green
                0xFF2196F3.toInt(),  // Blue
                0xFFF44336.toInt(),  // Red
                0xFFFFC107.toInt(),  // Amber
                0xFF9C27B0.toInt()   // Purple
            )
        )
        valueLinePart1OffsetPercentage = 80f
        valueLinePart1Length = 0.3f
        valueLinePart2Length = 0.4f
        valueLineColor = Color.Black.value.toInt()
        yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
    }

    val pieData = PieData(dataSet).apply {
        setValueFormatter(PercentFormatter(this@setPieData))
        setValueTextSize(12f)
        setValueTextColor(Color.Black.value.toInt())
    }

    this.data = pieData
    invalidate()
}