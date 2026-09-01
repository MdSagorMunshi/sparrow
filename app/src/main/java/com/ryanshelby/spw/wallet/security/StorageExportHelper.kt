package com.ryanshelby.spw.wallet.security

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import java.io.File

/**
 * Utility for saving exported PDFs and CSVs directly to the user's public Downloads directory.
 */
object StorageExportHelper {

    private const val TAG = "StorageExportHelper"

    fun saveFileToDownloads(context: Context, sourceFile: File, mimeType: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, sourceFile.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SPW_Wallet")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        sourceFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    Toast.makeText(context, "Saved to Downloads/SPW_Wallet/${sourceFile.name}", Toast.LENGTH_LONG).show()
                    true
                } else {
                    // Fallback to direct copy if MediaStore URI is null
                    fallbackDirectSave(context, sourceFile, mimeType)
                }
            } else {
                fallbackDirectSave(context, sourceFile, mimeType)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save file to downloads", e)
            Toast.makeText(context, "Save failed: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun fallbackDirectSave(context: Context, sourceFile: File, mimeType: String): Boolean {
        return try {
            val downloadDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "SPW_Wallet")
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            val destFile = File(downloadDir, sourceFile.name)
            sourceFile.copyTo(destFile, overwrite = true)
            MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), arrayOf(mimeType), null)
            Toast.makeText(context, "Saved to Downloads/SPW_Wallet/${sourceFile.name}", Toast.LENGTH_LONG).show()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Fallback direct save failed", e)
            false
        }
    }
}
