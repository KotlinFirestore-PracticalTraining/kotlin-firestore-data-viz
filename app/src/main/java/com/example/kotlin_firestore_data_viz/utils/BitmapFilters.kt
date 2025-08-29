package com.example.kotlin_firestore_data_viz.utils

import android.graphics.*

/**
 * Applies an RGB color filter to a source bitmap.
 * @param source The original bitmap to apply the filter to.
 * @param red The multiplicative factor for the red channel (1.0 means no change).
 * @param green The multiplicative factor for the green channel (1.0 means no change).
 * @param blue The multiplicative factor for the blue channel (1.0 means no change).
 * @return A new bitmap with the RGB filter applied.
 */
fun applyRgbFilter(source: Bitmap, red: Float, green: Float, blue: Float): Bitmap {
    // Use the Elvis operator to provide a default config if source.config is null
    val result = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint()
    val colorMatrix = ColorMatrix()

    // Set the scale for each color channel.
    // The fourth value is for the alpha channel (1f = no change).
    colorMatrix.setScale(red, green, blue, 1f)

    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    canvas.drawBitmap(source, 0f, 0f, paint)

    return result
}