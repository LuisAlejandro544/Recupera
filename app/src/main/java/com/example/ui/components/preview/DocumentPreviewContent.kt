package com.example.ui.components.preview

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.extractor.MediaMetadataExtractor
import com.example.model.RecoverablePhoto
import com.example.ui.theme.DarkSurface
import java.io.File
import java.util.Locale

@Composable
fun DocumentPreviewContent(
    photo: RecoverablePhoto,
    mediaFile: File?,
    modifier: Modifier = Modifier
) {
    val ext = photo.fileExtension.lowercase(Locale.ROOT)
    val (docColor, docIcon, docCategory) = when (ext) {
        "pdf" -> Triple(Color(0xFFEF4444), Icons.Default.PictureAsPdf, "Documento PDF")
        "docx", "doc" -> Triple(Color(0xFF3B82F6), Icons.Default.Description, "Documento Word")
        "xlsx", "xls", "csv" -> Triple(Color(0xFF10B981), Icons.Default.TableChart, "Hoja de Cálculo")
        "pptx", "ppt" -> Triple(Color(0xFFF59E0B), Icons.Default.Summarize, "Presentación")
        "epub" -> Triple(Color(0xFF8B5CF6), Icons.Default.MenuBook, "Libro Digital")
        else -> Triple(Color(0xFF00E5FF), Icons.Default.Description, "Archivo de Texto")
    }

    val extractedMeta = if (mediaFile != null && mediaFile.exists()) {
        MediaMetadataExtractor.extractDocumentMetadata(mediaFile)
    } else null

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF020617)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Document Icon Badge
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(docColor.copy(alpha = 0.2f))
                    .border(2.dp, docColor.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = docIcon,
                    contentDescription = docCategory,
                    tint = docColor,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Extension Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(docColor.copy(alpha = 0.9f))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = ext.uppercase(Locale.ROOT),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = photo.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                ),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "$docCategory • ${photo.formattedSize}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            )

            // Text Snippet Preview Card if text/csv file
            if (extractedMeta?.textSnippet != null && extractedMeta.textSnippet.isNotBlank()) {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.85f)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = docColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Vista Previa de Contenido (Primeras Líneas):",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = docColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = extractedMeta.textSnippet,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.9f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
