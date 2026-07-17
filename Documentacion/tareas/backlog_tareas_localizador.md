# 🗺️ Plan de Trabajo y Backlog de Tareas — Proyecto Localizador
## Fase Alfa hacia Beta Temprana · Creado para: **Delm**

Este documento sistematiza todas las deudas técnicas y sugerencias identificadas en el análisis integral del código fuente de **Localizador**, dividiendo cada problema en tareas accionables, estimadas y priorizadas.

---

## 🔴 PRIORIDAD CRÍTICA (Bloqueantes para Fase Beta)

### Tarea CR-01: Implementación de la Suite de Pruebas Unitarias Básicas
- **Módulo:** Core / Testing
- **Estimación:** Alta (8-12 horas)
- **Riesgo:** Bajo
- **Descripción:** Crear la base de tests unitarios ejecutables en la JVM para proteger el código ante refactorizaciones masivas.
- **Pasos de ejecución:**
  1. Escribir pruebas unitarias para `CinematicFilterManager` (limpieza de saltos aberrantes de GPS).
  2. Escribir pruebas unitarias para `StationaryAnchorManager` (comportamiento de congelación por velocidad cero).
  3. Configurar Mockk/JUnit4 para simular `RouteDao` y probar `RouteRepository`.
  4. Validar la integración de pruebas desde la línea de comandos con `./gradlew test`.

### Tarea CR-02: Migración Global de Namespace a Producción
- **Módulo:** Build System
- **Estimación:** Media (4 horas)
- **Riesgo:** Alto (Afecta todas las referencias)
- **Descripción:** Reemplazar el namespace genérico `com.example` por el definitivo `com.aistudio.localizador` para cumplir con las políticas de Google Play Store.
- **Pasos de ejecución:**
  1. Modificar `applicationId` y `namespace` en `build.gradle.kts`.
  2. Refactorizar la estructura de directorios física del código fuente de `com/example` a `com/aistudio/localizador`.
  3. Actualizar el paquete de referencia en el `AndroidManifest.xml` y las importaciones de `R` en todas las pantallas.
  4. Ejecutar un *Build Clean -> Rebuild* para asegurar cero errores de enlace.

### Tarea CR-03: Mitigación de Pérdida de Datos en Room (Eliminar Fallback Destructivo)
- **Módulo:** Datos (Room DB)
- **Estimación:** Baja (2 horas)
- **Riesgo:** Medio
- **Descripción:** Desactivar el borrado automático de tablas ante fallas de migración en producción (`fallbackToDestructiveMigration`) en `AppDatabase.kt`.
- **Pasos de ejecución:**
  1. Retirar `.fallbackToDestructiveMigration(dropAllTables = true)` del constructor del builder de Room.
  2. Implementar un gestor de excepciones de inicialización que intente una copia de respaldo del archivo cifrado antes de recrearlo.

### Tarea CR-04: Saneamiento de Capturas Silenciosas de Excepciones
- **Módulo:** Estabilidad Core
- **Estimación:** Media (3 horas)
- **Riesgo:** Bajo
- **Descripción:** Remover los bloques `catch (_: Exception) {}` vacíos detectados en `RouteViewModel`, `RouteExportUseCase` y `BackupUseCase`.
- **Pasos de ejecución:**
  1. Localizar cada uno de los bloques de captura vacíos referenciados en la auditoría.
  2. Integrar reporte de errores mediante `Log.e()` o el sistema centralizado `AlertNotifier`.
  3. Propagar estados de error a la UI para mantener al usuario informado.

---

## 🟡 PRIORIDAD ALTA (Desacoplamiento y Refactorización Estructural)

### Tarea AL-01: Desintegración del Monolito LocationService
- **Módulo:** Location Service (Rastreo)
- **Estimación:** Muy Alta (16-24 horas)
- **Riesgo:** Alto (Core del sistema)
- **Descripción:** Desacoplar el archivo `LocationService.kt` (1161 líneas) delegando responsabilidades lógicas en componentes inyectables dedicados.
- **Pasos de ejecución:**
  1. Crear `TrackingSessionManager` para gobernar los estados de inicio, pausa y finalización de rutas.
  2. Crear `AutoModeManager` para la lógica de arranque y detención automática.
  3. Crear `ProximityAlertManager` para la detección BLE y "Cerca de mí".
  4. Crear `ServiceNotificationManager` para el mantenimiento dinámico de la notificación del Foreground Service.

### Tarea AL-02: Modularización de MainActivity
- **Módulo:** Navegación / UI Core
- **Estimación:** Alta (8 horas)
- **Riesgo:** Medio
- **Descripción:** Extraer flujos secundarios fuera de `MainActivity.kt` (1044 líneas) para recuperar mantenibilidad.
- **Pasos de ejecución:**
  1. Crear un fichero independiente `MainNavigation.kt` para el `NavHost`.
  2. Encapsular el Drawer lateral en el composable propio `MainDrawer.kt`.
  3. Mudar el gestor de permisos y la autenticación biométrica a controladores desacoplados.

### Tarea AL-03: Migración de Claves de Configuración a Constantes Estrictas
- **Módulo:** Configuración
- **Estimación:** Baja (2 horas)
- **Riesgo:** Bajo
- **Descripción:** Centralizar los literales de configuración dispersos ("auto_mode", "near_me_enabled", etc.) en el objeto centralizado `AppConstants.Preferences`.
- **Pasos de ejecución:**
  1. Identificar literales de cadenas huérfanas de preferencias en `LocationService` y `PreferencesManager`.
  2. Declarar cada una como `const val` dentro de `AppConstants.kt`.
  3. Efectuar el reemplazo y comprobar compilación libre de alertas.

---

## 💚 PRIORIDAD MEDIA (Deuda Técnica y Escalabilidad)

### Tarea ME-01: Migración e Integración de Hilt para Inyección de Dependencias
- **Módulo:** Arquitectura
- **Estimación:** Alta (12 horas)
- **Riesgo:** Alto
- **Descripción:** Eliminar el boilerplate manual de `RouteViewModelFactory` mediante Hilt.
- **Pasos de ejecución:**
  1. Integrar dependencias gradle de Hilt.
  2. Anotar la clase aplicación con `@HiltAndroidApp` y MainActivity con `@AndroidEntryPoint`.
  3. Configurar inyección por constructor para toda la capa de repositorios y UseCases.

### Tarea ME-02: Fragmentación de MapScreen y SavedLocationsScreen
- **Módulo:** Presentación / UI
- **Estimación:** Alta (10 horas)
- **Riesgo:** Medio
- **Descripción:** Dividir las pantallas monolíticas de Compose en micro-componentes legibles.
- **Pasos de ejecución:**
  1. Extraer los controles de tracking de mapa a `TrackingControls.kt`.
  2. Aislar los overlays del mapa de osmdroid a `MapOverlays.kt`.
  3. Extraer la tarjeta de item favorito de `SavedLocationsScreen` a `SavedLocationCard.kt`.

### Tarea ME-03: Activación de exportSchema de Room DB
- **Módulo:** Datos (Room DB)
- **Estimación:** Baja (1 hora)
- **Riesgo:** Bajo
- **Descripción:** Activar `exportSchema = true` para posibilitar auditorías del histórico y validación de migraciones SQL.
- **Pasos de ejecución:**
  1. Habilitar la directiva en `AppDatabase.kt`.
  2. Configurar la ruta del esquema en el bloque `room` de `build.gradle.kts`.
