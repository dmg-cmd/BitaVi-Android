# Informe de Verificación: Corrección del Motor de Flechas Direccionales en Rutas

**Fecha y hora:** 2026-10-19 14:46 (UTC-3)  
**Versión de la aplicación:** `v3.2.7-RC1` (`versionCode = 7`)  
**Módulos intervenidos:** `RouteDirectionArrowsOverlay.kt`, sincronización de versión.

---

## 1. Diagnóstico del Problema

El usuario reportó la siguiente irregularidad:
> *"no todas las rutas le pone la flechita la mayoria si peor no a todas"*

### Causa Raíz Técnica

El algoritmo anterior en `RouteDirectionArrowsOverlay.kt` utilizaba un muestreo rígido por saltos de índice discretos (`var i = effectiveStep`, saltando de 12 en 12 puntos) y evaluaba únicamente la distancia euclidiana entre dos puntos aislados: `pStart = routePoints[i - 2]` y `pEnd = routePoints[i]`.

Esto producía tres fallos estructurales que impedían que ciertas rutas mostraran flechas:

1. **Rutas a pie o de baja velocidad (alta densidad de puntos por metro):**
   - Entre dos puntos registrados con frecuencia de 1 segundo a paso de caminata (~1 m/s), la distancia entre el punto `[i-2]` e `[i]` es de apenas ~2 metros.
   - En niveles de zoom habituales (zoom 12 a 15), 2 metros equivalen a menos de 1 o 2 píxeles en pantalla.
   - La condición de umbral `segmentPx >= 4f * density` (~10 a 12 px) fallaba sistemáticamente en **todos** los intervalos de la ruta. Por ello, las rutas en vehículo o bicicleta rápida (mayor distancia entre puntos) sí mostraban flechas, pero las rutas a pie o pausadas no mostraban ninguna.

2. **Rutas circulares o de ida y vuelta al mismo punto:**
   - El mecanismo de contingencia (`arrowsDrawn == 0`) intentaba calcular el rumbo entre el primer punto (`routePoints.first()`) y el último (`routePoints.last()`).
   - Si el usuario regresaba al mismo lugar de inicio, ambos puntos se encontraban en el mismo píxel (`hypot < 8f * density`), haciendo que la contingencia también fuera descartada y la ruta quedara desprovista de flechas.

3. **Sobreconsumo de memoria por instanciación de GeoPoints:**
   - En cada ciclo del método `draw()`, se instanciaban nuevos objetos `GeoPoint` para proyectar coordenadas, elevando la presión del Garbage Collector en trayectos largos.

---

## 2. Solución Implementada

### A. Algoritmo Curvilíneo Acumulativo de Distancia en Pantalla
En [`RouteDirectionArrowsOverlay.kt`](file:///g:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/ui/screens/map/RouteDirectionArrowsOverlay.kt):
- Se sustituyó el muestreo rígido por un **recorrido secuencial continuo** a lo largo de la polilínea, acumulando la distancia proyectada en píxeles (`accumulatedPx`).
- En cuanto la distancia acumulada alcanza el umbral de separación visual ergonómica (`targetSpacingPx = 65f * density`), se calcula el vector direccional efectivo con el punto de anclaje precedente y se dibuja el chevron orientador.
- No importa si los puntos están separados por 50 centímetros o por 50 metros: el acumulador suma el desplazamiento real sobre el trazado, garantizando una cadencia homogénea de flechas en **todas las rutas sin excepción**, independientemente de la velocidad o medio de transporte.
- Para rutas cortas o con baja cantidad de puntos donde no se acumule la distancia completa, se garantiza una flecha en el centro del trazado evaluando los puntos medios consecutivos de la ruta real (funcionando correctamente incluso en circuitos cerrados e ida/vuelta).
- **Optimización Anti-OOM:** Se reutiliza una única instancia mutable `reusableGeoPoint` y puntos fijos `ptPrev`, `ptCurr`, eliminando toda asignación de memoria dentro del bucle de dibujo de osmdroid.

### B. Protocolo de Versionado Automático Sincronizado
- [`app/build.gradle.kts`](file:///g:/Sandbox/BitaVi-Android/app/build.gradle.kts): `versionCode = 7`, `versionName = "3.2.7"`.
- [`TopBarLocalizador.kt`](file:///g:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/ui/components/TopBarLocalizador.kt): `"Infraestructura Suite — v3.2.7-RC1"`.
- [`BrandWatermark.kt`](file:///g:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/ui/components/BrandWatermark.kt): `version: String = "v3.2.7-RC1"`.

---

## 3. Verificación de Compilación

- Compilación con Gradle: `BUILD SUCCESSFUL` en 1m 12s.
- Verificación limpia sin errores ni advertencias en el compilador Kotlin.
