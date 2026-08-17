package com.example.engine.scan

import android.content.Context
import android.os.Environment
import com.example.engine.health.FileHealthEvaluator
import java.io.File

object ThumbnailCacheScanner {

    /**
     * Scans DCIM, Pictures, Movies and app cache .thumbnails directories.
     */
    fun scanThumbnailDirectories(
        context: Context,
        onFileFound: (File) -> Unit,
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
                try {
                    dir.walkTopDown().maxDepth(3).forEach { file ->
                        onFileScanned(file.absolutePath)
                        if (file.isFile && FileHealthEvaluator.isValidMediaFile(file)) {
                            onFileFound(file)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }
}
