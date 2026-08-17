package com.example.engine.extractor

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import java.io.File

data class ExtractedMediaMetadata(
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0L,
    val dimensions: String? = null
)

object MediaMetadataExtractor {

    /**
     * Extracts visual and temporal metadata from a video file.
     */
    fun extractVideoMetadata(file: File): ExtractedMediaMetadata {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val rawW = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val rawH = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            retriever.release()

            val (width, height) = if (rotation == 90 || rotation == 270) {
                Pair(rawH, rawW)
            } else {
                Pair(rawW, rawH)
            }

            val dimStr = if (width > 0 && height > 0) "${width}x${height}" else null
            ExtractedMediaMetadata(width, height, duration, dimStr)
        } catch (e: Exception) {
            Log.w("MediaMetadataExtractor", "Could not extract video metadata from ${file.name}: ${e.message}")
            ExtractedMediaMetadata()
        }
    }

    /**
     * Extracts duration metadata from an audio file.
     */
    fun extractAudioMetadata(file: File): ExtractedMediaMetadata {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            retriever.release()
            ExtractedMediaMetadata(durationMs = duration)
        } catch (e: Exception) {
            Log.w("MediaMetadataExtractor", "Could not extract audio metadata from ${file.name}: ${e.message}")
            ExtractedMediaMetadata()
        }
    }

    /**
     * Extracts resolution dimensions from an image file without loading it fully into RAM.
     */
    fun extractImageDimensions(file: File): ExtractedMediaMetadata {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            val w = options.outWidth
            val h = options.outHeight
            val dimStr = if (w > 0 && h > 0) "${w}x${h}" else null
            ExtractedMediaMetadata(width = w, height = h, dimensions = dimStr)
        } catch (e: Exception) {
            Log.w("MediaMetadataExtractor", "Could not decode image bounds from ${file.name}: ${e.message}")
            ExtractedMediaMetadata()
        }
    }
}
