package com.example.ui.components.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RecoverablePhoto
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkSurface

@Composable
fun BatchRestoreBar(
    selectedPhotos: List<RecoverablePhoto>,
    isRestoring: Boolean,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedBytes = selectedPhotos.sumOf { it.fileSizeBytes }
    val selectedSizeText = if (selectedBytes >= 1024 * 1024) {
        String.format("%.1f MB", selectedBytes / (1024.0 * 1024.0))
    } else {
        String.format("%.0f KB", selectedBytes / 1024.0)
    }

    AnimatedVisibility(
        visible = selectedPhotos.isNotEmpty(),
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
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
                    onClick = onRestore,
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
}
