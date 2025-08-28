package com.example.kotlin_firestore_data_viz.screens

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.objecthunter.exp4j.ExpressionBuilder
import kotlin.math.*

enum class FigureType {
    PARABOLA, LINE, FUNCTION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphingScreen(
    modifier: Modifier = Modifier
) {
    var expr by remember { mutableStateOf("y = x^2") }
    var points by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    var figureType by remember { mutableStateOf(FigureType.FUNCTION) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1) Title and Input
        Text(
            text = "Graphing Calculator",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(16.dp))

        // 2) Expression input
        OutlinedTextField(
            value = expr,
            onValueChange = { expr = it },
            label = { Text("Enter equation (e.g., y = x^2)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                runCatching {
                    val (pts, type) = parseExpression(expr, -10f, 10f, 500)
                    points = pts
                    figureType = type
                    errorMsg = null
                }.onFailure {
                    errorMsg = "Parse error: ${it.message}"
                    Log.e("GraphingScreen", "Invalid expr", it)
                }
            }),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // 3) Draw button
        Button(onClick = {
            runCatching {
                val (pts, type) = parseExpression(expr, -10f, 10f, 500)
                points = pts
                figureType = type
                errorMsg = null
            }.onFailure {
                errorMsg = "Parse error: ${it.message}"
                Log.e("GraphingScreen", "Invalid expr", it)
            }
        }) {
            Text("Draw")
        }

        // 4) Show figure type and parse error
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Figure: ${figureType.name.lowercase().replaceFirstChar { it.uppercase() }}",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            errorMsg?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(Modifier.height(16.dp))

        // 5) Graph canvas
        GraphCanvas(
            points,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFFF5F5F5))
        )
    }
}

/** Parses expression and detects figure type. */
private fun parseExpression(
    expr: String,
    xMin: Float,
    xMax: Float,
    steps: Int
): Pair<List<Pair<Float, Float>>, FigureType> {
    val cleanedExpr = expr.replace(" ", "").replace("y=", "")
    val e = ExpressionBuilder(cleanedExpr)
        .variable("x")
        .build()

    val pts = mutableListOf<Pair<Float, Float>>()
    val dx = (xMax - xMin) / steps

    for (i in 0..steps) {
        val x = xMin + i * dx
        val y = runCatching {
            e.setVariable("x", x.toDouble())
            e.evaluate().toFloat()
        }.getOrNull() ?: Float.NaN

        if (y.isFinite()) pts += x to y
    }

    // Detect figure type
    val s = cleanedExpr.lowercase()
    val type = when {
        Regex("""^[+-]?[\d.]*x\^2([+-][\d.]+x)?([+-]\d+)?$""").matches(s) -> FigureType.PARABOLA
        Regex("""^[+-]?[\d.]*x([+-]\d+)?$""").matches(s) -> FigureType.LINE
        else -> FigureType.FUNCTION
    }
    return pts to type
}

@Composable
private fun GraphCanvas(
    points: List<Pair<Float, Float>>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    Canvas(modifier) {
        val pad = 16.dp.toPx()
        val w = size.width - 2 * pad
        val h = size.height - 2 * pad

        // Data bounds
        val xVals = points.map { it.first }
        val yVals = points.map { it.second }
        val xMin = xVals.minOrNull() ?: -1f
        val xMax = xVals.maxOrNull() ?: 1f
        val yMin = yVals.minOrNull() ?: -1f
        val yMax = yVals.maxOrNull() ?: 1f

        // Adjust bounds with padding
        val xRange = xMax - xMin
        val yRange = yMax - yMin
        val xMinAdj = xMin - xRange * 0.1f
        val xMaxAdj = xMax + xRange * 0.1f
        val yMinAdj = yMin - yRange * 0.1f
        val yMaxAdj = yMax + yRange * 0.1f

        // Map functions
        fun toScreenX(x: Float) = pad + (x - xMinAdj) / (xMaxAdj - xMinAdj) * w
        fun toScreenY(y: Float) = pad + h - (y - yMinAdj) / (yMaxAdj - yMinAdj) * h

        // Draw grid
        val gridXStep = niceIncrement((xMaxAdj - xMinAdj) / 5)
        val gridYStep = niceIncrement((yMaxAdj - yMinAdj) / 5)
        val xGridStart = floor(xMinAdj / gridXStep) * gridXStep
        val yGridStart = floor(yMinAdj / gridYStep) * gridYStep

        for (i in 0..5) {
            val x = xGridStart + i * gridXStep
            if (x in xMinAdj..xMaxAdj) {
                drawLine(
                    Color.LightGray,
                    Offset(toScreenX(x), pad),
                    Offset(toScreenX(x), size.height - pad),
                    strokeWidth = 0.5.dp.toPx()
                )
            }
            val y = yGridStart + i * gridYStep
            if (y in yMinAdj..yMaxAdj) {
                drawLine(
                    Color.LightGray,
                    Offset(pad, toScreenY(y)),
                    Offset(size.width - pad, toScreenY(y)),
                    strokeWidth = 0.5.dp.toPx()
                )
            }
        }

        // Draw axes
        drawLine(
            Color.Gray,
            Offset(toScreenX(0f), pad),
            Offset(toScreenX(0f), size.height - pad),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            Color.Gray,
            Offset(pad, toScreenY(0f)),
            Offset(size.width - pad, toScreenY(0f)),
            strokeWidth = 1.dp.toPx()
        )

        // Improved tick marks & labels
        val xTicks = (w / 50.dp.toPx()).toInt().coerceIn(4, 10) // Dynamic based on canvas width
        val yTicks = (h / 50.dp.toPx()).toInt().coerceIn(4, 10) // Dynamic based on canvas height
        val xStep = niceIncrement((xMaxAdj - xMinAdj) / (xTicks - 1))
        val yStep = niceIncrement((yMaxAdj - yMinAdj) / (yTicks - 1))
        val xStart = floor(xMinAdj / xStep) * xStep
        val yStart = floor(yMinAdj / yStep) * yStep

        for (i in 0 until xTicks) {
            val xv = xStart + i * xStep
            if (xv in xMinAdj..xMaxAdj) {
                val sx = toScreenX(xv)
                val sy = toScreenY(0f)
                drawLine(Color.DarkGray, Offset(sx, sy - 6), Offset(sx, sy + 6), strokeWidth = 1.dp.toPx())
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        String.format("%.2f", xv), // Two decimal places for precision
                        sx, sy + 20,
                        android.graphics.Paint().apply {
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 12.sp.toPx()
                            color = android.graphics.Color.DKGRAY
                        }
                    )
                }
            }
        }

        for (i in 0 until yTicks) {
            val yv = yStart + i * yStep
            if (yv in yMinAdj..yMaxAdj) {
                val sx = toScreenX(0f)
                val sy = toScreenY(yv)
                drawLine(Color.DarkGray, Offset(sx - 6, sy), Offset(sx + 6, sy), strokeWidth = 1.dp.toPx())
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        String.format("%.2f", yv), // Two decimal places for precision
                        sx - 20, sy + 4,
                        android.graphics.Paint().apply {
                            textAlign = android.graphics.Paint.Align.RIGHT
                            textSize = 12.sp.toPx()
                            color = android.graphics.Color.DKGRAY
                        }
                    )
                }
            }
        }

        // Plot curve
        if (points.isNotEmpty()) {
            val path = Path().apply {
                val validPoints = points.filter { it.second.isFinite() }
                if (validPoints.isNotEmpty()) {
                    moveTo(toScreenX(validPoints[0].first), toScreenY(validPoints[0].second))
                    validPoints.drop(1).forEach { (x, y) ->
                        lineTo(toScreenX(x), toScreenY(y))
                    }
                }
            }
            drawPath(path, Color.Blue, style = Stroke(width = 2.dp.toPx()))
        }
    }
}

/** Pick a “nice” increment (1, 2, 5 × power-of-ten). */
private fun niceIncrement(raw: Float): Float {
    if (raw <= 0f) return 1f
    val exp = floor(log10(raw.toDouble())).toInt()
    val base = 10.0.pow(exp.toDouble()).toFloat()
    return when {
        raw / base <= 1.5 -> base
        raw / base <= 3.5 -> 2 * base
        raw / base <= 7.5 -> 5 * base
        else -> 10 * base
    }
}