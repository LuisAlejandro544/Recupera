package com.example.ui.components

import android.media.MediaPlayer
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.FileHealth
import com.example.model.RecoverablePhoto
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.TealAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FullscreenPhotoPreview(
    photo: RecoverablePhoto,
    isRestoring: Boolean,
    onDismiss: () -> Unit,
    onRestore: (RecoverablePhoto) -> Unit
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
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
                    else -> {
                        // Interactive Zoomable Photo Area
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(1f, 4.5f)
                                        if (scale > 1f) {
                                            offsetX += pan.x
                                            offsetY += pan.y
                                        } else {
                                            offsetX = 0f
                                            offsetY = 0f
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val imageModel = mediaFile ?: photo.contentUri
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(imageModel)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = photo.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offsetX,
                                        translationY = offsetY
                                    )
                            )
                        }
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
                                Spacer(modifier = Modifier.height(12.dp))

                                val mainIcon = when {
                                    photo.isVideo -> Icons.Default.Videocam
                                    photo.isAudio -> Icons.Default.Audiotrack
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
                                        icon = Icons.Default.Image,
                                        label = "Resolución",
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

@Composable
fun FileHealthMeterSection(health: FileHealth) {
    val levelColor = Color(health.level.colorHex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, levelColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = levelColor.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = "Salud del Archivo",
                        tint = levelColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Medidor de Salud",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(levelColor.copy(alpha = 0.25f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${health.percentage}% • ${health.level.label}",
                        color = levelColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { health.percentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = levelColor,
                trackColor = Color.White.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = health.description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun AudioPreviewPlayer(
    photo: RecoverablePhoto,
    mediaFile: File?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentProgressMs by remember { mutableIntStateOf(0) }
    var maxPreviewMs by remember { mutableIntStateOf(5000) }
    var isPrepared by remember { mutableStateOf(false) }

    val audioUri = remember(photo, mediaFile) {
        if (mediaFile != null && mediaFile.exists()) {
            Uri.fromFile(mediaFile)
        } else {
            photo.contentUri
        }
    }

    // Audio equalizer animation
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
    val barScale1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val barScale2 by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val barScale3 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    // Initialize MediaPlayer
    DisposableEffect(audioUri) {
        val player = MediaPlayer().apply {
            try {
                if (mediaFile != null && mediaFile.exists()) {
                    setDataSource(mediaFile.absolutePath)
                } else if (audioUri != null) {
                    setDataSource(context, audioUri)
                }
                setOnPreparedListener { mp ->
                    val dur = mp.duration
                    if (dur in 1..4999) {
                        maxPreviewMs = dur
                    } else {
                        maxPreviewMs = 5000
                    }
                    isPrepared = true
                    mp.start()
                    isPlaying = true
                }
                setOnCompletionListener {
                    seekTo(0)
                    isPlaying = false
                    currentProgressMs = 0
                }
                setOnErrorListener { _, _, _ ->
                    isPlaying = false
                    true
                }
                prepareAsync()
            } catch (_: Exception) {}
        }
        mediaPlayer = player

        onDispose {
            try {
                if (player.isPlaying) player.stop()
                player.release()
            } catch (_: Exception) {}
            mediaPlayer = null
        }
    }

    // Monitor 5-second playback window
    LaunchedEffect(isPlaying, isPrepared) {
        while (isActive && isPlaying && mediaPlayer != null) {
            val current = try { mediaPlayer?.currentPosition ?: 0 } catch (_: Exception) { 0 }
            currentProgressMs = current

            if (current >= maxPreviewMs) {
                try {
                    mediaPlayer?.pause()
                    mediaPlayer?.seekTo(0)
                } catch (_: Exception) {}
                isPlaying = false
                currentProgressMs = 0
            }
            delay(100)
        }
    }

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF020617),
                        Color.Black
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Audio Artwork & Equalizer Visualizer
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                    .border(2.dp, Color(0xFFF59E0B).copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Audiotrack,
                    contentDescription = "Pista de Audio",
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Audio File Name
            Text(
                text = photo.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${photo.fileExtension.uppercase()} • ${photo.formattedSize}",
                color = Color(0xFFF59E0B),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Equalizer Bars
            Row(
                modifier = Modifier.height(36.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val scales = listOf(barScale1, barScale2, barScale3, barScale2, barScale1)
                scales.forEach { s ->
                    val heightFactor = if (isPlaying) s else 0.2f
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(36.dp * heightFactor)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFFF59E0B))
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Play / Restart Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Restart button
                IconButton(
                    onClick = {
                        try {
                            mediaPlayer?.seekTo(0)
                            mediaPlayer?.start()
                            isPlaying = true
                            currentProgressMs = 0
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Reiniciar Audio",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Play / Pause Button
                IconButton(
                    onClick = {
                        try {
                            if (isPlaying) {
                                mediaPlayer?.pause()
                                isPlaying = false
                            } else {
                                if (currentProgressMs >= maxPreviewMs) {
                                    mediaPlayer?.seekTo(0)
                                }
                                mediaPlayer?.start()
                                isPlaying = true
                            }
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B))
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5-second indicator box
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val progressFraction = (currentProgressMs.toFloat() / maxPreviewMs.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color(0xFFF59E0B),
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val curSec = currentProgressMs / 1000f
                    val maxSec = maxPreviewMs / 1000f
                    Text(
                        text = String.format(Locale.getDefault(), "Preview de Audio 5s: %.1fs / %.1fs", curSec, maxSec),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoPreviewPlayer(
    photo: RecoverablePhoto,
    mediaFile: File?,
    modifier: Modifier = Modifier
) {
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentProgressMs by remember { mutableIntStateOf(0) }
    var maxPreviewMs by remember { mutableIntStateOf(5000) }
    var isPrepared by remember { mutableStateOf(false) }

    val videoUri = remember(photo, mediaFile) {
        if (mediaFile != null && mediaFile.exists()) {
            Uri.fromFile(mediaFile)
        } else {
            photo.contentUri
        }
    }

    // Monitor 5-second playback window
    LaunchedEffect(isPlaying, isPrepared) {
        while (isActive && isPlaying && videoViewRef != null) {
            val current = videoViewRef?.currentPosition ?: 0
            currentProgressMs = current

            if (current >= maxPreviewMs) {
                videoViewRef?.pause()
                videoViewRef?.seekTo(0)
                isPlaying = false
                currentProgressMs = 0
            }
            delay(100)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoViewRef?.stopPlayback()
            videoViewRef = null
        }
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Video Surface
        if (videoUri != null) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setVideoURI(videoUri)
                        setOnPreparedListener { mp ->
                            mp.isLooping = false
                            val dur = mp.duration
                            if (dur in 1..4999) {
                                maxPreviewMs = dur
                            } else {
                                maxPreviewMs = 5000
                            }
                            isPrepared = true
                            start()
                            isPlaying = true
                        }
                        setOnCompletionListener {
                            seekTo(0)
                            isPlaying = false
                            currentProgressMs = 0
                        }
                        setOnErrorListener { _, _, _ ->
                            isPlaying = false
                            true
                        }
                    }.also { videoViewRef = it }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Overlay Controls for 5-Second Preview
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Play / Pause Circle
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Restart button
                IconButton(
                    onClick = {
                        videoViewRef?.seekTo(0)
                        videoViewRef?.start()
                        isPlaying = true
                        currentProgressMs = 0
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Reiniciar Preview",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Play / Pause Button
                IconButton(
                    onClick = {
                        if (isPlaying) {
                            videoViewRef?.pause()
                            isPlaying = false
                        } else {
                            if (currentProgressMs >= maxPreviewMs) {
                                videoViewRef?.seekTo(0)
                            }
                            videoViewRef?.start()
                            isPlaying = true
                        }
                    },
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(CyanPrimary)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 5-second indicator box
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.75f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .border(1.dp, CyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val progressFraction = (currentProgressMs.toFloat() / maxPreviewMs.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = CyanPrimary,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val curSec = currentProgressMs / 1000f
                    val maxSec = maxPreviewMs / 1000f
                    Text(
                        text = String.format(Locale.getDefault(), "Preview de Video 5s: %.1fs / %.1fs", curSec, maxSec),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
