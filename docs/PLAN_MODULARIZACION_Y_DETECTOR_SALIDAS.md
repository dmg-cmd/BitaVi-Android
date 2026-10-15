# Plan Táctico: Modularización Incremental y Detector Autónomo de Salidas

> **Proyecto:** BitaVi-Android (Localizador Táctico)  
> **Fecha:** 14 de Octubre de 2026  
> **Objetivo Dual:**  
> 1. Modularizar `MainActivity.kt` mediante migración segura y desacoplada en directorio independiente sin romper la versión operativa actual.  
> 2. Diseñar e implementar un módulo ligero de bajo consumo (**Detector Autónomo de Salidas / Geofence Departure**) que garantice el encendido automático de rutas en segundo plano al salir de un lugar, y libere el 100% de la memoria y batería cuando el modo automático esté desactivado.

---

## 1. Diagnóstico del Problema en Modo Automático

### ¿Por qué falla actualmente en segundo plano tras un tiempo prolongado?
1. **Suspensión por Doze Mode:**  
   Cuando el teléfono reposa con la pantalla apagada más de 15 minutos, Android restringe severamente las actualizaciones de ubicación del GPS (`LocationCallback`). El servicio intenta seguir pidiendo GPS de alta frecuencia (`requestLocationUpdates(5000L, 2f)`), lo cual agota la batería o provoca que el sistema operativo o el fabricante (Samsung, Xiaomi, Pixel) finalice el proceso.
2. **Restricción de Foreground Service desde Background (Android 12/14+):**  
   Si el servicio se suspende o muere, cuando `ActivityTransitionReceiver` detecta movimiento e intenta hacer `context.startForegroundService()`, el sistema operativo bloquea la llamada con `ForegroundServiceStartNotAllowedException`.
3. **Consumo Parásito con Modo Automático Apagado:**  
   Si el usuario no está en modo automático y detiene una ruta, el servicio a menudo permanece vivo en modo pasivo pidiendo coordenadas GPS cada 5 segundos, manteniendo el CPU despierto y consumiendo batería de forma innecesaria.
4. **Umbral de Precisión Rígido en Interiores:**  
   En interiores (casa, trabajo), la precisión satelital raramente alcanza `< 20m`. Si el anclaje de parada de `AutoTrackingManager` no se fija por descarte de precisión, el usuario sale del inmueble y el sistema nunca detecta el alejamiento radial.

---

## 2. Arquitectura de la Solución 2: Módulo Autónomo Detector de Salidas (`DepartureDetectorManager`)

### Principio Operativo: Consumo Cero vs. Despertar Garantizado
```
                            [ MODO AUTOMÁTICO ]
                                     │
                    ┌────────────────┴────────────────┐
                    ▼                                 ▼
             [ AUTO_MODE = ON ]                [ AUTO_MODE = OFF ]
                    │                                 │
         ¿Hay ruta grabando?                 ¡APAGADO ABSOLUTO!
         ┌──────────┴──────────┐                      │
         ▼                     ▼                      ├─ Quitar Geofence
      [ SÍ ]                [ NO ]                    ├─ Quitar Activity Recognition
  LocationService       Activar Geofence              ├─ Cancelar WakeLock
  GPS Activo            de Salida (50-70m)            └─ stopSelf() (0% RAM / 0% Batería)
  (Foreground)          en punto de reposo
                               │
                        ¿Cruza perímetro?
                               │
                               ▼
                        [ DESPERTAR ]
                   Lanzar LocationService
                   Iniciar Grabación
```

### Componentes del Módulo Detector:
1. **`DepartureWatcherManager.kt`** (en `location/manager/`):
   - Gestiona el alta y baja del Geofence pasivo de salida utilizando `GeofencingClient` de Google Play Services con `GEOFENCE_TRANSITION_EXIT`.
   - Cuando el usuario se detiene y la ruta activa finaliza, se ancla un geofence circular (radio configurable de 50 a 70 metros) alrededor de la última coordenada válida.
   - El monitoreo lo ejecuta el coprocesador de hardware del módem (WiFi / Cell ID / GPS por hardware), sin despertar la CPU del teléfono.
2. **`GeofenceDepartureReceiver.kt`** (en `location/receiver/`):
   - `BroadcastReceiver` registrado en el manifiesto con `PendingIntent` nativo.
   - Cuando el usuario abandona el perímetro de reposo, el sistema operativo despierta al receptor con privilegios de excepción para iniciar el `LocationService` inmediatamente.
3. **Regla de Apagado Radical (Zero-Battery Leak):**
   - Si `auto_mode == false` y `isTracking == false`:
     - Se invoca `departureWatcherManager.stopWatching()`.
     - Se retiran todos los geofences y suscripciones de actividad.
     - Se ejecuta `stopForeground(STOP_FOREGROUND_REMOVE)` y `stopSelf()`.
     - No queda ningún hilo, servicio, corrutina ni WakeLock en ejecución.

---

## 3. Arquitectura de la Solución 1: Modularización Segura de `MainActivity.kt`

Para **no desarmar ni poner en riesgo la funcionalidad actual**, la modularización se ejecutará en una carpeta aislada: `com.aistudio.localizador.ui.container/`.

### Desglose de Nuevos Módulos Desacoplados:
```
app/src/main/java/com/aistudio/localizador/ui/container/
├── ActivityContractsCoordinator.kt   # Aísla todos los registerForActivityResult
├── BiometricAuthHandler.kt           # Aísla BiometricPrompt y Keystore
├── MainScaffoldContainer.kt          # UI declarativa de Drawer, TopBar y Diálogos
└── NavigationHostDelegate.kt         # Orquestador desacoplado de pantallas
```

### Fases de Implementación Incremental:

#### Fase 1: Creación de Módulos Satélite en Carpeta Separada
- Crear `ActivityContractsCoordinator.kt`: Encapsula los contratos para solicitud de permisos (ubicación, notificaciones, cámara), selector de archivos (SAF para ZIP y Excel) y captura de fotos.
- Crear `BiometricAuthHandler.kt`: Encapsula la orquestación del diálogo biométrico de huella/rostro y la entrega segura de la API Key.
- Crear `MainScaffoldContainer.kt`: Mueve la estructura composable del Drawer y la barra superior fuera de `MainActivity.kt`.

#### Fase 2: Conexión Delegada en `MainActivity.kt`
- En lugar de reescribir `MainActivity.kt` desde cero, se reemplazan bloques gigantes por llamadas a sus respectivos delegados:
  ```kotlin
  // Ejemplo de reducción en MainActivity:
  private val contractsCoordinator = ActivityContractsCoordinator(this)
  private val biometricHandler = BiometricAuthHandler(this)
  
  override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      contractsCoordinator.register(this)
      setContent {
          MainScaffoldContainer(
              viewModel = viewModel,
              onOpenBackup = { contractsCoordinator.launchBackupPicker() },
              ...
          )
      }
  }
  ```
- La actividad principal pasa de **1115 líneas a menos de 200 líneas**, conservando el 100% de la lógica probada y los identificadores de vistas.

#### Fase 3: Integración del `DepartureWatcherManager`
- Modificar el flujo de `stopTrackingWithAutoRestart()` en `LocationService.kt`:
  - Si `auto_mode == true`: el servicio no se queda en loop de GPS de 5s; activa el `DepartureWatcherManager` y duerme el sensor de GPS continuo.
  - Si `auto_mode == false`: se apaga de inmediato sin dejar nada en memoria.
- Conectar `GeofenceDepartureReceiver` para recibir el evento `EXIT` e iniciar `LocationService.ACTION_START`.

#### Fase 4: Validación y Suite de Pruebas
- Auditoría con `gradle-compiler-qa`: compilación en limpio sin warnings.
- Ejecución de la suite completa de tests unitarios (incluyendo nuevos tests para `DepartureWatcherManagerTest` y `ActivityContractsCoordinatorTest`).
- Verificación en dispositivo real de consumo de batería y reacción de encendido al alejarse 50 metros.

---

## 4. Subagentes Especializados y sus Roles en este Plan

1. **`doc-custodian`**:
   - Genera respaldo preventivo de `MainActivity.kt`, `LocationService.kt` y `AutoTrackingManager.kt` en `RESPALDO_ANTERIOR/`.
   - Registra el avance en `HISTORIAL_CAMBIOS.md` y documenta el walkthrough final.
2. **`scope-boundary-auditor`**:
   - Audita que la creación de módulos en `ui/container/` no altere las pantallas hijas (`MapScreen`, `HistoryScreen`, etc.) ni los contratos existentes.
3. **`telemetry-gps-specialist`**:
   - Diseña el Geofence de bajo consumo en `DepartureWatcherManager.kt` para garantizar que la transición a segundo plano no sufra *Doze freeze*.
   - Supervisa la política estricta de apagado cuando `auto_mode == false`.
4. **`compose-ui-tactical`**:
   - Revisa que `MainScaffoldContainer.kt` mantenga intacta la taxonomía `PANTALLA.BARRA.BOTON` y la paleta cromática semántica.
5. **`gradle-compiler-qa`**:
   - Ejecuta `./gradlew test` y `./gradlew assembleDebug` asegurando cero warnings y preservando la suite verde.
