package com.example.engine.duplicate

import android.content.Context
import android.util.Log
import com.example.model.DuplicateGroup
import com.example.model.DuplicateScanResult
import com.example.model.OrphanCleanResult
import com.example.model.RecoverablePhoto
import com.example.model.RecoverySource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.UUID

object DuplicateMediaDetector {

    private const val TAG = "DuplicateMediaDetector"

    /**
     * Identifies duplicate residual files from the scanned recoverable photos list.
     * Uses a multi-tier comparison (File Size -> Header/Footer Fast Hash) to ensure high speed
     * and zero memory overhead.
     */
    suspend fun findDuplicates(photos: List<RecoverablePhoto>): DuplicateScanResult = withContext(Dispatchers.Default) {
        if (photos.size < 2) {
            return@withContext DuplicateScanResult()
        }

        // Tier 1: Group by file size and media type (fastest filter)
        val sizeBuckets = photos
            .filter { it.fileSizeBytes > 0L && it.filePath.isNotBlank() && File(it.filePath).exists() }
            .groupBy { "${it.mediaType}_${it.fileSizeBytes}" }
            .filter { it.value.size >= 2 }

        val duplicateGroups = mutableListOf<DuplicateGroup>()
        var totalDuplicatesCount = 0
        var totalWastedBytes = 0L

        // Tier 2: Calculate Fast Hash (First 16KB + Last 4KB) for size buckets
        for ((_, bucket) in sizeBuckets) {
            val hashGroups = bucket.groupBy { photo ->
                computeFastFileHash(File(photo.filePath))
            }.filter { it.value.size >= 2 && it.key.isNotBlank() }

            for ((_, duplicateCandidates) in hashGroups) {
                // Determine the primary / best quality candidate
                val sortedCandidates = duplicateCandidates.sortedWith(
                    compareByDescending<RecoverablePhoto> { it.health.percentage }
                        .thenByDescending { getSourcePriority(it.sourceCategory) }
                        .thenByDescending { it.dimensions ?: "" }
                        .thenBy { it.filePath.contains(".thumbnail", ignoreCase = true) }
                )

                val primary = sortedCandidates.first()
                val duplicates = sortedCandidates.drop(1)
                val wastedBytes = duplicates.sumOf { it.fileSizeBytes }

                totalDuplicatesCount += duplicates.size
                totalWastedBytes += wastedBytes

                duplicateGroups.add(
                    DuplicateGroup(
                        id = UUID.randomUUID().toString(),
                        primaryPhoto = primary,
                        duplicates = duplicates,
                        totalWastedBytes = wastedBytes
                    )
                )
            }
        }

        DuplicateScanResult(
            groups = duplicateGroups.sortedByDescending { it.totalWastedBytes },
            totalDuplicatesCount = totalDuplicatesCount,
            totalWastedBytes = totalWastedBytes
        )
    }

    /**
     * Purges selected duplicate files from disk to reclaim wasted storage.
     */
    suspend fun purgeDuplicates(
        context: Context,
        duplicatePhotoIds: Set<String>,
        allPhotos: List<RecoverablePhoto>
    ): OrphanCleanResult = withContext(Dispatchers.IO) {
        val targets = allPhotos.filter { duplicatePhotoIds.contains(it.id) }
        var deletedCount = 0
        var failedCount = 0
        var freedBytes = 0L

        for (photo in targets) {
            val file = File(photo.filePath)
            if (file.exists() && file.isFile) {
                val size = file.length()
                val deleted = try {
                    file.delete()
                } catch (e: Exception) {
                    Log.w(TAG, "Error eliminando duplicado ${file.path}: ${e.message}")
                    false
                }

                if (deleted) {
                    deletedCount++
                    freedBytes += size
                } else {
                    failedCount++
                }
            } else {
                failedCount++
            }
        }

        OrphanCleanResult(
            scannedCount = targets.size,
            deletedCount = deletedCount,
            freedBytes = freedBytes,
            failedCount = failedCount,
            isDryRun = false
        )
    }

    private fun getSourcePriority(source: RecoverySource): Int {
        return when (source) {
            RecoverySource.TRASH_MEDIASTORE -> 100
            RecoverySource.DEEP_STORAGE -> 80
            RecoverySource.SHIZUKU_SYSTEM -> 70
            RecoverySource.HIDDEN_VAULT -> 60
            RecoverySource.APP_TEMP_CACHE -> 40
            RecoverySource.THUMBNAILS_CACHE -> 20
        }
    }

    private fun computeFastFileHash(file: File): String {
        return try {
            val length = file.length()
            if (length <= 0) return ""

            val digest = MessageDigest.getInstance("MD5")
            val sampleSize = minOf(16384, length.toInt())
            val buffer = ByteArray(sampleSize)

            FileInputStream(file).use { stream ->
                // Read first 16 KB
                val read = stream.read(buffer, 0, sampleSize)
                if (read > 0) {
                    digest.update(buffer, 0, read)
                }

                // If file is large enough (> 32 KB), read last 4 KB
                if (length > 32768) {
                    val channel = stream.channel
                    channel.position(length - 4096)
                    val tailBuffer = ByteArray(4096)
                    val tailRead = stream.read(tailBuffer, 0, 4096)
                    if (tailRead > 0) {
                        digest.update(tailBuffer, 0, tailRead)
                    }
                }
            }

            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            ""
        }
    }
}
