package com.example.engine.scan.trash

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import com.example.model.FileHealth
import com.example.model.HealthLevel
import com.example.model.MediaType
import com.example.model.RecoverablePhoto
import com.example.model.RecoverySource
import java.util.Locale

object MediaStoreTrashQueryHelper {

    /**
     * Executes a generalized query on MediaStore for items marked as IS_TRASHED (API 30+).
     */
    fun queryTrashedMedia(
        context: Context,
        contentUri: Uri,
        mediaType: MediaType,
        idPrefix: String,
        defaultNamePrefix: String,
        defaultExt: String,
        defaultSize: Long,
        foundPhotos: MutableList<RecoverablePhoto>,
        seenPaths: MutableSet<String>
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

        try {
            val baseProjection = mutableListOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_MODIFIED
            )

            if (mediaType == MediaType.IMAGE || mediaType == MediaType.VIDEO) {
                baseProjection.add(MediaStore.MediaColumns.WIDTH)
                baseProjection.add(MediaStore.MediaColumns.HEIGHT)
            }
            if (mediaType == MediaType.VIDEO || mediaType == MediaType.AUDIO) {
                baseProjection.add(MediaStore.MediaColumns.DURATION)
            }

            val bundle = Bundle().apply {
                putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
            }

            val cursor = context.contentResolver.query(
                contentUri,
                baseProjection.toTypedArray(),
                bundle,
                null
            )

            cursor?.use {
                parseCursorItems(
                    cursor = it,
                    contentUri = contentUri,
                    mediaType = mediaType,
                    idPrefix = idPrefix,
                    defaultNamePrefix = defaultNamePrefix,
                    defaultExt = defaultExt,
                    defaultSize = defaultSize,
                    foundPhotos = foundPhotos,
                    seenPaths = seenPaths
                )
            }
        } catch (e: Exception) {
            Log.w("TrashQueryHelper", "Error querying trashed $mediaType: ${e.message}")
        }
    }

    private fun parseCursorItems(
        cursor: Cursor,
        contentUri: Uri,
        mediaType: MediaType,
        idPrefix: String,
        defaultNamePrefix: String,
        defaultExt: String,
        defaultSize: Long,
        foundPhotos: MutableList<RecoverablePhoto>,
        seenPaths: MutableSet<String>
    ) {
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
        val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
        val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
        val widthCol = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
        val heightCol = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
        val durationCol = cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val name = cursor.getString(nameCol) ?: "${defaultNamePrefix}_$id.$defaultExt"
            val path = if (dataCol != -1) cursor.getString(dataCol) else ""
            val size = cursor.getLong(sizeCol)
            val dateModified = cursor.getLong(dateCol) * 1000
            val width = if (widthCol != -1) cursor.getInt(widthCol) else 0
            val height = if (heightCol != -1) cursor.getInt(heightCol) else 0
            val duration = if (durationCol != -1) cursor.getLong(durationCol) else 0L

            val itemUri = Uri.withAppendedPath(contentUri, id.toString())
            val ext = name.substringAfterLast(".", defaultExt).lowercase(Locale.ROOT)
            val dimStr = if (width > 0 && height > 0) "${width}x${height}" else null
            val finalPath = if (!path.isNullOrBlank()) path else itemUri.toString()

            if (!seenPaths.contains(finalPath)) {
                seenPaths.add(finalPath)
                foundPhotos.add(
                    RecoverablePhoto(
                        id = "${idPrefix}_$id",
                        name = name,
                        filePath = finalPath,
                        contentUri = itemUri,
                        fileSizeBytes = if (size > 0) size else defaultSize,
                        lastModifiedTimestamp = if (dateModified > 0) dateModified else System.currentTimeMillis(),
                        sourceCategory = RecoverySource.TRASH_MEDIASTORE,
                        fileExtension = ext,
                        dimensions = dimStr,
                        mediaType = mediaType,
                        durationMs = duration,
                        health = FileHealth(100, HealthLevel.EXCELLENT, "Archivo original en papelera (100% íntegro)")
                    )
                )
            }
        }
    }
}
