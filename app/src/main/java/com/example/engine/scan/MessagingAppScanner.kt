package com.example.engine.scan

import android.content.Context
import android.os.Environment
import com.example.engine.health.FileHealthEvaluator
import com.example.model.RecoverySource
import java.io.File

object MessagingAppScanner {

    /**
     * Scans WhatsApp, Telegram, Recordings and app temporary caches.
     */
    fun scanMessagingAndAppCaches(
        context: Context,
        onFileFound: (File, RecoverySource) -> Unit,
        onFileScanned: (String) -> Unit
    ) {
        val root = Environment.getExternalStorageDirectory()
        val dirsToScan = listOf(
            File(root, "WhatsApp/Media/.Statuses"),
            File(root, "WhatsApp/Media/WhatsApp Video"),
            File(root, "WhatsApp/Media/WhatsApp Video/.trash"),
            File(root, "WhatsApp/Media/WhatsApp Audio"),
            File(root, "WhatsApp/Media/WhatsApp Audio/.trash"),
            File(root, "WhatsApp/Media/WhatsApp Voice Notes"),
            File(root, "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
            File(root, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video"),
            File(root, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Audio"),
            File(root, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Voice Notes"),
            File(root, "Telegram/Telegram Images"),
            File(root, "Telegram/Telegram Video"),
            File(root, "Telegram/Telegram Audio"),
            File(root, "Recordings"),
            File(root, "Voice Recorder"),
            File(context.cacheDir, "image_cache"),
            File(context.cacheDir, "video_cache"),
            File(context.cacheDir, "audio_cache"),
            File(context.cacheDir, "coil"),
            context.externalCacheDir
        )

        for (dir in dirsToScan) {
            if (dir != null && dir.exists() && dir.isDirectory) {
                try {
                    dir.walkTopDown().maxDepth(3).forEach { file ->
                        onFileScanned(file.absolutePath)
                        if (file.isFile && FileHealthEvaluator.isValidMediaFile(file)) {
                            val source = if (file.absolutePath.contains(".nomedia") || file.name.startsWith(".")) {
                                RecoverySource.HIDDEN_VAULT
                            } else {
                                RecoverySource.APP_TEMP_CACHE
                            }
                            onFileFound(file, source)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }
}
