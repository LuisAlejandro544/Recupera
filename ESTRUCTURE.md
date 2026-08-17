# 🏛️ Arquitectura y Estructura Modular del Proyecto

Este documento detalla la estructura modular de paquetes, módulos de código desacoplados y el flujo de datos de **Recuperador Pro**.

---

## 📂 Árbol de Directorios Modular

```
/
├── .github/
│   └── workflows/
│       ├── build_apk_debug.yml                          # Compilación de APK Debug y entrega remota directa (hasta 2GB)
│       └── sync_zip.yml                                 # GitHub Action para sincronizar código desde archivos comprimidos
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt                      # Punto de entrada y gestión adaptativa de permisos
│   │   │   │   ├── permission/                          # 🔐 Gestión de permisos de almacenamiento
│   │   │   │   │   └── StoragePermissionManager.kt      # Verificación de permisos Scoped Storage y All Files Access (API 30+)
│   │   │   │   ├── engine/                              # ⚙️ Motor modular de recuperación y escaneo
│   │   │   │   │   ├── PhotoRecoveryEngine.kt           # Orquestador del ciclo de escaneo y restauración
│   │   │   │   │   ├── extractor/
│   │   │   │   │   │   └── MediaMetadataExtractor.kt    # Extracción desacoplada de metadatos (dimensiones, duración, rotación)
│   │   │   │   │   ├── filter/
│   │   │   │   │   │   └── ActiveGalleryFilter.kt       # Exclusión estricta de fotos/videos/audios activos en galería
│   │   │   │   │   ├── health/
│   │   │   │   │   │   └── FileHealthEvaluator.kt       # Diagnóstico de integridad binaria y cálculo de salud
│   │   │   │   │   ├── scan/
│   │   │   │   │   │   ├── StorageDirectoryScanner.kt   # Fachada coordinadora de escaneo en almacenamiento
│   │   │   │   │   │   ├── ThumbnailCacheScanner.kt     # Escaneo de carpetas .thumbnails y cachés de miniaturas
│   │   │   │   │   │   ├── MessagingAppScanner.kt       # Escaneo de WhatsApp (Audio, Video, Statuses), Telegram y Recordings
│   │   │   │   │   │   ├── DeepStorageScanner.kt        # Escaneo profundo de disco recursivo
│   │   │   │   │   │   ├── TrashMediaScanner.kt         # Fachada de papelera del sistema MediaStore
│   │   │   │   │   │   └── trash/
│   │   │   │   │   │       └── MediaStoreTrashQueryHelper.kt # Ejecutor genérico de consultas MediaStore con MATCH_ONLY
│   │   │   │   │   └── restore/
│   │   │   │   │       ├── MediaRestorer.kt             # Escritura en MediaStore y sincronización con MediaScannerConnection
│   │   │   │   │       ├── MimeTypeResolver.kt          # Resolución de tipos MIME y wildcards de escaneo
│   │   │   │   │       └── MediaDestinationResolver.kt  # Resolución de rutas públicas de restauración y nombres limpios
│   │   │   │   ├── model/
│   │   │   │   │   └── RecoverablePhoto.kt              # Entidades inmutables, FileHealth y Enums
│   │   │   │   ├── viewmodel/
│   │   │   │   │   └── PhotoRecoveryViewModel.kt        # Gestión reactiva de StateFlow y Corrutinas
│   │   │   │   └── ui/
│   │   │   │       ├── PhotoRecoveryScreen.kt           # Pantalla principal (Scaffold, TopBar, LazyVerticalGrid)
│   │   │   │       ├── components/
│   │   │   │       │   ├── FilterBar.kt                 # Barra de chips de categorías y buscador
│   │   │   │       │   ├── FullscreenPhotoPreview.kt    # Diálogo modal principal de previsualización
│   │   │   │       │   ├── PhotoCard.kt                 # Tarjeta de elemento multimedia con badge de salud y checkbox
│   │   │   │       │   ├── RestoreSuccessDialog.kt      # Diálogo de confirmación de restauración
│   │   │   │       │   ├── ScanProgressBanner.kt        # Banner de progreso de escaneo en tiempo real
│   │   │   │       │   ├── preview/                         # 🎬 Módulos de visualización y reproductores
│   │   │   │       │   │   ├── AudioPreviewPlayer.kt        # Reproductor 5s con ecualizador animado y reinicio
│   │   │   │       │   │   ├── VideoPreviewPlayer.kt        # Reproductor 5s con VideoView y control de loop
│   │   │   │       │   │   ├── ImagePreviewContent.kt       # Visor de fotos zoomable con gestos multitáctiles
│   │   │   │       │   │   ├── FileHealthMeterSection.kt    # Tarjeta de diagnóstico y barra de salud
│   │   │   │       │   │   └── DetailRow.kt                 # Fila reutilizable para metadatos técnicos
│   │   │   │       │   └── screen/                          # 📱 Componentes estructurales de pantalla
│   │   │   │       │       ├── OverviewCard.kt              # Tarjeta superior de métricas y botones de escaneo
│   │   │   │       │       ├── BatchRestoreBar.kt           # Barra flotante inferior para restauración por lotes
│   │   │   │       │       └── EmptyStateCard.kt            # Estado vacío para filtros o búsquedas sin resultados
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
├── commit_message.txt                                   # Registro del mensaje de commit con detalles del cambio
├── README.md                                            # Documentación general del proyecto
├── CONTRIBUTING.md                                      # Política de contribuciones del proyecto
├── ROADMAP.md                                           # Plan de desarrollo y siguientes etapas
├── ESTRUCTURE.md                                        # Estructura modular sincronizada
├── AI_CONTEXT.md                                        # Contexto técnico para asistentes de IA
├── AGENTS.md                                            # Roles, instrucciones y buenas prácticas de agentes
├── metadata.json                                        # Metadatos para AI Studio
└── settings.gradle.kts                                  # Configuración del proyecto Gradle
```

---

## 🧩 Descripción de Capas y Módulos

### 1. Capa del Motor (`engine/`)
- **`PhotoRecoveryEngine`**: Orquesta el flujo de escaneo delegando a módulos especializados manteniéndose por debajo de 150 líneas.
- **`MediaMetadataExtractor`**: Módulo desacoplado para extraer dimensiones, rotación y duraciones de fotos, videos y audios sin sobrecargar la memoria RAM.
- **`ActiveGalleryFilter`**: Indexa firmas de archivos activos en la galería para **excluirlos estrictamente de los resultados recuperables**.
- **`FileHealthEvaluator`**: Validador de cabeceras binarias (Magic Bytes para JPEG, PNG, MP4, MKV, MP3, OGG, FLAC, AMR, WAV) y motor heurístico de integridad (`EXCELLENT`, `GOOD`, `FAIR`, `DAMAGED`).
- **`StorageDirectoryScanner`**: Fachada coordinadora que delega en los sub-escáneres:
  - `ThumbnailCacheScanner`: Rastreo en `.thumbnails` de DCIM, Pictures, Movies y cachés de aplicaciones.
  - `MessagingAppScanner`: Rastreo en carpetas de WhatsApp (`WhatsApp Voice Notes`, `WhatsApp Audio`, `.Statuses`), Telegram y grabaciones.
  - `DeepStorageScanner`: Exploración profunda y recursiva de directorios respetando exclusiones de seguridad.
- **`TrashMediaScanner` & `MediaStoreTrashQueryHelper`**: Consultas parametrizadas a `MediaStore` con `QUERY_ARG_MATCH_TRASHED` para recuperar imágenes, videos y audios de la papelera nativa de Android (API 30+).
- **`MediaRestorer`**: Flujo de restauración modularizado apoyado en:
  - `MimeTypeResolver`: Mapeo riguroso de tipos MIME e identificadores de indexación.
  - `MediaDestinationResolver`: Resolución de carpetas públicas (`Pictures/Restored_Photos`, `Movies/Restored_Videos`, `Music/Restored_Audio`) y nombres de archivo limpios.

---

### 2. Capa de Permisos (`permission/`)
- **`StoragePermissionManager`**: Abstracción para verificar y solicitar permisos Scoped Storage según versión de Android (API 29 a API 34+) y creación de intents para "Acceso a todos los archivos" (`MANAGE_EXTERNAL_STORAGE`).

---

### 3. Capa de Previsualización (`ui/components/preview/`)
- **`AudioPreviewPlayer`**: Reproductor multimedia acotado a 5 segundos con ecualizador animado de 5 barras, control de pausa/reproducción, reinicio y contador en tiempo real.
- **`VideoPreviewPlayer`**: Reproductor integrado con `VideoView` nativo, limitador de 5s, reinicio y barra de progreso.
- **`ImagePreviewContent`**: Visor de imágenes de alta resolución con soporte para gestos multitáctiles (zoom de 1x a 4.5x y paneo).
- **`FileHealthMeterSection`**: Indicador visual y porcentaje de salud con colores dinámicos según el nivel de integridad.
- **`DetailRow`**: Componente de diseño limpio para mostrar metadatos técnicos del archivo.

---

### 4. Capa de Pantalla Principal (`ui/components/screen/`)
- **`OverviewCard`**: Tarjeta de resumen de archivos detectados, espacio recuperable y accesos rápidos a Escaneo Rápido y Profundo.
- **`BatchRestoreBar`**: Barra flotante animada que calcula el tamaño acumulado y ejecuta la restauración por lotes.
- **`EmptyStateCard`**: Componente visual amigable con acciones para limpiar búsqueda o iniciar escaneo profundo.
