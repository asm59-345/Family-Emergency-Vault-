package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

sealed class DocumentSaveResult {
    data class Success(val relativePath: String, val fileSizeBytes: Long) : DocumentSaveResult()
    data class Error(val message: String) : DocumentSaveResult()
}

object DocumentStorageManager {

    private const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024 // 10 MB limit
    private const val DOCS_DIR_NAME = "vault_documents"

    private val ALLOWED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "pdf")

    private fun getDocsDirectory(context: Context): File {
        val dir = File(context.filesDir, DOCS_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Saves a document from an input stream (e.g. from Photo Picker or File Picker).
     * Validates size, extension, and saves into private app storage.
     */
    fun saveDocumentStream(
        context: Context,
        inputStream: InputStream,
        originalFilename: String? = null
    ): DocumentSaveResult {
        return try {
            val extension = originalFilename?.substringAfterLast('.', "jpg")?.lowercase() ?: "jpg"
            if (extension !in ALLOWED_EXTENSIONS) {
                return DocumentSaveResult.Error("Unsupported file type .$extension. Allowed: jpg, png, webp, pdf.")
            }

            val safeName = "doc_${UUID.randomUUID().toString().replace("-", "")}.$extension"
            val targetFile = File(getDocsDirectory(context), safeName)

            var totalBytes = 0L
            val buffer = ByteArray(8192)

            FileOutputStream(targetFile).use { output ->
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    totalBytes += read
                    if (totalBytes > MAX_FILE_SIZE_BYTES) {
                        targetFile.delete()
                        return DocumentSaveResult.Error("Document exceeds maximum size limit of 10 MB.")
                    }
                    output.write(buffer, 0, read)
                }
                output.flush()
            }

            DocumentSaveResult.Success(
                relativePath = safeName,
                fileSizeBytes = totalBytes
            )
        } catch (e: Exception) {
            DocumentSaveResult.Error("Failed to store document: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    /**
     * Saves a captured Bitmap directly into private internal storage as JPEG.
     */
    fun saveBitmapDocument(context: Context, bitmap: Bitmap, quality: Int = 85): DocumentSaveResult {
        return try {
            val resized = ImageUtils.resizeBitmap(bitmap, maxDimension = 1600)
            val safeName = "doc_${UUID.randomUUID().toString().replace("-", "")}.jpg"
            val targetFile = File(getDocsDirectory(context), safeName)

            FileOutputStream(targetFile).use { out ->
                resized.compress(Bitmap.CompressFormat.JPEG, quality, out)
                out.flush()
            }

            DocumentSaveResult.Success(
                relativePath = safeName,
                fileSizeBytes = targetFile.length()
            )
        } catch (e: Exception) {
            DocumentSaveResult.Error("Failed to save photo: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    /**
     * Loads a Bitmap from a stored document reference safely without CursorWindow or OOM issues.
     * Checks private files first, then parses content URI if provided.
     */
    fun loadDocumentBitmap(context: Context, pathOrUri: String?, maxDimension: Int = 1200): Bitmap? {
        if (pathOrUri.isNullOrBlank()) return null

        return try {
            val file = if (pathOrUri.startsWith("/")) {
                File(pathOrUri)
            } else if (pathOrUri.startsWith("file://")) {
                File(Uri.parse(pathOrUri).path ?: "")
            } else {
                File(getDocsDirectory(context), pathOrUri.substringAfterLast('/'))
            }

            if (file.exists() && file.isFile) {
                // Decode bounds first for memory-safe sampling
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(file.absolutePath, options)

                var inSampleSize = 1
                if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                    val halfHeight = options.outHeight / 2
                    val halfWidth = options.outWidth / 2
                    while ((halfHeight / inSampleSize) >= maxDimension && (halfWidth / inSampleSize) >= maxDimension) {
                        inSampleSize *= 2
                    }
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                }
                BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Deletes a document file from private app storage to clean up disk space.
     */
    fun deleteDocument(context: Context, pathOrUri: String?): Boolean {
        if (pathOrUri.isNullOrBlank()) return false
        return try {
            val file = File(getDocsDirectory(context), pathOrUri.substringAfterLast('/'))
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Returns the size of the private vault documents directory in bytes.
     */
    fun getTotalStorageSizeBytes(context: Context): Long {
        val dir = getDocsDirectory(context)
        return dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    }
}
