# Resumen Técnico — Localizador Pro

> Archivo preparado para asesoría de código. Describe el alcance actual, arquitectura, y puntos clave del proyecto.

---

## 1. Lenguaje y Framework

| Aspecto | Detalle |
|---------|---------|
| **Lenguaje** | Kotlin 2.4.0 (K2 Compiler) |
| **UI** | Jetpack Compose (BOM 2026.06.00) + Material Design 3 |
| **Arquitectura** | MVVM con StateFlow + Coroutines |
| **Min / Target SDK** | 24 / 35 |
| **Gradle** | Kotlin DSL, version catalog (`libs.versions.toml`) |

---

## 2. Función Principal

Aplicación de **tracking GPS inteligente** para Android que:

- Registra rutas en segundo plano con un **modo automático** que inicia/para según detección de movimiento real (>25m) e inactividad (>15min)
- Muestra rutas en **mapas OpenStreetMap** (osmdroid) con trazado de puntos, waypoints y favoritos
- **Vincula fotos** a coordenadas mediante metadatos EXIF
- Exporta/importa rutas completas en **ZIP** (incluye JSON con metadata + fotos)
- **Análisis con IA** (Groq/Llama 3.3) de telemetría de rutas
- Cifrado de API keys en **Android Keystore** + autenticación biométrica
- **Pantalla "Experience Map"** con panel lateral de fotos seleccionables, líneas guía overlay, y captura de pantalla para compartir

---

## 3. Estructura por Módulos (Contexto Acotado)

```
app/src/main/java/com/example/
├── MainActivity.kt              (902 lines)  ── Navegación, permisos, entry point
│
├── data/
│   ├── api/
│   │   ├── GeminiApi.kt         (12 lines)   ── Interface Retrofit para Gemini
│   │   ├── GroqApi.kt           (31 lines)   ── Interface Retrofit para Groq
│   │   └── RetrofitClient.kt    (29 lines)   ── Cliente Retrofit singleton
│   ├── db/
│   │   ├── AppDatabase.kt       (87 lines)   ── Room DB con SQLCipher
│   │   └── RouteDao.kt          (115 lines)  ── DAO con 17 queries
│   ├── export/
│   │   └── RouteExchanger.kt    (330 lines)  ── Export/import ZIP de rutas
│   ├── model/
│   │   ├── LocationPoint.kt     (29 lines)   ── Punto GPS (lat, lon, alt, timestamp)
│   │   ├── Route.kt             (17 lines)   ── Ruta (start/end, dist, speeds)
│   │   ├── RouteFavoriteCrossRef.kt (30 lines) ── Tabla N:M rutas ↔ favoritos
│   │   ├── SavedLocation.kt     (28 lines)   ── Favorito fijo (foto, color, contacto)
│   │   └── Waypoint.kt          (32 lines)   ── Punto de interés en ruta (foto, color)
│   └── repository/
│       └── RouteRepository.kt   (236 lines)  ── Capa de acceso unificada
│
├── location/
│   └── LocationService.kt       (680 lines)  ── Foreground Service GPS + lógica auto
│
├── ui/
│   ├── RouteViewModel.kt        (1116 lines) ── ViewModel principal (muy grande)
│   ├── components/
│   │   ├── FullScreenPhotoDialog.kt (69 lines)  ── Diálogo foto a pantalla completa
│   │   └── TourOverlay.kt       (38 lines)   ── Overlay de bienvenida
│   ├── screens/
│   │   ├── DetailScreen.kt      (434 lines)  ── Detalle de ruta individual
│   │   ├── ExperienceMapScreen.kt (407 lines) ── Mapa con panel de fotos + líneas guía
│   │   ├── HistoryScreen.kt     (336 lines)  ── Historial de rutas
│   │   ├── MapScreen.kt         (951 lines)  ── Mapa principal de tracking en vivo
│   │   └── SavedLocationsScreen.kt (721 lines) ── CRUD de favoritos + contacto
│   └── theme/
│       ├── Color.kt / Theme.kt / Type.kt
│
└── util/
    └── BiometricCipherHelper.kt (73 lines)   ── Cifrado Keystore + biometría
```

---

## 4. Base de Datos (Room + SQLCipher)

### Entidades

| Tabla | Columnas clave | PK |
|-------|---------------|----|
| `routes` | name, startTime, endTime, distanceMeters, isActive, maxSpeed, avgSpeed | id (autogen) |
| `location_points` | routeId FK, latitude, longitude, altitude, speed, timestamp | id (autogen) |
| `waypoints` | routeId FK, name, latitude, longitude, photoPath, color | id (autogen) |
| `saved_locations` | name, address, latitude, longitude, photoPath, color, phone, email, isContact | id (autogen) — **UNIQUE(lat, lon)** |
| `route_favorite_cross_ref` | routeId FK, favoriteId FK | composite (routeId, favoriteId) |

### Queries clave en RouteDao (115 lines, 17 queries)

- `getLocationPointsForRoute(routeId)` — puntos ordenados por timestamp
- `getFavoritesForRoute(routeId)` — favoritos vinculados vía cross_ref
- `getAllSavedLocationsSync()` — lista completa
- `insertIfNew(location)` — `OnConflictStrategy.IGNORE` (por eso se pierde photoPath en import)
- `clearWaypointPhoto(waypointId)` / `clearFavoritePhoto(favoriteId)` — para quitar foto
- `addFavoriteToRoute(routeId, favoriteId)` — inserta cross_ref

---

## 5. Pantallas y Navegación

```
MainActivity (Navigation Compose)
├── HistoryScreen        ── Lista de rutas guardadas + botón Importar ZIP
├── DetailScreen         ── Detalle de ruta: stats, gráficos, botón Exportar ZIP
├── ExperienceMapScreen  ── Mapa + panel de fotos seleccionables + líneas guía + Share/Save
├── MapScreen            ── Mapa de tracking en vivo con capas toggle
└── SavedLocationsScreen ── CRUD de favoritos + contacto + galería
```

---

## 6. Mejoras Específicas Buscadas (Estado Actual)

| Mejora | Estado | Descripción |
|--------|--------|-------------|
| **Export/Import ZIP** | ✅ Implementado | RouteExchanger con ZIP+JSON+photos |
| **Experience Map (panel + líneas guía)** | ✅ Implementado | Panel 160dp, marcadores agregan/quitan fotos, líneas naranja |
| **Share screenshot** | ✅ Implementado | `drawToBitmap()` + Intent `image/png` |
| **Guardar ZIP con SAF** | ✅ Implementado | `CreateDocument` + export a URI |
| **Import: photoPath perdido** | ✅ Corregido | `updatePhotoPathByLatLon` tras `insertIfNew` fallido |
| **Mapa fluido (touch/zoom)** | ⚠️ En revisión | Se eliminó `viewportTracker`, `canvasKey`, se optimizó `AndroidView update` con versionTag |
| **GuideLinesOverlay** | ✅ Implementado | Overlay osmdroid en lugar de Canvas Compose (para z-order) |
| **RouteViewModel** | 🔴 Muy grande (1116 lines) | Candidato a refactor en múltiples ViewModels |
| **MainActivity** | 🔴 Grande (902 lines) | Candidato a refactor |
| **MapScreen** | 🔴 Grande (951 lines) | Candidato a refactor |

---

## 7. Archivos Críticos (por orden de importancia)

| Prioridad | Archivo | Líneas | Por qué es crítico |
|-----------|---------|--------|-------------------|
| 1 | `RouteViewModel.kt` | 1116 | Orquesta toda la lógica de datos; demasiado grande |
| 2 | `LocationService.kt` | 680 | Lógica de tracking GPS en segundo plano |
| 3 | `RouteExchanger.kt` | 330 | Export/import ZIP con toda la semántica de negocio |
| 4 | `ExperienceMapScreen.kt` | 407 | Integración osmdroid + Compose + overlays |
| 5 | `MapScreen.kt` | 951 | Mapa de tracking en vivo |
| 6 | `RouteDao.kt` | 115 | Todas las queries de la BD |
| 7 | `MainActivity.kt` | 902 | Navegación, permisos, entry point |

---

## 8. Patrones y Decisiones Técnicas

- **MVVM**: ViewModel + StateFlow → CollectAsState en Composables
- **Navigation Compose**: `NavHost` con rutas tipadas (`Screen` sealed class)
- **Room con SQLCipher**: Base cifrada con `SupportFactory` de SQLCipher
- **osmdroid Overlay**: Para dibujar encima del mapa (guía lines), no Compose Canvas
- **SAF (Storage Access Framework)**: Para guardar ZIP en ubicación elegida por usuario
- **FileProvider**: Para compartir archivos (screenshot PNG, ZIP)
- **BiometricPrompt + CryptoObject**: Para desbloquear configuración sensible
- **EXIF**: Extracción de coordenadas de fotos para geolocalización
