# Informe de Verificación: Geocodificación Automática de Paradas en Favoritos Exclusiva por Wi-Fi

**Fecha y hora:** 2026-10-21 12:45 (UTC-3)  
**Versión de la aplicación:** `v3.2.10-RC1` (`versionCode = 10`)  
**Módulos intervenidos:** `WifiGeocodeFavoritesUseCase.kt`, `AppModule.kt`, `RouteViewModel.kt`, `WifiGeocodeFavoritesUseCaseTest.kt`, sincronización de versión.

---

## 1. Requerimiento del Usuario

El usuario solicitó:
> *"tambien me gustaría que lo s favoritos al tener señal wifi verifique la posición de los mismos en tforma automática y coloque en lugar de Parada 2026... Parada en y la dirección o aproximada si es una situación fuera de una ciudad pero siempre pidiendo wifi y no datos moviles"*  
> *"no coloques en el titulo Parada solo la dirección porque arriba ya dice punto de parada"*

---

## 2. Solución Arquitectónica Implementada

### A. Nuevo Caso de Uso `WifiGeocodeFavoritesUseCase.kt`
- **Condición estricta de Wi-Fi:** Verifica que la red activa disponga del transporte `TRANSPORT_WIFI` y de la capacidad `NET_CAPABILITY_INTERNET`, impidiendo expresamente el consumo de datos móviles (`TRANSPORT_CELLULAR`).
- **Detección de candidatos:** Filtra en la base de datos Room (`SavedLocation`) aquellas paradas que poseen nombre genérico provisional (`Regex("""^Parada\s+\d{4}.*""")`, `Parada 20...`) o cuya dirección aún figura como coordenadas brutas (`GPS: ...` / `Desconocida`).
- **Formateo de títulos sin prefijo redundante:**
  - En zonas urbanas con calle resuelta: asigna directamente el nombre de la calle/número/ruta (ej. `Av. Corrientes 1234` o `Ruta 9 Km 45`), omitiendo la palabra "Parada" dado que la interfaz ya clasifica la tarjeta como "Punto de parada".
  - En situaciones fuera de ciudad (zonas rurales o parajes): asigna directamente el nombre de la localidad, paraje o municipio más cercano (`city`).
  - Actualiza la dirección completa (`address`), ciudad (`city`) y país (`country`) en el registro de Room.
- **Monitoreo reactivo de conectividad:** Incluye `registerWifiCallback` mediante `ConnectivityManager.NetworkCallback` para disparar la resolución en cuanto el dispositivo entra en zona Wi-Fi.

### B. Inyección y Orquestación en `RouteViewModel.kt`
- Registrado como `@Singleton` en `AppModule.kt` e inyectado en `RouteViewModel`.
- En el bloque de inicio (`init`):
  - Verifica si ya existe conexión Wi-Fi disponible y ejecuta la sincronización en `Dispatchers.IO`.
  - Registra el listener reactivo para ejecutarla automáticamente en cuanto se conecte a Wi-Fi.
- Se implementó `onCleared()` para desregistrar el callback y prevenir fugas de memoria.
- Se expuso la función `resolvePendingStopsOverWifi()` para ejecuciones manuales bajo demanda.

### C. Pruebas Unitarias (`WifiGeocodeFavoritesUseCaseTest.kt`)
- `cuando no hay wifi no intenta geocodificar ni actualiza favoritos`: pasa exitosamente.
- `cuando hay wifi actualiza el titulo solo con la direccion sin prefijo Parada`: pasa exitosamente.
- `cuando esta fuera de ciudad y no hay calle coloca la localidad sin prefijo Parada`: pasa exitosamente.

### D. Protocolo de Versionado Automático Sincronizado
- `app/build.gradle.kts`: `versionCode = 10`, `versionName = "3.2.10"`.
- `TopBarLocalizador.kt`: `"Infraestructura Suite — v3.2.10-RC1"`.
- `BrandWatermark.kt`: `version: String = "v3.2.10-RC1"`.

---

## 3. Verificación de Compilación y Calidad

- **Pruebas unitarias:** `.\gradlew testDebugUnitTest` -> `BUILD SUCCESSFUL` (116 pruebas exitosas, 0 fallos).
- **Compilación de empaquetado:** `.\gradlew assembleDebug` -> `BUILD SUCCESSFUL`.
