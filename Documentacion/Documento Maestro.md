# DOCUMENTO MAESTRO - LOCALIZADOR PRO

## 1. RESUMEN EJECUTIVO
- **Propósito**: Tracking GPS inteligente, diario multimedia y análisis con IA.
- **Arquitectura**: MVVM, Jetpack Compose, Room (SQLCipher).
- **Core de Tracking**: Servicio en segundo plano con filtrado multicapa y detección automática de movimiento.

## 2. REGLAS DE NEGOCIO (LA "LEY" DE LA APP)
- **Filtro GPS (`isLocationValid`)**: Precisión <= 20m, Velocidad <= 45m/s (162km/h), Delta tiempo >= 3s.
- **Autostop**: Inactividad > 15 min. Disparo de grabación tras >25m de movimiento.
- **Seguridad**: Biometría para API Key y borrado masivo. Cifrado AES-256-GCM.
- **Multimedia**: Fotos clonadas a directorio privado + EXIF (ubicación original).
- **UI/UX**: `DetailScreen` con menú `⋮` (Compartir, Exportar, Ayuda) + Diálogos de confirmación.

## 3. HOJA DE RUTA Y PRIORIDADES (FASE 1: ESTABILIZACIÓN)
- **Bug Autostop**: Implementar `lastGpsUpdateTimeMillis` (siempre activo) vs `lastMovementTimeMillis` (persistencia) vs `lastUserActivityTimeMillis`.
- **Lógica Final**: `inactivityTime = now - maxOf(lastMovementTimeMillis, lastUserActivityTimeMillis)`.
- **Ajuste**: `MIN_DISTANCE_THRESHOLD` a 5m.

## 4. ESTRUCTURA DE ARCHIVOS CRÍTICOS
- `RouteViewModel.kt` (Orquestador).
- `LocationService.kt` (Tracking + Sensores).
- `RouteExchanger.kt` (ZIP + Metadatos).
- `DetailScreen.kt` (UI Detalle).

## 5. HISTORIAL DE LOGROS (RESUMIDO)
- **Fase 3**: Social (Compartir mapas, proximidad 100m, buffer pre-grabación).
- **Fase 2**: Contactos, Fotos EXIF, Export CSV/Excel.
- **Fase 1**: Refactor arquitectura, filtro GPS, seguridad biométrica, IA Groq (Llama 3.3).
## 6. MÓDULO 3: ARQUITECTURA AVANZADA Y ESTADO GLOBAL

### 🔄 Actualización del Pipeline de Inteligencia Artificial (Fase 3 - Resiliencia)

Se reestructuró por completo el procesamiento y la captura de telemetría analizada con IA para migrar de tipos primitivos sueltos a una arquitectura reactiva de tipo Máquina de Estados Finitos (FSM).

#### 1. Máquina de Estados de la Interfaz (`AiAnalysisState`)
La comunicación entre la capa de datos de Groq y la interfaz declarativa (`DetailScreen.kt`) queda normada de manera estricta mediante el siguiente `sealed class` en el `RouteViewModel.kt`:
* `Idle`: Estado inicial. Muestra el botón de acción nativo.
* `Loading`: Bloqueo de UI con indicador de progreso asincrónico en pantalla.
* `Success(val report: String)`: Despliega el reporte de análisis en texto enriquecido.
* `MissingKey`: Alerta de configuración inicial.
* `ExpiredKey`: Estado crítico de infraestructura. Se activa al interceptar un error **HTTP 401 (Unauthorized)**. Muestra una tarjeta adaptativa de error con un campo de texto inline y botón condicionado (`newKey.startsWith("gsk_")`) para permitir la actualización de credenciales e inmediato reintento de análisis en un flujo sin fricción (*Self-Service Flow*).
* `Error(val message: String)`: Manejo de fallos generales de red o base de datos.

#### 2. Esquema de Datos y Persistencia Contextual (Base de Datos v11)
El modo de transporte contextual calculado por el acelerómetro y la velocidad en tiempo real ahora se persiste de forma permanente en el almacenamiento local:
* **Entidad `Route`:** Se añadió la columna `activityMode: String` (Valor por defecto: `"WALKING"`).
* **Migración Estructural:** Se inyectó en `AppDatabase.kt` la instrucción `MIGRATION_10_11` mediante un comando SQLite de alteración de tabla (`ALTER TABLE routes ADD COLUMN activityMode TEXT NOT NULL DEFAULT 'WALKING'`), evitando la pérdida destructiva de datos del historial de usuario durante la actualización de la base de datos.
* **Componentes de Interfaz Asociados:** `DetailScreen.kt` intercepta dinámicamente este registro para renderizar los micro-iconos adaptativos del sistema (`DirectionsWalk`, `DirectionsBike`, `DirectionsCar`, `Place`).
