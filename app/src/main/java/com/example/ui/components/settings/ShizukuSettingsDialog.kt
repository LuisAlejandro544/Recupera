package com.example.ui.components.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.shizuku.ShizukuState
import com.example.shizuku.ShizukuStatus
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.TealAccent

@Composable
fun ShizukuSettingsDialog(
    shizukuState: ShizukuState,
    onRequestPermission: () -> Unit,
    onRefreshStatus: () -> Unit,
    onToggleEnhancedScan: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(24.dp))
                .background(DarkBackground)
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
                .testTag("shizuku_settings_dialog"),
            color = DarkBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyanPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Integración Shizuku",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Privilegios ADB / Sistema",
                                color = CyanPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_shizuku_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Estado Actual Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (shizukuState.status) {
                            ShizukuStatus.AUTHORIZED_ACTIVE -> EmeraldGreen.copy(alpha = 0.10f)
                            ShizukuStatus.RUNNING_UNAUTHORIZED -> AmberWarning.copy(alpha = 0.10f)
                            ShizukuStatus.NOT_RUNNING -> DarkSurface
                        }
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            when (shizukuState.status) {
                                ShizukuStatus.AUTHORIZED_ACTIVE -> EmeraldGreen.copy(alpha = 0.5f)
                                ShizukuStatus.RUNNING_UNAUTHORIZED -> AmberWarning.copy(alpha = 0.5f)
                                ShizukuStatus.NOT_RUNNING -> Color(0xFF334155)
                            }
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (shizukuState.status) {
                                    ShizukuStatus.AUTHORIZED_ACTIVE -> Icons.Default.CheckCircle
                                    ShizukuStatus.RUNNING_UNAUTHORIZED -> Icons.Default.FlashOn
                                    ShizukuStatus.NOT_RUNNING -> Icons.Default.ErrorOutline
                                },
                                contentDescription = null,
                                tint = when (shizukuState.status) {
                                    ShizukuStatus.AUTHORIZED_ACTIVE -> EmeraldGreen
                                    ShizukuStatus.RUNNING_UNAUTHORIZED -> AmberWarning
                                    ShizukuStatus.NOT_RUNNING -> Color(0xFF94A3B8)
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (shizukuState.status) {
                                    ShizukuStatus.AUTHORIZED_ACTIVE -> "Servicio Vinculado y Activo"
                                    ShizukuStatus.RUNNING_UNAUTHORIZED -> "Shizuku Activo (Sin Permiso)"
                                    ShizukuStatus.NOT_RUNNING -> "Shizuku No Detectado"
                                },
                                color = when (shizukuState.status) {
                                    ShizukuStatus.AUTHORIZED_ACTIVE -> EmeraldGreen
                                    ShizukuStatus.RUNNING_UNAUTHORIZED -> AmberWarning
                                    ShizukuStatus.NOT_RUNNING -> Color(0xFFCBD5E1)
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = when (shizukuState.status) {
                                ShizukuStatus.AUTHORIZED_ACTIVE ->
                                    "Recuperador Pro cuenta con autorización para acceder a directorios de sistema y papeleras protegidas con identidad ${shizukuState.uidLabel} (v${shizukuState.version})."
                                ShizukuStatus.RUNNING_UNAUTHORIZED ->
                                    "El servicio de Shizuku está corriendo en tu teléfono, pero aún no has otorgado permisos a Recuperador Pro."
                                ShizukuStatus.NOT_RUNNING ->
                                    "No se detectó el servicio de Shizuku en ejecución. Puedes iniciarlo directamente desde la app Shizuku mediante Depuración Inalámbrica (sin PC)."
                            },
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons based on status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (shizukuState.status == ShizukuStatus.RUNNING_UNAUTHORIZED) {
                                Button(
                                    onClick = onRequestPermission,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("request_shizuku_permission_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Autorizar Shizuku",
                                        color = Color.Black,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = onRefreshStatus,
                                modifier = Modifier
                                    .weight(if (shizukuState.status == ShizukuStatus.RUNNING_UNAUTHORIZED) 0.8f else 1f)
                                    .height(40.dp)
                                    .testTag("refresh_shizuku_status_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Comprobar",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Switch de Escaneo Mejorado
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
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
                                text = "Escaneo Profundo con Shizuku",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Inspecciona carpetas Android/data y papeleras de fabricantes (Samsung, Xiaomi, etc.)",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Switch(
                            checked = shizukuState.isEnhancedScanEnabled,
                            onCheckedChange = { onToggleEnhancedScan(it) },
                            enabled = shizukuState.status == ShizukuStatus.AUTHORIZED_ACTIVE,
                            modifier = Modifier.testTag("toggle_shizuku_enhanced_scan_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CyanPrimary,
                                uncheckedThumbColor = Color(0xFF64748B),
                                uncheckedTrackColor = Color(0xFF334155)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Guía Rápida para el Usuario (Sin PC)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.7f)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF334155))
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = TealAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "¿Cómo activar Shizuku en tu móvil sin PC?",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        StepItem(number = "1", text = "Instala la app Shizuku en tu teléfono (APK / F-Droid / GitHub).")
                        StepItem(number = "2", text = "Activa 'Opciones de desarrollador' y 'Depuración inalámbrica' en Ajustes.")
                        StepItem(number = "3", text = "En Shizuku, toca 'Vincular' e introduce el código de emparejamiento.")
                        StepItem(number = "4", text = "Toca 'Iniciar' en Shizuku y regresa a esta app para autorizarla.")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("dismiss_shizuku_dialog_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Entendido",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun StepItem(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(CyanPrimary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = CyanPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = Color(0xFFCBD5E1),
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}
