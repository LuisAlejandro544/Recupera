package com.example.ui.components.preview

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RecoverablePhoto
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.util.Locale

@Composable
fun AudioPreviewPlayer(
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
