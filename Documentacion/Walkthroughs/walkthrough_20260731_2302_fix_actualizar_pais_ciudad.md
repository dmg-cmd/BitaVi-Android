# Walkthrough — Fix Botón "Actualizar País/Ciudad" (CR-25)

**Fecha/hora:** 20260731_2302
**Módulo:** Diálogo Editar Favorito (Pantalla 3) + RouteViewModel.refreshLocationDetails
**Build:** app-debug (assembleDebug)

## Objetivo
Verificar que el botón "Actualizar País/Ciudad" persista la geocodificación (país/ciudad/dirección) al pulsar "Guardar Cambios" y que muestre el resultado en tiempo real.

## Síntoma reportado
Tras el CR-24 (cobertura mundial), el guardado de favoritos nuevos funciona, pero el botón "Actualizar País/Ciudad" sobre un favorito antiguo no refleja el cambio.

## Diagnóstico
1. `refreshLocationDetails` sí geocodificaba y persistía en la DB (comportamiento correcto del ViewModel).
2. El diálogo de edición usa `locationToEdit` (estado local Compose) = snapshot del objeto ORIGINAL. El confirmButton "Guardar Cambios" ejecutaba `locationToEdit!!.copy(name=..., ...)` → reescribía `country="Desconocido"`/`city="Desconocida"` sobre la geocodificación recién persistida → reversión.
3. El diálogo no mostraba el país/ciudad: cero feedback visual.

## Cambios verificados en código
1. **RouteViewModel.kt — refreshLocationDetails**
   - Firma: `(location: SavedLocation, onResult: (SavedLocation) -> Unit = {})`.
   - Base fresca: `repository.allSavedLocations.first().firstOrNull { it.id == location.id } ?: location` → no pisa campos editados/guardados mientras corre la corutina (race condition).
   - `repository.updateSavedLocation(updated)` y `withContext(Dispatchers.Main) { onResult(updated) }`.
   - Import añadido: `kotlinx.coroutines.flow.first`.
2. **SavedLocationsScreen.kt — diálogo Editar Favorito**
   - Estado `isRefreshingLocation`; OutlinedButton con `CircularProgressIndicator(16.dp)` + texto "Geocodificando..." (deshabilitado mientras procesa).
   - Callback actualiza `locationToEdit = updated`.
   - Fila M3 de feedback: `Icons.Default.Place` (14dp, primary) + `${locationToEdit!!.country} • ${locationToEdit!!.city}` (bodySmall, onSurfaceVariant).
   - confirmButton: base = `locations.firstOrNull { it.id == locationToEdit!!.id } ?: locationToEdit!!` → conserva país/ciudad actualizado aunque el refresh termine después.

## Notas de diseño
- Se respeta UDF: la lógica de geocodificación permanece en GeocoderUseCase; el ViewModel solo orquesta y despacha en Main.
- No se usó `repository.updateLocationAddress(id, country, city)` del dictamen (no existe): se usa `updateSavedLocation` existente con el objeto completo.
- `locationToEdit` (objeto inmutable) se reemplaza por el objeto actualizado vía callback; los `remember` de campos editados (editName, etc.) se preservan porque el estado del diálogo no se recompone desde cero.

## Pruebas de compilación
- `gradlew :app:testDebugUnitTest` → 77 tests, 68 OK / 9 fallos preexistentes (ajenos al cambio).
- `gradlew :app:assembleDebug` → BUILD SUCCESSFUL.

## Deploy
- `adb -s 48201FDAS001L5 install -r app-debug.apk` → Success.
- Lanzamiento `com.aistudio.localizador.vqtspk/.MainActivity` → OK.

## Resultado
Intervención completa. Prueba manual recomendada: abrir Editar Favorito sobre un favorito con "Desconocido", pulsar "Actualizar País/Ciudad", verificar el indicador "Geocodificando..." y luego "Perú • Machupicchu" debajo del botón; pulsar "Guardar Cambios" y confirmar que la tarjeta conserva el país/ciudad.