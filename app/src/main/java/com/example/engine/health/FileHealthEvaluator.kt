package com.example.engine.health

import com.example.model.FileHealth
import com.example.model.HealthLevel
import com.example.model.RecoverySource
import java.io.File
import java.util.Locale

object FileHealthEvaluator {

    val supportedImageExtensions = setOf("jpg", "jpeg", "png", "webp", "heic", "gif")
    val supportedVideoExtensions = setOf("mp4", "mkv", "mov", "3gp", "webm", "avi", "flv")
    val supportedAudioExtensions = setOf("mp3", "aac", "m4a", "opus", "ogg", "wav", "amr", "flac", "3ga", "wma")
    val supportedExtensions = supportedImageExtensions + supportedVideoExtensions + supportedAudioExtensions

    fun isValidMediaFile(file: File): Boolean {
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

    fun checkMediaMagicBytes(file: File): Boolean {
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
        } catch (_: Exception) {
            false
        }
    }

    fun calculateFileHealth(
        file: File,
        isVideo: Boolean,
        isAudio: Boolean,
        durationMs: Long,
        width: Int,
        height: Int,
        source: RecoverySource
    ): FileHealth {
        val length = file.length()
        if (length <= 0) {
            return FileHealth(10, HealthLevel.DAMAGED, "Archivo vacío (0 bytes)")
        }
        if (source == RecoverySource.TRASH_MEDIASTORE) {
            return FileHealth(100, HealthLevel.EXCELLENT, "Archivo original en papelera (100% íntegro)")
        }

        if (isAudio) {
            return if (durationMs > 0 && length > 15000) {
                FileHealth(98, HealthLevel.EXCELLENT, "Pista de audio intacta con metadatos completos")
            } else if (length > 5000) {
                FileHealth(85, HealthLevel.GOOD, "Audio recuperable con compresión de app")
            } else {
                FileHealth(55, HealthLevel.FAIR, "Fragmento de audio parcial")
            }
        }

        if (isVideo) {
            return if (durationMs > 0 && width > 0 && height > 0) {
                FileHealth(96, HealthLevel.EXCELLENT, "Video íntegro (${width}x${height})")
            } else if (length > 50000) {
                FileHealth(80, HealthLevel.GOOD, "Video recuperable de caché")
            } else {
                FileHealth(45, HealthLevel.FAIR, "Fragmento de video parcial")
            }
        }

        // Image
        if (width >= 1080 || height >= 1080) {
            return FileHealth(100, HealthLevel.EXCELLENT, "Foto en alta definición (${width}x${height})")
        } else if (width > 0 && height > 0) {
            return if (source == RecoverySource.THUMBNAILS_CACHE) {
                FileHealth(75, HealthLevel.GOOD, "Miniatura de alta resolución (${width}x${height})")
            } else {
                FileHealth(90, HealthLevel.EXCELLENT, "Imagen intacta (${width}x${height})")
            }
        } else if (length > 30000) {
            return FileHealth(70, HealthLevel.GOOD, "Imagen recuperable sin metadatos EXIF")
        } else {
            return FileHealth(40, HealthLevel.FAIR, "Miniatura reducida o fragmento")
        }
    }
}
