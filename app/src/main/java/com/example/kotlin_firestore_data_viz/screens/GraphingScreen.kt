package com.example.kotlin_firestore_data_viz.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import net.objecthunter.exp4j.Expression
import net.objecthunter.exp4j.ExpressionBuilder
import net.objecthunter.exp4j.function.Function
import kotlin.math.*

enum class FigureType { Parabola, Line, Function }

private data class ParsedFunc(
    val raw: String,                          // e.g. "y=x^2"
    val label: String,                        // e.g. "x^2"
    val pts: List<Pair<Float, Float>>,        // sampled points
    val type: FigureType,                     // detected type
    val color: Color                          // display color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphingScreen(modifier: Modifier = Modifier) {
    var expr by remember { mutableStateOf("y = x^2, y = 2*x + 1, y = sin(x)") }
    var plotted by remember { mutableStateOf<List<ParsedFunc>>(emptyList()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Analytics,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Function Plotter",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(12.dp))

        // Enhanced quick suggestions
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("x^2", "2*x+1", "sin(x)", "cos(x)", "tan(x)", "|x|", "sqrt(|x|)", "ln(x)")
                .forEach { suggestion ->
                    FilterChip(
                        selected = false,
                        onClick = { expr = "y = $suggestion" },
                        label = {
                            Text(
                                suggestion,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Functions,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        )
                    )
                }
        }

        Spacer(Modifier.height(12.dp))

        // Enhanced input field
        OutlinedTextField(
            value = expr,
            onValueChange = { expr = it },
            label = { Text("Enter functions (comma-separated)") },
            placeholder = { Text("e.g., y=x^2, y=2*x+1, y=sin(x)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { /* tap Plot */ }),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(Modifier.height(12.dp))

        // Enhanced button
        FilledTonalButton(
            onClick = {
                runCatching {
                    plotted = parseAndSample(expr, -10f, 10f, 600)
                    errorMsg = null
                }.onFailure {
                    errorMsg = "Parse error: ${it.message}"
                    plotted = emptyList()
                    Log.e("GraphingScreen", "Invalid expr", it)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Plot Functions",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(Modifier.height(8.dp))

        // Enhanced error message
        errorMsg?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    "⚠️ $it",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Original graph size
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
        ) {
            GraphCanvas(plotted)
        }

        // Enhanced legend
        if (plotted.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Functions,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Detected Functions:",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    plotted.forEach { func ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(func.color)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    func.raw,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Type: ${func.type}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Enhanced help text
        Text(
            "📚 Supported: +, -, ×, /, ^, sin, cos, tan, ln, log, sqrt, abs, |x|, π, e",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/* ------------------------ parsing + detection ------------------------ */

private fun parseAndSample(
    expr: String,
    xMin: Float,
    xMax: Float,
    steps: Int
): List<ParsedFunc> {

    val colors = listOf(
        Color(0xFF2962FF), Color(0xFFD81B60), Color(0xFF2E7D32),
        Color(0xFF6A1B9A), Color(0xFF00838F), Color(0xFFF57C00)
    )

    val parts = expr.split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }

    val dx = (xMax - xMin) / steps
    val out = mutableListOf<ParsedFunc>()

    parts.forEachIndexed { idx, raw ->
        val cleanForLabel = raw.replace(" ", "").removePrefix("y=")

        // Build an exp4j expression with aliases/normalization (|x|, ln, pi, e)
        val e = buildExpression(raw)

        // sample
        val pts = buildList(steps + 1) {
            var i = 0
            while (i <= steps) {
                val x = xMin + i * dx
                val y = runCatching {
                    e.setVariable("x", x.toDouble())
                    e.evaluate().toFloat()
                }.getOrNull()
                if (y != null && y.isFinite()) add(x to y)
                i++
            }
        }

        val type = detectType(cleanForLabel)
        out += ParsedFunc(
            raw = raw,
            label = cleanForLabel,
            pts = pts,
            type = type,
            color = colors[idx % colors.size]
        )
    }
    return out
}

/** Add friendly syntax and functions for the user: |x|, ln(x), pi/π, e. */
private fun buildExpression(userExpr: String): Expression {
    val normalized = normalizeExpression(userExpr)      // handle | |, pi, e, strip y=
    return ExpressionBuilder(normalized)
        .variables("x")
        .functions(lnFunction)                          // natural log alias
        .build()
}

/** Convert user-friendly tokens to exp4j-compatible: */
private fun normalizeExpression(raw: String): String {
    var s = raw.trim()
        .replace(" ", "")
        .removePrefix("y=")
        .replace("π", Math.PI.toString(), ignoreCase = true)
        .replace("pi", Math.PI.toString(), ignoreCase = true)
        // exp4j already knows 'e' constant; leave it, but avoid replacing inside identifiers
        // turn ln(...) into our ln(...) function (we register lnFunction below)
        .replace("Ln(", "ln(", ignoreCase = true)

    s = pipesToAbs(s) // convert |...| to abs(...)
    return s
}

/** Turn absolute-value pipes into abs(...). Supports nesting: |x+|y|| -> abs(x+abs(y)) */
private fun pipesToAbs(input: String): String {
    val out = StringBuilder()
    var open = 0
    for (ch in input) {
        if (ch == '|') {
            if (open == 0) { out.append("abs("); open = 1 }
            else { out.append(")"); open = 0 }
        } else out.append(ch)
    }
    if (open == 1) out.append(")") // close unmatched
    return out.toString()
}

/** Natural log alias for exp4j (exp4j's built-in `log(x)` is natural log). */
private val lnFunction = object : Function("ln", 1) {
    override fun apply(vararg args: Double): Double = kotlin.math.ln(args[0])
}

private fun detectType(cleanExpr: String): FigureType {
    val s = cleanExpr.replace(" ", "")
    // ax^2 + bx + c   (a, b, c optional; allow decimals, leading sign)
    val quad = Regex("""^[+-]?(\d+(\.\d+)?)?x\^2([+-]\d+(\.\d+)?x)?([+-]\d+(\.\d+)?)?$""")
    // ax + b
    val line = Regex("""^[+-]?(\d+(\.\d+)?)?x([+-]\d+(\.\d+)?)?$""")
    return when {
        quad.matches(s) -> FigureType.Parabola
        line.matches(s) -> FigureType.Line
        else -> FigureType.Function
    }
}

/* ----------------------------- drawing ------------------------------ */

@Composable
private fun GraphCanvas(series: List<ParsedFunc>) {
    Canvas(Modifier.fillMaxSize()) {
        val pad = 40.dp.toPx()
        val w = size.width - 2 * pad
        val h = size.height - 2 * pad

        // Fixed uniform range: -10 to +10 for both axes
        val xMin = -10f
        val xMax = 10f
        val yMin = -10f
        val yMax = 10f

        // Screen coordinate conversion
        fun sx(x: Float) = pad + (x - xMin) / (xMax - xMin) * w
        fun sy(y: Float) = pad + h - (y - yMin) / (yMax - yMin) * h

        // Fixed tick spacing of 2 units
        val tickSpacing = 2f

        val txt = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 12.sp.toPx()
            color = android.graphics.Color.DKGRAY
        }
        val txtLeft = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.RIGHT
            textSize = 12.sp.toPx()
            color = android.graphics.Color.DKGRAY
        }

        // Draw vertical grid lines and x-axis labels
        var x = xMin
        while (x <= xMax) {
            val screenX = sx(x)
            // Light grid lines
            drawLine(
                Color(0xFFE0E0E0),
                Offset(screenX, pad),
                Offset(screenX, size.height - pad),
                1f
            )
            // X-axis labels (skip 0, we'll handle it specially)
            if (x != 0f) {
                drawContext.canvas.nativeCanvas.drawText(
                    x.toInt().toString(),
                    screenX,
                    size.height - pad/2,
                    txt
                )
            }
            x += tickSpacing
        }

        // Draw horizontal grid lines and y-axis labels
        var y = yMin
        while (y <= yMax) {
            val screenY = sy(y)
            // Light grid lines
            drawLine(
                Color(0xFFE0E0E0),
                Offset(pad, screenY),
                Offset(size.width - pad, screenY),
                1f
            )
            // Y-axis labels (skip 0, we'll handle it specially)
            if (y != 0f) {
                drawContext.canvas.nativeCanvas.drawText(
                    y.toInt().toString(),
                    pad - 6.dp.toPx(),
                    screenY + 4f,
                    txtLeft
                )
            }
            y += tickSpacing
        }

        // Draw MAIN AXES (thick, centered lines)
        // X-axis (y = 0)
        drawLine(
            Color.Black,
            Offset(pad, sy(0f)),
            Offset(size.width - pad, sy(0f)),
            strokeWidth = 3f
        )

        // Y-axis (x = 0)
        drawLine(
            Color.Black,
            Offset(sx(0f), pad),
            Offset(sx(0f), size.height - pad),
            strokeWidth = 3f
        )

        // Draw origin label (0,0)
        drawContext.canvas.nativeCanvas.drawText(
            "0",
            sx(0f) - 12.dp.toPx(),
            sy(0f) + 16.dp.toPx(),
            txt
        )

        // Plot each function series
        series.forEach { s ->
            val pts = s.pts.filter { (x, y) ->
                // Only draw points within our visible range
                x >= xMin && x <= xMax && y >= yMin && y <= yMax
            }

            if (pts.isNotEmpty()) {
                val path = Path().apply {
                    moveTo(sx(pts[0].first), sy(pts[0].second))
                    for (i in 1 until pts.size) {
                        val (x, y) = pts[i]
                        lineTo(sx(x), sy(y))
                    }
                }
                drawPath(path, s.color, style = Stroke(width = 3.dp.toPx()))
            }
        }
    }
}

/* ----------------------------- utilities --------------------------- */

private fun formatTick(v: Float): String {
    val i = v.roundToInt()
    return if (abs(v - i) < 1e-3) i.toString() else String.format("%.1f", v)
}

/** 1–2–5 tick rule (1, 2, 5 × 10^k) */
private fun niceTick(raw: Float): Float {
    val r = raw.coerceAtLeast(1e-6f)
    val exp = floor(log10(r.toDouble()))
    val pow10 = 10.0.pow(exp).toFloat()
    val scaled = r / pow10
    val base = when {
        scaled <= 1.2f -> 1f
        scaled <= 2.5f -> 2f
        scaled <= 5.0f -> 5f
        else -> 10f
    }
    return base * pow10
}