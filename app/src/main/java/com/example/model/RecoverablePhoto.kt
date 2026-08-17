package com.example.model

import android.net.Uri

enum class MediaType {
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT
}

enum class HealthLevel(val label: String, val colorHex: Long) {
    EXCELLENT("100% Íntegro", 0xFF10B981),
    GOOD("85% Muy Bueno", 0xFF00E5FF),
    FAIR("60% Recuperable", 0xFFF59E0B),
    DAMAGED("35% Parcial", 0xFFEF4444)
}

data class FileHealth(
    val percentage: Int,
    val level: HealthLevel,
    val description: String
)

enum class RecoverySource(val displayName: String, val description: String) {
    TRASH_MEDIASTORE("Papelera del Sistema", "Archivos eliminados retenidos por el sistema"),
    THUMBNAILS_CACHE("Caché de Miniaturas", "Copias de seguridad en caché de alta resolución"),
    APP_TEMP_CACHE("Caché de Apps", "Restos temporales de redes sociales y apps"),
    HIDDEN_VAULT("Carpetas Ocultas", "Archivos en directorios ocultos (.nomedia)"),
    DEEP_STORAGE("Almacenamiento Profundo", "Archivos huérfanos escaneados en disco"),
    SHIZUKU_SYSTEM("Bóvedas Shizuku", "Archivos en carpetas protegidas y papeleras del fabricante")
}

enum class CategoryFilter(val title: String) {
    ALL("Todas"),
    PHOTOS("Fotos"),
    VIDEOS("Videos"),
    AUDIOS("Audios"),
    DOCUMENTS("Documentos"),
    TRASH("Papelera"),
    THUMBNAILS("Miniaturas"),
    HIDDEN("Ocultas"),
    APP_CACHE("Caché Apps"),
    SHIZUKU("Shizuku")
}

enum class SortOption(val title: String) {
    DATE_DESC("Más recientes"),
    DATE_ASC("Más antiguas"),
    SIZE_DESC("Mayor tamaño"),
    SIZE_ASC("Menor tamaño"),
    NAME_ASC("Nombre (A-Z)")
}

data class RecoverablePhoto(
    val id: String,
    val name: String,
    val filePath: String,
    val contentUri: Uri? = null,
    val fileSizeBytes: Long,
    val lastModifiedTimestamp: Long,
    val sourceCategory: RecoverySource,
    val fileExtension: String,
    val dimensions: String? = null,
    val isSelected: Boolean = false,
    val isRestored: Boolean = false,
    val restoredUri: Uri? = null,
    val isSample: Boolean = false,
    val mediaType: MediaType = MediaType.IMAGE,
    val durationMs: Long = 0L,
    val health: FileHealth = FileHealth(95, HealthLevel.EXCELLENT, "Estructura de archivo intacta")
) {
    val isVideo: Boolean
        get() = mediaType == MediaType.VIDEO || fileExtension.lowercase() in listOf("mp4", "mkv", "mov", "3gp", "webm", "avi")

    val isAudio: Boolean
        get() = mediaType == MediaType.AUDIO || fileExtension.lowercase() in listOf("mp3", "aac", "m4a", "opus", "ogg", "wav", "amr", "flac", "3ga")

    val isDocument: Boolean
        get() = mediaType == MediaType.DOCUMENT || fileExtension.lowercase() in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "epub", "csv", "rtf")

    val isImage: Boolean
        get() = !isVideo && !isAudio && !isDocument

    val durationFormatted: String
        get() {
            if (durationMs <= 0) return ""
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

    val formattedSize: String
        get() {
            if (fileSizeBytes <= 0) return "0 KB"
            val kb = fileSizeBytes / 1024.0
            val mb = kb / 1024.0
            return if (mb >= 1.0) {
                String.format("%.1f MB", mb)
            } else {
                String.format("%.0f KB", kb)
            }
        }
}

data class ScanProgress(
    val isScanning: Boolean = false,
    val progress: Float = 0f,
    val currentPath: String = "",
    val filesScannedCount: Int = 0,
    val photosFoundCount: Int = 0,
    val phaseName: String = "",
    val isComplete: Boolean = false
)

data class RestoreSummary(
    val successCount: Int,
    val failedCount: Int,
    val totalBytesRestored: Long,
    val destinationFolder: String = "Pictures/Restored_Photos",
    val restoredUris: List<Uri> = emptyList()
)

data class OrphanCleanResult(
    val scannedCount: Int,
    val deletedCount: Int,
    val freedBytes: Long,
    val failedCount: Int = 0,
    val isDryRun: Boolean = false
) {
    val formattedFreedSize: String
        get() {
            if (freedBytes <= 0) return "0 KB"
            val kb = freedBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                else -> String.format("%.0f KB", kb)
            }
        }
}

data class RepairResult(
    val isSuccess: Boolean,
    val repairedFilePath: String? = null,
    val repairedHealth: FileHealth? = null,
    val message: String = ""
)

data class DuplicateGroup(
    val id: String,
    val primaryPhoto: RecoverablePhoto,
    val duplicates: List<RecoverablePhoto>,
    val totalWastedBytes: Long
) {
    val formattedWastedSize: String
        get() {
            if (totalWastedBytes <= 0) return "0 KB"
            val kb = totalWastedBytes / 1024.0
            val mb = kb / 1024.0
            return if (mb >= 1.0) {
                String.format("%.1f MB", mb)
            } else {
                String.format("%.0f KB", kb)
            }
        }
}

data class DuplicateScanResult(
    val groups: List<DuplicateGroup> = emptyList(),
    val totalDuplicatesCount: Int = 0,
    val totalWastedBytes: Long = 0L
) {
    val formattedTotalWastedSize: String
        get() {
            if (totalWastedBytes <= 0) return "0 KB"
            val kb = totalWastedBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                else -> String.format("%.0f KB", kb)
            }
        }
}

