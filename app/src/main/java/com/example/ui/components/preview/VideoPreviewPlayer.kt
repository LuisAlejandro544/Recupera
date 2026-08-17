package com.example.ui.components.preview

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.RecoverablePhoto
import com.example.ui.theme.CyanPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.util.Locale

@Composable
fun VideoPreviewPlayer(
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
