# Plan de Trabajo: Refactorización y Mejoras Arquitectónicas
**Proyecto:** Localizador  
**Autor:** Antigravity (Google DeepMind)  
**Fecha:** 2026/07/19  
**Estado:** PLANIFICADO (Modo Solo Lectura)

Este documento define el plan de trabajo estructurado paso a paso para implementar de forma segura las recomendaciones arquitectónicas propuestas en el archivo [RECOMENDACIONES.md](file:///d:/Google%20IA%20Studio/localizador/Documentacion/RECOMENDACIONES.md). El objetivo es asegurar que la aplicación siga compilando y mantenga su estabilidad funcional en cada fase del proceso.

---

## 🛠️ Principio de Refactorización Segura: "Compilación Continua"
Para evitar romper el estado de la aplicación durante cambios estructurales mayores, cada tarea debe finalizar con una verificación de compilación exitosa. No se deben acumular cambios masivos sin probar el hilo de ejecución principal.

---

## 📌 Fases del Plan de Trabajo

### Fase 1: Preparación del Terreno e Infraestructura (Esfuerzo: Bajo | Prioridad: Alta)
*El objetivo de esta fase es estructurar el proyecto y limpiar elementos menores antes de separar la lógica compleja.*

1. **Crear Estructura de Paquetes:**
   Crear los directorios físicos en la estructura del código (`app/src/main/java/com/aistudio/localizador/`):
   * `ui/screens/` (Para las pantallas composables).
   * `ui/dialogs/` (Para diálogos y overlays).
   * `ui/theme/` (Para temas y estilos, si aplica).
   * `data/` y `data/preferences/` (Para la futura migración a DataStore).
   * `util/` (Para herramientas genéricas y loggers).

2. **Crear `AppConstants.kt`:**
   * Crear el archivo `util/AppConstants.kt` y centralizar los umbrales del GPS (velocidad sostenida, intervalos de tiempo de fondo/primer plano) y cachés de mapas.
   * Reemplazar los valores mágicos en `LocationService.kt` y otras clases por estas constantes.
   * **Verificación:** Compilar el proyecto.

3. **Configurar el Logger Centralizado (`AppLogger`):**
   * Implementar `AppLogger.kt` en el paquete `util/`.
   * **Verificación:** Compilar.

---

### Fase 2: Modularización de Pantallas y Diálogos (Esfuerzo: Alto | Prioridad: Alta)
*El objetivo de esta fase es reducir las 1074 líneas de `MainActivity.kt` abstrayendo componentes de la interfaz gráfica.*

1. **Extraer Diálogos de UI:**
   Crear los archivos individuales dentro de `ui/dialogs/` y migrar su lógica desde `MainActivity.kt`:
   * `ApiKeyDialog.kt`
   * `LocationDialog.kt`
   * `DeleteConfirmDialog.kt`
   * `HelpDialog.kt`
   * *Nota:* Mantener el paso de eventos a través de lambdas para que la lógica de negocio siga residiendo en el ViewModel o Activity por ahora.
   * **Verificación:** Compilar y probar que los diálogos abran correctamente.

2. **Extraer Pantallas de la Interfaz:**
   Crear archivos individuales dentro de `ui/screens/` y migrar la UI:
   * `MapScreen.kt` (Contenedor de osmdroid y controles flotantes).
   * `HistoryScreen.kt` (Lista de tracks e historial de rutas).
   * `SavedLocationsScreen.kt` (Puntos favoritos).
   * `ExperienceMapScreen.kt` (Mapa de experiencias).
   * **Verificación:** Compilar en cada extracción para detectar dependencias de variables privadas rotas.

3. **Simplificar `MainActivity.kt`:**
   * Reducir el archivo para que actúe únicamente como el host básico que inicializa el ViewModel, el sistema biométrico, las verificaciones de permisos del OS y el contenedor principal (`MainContainer`).
   * **Verificación:** Compilar y asegurar que el peso de `MainActivity.kt` se reduzca por debajo de 300 líneas.

---

### Fase 3: Modernización de la Navegación (Esfuerzo: Medio | Prioridad: Media-Alta)
*Reemplazo del enrutamiento manual basado en variables de estado.*

1. **Agregar Dependencia de Navigation Compose:**
   * Añadir `androidx.navigation:navigation-compose` en `build.gradle.kts`.
   * Sincronizar Gradle.

2. **Definir el Grafo de Navegación:**
   * Crear la estructura de rutas usando clases seguras (`@Serializable`).
   * Reemplazar los condicionales del `MainContainer` por un `NavHost` formal.
   * Conectar los botones de navegación de la UI (menú lateral, barra inferior) al `navController`.
   * **Verificación:** Probar la navegación adelante/atrás y la retención del backstack.

---

### Fase 4: Migración de Persistencia no Sensible a DataStore (Esfuerzo: Medio | Prioridad: Media)
*Eliminación progresiva del uso de SharedPreferences tradicionales.*

1. **Configurar DataStore:**
   * Agregar dependencias de DataStore Preferences en `build.gradle.kts`.
   * Crear el gestor de preferencias `UserPreferencesRepository.kt`.

2. **Migrar Campos de Configuración:**
   * Configurar la migración automática desde las SharedPreferences actuales de UI (modo avión, modo sin conexión, etc.) para no perder las elecciones del usuario.
   * Reemplazar las llamadas síncronas de lectura/escritura por flujos asíncronos (`Flow`) en los ViewModels.
   * **Verificación:** Compilar y asegurar que las configuraciones de usuario persistan al reiniciar la aplicación.

---

### Fase 5: Tareas en Segundo Plano y Robustez (Esfuerzo: Medio | Prioridad: Media)
*Optimización de batería y robustez en la integración de IA.*

1. **Configurar WorkManager:**
   * Integrar WorkManager para tareas de mantenimiento periódicas (limpieza de cachés de mapas, sincronizaciones automáticas diarias).
   * Liberar al `LocationService` de tareas que no pertenezcan al tracking GPS en tiempo real.

2. **Implementar Reintento Exponencial en Red:**
   * Aplicar la función utilitaria `withRetry` sobre las llamadas de la API de Groq para evitar fallos si el usuario pasa por zonas de túneles o cobertura intermitente.
   * **Verificación:** Realizar pruebas de simulación de pérdida de red en el emulador de Android.

---

## 📈 Resumen de Pasos de Compilación y Control

| Paso | Tarea | Impacto UI | Requiere Gradle Sync | Compila (Check) |
|---|---|---|---|---|
| 1.1 | Estructura de paquetes | Ninguno | No | [ ] |
| 1.2 | `AppConstants.kt` | Ninguno | No | [ ] |
| 2.1 | Extraer diálogos a `ui/dialogs` | Medio | No | [ ] |
| 2.2 | Extraer pantallas a `ui/screens` | Alto | No | [ ] |
| 3.1 | Implementar Navigation Compose | Alto | Sí | [ ] |
| 4.1 | Migración a DataStore | Bajo | Sí | [ ] |
| 5.1 | Integrar WorkManager | Ninguno | Sí | [ ] |
