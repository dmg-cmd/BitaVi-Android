# Walkthrough: Reparación del Guardado de Fotos con Coordenadas

> **Proyecto:** BitaVi-Android (Localizador Táctico)  
> **Fecha:** 14 de Octubre de 2026, 19:50  
> **Dispositivo probado:** Google Pixel 9 Pro XL (`48201FDAS001L5`)  
> **Agentes involucrados:** `telemetry-gps-specialist`, `compose-ui-tactical`, `scope-boundary-auditor`, `gradle-compiler-qa`  
> **Estado:** Resuelto, Verificado e Instalado (Success, PID 13555)

---

## 1. Diagnóstico de la Causa Raíz

A partir del análisis en tiempo real de Logcat tras el intento de guardado en el Pixel 9 Pro XL, se descubrieron tres factores concurrentes que provocaban que al presionar "Guardar" la foto con coordenadas no se guardase:

1. **Condición de carrera asíncrona (`NullPointerException` en `SavedLocationsScreen.kt:136`):**
   ```text
   FATAL EXCEPTION: DefaultDispatcher-worker-1
   Process: com.aistudio.localizador.vqtspk, PID: 5850
   java.lang.NullPointerException
       at com.aistudio.localizador.ui.screens.SavedLocationsScreenKt$SavedLocationsScreen$mediaLocationPermissionLauncher$1$1$1.invokeSuspend(SavedLocationsScreen.kt:136)
   ```
   Al solicitar el permiso de metadatos de medios (`ACCESS_MEDIA_LOCATION`), la variable de estado `pendingPhotoUri = null` se ejecutaba síncronamente antes de que la corrutina en `Dispatchers.IO` iniciase la copia, causando una excepción fatal al hacer `pendingPhotoUri!!`.

2. **Orden invertido en el consumo del Stream de Galería:**
   Se invocaba `saveImageToInternalStorage(targetUri)` antes de `extractLocationFromPhoto(targetUri)`. Al consumir y cerrar el stream del ContentResolver, la lectura posterior de cabeceras EXIF fallaba o producía metadatos nulos.

3. **Bloqueo silencioso por campo de Nombre vacío (`if (newName.isNotBlank())`):**
   Al cargar una foto con coordenadas en el diálogo de Favoritos, el usuario esperaba poder presionar "Guardar" de inmediato. Sin embargo, el botón exigía rígidamente que `newName` contuviera texto sin mostrar aviso ni alerta, ignorando el clic por completo.

---

## 2. Soluciones Implementadas

### 2.1. `SavedLocationsScreen.kt`
* **Protección inmutable de URI:**
  Se captura el URI en una variable local inmutable `val targetUri = pendingPhotoUri` antes de resetear el estado mutable, garantizando seguridad de hilos ante corrutinas asíncronas.
* **Tolerancia a permisos:**
  Si el permiso especial `ACCESS_MEDIA_LOCATION` no está otorgado o es denegado, la foto seleccionada por el usuario **no se descarta**: se procede a guardarla localmente y se extrae la información disponible.
* **Extracción previa de metadatos:**
  Se invierte el orden para asegurar que `extractLocationFromPhoto()` lea los datos EXIF sobre el archivo original antes de su almacenamiento interno.
* **Nombre sugerido y guardado tolerante:**
  * Al extraer una foto con metadatos, se autocompleta `newName` con `"Foto dd/MM HH:mm"` si está vacío.
  * Al presionar "Guardar", si el usuario no ingresó un nombre, se genera automáticamente un nombre descriptivo a partir de la foto o de la dirección (`finalName = newName.ifBlank { ... }`).
  * Se agregó confirmación visual `Toast.makeText(context, "Ubicación guardada con éxito", Toast.LENGTH_SHORT).show()`.

### 2.2. `MapScreen.kt`
* Se aplicó la misma arquitectura defensiva en los selectores de fotos de waypoints y marcadores para evitar cualquier riesgo análogo de `NullPointerException`.

---

## 3. Verificación y Despliegue en Vivo

1. **Pruebas Unitarias:**
   `./gradlew testDebugUnitTest` -> **BUILD SUCCESSFUL** (100% verde).
2. **Compilación de Distribución:**
   `./gradlew assembleDebug` -> **BUILD SUCCESSFUL** en 1m 38s.
3. **Instalación y Despliegue:**
   * Instalado con éxito en el Google Pixel 9 Pro XL (`Performing Streamed Install -> Success`).
   * Aplicación reiniciada en caliente (`PID 13555`).
