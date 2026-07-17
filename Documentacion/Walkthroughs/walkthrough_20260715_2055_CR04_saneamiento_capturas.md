# Walkthrough CR-04: Saneamiento de Capturas Silenciosas de Excepciones
**Fecha**: 2026/07/15 20:55

## Objetivo
Eliminar los bloques catch vacios que tragaban excepciones silenciosamente en operaciones de exportacion, importacion y restauracion de datos.

## Arquitectura Implementada
UseCase --Log.e + Result<T>--> ViewModel --Channel<UiEvent>--> MainActivity --SnackbarHost--> Usuario

## Cambios Realizados

### UiEvent.kt (NUEVO)
- Sealed class con ShowSnackbar(message, actionLabel) para eventos one-shot.

### RouteViewModel.kt
- **Anadido**: _uiEvent: Channel<UiEvent>(Channel.BUFFERED) + al uiEvent = _uiEvent.receiveAsFlow()
- **importGpxFile()**: catch vacio -> Log.e + _uiEvent.send(UiEvent.ShowSnackbar)
- **importarRutaZip()**: catch vacio -> Log.e + _uiEvent.send(UiEvent.ShowSnackbar)
- **restoreFromBackup()**: catch vacio -> Log.e + _uiEvent.send(UiEvent.ShowSnackbar)

### RouteExportUseCase.kt
- **exportCsv()**: catch (_: Exception) { } -> Log.e("RouteExportUseCase", ...)
- **exportGpx()**: catch (_: Exception) { } -> Log.e("RouteExportUseCase", ...)
- **importGpx()**: catch (_: Exception) { false } -> Log.e + false
- **shareFile()**: catch (_: Exception) { } -> Log.e("RouteExportUseCase", ...)

### BackupUseCase.kt
- **detectRestoreType()**: catch (_: Exception) { RestoreDecision.Invalid } -> Log.e + RestoreDecision.Invalid

### MainActivity.kt
- **Imports**: SnackbarHost, SnackbarHostState, SnackbarDuration, UiEvent, kotlinx.coroutines.flow.collect
- **SnackbarHostState**: Anadido remember {} en MainContainer
- **LaunchedEffect**: Colector de viewModel.uiEvent que muestra Snackbar
- **Scaffold**: snackbarHost = { SnackbarHost(hostState = snackbarHostState) }

## Verificacion
- `./gradlew test`: BUILD SUCCESSFUL (32 tareas, 7 tests unitarios)
- `./gradlew assembleDebug`: BUILD SUCCESSFUL (39 tareas, APK generado)

## Archivos Modificados (5)
| Archivo | Cambio |
|---------|--------|
| ui/UiEvent.kt | CREADO |
| ui/RouteViewModel.kt | +_uiEvent, 3 catches saneados |
| ui/RouteExportUseCase.kt | 4 catches saneados con Log.e |
| ui/BackupUseCase.kt | 1 catch saneado con Log.e |
| MainActivity.kt | SnackbarHost + LaunchedEffect |

## Respaldos
- RESPALDO_ANTERIOR/20260715_RouteViewModel_pre_CR04.kt
- RESPALDO_ANTERIOR/20260715_RouteExportUseCase_pre_CR04.kt
- RESPALDO_ANTERIOR/20260715_BackupUseCase_pre_CR04.kt
- RESPALDO_ANTERIOR/20260715_MainActivity_pre_CR04.kt
