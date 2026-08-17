package com.example.engine.restore

import com.example.model.RecoverablePhoto
import java.util.Locale

object MimeTypeResolver {

    /**
     * Resolves the MIME type for images, videos, and audio files.
     */
    fun resolveMimeType(photo: RecoverablePhoto): String {
        val ext = photo.fileExtension.lowercase(Locale.ROOT)
        return when {
            photo.isVideo -> {
                when (ext) {
                    "mkv" -> "video/x-matroska"
                    "mov" -> "video/quicktime"
                    "3gp" -> "video/3gpp"
                    "webm" -> "video/webm"
                    "avi" -> "video/x-msvideo"
                    else -> "video/mp4"
                }
            }
            photo.isAudio -> {
                when (ext) {
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
                when (ext) {
                    "png" -> "image/png"
                    "webp" -> "image/webp"
                    "heic" -> "image/heic"
                    "gif" -> "image/gif"
                    else -> "image/jpeg"
                }
            }
        }
    }

    /**
     * Resolves general MIME wildcard for MediaScanner notification.
     */
    fun resolveScannerMimeType(photo: RecoverablePhoto): String {
        return when {
            photo.isVideo -> "video/*"
            photo.isAudio -> "audio/*"
            else -> "image/*"
        }
    }
}
