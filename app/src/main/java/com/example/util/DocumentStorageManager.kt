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
     * Validates size, extension, encrypts with authenticated AES-256-GCM, and atomically saves into private app storage.
     */
    fun saveDocumentStream(
        context: Context,
        inputStream: InputStream,
        originalFilename: String? = null
    ): DocumentSaveResult {
        val dir = getDocsDirectory(context)
        var tempFile: File? = null
        return try {
            val extension = originalFilename?.substringAfterLast('.', "jpg")?.lowercase() ?: "jpg"
            if (extension !in ALLOWED_EXTENSIONS) {
                return DocumentSaveResult.Error("Unsupported file type .$extension. Allowed: jpg, png, webp, pdf.")
            }

            val safeName = "doc_${UUID.randomUUID().toString().replace("-", "")}.$extension"
            val targetFile = File(dir, safeName)
            tempFile = File(dir, "$safeName.tmp")

            val byteStream = java.io.ByteArrayOutputStream()
            var totalBytes = 0L
            val buffer = ByteArray(8192)
            var read: Int

            while (inputStream.read(buffer).also { read = it } != -1) {
                totalBytes += read
                if (totalBytes > MAX_FILE_SIZE_BYTES) {
                    return DocumentSaveResult.Error("Document exceeds maximum size limit of 10 MB.")
                }
                byteStream.write(buffer, 0, read)
            }

            val plainBytes = byteStream.toByteArray()
            val encryptedBytes = com.example.ui.SubtleCrypto.encryptBytes(plainBytes)

            // Safe atomic file handling: write to .tmp, flush, sync to disk, then atomically rename
            FileOutputStream(tempFile).use { fos ->
                fos.write(encryptedBytes)
                fos.flush()
                try {
                    fos.fd.sync()
                } catch (ignored: Exception) {}
            }

            if (!tempFile.renameTo(targetFile)) {
                // Fallback copy if direct rename fails on some file systems
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            DocumentSaveResult.Success(
                relativePath = safeName,
                fileSizeBytes = targetFile.length()
            )
        } catch (e: Exception) {
            tempFile?.delete()
            DocumentSaveResult.Error("Failed to store encrypted document: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    /**
     * Saves a captured Bitmap directly into private internal storage as an encrypted JPEG.
     */
    fun saveBitmapDocument(context: Context, bitmap: Bitmap, quality: Int = 85): DocumentSaveResult {
        val dir = getDocsDirectory(context)
        var tempFile: File? = null
        return try {
            val resized = ImageUtils.resizeBitmap(bitmap, maxDimension = 1600)
            val safeName = "doc_${UUID.randomUUID().toString().replace("-", "")}.jpg"
            val targetFile = File(dir, safeName)
            tempFile = File(dir, "$safeName.tmp")

            val byteStream = java.io.ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, quality, byteStream)
            val plainBytes = byteStream.toByteArray()

            val encryptedBytes = com.example.ui.SubtleCrypto.encryptBytes(plainBytes)

            FileOutputStream(tempFile).use { fos ->
                fos.write(encryptedBytes)
                fos.flush()
                try {
                    fos.fd.sync()
                } catch (ignored: Exception) {}
            }

            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            DocumentSaveResult.Success(
                relativePath = safeName,
                fileSizeBytes = targetFile.length()
            )
        } catch (e: Exception) {
            tempFile?.delete()
            DocumentSaveResult.Error("Failed to save encrypted photo: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    /**
     * Loads raw decrypted bytes from a stored document reference safely.
     * Supports both encrypted files (FEV_ENC1 header) and legacy unencrypted files for seamless backward compatibility.
     */
    fun loadDocumentBytes(context: Context, pathOrUri: String?): ByteArray? {
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
                val rawBytes = file.readBytes()
                com.example.ui.SubtleCrypto.decryptBytes(rawBytes)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Loads a Bitmap from an encrypted stored document reference safely without CursorWindow or OOM issues.
     * Decodes bounds from decrypted byte array, calculates optimal inSampleSize, and decodes Bitmap.
     */
    fun loadDocumentBitmap(context: Context, pathOrUri: String?, maxDimension: Int = 1200): Bitmap? {
        val decryptedBytes = loadDocumentBytes(context, pathOrUri) ?: return null

        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size, options)

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
            BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size, decodeOptions)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks whether an existing stored document file is encrypted with the FEV_ENC1 format.
     */
    fun isDocumentEncrypted(context: Context, pathOrUri: String?): Boolean {
        if (pathOrUri.isNullOrBlank()) return false
        return try {
            val file = File(getDocsDirectory(context), pathOrUri.substringAfterLast('/'))
            if (file.exists() && file.length() >= 20) {
                val header = ByteArray(8)
                file.inputStream().use { it.read(header) }
                com.example.ui.SubtleCrypto.isEncryptedFile(header)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Migrates any unencrypted legacy documents in private storage to authenticated AES-256-GCM.
     * Preserves access to existing documents during migration: if encryption or writing fails,
     * the original file is NEVER deleted or silently overwritten.
     * Returns count of migrated documents.
     */
    fun migrateLegacyDocumentsToEncrypted(context: Context): Int {
        val dir = getDocsDirectory(context)
        if (!dir.exists() || !dir.isDirectory) return 0

        var migratedCount = 0
        val files = dir.listFiles() ?: return 0

        for (file in files) {
            if (file.isFile && !file.name.endsWith(".tmp")) {
                try {
                    val rawBytes = file.readBytes()
                    if (!com.example.ui.SubtleCrypto.isEncryptedFile(rawBytes)) {
                        // Unencrypted legacy file - encrypt safely via temp file
                        val encryptedBytes = com.example.ui.SubtleCrypto.encryptBytes(rawBytes)
                        val tempFile = File(dir, "${file.name}.tmp")
                        FileOutputStream(tempFile).use { fos ->
                            fos.write(encryptedBytes)
                            fos.flush()
                            try { fos.fd.sync() } catch (ignored: Exception) {}
                        }
                        if (tempFile.renameTo(file)) {
                            migratedCount++
                        } else {
                            tempFile.delete()
                        }
                    }
                } catch (e: Exception) {
                    // Fail safely: preserve original file untouched
                }
            }
        }
        return migratedCount
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
