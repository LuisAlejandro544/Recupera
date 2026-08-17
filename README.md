# Recuperador Pro - Recuperación de Fotos, Videos y Audios Eliminados 📱⚡

[![Android](https://img.shields.io/badge/Android-10%2B%20(API%2029%2B)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![GitHub Action](https://img.shields.io/badge/CI%2FCD-Sync%20from%20Zip-2088FF?logo=githubactions&logoColor=white)](#-sincronización-de-código-desde-archivos-zip-github-action)
[![Licence](https://img.shields.io/badge/Privacidad-100%25%20Local-success)](#privacidad-y-seguridad)

Aplicación nativa para Android diseñada para **escanear, previsualizar y restaurar fotos, videos y archivos de audio eliminados** o residuales directamente en el almacenamiento de tu teléfono, sin necesidad de conexión a internet, sin servidores externos y sin requerir acceso root.

---

## 🌟 Características Principales

- 🔍 **Filtro Inteligente de Galería Activa**: Excluye automáticamente fotos, videos y canciones que ya se encuentran guardados y visibles en tu galería o reproductor, mostrando **únicamente archivos eliminados, en papelera o residuales**.
- 🩺 **Medidor de Salud del Archivo (File Health Meter)**: Diagnóstico en tiempo real del estado y porcentaje de integridad del archivo recuperable (100% Íntegro en papelera, 85-98% en caché de apps, 40-70% en fragmentos o miniaturas).
- 🎵 **Recuperación y Preview de Audios (5 Segundos)**: Escucha una vista previa de 5 segundos de audios y notas de voz eliminadas (`MP3`, `AAC`, `M4A`, `OGG`, `OPUS`, `WAV`, `AMR`, `FLAC`) con ecualizador animado.
- 🎬 **Reproductor de Vista Previa de Videos (5 Segundos)**: Previsualiza los primeros 5 segundos de cualquier video recuperable (`MP4`, `MKV`, `MOV`, `3GP`, `WEBM`, `AVI`) con barra de progreso interactiva antes de restaurarlo.
- 🖼️ **Visor de Fotos Interactivo**: Zoom táctil de hasta 4.5x, metadatos detallados (resolución, tamaño, fecha) y diseño inmersivo.
- 🗑️ **Escaneo de Papelera del Sistema (MediaStore Trash)**: Acceso directo a la papelera nativa de Android (API 30+) para fotos, videos y audios borrados recientemente.
- 🗂️ **Rastreo de Miniaturas y Caché Residual**: Localiza fragmentos y miniaturas en `.thumbnails`, carpetas de caché de apps y directorios `.nomedia`.
- 🕵️ **Recuperación de Carpetas de Mensajería**: Detecta notas de voz de WhatsApp (`WhatsApp Voice Notes` / `WhatsApp Audio`), estados de WhatsApp (`.Statuses`), carpetas de Telegram y bóvedas ocultas.
- ⚡ **Restauración en 1-Toque**: Restaura archivos de forma individual o por lotes (Batch Restore) a las carpetas públicas `Pictures/Restored_Photos`, `Movies/Restored_Videos` y `Music/Restored_Audio`.
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

## 📦 Sincronización de Código desde Archivos ZIP (GitHub Action)

El repositorio cuenta con un flujo de trabajo automatizado en `.github/workflows/sync_zip.yml`:

1. **Subida de Archivos**: Sube un archivo comprimido (`.zip`, `.7z`, `.tar.gz`, `.tar`, `.tgz`) dentro del directorio `zip/`.
2. **Mensaje de Commit Personalizado**: Puedes escribir un mensaje en el archivo `commit_message.txt` en la raíz del proyecto para definir el mensaje del commit resultante.
3. **Extracción y Sobreescritura Automática**: El Action extrae el contenido, sincroniza y sobreescribe los archivos correspondientes, limpia el archivo comprimido procesado y realiza un commit unificado (`--amend` y `force-push`) manteniendo un historial limpio.

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

## 📄 Licencia

Desarrollado como proyecto open source para la comunidad móvil.
