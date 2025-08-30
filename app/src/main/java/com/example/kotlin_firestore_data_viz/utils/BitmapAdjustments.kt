package com.example.kotlin_firestore_data_viz.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

/**
 * Applies a chain of color adjustments to a bitmap in a single, efficient operation.
 */
fun applyAllAdjustments(
    source: Bitmap,
    saturation: Float,
    brightness: Float,
    contrast: Float,
    red: Float,
    green: Float,
    blue: Float
): Bitmap {
    val result = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint()
    val matrix = ColorMatrix()

    // 1. Saturation
    val saturationMatrix = ColorMatrix().apply { setSaturation(saturation) }

    // 2. Contrast
    val contrastMatrix = ColorMatrix().apply {
        val c = contrast
        val offset = (1f - c) * 127.5f // Offset to keep middle gray constant
        set(floatArrayOf(
            c, 0f, 0f, 0f, offset,
            0f, c, 0f, 0f, offset,
            0f, 0f, c, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        ))
    }

    // 3. Brightness and RGB balance are combined for efficiency
    val scaleMatrix = ColorMatrix().apply {
        setScale(brightness * red, brightness * green, brightness * blue, 1f)
    }

    // Concatenate all matrices. Order matters.
    matrix.postConcat(saturationMatrix)
    matrix.postConcat(contrastMatrix)
    matrix.postConcat(scaleMatrix)

    paint.colorFilter = ColorMatrixColorFilter(matrix)
    canvas.drawBitmap(source, 0f, 0f, paint)
    return result
}