package com.example.engine.scan

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.model.FileHealth
import com.example.model.HealthLevel
import com.example.model.MediaType
import com.example.model.RecoverablePhoto
import com.example.model.RecoverySource
import java.util.Locale

object TrashMediaScanner {

    fun scanMediaStoreTrash(
        context: Context,
        foundPhotos: MutableList<RecoverablePhoto>,
        seenPaths: MutableSet<String>
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.SIZE,
                    MediaStore.Images.Media.DATE_MODIFIED,
                    MediaStore.Images.Media.WIDTH,
                    MediaStore.Images.Media.HEIGHT
                )

                val bundle = android.os.Bundle().apply {
                    putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
                }

                val cursor = context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    bundle,
                    null
                )

                cursor?.use {
                    val idCol = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val dataCol = it.getColumnIndex(MediaStore.Images.Media.DATA)
                    val sizeCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                    val dateCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                    val widthCol = it.getColumnIndex(MediaStore.Images.Media.WIDTH)
                    val heightCol = it.getColumnIndex(MediaStore.Images.Media.HEIGHT)

                    while (it.moveToNext()) {
                        val id = it.getLong(idCol)
                        val name = it.getString(nameCol) ?: "Foto_Papelera_$id.jpg"
                        val path = if (dataCol != -1) it.getString(dataCol) else ""
                        val size = it.getLong(sizeCol)
                        val dateModified = it.getLong(dateCol) * 1000
                        val width = if (widthCol != -1) it.getInt(widthCol) else 0
                        val height = if (heightCol != -1) it.getInt(heightCol) else 0

                        val contentUri = Uri.withAppendedPath(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id.toString()
                        )

                        val ext = name.substringAfterLast(".", "jpg").lowercase(Locale.ROOT)
                        val dimStr = if (width > 0 && height > 0) "${width}x${height}" else null

                        val finalPath = path ?: contentUri.toString()
                        if (!seenPaths.contains(finalPath)) {
                            seenPaths.add(finalPath)
                            foundPhotos.add(
                                RecoverablePhoto(
                                    id = "trash_img_$id",
                                    name = name,
                                    filePath = finalPath,
                                    contentUri = contentUri,
                                    fileSizeBytes = if (size > 0) size else 512 * 1024L,
                                    lastModifiedTimestamp = if (dateModified > 0) dateModified else System.currentTimeMillis(),
                                    sourceCategory = RecoverySource.TRASH_MEDIASTORE,
                                    fileExtension = ext,
                                    dimensions = dimStr,
                                    mediaType = MediaType.IMAGE,
                                    health = FileHealth(100, HealthLevel.EXCELLENT, "Archivo original en papelera (100% íntegro)")
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("TrashMediaScanner", "Error querying trashed images: ${e.message}")
            }
        }
    }

    fun scanMediaStoreVideoTrash(
        context: Context,
        foundPhotos: MutableList<RecoverablePhoto>,
        seenPaths: MutableSet<String>
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val projection = arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.DATA,
                    MediaStore.Video.Media.SIZE,
                    MediaStore.Video.Media.DATE_MODIFIED,
                    MediaStore.Video.Media.WIDTH,
                    MediaStore.Video.Media.HEIGHT,
                    MediaStore.Video.Media.DURATION
                )

                val bundle = android.os.Bundle().apply {
                    putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
                }

                val cursor = context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    bundle,
                    null
                )

                cursor?.use {
                    val idCol = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val nameCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val dataCol = it.getColumnIndex(MediaStore.Video.Media.DATA)
                    val sizeCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                    val dateCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                    val widthCol = it.getColumnIndex(MediaStore.Video.Media.WIDTH)
                    val heightCol = it.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                    val durationCol = it.getColumnIndex(MediaStore.Video.Media.DURATION)

                    while (it.moveToNext()) {
                        val id = it.getLong(idCol)
                        val name = it.getString(nameCol) ?: "Video_Papelera_$id.mp4"
                        val path = if (dataCol != -1) it.getString(dataCol) else ""
                        val size = it.getLong(sizeCol)
                        val dateModified = it.getLong(dateCol) * 1000
                        val width = if (widthCol != -1) it.getInt(widthCol) else 0
                        val height = if (heightCol != -1) it.getInt(heightCol) else 0
                        val duration = if (durationCol != -1) it.getLong(durationCol) else 0L

                        val contentUri = Uri.withAppendedPath(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            id.toString()
                        )

                        val ext = name.substringAfterLast(".", "mp4").lowercase(Locale.ROOT)
                        val dimStr = if (width > 0 && height > 0) "${width}x${height}" else null

                        val finalPath = path ?: contentUri.toString()
                        if (!seenPaths.contains(finalPath)) {
                            seenPaths.add(finalPath)
                            foundPhotos.add(
                                RecoverablePhoto(
                                    id = "trash_vid_$id",
                                    name = name,
                                    filePath = finalPath,
                                    contentUri = contentUri,
                                    fileSizeBytes = if (size > 0) size else 2 * 1024 * 1024L,
                                    lastModifiedTimestamp = if (dateModified > 0) dateModified else System.currentTimeMillis(),
                                    sourceCategory = RecoverySource.TRASH_MEDIASTORE,
                                    fileExtension = ext,
                                    dimensions = dimStr,
                                    mediaType = MediaType.VIDEO,
                                    durationMs = duration,
                                    health = FileHealth(100, HealthLevel.EXCELLENT, "Video original en papelera (100% íntegro)")
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("TrashMediaScanner", "Error querying trashed videos: ${e.message}")
            }
        }
    }

    fun scanMediaStoreAudioTrash(
        context: Context,
        foundPhotos: MutableList<RecoverablePhoto>,
        seenPaths: MutableSet<String>
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.SIZE,
                    MediaStore.Audio.Media.DATE_MODIFIED,
                    MediaStore.Audio.Media.DURATION
                )

                val bundle = android.os.Bundle().apply {
                    putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
                }

                val cursor = context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    bundle,
                    null
                )

                cursor?.use {
                    val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val nameCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                    val dataCol = it.getColumnIndex(MediaStore.Audio.Media.DATA)
                    val sizeCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                    val dateCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                    val durationCol = it.getColumnIndex(MediaStore.Audio.Media.DURATION)

                    while (it.moveToNext()) {
                        val id = it.getLong(idCol)
                        val name = it.getString(nameCol) ?: "Audio_Papelera_$id.mp3"
                        val path = if (dataCol != -1) it.getString(dataCol) else ""
                        val size = it.getLong(sizeCol)
                        val dateModified = it.getLong(dateCol) * 1000
                        val duration = if (durationCol != -1) it.getLong(durationCol) else 0L

                        val contentUri = Uri.withAppendedPath(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id.toString()
                        )

                        val ext = name.substringAfterLast(".", "mp3").lowercase(Locale.ROOT)
                        val finalPath = path ?: contentUri.toString()
                        if (!seenPaths.contains(finalPath)) {
                            seenPaths.add(finalPath)
                            foundPhotos.add(
                                RecoverablePhoto(
                                    id = "trash_aud_$id",
                                    name = name,
                                    filePath = finalPath,
                                    contentUri = contentUri,
                                    fileSizeBytes = if (size > 0) size else 500 * 1024L,
                                    lastModifiedTimestamp = if (dateModified > 0) dateModified else System.currentTimeMillis(),
                                    sourceCategory = RecoverySource.TRASH_MEDIASTORE,
                                    fileExtension = ext,
                                    mediaType = MediaType.AUDIO,
                                    durationMs = duration,
                                    health = FileHealth(100, HealthLevel.EXCELLENT, "Audio original en papelera (100% íntegro)")
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("TrashMediaScanner", "Error querying trashed audios: ${e.message}")
            }
        }
    }
}
