package com.example.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {

    /**
     * Resizes bitmap to ensure maximum dimension does not exceed [maxDimension]
     * to keep SQLite storage fast and within Android CursorWindow bounds.
     */
    fun resizeBitmap(bitmap: Bitmap, maxDimension: Int = 1024): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }
        val ratio = width.toFloat() / height.toFloat()
        val (newWidth, newHeight) = if (width > height) {
            maxDimension to (maxDimension / ratio).toInt()
        } else {
            (maxDimension * ratio).toInt() to maxDimension
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth.coerceAtLeast(1), newHeight.coerceAtLeast(1), true)
    }

    /**
     * Converts a Bitmap to a Base64 string encoded in JPEG format.
     */
    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 80): String {
        return try {
            val resized = resizeBitmap(bitmap)
            val outputStream = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Decodes a Base64 string back into an Android Bitmap.
     */
    fun base64ToBitmap(base64Str: String): Bitmap? {
        if (base64Str.isBlank()) return null
        return try {
            val cleanStr = if (base64Str.contains(",")) base64Str.substringAfter(",") else base64Str
            val decodedBytes = Base64.decode(cleanStr, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
