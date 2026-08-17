package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine.repair.HeaderRepairEngine
import com.example.model.HealthLevel
import com.example.model.RecoverablePhoto
import com.example.ui.components.preview.AudioPreviewPlayer
import com.example.ui.components.preview.DetailRow
import com.example.ui.components.preview.DocumentPreviewContent
import com.example.ui.components.preview.FileHealthMeterSection
import com.example.ui.components.preview.ImagePreviewContent
import com.example.ui.components.preview.VideoPreviewPlayer
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.TealAccent
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FullscreenPhotoPreview(
    photo: RecoverablePhoto,
    isRestoring: Boolean,
    isRepairing: Boolean = false,
    onDismiss: () -> Unit,
    onRestore: (RecoverablePhoto) -> Unit,
    onRepairHeader: ((RecoverablePhoto) -> Unit)? = null
) {
    var showInfoSheet by remember { mutableStateOf(false) }

    val mediaFile = if (photo.filePath.isNotBlank() && File(photo.filePath).exists()) {
        File(photo.filePath)
    } else {
        null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("fullscreen_preview_dialog"),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    photo.isVideo -> {
                        // 5-Second Video Preview Player
                        VideoPreviewPlayer(
                            photo = photo,
                            mediaFile = mediaFile,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    photo.isAudio -> {
                        // 5-Second Audio Preview Player
                        AudioPreviewPlayer(
                            photo = photo,
                            mediaFile = mediaFile,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    photo.isDocument -> {
                        // Document Preview Content
                        DocumentPreviewContent(
                            photo = photo,
                            mediaFile = mediaFile,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        // Interactive Zoomable Photo Area
                        ImagePreviewContent(
                            photo = photo,
                            mediaFile = mediaFile,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Top Floating Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .testTag("preview_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (photo.isVideo) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.9f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "PREVIEW VIDEO 5s",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        } else if (photo.isAudio) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.9f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "PREVIEW AUDIO 5s",
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        SourceBadge(source = photo.sourceCategory)
                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { showInfoSheet = !showInfoSheet },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (showInfoSheet) CyanPrimary else Color.Black.copy(alpha = 0.6f))
                                .testTag("toggle_info_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Detalles y Salud",
                                tint = if (showInfoSheet) Color.Black else Color.White
                            )
                        }
                    }
                }

                // Bottom Action & Information Drawer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    AnimatedVisibility(
                        visible = showInfoSheet,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.95f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                val typeTitle = when {
                                    photo.isVideo -> "Detalles del Video"
                                    photo.isAudio -> "Detalles del Audio"
                                    photo.isDocument -> "Detalles del Documento"
                                    else -> "Detalles de la Foto"
                                }
                                Text(
                                    text = typeTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = CyanPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                // Health Meter Diagnostic Card
                                FileHealthMeterSection(health = photo.health)

                                if (HeaderRepairEngine.isRepairRecommended(photo) || photo.health.level == HealthLevel.DAMAGED) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = { onRepairHeader?.invoke(photo) },
                                        enabled = !isRepairing && !isRestoring,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .testTag("repair_header_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color(0xFFF59E0B).copy(alpha = 0.15f)
                                        )
                                    ) {
                                        if (isRepairing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = Color(0xFFF59E0B),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Reconstruyendo cabecera...", color = Color(0xFFFBBF24), fontSize = 12.sp)
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Build,
                                                contentDescription = null,
                                                tint = Color(0xFFFBBF24),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Reparar Cabecera Binaria (${photo.fileExtension.uppercase()})",
                                                color = Color(0xFFFBBF24),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                val mainIcon = when {
                                    photo.isVideo -> Icons.Default.Videocam
                                    photo.isAudio -> Icons.Default.Audiotrack
                                    photo.isDocument -> Icons.Default.Description
                                    else -> Icons.Default.Image
                                }
                                DetailRow(
                                    icon = mainIcon,
                                    label = "Nombre",
                                    value = photo.name
                                )
                                DetailRow(
                                    icon = Icons.Default.Storage,
                                    label = "Tamaño",
                                    value = "${photo.formattedSize} (${photo.fileSizeBytes} bytes)"
                                )
                                if ((photo.isVideo || photo.isAudio) && photo.durationFormatted.isNotBlank()) {
                                    DetailRow(
                                        icon = Icons.Default.Timer,
                                        label = "Duración total",
                                        value = photo.durationFormatted
                                    )
                                }
                                if (photo.dimensions != null) {
                                    DetailRow(
                                        icon = if (photo.isDocument) Icons.Default.Description else Icons.Default.Image,
                                        label = if (photo.isDocument) "Tipo de Formato" else "Resolución",
                                        value = photo.dimensions
                                    )
                                }
                                val formattedDate = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(
                                    Date(photo.lastModifiedTimestamp)
                                )
                                DetailRow(icon = Icons.Default.CalendarToday, label = "Fecha Estimada", value = formattedDate)
                                DetailRow(icon = Icons.Default.Folder, label = "Origen", value = photo.sourceCategory.displayName)
                                DetailRow(icon = Icons.Default.Folder, label = "Ruta detectada", value = photo.filePath)
                            }
                        }
                    }

                    // Main Action Button (Restaurar)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val mediaLabel = when {
                            photo.isVideo -> "Video"
                            photo.isAudio -> "Audio"
                            photo.isDocument -> "Documento"
                            else -> "Foto"
                        }
                        Button(
                            onClick = { onRestore(photo) },
                            enabled = !isRestoring && !photo.isRestored,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("preview_restore_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (photo.isRestored) TealAccent else CyanPrimary,
                                disabledContainerColor = if (photo.isRestored) TealAccent.copy(alpha = 0.8f) else CyanPrimary.copy(alpha = 0.4f)
                            )
                        ) {
                            if (isRestoring) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restaurando al Teléfono...", color = Color.Black, fontWeight = FontWeight.Bold)
                            } else if (photo.isRestored) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("¡$mediaLabel Restaurado!", color = Color.Black, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Restore, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restaurar $mediaLabel al Teléfono", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
