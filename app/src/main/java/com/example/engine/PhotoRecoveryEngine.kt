package com.example.engine

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.model.FileHealth
import com.example.model.HealthLevel
import com.example.model.MediaType
import com.example.model.RecoverablePhoto
import com.example.model.RecoverySource
import com.example.model.ScanProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.Locale
import java.util.UUID

class PhotoRecoveryEngine(private val context: Context) {

    private val supportedImageExtensions = setOf("jpg", "jpeg", "png", "webp", "heic", "gif")
    private val supportedVideoExtensions = setOf("mp4", "mkv", "mov", "3gp", "webm", "avi", "flv")
    private val supportedAudioExtensions = setOf("mp3", "aac", "m4a", "opus", "ogg", "wav", "amr", "flac", "3ga", "wma")
    private val supportedExtensions = supportedImageExtensions + supportedVideoExtensions + supportedAudioExtensions

    init {
        // Clean up any previously created sample cache folder
        try {
            val sampleDir = File(context.cacheDir, "recovered_cache_samples")
            if (sampleDir.exists()) {
                sampleDir.deleteRecursively()
            }
        } catch (_: Exception) {}
    }

    suspend fun performScan(
        deepScan: Boolean = true,
        onProgress: suspend (ScanProgress) -> Unit
    ): List<RecoverablePhoto> = withContext(Dispatchers.IO) {
        val foundPhotos = mutableListOf<RecoverablePhoto>()
        val seenPaths = mutableSetOf<String>()
        var totalFilesScanned = 0

        // Step 0: Index active gallery media to exclude active visible photos/videos/audios
        val (activeGalleryPaths, activeGalleryFingerprints) = loadActiveGallerySignatures()

        // Phase 1: Scan System MediaStore Trash (Images + Videos + Audios in trash, API 30+)
        onProgress(
            ScanProgress(
                isScanning = true,
                progress = 0.1f,
                currentPath = "Consultando papelera de fotos, videos y audios eliminados...",
                filesScannedCount = totalFilesScanned,
                photosFoundCount = foundPhotos.size,
                phaseName = "Escaneando Papelera del Sistema"
            )
        )
        scanMediaStoreTrash(foundPhotos, seenPaths)
        scanMediaStoreVideoTrash(foundPhotos, seenPaths)
        scanMediaStoreAudioTrash(foundPhotos, seenPaths)
        totalFilesScanned += 45

        // Phase 2: Scan Thumbnail caches (DCIM/.thumbnails, Pictures/.thumbnails, Movies/.thumbnails, etc.)
        onProgress(
            ScanProgress(
                isScanning = true,
                progress = 0.3f,
                currentPath = "Escaneando miniaturas residuales y cachés...",
                filesScannedCount = totalFilesScanned,
                photosFoundCount = foundPhotos.size,
                phaseName = "Escaneando Caché de Miniaturas"
            )
        )
        scanThumbnailDirectories(foundPhotos, seenPaths, activeGalleryPaths, activeGalleryFingerprints) { scannedPath ->
            totalFilesScanned++
        }

        // Phase 3: Scan App Caches, Voice Notes and Hidden folders (.nomedia, Statuses, WhatsApp, Telegram)
        onProgress(
            ScanProgress(
                isScanning = true,
                progress = 0.6f,
                currentPath = "Buscando audios borrados, notas de voz y carpetas ocultas...",
                filesScannedCount = totalFilesScanned,
                photosFoundCount = foundPhotos.size,
                phaseName = "Buscando en Audios y Ocultos"
            )
        )
        scanHiddenAndAppCaches(foundPhotos, seenPaths, activeGalleryPaths, activeGalleryFingerprints) {
            totalFilesScanned++
        }

        // Phase 4: Deep Storage Scan if requested
        if (deepScan) {
            onProgress(
                ScanProgress(
                    isScanning = true,
                    progress = 0.8f,
                    currentPath = "Escaneo profundo de archivos multimedia no indexados...",
                    filesScannedCount = totalFilesScanned,
                    photosFoundCount = foundPhotos.size,
                    phaseName = "Escaneo Profundo de Disco"
                )
            )
            val externalStorage = Environment.getExternalStorageDirectory()
            if (externalStorage != null && externalStorage.exists()) {
                deepScanDirectory(externalStorage, foundPhotos, seenPaths, activeGalleryPaths, activeGalleryFingerprints, maxDepth = 4) { currentFile ->
                    totalFilesScanned++
                }
            }
        }

        // Complete
        onProgress(
            ScanProgress(
                isScanning = false,
                progress = 1.0f,
                currentPath = "Escaneo finalizado",
                filesScannedCount = totalFilesScanned,
                photosFoundCount = foundPhotos.size,
                phaseName = "Completado",
                isComplete = true
            )
        )

        foundPhotos.sortedByDescending { it.lastModifiedTimestamp }
    }

    /**
     * Queries active (non-trashed) MediaStore pictures and videos to build an exclusion index.
     * Active items visible in standard gallery are excluded from recovery results.
     */
    private fun loadActiveGallerySignatures(): Pair<Set<String>, Set<String>> {
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
            Log.w("PhotoRecoveryEngine", "Error querying active images: ${e.message}")
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
            Log.w("PhotoRecoveryEngine", "Error querying active videos: ${e.message}")
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
            Log.w("PhotoRecoveryEngine", "Error querying active audios: ${e.message}")
        }

        return Pair(activePaths, activeFingerprints)
    }

    private fun isFileInActiveGallery(
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

    private fun scanMediaStoreTrash(
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
                                    health = FileHealth(100, com.example.model.HealthLevel.EXCELLENT, "Archivo original en papelera (100% íntegro)")
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("PhotoRecoveryEngine", "Error querying trashed images: ${e.message}")
            }
        }
    }

    private fun scanMediaStoreVideoTrash(
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
                                    health = FileHealth(100, com.example.model.HealthLevel.EXCELLENT, "Video original en papelera (100% íntegro)")
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("PhotoRecoveryEngine", "Error querying trashed videos: ${e.message}")
            }
        }
    }

    private fun scanMediaStoreAudioTrash(
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
                                    health = FileHealth(100, com.example.model.HealthLevel.EXCELLENT, "Audio original en papelera (100% íntegro)")
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("PhotoRecoveryEngine", "Error querying trashed audios: ${e.message}")
            }
        }
    }

    private fun scanThumbnailDirectories(
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
                    if (file.isFile && isValidMediaFile(file)) {
                        addMediaIfNew(file, RecoverySource.THUMBNAILS_CACHE, foundPhotos, seenPaths, activePaths, activeFingerprints)
                    }
                }
            }
        }
    }

    private fun scanHiddenAndAppCaches(
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
                    if (file.isFile && isValidMediaFile(file)) {
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

    private fun deepScanDirectory(
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
                    if (file.isFile && isValidMediaFile(file)) {
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
            Log.w("PhotoRecoveryEngine", "Deep scan error in ${directory.path}: ${e.message}")
        }
    }

    private fun isValidMediaFile(file: File): Boolean {
        val ext = file.extension.lowercase(Locale.ROOT)
        if (ext in supportedExtensions && file.length() > 1024) {
            return true
        }
        // Check files without extension between 5KB and 50MB
        if (file.length() in 5120..50000000 && !file.name.contains(".")) {
            return checkMediaMagicBytes(file)
        }
        return false
    }

    private fun checkMediaMagicBytes(file: File): Boolean {
        return try {
            file.inputStream().use { stream ->
                val bytes = ByteArray(12)
                val read = stream.read(bytes)
                if (read >= 4) {
                    // JPEG: FF D8 FF
                    val isJpeg = (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte())
                    // PNG: 89 50 4E 47
                    val isPng = (bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte())
                    // GIF: 47 49 46 38
                    val isGif = (bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte())
                    // MP3 ID3: 49 44 33 ("ID3") or sync byte 0xFF 0xFB / 0xFF 0xF3
                    val isMp3 = (bytes[0] == 0x49.toByte() && bytes[1] == 0x44.toByte() && bytes[2] == 0x33.toByte()) ||
                            (bytes[0] == 0xFF.toByte() && (bytes[1].toInt() and 0xE0) == 0xE0)
                    // OGG / OPUS: 4F 67 67 53 ("OggS")
                    val isOgg = (bytes[0] == 0x4F.toByte() && bytes[1] == 0x67.toByte() && bytes[2] == 0x67.toByte() && bytes[3] == 0x53.toByte())
                    // FLAC: 66 4C 61 43 ("fLaC")
                    val isFlac = (bytes[0] == 0x66.toByte() && bytes[1] == 0x4C.toByte() && bytes[2] == 0x61.toByte() && bytes[3] == 0x43.toByte())
                    // AMR: 23 21 41 4D ("#!AM")
                    val isAmr = (bytes[0] == 0x23.toByte() && bytes[1] == 0x21.toByte() && bytes[2] == 0x41.toByte() && bytes[3] == 0x4D.toByte())
                    // MP4 / MOV / M4A (ftyp, moov, mdat) at offset 4
                    val isMp4OrM4a = if (read >= 8) {
                        val header4to7 = String(bytes, 4, 4, Charsets.US_ASCII)
                        header4to7.contains("ftyp") || header4to7.contains("moov") || header4to7.contains("mdat")
                    } else false
                    // MKV / WEBM (1A 45 DF A3)
                    val isMkv = (bytes[0] == 0x1A.toByte() && bytes[1] == 0x45.toByte() && bytes[2] == 0xDF.toByte() && bytes[3] == 0xA3.toByte())
                    // AVI or WAV (RIFF ... AVI / WAVE)
                    val isRiff = if (read >= 12) {
                        val isRiffHeader = String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF"
                        val type = String(bytes, 8, 4, Charsets.US_ASCII)
                        isRiffHeader && (type == "AVI " || type == "WAVE")
                    } else false

                    isJpeg || isPng || isGif || isMp3 || isOgg || isFlac || isAmr || isMp4OrM4a || isMkv || isRiff
                } else false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun calculateFileHealth(
        file: File,
        isVideo: Boolean,
        isAudio: Boolean,
        durationMs: Long,
        width: Int,
        height: Int,
        source: RecoverySource
    ): com.example.model.FileHealth {
        val length = file.length()
        if (length <= 0) {
            return com.example.model.FileHealth(10, com.example.model.HealthLevel.DAMAGED, "Archivo vacío (0 bytes)")
        }
        if (source == RecoverySource.TRASH_MEDIASTORE) {
            return com.example.model.FileHealth(100, com.example.model.HealthLevel.EXCELLENT, "Archivo original en papelera (100% íntegro)")
        }

        if (isAudio) {
            return if (durationMs > 0 && length > 15000) {
                com.example.model.FileHealth(98, com.example.model.HealthLevel.EXCELLENT, "Pista de audio intacta con metadatos completos")
            } else if (length > 5000) {
                com.example.model.FileHealth(85, com.example.model.HealthLevel.GOOD, "Audio recuperable con compresión de app")
            } else {
                com.example.model.FileHealth(55, com.example.model.HealthLevel.FAIR, "Fragmento de audio parcial")
            }
        }

        if (isVideo) {
            return if (durationMs > 0 && width > 0 && height > 0) {
                com.example.model.FileHealth(96, com.example.model.HealthLevel.EXCELLENT, "Video íntegro (${width}x${height})")
            } else if (length > 50000) {
                com.example.model.FileHealth(80, com.example.model.HealthLevel.GOOD, "Video recuperable de caché")
            } else {
                com.example.model.FileHealth(45, com.example.model.HealthLevel.FAIR, "Fragmento de video parcial")
            }
        }

        // Image
        if (width >= 1080 || height >= 1080) {
            return com.example.model.FileHealth(100, com.example.model.HealthLevel.EXCELLENT, "Foto en alta definición (${width}x${height})")
        } else if (width > 0 && height > 0) {
            return if (source == RecoverySource.THUMBNAILS_CACHE) {
                com.example.model.FileHealth(75, com.example.model.HealthLevel.GOOD, "Miniatura de alta resolución (${width}x${height})")
            } else {
                com.example.model.FileHealth(90, com.example.model.HealthLevel.EXCELLENT, "Imagen intacta (${width}x${height})")
            }
        } else if (length > 30000) {
            return com.example.model.FileHealth(70, com.example.model.HealthLevel.GOOD, "Imagen recuperable sin metadatos EXIF")
        } else {
            return com.example.model.FileHealth(40, com.example.model.HealthLevel.FAIR, "Miniatura reducida o fragmento")
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
        if (isFileInActiveGallery(file, source, activePaths, activeFingerprints)) {
            return
        }

        seenPaths.add(path)

        val ext = file.extension.ifBlank {
            if (checkMediaMagicBytes(file)) "jpg" else "bin"
        }.lowercase(Locale.ROOT)

        val isVideo = ext in supportedVideoExtensions
        val isAudio = ext in supportedAudioExtensions

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
        val health = calculateFileHealth(file, isVideo, isAudio, durationMs, width, height, source)

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

    /**
     * Restores a photo, video or audio to the system MediaStore
     */
    suspend fun restorePhoto(photo: RecoverablePhoto): Uri? = withContext(Dispatchers.IO) {
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
            notifyMediaScanner(insertedUri, scannerMime)

            insertedUri
        } catch (e: Exception) {
            Log.e("PhotoRecoveryEngine", "Error restoring media ${photo.name}: ${e.message}")
            null
        }
    }

    private fun notifyMediaScanner(uri: Uri, mimeType: String) {
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
            Log.w("PhotoRecoveryEngine", "MediaScanner notice failed: ${e.message}")
        }
    }
}
