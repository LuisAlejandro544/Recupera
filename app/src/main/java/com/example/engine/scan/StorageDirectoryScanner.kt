package com.example.engine.scan

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.engine.filter.ActiveGalleryFilter
import com.example.engine.health.FileHealthEvaluator
import com.example.model.MediaType
import com.example.model.RecoverablePhoto
import com.example.model.RecoverySource
import java.io.File
import java.util.Locale
import java.util.UUID

object StorageDirectoryScanner {

    fun scanThumbnailDirectories(
        context: Context,
        foundPhotos: MutableList<RecoverablePhoto>,
        seenPaths: MutableSet<String>,
        activePaths: Set<String>,
        activeFingerprints: Set<String>,
        onFileScanned: (String) -> Unit
    ) {
        val targetDirs = listOf(
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), ".thumbnails"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), ".thumbnails"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), ".thumbnails"),
            File(context.cacheDir, "thumbnails"),
            File(context.externalCacheDir, "thumbnails")
        )

        for (dir in targetDirs) {
            if (dir.exists() && dir.isDirectory) {
                dir.walkTopDown().maxDepth(3).forEach { file ->
                    onFileScanned(file.absolutePath)
                    if (file.isFile && FileHealthEvaluator.isValidMediaFile(file)) {
                        addMediaIfNew(file, RecoverySource.THUMBNAILS_CACHE, foundPhotos, seenPaths, activePaths, activeFingerprints)
                    }
                }
            }
        }
    }

    fun scanHiddenAndAppCaches(
        context: Context,
        foundPhotos: MutableList<RecoverablePhoto>,
        seenPaths: MutableSet<String>,
        activePaths: Set<String>,
        activeFingerprints: Set<String>,
        onFileScanned: (String) -> Unit
    ) {
        val dirsToScan = listOf(
            File(Environment.getExternalStorageDirectory(), "WhatsApp/Media/.Statuses"),
            File(Environment.getExternalStorageDirectory(), "WhatsApp/Media/WhatsApp Video"),
            File(Environment.getExternalStorageDirectory(), "WhatsApp/Media/WhatsApp Video/.trash"),
            File(Environment.getExternalStorageDirectory(), "WhatsApp/Media/WhatsApp Audio"),
            File(Environment.getExternalStorageDirectory(), "WhatsApp/Media/WhatsApp Audio/.trash"),
            File(Environment.getExternalStorageDirectory(), "WhatsApp/Media/WhatsApp Voice Notes"),
            File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
            File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video"),
            File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Audio"),
            File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Voice Notes"),
            File(Environment.getExternalStorageDirectory(), "Telegram/Telegram Images"),
            File(Environment.getExternalStorageDirectory(), "Telegram/Telegram Video"),
            File(Environment.getExternalStorageDirectory(), "Telegram/Telegram Audio"),
            File(Environment.getExternalStorageDirectory(), "Recordings"),
            File(Environment.getExternalStorageDirectory(), "Music"),
            File(Environment.getExternalStorageDirectory(), "Voice Recorder"),
            File(context.cacheDir, "image_cache"),
            File(context.cacheDir, "video_cache"),
            File(context.cacheDir, "audio_cache"),
            File(context.cacheDir, "coil"),
            context.externalCacheDir
        )

        for (dir in dirsToScan) {
            if (dir != null && dir.exists() && dir.isDirectory) {
                dir.walkTopDown().maxDepth(3).forEach { file ->
                    onFileScanned(file.absolutePath)
                    if (file.isFile && FileHealthEvaluator.isValidMediaFile(file)) {
                        val source = if (file.absolutePath.contains(".nomedia") || file.name.startsWith(".")) {
                            RecoverySource.HIDDEN_VAULT
                        } else {
                            RecoverySource.APP_TEMP_CACHE
                        }
                        addMediaIfNew(file, source, foundPhotos, seenPaths, activePaths, activeFingerprints)
                    }
                }
            }
        }
    }

    fun deepScanDirectory(
        directory: File,
        foundPhotos: MutableList<RecoverablePhoto>,
        seenPaths: MutableSet<String>,
        activePaths: Set<String>,
        activeFingerprints: Set<String>,
        maxDepth: Int,
        onFileScanned: (String) -> Unit
    ) {
        try {
            directory.walkTopDown()
                .maxDepth(maxDepth)
                .onEnter { file ->
                    // Skip restricted internal dirs to avoid slow permission faults
                    !file.absolutePath.contains("/Android/data") && !file.absolutePath.contains("/Android/obb")
                }
                .forEach { file ->
                    onFileScanned(file.absolutePath)
                    if (file.isFile && FileHealthEvaluator.isValidMediaFile(file)) {
                        val source = when {
                            file.absolutePath.contains(".thumbnails") -> RecoverySource.THUMBNAILS_CACHE
                            file.absolutePath.contains(".nomedia") || file.name.startsWith(".") -> RecoverySource.HIDDEN_VAULT
                            file.absolutePath.contains("cache", ignoreCase = true) -> RecoverySource.APP_TEMP_CACHE
                            else -> RecoverySource.DEEP_STORAGE
                        }
                        addMediaIfNew(file, source, foundPhotos, seenPaths, activePaths, activeFingerprints)
                    }
                }
        } catch (e: Exception) {
            Log.w("StorageDirScanner", "Deep scan error in ${directory.path}: ${e.message}")
        }
    }

    private fun addMediaIfNew(
        file: File,
        source: RecoverySource,
        foundPhotos: MutableList<RecoverablePhoto>,
        seenPaths: MutableSet<String>,
        activePaths: Set<String>,
        activeFingerprints: Set<String>
    ) {
        val path = file.absolutePath
        if (seenPaths.contains(path)) return

        // Filter out active gallery photos/videos/audios so only deleted/residual items are recovered
        if (ActiveGalleryFilter.isFileInActiveGallery(file, source, activePaths, activeFingerprints)) {
            return
        }

        seenPaths.add(path)

        val ext = file.extension.ifBlank {
            if (FileHealthEvaluator.checkMediaMagicBytes(file)) "jpg" else "bin"
        }.lowercase(Locale.ROOT)

        val isVideo = ext in FileHealthEvaluator.supportedVideoExtensions
        val isAudio = ext in FileHealthEvaluator.supportedAudioExtensions

        var width = 0
        var height = 0
        var durationMs = 0L

        if (isVideo) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(path)
                val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                retriever.release()

                if (rotation == 90 || rotation == 270) {
                    width = h
                    height = w
                } else {
                    width = w
                    height = h
                }
                durationMs = dur
            } catch (_: Exception) {}
        } else if (isAudio) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(path)
                val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                retriever.release()
                durationMs = dur
            } catch (_: Exception) {}
        } else {
            try {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, options)
                width = options.outWidth
                height = options.outHeight
            } catch (_: Exception) {}
        }

        val dimStr = if (width > 0 && height > 0) "${width}x${height}" else null
        val health = FileHealthEvaluator.calculateFileHealth(file, isVideo, isAudio, durationMs, width, height, source)

        val mediaType = when {
            isVideo -> MediaType.VIDEO
            isAudio -> MediaType.AUDIO
            else -> MediaType.IMAGE
        }

        foundPhotos.add(
            RecoverablePhoto(
                id = UUID.nameUUIDFromBytes(path.toByteArray()).toString(),
                name = file.name,
                filePath = path,
                contentUri = Uri.fromFile(file),
                fileSizeBytes = file.length(),
                lastModifiedTimestamp = file.lastModified(),
                sourceCategory = source,
                fileExtension = ext,
                dimensions = dimStr,
                isSample = false,
                mediaType = mediaType,
                durationMs = durationMs,
                health = health
            )
        )
    }
}
