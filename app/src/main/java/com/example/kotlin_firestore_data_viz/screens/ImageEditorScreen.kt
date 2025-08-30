package com.example.kotlin_firestore_data_viz.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
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
import com.example.kotlin_firestore_data_viz.controller.AdjustmentControls
import com.example.kotlin_firestore_data_viz.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat

@Composable
fun ImageEditorScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- STATE MANAGEMENT ---
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var transformedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var editedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // State for all non-destructive adjustments
    var redValue by remember { mutableStateOf(1f) }
    var greenValue by remember { mutableStateOf(1f) }
    var blueValue by remember { mutableStateOf(1f) }
    var saturation by remember { mutableStateOf(1f) }
    var brightness by remember { mutableStateOf(1f) }
    var contrast by remember { mutableStateOf(1f) }
    var grayscale by remember { mutableStateOf(false) }

    // --- CENTRAL RENDERING EFFECT ---
    LaunchedEffect(
        transformedBitmap, redValue, greenValue, blueValue,
        saturation, brightness, contrast, grayscale
    ) {
        transformedBitmap?.let { source ->
            scope.launch {
                val result = withContext(Dispatchers.Default) {
                    applyAllAdjustments(
                        source = source,
                        saturation = if (grayscale) 0f else saturation,
                        brightness = brightness,
                        contrast = contrast,
                        red = redValue,
                        green = greenValue,
                        blue = blueValue
                    )
                }
                editedBitmap = result
            }
        }
    }

    // --- IMAGE LAUNCHERS ---
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bmp = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }.copy(Bitmap.Config.ARGB_8888, true)

            originalBitmap = bmp
            transformedBitmap = bmp.copy(Bitmap.Config.ARGB_8888, true)
            // Reset all adjustment parameters
            redValue = 1f; greenValue = 1f; blueValue = 1f
            saturation = 1f; brightness = 1f; contrast = 1f
            grayscale = false
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

    // --- UI ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Image Editor") },
                actions = {
                    IconButton(onClick = {
                        originalBitmap = null
                        transformedBitmap = null
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
                SelectImagePlaceholder(onSelectClick = { launcher.launch("image/*") })
            } else {
                EditorContent(
                    editedBitmap = editedBitmap,
                    redValue = redValue, onRedChange = { redValue = it },
                    greenValue = greenValue, onGreenChange = { greenValue = it },
                    blueValue = blueValue, onBlueChange = { blueValue = it },
                    saturation = saturation, onSaturationChange = { saturation = it },
                    brightness = brightness, onBrightnessChange = { brightness = it },
                    contrast = contrast, onContrastChange = { contrast = it },
                    grayscale = grayscale, onGrayscaleChange = { grayscale = it },
                    onCropClick = { transformedBitmap?.let { transformedBitmap = cropBitmap(it) } },
                    onRotateClick = { transformedBitmap?.let { transformedBitmap = rotateBitmap(it, 90f) } },
                    onPassportClick = { transformedBitmap?.let { transformedBitmap = resizeToPassportSize(it) } },
                    onSaveClick = { editedBitmap?.let { saveBitmapToGallery(context, it) } },
                    onSaveAsClick = { saveAsLauncher.launch("EditedImage_${System.currentTimeMillis()}.png") },
                    onResetClick = {
                        transformedBitmap = originalBitmap?.copy(Bitmap.Config.ARGB_8888, true)
                        redValue = 1f; greenValue = 1f; blueValue = 1f
                        saturation = 1f; brightness = 1f; contrast = 1f
                        grayscale = false
                    }
                )
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
    saturation: Float, onSaturationChange: (Float) -> Unit,
    brightness: Float, onBrightnessChange: (Float) -> Unit,
    contrast: Float, onContrastChange: (Float) -> Unit,
    grayscale: Boolean, onGrayscaleChange: (Boolean) -> Unit,
    onCropClick: () -> Unit, onRotateClick: () -> Unit, onPassportClick: () -> Unit,
    onSaveClick: () -> Unit, onSaveAsClick: () -> Unit, onResetClick: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Transform", "Adjust")

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().height(300.dp).padding(16.dp),
            elevation = 4.dp, shape = RoundedCornerShape(8.dp)
        ) {
            if (editedBitmap != null) {
                Image(
                    bitmap = editedBitmap.asImageBitmap(),
                    contentDescription = "Edited Image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onSaveClick) { Icon(Icons.Default.Save, "Save"); Spacer(Modifier.width(4.dp)); Text("Save") }
            OutlinedButton(onClick = onSaveAsClick) { Text("Save As") }
            OutlinedButton(onClick = onResetClick) { Icon(Icons.Default.RestartAlt, "Reset"); Spacer(Modifier.width(4.dp)); Text("Reset") }
        }
        Spacer(modifier = Modifier.height(16.dp))

        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTabIndex == index, onClick = { selectedTabIndex = index }, text = { Text(title) })
            }
        }

        Box(modifier = Modifier.padding(16.dp)) {
            AnimatedContent(targetState = selectedTabIndex) { tabIndex ->
                when (tabIndex) {
                    0 -> TransformTab(onCropClick, onRotateClick, onPassportClick)
                    1 -> AdjustTab(
                        redValue, onRedChange, greenValue, onGreenChange, blueValue, onBlueChange,
                        saturation, onSaturationChange, brightness, onBrightnessChange,
                        contrast, onContrastChange, grayscale, onGrayscaleChange
                    )
                }
            }
        }
    }
}

@Composable
fun AdjustTab(
    redValue: Float, onRedChange: (Float) -> Unit,
    greenValue: Float, onGreenChange: (Float) -> Unit,
    blueValue: Float, onBlueChange: (Float) -> Unit,
    saturation: Float, onSaturationChange: (Float) -> Unit,
    brightness: Float, onBrightnessChange: (Float) -> Unit,
    contrast: Float, onContrastChange: (Float) -> Unit,
    grayscale: Boolean, onGrayscaleChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Color Balance", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
            ColorSlider(label = "Red", value = redValue, onValueChange = onRedChange, color = Color.Red)
            ColorSlider(label = "Green", value = greenValue, onValueChange = onGreenChange, color = Color.Green)
            ColorSlider(label = "Blue", value = blueValue, onValueChange = onBlueChange, color = Color.Blue)
        }

        Divider()

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Image Adjustments", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
            AdjustmentControls(
                saturation, onSaturationChange,
                brightness, onBrightnessChange,
                contrast, onContrastChange,
                grayscale, onGrayscaleChange
            )
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