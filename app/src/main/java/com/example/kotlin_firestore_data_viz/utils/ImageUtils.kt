package com.example.kotlin_firestore_data_viz.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint


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
