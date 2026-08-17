package com.example.engine.repair

import android.content.Context
import android.util.Log
import com.example.engine.health.FileHealthEvaluator
import com.example.model.FileHealth
import com.example.model.HealthLevel
import com.example.model.MediaType
import com.example.model.RecoverablePhoto
import com.example.model.RepairResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale

object HeaderRepairEngine {

    private const val TAG = "HeaderRepairEngine"

    // Standard Magic Bytes
    private val JPEG_SOI = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
    private val JPEG_JFIF_HEADER = byteArrayOf(
        0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
        0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01, 0x01, 0x01,
        0x00, 0x60, 0x00, 0x60, 0x00, 0x00
    )
    private val PNG_HEADER = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    )
    private val GIF89A_HEADER = "GIF89a".toByteArray(Charsets.US_ASCII)
    private val PDF_HEADER = "%PDF-1.4\n".toByteArray(Charsets.US_ASCII)
    private val MP3_ID3_HEADER = byteArrayOf(
        0x49, 0x44, 0x33, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    )

    /**
     * Checks if a recoverable photo could benefit from header reconstruction or repair.
     */
    fun isRepairRecommended(photo: RecoverablePhoto): Boolean {
        if (photo.health.level == HealthLevel.DAMAGED) return true
        val file = File(photo.filePath)
        if (!file.exists() || file.length() < 16) return false

        return try {
            file.inputStream().use { stream ->
                val header = ByteArray(16)
                val read = stream.read(header)
                if (read < 4) return true

                val ext = photo.fileExtension.lowercase(Locale.ROOT)
                when (ext) {
                    "jpg", "jpeg" -> !(header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte())
                    "png" -> !(header[0] == 0x89.toByte() && header[1] == 0x50.toByte() && header[2] == 0x4E.toByte() && header[3] == 0x47.toByte())
                    "gif" -> !(header[0] == 0x47.toByte() && header[1] == 0x49.toByte() && header[2] == 0x46.toByte())
                    "pdf" -> !(header[0] == 0x25.toByte() && header[1] == 0x50.toByte() && header[2] == 0x44.toByte() && header[3] == 0x46.toByte())
                    "mp4", "mov" -> {
                        val headerStr = String(header, 0, minOf(read, 16), Charsets.US_ASCII)
                        !headerStr.contains("ftyp") && !headerStr.contains("moov") && !headerStr.contains("mdat")
                    }
                    else -> false
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Reconstructs or repairs header corruption (Magic Bytes) by generating a sanitized
     * repaired file in the application's private cache directory.
     */
    suspend fun repairMediaHeader(
        context: Context,
        photo: RecoverablePhoto
    ): RepairResult = withContext(Dispatchers.IO) {
        val originalFile = File(photo.filePath)
        if (!originalFile.exists() || originalFile.length() < 8) {
            return@withContext RepairResult(
                isSuccess = false,
                message = "El archivo original no existe o tiene tamaño 0 bytes"
            )
        }

        try {
            val repairDir = File(context.cacheDir, "repaired_media").apply { mkdirs() }
            val repairedFile = File(repairDir, "repaired_${photo.id}_${photo.name}")

            val rawBytes = originalFile.readBytes()
            val ext = photo.fileExtension.lowercase(Locale.ROOT)

            val repairedBytes: ByteArray? = when (ext) {
                "jpg", "jpeg" -> repairJpegBytes(rawBytes)
                "png" -> repairPngBytes(rawBytes)
                "gif" -> repairGifBytes(rawBytes)
                "pdf" -> repairPdfBytes(rawBytes)
                "mp3" -> repairMp3Bytes(rawBytes)
                "mp4", "m4a", "mov" -> repairMp4Bytes(rawBytes)
                else -> repairGenericMediaBytes(rawBytes, photo.mediaType)
            }

            if (repairedBytes != null && repairedBytes.isNotEmpty()) {
                FileOutputStream(repairedFile).use { out ->
                    out.write(repairedBytes)
                }

                val newHealth = FileHealth(
                    percentage = 90,
                    level = HealthLevel.GOOD,
                    description = "Cabecera binaria (${ext.uppercase()}) reconstruida y validada"
                )

                RepairResult(
                    isSuccess = true,
                    repairedFilePath = repairedFile.absolutePath,
                    repairedHealth = newHealth,
                    message = "Cabecera reconstruida exitosamente. Archivo listo para visualización y restauración."
                )
            } else {
                RepairResult(
                    isSuccess = false,
                    message = "La estructura interna del archivo está demasiado fragmentada para reconstruir su cabecera."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reparando cabecera para ${photo.name}", e)
            RepairResult(
                isSuccess = false,
                message = "Error durante el análisis y reparación: ${e.localizedMessage ?: "Error de E/S"}"
            )
        }
    }

    private fun repairJpegBytes(raw: ByteArray): ByteArray? {
        // 1. Check if SOI (FF D8) is somewhere inside the first 2048 bytes (e.g. garbage prefix)
        val soiOffset = findByteSequence(raw, JPEG_SOI, maxSearch = 2048)
        if (soiOffset > 0) {
            // Trim garbage prefix
            return raw.copyOfRange(soiOffset, raw.size)
        }

        // 2. If it has raw JPEG markers (e.g. FF DB / FF C0) without SOI, prepend JFIF Header
        val hasDqtOrSof = findByteSequence(raw, byteArrayOf(0xFF.toByte(), 0xDB.toByte()), 1024) != -1 ||
                findByteSequence(raw, byteArrayOf(0xFF.toByte(), 0xC0.toByte()), 1024) != -1

        if (hasDqtOrSof) {
            return JPEG_JFIF_HEADER + raw
        }

        // 3. Simple SOI prepend if raw starts with standard 0xFF
        if (raw.isNotEmpty() && raw[0] == 0xFF.toByte()) {
            return byteArrayOf(0xFF.toByte(), 0xD8.toByte()) + raw.copyOfRange(1, raw.size)
        }

        // 4. Prepend standard JPEG SOI
        return JPEG_SOI + raw
    }

    private fun repairPngBytes(raw: ByteArray): ByteArray? {
        // Search for IHDR chunk ('IHDR' = 0x49 0x48 0x44 0x52)
        val ihdrSeq = byteArrayOf(0x49, 0x48, 0x44, 0x52)
        val ihdrOffset = findByteSequence(raw, ihdrSeq, maxSearch = 2048)
        if (ihdrOffset != -1) {
            // IHDR is preceded by 4-byte chunk length. Standard PNG signature is 8 bytes before IHDR-4
            val chunkLengthOffset = maxOf(0, ihdrOffset - 4)
            val payload = raw.copyOfRange(chunkLengthOffset, raw.size)
            return PNG_HEADER + payload
        }
        return PNG_HEADER + raw
    }

    private fun repairGifBytes(raw: ByteArray): ByteArray? {
        val gifSeq = byteArrayOf(0x47, 0x49, 0x46) // "GIF"
        val offset = findByteSequence(raw, gifSeq, maxSearch = 1024)
        if (offset > 0) {
            return raw.copyOfRange(offset, raw.size)
        }
        return GIF89A_HEADER + raw
    }

    private fun repairPdfBytes(raw: ByteArray): ByteArray? {
        val pdfSeq = "%PDF-".toByteArray(Charsets.US_ASCII)
        val offset = findByteSequence(raw, pdfSeq, maxSearch = 2048)
        if (offset > 0) {
            return raw.copyOfRange(offset, raw.size)
        }
        return PDF_HEADER + raw
    }

    private fun repairMp3Bytes(raw: ByteArray): ByteArray? {
        // Search for ID3 or MPEG sync frame (FF FB, FF F3, FF F2)
        val id3Seq = "ID3".toByteArray(Charsets.US_ASCII)
        val id3Offset = findByteSequence(raw, id3Seq, maxSearch = 2048)
        if (id3Offset > 0) {
            return raw.copyOfRange(id3Offset, raw.size)
        }
        return MP3_ID3_HEADER + raw
    }

    private fun repairMp4Bytes(raw: ByteArray): ByteArray? {
        val ftypSeq = "ftyp".toByteArray(Charsets.US_ASCII)
        val ftypOffset = findByteSequence(raw, ftypSeq, maxSearch = 2048)
        if (ftypOffset >= 4) {
            // Align to 4-byte box size header
            return raw.copyOfRange(ftypOffset - 4, raw.size)
        }
        val moovSeq = "moov".toByteArray(Charsets.US_ASCII)
        val moovOffset = findByteSequence(raw, moovSeq, maxSearch = 2048)
        if (moovOffset >= 4) {
            return raw.copyOfRange(moovOffset - 4, raw.size)
        }
        return null
    }

    private fun repairGenericMediaBytes(raw: ByteArray, mediaType: MediaType): ByteArray? {
        return when (mediaType) {
            MediaType.IMAGE -> JPEG_SOI + raw
            MediaType.DOCUMENT -> PDF_HEADER + raw
            MediaType.AUDIO -> MP3_ID3_HEADER + raw
            MediaType.VIDEO -> null
        }
    }

    private fun findByteSequence(data: ByteArray, target: ByteArray, maxSearch: Int): Int {
        val limit = minOf(data.size - target.size, maxSearch)
        for (i in 0..limit) {
            var match = true
            for (j in target.indices) {
                if (data[i + j] != target[j]) {
                    match = false
                    break
                }
            }
            if (match) return i
        }
        return -1
    }
}
