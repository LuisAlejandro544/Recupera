package com.example.engine.scan

import android.content.Context
import android.util.Log
import com.example.model.FileHealth
import com.example.model.HealthLevel
import com.example.model.MediaType
import com.example.model.RecoverablePhoto
import com.example.model.RecoverySource
import com.example.shizuku.ShizukuManager
import com.example.shizuku.service.ShizukuServiceClient
import java.io.File
import java.util.Locale

object ShizukuVendorTrashScanner {

    private const val TAG = "ShizukuTrashScanner"

    suspend fun scanProtectedVaults(
        context: Context,
        foundPhotos: MutableList<RecoverablePhoto>,
        seenPaths: MutableSet<String>,
        activePaths: Set<String>,
        activeFingerprints: Set<String>,
        onItemScanned: () -> Unit
    ) {
        if (!ShizukuManager.shizukuState.value.isReadyForEnhancedScan) {
            Log.d(TAG, "Shizuku enhanced scan is disabled or not authorized. Skipping.")
            return
        }

        try {
            val scannedItems = ShizukuServiceClient.scanProtectedDirectories(context)
            Log.i(TAG, "Retrieved ${scannedItems.size} items from Shizuku scan service")

            for (item in scannedItems) {
                onItemScanned()
                if (seenPaths.contains(item.absolutePath)) continue

                // Check active gallery filter
                if (activePaths.contains(item.absolutePath)) continue
                val fingerprint = "${item.fileName}|${item.sizeBytes}"
                if (activeFingerprints.contains(fingerprint)) continue

                seenPaths.add(item.absolutePath)

                val ext = item.fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
                val mediaType = resolveMediaType(item.mimeType, ext)
                val health = calculateHealth(item.isFromVendorTrash, item.sizeBytes, ext)

                val photo = RecoverablePhoto(
                    id = "shizuku_${item.absolutePath.hashCode()}",
                    name = item.fileName,
                    filePath = item.absolutePath,
                    contentUri = null,
                    fileSizeBytes = item.sizeBytes,
                    lastModifiedTimestamp = item.lastModified,
                    sourceCategory = RecoverySource.SHIZUKU_SYSTEM,
                    fileExtension = ext,
                    mediaType = mediaType,
                    health = health
                )
                foundPhotos.add(photo)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error in Shizuku protected vault scan: ${e.message}", e)
        }
    }

    private fun resolveMediaType(mimeType: String, extension: String): MediaType {
        return when {
            mimeType.startsWith("image/") || extension in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "dng") -> MediaType.IMAGE
            mimeType.startsWith("video/") || extension in listOf("mp4", "mkv", "3gp", "mov", "webm", "avi", "ts") -> MediaType.VIDEO
            mimeType.startsWith("audio/") || extension in listOf("mp3", "m4a", "wav", "aac", "ogg", "opus", "flac", "amr") -> MediaType.AUDIO
            else -> MediaType.DOCUMENT
        }
    }

    private fun calculateHealth(isFromVendorTrash: Boolean, sizeBytes: Long, extension: String): FileHealth {
        return when {
            isFromVendorTrash && sizeBytes > 50_000 -> FileHealth(100, HealthLevel.EXCELLENT, "Papelera de fabricante intacta")
            sizeBytes > 100_000 -> FileHealth(95, HealthLevel.EXCELLENT, "Archivo de sistema íntegro")
            sizeBytes > 10_000 -> FileHealth(85, HealthLevel.GOOD, "Archivo protegido en buen estado")
            else -> FileHealth(65, HealthLevel.FAIR, "Copia residual o miniatura de app")
        }
    }
}
