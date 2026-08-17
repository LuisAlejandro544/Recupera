package com.example.engine

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.example.engine.filter.ActiveGalleryFilter
import com.example.engine.restore.MediaRestorer
import com.example.engine.scan.StorageDirectoryScanner
import com.example.engine.scan.TrashMediaScanner
import com.example.model.RecoverablePhoto
import com.example.model.ScanProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PhotoRecoveryEngine(private val context: Context) {

    init {
        // Clean up any previously created sample cache folder
        try {
            val sampleDir = File(context.cacheDir, "recovered_cache_samples")
            if (sampleDir.exists()) {
                sampleDir.deleteRecursively()
            }
        } catch (_: Exception) {}
    }

    suspend fun performScan(
        deepScan: Boolean = true,
        onProgress: suspend (ScanProgress) -> Unit
    ): List<RecoverablePhoto> = withContext(Dispatchers.IO) {
        val foundPhotos = mutableListOf<RecoverablePhoto>()
        val seenPaths = mutableSetOf<String>()
        var totalFilesScanned = 0

        // Step 0: Index active gallery media to exclude active visible photos/videos/audios
        val activeSignatures = ActiveGalleryFilter.loadActiveGallerySignatures(context)

        // Phase 1: Scan System MediaStore Trash (Images + Videos + Audios in trash, API 30+)
        onProgress(
            ScanProgress(
                isScanning = true,
                progress = 0.1f,
                currentPath = "Consultando papelera de fotos, videos y audios eliminados...",
                filesScannedCount = totalFilesScanned,
                photosFoundCount = foundPhotos.size,
                phaseName = "Escaneando Papelera del Sistema"
            )
        )
        TrashMediaScanner.scanMediaStoreTrash(context, foundPhotos, seenPaths)
        TrashMediaScanner.scanMediaStoreVideoTrash(context, foundPhotos, seenPaths)
        TrashMediaScanner.scanMediaStoreAudioTrash(context, foundPhotos, seenPaths)
        totalFilesScanned += 45

        // Phase 2: Scan Thumbnail caches (DCIM/.thumbnails, Pictures/.thumbnails, Movies/.thumbnails, etc.)
        onProgress(
            ScanProgress(
                isScanning = true,
                progress = 0.3f,
                currentPath = "Escaneando miniaturas residuales y cachés...",
                filesScannedCount = totalFilesScanned,
                photosFoundCount = foundPhotos.size,
                phaseName = "Escaneando Caché de Miniaturas"
            )
        )
        StorageDirectoryScanner.scanThumbnailDirectories(
            context = context,
            foundPhotos = foundPhotos,
            seenPaths = seenPaths,
            activePaths = activeSignatures.activePaths,
            activeFingerprints = activeSignatures.activeFingerprints
        ) {
            totalFilesScanned++
        }

        // Phase 3: Scan App Caches, Voice Notes and Hidden folders (.nomedia, Statuses, WhatsApp, Telegram)
        onProgress(
            ScanProgress(
                isScanning = true,
                progress = 0.55f,
                currentPath = "Buscando audios borrados, notas de voz y carpetas ocultas...",
                filesScannedCount = totalFilesScanned,
                photosFoundCount = foundPhotos.size,
                phaseName = "Buscando en Audios y Ocultos"
            )
        )
        StorageDirectoryScanner.scanHiddenAndAppCaches(
            context = context,
            foundPhotos = foundPhotos,
            seenPaths = seenPaths,
            activePaths = activeSignatures.activePaths,
            activeFingerprints = activeSignatures.activeFingerprints
        ) {
            totalFilesScanned++
        }

        // Phase 3.5: Scan Shizuku Protected Vaults (Samsung/Xiaomi Trash & Android/data) if enabled
        if (com.example.shizuku.ShizukuManager.shizukuState.value.isReadyForEnhancedScan) {
            onProgress(
                ScanProgress(
                    isScanning = true,
                    progress = 0.70f,
                    currentPath = "Escaneando con privilegios ADB/Shizuku (Android/data y papelera del fabricante)...",
                    filesScannedCount = totalFilesScanned,
                    photosFoundCount = foundPhotos.size,
                    phaseName = "Bóvedas Shizuku / Sistema"
                )
            )
            com.example.engine.scan.ShizukuVendorTrashScanner.scanProtectedVaults(
                context = context,
                foundPhotos = foundPhotos,
                seenPaths = seenPaths,
                activePaths = activeSignatures.activePaths,
                activeFingerprints = activeSignatures.activeFingerprints
            ) {
                totalFilesScanned++
            }
        }

        // Phase 4: Deep Storage Scan if requested
        if (deepScan) {
            onProgress(
                ScanProgress(
                    isScanning = true,
                    progress = 0.8f,
                    currentPath = "Escaneo profundo de archivos multimedia no indexados...",
                    filesScannedCount = totalFilesScanned,
                    photosFoundCount = foundPhotos.size,
                    phaseName = "Escaneo Profundo de Disco"
                )
            )
            val externalStorage = Environment.getExternalStorageDirectory()
            if (externalStorage != null && externalStorage.exists()) {
                StorageDirectoryScanner.deepScanDirectory(
                    directory = externalStorage,
                    foundPhotos = foundPhotos,
                    seenPaths = seenPaths,
                    activePaths = activeSignatures.activePaths,
                    activeFingerprints = activeSignatures.activeFingerprints,
                    maxDepth = 4
                ) {
                    totalFilesScanned++
                }
            }
        }

        // Complete
        onProgress(
            ScanProgress(
                isScanning = false,
                progress = 1.0f,
                currentPath = "Escaneo finalizado",
                filesScannedCount = totalFilesScanned,
                photosFoundCount = foundPhotos.size,
                phaseName = "Completado",
                isComplete = true
            )
        )

        foundPhotos.sortedByDescending { it.lastModifiedTimestamp }
    }

    /**
     * Restores a photo, video or audio to the system MediaStore
     */
    suspend fun restorePhoto(photo: RecoverablePhoto): Uri? {
        return MediaRestorer.restoreMedia(context, photo)
    }
}
