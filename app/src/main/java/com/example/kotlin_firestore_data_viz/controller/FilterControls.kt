package com.example.kotlin_firestore_data_viz.controller

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * A stateless composable that displays UI controls for image adjustments.
 * It takes the current values and reports changes via lambdas.
 */
@Composable
fun AdjustmentControls(
    saturation: Float,
    onSaturationChange: (Float) -> Unit,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    contrast: Float,
    onContrastChange: (Float) -> Unit,
    grayscale: Boolean,
    onGrayscaleChange: (Boolean) -> Unit
) {
    Column {
        Text("Saturation")
        Slider(
            value = saturation,
            onValueChange = onSaturationChange,
            valueRange = 0f..2f,
            enabled = !grayscale // Disable slider if grayscale is enabled
        )

        Text("Brightness")
        Slider(
            value = brightness,
            onValueChange = onBrightnessChange,
            valueRange = 0.5f..2f
        )

        Text("Contrast")
        Slider(
            value = contrast,
            onValueChange = onContrastChange,
            valueRange = 0.5f..2f
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Grayscale")
            Switch(
                checked = grayscale,
                onCheckedChange = onGrayscaleChange
            )
        }
    }
}