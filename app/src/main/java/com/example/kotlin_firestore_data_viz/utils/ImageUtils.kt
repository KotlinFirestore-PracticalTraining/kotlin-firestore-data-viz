package com.example.kotlin_firestore_data_viz.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.provider.MediaStore
import android.widget.Toast


fun applyImageFilters(
    source: Bitmap,
    hue: Float = 0f,
    saturation: Float = 1f,
    brightness: Float = 1f,
    contrast: Float = 1f
): Bitmap {
    val src = source.copy(Bitmap.Config.ARGB_8888, true)
    val bmp = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint()

    // Hue
    val hueMatrix = ColorMatrix().apply {
        setRotate(0, hue)
        setRotate(1, hue)
        setRotate(2, hue)
    }

    // Saturation
    val saturationMatrix = ColorMatrix().apply {
        setSaturation(saturation)
    }

    // Brightness
    val brightnessMatrix = ColorMatrix(
        floatArrayOf(
            brightness, 0f, 0f, 0f, 0f,
            0f, brightness, 0f, 0f, 0f,
            0f, 0f, brightness, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )

    // ✅ Contrast
    val contrastScale = contrast
    val contrastTranslate = (-0.5f * contrastScale + 0.5f) * 255f
    val contrastMatrix = ColorMatrix(
        floatArrayOf(
            contrastScale, 0f, 0f, 0f, contrastTranslate,
            0f, contrastScale, 0f, 0f, contrastTranslate,
            0f, 0f, contrastScale, 0f, contrastTranslate,
            0f, 0f, 0f, 1f, 0f
        )
    )

    // Merge all filters
    hueMatrix.postConcat(saturationMatrix)
    hueMatrix.postConcat(brightnessMatrix)
    hueMatrix.postConcat(contrastMatrix)

    paint.colorFilter = ColorMatrixColorFilter(hueMatrix)
    canvas.drawBitmap(src, 0f, 0f, paint)

    return bmp
}


fun cropBitmap(source: Bitmap): Bitmap {
    val width = source.width
    val height = source.height

    // Crop 80% of the center area
    val newWidth = (width * 0.8).toInt()
    val newHeight = (height * 0.8).toInt()
    val startX = (width - newWidth) / 2
    val startY = (height - newHeight) / 2

    return Bitmap.createBitmap(source, startX, startY, newWidth, newHeight)
}

fun resizeBitmap(source: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
    val resizedBitmap = Bitmap.createBitmap(newWidth, newHeight, source.config ?: Bitmap.Config.ARGB_8888)
    val canvas = Canvas(resizedBitmap)
    val scaleX = newWidth.toFloat() / source.width
    val scaleY = newHeight.toFloat() / source.height
    val scaleMatrix = android.graphics.Matrix().apply {
        setScale(scaleX, scaleY)
    }
    canvas.drawBitmap(source, scaleMatrix, null)
    return resizedBitmap
}

// Utility functions
fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
    val matrix = android.graphics.Matrix().apply { postRotate(angle) }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

fun saveBitmapToGallery(context: Context, bitmap: Bitmap) {
    // Show a message before saving
    Toast.makeText(context, "Saving image...", Toast.LENGTH_SHORT).show()

    val filename = "IMG_${System.currentTimeMillis()}.png"
    val contentValues = android.content.ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyApp") // Save in Pictures/MyApp folder
    }

    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    uri?.let { outputUri ->
        context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                throw IllegalStateException("Failed to save bitmap")
            }
        }
        // Show a success message after saving
        Toast.makeText(context, "Image saved successfully!", Toast.LENGTH_SHORT).show()
    } ?: throw IllegalStateException("Failed to create MediaStore entry")
}