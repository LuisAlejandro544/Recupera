# 🏛️ Arquitectura y Estructura Modular del Proyecto

Este documento detalla la estructura modular de paquetes, módulos de código desacoplados y el flujo de datos de **Recuperador Pro**.

---

## 📂 Árbol de Directorios Modular

```
/
├── .github/
│   └── workflows/
│       └── sync_zip.yml                                 # GitHub Action para sincronizar código desde archivos comprimidos
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt                      # Punto de entrada y gestión de permisos multimedia
│   │   │   │   ├── engine/                              # ⚙️ Motor modular de recuperación y escaneo
│   │   │   │   │   ├── PhotoRecoveryEngine.kt           # Orquestador del ciclo de escaneo y restauración
│   │   │   │   │   ├── filter/
│   │   │   │   │   │   └── ActiveGalleryFilter.kt       # Exclusión estricta de fotos/videos/audios activos en galería
│   │   │   │   │   ├── health/
│   │   │   │   │   │   └── FileHealthEvaluator.kt       # Diagnóstico de integridad binaria y cálculo de salud
│   │   │   │   │   ├── scan/
│   │   │   │   │   │   ├── TrashMediaScanner.kt         # Escaneo de papelera del sistema (Images/Video/Audio)
│   │   │   │   │   │   └── StorageDirectoryScanner.kt   # Escaneo de miniaturas, WhatsApp, Telegram y deep storage
│   │   │   │   │   └── restore/
│   │   │   │   │       └── MediaRestorer.kt             # Escritura en MediaStore y re-indexación con MediaScanner
│   │   │   │   ├── model/
│   │   │   │   │   └── RecoverablePhoto.kt              # Entidades inmutables, FileHealth y Enums
│   │   │   │   ├── viewmodel/
│   │   │   │   │   └── PhotoRecoveryViewModel.kt        # Gestión reactiva de StateFlow y Corrutinas
│   │   │   │   └── ui/
│   │   │   │       ├── PhotoRecoveryScreen.kt           # Pantalla principal (Scaffold, TopBar, LazyVerticalGrid)
│   │   │   │       ├── components/
│   │   │   │       │   ├── FilterBar.kt                 # Barra de chips de categorías y buscador
│   │   │   │       │   ├── FullscreenPhotoPreview.kt    # Diálogo modal principal de previsualización
│   │   │   │       │   ├── PermissionBanner.kt          # Banner reactivo para solicitar permisos
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
├── ROADMAP.md                                           # Plan de desarrollo y siguientes etapas
├── STRUCTURE.md                                         # Arquitectura y estructura modular
├── ESTRUCTURE.md                                        # Estructura sincronizada
├── AI_CONTEXT.md                                        # Contexto técnico para asistentes de IA
├── AGENTS.md                                            # Roles, instrucciones y buenas prácticas de agentes
├── metadata.json                                        # Metadatos para AI Studio
└── settings.gradle.kts                                  # Configuración del proyecto Gradle
```

---

## 🧩 Descripción de Capas y Módulos

### 1. Capa del Motor (`engine/`)
- **`PhotoRecoveryEngine`**: Orquesta el flujo de escaneo y delega a los módulos especializados manteniendo la cohesión y archivos menores a 200 líneas.
- **`ActiveGalleryFilter`**: Rastrear y memorizar las rutas e identidades de fotos/videos/audios actualmente activos en el almacenamiento normal para **excluirlas de los resultados**.
- **`FileHealthEvaluator`**: Validador de cabeceras binarias (Magic Bytes para JPEG, PNG, MP4, MKV, MP3, OGG, FLAC, AMR, WAV) y motor heurístico de integridad (`EXCELLENT`, `GOOD`, `FAIR`, `DAMAGED`).
- **`TrashMediaScanner`**: Consultas directas a `MediaStore` con `QUERY_ARG_MATCH_TRASHED` para recuperar imágenes, videos y audios de la papelera del sistema.
- **`StorageDirectoryScanner`**: Exploración especializada de carpetas `.thumbnails`, cachés de mensajería (WhatsApp, Telegram) y Deep Storage.
- **`MediaRestorer`**: Escribe el flujo de bytes de vuelta al `MediaStore` con `IS_PENDING = 0` y ejecuta `MediaScannerConnection` para visibilidad inmediata en el teléfono.

---

### 2. Capa de Previsualización (`ui/components/preview/`)
- **`AudioPreviewPlayer`**: Reproductor multimedia acotado a 5 segundos con ecualizador animado de 5 barras, control de pausa/reproducción, reinicio y contador en tiempo real.
- **`VideoPreviewPlayer`**: Reproductor integrado con `VideoView` nativo, limitador de 5s, reinicio y barra de progreso.
- **`ImagePreviewContent`**: Visor de imágenes de alta resolución con soporte para gestos multitáctiles (zoom de 1x a 4.5x y paneo).
- **`FileHealthMeterSection`**: Indicador visual y porcentaje de salud con colores dinámicos según el nivel de integridad.
- **`DetailRow`**: Componente de diseño limpio para mostrar metadatos técnicos del archivo.

---

### 3. Capa de Pantalla Principal (`ui/components/screen/`)
- **`OverviewCard`**: Tarjeta de resumen de archivos detectados, espacio recuperable y accesos rápidos a Escaneo Rápido y Profundo.
- **`BatchRestoreBar`**: Barra flotante animada que calcula el tamaño acumulado y ejecuta la restauración por lotes.
- **`EmptyStateCard`**: Componente visual amigable con acciones para limpiar búsqueda o iniciar escaneo profundo.
