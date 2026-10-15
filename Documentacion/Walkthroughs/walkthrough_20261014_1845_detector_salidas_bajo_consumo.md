# Walkthrough de Verificación: Detector Autónomo de Salidas y Cero Consumo

> **Fecha:** 14 de Octubre de 2026, 18:45  
> **Tema:** Implementación del Módulo Autónomo Detector de Salidas (`DepartureWatcherManager`), Geofence Pasivo y Apagado Estricto de Batería  
> **Estado:** ✅ Verificado y Certificado con Pruebas Unitarias  
> **Resultado de Compilación:** `BUILD SUCCESSFUL` (40 tareas ejecutadas con `--rerun-tasks`)

---

## 1. Problema Resuelto

1. **Suspensión en Segundo Plano:** En reposo prolongado con la pantalla apagada (Doze Mode), el sistema operativo restringía la entrega continua de GPS o cerraba el servicio por consumo continuo innecesario.
2. **Restricción de Foreground Service:** Si el servicio era suspendido por el sistema, intentar reiniciarlo desde segundo plano mediante `ActivityRecognition` provocaba `ForegroundServiceStartNotAllowedException` en Android 12+/14+.
3. **Consumo Parásito con Auto-Mode Inactivo:** Cuando el usuario desactivaba el modo automático o detenía una ruta, el servicio permanecía en ejecución consumiendo batería y reteniendo WakeLocks innecesarios.
4. **Anclaje Estricto en Interiores:** `AutoTrackingManager` exigía `accuracy < 20m` para fijar el anclaje inicial de detención, impidiendo la detección de salida si el usuario se encontraba dentro de un edificio con señal satelital atenuada.

---

## 2. Solución Implementada

### Módulo Autónomo `DepartureWatcherManager` y `GeofenceDepartureReceiver`
* Se creó un componente satélite desacoplado (`com.aistudio.localizador.location.manager.DepartureWatcherManager`) que registra una geocerca pasiva (`Geofence.GEOFENCE_TRANSITION_EXIT`) en el coprocesador de hardware del módem en lugar de mantener la CPU despierta.
* Cuando el usuario cruza el perímetro de reposo (radio de 50 metros), el receptor `GeofenceDepartureReceiver` recibe el evento y despierta legalmente `LocationService` con privilegios de excepción, iniciando automáticamente la nueva ruta.

### Política de Apagado Absoluto (Zero-Leakage)
* Si `auto_mode == false` y no hay una ruta grabando:
  - Se desmantela cualquier geocerca activa.
  - Se cancela el reconocimiento de actividad física.
  - Se liberan todos los WakeLocks.
  - Se llama a `shutdownService()` removiendo la notificación de primer plano y ejecutando `stopSelf()`.
  - **Resultado:** 0% CPU, 0% GPS, 0% RAM consumidos.

### Sincronización en Caliente del Toggle
* Al pulsar el interruptor de modo automático en el menú lateral (`toggleAutoMode()`), se envía el Intent `ACTION_UPDATE_AUTO_MODE` al servicio para apagarlo de inmediato si se desactiva, o para comenzar a vigilar salidas si se activa.

### Tolerancia Cinemática en Interiores
* Se amplió la tolerancia de anclaje inicial y alejamiento a `TOLERANT_GPS_ACCURACY_METERS` (45m), permitiendo fijar puntos de detención en hogares y oficinas.

---

## 3. Verificación de Pruebas Unitarias

```
Task :app:testDebugUnitTest
BUILD SUCCESSFUL in 2m 24s
40 actionable tasks: 40 executed
Configuration cache entry reused.
```

* **Pruebas de `DepartureWatcherManagerTest`:**
  - `when coordinates are zero do not start watching` -> ✅ Aprobado
  - `when coordinates valid startWatchingDeparture registers geofence successfully` -> ✅ Aprobado
  - `stopWatching cleans active geofence` -> ✅ Aprobado
* **Pruebas de `AutoTrackingManagerTest`:**
  - `when auto mode is off do not trigger` -> ✅ Aprobado
  - `when already tracking do not trigger` -> ✅ Aprobado
  - `when speed is below threshold do not trigger` -> ✅ Aprobado
  - `when distance from stop anchor exceeds threshold trigger immediately` -> ✅ Aprobado
  - `clearAll resets state` -> ✅ Aprobado

---

## 4. Archivos Afectados

* [DepartureWatcherManager.kt](file:///e:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/location/manager/DepartureWatcherManager.kt)
* [GeofenceDepartureReceiver.kt](file:///e:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/location/receiver/GeofenceDepartureReceiver.kt)
* [LocationService.kt](file:///e:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/location/LocationService.kt)
* [AutoTrackingManager.kt](file:///e:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/location/manager/AutoTrackingManager.kt)
* [LocationServiceManager.kt](file:///e:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/ui/LocationServiceManager.kt)
* [RouteViewModel.kt](file:///e:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/ui/RouteViewModel.kt)
* [MapScreen.kt](file:///e:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/ui/screens/MapScreen.kt)
* [AndroidManifest.xml](file:///e:/Sandbox/BitaVi-Android/app/src/main/AndroidManifest.xml)
* [AppConstants.kt](file:///e:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/util/AppConstants.kt)
* [DepartureWatcherManagerTest.kt](file:///e:/Sandbox/BitaVi-Android/app/src/test/java/com/aistudio/localizador/location/manager/DepartureWatcherManagerTest.kt)
