package com.example.kotlin_firestore_data_viz.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.toColorInt
import androidx.core.graphics.get

// quantization settings
private const val MAX_DIMENSION    = 300
private const val BITS_PER_CHANNEL = 4
private const val THRESHOLD_PERCENT = 1f

// UI model
private data class Swatch(val colorInt: Int, val percentTimes10: Int)

// DataClasses for saving/loading analyses in FIrestore:
private data class Analysis(
    val imageUri: String = "",
    val timestamp: Long = 0L,
    val swatches: List<SwatchDTO> = emptyList()
)
private data class SwatchDTO(val colorHex: String = "", val percent: Float = 0f)


// Main Composable screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorAnalysisScreen() {
    // Android / Compose contexts & helpers
    val context    = LocalContext.current
    val scope      = rememberCoroutineScope()
    val snackbar   = remember { SnackbarHostState() }
    val db         = remember { FirebaseFirestore.getInstance() }
    val scroll     = rememberScrollState()

    // UI state variables
    var pickedUri   by remember { mutableStateOf<Uri?>(null) }
    var swatches    by remember { mutableStateOf<List<Swatch>>(emptyList()) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var history     by remember { mutableStateOf<List<Analysis>>(emptyList()) }
    var detailRec   by remember { mutableStateOf<Analysis?>(null) }

    // Launcher for the system image-picker
    val launcher = rememberLauncherForActivityResult(GetContent()) { uri ->
        uri?.let {
            pickedUri = it
            swatches = emptyList()
            isAnalyzing = true
            detailRec = null
        }
    }

    // 2) fetch history when sheet opens
    LaunchedEffect(showHistory) {
        if (showHistory) {
            db.collection("analyses")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { snap ->
                    history = snap.documents.mapNotNull {
                        it.toObject(Analysis::class.java)
                    }
                }
                .addOnFailureListener { e ->
                    scope.launch { snackbar.showSnackbar("Load history failed: ${e.message}") }
                }
        }
    }

    // 3) scaffold + top‐bar
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title   = { Text("Color Analyzer") },
                actions = {
                    IconButton(onClick = { showHistory = true }) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // detail view if selected
            detailRec?.let { rec ->
                HistoryItemDetail(rec) { detailRec = null }
            }

            //  main UI
            AnimatedVisibility(
                visible = detailRec == null,
                enter   = fadeIn(tween(300)),
                exit    = fadeOut(tween(300))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ElevatedButton(
                        onClick = { launcher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = "Pick")
                        Spacer(Modifier.width(8.dp))
                        Text("Choose Photo", fontSize = 16.sp)
                    }

                    Spacer(Modifier.height(24.dp))

                    pickedUri?.let { uri ->
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f/3f),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(8.dp)
                        ) {
                            Box {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isAnalyzing) {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = .6f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = Color.White)
                                            Spacer(Modifier.height(12.dp))
                                            Text("Analyzing…", color = Color.White)
                                        }
                                    }
                                }
                            }
                        }

                        // kick off analysis once
                        LaunchedEffect(uri) {
                            val bmp = withContext(Dispatchers.IO) { loadDownsampledBitmap(context, uri) }
                            bmp?.let { bitmap ->
                                val freq  = computeQuantizedHistogram(bitmap)
                                val total = freq.values.sum().toFloat()

                                // convert histogram into our Swatch list
                                swatches = freq.entries.map { (k, v) ->
                                    val c    = dequantizeBucket(k)
                                    val pct10 = ((v * 1000f) / total).roundToInt()
                                    Swatch(c, pct10)
                                }.sortedByDescending { it.percentTimes10 }
                                    .filter { it.percentTimes10/10f >= THRESHOLD_PERCENT }
                            }
                            isAnalyzing = false
                        }

                        Spacer(Modifier.height(24.dp))

                        // Once analyzed, show results + “Save” button
                        if (!isAnalyzing && swatches.isNotEmpty()) {
                            Text("Dominant Colors", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(swatches) { sw -> ColorSwatchItem(sw) }
                            }
                            Spacer(Modifier.height(16.dp))
                            ColorPercentageBar(swatches)
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    pickedUri?.toString()?.let { uriString ->
                                        scope.launch {
                                            // First check for an existing analysis with the same image URI
                                            db.collection("analyses")
                                                .whereEqualTo("imageUri", uriString)
                                                .get()
                                                .addOnSuccessListener { snap ->
                                                    if (snap.isEmpty) {
                                                        //  none found → save new
                                                        val rec = Analysis(
                                                            imageUri = uriString,
                                                            timestamp = System.currentTimeMillis(),
                                                            swatches = swatches.map {
                                                                SwatchDTO("#%06X".format(0xFFFFFF and it.colorInt), it.percentTimes10 / 10f)
                                                            }
                                                        )
                                                        db.collection("analyses")
                                                            .add(rec)
                                                            .addOnSuccessListener {
                                                                scope.launch { snackbar.showSnackbar("Analysis saved!") }
                                                            }
                                                            .addOnFailureListener { e ->
                                                                scope.launch { snackbar.showSnackbar("Save failed: ${e.message}") }
                                                            }

                                                    } else {
                                                        // duplicate found → warn user
                                                        scope.launch {
                                                            snackbar.showSnackbar("You’ve already saved this photo’s analysis.")
                                                        }
                                                    }
                                                }
                                                .addOnFailureListener { e ->
                                                    scope.launch {
                                                        snackbar.showSnackbar("History check failed: ${e.message}")
                                                    }
                                                }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save Analysis", fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // 4) history sheet
    if (showHistory) {
        ModalBottomSheet(
            onDismissRequest = { showHistory = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Analysis History", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showHistory = false }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
                HorizontalDivider()
                if (history.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No saved analyses", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(history) { rec ->
                            HistoryRow(rec) {
                                detailRec = rec
                                showHistory = false
                            }
                        }
                    }
                }
            }
        }
    }
}

//   Reusable small Composable for swatches, bars, history rows

@Composable
private fun ColorSwatchItem(swatch: Swatch) {
    val color     = Color(swatch.colorInt)
    val textColor = if (color.luminance() > .5f) Color.Black else Color.White
    val hex       = "#%06X".format(0xFFFFFF and swatch.colorInt)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(elevation = CardDefaults.cardElevation(4.dp), shape = RoundedCornerShape(8.dp)) {
            Box(
                Modifier.size(70.dp).background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(hex, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("${swatch.percentTimes10/10f}%", fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ColorPercentageBar(swatches: List<Swatch>) {
    val total = swatches.sumOf { it.percentTimes10 }.toFloat()
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(4.dp))
        Row(
            Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(12.dp))
        ) {
            swatches.forEach { sw ->
                Box(
                    Modifier
                        .fillMaxHeight()
                        .weight(sw.percentTimes10/total)
                        .background(Color(sw.colorInt))
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(analysis: Analysis, onClick: ()->Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            AsyncImage(
                model = analysis.imageUri,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                analysis.swatches.take(6).forEach { sw ->
                    Box(
                        Modifier.size(24.dp).clip(CircleShape)
                            .background(Color(sw.colorHex.toColorInt()))
                    )
                }
                if (analysis.swatches.size>6) {
                    Box(
                        Modifier.size(24.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha=.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+${analysis.swatches.size-6}", fontSize=10.sp, fontWeight=FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryItemDetail(analysis: Analysis, onClose: ()->Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(bottom=12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment   = Alignment.CenterVertically
        ) {
            Text("Analysis Details", fontSize=20.sp, fontWeight=FontWeight.Bold)
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = null) }
        }
        AsyncImage(
            model = analysis.imageUri,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().aspectRatio(4f/3f),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(12.dp))
        Text(
            SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
                .format(Date(analysis.timestamp)),
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(16.dp))
        Text("Color Analysis", fontSize=18.sp, fontWeight=FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        val uiSw = analysis.swatches.map {
            val cInt = it.colorHex.toColorInt()
            Swatch(cInt, (it.percent*10).roundToInt())
        }
        LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            items(uiSw) { sw -> ColorSwatchItem(sw) }
        }
        Spacer(Modifier.height(12.dp))
        ColorPercentageBar(uiSw)
    }
}

// Helper functions for pixel-level histogram & quantize
private fun loadDownsampledBitmap(ctx: Context, uri: Uri): Bitmap? {
    val b = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    ctx.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, b)
    }
    val max = maxOf(b.outWidth, b.outHeight)
    val sample = (max/MAX_DIMENSION.toFloat()).coerceAtLeast(1f).toInt()
    val opts = BitmapFactory.Options().apply {
        inJustDecodeBounds = false
        inSampleSize = sample
    }
    return ctx.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, opts)
    }
}
// Builds a histogram of quantized color buckets

private fun computeQuantizedHistogram(bitmap: Bitmap): Map<Int,Int> {
    val freq = mutableMapOf<Int,Int>()
    val shift = 8 - BITS_PER_CHANNEL
    for (y in 0 until bitmap.height)
        for (x in 0 until bitmap.width) {
            val px = bitmap[x, y] and 0x00FFFFFF
            val r = (px shr 16 and 0xFF) shr shift
            val g = (px shr 8  and 0xFF) shr shift
            val b = (px       and 0xFF) shr shift
            val key = (r shl (2*BITS_PER_CHANNEL)) or (g shl BITS_PER_CHANNEL) or b
            freq[key] = freq.getOrDefault(key,0) + 1
        }
    return freq
}
// Converts a bucket key back into a full 0xAARRGGBB color

private fun dequantizeBucket(bucket: Int): Int {
    val mask = (1 shl BITS_PER_CHANNEL) - 1
    val rQ   = (bucket shr (2*BITS_PER_CHANNEL)) and mask
    val gQ   = (bucket shr BITS_PER_CHANNEL)    and mask
    val bQ   = bucket                           and mask
    val r = rQ * 255 / mask
    val g = gQ * 255 / mask
    val b = bQ * 255 / mask
    return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
}