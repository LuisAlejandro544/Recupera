package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CategoryFilter
import com.example.model.RecoverySource
import com.example.shizuku.ShizukuStatus
import com.example.ui.components.FilterBar
import com.example.ui.components.FullscreenPhotoPreview
import com.example.ui.components.PhotoCard
import com.example.ui.components.RestoreSuccessDialog
import com.example.ui.components.ScanProgressBanner
import com.example.ui.components.cleaner.OrphanCleanerDialog
import com.example.ui.components.settings.ShizukuSettingsDialog
import com.example.ui.components.screen.BatchRestoreBar
import com.example.ui.components.screen.EmptyStateCard
import com.example.ui.components.screen.OverviewCard
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.TealAccent
import com.example.viewmodel.PhotoRecoveryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoRecoveryScreen(
    viewModel: PhotoRecoveryViewModel
) {
    val rawPhotos by viewModel.rawPhotos.collectAsState()
    val displayedPhotos by viewModel.displayedPhotos.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val selectedPhotoIds by viewModel.selectedPhotoIds.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val activeSort by viewModel.activeSort.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val previewPhoto by viewModel.selectedPreviewPhoto.collectAsState()
    val isRestoring by viewModel.isRestoring.collectAsState()
    val restoreSummary by viewModel.restoreSummary.collectAsState()

    val showOrphanCleaner by viewModel.showOrphanCleanerDialog.collectAsState()
    val isScanningOrphans by viewModel.isScanningOrphans.collectAsState()
    val isCleaningOrphans by viewModel.isCleaningOrphans.collectAsState()
    val orphanCleanResult by viewModel.orphanCleanResult.collectAsState()

    val shizukuState by viewModel.shizukuState.collectAsState()
    val showShizukuDialog by viewModel.showShizukuDialog.collectAsState()

    // Calculate category counts
    val categoryCounts = remember(rawPhotos) {
        mapOf<CategoryFilter, Int>(
            CategoryFilter.ALL to rawPhotos.size,
            CategoryFilter.PHOTOS to rawPhotos.count { it.isImage },
            CategoryFilter.VIDEOS to rawPhotos.count { it.isVideo },
            CategoryFilter.AUDIOS to rawPhotos.count { it.isAudio },
            CategoryFilter.DOCUMENTS to rawPhotos.count { it.isDocument },
            CategoryFilter.TRASH to rawPhotos.count { it.sourceCategory == RecoverySource.TRASH_MEDIASTORE },
            CategoryFilter.THUMBNAILS to rawPhotos.count { it.sourceCategory == RecoverySource.THUMBNAILS_CACHE },
            CategoryFilter.HIDDEN to rawPhotos.count { it.sourceCategory == RecoverySource.HIDDEN_VAULT },
            CategoryFilter.APP_CACHE to rawPhotos.count { it.sourceCategory == RecoverySource.APP_TEMP_CACHE },
            CategoryFilter.SHIZUKU to rawPhotos.count { it.sourceCategory == RecoverySource.SHIZUKU_SYSTEM }
        )
    }

    val selectedPhotos = rawPhotos.filter { selectedPhotoIds.contains(it.id) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(CyanPrimary, TealAccent))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Recuperador Pro",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Fotos, videos, audios y documentos eliminados",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                },
                actions = {
                    if (displayedPhotos.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                if (selectedPhotoIds.size == displayedPhotos.size) {
                                    viewModel.clearSelection()
                                } else {
                                    viewModel.selectAll()
                                }
                            },
                            modifier = Modifier.testTag("select_all_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (selectedPhotoIds.size == displayedPhotos.size) Icons.Default.Check else Icons.Default.SelectAll,
                                contentDescription = "Seleccionar todo",
                                tint = CyanPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.openShizukuSettings() },
                        modifier = Modifier.testTag("open_shizuku_settings_button")
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Configuración Shizuku",
                                tint = when (shizukuState.status) {
                                    ShizukuStatus.AUTHORIZED_ACTIVE -> CyanPrimary
                                    ShizukuStatus.RUNNING_UNAUTHORIZED -> Color(0xFFF59E0B)
                                    ShizukuStatus.NOT_RUNNING -> Color(0xFF94A3B8)
                                }
                            )
                            if (shizukuState.status == ShizukuStatus.AUTHORIZED_ACTIVE) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { viewModel.startScan(deepScan = true) },
                        enabled = !scanProgress.isScanning,
                        modifier = Modifier.testTag("rescan_action_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Volver a escanear",
                            tint = if (scanProgress.isScanning) Color.Gray else CyanPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 145.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = if (selectedPhotoIds.isNotEmpty()) 100.dp else 24.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("photos_grid")
            ) {
                // Header Span: Stats & Scan actions
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Overview Stats Card
                        OverviewCard(
                            totalPhotosCount = rawPhotos.size,
                            totalSizeBytes = rawPhotos.sumOf { it.fileSizeBytes },
                            isScanning = scanProgress.isScanning,
                            onQuickScan = { viewModel.startScan(deepScan = false) },
                            onDeepScan = { viewModel.startScan(deepScan = true) },
                            onCleanOrphans = { viewModel.openOrphanCleaner() }
                        )

                        // Scan Progress Live Banner
                        if (scanProgress.isScanning) {
                            ScanProgressBanner(scanProgress = scanProgress)
                        }

                        // Search and Filter Controls
                        FilterBar(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            activeFilter = activeFilter,
                            onFilterChange = { viewModel.setFilter(it) },
                            activeSort = activeSort,
                            onSortChange = { viewModel.setSort(it) },
                            photoCounts = categoryCounts
                        )
                    }
                }

                // Grid Items: Recoverable Photos
                if (displayedPhotos.isNotEmpty()) {
                    items(
                        items = displayedPhotos,
                        key = { it.id }
                    ) { photo ->
                        PhotoCard(
                            photo = photo,
                            onPhotoClick = { viewModel.openPreview(it) },
                            onToggleSelect = { viewModel.togglePhotoSelection(it) }
                        )
                    }
                } else if (!scanProgress.isScanning) {
                    // Empty State Span
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyStateCard(
                            hasSearch = searchQuery.isNotBlank(),
                            onClearSearch = { viewModel.setSearchQuery("") },
                            onDeepScan = { viewModel.startScan(deepScan = true) }
                        )
                    }
                }
            }

            // Bottom Floating Bar for Batch Restoration
            BatchRestoreBar(
                selectedPhotos = selectedPhotos,
                isRestoring = isRestoring,
                onRestore = { viewModel.restoreSelectedPhotos() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )

            // Fullscreen Zoomable Photo Preview Modal
            previewPhoto?.let { photo ->
                FullscreenPhotoPreview(
                    photo = photo,
                    isRestoring = isRestoring,
                    onDismiss = { viewModel.closePreview() },
                    onRestore = { viewModel.restoreSinglePhoto(it) }
                )
            }

            // Restore Success Dialog
            restoreSummary?.let { summary ->
                RestoreSuccessDialog(
                    summary = summary,
                    onDismiss = { viewModel.dismissRestoreSummary() },
                    onOpenGallery = {
                        viewModel.dismissRestoreSummary()
                        viewModel.openSystemGallery()
                    }
                )
            }

            // Orphan Thumbnail Cleaner Dialog
            if (showOrphanCleaner) {
                OrphanCleanerDialog(
                    isScanningOrphans = isScanningOrphans,
                    isCleaningOrphans = isCleaningOrphans,
                    cleanResult = orphanCleanResult,
                    onCleanOrphans = { viewModel.executeOrphanClean() },
                    onDismiss = { viewModel.closeOrphanCleaner() }
                )
            }

            // Shizuku Settings and Status Dialog
            if (showShizukuDialog) {
                ShizukuSettingsDialog(
                    shizukuState = shizukuState,
                    onRequestPermission = { viewModel.requestShizukuPermission() },
                    onRefreshStatus = { viewModel.refreshShizukuStatus() },
                    onToggleEnhancedScan = { viewModel.toggleShizukuEnhancedScan(it) },
                    onDismiss = { viewModel.closeShizukuSettings() }
                )
            }
        }
    }
}
