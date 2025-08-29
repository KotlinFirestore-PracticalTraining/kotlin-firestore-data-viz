package com.example.kotlin_firestore_data_viz.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder // <<< FIX: ADD THIS IMPORT
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kotlin_firestore_data_viz.controller.FilterControls
import com.example.kotlin_firestore_data_viz.utils.*
import java.text.DecimalFormat

@Composable
fun ImageEditorScreen() {
    val context = LocalContext.current
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var editedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // State for RGB sliders
    var redValue by remember { mutableStateOf(1f) }
    var greenValue by remember { mutableStateOf(1f) }
    var blueValue by remember { mutableStateOf(1f) }

    // This effect applies the RGB filter whenever slider values change.
    LaunchedEffect(redValue, greenValue, blueValue, originalBitmap) {
        if (originalBitmap != null) {
            editedBitmap = applyRgbFilter(originalBitmap!!, redValue, greenValue, blueValue)
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bmp = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }.copy(Bitmap.Config.ARGB_8888, true)
            originalBitmap = bmp
            editedBitmap = bmp.copy(Bitmap.Config.ARGB_8888, true)
            // Reset sliders when a new image is loaded
            redValue = 1f
            greenValue = 1f
            blueValue = 1f
        }
    }

    val saveAsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri: Uri? ->
        uri?.let { outputUri ->
            editedBitmap?.let { bitmap ->
                context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                    if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                        Toast.makeText(context, "Image saved successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("SaveAs", "Failed to save bitmap")
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Image Editor") },
                actions = {
                    IconButton(onClick = {
                        originalBitmap = null
                        editedBitmap = null
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close Image")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (originalBitmap == null) {
                // Show a placeholder screen if no image is selected
                SelectImagePlaceholder(onSelectClick = { launcher.launch("image/*") })
            } else {
                // Show the main editor UI
                EditorContent(
                    editedBitmap = editedBitmap,
                    redValue = redValue,
                    onRedChange = { redValue = it },
                    greenValue = greenValue,
                    onGreenChange = { greenValue = it },
                    blueValue = blueValue,
                    onBlueChange = { blueValue = it },
                    onCropClick = { editedBitmap?.let { editedBitmap = cropBitmap(it) } },
                    onRotateClick = { editedBitmap?.let { editedBitmap = rotateBitmap(it, 90f) } },
                    onPassportClick = { editedBitmap?.let { editedBitmap = resizeToPassportSize(it) } },
                    onSaveClick = { editedBitmap?.let { saveBitmapToGallery(context, it) } },
                    onSaveAsClick = { saveAsLauncher.launch("EditedImage_${System.currentTimeMillis()}.png") },
                    onResetClick = {
                        editedBitmap = originalBitmap?.copy(Bitmap.Config.ARGB_8888, true)
                        redValue = 1f
                        greenValue = 1f
                        blueValue = 1f
                    },
                    originalBitmap = originalBitmap,
                    onFilterApplied = { newBmp ->
                        editedBitmap = newBmp
                        redValue = 1f
                        greenValue = 1f
                        blueValue = 1f
                    }
                )
            }
        }
    }
}

@Composable
fun SelectImagePlaceholder(onSelectClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AddPhotoAlternate,
                contentDescription = "Select Image Icon",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colors.primary
            )
            Text(
                "Select an image to start editing",
                style = MaterialTheme.typography.h6
            )
            Button(onClick = onSelectClick) {
                Text("Select Image")
            }
        }
    }
}

@Composable
fun EditorContent(
    editedBitmap: Bitmap?,
    redValue: Float, onRedChange: (Float) -> Unit,
    greenValue: Float, onGreenChange: (Float) -> Unit,
    blueValue: Float, onBlueChange: (Float) -> Unit,
    onCropClick: () -> Unit,
    onRotateClick: () -> Unit,
    onPassportClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSaveAsClick: () -> Unit,
    onResetClick: () -> Unit,
    originalBitmap: Bitmap?,
    onFilterApplied: (Bitmap) -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Transform", "Adjust", "Filters")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Image Preview
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(16.dp),
            elevation = 4.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            editedBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Edited Image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                )
            }
        }

        // Global Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onSaveClick) { Icon(Icons.Default.Save, contentDescription = "Save"); Spacer(Modifier.width(4.dp)); Text("Save") }
            OutlinedButton(onClick = onSaveAsClick) { Text("Save As") }
            OutlinedButton(onClick = onResetClick) { Icon(Icons.Default.RestartAlt, contentDescription = "Reset"); Spacer(Modifier.width(4.dp)); Text("Reset") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Layout for Editing Tools
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        // Content for the selected tab
        Box(modifier = Modifier.padding(16.dp)) {
            AnimatedContent(targetState = selectedTabIndex) { tabIndex ->
                when (tabIndex) {
                    0 -> TransformTab(onCropClick, onRotateClick, onPassportClick)
                    1 -> AdjustTab(redValue, onRedChange, greenValue, onGreenChange, blueValue, onBlueChange)
                    2 -> FilterTab(originalBitmap, onFilterApplied)
                }
            }
        }
    }
}

@Composable
fun TransformTab(onCropClick: () -> Unit, onRotateClick: () -> Unit, onPassportClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        EditorToolButton(icon = Icons.Default.Crop, text = "Crop", onClick = onCropClick)
        EditorToolButton(icon = Icons.Default.Rotate90DegreesCcw, text = "Rotate", onClick = onRotateClick)
        EditorToolButton(icon = Icons.Default.ContactMail, text = "Passport", onClick = onPassportClick)
    }
}

@Composable
fun AdjustTab(
    redValue: Float, onRedChange: (Float) -> Unit,
    greenValue: Float, onGreenChange: (Float) -> Unit,
    blueValue: Float, onBlueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("RGB Color Adjustment", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
        ColorSlider(
            label = "Red",
            value = redValue,
            onValueChange = onRedChange,
            color = Color.Red
        )
        ColorSlider(
            label = "Green",
            value = greenValue,
            onValueChange = onGreenChange,
            color = Color.Green
        )
        ColorSlider(
            label = "Blue",
            value = blueValue,
            onValueChange = onBlueChange,
            color = Color.Blue
        )
        // You can add other sliders (Brightness, Contrast etc.) from FilterControls here
    }
}

@Composable
fun FilterTab(editedBitmap: Bitmap?, onBitmapChanged: (Bitmap) -> Unit) {
    if (editedBitmap != null) {
        // Pass the current editedBitmap to FilterControls
        FilterControls(source = editedBitmap, onBitmapChanged = onBitmapChanged)
    } else {
        Text("Load an image to see filters.")
    }
}


@Composable
fun ColorSlider(label: String, value: Float, onValueChange: (Float) -> Unit, color: Color) {
    val decimalFormat = remember { DecimalFormat("0.00") }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = color, fontWeight = FontWeight.Bold)
            Text("${decimalFormat.format(value)}x", style = MaterialTheme.typography.caption)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..2f,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color
            )
        )
    }
}

@Composable
fun EditorToolButton(icon: ImageVector, text: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colors.primary.copy(alpha = 0.1f), shape = CircleShape)
        ) {
            Icon(icon, contentDescription = text, tint = MaterialTheme.colors.primary, modifier = Modifier.size(32.dp))
        }
        Text(text, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}