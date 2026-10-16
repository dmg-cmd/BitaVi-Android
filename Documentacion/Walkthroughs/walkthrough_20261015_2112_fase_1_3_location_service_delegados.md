# Walkthrough — FASE 1.3: Patron Fachada en LocationService + Politica de WakeLock

**Fecha de ocurrencia:** 2026-10-15 21:12
**Ambito:** `LocationService.kt` y nuevo paquete `location/delegate/`.

---

## 1. Objetivo

Extraer del mono-servicio `LocationService.kt` (1057 lineas) la persistencia, las
notificaciones y el ciclo de vida del GPS a delegados inyectables, y corregir la
**politica del WakeLock** que tenia un limite fijo de 25 horas.

## 2. Requisito del usuario

> "Mientras la grabacion esta activa, el sistema tiene que permanecer encendido y
> solicitando senal GPS, sin importar cuanto dure."

Es decir: una grabacion larga (mas de 25 horas) **no debe cortarse** por la expiracion
del WakeLock.

## 3. Que es el WakeLock (contexto)

Android suspende la CPU cuando la pantalla se apaga para ahorrar bateria (Doze). Un
`PARTIAL_WAKE_LOCK` le pide al sistema que mantenga la CPU despierta (sin encender la
pantalla) mientras se graba una ruta. El codigo anterior hacia:

`wakeLock.acquire(25 * 60 * 60 * 1000L)`  -> se liberaba solo a las 25 h.

## 4. Cambios realizados

### Nuevos delegados (`location/delegate/`)
- **GpsLifecycleDelegate.kt**: cliente GPS fusionado, `HandlerThread` dedicado, callback
  de ubicaciones y el **WakeLock**. Politica nueva:
  - Grabacion activa -> `acquire()` **SIN timeout** (permanece despierto indefinidamente).
  - Modo pasivo/detenido -> `release()` inmediato.
- **LocationNotificationDelegate.kt**: envuelve `ServiceNotificationManager` y centraliza
  la notificacion de seguimiento y las alertas de proximidad ("Cerca de mi").
- **LocationPersistenceDelegate.kt**: envuelve `RouteRepository`. Como la base Room esta
  cifrada con SQLCipher, el repositorio real se asocia con `attach()` desde el servicio
  una vez descifrada la clave (hasta entonces opera en no-op seguro).

### `LocationService.kt` (1057 -> 806 lineas)
- Inyecta los 3 delegados con Hilt.
- Se elimina `acquire(25h)` y todo el manejo directo de WakeLock/HandlerThread/callback.
- La liberacion del WakeLock ahora ocurre en: modo pasivo, pausa, detencion, apagado y `onDestroy`.

## 5. Verificacion

| Comprobacion | Resultado |
|---|---|
| `compileDebugKotlin` | BUILD SUCCESSFUL |
| `testDebugUnitTest` | 109 tests, 0 fallos, 0 errores |
| `installDebug` (deploy) | Instalado en 2 dispositivos (Pixel 9 Pro XL) |

## 6. Pendientes / notas

- La meta original de <400 lineas **no se alcanzo** (quedo en 806). La logica de negocio
  (`processNewLocation`, temporizadores y auto-tracking) sigue en el servicio; bajarla
  requeriria extraer mas delegados de dominio.
- **No se realizo una prueba prolongada (4 h+ con pantalla apagada)** en dispositivo;
  se recomienda observar una grabacion larga real.
- Los respaldos previos estan en `RESPALDO_ANTERIOR/20261015_LocationService_pre_fase13.kt`.