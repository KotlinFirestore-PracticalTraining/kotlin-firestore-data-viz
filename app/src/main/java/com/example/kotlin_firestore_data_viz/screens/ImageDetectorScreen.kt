package com.example.kotlin_firestore_data_viz.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.util.concurrent.Executors

@Composable
fun ImageDetectorScreen() {
    val context = LocalContext.current
    var useCamera by remember { mutableStateOf(true) }
    val description = remember { mutableStateOf("Labeling image...") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            labelImageFromUri(context, uri) { labels ->
                description.value = if (labels.isEmpty()) {
                    "No labels found."
                } else {
                    labels.joinToString(", ") { it.text }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = {
                useCamera = true
                description.value = "Labeling image..."
            }) {
                Text("Use Camera")
            }
            Button(onClick = {
                useCamera = false
                imagePickerLauncher.launch("image/*")
            }) {
                Text("Pick from Gallery")
            }
        }

        Spacer(Modifier.height(16.dp))

        if (useCamera) {
            CameraPreviewLabeling(
                context = context,
                onLabelsDetected = { labels ->
                    description.value = if (labels.isEmpty()) {
                        "No labels found."
                    } else {
                        labels.joinToString(", ") { it.text }
                    }
                }
            )
        } else {
            selectedImageUri?.let { uri ->
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(text = description.value)
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreviewLabeling(
    context: Context,
    onLabelsDetected: (List<ImageLabel>) -> Unit
) {
    val previewView = remember { PreviewView(context) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(Unit) {
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        analysis.setAnalyzer(executor) { imageProxy ->
            val mediaImage = imageProxy.image
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

            if (mediaImage != null) {
                val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

                labeler.process(inputImage)
                    .addOnSuccessListener { labels ->
                        onLabelsDetected(labels)
                    }
                    .addOnFailureListener {
                        Log.e("MLKit", "Labeling failed", it)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                context as androidx.lifecycle.LifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )
        } catch (exc: Exception) {
            Log.e("CameraX", "Use case binding failed", exc)
        }
    }

    AndroidView(
        factory = {
            previewView.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    1200
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

fun labelImageFromUri(
    context: Context,
    uri: Uri,
    onResult: (List<ImageLabel>) -> Unit
) {
    val inputStream = context.contentResolver.openInputStream(uri)
    val bitmap = BitmapFactory.decodeStream(inputStream)
    val image = InputImage.fromBitmap(bitmap, 0)

    val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    labeler.process(image)
        .addOnSuccessListener { labels ->
            onResult(labels)
        }
        .addOnFailureListener {
            Log.e("MLKit", "Image labeling failed", it)
            onResult(emptyList())
        }
}
