package com.example.kotlin_firestore_data_viz.screens

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.kotlin_firestore_data_viz.utils.cropBitmap
import com.example.kotlin_firestore_data_viz.controller.FilterControls
import com.example.kotlin_firestore_data_viz.utils.resizeToPassportSize
import com.example.kotlin_firestore_data_viz.utils.rotateBitmap
import com.example.kotlin_firestore_data_viz.utils.saveBitmapToGallery


@Composable
fun ImageEditorScreen() {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var editedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            val bmp = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
            originalBitmap = bmp
            editedBitmap = bmp.copy(Bitmap.Config.ARGB_8888, true)
        }
    }

    val saveAsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri: Uri? ->
        uri?.let { outputUri ->
            editedBitmap?.let { bitmap ->
                context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                        Log.e("SaveAs", "Failed to save bitmap")
                    } else {
                        Toast.makeText(context, "Image saved successfully!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = { launcher.launch("image/*") }) {
            Text("Select Image")
        }

        Spacer(modifier = Modifier.height(16.dp))

        editedBitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .border(1.dp, Color.Gray)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    editedBitmap = cropBitmap(bmp)
                }) {
                    Text("Crop")
                }
                Button(onClick = {
                    editedBitmap = editedBitmap?.let { rotateBitmap(it, 90f) }
                }) {
                    Text("Rotate 90°")
                }
                Button(onClick = {
                    editedBitmap?.let { saveBitmapToGallery(context, it) }
                }) {
                    Text("Save")
                }
                Button(onClick = {
                    saveAsLauncher.launch("EditedImage_${System.currentTimeMillis()}.png")
                }) {
                    Text("Save As")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    editedBitmap = editedBitmap?.let { resizeToPassportSize(it) }
                }) {
                    Text("Passport Size")
                }

                Button(onClick = {
                    editedBitmap = originalBitmap?.copy(Bitmap.Config.ARGB_8888, true)
                }) {
                    Text("Reset")
                }

                Button(onClick = {
                    originalBitmap = null
                    editedBitmap = null
                    imageUri = null
                }) {
                    Text("Cancel")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Use originalBitmap for filtering, update editedBitmap
            originalBitmap?.let { original ->
                FilterControls(source = original) { newBmp ->
                    editedBitmap = newBmp
                }
            }
        }
    }
}
