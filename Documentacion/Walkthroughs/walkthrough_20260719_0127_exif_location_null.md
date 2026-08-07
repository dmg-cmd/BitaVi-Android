# Análisis y Resolución: Pérdida de Coordenadas EXIF en Selector de Imágenes

> [!CAUTION]
> **Naturaleza del Fallo:** La pérdida de los datos GPS (resolviendo a `null`) no se debe a la expiración de permisos en hilos de fondo (`Dispatchers.IO`), sino a una colisión crítica entre las políticas de privacidad del nuevo **Photo Picker** y las limitaciones de I/O secuencial del `ExifInterface` al procesar flujos de red.

## 1. La Física del Fallo en el Framework Android

El problema se aísla en la combinación de dos factores arquitectónicos fundamentales de la plataforma Android (API 29+):

### A. Redacción Activa de Privacidad (El efecto del "Refinamiento de UI")
Al cambiar el lanzador a `ActivityResultContracts.PickVisualMedia()`, la interfaz invocó el nuevo Photo Picker de Android. Por mandato de seguridad del sistema operativo, **el Photo Picker redacta (elimina) sistemáticamente todas las etiquetas EXIF de geolocalización** de las imágenes antes de entregarlas a la aplicación para proteger la privacidad del usuario. 
*¿Por qué falla `setRequireOriginal()`?* El método `MediaStore.setRequireOriginal(uri)` **solo funciona sobre URIs puros del MediaStore** (`content://media/external/...`). Los URIs efímeros que devuelve el Photo Picker (`content://media/picker/...`) ignoran esta directiva, por lo que el stream siempre llega "limpio" de coordenadas, sin importar que se cuente con el permiso `ACCESS_MEDIA_LOCATION`.

### B. Mecánica del ExifInterface sobre Streams (Factor 3) y Aislamiento de Red (Factor 2)
Aún si se utilizara un URI que permitiera la lectura de ubicación, pasar directamente un `InputStream` (devuelto por `contentResolver.openInputStream(uri)`) al `ExifInterface` es una operación inestable, especialmente con recursos remotos como Google Fotos:
1. **Falta de Random Access (Seek):** Los flujos provenientes de un `ContentProvider` remoto son tuberías secuenciales (Binder pipes). El estándar EXIF (basado en la estructura TIFF) a menudo requiere saltar hacia adelante y hacia atrás (operaciones `seek()`) para leer los punteros (offsets) de los directorios IFD donde residen los datos GPS. Al no poder retroceder en un stream no almacenable en caché, el parser falla silenciosamente.
2. **Aislamiento de Google Fotos:** Cuando el archivo reside en la nube, el provider tiende a emitir un byte-stream remuestreado o optimizado que purga las cabeceras pesadas si no se le exige un descriptor de archivo físico (`FileDescriptor`).

## 2. La Solución Arquitectónica

Para restaurar la extracción precisa sin violar la integridad de la base de datos con fallbacks espurios (ej: `0.0, 0.0`), se debe aplicar la siguiente arquitectura:

1. **Evadir el Photo Picker:** Reemplazar `PickVisualMedia` por el Storage Access Framework (SAF) usando `OpenDocument()`. Esto permite acceder al documento real sin la capa proxy de redacción de privacidad.
2. **Transcodificación Temporal (Buffer):** Volcar el `InputStream` remoto en un archivo temporal local físico (`File`). Esto proporciona al `ExifInterface` capacidades plenas de lectura aleatoria (`seek()`) sobre el sistema de archivos local, asegurando la extracción inyectando estabilidad ante el Factor 3.

## 3. Fragmento de Código Corregido

### A. Modificación del Lanzador en la UI (`SavedLocationsScreen.kt`)
Reemplazamos el contrato en la vista. Note que para invocarlo, se debe usar `photoPickerLauncher.launch(arrayOf("image/*"))`.

```kotlin
// Se reemplaza PickVisualMedia por OpenDocument para evadir la redacción obligatoria
val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
) { uri ->
    uri?.let { targetUri ->
        scope.launch(Dispatchers.IO) {
            // El procesamiento físico ocurre de forma robusta en el pool de IO
            val loc = viewModel.extractLocationFromPhotoRobust(targetUri, context)
            
            withContext(Dispatchers.Main) {
                photoGpsLocation = loc 
                if (loc != null) {
                    inputLatitude = String.format(Locale.US, "%.6f", loc.first)
                    inputLongitude = String.format(Locale.US, "%.6f", loc.second)
                    usePhotoGps = true
                    viewModel.fetchPhotoAddress(loc.first, loc.second)
                }
                // Si es null genuinamente, el FilterChip permanecerá deshabilitado
            }
        }
    }
}
```

### B. Implementación Robusta del Extractor en el ViewModel (o PhotoManager)
Se aísla la lógica de transcodificación, manteniendo la vista libre de variables huérfanas.

```kotlin
fun extractLocationFromPhotoRobust(uri: Uri, context: Context): Pair<Double, Double>? {
    return try {
        // 1. Crear un archivo temporal en caché para volcar el flujo remoto
        val tempFile = File(context.cacheDir, "temp_exif_buffer.jpg")
        
        // 2. Transcodificar: Copiar el flujo secuencial remoto al archivo físico
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        // 3. Leer EXIF desde la ruta física local. 
        // Esto garantiza que ExifInterface tenga operaciones de random access (seek) sobre el disco.
        val exif = androidx.exifinterface.media.ExifInterface(tempFile.absolutePath)
        val latLong = exif.latLong
        
        // 4. Limpieza higiénica del sistema de archivos
        if (tempFile.exists()) {
            tempFile.delete()
        }
        
        if (latLong != null) Pair(latLong[0], latLong[1]) else null
    } catch (e: Exception) {
        // Loggear la excepción real para auditoría
        e.printStackTrace()
        null // Retorno nulo estricto, sin fallbacks de ubicación falsos
    }
}
```
