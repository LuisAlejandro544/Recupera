package com.example.shizuku

enum class ShizukuStatus {
    NOT_RUNNING,           // Shizuku no está en ejecución o no está instalado
    RUNNING_UNAUTHORIZED,  // Shizuku activo, pero permiso pendiente de autorizar
    AUTHORIZED_ACTIVE      // Shizuku activo con permisos de ADB/Sistema concedidos
}

data class ShizukuState(
    val status: ShizukuStatus = ShizukuStatus.NOT_RUNNING,
    val isBinderAlive: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val version: Int = 0,
    val uid: Int = -1,
    val isSui: Boolean = false,
    val isEnhancedScanEnabled: Boolean = false
) {
    val isReadyForEnhancedScan: Boolean
        get() = isBinderAlive && isPermissionGranted && isEnhancedScanEnabled

    val uidLabel: String
        get() = when {
            isSui -> "Sui (Magisk Root)"
            uid == 0 -> "Root (Superusuario)"
            uid == 2000 -> "ADB (Shell de Sistema)"
            uid == -1 -> "Desconectado"
            else -> "UID: $uid"
        }
}
