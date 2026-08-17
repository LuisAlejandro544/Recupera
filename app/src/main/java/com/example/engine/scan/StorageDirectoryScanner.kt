package com.example.engine.scan

import android.content.Context
import android.net.Uri
import com.example.engine.extractor.MediaMetadataExtractor
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
        ThumbnailCacheScanner.scanThumbnailDirectories(
            context = context,
            onFileFound = { file ->
                addMediaIfNew(file, RecoverySource.THUMBNAILS_CACHE, foundPhotos, seenPaths, activePaths, activeFingerprints)
            },
            onFileScanned = onFileScanned
        )
    }

    fun scanHiddenAndAppCaches(
        context: Context,
        foundPhotos: MutableList<RecoverablePhoto>,
        seenPaths: MutableSet<String>,
        activePaths: Set<String>,
        activeFingerprints: Set<String>,
        onFileScanned: (String) -> Unit
    ) {
        MessagingAppScanner.scanMessagingAndAppCaches(
            context = context,
            onFileFound = { file, source ->
                addMediaIfNew(file, source, foundPhotos, seenPaths, activePaths, activeFingerprints)
            },
            onFileScanned = onFileScanned
        )
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
        DeepStorageScanner.deepScanDirectory(
            directory = directory,
            maxDepth = maxDepth,
            onFileFound = { file, source ->
                addMediaIfNew(file, source, foundPhotos, seenPaths, activePaths, activeFingerprints)
            },
            onFileScanned = onFileScanned
        )
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

        val metadata = when {
            isVideo -> MediaMetadataExtractor.extractVideoMetadata(file)
            isAudio -> MediaMetadataExtractor.extractAudioMetadata(file)
            else -> MediaMetadataExtractor.extractImageDimensions(file)
        }

        val health = FileHealthEvaluator.calculateFileHealth(
            file = file,
            isVideo = isVideo,
            isAudio = isAudio,
            durationMs = metadata.durationMs,
            width = metadata.width,
            height = metadata.height,
            source = source
        )

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
                dimensions = metadata.dimensions,
                isSample = false,
                mediaType = mediaType,
                durationMs = metadata.durationMs,
                health = health
            )
        )
    }
}
