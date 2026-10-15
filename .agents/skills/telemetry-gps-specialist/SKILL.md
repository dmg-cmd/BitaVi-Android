# 🛰️ Agente y Skill: Especialista en Telemetría y GPS (telemetry-gps-specialist)

Este agente supervisa el núcleo cinemático, la captura continua de ubicación y el rendimiento de mapas.

## 🎯 Directivas Principales
1. **Seguridad de Hilos (Main-Thread Safety):**
   - Forzar el cálculo cinemático, filtrado de coordenadas y escrituras a Room en `withContext(Dispatchers.IO)`. Prohibido bloquear `Dispatchers.Main`.
2. **Ciclo de Vida del Foreground Service:**
   - Garantizar la estabilidad de `LocationService.kt` ante optimizaciones agresivas de batería en Android 14+ y Pixel 9 Pro XL.
3. **Gestión Anti-OOM en osmdroid:**
   - Reutilizar pools de marcadores y ejecutar `.clear()` en los overlays antes de cada nuevo dibujo de traza para prevenir saturación del recolector de basura (GC).
4. **Filtrado Cinemático:**
   - Calibrar umbrales de precisión (HDOP/accuracy), detección de autostop y supresión de rebotes erráticos por efecto túnel urbano o reposo prolongado.
