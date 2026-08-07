# 🛰️ ESPECIFICACIÓN DE CONTEXTO ARQUITECTÓNICO Y REGISTRO DE CAMBIOS: SUITE BITARU

**Fecha de Actualización:** 20 de Julio de 2026 **Proyecto:** Localizador (Suite Bitaru) **Ruta Destino Local:** `D:\\\\Google IA Studio\\\\localizador\\\\Documentacion\\\\ESPECIFICACION\\\_ARQUITECTURA\\\_BITARU.md` **Versión del Documento:** 1.5 (Production-Ready)

## 1. Configuración de Operación del Sistema

### Rol Actor y Principios de Trabajo

- **Identidad:** Ingeniero de Software Senior especializado en Arquitectura Android, Kotlin Avanzado y Jetpack Compose Reactivo\[cite: 2\].

- **Enfoque de Trabajo:** Código de grado de producción (*Production-Ready*), modular, robusto, altamente testeable (`JUnit`/`Mockk` sobre JVM) y optimizado a nivel de bytecode para hardware de alta gama (Pixel 9 Pro XL)\[cite: 2\].

### Arquitectura General del Sistema (Búnker Bitaru)

- **Patrón de Presentación:** MVVM Limpio. Los `ViewModels` (ej: `RouteViewModel`) actúan exclusivamente como orquestadores reactivos de estados de la UI (expuestos mediante `StateFlow`), delegando de manera estricta la carga lógica pesada\[cite: 2\].

- **Capa de Dominio (Lógica de Negocio):** Desacoplada mediante componentes únicos y especializados denominados `UseCases` (ej: `GeocoderUseCase`, `ShareUseCase`, `RouteExportUseCase`, `BiometricAuthUseCase`), inyectados por constructor a los ViewModels correspondientes\[cite: 2\].

- **Capa de Servicios y Tracking:** El núcleo de geolocalización perimetral reside de forma imperativa en un `LocationService` (Foreground Service con persistencia y ejecución en background) con filtros cinemáticos de precisión (`CinematicFilterManager`) para la purga de coordenadas de GPS ruidosas y telemetría BLE\[cite: 2\].

- **Capa de Datos:** Persistencia local estructurada sobre una base de datos SQLite cifrada mediante la suite de seguridad **SQLCipher**\[cite: 2\].

## 2. Las Cinco Reglas de Oro de Operación (Contrato Absoluto)

1. **Modularidad Estricta (Anti-God-Object):** Queda prohibido inyectar lógica de cómputo, serialización o persistencia cruda dentro de los ViewModels o Vistas. Si una función resuelve una regla de negocio, se extrae inmediatamente a un `UseCase` o un `Manager` inyectable por constructor\[cite: 2\].

2. **Código Seguro y Tipado Fuerte:** Priorización absoluta de la legibilidad, aserciones seguras controladas por el compilador y tipado robusto. Se evitan funciones "top-level" o globales sueltas en favor de componentes estructurados para facilitar el testing unitario determinístico sin mocks de Android en la JVM\[cite: 2\].

3. **Cero Advertencias (Obsesión Sintáctica):** Tolerancia cero a la acumulación de código muerto ocioso en disco. El entorno operativo busca el cero absoluto en *warnings* reales de compilación (remover imports no utilizados, variables muertas, firmas redundantes, operadores Elvis ociosos y advertencias de Lint)\[cite: 2\].

4. **UX Profesional (Material Design 3):** Toda alteración en la interfaz gráfica reactiva debe alinearse con la jerarquía de información, control estricto del ciclo de vida de Compose (evitando el uso de contextos observables locales en bloques imperativos tradicionales como `AndroidView/osmdroid`), y el uso óptimo de las densidades de pantalla\[cite: 2\].

5. **Documentación Obligatoria y Justificación Física:** Ante cualquier propuesta de cambio de lógica compleja (filtros espaciales, autostop, reestructuración cinemática), se debe proveer una explicación conceptual, matemática o de diseño arquitectónico previa a la entrega de la solución de código\[cite: 2\].

## 3. Historial de Hitos y Solicitudes de Cambio (CR-08 a CR-11)

### CR-08: Bypass Biométrico y Escudo de Autenticación

- **Estado:** ✅ **Completado e Implementado.**

- **Diagnóstico:** Inexistencia de guarda en el ciclo de vida de `MainActivity`. El `BiometricPrompt` se invocaba solo desde el Drawer pero permitía renderizar la app sin restricciones.

- **Solución Aplicada:**

  1. Creación de `BiometricAuthUseCase.kt` inyectando `PreferencesManager` (método `isBiometricEnabled()`).

  2. Creación de `BiometricLockScreen.kt` con M3 y botón de reintento.

  3. Modificación de `MainActivity.kt` con propiedades `isAppUnlocked` y `biometricErrorMessage`. Gating en `setContent` interceptando `MainContainer(routeViewModel, encryptedPrefs)`.

  4. Implementación del patrón *Shield con Retry* (evitando `finish()` para proteger la UX de acuerdo a la Regla IV)\[cite: 2\].

  5. Creación de pruebas unitarias en `BiometricAuthUseCaseTest.kt`.

### CR-09: Restauración de Diferenciación Cromática de Trayectos

- **Estado:** 📌 **Registrado y Pendiente de Auditoría.**

- **Incidencia:** Pérdida de variantes cromáticas en el dibujado de polylineas/trayectos (se visualizan de color uniforme).

- **Hoja de Ruta:**

  1. Auditar `LocationService` y `RoutePointEntity` para verificar la persistencia del campo `speed`.

  2. Auditar `RouteColorizerUseCase.kt` para evaluar asignación de rangos térmicos/velocidades.

  3. Inspeccionar el renderizado de `Polyline` en `MapScreen.kt` (`osmdroid`).

### CR-10: Agrupación Cromática por Día y Submenú 4.2.4

- **Estado:** 📌 **Registrado e Integrado en la Especificación.**

- **Requerimiento:** Agrupar recorridos por día con distinción cromática por fecha en la lista. Ampliar el submenú contextual `4.2.4` (botón `\\\[⁝\\\]`) en la pantalla de detalle.

- **Especificación del Submenú 4.2.4 (Menú Ruta):**

  - `4.2.4.1` `\\\[📤\\\]` Compartir ruta

  - `4.2.4.2` `\\\[🔽\\\]` Colapsar grupos *(NUEVO)*

  - `4.2.4.3` `\\\[🔼\\\]` Desplegar grupos *(NUEVO)*

  - `4.2.4.4` `\\\[🗑️\\\]` Eliminar ruta (Rojo)

- **Nota de Diseño:** Los botones de exportar y ayuda no pertenecen a este submenú contextual (el botón de ayuda reside en la barra inferior global `1.1.4`).

### CR-11: Reestructuración Geográfica de Favoritos y Limpieza UI (Pantalla 3)

- **Estado:** 📌 **Registrado e Integrado en la Especificación.**

- **Requerimiento:**

  1. **Jerarquía Geográfica Anidada:** Agrupar los favoritos en tres niveles anidados: **País $\\rightarrow$ Ciudad/Localidad $\\rightarrow$ Barrio/Distrito**.

  2. **Formato de Título:** El nombre mostrará la dirección concreta (Calle y Altura) o referencia provista. Si no la hay, mostrará las coordenadas formateadas ($Lat, Lon$).

  3. **Limpieza de TopBar:** La barra superior (`3.3.1`) contiene únicamente el título `Favoritos y Destacados` sin botones.

  4. **Sub-barra Inferior (3.2):** Aloja a la **izquierda** los controles `3.2.2 \\\[🔽 Colapsar Países\\\]` y `3.2.3 \\\[🔼 Desplegar Todo\\\]`, y a la **derecha** el indicador de estado `3.2.1 (🟢 GPS: 3m)`.

  5. **Depuración de Concepto:** Eliminación total del concepto y botón de "Agregar Contacto" (`3.4.1`). Un favorito es una ubicación geográfica, no un contacto.

  6. **FAB Único:** Se mantiene un solo botón flotante `3.4.1 \\\<📍 ➕\\\>` abajo a la derecha para agregar nuevo favorito.

## 4. Esquema Actualizado de Pantalla 3 (SavedLocationsScreen)

\_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_

| |  3.3.1 \[ Favoritos y Destacados \]                                               |  ← TopBar: Solo Título |

| |

| |  🌍 España

| |  └── 🏙️ Madrid

| |      └── 🏡 Centro (Barrio)

| |          ┌──────────────────────────────────────────────────────┐

| |          │ 3.5.N  \[🏠\] Casa de la abuela (Calle Mayor 15)                   │

| |          │        \[📞 Llamar\] \[✉️ Email\] \[🗺️ Maps\] \[🎯Ver\] \[✏️\] \[🗑️\]         │

| |          └──────────────────────────────────────────────────────┘

| |

| |                                                                   \< 📍 ➕ \>     |  ← FAB único | | |

| | 3.2.2 \[🔽 Colapsar Pais\]   3.2.3 \[🔼 Desplegar Todo\]      3.2.1 (🟢 GPS: 3m)   |  ← Sub-barra 3.2 | \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_ \_

| |\# 1.1.1 \[Mapa\] \# 1.1.2 \[Fav\] \# 1.1.3 \[Hist\] \# 1.1.4 \[Ayuda\]                    \#

| |\#              \#  🔲 VERDE   \#              \#                                    \#

| |\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#\#|

## 5. Directivas Estrictas de Desarrollo (Suite Bitaru)

### Lo que SÍ se hace

- **Guardianes de Nulidad Tempranos:** En `LocationService`, la primera línea ante un callback debe ser: `if (intent == null) return`\[cite: 3, 5\].

- **Desempacado Limpio:** Las funciones nativas como `intent.getIntExtra(KEY, default)` se consumen directamente sin operadores Elvis (`?:`) redundantes\[cite: 3, 5\].

- **Aislamiento del Ciclo de Vida en Mapas:** Capturar variables de configuración global (como densidades con `LocalDensity.current.density`) **antes** de entrar al bloque imperativo de `AndroidView`\[cite: 3, 5\].

- **Limpieza Manual de Cabeceras:** Remover importaciones ociosas mediante revisión visual archivo por archivo\[cite: 3, 5\].

### Lo que NO se hace bajo ninguna circunstancia

- 🚫 **PROHIBIDO - Optimize Imports Automatizado Masivo:** Elimina por error los delegados de Compose (`getValue`/`setValue`)\[cite: 3, 5\].

- 🚫 **PROHIBIDO - Migrar Recursos en Entornos Imperativos:** Conservar `context.resources` exigido por librerías de terceros (`osmdroid`)\[cite: 3, 5\].

- 🚫 **PROHIBIDO - Lógica de Negocio en ViewModels:** Si el ViewModel excede las 400 líneas, extraer a `UseCase` especializado\[cite: 3, 5\].

- 🚫 **PROHIBIDO - Mutar Firmas Inactivas por Estética:** Eliminar parámetros huérfanos completamente en lugar de comentarlos\[cite: 3, 5\].

