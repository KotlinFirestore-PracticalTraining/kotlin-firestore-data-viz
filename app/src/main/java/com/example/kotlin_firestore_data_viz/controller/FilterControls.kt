package com.example.kotlin_firestore_data_viz.controller

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import com.example.kotlin_firestore_data_viz.utils.applyImageFilters


@Composable
fun FilterControls(
    source: Bitmap, // renamed to match ImageEditorScreen
    onBitmapChanged: (Bitmap) -> Unit
) {
    var saturation by remember { mutableStateOf(1f) }
    var brightness by remember { mutableStateOf(1f) }
    var contrast by remember { mutableStateOf(1f) }

    // Apply filters when sliders change
    LaunchedEffect(saturation, brightness, contrast) {
        val filteredBitmap = applyImageFilters(
            source = source,
            saturation = saturation,
            brightness = brightness,
            contrast = contrast
        )
        onBitmapChanged(filteredBitmap)
    }

    Column {
        Text("Saturation")
        Slider(
            value = saturation,
            onValueChange = { saturation = it },
            valueRange = 0f..2f
        )

        Text("Brightness")
        Slider(
            value = brightness,
            onValueChange = { brightness = it },
            valueRange = 0.5f..2f // avoids complete black at 0
        )

        Text("Contrast")
        Slider(
            value = contrast,
            onValueChange = { contrast = it },
            valueRange = 0.5f..2f
        )
    }
}
