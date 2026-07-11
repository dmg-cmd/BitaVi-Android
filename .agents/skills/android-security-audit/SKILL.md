# 🛰️ ANTIGRAVITY ENGINE: REGLAS DE CUMPLIMIENTO OBLIGATORIO v3.2.0

Este documento define las directivas primarias de comportamiento, arquitectura y calidad de código para el desarrollo de la Suite de Telemetría Premium ("Proyecto Localizador") en el Pixel 9 Pro XL. El sistema tiene prohibido violar o ignorar estas reglas bajo cualquier circunstancia.

---

## 🛠️ 1. MODO DE OPERACIÓN Y RESTRICCIONES DE INTERFAZ

*   **PROHIBICIÓN DE SIMULACIÓN:** El sistema NO debe inventar reportes de compilación de Gradle ficticios (ej. "BUILD SUCCESSFUL en 50 segundos"), logs de consola genéricos, ni asumir que ha modificado el almacenamiento local.
*   **FALLBACK DE EJECUCIÓN:** Si el sistema no puede ejecutar comandos reales o leer los archivos locales debido a restricciones de la interfaz, debe detenerse inmediatamente y declarar: "ERROR: No tengo acceso de ejecución, se requiere intervención manual".
*   **FORMATO CERRADO:** Toda entrega de código de producción debe ser completa, íntegra y sin omisiones. Está estrictamente prohibido el uso de comentarios de truncado como `// ... resto del código ...`.

---

## 📐 2. REGLAS DE ORO DE ARQUITECTURA (CLEAN & MVVM)

### 🔹 Modularidad Estricta vs. God Objects
*   `RouteViewModel` actúa únicamente como un orquestador liviano de la interfaz de usuario. Tiene prohibido acumular lógica de negocio.
*   Antes de agregar una nueva función al ViewModel, evalúa si pertenece a un `UseCase` atómico en la capa de dominio o a un `Manager` inyectado por constructor.
*   Evita funciones "top-level" o utilitarios estáticos globales. Toda lógica debe encapsularse en clases inyectables por constructor para garantizar la testabilidad unitaria.

### 🔹 Seguridad de Hilos (Main-Thread Safety)
*   Queda estrictamente prohibido ejecutar operaciones bloqueantes de entrada/salida (I/O), acceso a bases de datos (Room/SQLite), o llamadas IPC del sistema operativo (como `android.location.Geocoder`) en el hilo principal (`Dispatchers.Main`).
*   Toda función que invoque servicios externos o de red fluctuante debe ser una función de suspensión (`suspend`) y forzar explícitamente su contexto en `withContext(Dispatchers.IO)`.

### 🔹 Gestión Defensiva de Memoria (Anti-OOM & GC Thrashing)
*   En la capa de presentación (`MapScreen.kt`), todo renderizado de componentes dinámicos en osmdroid (marcadores de rumbo, líneas de transición) debe reutilizar pools de caché locales (`remember { mutableListOf<Marker>() }`).
*   El sistema debe limpiar explícitamente los overlays con `.clear()` y removerlos del mapa antes de cada ciclo de re-renderizado para evitar fugas en el Heap de memoria y micro-tirones de FPS.

---

## 🎨 3. ESTÁNDAR VISUAL MATERIAL DESIGN 3 (MD3)

*   **UNIFICACIÓN CROMÁTICA:** El modo de caminata (`WALKING`) está asignado de manera inmutable al **Naranja Premium (`#FF6D00` / 0xFFFF6D00)**. Cualquier traza, panel del Dashboard o selector de color de favoritos debe respetar esta paleta.
*   **JERARQUÍA DE INFORMACIÓN:** Prioriza la legibilidad rápida en campo. Las métricas críticas de telemetría (como la velocidad) deben conservar su tamaño premium (`32.sp`), y las secundarias (como la altitud `ALT`) deben sanearse a colores neutros (blanco/gris) para no saturar la UI con iconos o contrastes redundantes.

---

## 🚨 4. DIRECTIVA DE CERO ADVERTENCIAS (COMPILER OBSESSION)

*   El código de producción entregado debe estar completamente limpio de advertencias del compilador de Kotlin (*Warnings*).
*   Se exige la eliminación absoluta de importaciones no utilizadas, variables muertas y código muerto.
*   **Aislamiento de Obsolescencias:** Si el framework de Android exige el uso de métodos deprecados para mantener la compatibilidad hacia atrás (ej. APIs previas a Android 13 en el Geocoder), el uso de `@Suppress("DEPRECATION")` debe ser quirúrgico, estar encapsulado dentro del UseCase de la capa de dominio y nunca filtrarse a la UI o al ViewModel.

---

## 📑 5. PROTOCOLO DE DOCUMENTACIÓN CIENTÍFICA

Antes de entregar cualquier bloque de código que altere lógica compleja (filtros cinemáticos de GPS, lógicas de autostop, o reingeniería de sincronización asíncrona), el sistema debe exponer de forma concisa el razonamiento técnico detrás de la solución, detallando:
1. El problema de rendimiento o arquitectura detectado.
2. La mitigación del riesgo en el Pixel 9 Pro XL.
3. El mapa de intercambio de hilos (`Dispatchers`).