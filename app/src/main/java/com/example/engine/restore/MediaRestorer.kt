package com.example.engine.restore

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.model.RecoverablePhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

object MediaRestorer {

    /**
     * Restores a photo, video or audio to the system MediaStore.
     */
    suspend fun restoreMedia(context: Context, photo: RecoverablePhoto): Uri? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val restoredFileName = MediaDestinationResolver.generateRestoredFileName(photo)
            val mimeType = MimeTypeResolver.resolveMimeType(photo)
            val relativePath = MediaDestinationResolver.resolveRelativePath(photo)
            val targetCollection = MediaDestinationResolver.resolveTargetCollection(photo)

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, restoredFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis() / 1000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val insertedUri = contentResolver.insert(targetCollection, contentValues) ?: return@withContext null

            // Read source bytes and write to destination
            val sourceStream: InputStream? = if (photo.filePath.isNotBlank() && File(photo.filePath).exists()) {
                File(photo.filePath).inputStream()
            } else if (photo.contentUri != null) {
                contentResolver.openInputStream(photo.contentUri)
            } else {
                null
            }

            if (sourceStream != null) {
                sourceStream.use { input ->
                    contentResolver.openOutputStream(insertedUri)?.use { output ->
                        input.copyTo(output)
                    }
                }
            }

            // Mark IS_PENDING = 0 on API 29+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(insertedUri, contentValues, null, null)
            }

            // Trigger MediaScanner to index immediately
            val scannerMime = MimeTypeResolver.resolveScannerMimeType(photo)
            notifyMediaScanner(context, insertedUri, scannerMime)

            insertedUri
        } catch (e: Exception) {
            Log.e("MediaRestorer", "Error restoring media ${photo.name}: ${e.message}")
            null
        }
    }

    private fun notifyMediaScanner(context: Context, uri: Uri, mimeType: String) {
        try {
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DATA),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val dataIndex = it.getColumnIndex(MediaStore.MediaColumns.DATA)
                    if (dataIndex != -1) {
                        val filePath = it.getString(dataIndex)
                        if (!filePath.isNullOrBlank()) {
                            MediaScannerConnection.scanFile(
                                context,
                                arrayOf(filePath),
                                arrayOf(mimeType),
                                null
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("MediaRestorer", "MediaScanner notice failed: ${e.message}")
        }
    }
}
