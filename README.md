# Recuperador Pro - Recuperación de Fotos, Videos, Audios y Documentos 📱⚡

[![Android](https://img.shields.io/badge/Android-10%2B%20(API%2029%2B)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![GitHub Action](https://img.shields.io/badge/CI%2FCD-Sync%20from%20Zip-2088FF?logo=githubactions&logoColor=white)](#-sincronización-de-código-desde-archivos-zip-github-action)
[![Licence](https://img.shields.io/badge/Privacidad-100%25%20Local-success)](#privacidad-y-seguridad)

Aplicación nativa para Android diseñada para **escanear, previsualizar y restaurar fotos, videos, notas de voz, pistas de audio y documentos de trabajo eliminados** o residuales directamente en el almacenamiento de tu teléfono, sin necesidad de conexión a internet, sin servidores externos y sin requerir acceso root. Además, incluye una **herramienta de limpieza de miniaturas huérfanas** para liberar almacenamiento residual en disco.

---

## 🌟 Características Principales

- 🔍 **Filtro Inteligente de Galería Activa**: Excluye automáticamente fotos, videos, audios y documentos que ya se encuentran guardados y visibles en tu almacenamiento normal, mostrando **únicamente archivos eliminados, en papelera o residuales**.
- 🛡️ **Integración de Shizuku y Sui (Privilegios ADB / Magisk Root)**: Utiliza `UserService` de Shizuku (`bindUserService`) para ejecutar un proceso con privilegios ADB (UID 2000) o Root (UID 0), permitiendo inspeccionar directorios restringidos en Android 11+ (`Android/data`, cachés profundas y papeleras propietarias de Samsung y Xiaomi) sin necesidad de PC.
- 📑 **Recuperación de Documentos de Trabajo y Ofimática**: Escanea y recupera archivos `PDF`, Word (`DOCX`, `DOC`), Excel (`XLSX`, `XLS`, `CSV`), PowerPoint (`PPTX`, `PPT`), texto plano (`TXT`, `RTF`) y libros digitales (`EPUB`) con visor de metadatos y fragmentos de texto.
- 🧹 **Limpieza de Miniaturas Huérfanas (Orphan Thumbnail Cleaner)**: Detecta y purga de forma segura las miniaturas residuales en directorios `.thumbnails` cuyos archivos originales ya no existen, liberando valiosos megabytes de almacenamiento.
- 🧬 **Reparador Básico de Cabeceras (Magic Bytes Reconstructor)**: Reconstruye y sintetiza bloques de cabeceras binarias dañadas para imágenes (`JPEG`, `PNG`, `GIF`), documentos (`PDF`) y pistas multimedia (`MP3`, `MP4`) restaurando su capacidad de ser leídos e indexados por el sistema operativo.
- 👯 **Buscador y Limpiador de Archivos Duplicados Residuales**: Detección inteligente multi-nivel basada en tamaño y hashing de alta velocidad (16KB iniciales + 4KB de cola) para identificar copias residuales idénticas y purgar duplicados innecesarios liberando almacenamiento.
- 🩺 **Medidor de Salud del Archivo (File Health Meter)**: Diagnóstico en tiempo real del estado y porcentaje de integridad del archivo recuperable (100% Íntegro en papelera, 85-98% en carpetas de apps, 40-70% en fragmentos o miniaturas).
- 🎵 **Recuperación y Preview de Audios (5 Segundos)**: Escucha una vista previa de 5 segundos de audios y notas de voz eliminadas (`MP3`, `AAC`, `M4A`, `OGG`, `OPUS`, `WAV`, `AMR`, `FLAC`) con ecualizador animado.
- 🎬 **Reproductor de Vista Previa de Videos (5 Segundos)**: Previsualiza los primeros 5 segundos de cualquier video recuperable (`MP4`, `MKV`, `MOV`, `3GP`, `WEBM`, `AVI`) con barra de progreso interactiva antes de restaurarlo.
- 🖼️ **Visor de Fotos Interactivo**: Zoom táctil de hasta 4.5x, metadatos detallados (resolución, tamaño, fecha) y diseño inmersivo.
- 🗑️ **Escaneo de Papelera del Sistema (MediaStore Trash)**: Acceso directo a la papelera nativa de Android (API 30+) para fotos, videos y audios borrados recientemente.
- 🗂️ **Rastreo de Miniaturas y Caché Residual**: Localiza fragmentos y miniaturas en `.thumbnails`, carpetas de caché de apps y directorios `.nomedia`.
- 🕵️ **Recuperación de Mensajería y Descargas**: Detecta notas de voz y documentos de WhatsApp (`WhatsApp Voice Notes`, `WhatsApp Documents`), Telegram (`Telegram Documents`, `Telegram Audio`), carpeta `Download` y bóvedas ocultas.
- ⚡ **Restauración en 1-Toque**: Restaura archivos de forma individual o por lotes (Batch Restore) a las carpetas públicas `Pictures/Restored_Photos`, `Movies/Restored_Videos`, `Music/Restored_Audio` y `Documents/Restored_Documents`.
- 🔒 **100% Privado y Seguro**: Todo el procesamiento se realiza en el hardware del teléfono. Cero telemetría, cero subida a la nube.

---

## 🏗️ Arquitectura y Tecnologías

| Componente | Tecnología | Propósito |
| :--- | :--- | :--- |
| **Lenguaje** | Kotlin 2.0+ | Rendimiento nativo, seguridad de nulos y corrutinas |
| **Interfaz (UI)** | Jetpack Compose + Material Design 3 | Diseño reactivo, moderno con tema oscuro de alto contraste |
| **Concurrencia** | Kotlin Coroutines & Flow | Escaneo asíncrono en segundo plano (`Dispatchers.IO`) |
| **Carga de Imágenes** | Coil 2.7+ | Decodificación rápida de bitmaps y miniaturas locales |
| **Almacenamiento** | Android MediaStore API & SAF | Gestión de Scoped Storage y restauración en colecciones públicas |
| **Reproducción Multimedia** | Android VideoView & MediaPlayer | Previsualización nativa de 5s para videos y pistas de audio |
| **Diagnóstico de Salud** | Análisis de Cabeceras & Magic Bytes | Cálculo de porcentaje de integridad y metadatos |
| **Automatización CI/CD** | GitHub Actions (`sync_zip.yml`) | Sincronización y sobreescritura automática del código desde `.zip` / `.7z` / `.tar.gz` |

---

## 📦 Flujos de Trabajo Automatizados (GitHub Actions)

### 1. 🤖 Compilación de APK Debug (`build_apk_debug.yml`)
Compila el APK Debug bajo demanda (**activación manual**) directamente en el runner de GitHub Actions con firma automática (`debug.keystore`), caché persistente de Gradle para compilaciones ultrarrápidas y entrega directa remota (hasta **2 GB**):

- **Activación Manual (`workflow_dispatch`)**: Se ejecuta únicamente cuando tú lo decides desde la pestaña *Actions* de GitHub.
- **Caché Inteligente de Gradle y Pip**: Reutiliza dependencias y artefactos previamente descargados (`gradle/actions/setup-gradle@v4`) reduciendo drásticamente el tiempo de compilación.
- **Firma Automática**: Genera el keystore de debug automáticamente en la máquina virtual antes de compilar.
- **Entrega Directa de Alta Capacidad (2 GB)**: Protocolo directo de entrega evitando el límite estricto de los artefactos estándar o APIs básicas.
- **Secrets Requeridos en GitHub**:
  * `TELEGRAM_API_ID`: Tu `App api_id` obtenido en [my.telegram.org](https://my.telegram.org) (Número entero).
  * `TELEGRAM_API_HASH`: Tu `App api_hash` obtenido en [my.telegram.org](https://my.telegram.org) (Cadena hexadecimal).
  * `TELEGRAM_BOT_TOKEN`: Token de autenticación del bot de entrega (ej: `123456789:ABCdefGhI...`).
  * `TELEGRAM_CHAT_ID`: ID numérico de tu chat/grupo/canal o `@username` de destino.

### 2. 🗜️ Sincronización de Código desde Archivos ZIP (`sync_zip.yml`)
1. Sube un archivo comprimido (`.zip`, `.7z`, `.tar.gz`, `.tar`, `.tgz`) dentro del directorio `zip/`.
2. Escribe el mensaje deseado en `commit_message.txt`.
3. El Action extrae el contenido, sincroniza y sobreescribe los archivos correspondientes, limpia el archivo temporal y realiza un commit unificado (`--amend` y `force-push`).

---

## 🚀 Cómo Generar el APK e Instalarlo (Sin PC)

Esta app está optimizada para ser descargada e instalada en formato **APK directo o a través de tiendas como Uptodown**:

1. **Desde AI Studio / Cloud Build**:
   - Pulsa en el menú superior o ajustes del proyecto.
   - Selecciona **Export APK / Generate APK**.
2. **Instalación en el Teléfono**:
   - Descarga el archivo `.apk` directamente en tu navegador móvil.
   - Abre el archivo y concede el permiso *"Instalar aplicaciones de fuentes desconocidas"* en tu navegador o gestor de archivos.
   - Abre **Recuperador Pro**, concede los permisos de almacenamiento y presiona **Escanear**.

---

## 📱 Flujo de Uso Rápido

```
[ Iniciar App ] ──> [ Conceder Permisos ] ──> [ Escaneo Profundo Automático ]
                                                      │
                                                      ▼
[ Galería de Resultados Filtrados (Solo Borrados) ] <───
   ├── Filtros: Fotos | Videos | Audios | Papelera | Miniaturas | Ocultos
   ├── Medidor de Salud: Indicador de integridad (10% - 100%)
   ├── Búsqueda por nombre y Ordenación (Fecha / Tamaño)
   ├── Previsualización: Zoom en Fotos | 5s Preview en Videos y Audios
   └── [ Seleccionar y Restaurar en Teléfono ]
```

---

## 🛡️ Privacidad y Seguridad

- **Sin Servidores**: La aplicación no tiene dependencias backend ni sube archivos a ningún servidor.
- **Sin Root Requerido**: Utiliza las APIs oficiales de Android para garantizar la integridad del sistema de archivos.
- **Permisos Transparentes**: Solo solicita permisos de lectura/escritura de almacenamiento (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`, `MANAGE_EXTERNAL_STORAGE` en Android 11+).

---

## 🤝 Contribuciones

Por el momento, **no se aceptan contribuciones directas ni Pull Requests de código externo** mientras se consolida la arquitectura y el roadmap principal. Para más detalles, consulta [CONTRIBUTING.md](CONTRIBUTING.md).

---

## 📄 Licencia

Desarrollado como proyecto open source para la comunidad móvil.
