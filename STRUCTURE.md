# 🏛️ Arquitectura y Estructura del Proyecto

Este documento detalla la estructura de carpetas, módulos de código y el flujo de datos de **Recuperador Pro**.

---

## 📂 Árbol de Directorios

```
/
├── .github/
│   └── workflows/
│       └── sync_zip.yml                                 # GitHub Action para extraer y sincronizar código desde archivos zip
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt                      # Punto de entrada y gestión de permisos multimedia
│   │   │   │   ├── engine/
│   │   │   │   │   └── PhotoRecoveryEngine.kt           # Motor de escaneo, exclusión de galería, salud y restauración
│   │   │   │   ├── model/
│   │   │   │   │   └── RecoverablePhoto.kt              # Modelos de datos, FileHealth, Enums de fuentes y filtros
│   │   │   │   ├── viewmodel/
│   │   │   │   │   └── PhotoRecoveryViewModel.kt        # Gestión de estados reactivos con StateFlow y Corrutinas
│   │   │   │   └── ui/
│   │   │   │       ├── PhotoRecoveryScreen.kt           # Pantalla principal con estadísticas y cuadrícula
│   │   │   │       ├── components/
│   │   │   │       │   ├── FilterBar.kt                 # Barra de chips de categorías (Fotos, Videos, Audios, etc.)
│   │   │   │       │   ├── FullscreenPhotoPreview.kt    # Visor interactivo, preview 5s de Video/Audio y Medidor de Salud
│   │   │   │       │   ├── PermissionBanner.kt          # Banner reactivo para solicitar permisos
│   │   │   │       │   ├── PhotoCard.kt                 # Tarjeta de elemento multimedia con badge de salud y checkbox
│   │   │   │       │   ├── RestoreSuccessDialog.kt      # Diálogo de confirmación de restauración
│   │   │   │       │   └── ScanProgressBanner.kt        # Banner de progreso de escaneo en tiempo real
│   │   │   │       └── theme/
│   │   │   │           ├── Color.kt                     # Paleta de colores Dark Cyan / Teal Premium
│   │   │   │           ├── Theme.kt                     # Configuración de Material 3 Dark Theme
│   │   │   │           └── Type.kt                      # Tipografía de la aplicación
│   │   │   ├── res/                                     # Recursos XML, drawables y strings
│   │   │   └── AndroidManifest.xml                      # Declaración de permisos de almacenamiento e imágenes/videos/audios
│   │   └── test/java/com/example/
│   │       ├── ExampleRobolectricTest.kt                # Tests unitarios del modelo, formateo y cálculo de salud
│   │       └── ExampleUnitTest.kt
│   └── build.gradle.kts                                 # Dependencias y configuración de compilación de la app
├── gradle/
│   └── libs.versions.toml                               # Catálogo de versiones centralizado
├── zip/
│   └── .gitkeep                                         # Directorio receptor de archivos comprimidos (.zip, .7z, .tar.gz)
├── commit_message.txt                                   # Archivo opcional para mensaje de commit automatizado
├── README.md                                            # Documentación general del proyecto
├── ROADMAP.md                                           # Plan de desarrollo y siguientes etapas
├── STRUCTURE.md                                         # Este documento de arquitectura
├── AI_CONTEXT.md                                        # Contexto técnico para asistentes de IA
├── AGENTS.md                                            # Roles, instrucciones y buenas prácticas de agentes
├── metadata.json                                        # Metadatos para AI Studio
└── settings.gradle.kts                                  # Configuración del proyecto Gradle
```

---

## 🧩 Descripción de Capas

### 1. Capa de Datos (`model/`)
- **`RecoverablePhoto`**: Entidad inmutable que representa un archivo recuperable. Contiene `id`, `name`, `filePath`, `contentUri`, `fileSizeBytes`, `sourceCategory`, `mediaType` (IMAGE / VIDEO / AUDIO), `durationMs`, `dimensions`, `health` (porcentaje y nivel de integridad) y estados de selección/restauración.
- **`FileHealth` & `HealthLevel`**: Modelo de evaluación de integridad:
  - `EXCELLENT` (95-100%): Archivo íntegro en papelera del sistema con metadatos completos.
  - `GOOD` (75-94%): Archivo en caché o mensajería con cabecera intacta.
  - `FAIR` (50-74%): Miniatura o archivo con resolución reducida.
  - `DAMAGED` (10-49%): Fragmento con cabecera truncada o tamaño mínimo.
- **`RecoverySource`**: Enum con las 5 fuentes detectadas (`TRASH_MEDIASTORE`, `THUMBNAILS_CACHE`, `HIDDEN_VAULT`, `APP_TEMP_CACHE`, `DEEP_STORAGE`).
- **`CategoryFilter`** y **`SortOption`**: Enums para el control de filtrado (`ALL`, `PHOTOS`, `VIDEOS`, `AUDIOS`, etc.) y ordenación.

---

### 2. Capa del Motor (`engine/`)
- **`PhotoRecoveryEngine`**:
  - `loadActiveGallerySignatures()`: Rastrear y memorizar las rutas e identidades de fotos/videos/audios actualmente en el almacenamiento normal para **excluirlas de los resultados**.
  - `performScan(deepScan)`: Orquesta las 4 fases de escaneo asíncrono emitiendo progreso mediante lambdas suspendidas.
  - `checkMediaMagicBytes()`: Validador de cabeceras binarias (JPEG, PNG, MP4, MKV, MP3, OGG, FLAC, AMR, WAV, etc.).
  - `calculateFileHealth()`: Motor heurístico que evalúa el estado del archivo según su origen, tamaño, duración y validez de cabecera.
  - `restorePhoto()`: Escribe el flujo de bytes de vuelta al `MediaStore` (`Images`, `Video`, `Audio`) con `IS_PENDING = 0` y ejecuta `MediaScannerConnection` para que aparezca instantáneamente en el teléfono.

---

### 3. Capa de Presentación (`viewmodel/` y `ui/`)
- **`PhotoRecoveryViewModel`**:
  - Expondrá `StateFlow` reactivos: `rawPhotos`, `displayedPhotos`, `scanProgress`, `selectedPhotoIds`, `activeFilter`, `activeSort`, `searchQuery`, `selectedPreviewPhoto`, `isRestoring`, `restoreSummary`.
  - Concatena las transformaciones reactivas mediante `combine(...)` para que los filtros y búsquedas respondan a 60 FPS sin bloquear el hilo principal.
- **Jetpack Compose UI**:
  - Componentes modulares y reutilizables en `ui/components/`.
  - Preview de Video (5s) y Preview de Audio (5s con ecualizador animado y control de reproducción).
  - Medidor de salud integrado en las tarjetas de cuadrícula y ficha técnica detallada.

---

### 4. Automatización y CI/CD (`.github/workflows/`)
- **`sync_zip.yml`**: Flujo de GitHub Actions que permite subir archivos comprimidos (`.zip`, `.7z`, `.tar.gz`, `.tar`, `.tgz`) a la carpeta `zip/`, descomprimirlos automáticamente, sincronizar y sobreescribir el código base con `rsync`, eliminar el archivo temporal y realizar un commit enmendado (`--amend`) con force-push para mantener un árbol de Git limpio.

---

## 🔄 Flujo de Datos

```
[ Sistema de Archivos / MediaStore ]
               │
               ▼
[ PhotoRecoveryEngine ] ── (Filtra archivos activos en Galería / Calcula Salud)
               │
               ▼ Emite lista de fotos/videos/audios borrados con FileHealth
[ PhotoRecoveryViewModel ] ── (StateFlow / Coroutines)
               │
               ▼ Observado reactivamente
[ Jetpack Compose UI ] ──> [ Previsualiza 5s / Evalúa Salud / Pulsa Restaurar ]
               │
               ▼
[ PhotoRecoveryEngine.restorePhoto() ] ──> [ MediaStore / Music / Pictures / Movies ]
```
