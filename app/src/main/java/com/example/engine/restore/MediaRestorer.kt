package com.example.engine.restore

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.model.RecoverablePhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.Locale

object MediaRestorer {

    /**
     * Restores a photo, video or audio to the system MediaStore.
     */
    suspend fun restoreMedia(context: Context, photo: RecoverablePhoto): Uri? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val originalName = photo.name.removePrefix(".")
            val isVideo = photo.isVideo
            val isAudio = photo.isAudio

            val defaultExt = when {
                isVideo -> "mp4"
                isAudio -> "mp3"
                else -> "jpg"
            }
            val cleanName = if (!originalName.contains(".")) "$originalName.$defaultExt" else originalName
            val restoredFileName = "Restaurado_${System.currentTimeMillis()}_$cleanName"

            val mimeType = when {
                isVideo -> {
                    when (photo.fileExtension.lowercase(Locale.ROOT)) {
                        "mkv" -> "video/x-matroska"
                        "mov" -> "video/quicktime"
                        "3gp" -> "video/3gpp"
                        "webm" -> "video/webm"
                        "avi" -> "video/x-msvideo"
                        else -> "video/mp4"
                    }
                }
                isAudio -> {
                    when (photo.fileExtension.lowercase(Locale.ROOT)) {
                        "aac" -> "audio/aac"
                        "ogg", "opus" -> "audio/ogg"
                        "wav" -> "audio/wav"
                        "m4a" -> "audio/mp4"
                        "amr" -> "audio/amr"
                        "flac" -> "audio/flac"
                        else -> "audio/mpeg"
                    }
                }
                else -> {
                    when (photo.fileExtension.lowercase(Locale.ROOT)) {
                        "png" -> "image/png"
                        "webp" -> "image/webp"
                        "heic" -> "image/heic"
                        "gif" -> "image/gif"
                        else -> "image/jpeg"
                    }
                }
            }

            val relativePath = when {
                isVideo -> "${Environment.DIRECTORY_MOVIES}/Restored_Videos"
                isAudio -> "${Environment.DIRECTORY_MUSIC}/Restored_Audio"
                else -> "${Environment.DIRECTORY_PICTURES}/Restored_Photos"
            }

            val targetCollection = when {
                isVideo -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                isAudio -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val contentValues = ContentValues().apply {
                when {
                    isVideo -> {
                        put(MediaStore.Video.Media.DISPLAY_NAME, restoredFileName)
                        put(MediaStore.Video.Media.MIME_TYPE, mimeType)
                        put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                        put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Video.Media.RELATIVE_PATH, relativePath)
                            put(MediaStore.Video.Media.IS_PENDING, 1)
                        }
                    }
                    isAudio -> {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, restoredFileName)
                        put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                        put(MediaStore.Audio.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                        put(MediaStore.Audio.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath)
                            put(MediaStore.Audio.Media.IS_PENDING, 1)
                        }
                    }
                    else -> {
                        put(MediaStore.Images.Media.DISPLAY_NAME, restoredFileName)
                        put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                        put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                        put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                            put(MediaStore.Images.Media.IS_PENDING, 1)
                        }
                    }
                }
            }

            val insertedUri = contentResolver.insert(
                targetCollection,
                contentValues
            ) ?: return@withContext null

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
                when {
                    isVideo -> contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                    isAudio -> contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    else -> contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                contentResolver.update(insertedUri, contentValues, null, null)
            }

            // Trigger MediaScanner to index immediately
            val scannerMime = when {
                isVideo -> "video/*"
                isAudio -> "audio/*"
                else -> "image/*"
            }
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
