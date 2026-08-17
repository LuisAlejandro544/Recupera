# 🗺️ Roadmap - Recuperador Pro

Plan de evolución estratégica y técnica para el desarrollo de **Recuperador Pro**.

---

## 📌 Fase 1: Motor Principal y Exclusión de Galería (Completada ✅)

- [x] **Motor de Escaneo Asíncrono**: Escaneo en 4 fases (`MediaStore Trash`, `Thumbnails`, `Hidden/.nomedia`, `Deep Storage`).
- [x] **Filtro de Exclusión de Galería Activa**: Identificación de firmas y huellas de archivos existentes en la galería para mostrar **estrictamente archivos eliminados / residuales**.
- [x] **Soporte de Formatos Multimedia**:
  - Imágenes: `JPG`, `JPEG`, `PNG`, `WEBP`, `HEIC`, `GIF`, `BMP`.
  - Videos: `MP4`, `MKV`, `MOV`, `3GP`, `WEBM`, `AVI`.
- [x] **Detección por Magic Bytes**: Análisis de cabeceras binarias para recuperar archivos huérfanos sin extensión.

---

## 📌 Fase 2: Experiencia de Usuario y Previsualización (Completada ✅)

- [x] **Reproductor de Vista Previa de Video (5 Segundos)**: Reproductor integrado con limitador temporal de 5s, barra de progreso y reinicio rápido.
- [x] **Visor de Fotos Interactivo**: Gestos táctiles de zoom y paneo (hasta 4.5x) en diálogo de pantalla completa.
- [x] **Restauración por Lotes y Unitaria**: Inserción mediante `ContentResolver` en colecciones públicas indexadas (`Pictures/Restored_Photos` y `Movies/Restored_Videos`).
- [x] **Filtrado Avanzado**: Chips de filtro por tipo (Fotos, Videos, Papelera, Miniaturas, Carpetas Ocultas, Almacenamiento Profundo).
- [x] **Búsqueda y Ordenamiento**: Búsqueda en tiempo real por nombre, ordenación por fecha reciente/antigua y tamaño mayor/menor.

---

## 📌 Fase 3: Audio y Medidor de Salud del Archivo (Completada ✅)

- [x] **Recuperación de Audios y Notas de Voz**:
  - Formatos soportados: `MP3`, `AAC`, `M4A`, `OGG`, `OPUS`, `WAV`, `AMR`, `FLAC`.
  - Rastreo en carpetas de WhatsApp Voice Notes (`WhatsApp/Media/WhatsApp Voice Notes`, `WhatsApp Audio`), Telegram Audio y Recordings.
  - Permiso `READ_MEDIA_AUDIO` en Android 13+ y escaneo en papelera de audio.
- [x] **Reproductor de Preview de Audio (5 Segundos)**:
  - Reproductor con `MediaPlayer`, barra de progreso de 5.0s, control de play/pause, reinicio y ecualizador animado.
- [x] **Medidor de Salud del Archivo (File Health Meter)**:
  - Cálculo de porcentaje de salud (10% - 100%) y nivel (`Excelente`, `Bueno`, `Aceptable`, `Dañado`).
  - Badge visual en las tarjetas de la galería y sección de diagnóstico detallado en el visor de pantalla completa.
- [x] **Restauración de Audio**: Inserción automática en `Music/Restored_Audio`.

---

## 📌 Fase 4: CI/CD y Automatización de Código (Completada ✅)

- [x] **GitHub Action para Sincronización desde Archivos Comprimidos**:
  - Flujo `.github/workflows/sync_zip.yml` para procesar archivos `.zip`, `.7z`, `.tar.gz`, `.tar`, `.tgz` subidos a `zip/`.
  - Descompresión, sincronización `rsync` y reemplazo automático de código.
  - Soporte de mensajes de commit personalizados mediante `commit_message.txt`.
  - Enmienda limpia de commit (`--amend`) y force-push automático.

---

## 📌 Fase 5: Documentos de Ofimática y Limpieza de Almacenamiento (Completada ✅)

- [x] **Recuperación de Documentos de Trabajo y Ofimática**:
  - Formatos soportados: `PDF`, `DOCX`, `DOC`, `XLSX`, `XLS`, `CSV`, `PPTX`, `PPT`, `TXT`, `EPUB`, `RTF`.
  - Extracción de metadatos ofimáticos y previsualización de fragmentos de texto (`DocumentPreviewContent`).
  - Mapeo de tipos MIME y restauración automática en `Documents/Restored_Documents`.
  - Escaneo en carpetas de WhatsApp Documents, Telegram Documents, Download y Documents.
  - Clasificación de salud binaria con firmas mágicas (`%PDF`, `PK..`, `D0 CF 11 E0`, `{\rt`).
- [x] **Herramienta de Limpieza de Miniaturas Huérfanas**:
  - `OrphanThumbnailCleaner`: Auditoría y purga de miniaturas residuales en `.thumbnails` sin archivo activo en galería.
  - `OrphanCleanerDialog`: Diálogo interactivo con desglose de archivos residuales y espacio liberable.
  - Acceso directo desde la tarjeta de resumen superior (`OverviewCard`).

---

## 📌 Fase 6: Integración Shizuku y Distribución (Uptodown / Direct APK 🚀)

- [x] **Integración de Shizuku (Privilegios de Sistema / ADB)**:
  - Vinculación reactiva con el Binder de Shizuku (`Shizuku.addBinderReceivedListener`, `Shizuku.addBinderDeadListener`).
  - Detección de estado en tiempo real (Conectado / Desconectado / Sin Autorización) y soporte nativo para `Sui` (Magisk).
  - Flujo de solicitud de permisos en 1-toque (`Shizuku.requestPermission()`).
  - Panel de Ajustes de Shizuku (`ShizukuSettingsDialog`) con guía paso a paso para usuarios móviles sin PC.
  - Switch de activación para el modo de escaneo con privilegios de sistema.
- [x] **Escaneo de Bóvedas de Sistema con Shizuku UserService**:
  - Implementación de `ShizukuScanUserService` (`bindUserService` con `UserServiceArgs`, UID 2000/0) y `ShizukuServiceClient`.
  - Lectura profunda de directorios restringidos en Android 11+ (`Android/data`, WhatsApp, Telegram, papeleras de Samsung Gallery `com.sec.android.gallery3d` y Xiaomi `com.miui.gallery`).
  - Filtrado contra biblioteca activa y cálculo de diagnóstico `FileHealth`.
  - Terminación limpia de procesos remotos con código de transacción `16777115`.
- [ ] **Recuperación de Paquetes de Instalación (APKs)**:
  - Búsqueda de archivos `.apk` antiguos en cachés de descarga y almacenamiento interno.
- [ ] **Detector de Archivos Duplicados**: Análisis hash (MD5/SHA-256) para identificar y eliminar duplicados.
- [ ] **Soporte Multilenguaje Completo**: Español, Inglés, Portugués, Francés.
- [ ] **Optimizaciones R8/ProGuard**: Reducción del tamaño final del APK a menos de 8 MB.
- [ ] **Ficha para Uptodown**: Metadatos, capturas de pantalla de alta resolución y changelog optimizado.
