# 🧠 Contexto de Inteligencia Artificial (AI_CONTEXT.md)

Este archivo proporciona contexto técnico crítico para cualquier modelo de lenguaje o agente autónomo que trabaje, mantenga o extienda el código base de **Recuperador Pro**.

---

## 🎯 Regla Fundamental de Negocio

> **CRÍTICO: NO MOSTRAR ARCHIVOS ACTIVOS DE LA GALERÍA NI REPRODUCTORES**
> La aplicación está diseñada para recuperar **fotos, videos, notas de voz, pistas de audio y documentos de trabajo eliminados, perdidos, residuales o en papelera**.
> Bajo ninguna circunstancia se deben mostrar fotos, videos, canciones o documentos que el usuario ya tenga guardados y visibles en su galería normal o almacenamiento activo.
> Cualquier nuevo método de escaneo DEBE verificar y respetar `loadActiveGallerySignatures()` y `isFileInActiveGallery()`.

---

## 📱 Contexto de la Plataforma y Entorno

1. **Dispositivo del Usuario**: El usuario opera directamente desde un **teléfono móvil Android** (sin PC de escritorio). La app debe ser instalable directamente mediante archivo `.apk` o a través de **Uptodown / tiendas de terceros**.
2. **Entorno de Compilación**: Google AI Studio Cloud Container con Kotlin 2.0+ y Jetpack Compose.
3. **Restricciones de Sandbox en Android (API 29 a 34+)**:
   - Para Android 10 (API 29) a Android 14+ (API 34+), el acceso a la papelera del sistema se realiza con `MediaStore.QUERY_ARG_MATCH_TRASHED = MediaStore.MATCH_ONLY`.
   - La inserción de archivos recuperados debe usar `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`, `MediaStore.Video.Media.EXTERNAL_CONTENT_URI`, `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` o `MediaStore.Files.getContentUri("external")` estableciendo `RELATIVE_PATH` en `Pictures/Restored_Photos`, `Movies/Restored_Videos`, `Music/Restored_Audio` o `Documents/Restored_Documents`, marcando `IS_PENDING = 0` al finalizar la copia de bytes.

---

## 🛠️ Tecnologías y Decisiones de Stack

- **¿Por qué NO C++ / NDK?**: En Android sin root, el acceso a archivos de bajo nivel está restringido por SELinux. Las APIs de Kotlin/Java interactúan directamente con el framework del sistema operativo, el cual ya está implementado en C++ nativo.
- **¿Por qué NO Rust?**: Añade overhead de compilación y mayor tamaño de APK sin ventajas de I/O en almacenamiento estándar.
- **¿Por qué NO Python?**: Aumentaría el tamaño del APK en más de 20 MB y degradaría drásticamente el tiempo de inicio y el consumo de batería.
- **Kotlin + Jetpack Compose**: Máxima fluidez a 60/120 Hz, peso mínimo del APK y compatibilidad nativa con Material Design 3.

---

## 🔍 Reglas de Implementación para Nuevas Funcionalidades

1. **Escaneo Asíncrono**: Todo el trabajo de I/O de disco debe ejecutarse en `Dispatchers.IO`. Nunca bloquear el hilo principal (`Dispatchers.Main`).
2. **Previsualización Multimedia**: Mantener el límite de vista previa de 5 segundos con parada automática y liberación de `MediaPlayer` / `VideoView` para proteger la memoria RAM. Para documentos, mostrar metadatos ofimáticos y fragmentos de texto limpios.
3. **Limpieza de Miniaturas Huérfanas**: El purgador de miniaturas en `.thumbnails` solo debe eliminar archivos si su contraparte original no existe en la galería activa (`isFileInActiveGallery`).
4. **Reparación de Cabeceras (Magic Bytes)**: Toda reparación debe generar un archivo temporal en `context.cacheDir` antes de la inserción en MediaStore, asegurando que el archivo original no se corrompa si la reconstrucción no es exitosa.
5. **Detector de Duplicados**: El cálculo de hash debe usar un muestreo selectivo (primeros 16 KB + últimos 4 KB) para evitar leer gigabytes de datos en memoria RAM o saturar el bus I/O del procesador móvil.
6. **Medidor de Salud**: Cada archivo debe contar con su objeto `FileHealth` calculado al escanear, evaluando extensión, cabecera de bytes (Magic Bytes), resolución/duración y ubicación fuente.
7. **Manejo de Permisos**:
   - `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO` y `READ_MEDIA_AUDIO` para Android 13+ (API 33+).
   - `READ_EXTERNAL_STORAGE` y `WRITE_EXTERNAL_STORAGE` para Android 10-12 (API 29-32).
   - `MANAGE_EXTERNAL_STORAGE` (Settings Action) para acceso profundo en Android 11+.
8. **Pruebas Locales**: Ejecutar tests con `compile_applet` o pruebas unitarias con Robolectric sin depender de un emulador físico conectado a ADB.
9. **Automatización y Despliegue CI/CD**:
   - `build_apk_debug.yml`: Compila el APK Debug con firma autogenerada en el runner y entrega directa remota (hasta 2GB).
   - `sync_zip.yml`: Sincronización continua y unificación de commits desde archivos comprimidos.
10. **Integración con Shizuku y Sui (ADB / Root)**:
   - Shizuku opera mediante un IPC Binder (`rikka.shizuku.Shizuku`) con UID 2000 (Shell ADB) o UID 0 (Root).
   - Soporte automático para `Sui` (Magisk) mediante `rikka.sui.Sui.init()` y verificación con `Sui.isSui()`.
   - Implementación de `ShizukuScanUserService` vinculado mediante `Shizuku.bindUserService(UserServiceArgs, ...)` para ejecutar código nativo en el proceso Shell sin restricciones de `Android/data`.
   - Salida limpia del proceso remoto con código de transacción `16777115` (`System.exit(0)`).
   - Todas las llamadas al Binder están protegidas con `Shizuku.pingBinder()` y capturas seguras de excepciones.
   - El permiso se solicita dinámicamente con `Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)`.
