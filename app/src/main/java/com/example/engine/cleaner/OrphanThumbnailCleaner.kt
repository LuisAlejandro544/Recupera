package com.example.engine.cleaner

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.model.OrphanCleanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object OrphanThumbnailCleaner {

    private fun getThumbnailTargetDirectories(context: Context): List<File> {
        val storageRoot = Environment.getExternalStorageDirectory()
        return listOfNotNull(
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), ".thumbnails"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), ".thumbnails"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), ".thumbnails"),
            File(storageRoot, "DCIM/.thumbnails"),
            File(storageRoot, "Pictures/.thumbnails"),
            File(storageRoot, "WhatsApp/Media/.thumbnails"),
            File(storageRoot, "Android/media/com.whatsapp/WhatsApp/Media/.thumbnails"),
            File(context.cacheDir, "thumbnails"),
            File(context.cacheDir, "image_cache"),
            File(context.externalCacheDir, "thumbnails"),
            File(context.externalCacheDir, "image_cache")
        )
    }

    /**
     * Scans for residual / orphan thumbnail files and estimates recoverable disk space.
     */
    suspend fun analyzeOrphanThumbnails(context: Context): OrphanCleanResult = withContext(Dispatchers.IO) {
        val targetDirs = getThumbnailTargetDirectories(context)
        var count = 0
        var totalBytes = 0L

        for (dir in targetDirs) {
            if (dir.exists() && dir.isDirectory) {
                try {
                    dir.walkTopDown().maxDepth(3).forEach { file ->
                        if (file.isFile && isThumbnailFile(file)) {
                            count++
                            totalBytes += file.length()
                        }
                    }
                } catch (e: Exception) {
                    Log.w("OrphanThumbnailCleaner", "Error analyzing dir ${dir.path}: ${e.message}")
                }
            }
        }

        OrphanCleanResult(
            scannedCount = count,
            deletedCount = 0,
            freedBytes = totalBytes,
            failedCount = 0,
            isDryRun = true
        )
    }

    /**
     * Deletes orphan and residual thumbnail cache files to free storage.
     */
    suspend fun purgeOrphanThumbnails(context: Context): OrphanCleanResult = withContext(Dispatchers.IO) {
        val targetDirs = getThumbnailTargetDirectories(context)
        var scannedCount = 0
        var deletedCount = 0
        var failedCount = 0
        var freedBytes = 0L

        for (dir in targetDirs) {
            if (dir.exists() && dir.isDirectory) {
                try {
                    dir.walkTopDown().maxDepth(3).forEach { file ->
                        if (file.isFile && isThumbnailFile(file)) {
                            scannedCount++
                            val fileLength = file.length()
                            val deleted = try {
                                file.delete()
                            } catch (_: Exception) {
                                false
                            }

                            if (deleted) {
                                deletedCount++
                                freedBytes += fileLength
                            } else {
                                failedCount++
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("OrphanThumbnailCleaner", "Error purging dir ${dir.path}: ${e.message}")
                }
            }
        }

        OrphanCleanResult(
            scannedCount = scannedCount,
            deletedCount = deletedCount,
            freedBytes = freedBytes,
            failedCount = failedCount,
            isDryRun = false
        )
    }

    private fun isThumbnailFile(file: File): Boolean {
        val name = file.name.lowercase()
        return name.endsWith(".jpg") ||
                name.endsWith(".jpeg") ||
                name.endsWith(".png") ||
                name.endsWith(".webp") ||
                name.endsWith(".thumb") ||
                name.endsWith(".dthumb") ||
                name.contains("micro") ||
                name.contains("thumb") ||
                name.startsWith(".")
    }
}
