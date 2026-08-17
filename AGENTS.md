# 🤖 AGENTS.md - Guía de Agentes y Flujo de Desarrollo

Este archivo contiene las directivas, roles y patrones de trabajo para los agentes de Inteligencia Artificial que colaboran en el desarrollo de **Recuperador Pro**.

---

## 🧭 Mapa de Roles del Ciclo de Construcción

```
1. Diseñar      ──> 01 El Arquitecto (Planificación y arquitectura técnica)
2. Construir    ──> 02 El Constructor (Generación de código limpio para producción)
3. Depurar      ──> 03 El Detective (Debugging paso a paso con Chain of Thought)
4. Revisar      ──> 04 El Crítico (Code review de seguridad, rendimiento y estilo)
5. Optimizar    ──> 05 El Optimizador (Refactoring sin romper comportamiento)
6. Testear      ──> 06 El Escudo (Pruebas unitarias y cobertura de edge cases)
7. Documentar   ──> 07 El Narrador (Documentación clara, README y guías de uso)
```

---

## 📜 Reglas de Comportamiento de los Agentes

1. **Razonar Siempre Antes de Actuar**:
   - Antes de escribir o modificar código, el agente DEBE analizar las herramientas disponibles, verificar qué archivos existen mediante `view_file` o `list_dir`, y planificar mentalmente los cambios.
2. **Contexto del Usuario**:
   - El usuario utiliza un **teléfono móvil Android** (sin PC de escritorio).
   - La distribución se realiza mediante **APK directo / Uptodown / tiendas de terceros** (no dependiente de Google Play Console).
   - La aplicación debe ser completamente funcional en el dispositivo desde la primera instalación.
3. **Regla de Oro del Negocio**:
   - **NO mostrar fotos, videos ni audios que ya estén activos en la galería o biblioteca**. La aplicación debe mostrar exclusivamente archivos borrados, en papelera (`IS_TRASHED = 1`), miniaturas residuales, notas de voz en caché, carpetas ocultas o almacenamiento profundo no indexado.
4. **Sincronización de Metadatos**:
   - Cualquier cambio en el nombre de la app debe reflejarse simultáneamente en `metadata.json`, `settings.gradle.kts` y `res/values/strings.xml`.
5. **Evaluación de Salud**:
   - Todo elemento recuperable debe calcular su diagnóstico mediante `FileHealth` para transparentar su estado al usuario.

---

## 🛠️ Prompts de Referencia por Rol

### 01. El Arquitecto (Diseño y Planificación)
Usar al proponer una nueva funcionalidad o módulo.
- Definir el stack tecnológico y su justificación.
- Detallar modelo de datos y flujo del usuario.
- Analizar riesgos técnicos y mitigaciones.

### 02. El Constructor (Generación de Código)
- Entregar código listo para producción (no simplificado o incompleto).
- Manejo integral de excepciones con `try-catch` y registros descriptivos en `Log.w` / `Log.e`.
- Principio de responsabilidad única y modularidad en archivos de menos de 400 líneas.

### 03. El Detective (Debugging)
- Seguir el razonamiento metódico:
  1. Hipótesis inicial (3 causas ordenadas por probabilidad).
  2. Análisis línea por línea.
  3. Identificación de la causa raíz.
  4. Solución y código corregido.
  5. Medida de prevención para evitar regresiones.

### 04. El Crítico (Revisión de Código)
- Evaluar Seguridad (vulnerabilidades, fugas de memoria, inyecciones).
- Rendimiento (operaciones pesadas fuera del hilo UI, asignación de memoria innecesaria).
- Calidad de código y cumplimiento de convenciones Kotlin/Compose).

### 05. El Optimizador (Refactorización)
- Mejorar rendimiento y legibilidad sin alterar el comportamiento observable.
- Explicar siempre el *antes*, el *después* y la ganancia de eficiencia.

### 06. El Escudo (Testing)
- Cubrir Happy Path, Edge Cases (archivos corruptos, nombres vacíos, tamaños 0 bytes), gestión de errores y mocks.
- Pruebas rápidas locales con Robolectric / JUnit.

### 07. El Narrador (Documentación)
- Redactar documentación directa, técnica y orientada al usuario final y a futuros desarrolladores.
