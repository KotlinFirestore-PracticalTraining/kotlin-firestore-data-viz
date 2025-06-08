package com.example.kotlin_firestore_data_viz.controller

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.kotlin_firestore_data_viz.utils.applyImageFilters


@Composable
fun FilterControls(
    source: Bitmap,
    onBitmapChanged: (Bitmap) -> Unit
) {
    var saturation by remember { mutableStateOf(1f) }
    var brightness by remember { mutableStateOf(1f) }
    var contrast by remember { mutableStateOf(1f) }
    var grayscale by remember { mutableStateOf(false) } // Add grayscale toggle

    // Apply filters when sliders or grayscale toggle change
    LaunchedEffect(saturation, brightness, contrast, grayscale) {
        val filteredBitmap = applyImageFilters(
            source = source,
            saturation = if (grayscale) 0f else saturation, // Set saturation to 0 if grayscale
            brightness = brightness,
            contrast = contrast,
            grayscale = grayscale
        )
        onBitmapChanged(filteredBitmap)
    }

    Column {
        Text("Saturation")
        Slider(
            value = saturation,
            onValueChange = { saturation = it },
            valueRange = 0f..2f,
            enabled = !grayscale // Disable slider if grayscale is enabled
        )

        Text("Brightness")
        Slider(
            value = brightness,
            onValueChange = { brightness = it },
            valueRange = 0.5f..2f
        )

        Text("Contrast")
        Slider(
            value = contrast,
            onValueChange = { contrast = it },
            valueRange = 0.5f..2f
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Grayscale")
            Switch(
                checked = grayscale,
                onCheckedChange = { grayscale = it }
            )
        }
    }
}