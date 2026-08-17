package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CategoryFilter
import com.example.model.RecoverySource
import com.example.ui.components.FilterBar
import com.example.ui.components.FullscreenPhotoPreview
import com.example.ui.components.PhotoCard
import com.example.ui.components.RestoreSuccessDialog
import com.example.ui.components.ScanProgressBanner
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
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

    // Calculate category counts
    val categoryCounts = remember(rawPhotos) {
        mapOf<CategoryFilter, Int>(
            CategoryFilter.ALL to rawPhotos.size,
            CategoryFilter.PHOTOS to rawPhotos.count { it.isImage },
            CategoryFilter.VIDEOS to rawPhotos.count { it.isVideo },
            CategoryFilter.AUDIOS to rawPhotos.count { it.isAudio },
            CategoryFilter.TRASH to rawPhotos.count { it.sourceCategory == RecoverySource.TRASH_MEDIASTORE },
            CategoryFilter.THUMBNAILS to rawPhotos.count { it.sourceCategory == RecoverySource.THUMBNAILS_CACHE },
            CategoryFilter.HIDDEN to rawPhotos.count { it.sourceCategory == RecoverySource.HIDDEN_VAULT },
            CategoryFilter.APP_CACHE to rawPhotos.count { it.sourceCategory == RecoverySource.APP_TEMP_CACHE }
        )
    }

    // Calculate selected size
    val selectedPhotos = rawPhotos.filter { selectedPhotoIds.contains(it.id) }
    val selectedBytes = selectedPhotos.sumOf { it.fileSizeBytes }
    val selectedSizeText = if (selectedBytes >= 1024 * 1024) {
        String.format("%.1f MB", selectedBytes / (1024.0 * 1024.0))
    } else {
        String.format("%.0f KB", selectedBytes / 1024.0)
    }

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
                                text = "Recupera fotos, videos y audios eliminados",
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
                            onDeepScan = { viewModel.startScan(deepScan = true) }
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
            AnimatedVisibility(
                visible = selectedPhotoIds.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("batch_restore_bar"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${selectedPhotos.size} archivo(s) seleccionado(s)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            )
                            Text(
                                text = "Tamaño total: $selectedSizeText",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = CyanPrimary,
                                    fontSize = 12.sp
                                )
                            )
                        }

                        Button(
                            onClick = { viewModel.restoreSelectedPhotos() },
                            enabled = !isRestoring,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("batch_restore_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                        ) {
                            if (isRestoring) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restaurando...", color = Color.Black, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Restore,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Restaurar",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

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
        }
    }
}

@Composable
private fun OverviewCard(
    totalPhotosCount: Int,
    totalSizeBytes: Long,
    isScanning: Boolean,
    onQuickScan: () -> Unit,
    onDeepScan: () -> Unit
) {
    val formattedTotalSize = if (totalSizeBytes >= 1024 * 1024) {
        String.format("%.1f MB", totalSizeBytes / (1024.0 * 1024.0))
    } else {
        String.format("%.0f KB", totalSizeBytes / 1024.0)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("overview_stats_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Archivos Detectados",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = "$totalPhotosCount detectados",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Espacio Recuperable",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = formattedTotalSize,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = CyanPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onQuickScan,
                    enabled = !isScanning,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("quick_scan_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ImageSearch,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Escaneo Rápido",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onDeepScan,
                    enabled = !isScanning,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("deep_scan_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Escaneo Profundo",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    hasSearch: Boolean,
    onClearSearch: () -> Unit,
    onDeepScan: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
            .testTag("empty_state_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(CyanPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (hasSearch) "Sin coincidencias" else "No se encontraron fotos en este filtro",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (hasSearch) "Intenta buscar con otro término o limpia el buscador." else "Ejecuta un escaneo profundo para buscar rastros en caché y carpetas ocultas.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (hasSearch) {
                Button(
                    onClick = onClearSearch,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("Limpiar Búsqueda", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onDeepScan,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("Iniciar Escaneo Profundo", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
