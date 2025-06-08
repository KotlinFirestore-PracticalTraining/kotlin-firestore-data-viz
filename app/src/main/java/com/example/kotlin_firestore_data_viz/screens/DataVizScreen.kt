package com.example.kotlin_firestore_data_viz.screens

import android.graphics.Color as AndroidColor
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet

// ——— DATA MODELS ———
data class ColumnDef(val name: String)
typealias RowData = SnapshotStateList<String>
enum class ChartType { Pie, Bar }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataVizScreen() {
    // — State —
    val columns = remember { mutableStateListOf(ColumnDef("Region")) }
    val rows = remember { mutableStateListOf<RowData>() }
    var selectedChart by remember { mutableStateOf(ChartType.Bar) }
    var selectedColumn by remember { mutableIntStateOf(1) }
    var dropDownOpen by remember { mutableStateOf(false) }
    var showChart by remember { mutableStateOf(false) }
    var expandedChart by remember { mutableStateOf(false) }

    val allFilled = rows.isNotEmpty() &&
            rows.all { r -> r[0].isNotBlank() && r.drop(1).all(String::isNotBlank) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val newRow = mutableStateListOf<String>()
                    repeat(columns.size) { newRow.add("") }
                    rows += newRow
                    showChart = false
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Row") },
                text = { Text("Add Row") },
                containerColor = MaterialTheme.colorScheme.tertiary
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Title
            item {
                Text(
                    "Data Visualization Studio",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }
            // 1) Metrics Definition
            item {
                GradientCard {
                    Column(Modifier.padding(20.dp)) {
                        SectionTitle("Define Metrics", Icons.Outlined.Create)
                        Spacer(Modifier.height(16.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            itemsIndexed(columns) { idx, col ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = col.name,
                                        onValueChange = { columns[idx] = ColumnDef(it) },
                                        label = { Text("Metric ${idx+1}") },
                                        singleLine = true,
                                        modifier = Modifier.widthIn(min = 120.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            focusedLabelColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    if (columns.size > 2) {
                                        FilledIconButton(
                                            onClick = {
                                                columns.removeAt(idx)
                                                rows.forEach { row -> if (idx < row.size) row.removeAt(idx) }
                                                if (selectedColumn >= columns.size) selectedColumn = 1
                                                showChart = false
                                            },
                                            modifier = Modifier.padding(start = 8.dp),
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer
                                            )
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                                        }
                                    }
                                }
                            }
                            item {
                                FilledTonalIconButton(
                                    onClick = {
                                        columns += ColumnDef("Metric")
                                        rows.forEach { it.add("") }
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Metric")
                                }
                            }
                        }
                    }
                }
            }

            // 2) Data Table
            item {
                GradientCard {
                    Column(Modifier.padding(20.dp)) {
                        SectionTitle("Data Table", Icons.Outlined.DataArray)
                        Spacer(Modifier.height(16.dp))

                        // Header
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp)
                        ) {
                            columns.forEach { col ->
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    Text(col.name, fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(Modifier.width(48.dp))
                        }

                        Spacer(Modifier.height(12.dp))

                        if (rows.isEmpty()) {
                            EmptyTableMessage()
                        } else {
                            rows.forEachIndexed { ri, row ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    row.forEachIndexed { ci, cell ->
                                        OutlinedTextField(
                                            value = cell,
                                            onValueChange = {
                                                row[ci] = if (ci > 0) it.filter(Char::isDigit) else it
                                                showChart = false
                                            },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                    FilledIconButton(
                                        onClick = {
                                            rows.removeAt(ri)
                                            showChart = false
                                        },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer
                                        )
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3) Chart Configuration
            item {
                GradientCard {
                    Column(Modifier.padding(20.dp)) {
                        SectionTitle("Chart Configuration", Icons.Default.BarChart)
                        Spacer(Modifier.height(16.dp))

                        // Chart Type Selection
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Chart Type:", fontWeight = FontWeight.Medium)
                            Spacer(Modifier.width(16.dp))
                            ChartType.values().forEach { ct ->
                                Row(
                                    Modifier.padding(end = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedChart == ct,
                                        onClick = { selectedChart = ct; showChart = false }
                                    )
                                    Text(ct.name, Modifier.padding(start = 4.dp))
                                }
                            }
                        }

                        if (selectedChart == ChartType.Pie) {
                            Spacer(Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Data Column:", fontWeight = FontWeight.Medium)
                                Spacer(Modifier.width(16.dp))
                                Box {
                                    FilledTonalButton(onClick = { dropDownOpen = true }) {
                                        Text(columns[selectedColumn].name)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                    DropdownMenu(
                                        expanded = dropDownOpen,
                                        onDismissRequest = { dropDownOpen = false }
                                    ) {
                                        columns.drop(1).forEachIndexed { idx, col ->
                                            DropdownMenuItem(
                                                text = { Text(col.name) },
                                                onClick = {
                                                    selectedColumn = idx + 1
                                                    dropDownOpen = false
                                                    showChart = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { showChart = true },
                            enabled = allFilled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.AutoGraph, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Generate Chart", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 4) Chart Preview
            item {
                AnimatedVisibility(
                    visible = showChart,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    GradientCard {
                        Column(Modifier.padding(20.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionTitle("Chart Preview", Icons.Outlined.Preview)
                                FilledTonalIconButton(onClick = { expandedChart = !expandedChart }) {
                                    Icon(
                                        if (expandedChart) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = if (expandedChart) "Collapse" else "Expand"
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Card(
                                Modifier
                                    .fillMaxWidth()
                                    .height(if (expandedChart) 450.dp else 300.dp),
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.cardElevation(8.dp)
                            ) {
                                AndroidView(
                                    modifier = Modifier.fillMaxSize(),
                                    factory = { ctx ->
                                        when (selectedChart) {
                                            ChartType.Pie -> PieChart(ctx).apply {
                                                setUsePercentValues(true)
                                                isDrawHoleEnabled = true
                                                holeRadius = 40f
                                                setHoleColor(AndroidColor.WHITE)
                                                description.isEnabled = false
                                                legend.apply {
                                                    verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                                                    horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                                                    orientation = Legend.LegendOrientation.HORIZONTAL
                                                    textSize = if (expandedChart) 14f else 12f
                                                }
                                                animateY(800, Easing.EaseInOutQuad)
                                            }
                                            ChartType.Bar -> BarChart(ctx).apply {
                                                description.isEnabled = false
                                                axisRight.isEnabled = false
                                                xAxis.position = XAxis.XAxisPosition.BOTTOM
                                                animateY(800, Easing.EaseInOutQuad)
                                            }
                                        }
                                    },
                                    update = { chart ->
                                        try {
                                            if (chart is PieChart && selectedChart == ChartType.Pie) {
                                                val entries = rows.map {
                                                    PieEntry(it[selectedColumn].toFloatOrNull() ?: 0f, it[0])
                                                }
                                                val ds = PieDataSet(entries, "").apply {
                                                    sliceSpace = if (expandedChart) 3f else 2f
                                                    selectionShift = if (expandedChart) 8f else 5f
                                                    colors = listOf(
                                                        AndroidColor.rgb(33, 150, 243),
                                                        AndroidColor.rgb(76, 175, 80),
                                                        AndroidColor.rgb(255, 152, 0)
                                                    )
                                                }
                                                chart.data = PieData(ds).apply {
                                                    setValueFormatter(PercentFormatter(chart))
                                                    setValueTextSize(if (expandedChart) 16f else 14f)
                                                    setValueTextColor(AndroidColor.WHITE)
                                                }
                                                chart.invalidate()
                                            } else if (chart is BarChart && selectedChart == ChartType.Bar) {
                                                val series = columns.drop(1).mapIndexed { idx, col ->
                                                    rows.mapIndexed { i, row ->
                                                        BarEntry(i.toFloat(), row[idx + 1].toFloatOrNull() ?: 0f)
                                                    } to col.name
                                                }
                                                val dataSets = series.mapIndexed { idx, (entries, label) ->
                                                    BarDataSet(entries, label).apply {
                                                        color = when (idx) {
                                                            0 -> AndroidColor.rgb(33, 150, 243)
                                                            1 -> AndroidColor.rgb(76, 175, 80)
                                                            else -> AndroidColor.rgb(255, 152, 0)
                                                        }
                                                    }
                                                }
                                                val data = BarData(dataSets as List<IBarDataSet>).apply {
                                                    barWidth = 0.2f
                                                }
                                                chart.data = data
                                                val groupSpace = 0.1f
                                                val barSpace = (1f - groupSpace) / dataSets.size / dataSets.size * 0.5f
                                                chart.xAxis.apply {
                                                    axisMinimum = 0f
                                                    axisMaximum = rows.size.toFloat()
                                                    granularity = 1f
                                                    valueFormatter = IndexAxisValueFormatter(rows.map { it[0] })
                                                }
                                                chart.groupBars(0f, groupSpace, barSpace)
                                                chart.invalidate()
                                            }
                                        } catch (_: Throwable) {}
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GradientCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            content()
        }
    }
}

@Composable
fun SectionTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun EmptyTableMessage() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.DataArray,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "No data rows. Tap + to add.",
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}