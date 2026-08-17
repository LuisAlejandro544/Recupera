package com.example.shizuku.service

import android.content.Context
import android.os.Binder
import android.os.Parcel
import android.util.Log
import com.example.shizuku.model.ShizukuScannedItem
import java.io.File
import java.util.Locale

/**
 * Service that runs in a separate process with ADB (UID 2000) or Root (UID 0) privileges via Shizuku.
 * Scans protected Android/data and vendor trash directories that are inaccessible to standard apps on Android 11+.
 */
class ShizukuScanUserService : Binder {

    companion object {
        const val DESCRIPTOR = "com.example.shizuku.service.IShizukuScanService"
        const val TRANSACTION_DESTROY = 16777115
        const val TRANSACTION_SCAN_PROTECTED_DIRECTORIES = 1
        const val TRANSACTION_CHECK_ALIVE = 2
        private const val TAG = "ShizukuScanService"

        private val RECOVERABLE_EXTENSIONS = setOf(
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "dng",
            "mp4", "mkv", "3gp", "mov", "webm", "avi", "ts",
            "mp3", "m4a", "wav", "aac", "ogg", "opus", "flac", "amr",
            "pdf", "docx", "doc", "xlsx", "xls", "csv", "pptx", "ppt", "txt", "rtf", "epub"
        )
    }

    constructor() : super()

    constructor(context: Context) : super() {
        Log.i(TAG, "ShizukuScanUserService initialized with Context in ADB process")
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        when (code) {
            TRANSACTION_DESTROY -> {
                Log.i(TAG, "Received destroy transaction. Exiting process.")
                reply?.writeNoException()
                try {
                    System.exit(0)
                } catch (_: Throwable) {}
                return true
            }
            TRANSACTION_CHECK_ALIVE -> {
                data.enforceInterface(DESCRIPTOR)
                reply?.writeNoException()
                reply?.writeInt(1)
                return true
            }
            TRANSACTION_SCAN_PROTECTED_DIRECTORIES -> {
                data.enforceInterface(DESCRIPTOR)
                val targetDirectories = data.createStringArrayList() ?: emptyList()
                val results = scanDirectories(targetDirectories)
                reply?.writeNoException()
                reply?.writeTypedList(results)
                return true
            }
        }
        return super.onTransact(code, data, reply, flags)
    }

    private fun scanDirectories(dirPaths: List<String>): List<ShizukuScannedItem> {
        val foundItems = mutableListOf<ShizukuScannedItem>()
        val defaultPaths = if (dirPaths.isNotEmpty()) dirPaths else listOf(
            "/sdcard/Android/data/com.sec.android.gallery3d",
            "/sdcard/Android/data/com.miui.gallery",
            "/sdcard/Android/data/com.whatsapp",
            "/sdcard/Android/media/com.whatsapp",
            "/sdcard/Android/data/org.telegram.messenger",
            "/sdcard/Android/data/com.instagram.android",
            "/sdcard/Android/data/com.zhiliaoapp.musically",
            "/sdcard/Android/data/com.google.android.apps.photos",
            "/sdcard/DCIM/.trash",
            "/sdcard/Pictures/.trash",
            "/sdcard/MIUI/Gallery/cloud/.trashBin"
        )

        for (path in defaultPaths) {
            val dir = File(path)
            if (dir.exists() && dir.canRead()) {
                scanDirectoryRecursively(dir, foundItems, depth = 0, maxDepth = 6)
            }
        }
        return foundItems
    }

    private fun scanDirectoryRecursively(
        directory: File,
        results: MutableList<ShizukuScannedItem>,
        depth: Int,
        maxDepth: Int
    ) {
        if (depth > maxDepth || results.size >= 1000) return
        val files = try { directory.listFiles() } catch (_: Throwable) { null } ?: return

        for (file in files) {
            if (file.isDirectory) {
                scanDirectoryRecursively(file, results, depth + 1, maxDepth)
            } else if (file.isFile && file.length() > 0) {
                val ext = file.extension.lowercase(Locale.ROOT)
                val isNomediaOrResidual = file.name.startsWith(".") || file.parent?.contains(".trash", ignoreCase = true) == true
                if (RECOVERABLE_EXTENSIONS.contains(ext) || isNomediaOrResidual) {
                    val mimeType = resolveMimeType(ext)
                    val isVendorTrash = file.absolutePath.contains("trash", ignoreCase = true) ||
                            file.absolutePath.contains("gallery", ignoreCase = true)
                    val sourcePackage = resolvePackageName(file.absolutePath)

                    results.add(
                        ShizukuScannedItem(
                            absolutePath = file.absolutePath,
                            fileName = file.name,
                            sizeBytes = file.length(),
                            lastModified = file.lastModified(),
                            mimeType = mimeType,
                            sourcePackage = sourcePackage,
                            isFromVendorTrash = isVendorTrash
                        )
                    )
                }
            }
        }
    }

    private fun resolvePackageName(path: String): String {
        return when {
            path.contains("com.sec.android.gallery3d") -> "com.sec.android.gallery3d"
            path.contains("com.miui.gallery") -> "com.miui.gallery"
            path.contains("com.whatsapp") -> "com.whatsapp"
            path.contains("org.telegram.messenger") -> "org.telegram.messenger"
            path.contains("com.instagram.android") -> "com.instagram.android"
            path.contains("com.google.android.apps.photos") -> "com.google.android.apps.photos"
            else -> "system.storage"
        }
    }

    private fun resolveMimeType(extension: String): String {
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "heic" -> "image/heic"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "3gp" -> "video/3gpp"
            "mov" -> "video/quicktime"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "wav" -> "audio/wav"
            "aac" -> "audio/aac"
            "ogg", "opus" -> "audio/ogg"
            "pdf" -> "application/pdf"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "doc" -> "application/msword"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "xls" -> "application/vnd.ms-excel"
            "csv" -> "text/csv"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "ppt" -> "application/vnd.ms-powerpoint"
            "txt" -> "text/plain"
            "epub" -> "application/epub+zip"
            else -> "application/octet-stream"
        }
    }
}
