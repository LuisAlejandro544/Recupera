package com.example.engine.filter

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.model.RecoverySource
import java.io.File
import java.util.Locale

data class ActiveGallerySignatures(
    val activePaths: Set<String>,
    val activeFingerprints: Set<String>
)

object ActiveGalleryFilter {

    /**
     * Queries active (non-trashed) MediaStore pictures, videos, and audios to build an exclusion index.
     * Active items visible in standard gallery are excluded from recovery results.
     */
    fun loadActiveGallerySignatures(context: Context): ActiveGallerySignatures {
        val activePaths = mutableSetOf<String>()
        val activeFingerprints = mutableSetOf<String>()
        val contentResolver = context.contentResolver

        // 1. Query active Images (IS_TRASHED = 0)
        try {
            val projection = arrayOf(
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE
            )
            val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                "${MediaStore.Images.Media.IS_TRASHED} = 0"
            } else {
                null
            }
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null
            )?.use { cursor ->
                val dataCol = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                while (cursor.moveToNext()) {
                    if (dataCol != -1) {
                        val path = cursor.getString(dataCol)
                        if (!path.isNullOrBlank()) {
                            activePaths.add(path)
                            try {
                                activePaths.add(File(path).canonicalPath)
                            } catch (_: Exception) {}
                        }
                    }
                    val name = if (nameCol != -1) cursor.getString(nameCol) else null
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                    if (!name.isNullOrBlank() && size > 0) {
                        activeFingerprints.add("${name.lowercase(Locale.ROOT)}|$size")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("ActiveGalleryFilter", "Error querying active images: ${e.message}")
        }

        // 2. Query active Videos (IS_TRASHED = 0)
        try {
            val projection = arrayOf(
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE
            )
            val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                "${MediaStore.Video.Media.IS_TRASHED} = 0"
            } else {
                null
            }
            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null
            )?.use { cursor ->
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                while (cursor.moveToNext()) {
                    if (dataCol != -1) {
                        val path = cursor.getString(dataCol)
                        if (!path.isNullOrBlank()) {
                            activePaths.add(path)
                            try {
                                activePaths.add(File(path).canonicalPath)
                            } catch (_: Exception) {}
                        }
                    }
                    val name = if (nameCol != -1) cursor.getString(nameCol) else null
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                    if (!name.isNullOrBlank() && size > 0) {
                        activeFingerprints.add("${name.lowercase(Locale.ROOT)}|$size")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("ActiveGalleryFilter", "Error querying active videos: ${e.message}")
        }

        // 3. Query active Audios (IS_TRASHED = 0)
        try {
            val projection = arrayOf(
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.SIZE
            )
            val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                "${MediaStore.Audio.Media.IS_TRASHED} = 0"
            } else {
                null
            }
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null
            )?.use { cursor ->
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val nameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                while (cursor.moveToNext()) {
                    if (dataCol != -1) {
                        val path = cursor.getString(dataCol)
                        if (!path.isNullOrBlank()) {
                            activePaths.add(path)
                            try {
                                activePaths.add(File(path).canonicalPath)
                            } catch (_: Exception) {}
                        }
                    }
                    val name = if (nameCol != -1) cursor.getString(nameCol) else null
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                    if (!name.isNullOrBlank() && size > 0) {
                        activeFingerprints.add("${name.lowercase(Locale.ROOT)}|$size")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("ActiveGalleryFilter", "Error querying active audios: ${e.message}")
        }

        return ActiveGallerySignatures(activePaths, activeFingerprints)
    }

    /**
     * Determines whether a detected file is already visible in the user's active photo/media gallery.
     */
    fun isFileInActiveGallery(
        file: File,
        source: RecoverySource,
        activePaths: Set<String>,
        activeFingerprints: Set<String>
    ): Boolean {
        // Trashed items from MediaStore are explicitly deleted/recoverable
        if (source == RecoverySource.TRASH_MEDIASTORE) return false

        val path = file.absolutePath
        if (activePaths.contains(path)) return true
        try {
            if (activePaths.contains(file.canonicalPath)) return true
        } catch (_: Exception) {}

        val fingerprint = "${file.name.lowercase(Locale.ROOT)}|${file.length()}"
        val isInResidualFolder = path.contains(".thumbnails") ||
                path.contains(".nomedia") ||
                path.contains(".trash") ||
                path.contains("cache", ignoreCase = true) ||
                file.name.startsWith(".")

        if (activeFingerprints.contains(fingerprint) && !isInResidualFolder) {
            return true
        }

        // Direct public gallery folders (DCIM/Camera, Pictures, Movies, Music) that are visible and NOT residual
        val isDirectVisibleGalleryFolder = (
            path.contains("/DCIM/Camera/", ignoreCase = true) ||
            path.contains("/DCIM/100ANDRO/", ignoreCase = true) ||
            (path.contains("/Pictures/", ignoreCase = true) && !path.contains("Restored_Photos")) ||
            (path.contains("/Movies/", ignoreCase = true) && !path.contains("Restored_Videos")) ||
            (path.contains("/Music/", ignoreCase = true) && !path.contains("Restored_Audio"))
        ) && !isInResidualFolder

        if (isDirectVisibleGalleryFolder) {
            return true
        }

        return false
    }
}
