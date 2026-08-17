package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.PhotoRecoveryEngine
import com.example.engine.cleaner.OrphanThumbnailCleaner
import com.example.engine.duplicate.DuplicateMediaDetector
import com.example.engine.repair.HeaderRepairEngine
import com.example.model.CategoryFilter
import com.example.model.DuplicateScanResult
import com.example.model.OrphanCleanResult
import com.example.model.RecoverablePhoto
import com.example.model.RecoverySource
import com.example.model.RepairResult
import com.example.model.RestoreSummary
import com.example.model.ScanProgress
import com.example.model.SortOption
import com.example.shizuku.ShizukuManager
import com.example.shizuku.ShizukuState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PhotoRecoveryViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = PhotoRecoveryEngine(application)
    private var scanJob: Job? = null

    private val _rawPhotos = MutableStateFlow<List<RecoverablePhoto>>(emptyList())
    val rawPhotos: StateFlow<List<RecoverablePhoto>> = _rawPhotos.asStateFlow()

    private val _scanProgress = MutableStateFlow(ScanProgress())
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    private val _selectedPhotoIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedPhotoIds: StateFlow<Set<String>> = _selectedPhotoIds.asStateFlow()

    private val _activeFilter = MutableStateFlow(CategoryFilter.ALL)
    val activeFilter: StateFlow<CategoryFilter> = _activeFilter.asStateFlow()

    private val _activeSort = MutableStateFlow(SortOption.DATE_DESC)
    val activeSort: StateFlow<SortOption> = _activeSort.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPreviewPhoto = MutableStateFlow<RecoverablePhoto?>(null)
    val selectedPreviewPhoto: StateFlow<RecoverablePhoto?> = _selectedPreviewPhoto.asStateFlow()

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    private val _isRepairingHeader = MutableStateFlow(false)
    val isRepairingHeader: StateFlow<Boolean> = _isRepairingHeader.asStateFlow()

    private val _restoreSummary = MutableStateFlow<RestoreSummary?>(null)
    val restoreSummary: StateFlow<RestoreSummary?> = _restoreSummary.asStateFlow()

    private val _hasScanned = MutableStateFlow(false)
    val hasScanned: StateFlow<Boolean> = _hasScanned.asStateFlow()

    // Orphan thumbnail cleaner state
    private val _showOrphanCleanerDialog = MutableStateFlow(false)
    val showOrphanCleanerDialog: StateFlow<Boolean> = _showOrphanCleanerDialog.asStateFlow()

    private val _isScanningOrphans = MutableStateFlow(false)
    val isScanningOrphans: StateFlow<Boolean> = _isScanningOrphans.asStateFlow()

    private val _isCleaningOrphans = MutableStateFlow(false)
    val isCleaningOrphans: StateFlow<Boolean> = _isCleaningOrphans.asStateFlow()

    private val _orphanCleanResult = MutableStateFlow<OrphanCleanResult?>(null)
    val orphanCleanResult: StateFlow<OrphanCleanResult?> = _orphanCleanResult.asStateFlow()

    // Duplicate media finder & cleaner state
    private val _showDuplicateCleanerDialog = MutableStateFlow(false)
    val showDuplicateCleanerDialog: StateFlow<Boolean> = _showDuplicateCleanerDialog.asStateFlow()

    private val _isAnalyzingDuplicates = MutableStateFlow(false)
    val isAnalyzingDuplicates: StateFlow<Boolean> = _isAnalyzingDuplicates.asStateFlow()

    private val _isCleaningDuplicates = MutableStateFlow(false)
    val isCleaningDuplicates: StateFlow<Boolean> = _isCleaningDuplicates.asStateFlow()

    private val _duplicateScanResult = MutableStateFlow<DuplicateScanResult?>(null)
    val duplicateScanResult: StateFlow<DuplicateScanResult?> = _duplicateScanResult.asStateFlow()

    private val _duplicateCleanResult = MutableStateFlow<OrphanCleanResult?>(null)
    val duplicateCleanResult: StateFlow<OrphanCleanResult?> = _duplicateCleanResult.asStateFlow()

    // Shizuku system integration state
    val shizukuState: StateFlow<ShizukuState> = ShizukuManager.shizukuState

    private val _showShizukuDialog = MutableStateFlow(false)
    val showShizukuDialog: StateFlow<Boolean> = _showShizukuDialog.asStateFlow()


    init {
        ShizukuManager.initialize()
    }

    // Derived filtered and sorted photos stream
    val displayedPhotos: StateFlow<List<RecoverablePhoto>> = combine(
        _rawPhotos,
        _activeFilter,
        _activeSort,
        _searchQuery,
        _selectedPhotoIds
    ) { photos, filter, sort, query, selectedIds ->
        var list = photos

        // Filter by category
        if (filter != CategoryFilter.ALL) {
            list = list.filter { photo ->
                when (filter) {
                    CategoryFilter.PHOTOS -> photo.isImage
                    CategoryFilter.VIDEOS -> photo.isVideo
                    CategoryFilter.AUDIOS -> photo.isAudio
                    CategoryFilter.DOCUMENTS -> photo.isDocument
                    CategoryFilter.TRASH -> photo.sourceCategory == RecoverySource.TRASH_MEDIASTORE
                    CategoryFilter.THUMBNAILS -> photo.sourceCategory == RecoverySource.THUMBNAILS_CACHE
                    CategoryFilter.HIDDEN -> photo.sourceCategory == RecoverySource.HIDDEN_VAULT
                    CategoryFilter.APP_CACHE -> photo.sourceCategory == RecoverySource.APP_TEMP_CACHE
                    CategoryFilter.SHIZUKU -> photo.sourceCategory == RecoverySource.SHIZUKU_SYSTEM
                    CategoryFilter.ALL -> true
                }
            }
        }

        // Filter by search query
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) ||
                it.sourceCategory.displayName.lowercase().contains(q) ||
                it.fileExtension.lowercase().contains(q)
            }
        }

        // Sort
        list = when (sort) {
            SortOption.DATE_DESC -> list.sortedByDescending { it.lastModifiedTimestamp }
            SortOption.DATE_ASC -> list.sortedBy { it.lastModifiedTimestamp }
            SortOption.SIZE_DESC -> list.sortedByDescending { it.fileSizeBytes }
            SortOption.SIZE_ASC -> list.sortedBy { it.fileSizeBytes }
            SortOption.NAME_ASC -> list.sortedBy { it.name.lowercase() }
        }

        // Map isSelected flag
        list.map { photo ->
            photo.copy(isSelected = selectedIds.contains(photo.id))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun startScan(deepScan: Boolean = true) {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _selectedPhotoIds.value = emptySet()
            _scanProgress.value = ScanProgress(
                isScanning = true,
                progress = 0.05f,
                currentPath = "Iniciando escaneo...",
                filesScannedCount = 0,
                photosFoundCount = _rawPhotos.value.size,
                phaseName = "Preparando escaneo",
                isComplete = false
            )
            try {
                val results = engine.performScan(deepScan = deepScan) { progress ->
                    _scanProgress.value = progress
                }
                _rawPhotos.value = results
                _hasScanned.value = true
            } catch (e: CancellationException) {
                // El escaneo fue cancelado intencionalmente para iniciar uno nuevo
                throw e
            } catch (e: Exception) {
                Log.e("PhotoRecoveryVM", "Error durante el escaneo de medios", e)
            } finally {
                _scanProgress.update { current ->
                    current.copy(isScanning = false, isComplete = true)
                }
            }
        }
    }

    fun togglePhotoSelection(photoId: String) {
        _selectedPhotoIds.update { current ->
            if (current.contains(photoId)) {
                current - photoId
            } else {
                current + photoId
            }
        }
    }

    fun selectAll() {
        val allIds = displayedPhotos.value.map { it.id }.toSet()
        _selectedPhotoIds.value = allIds
    }

    fun clearSelection() {
        _selectedPhotoIds.value = emptySet()
    }

    fun setFilter(filter: CategoryFilter) {
        _activeFilter.value = filter
    }

    fun setSort(sort: SortOption) {
        _activeSort.value = sort
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openPreview(photo: RecoverablePhoto) {
        _selectedPreviewPhoto.value = photo
    }

    fun closePreview() {
        _selectedPreviewPhoto.value = null
    }

    fun restoreSinglePhoto(photo: RecoverablePhoto) {
        viewModelScope.launch {
            _isRestoring.value = true
            try {
                val restoredUri = engine.restorePhoto(photo)
                if (restoredUri != null) {
                    // Update photo state in rawPhotos
                    _rawPhotos.update { list ->
                        list.map {
                            if (it.id == photo.id) it.copy(isRestored = true, restoredUri = restoredUri)
                            else it
                        }
                    }
                    // Update preview if open
                    if (_selectedPreviewPhoto.value?.id == photo.id) {
                        _selectedPreviewPhoto.value = photo.copy(isRestored = true, restoredUri = restoredUri)
                    }

                    _restoreSummary.value = RestoreSummary(
                        successCount = 1,
                        failedCount = 0,
                        totalBytesRestored = photo.fileSizeBytes,
                        restoredUris = listOf(restoredUri)
                    )
                } else {
                    Toast.makeText(
                        getApplication(),
                        "No se pudo restaurar el archivo seleccionado",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("PhotoRecoveryVM", "Error al restaurar archivo", e)
                Toast.makeText(
                    getApplication(),
                    "Error inesperado al restaurar archivo",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                _isRestoring.value = false
            }
        }
    }

    fun restoreSelectedPhotos() {
        val selectedIds = _selectedPhotoIds.value
        if (selectedIds.isEmpty()) return

        val photosToRestore = _rawPhotos.value.filter { selectedIds.contains(it.id) }
        if (photosToRestore.isEmpty()) return

        viewModelScope.launch {
            _isRestoring.value = true
            try {
                var successCount = 0
                var failedCount = 0
                var totalBytes = 0L
                val restoredUris = mutableListOf<Uri>()

                val updatedList = _rawPhotos.value.toMutableList()

                for (photo in photosToRestore) {
                    val uri = engine.restorePhoto(photo)
                    if (uri != null) {
                        successCount++
                        totalBytes += photo.fileSizeBytes
                        restoredUris.add(uri)

                        val idx = updatedList.indexOfFirst { it.id == photo.id }
                        if (idx != -1) {
                            updatedList[idx] = photo.copy(isRestored = true, restoredUri = uri)
                        }
                    } else {
                        failedCount++
                    }
                }

                _rawPhotos.value = updatedList
                _selectedPhotoIds.value = emptySet()

                _restoreSummary.value = RestoreSummary(
                    successCount = successCount,
                    failedCount = failedCount,
                    totalBytesRestored = totalBytes,
                    restoredUris = restoredUris
                )
            } catch (e: Exception) {
                Log.e("PhotoRecoveryVM", "Error al restaurar lote de archivos", e)
            } finally {
                _isRestoring.value = false
            }
        }
    }

    fun dismissRestoreSummary() {
        _restoreSummary.value = null
    }

    fun openSystemGallery() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                getApplication(),
                "Archivos guardados en el almacenamiento del dispositivo",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun openOrphanCleaner() {
        _showOrphanCleanerDialog.value = true
        _isScanningOrphans.value = true
        viewModelScope.launch {
            try {
                val result = OrphanThumbnailCleaner.analyzeOrphanThumbnails(getApplication())
                _orphanCleanResult.value = result
            } catch (e: Exception) {
                Log.e("PhotoRecoveryVM", "Error analizando miniaturas huérfanas", e)
            } finally {
                _isScanningOrphans.value = false
            }
        }
    }

    fun executeOrphanClean() {
        if (_isCleaningOrphans.value) return
        _isCleaningOrphans.value = true
        viewModelScope.launch {
            try {
                val result = OrphanThumbnailCleaner.purgeOrphanThumbnails(getApplication())
                _orphanCleanResult.value = result
            } catch (e: Exception) {
                Log.e("PhotoRecoveryVM", "Error purgando miniaturas huérfanas", e)
            } finally {
                _isCleaningOrphans.value = false
            }
        }
    }

    fun closeOrphanCleaner() {
        _showOrphanCleanerDialog.value = false
        _orphanCleanResult.value = null
    }

    fun repairPhotoHeader(photo: RecoverablePhoto) {
        if (_isRepairingHeader.value) return
        _isRepairingHeader.value = true
        viewModelScope.launch {
            try {
                val repairResult = HeaderRepairEngine.repairMediaHeader(getApplication(), photo)
                if (repairResult.isSuccess && repairResult.repairedFilePath != null && repairResult.repairedHealth != null) {
                    val updatedPhoto = photo.copy(
                        filePath = repairResult.repairedFilePath,
                        health = repairResult.repairedHealth
                    )

                    _rawPhotos.update { list ->
                        list.map { if (it.id == photo.id) updatedPhoto else it }
                    }

                    if (_selectedPreviewPhoto.value?.id == photo.id) {
                        _selectedPreviewPhoto.value = updatedPhoto
                    }

                    Toast.makeText(
                        getApplication(),
                        "Cabecera reparada con éxito (${photo.fileExtension.uppercase()})",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        getApplication(),
                        repairResult.message.ifBlank { "No fue posible reparar la cabecera del archivo" },
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("PhotoRecoveryVM", "Error reparando cabecera", e)
                Toast.makeText(
                    getApplication(),
                    "Error al intentar reparar la cabecera",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                _isRepairingHeader.value = false
            }
        }
    }

    fun openDuplicateCleaner() {
        _showDuplicateCleanerDialog.value = true
        _isAnalyzingDuplicates.value = true
        _duplicateCleanResult.value = null
        viewModelScope.launch {
            try {
                val result = DuplicateMediaDetector.findDuplicates(_rawPhotos.value)
                _duplicateScanResult.value = result
            } catch (e: Exception) {
                Log.e("PhotoRecoveryVM", "Error detectando duplicados", e)
            } finally {
                _isAnalyzingDuplicates.value = false
            }
        }
    }

    fun executePurgeDuplicates() {
        val dupResult = _duplicateScanResult.value ?: return
        if (_isCleaningDuplicates.value || dupResult.groups.isEmpty()) return

        val duplicateIds = dupResult.groups.flatMap { it.duplicates }.map { it.id }.toSet()
        if (duplicateIds.isEmpty()) return

        _isCleaningDuplicates.value = true
        viewModelScope.launch {
            try {
                val cleanResult = DuplicateMediaDetector.purgeDuplicates(
                    context = getApplication(),
                    duplicatePhotoIds = duplicateIds,
                    allPhotos = _rawPhotos.value
                )
                _duplicateCleanResult.value = cleanResult

                // Remove purged duplicate photos from raw list
                _rawPhotos.update { list ->
                    list.filterNot { duplicateIds.contains(it.id) }
                }
            } catch (e: Exception) {
                Log.e("PhotoRecoveryVM", "Error purgando duplicados", e)
            } finally {
                _isCleaningDuplicates.value = false
            }
        }
    }

    fun closeDuplicateCleaner() {
        _showDuplicateCleanerDialog.value = false
        _duplicateScanResult.value = null
        _duplicateCleanResult.value = null
    }

    fun openShizukuSettings() {
        _showShizukuDialog.value = true
        ShizukuManager.refreshStatus()
    }

    fun closeShizukuSettings() {
        _showShizukuDialog.value = false
    }

    fun requestShizukuPermission() {
        ShizukuManager.requestPermission()
    }

    fun refreshShizukuStatus() {
        ShizukuManager.refreshStatus()
    }

    fun toggleShizukuEnhancedScan(enabled: Boolean) {
        ShizukuManager.toggleEnhancedScan(enabled)
    }
}
