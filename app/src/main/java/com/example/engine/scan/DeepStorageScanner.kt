package com.example.engine.scan

import android.util.Log
import com.example.engine.health.FileHealthEvaluator
import com.example.model.RecoverySource
import java.io.File

object DeepStorageScanner {

    /**
     * Traverses storage recursively to locate orphaned or deleted media files.
     */
    fun deepScanDirectory(
        directory: File,
        maxDepth: Int,
        onFileFound: (File, RecoverySource) -> Unit,
        onFileScanned: (String) -> Unit
    ) {
        try {
            directory.walkTopDown()
                .maxDepth(maxDepth)
                .onEnter { file ->
                    // Skip restricted internal dirs to prevent slow permission faults or sandbox crashes
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
                        onFileFound(file, source)
                    }
                }
        } catch (e: Exception) {
            Log.w("DeepStorageScanner", "Deep scan error in ${directory.path}: ${e.message}")
        }
    }
}
