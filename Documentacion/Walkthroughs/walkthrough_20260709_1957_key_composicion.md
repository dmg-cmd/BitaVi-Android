# Informe de Verificación: Clave Atómica de Composición (Composition Key Clamping)

Este informe documenta la inyección de la clave de desacople estructural `key(contexto)` en `MapScreen.kt` y `SavedLocationsScreen.kt` para asegurar que el subárbol de recomposición de Compose resetee de raíz el caché y la memoria de `HelpBottomSheet.kt` al cambiar de pantalla o contexto de ayuda.

---

## Cambios Realizados

### 1. Inyección de la Clave Atómica (`MapScreen.kt` y `SavedLocationsScreen.kt`)
- **Lugares de Modificación**:
  - [MapScreen.kt](file:///d:/Google%20IA%20Studio/localizador/app/src/main/java/com/example/ui/screens/MapScreen.kt)
  - [SavedLocationsScreen.kt](file:///d:/Google%20IA%20Studio/localizador/app/src/main/java/com/example/ui/screens/SavedLocationsScreen.kt)
- **Implementación**:
  - Se importó `androidx.compose.runtime.key` en ambos archivos.
  - Se envolvió la llamada condicional a `HelpBottomSheet` en un bloque `key(contexto) { ... }` en ambas vistas:
    ```kotlin
    contextoAyudaActivo?.let { contexto ->
        key(contexto) {
            HelpBottomSheet(context = contexto, onDismissRequest = { contextoAyudaActivo = null })
        }
    }
    ```
  - Esto destruye de forma síncrona el estado heredado de modales anteriores y fuerza la recreación limpia y redibujado de la hoja de asistencia en la memoria RAM del dispositivo.

### 2. Respaldos
Se resguardaron las copias preventivas de ambos archivos antes del parche en la carpeta `RESPALDO_ANTERIOR/` bajo los nombres `20260709_1957_MapScreen.kt` y `20260709_1957_SavedLocationsScreen.kt`.

---

## Verificación de Compilación

Tras inyectar los cambios de key, se ejecutó la validación estática del compilador Gradle:

```powershell
.\gradlew assembleDebug
```

*(El reporte final del build se anexará en cuanto finalice el proceso de compilación en segundo plano).*
