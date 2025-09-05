package com.example.kotlin_firestore_data_viz.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.kotlin_firestore_data_viz.viewmodels.FoodState
import com.example.kotlin_firestore_data_viz.viewmodels.OpenFoodFactsViewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionAnalysisScreen(
    navController: NavController,
    barcode: String?
) {
    val context = LocalContext.current
    val vm: OpenFoodFactsViewModel = viewModel()
    val state by vm.foodData.collectAsState()

    LaunchedEffect(barcode) {
        if (barcode != null) vm.fetchProductByBarcode(barcode)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Ravintoarvot / Nutrition Facts",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { padding ->
        when (state) {
            is FoodState.Loading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is FoodState.Error -> {
                val msg = (state as FoodState.Error).message
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Tuotetta ei löytynyt / Product not found\n$msg",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(24.dp))
                    FilledTonalButton(onClick = { barcode?.let { vm.fetchProductByBarcode(it) } }) {
                        Text("Yritä uudelleen / Try Again")
                    }
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = {
                        val url = "https://world.openfoodfacts.org/add-new"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }) { Text("Lisää tuote / Add Product") }
                }
            }

            is FoodState.Success -> {
                val item = (state as FoodState.Success)

                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                item.name,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                            item.brand?.let {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    it,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            if (item.origin?.contains("Finland", true) == true ||
                                item.brand?.contains("Suomi", true) == true
                            ) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "🇫🇮 Suomalainen tuote / Finnish product",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Nutrition Section
                    if (item.nutrients100g.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.padding(20.dp)) {
                                Text(
                                    "Ravintosisältö / Nutrition per 100g",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                // --- Pie data: keep at most TOP_N slices by grouping the rest into "Other" ---
                                val TOP_N = 6 // total slices <= 6 (top 5 + "Other")
                                // Normalize numeric values to Float and filter noise
                                val normalized = item.nutrients100g
                                    .mapNotNull { (k, v) ->
                                        val f = try { v.toFloat() } catch (_: Throwable) { null }
                                        if (k.isNotBlank() && f != null && f.isFinite() && f > 0f) k to f else null
                                    }
                                val sorted = normalized.sortedByDescending { it.second }

                                val (top, rest) = if (sorted.size > TOP_N) {
                                    Pair(sorted.take(TOP_N - 1), sorted.drop(TOP_N - 1))
                                } else {
                                    Pair(sorted, emptyList())
                                }

                                val pieEntries = buildList {
                                    addAll(top.map { (k, v) -> PieEntry(v, k) })
                                    if (rest.isNotEmpty()) {
                                        val otherSum = rest.sumOf { it.second.toDouble() }.toFloat()
                                        if (otherSum > 0f) add(PieEntry(otherSum, "Other"))
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                AndroidView(
                                    factory = { ctx ->
                                        PieChart(ctx).apply {
                                            setUsePercentValues(false)
                                            description.isEnabled = false

                                            // 🚫 remove labels from slices
                                            setDrawEntryLabels(false)

                                            setDrawHoleEnabled(true)
                                            holeRadius = 40f
                                            transparentCircleRadius = 45f
                                            setHoleColor(android.graphics.Color.TRANSPARENT)

                                            val dataSet = PieDataSet(pieEntries, "").apply {
                                                colors = listOf(
                                                    android.graphics.Color.parseColor("#4CAF50"),
                                                    android.graphics.Color.parseColor("#2196F3"),
                                                    android.graphics.Color.parseColor("#FF9800"),
                                                    android.graphics.Color.parseColor("#9C27B0"),
                                                    android.graphics.Color.parseColor("#F44336"),
                                                    android.graphics.Color.parseColor("#607D8B"),
                                                    android.graphics.Color.parseColor("#795548"),
                                                    android.graphics.Color.parseColor("#009688")
                                                )
                                                valueTextSize = 11f
                                                valueTextColor = android.graphics.Color.WHITE
                                                setDrawValues(true)
                                                sliceSpace = 2f
                                            }
                                            data = PieData(dataSet)

                                            legend.isWordWrapEnabled = true
                                            legend.isEnabled = true
                                            legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
                                            legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                                            legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER

                                            animateY(1000)
                                            invalidate()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp)
                                        .padding(vertical = 12.dp)
                                )

                                Spacer(Modifier.height(16.dp))

                                // Nutrition Values (show ALL raw items below, ungrouped)
                                item.nutrients100g.forEach { n ->
                                    NutritionRow(n.key, "${n.value} / 100g")
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }

                    // E-codes Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                "Lisäaineet (E-koodit) / Food additives",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (item.additivesTags.isEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Ei ilmoitettuja lisäaineita / No additives listed",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Spacer(Modifier.height(16.dp))
                                item.additivesTags.forEach { code ->
                                    ECodeCard(code, context)
                                    Spacer(Modifier.height(10.dp))
                                }
                            }
                        }
                    }

                    // Allergens
                    if (item.allergensText.isNotBlank() || item.allergensTags.isNotEmpty()) {
                        Spacer(Modifier.height(20.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.padding(20.dp)) {
                                Text(
                                    "⚠️ Allergeenit / Allergens",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.height(12.dp))
                                if (item.allergensText.isNotBlank()) {
                                    Text(
                                        item.allergensText,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                } else {
                                    Text(
                                        item.allergensTags.joinToString(", "),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
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
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Divider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            thickness = 0.5.dp
        )
    }
}

@Composable
private fun ECodeCard(code: String, context: android.content.Context) {
    val clipboard = LocalClipboardManager.current
    val (title, subtitle) = remember(code) { prettyAdditive(code) }

    // stable mini-palette → consistent color per code
    val palette = listOf(
        Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFF9800), Color(0xFF9C27B0),
        Color(0xFFF44336), Color(0xFF607D8B), Color(0xFF795548), Color(0xFF009688)
    )
    val chipColor = remember(code) {
        val idx = (code.hashCode().let { if (it < 0) -it else it }) % palette.size
        palette[idx]
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val url = "https://world.openfoodfacts.org/additive/${code.lowercase()}"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colored dot/avatar
            Surface(
                color = chipColor,
                shape = CircleShape,
                modifier = Modifier.size(14.dp),
                content = {}
            )

            Spacer(Modifier.width(12.dp))

            // Title + subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title, // e.g., "E 330"
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle, // e.g., "Food additive code"
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Actions
            IconButton(onClick = { clipboard.setText(AnnotatedString(title)) }) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy code")
            }
            IconButton(onClick = {
                val url = "https://world.openfoodfacts.org/additive/${code.lowercase()}"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }) {
                Icon(Icons.Outlined.OpenInNew, contentDescription = "Open details")
            }
        }
    }
}
private fun prettyAdditive(raw: String): Pair<String, String> {
    val slug = raw.substringAfterLast(":").trim().replace('_', '-')
    val compact = slug.replace(" ", "")
    val eRegex = Regex("(?i)^e\\d{3,4}[a-z]?$")
    return if (eRegex.matches(compact)) {
        "E " + compact.drop(1).uppercase() to "Food additive code"
    } else {
        // Title-case fallback for non-E labels (e.g., "nitrites")
        val words = slug.split('-', '_').filter { it.isNotBlank() }
        val title = words.joinToString(" ") { w ->
            w.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        title to ""
    }
}
