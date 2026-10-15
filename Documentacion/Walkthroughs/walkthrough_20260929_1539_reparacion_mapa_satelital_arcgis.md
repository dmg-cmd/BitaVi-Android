# Informe Walkthrough: Reparación del Mapa Satelital (ArcGIS Server Z/Y/X)

- **Fecha y Hora:** 2026-09-29 15:39
- **Tema:** Corrección de la Carga de Teselas del Mapa Satelital Desalineadas o Invertidas
- **Estado:** Implementado, Compilado y Desplegado en Dispositivo Real

---

## 1. Problema Abordado
Al seleccionar la capa "Satélite" en el mapa, se observaban mosaicos desordenados, incoherentes o correspondientes a otras regiones del mundo ("pedazos de otros mapas").

## 2. Diagnóstico de Causa Raíz
- **Convención de URL de Teselas:**
  - El servidor de imágenes satelitales de ArcGIS (`server.arcgisonline.com/.../MapServer/tile/`) estructura sus teselas bajo la convención:
    `.../tile/{z}/{y}/{x}` (donde `y` representa la fila / latitud y `x` la columna / longitud).
  - La clase estándar `XYTileSource` de `osmdroid` construye las URLs asumiendo la convención Slippy Map estándar de OpenStreetMap:
    `.../{z}/{x}/{y}`.
  - Al estar los ejes $X$ e $Y$ invertidos en la petición HTTP, ArcGIS devolvía cuadrículas satelitales transpuestas (por ejemplo, sectores del hemisferio norte o del océano en lugar del área geográfica local).
- **Caché en Disco Contaminada:**
  - Las teselas descargadas con la inversión de coordenadas quedaban guardadas en la caché interna de osmdroid bajo el identificador `EsriSatellite`, provocando que incluso al reiniciar se siguieran visualizando fragmentos desalineados.

---

## 3. Solución Implementada
- **`MapTileSourceManager.kt`:**
  - Se creó la clase especializada `ArcGisTileSource` extendiendo `OnlineTileSourceBase`.
  - Se sobreescribió `getTileURLString()` para ordenar explícitamente los parámetros como `{z}/{y}/{x}`:
    ```kotlin
    override fun getTileURLString(pMapTileIndex: Long): String {
        return baseUrl + MapTileIndex.getZoom(pMapTileIndex) + "/" +
                MapTileIndex.getY(pMapTileIndex) + "/" +
                MapTileIndex.getX(pMapTileIndex) + mImageFilenameEnding
    }
    ```
  - Se actualizó el identificador de la fuente a `"ArcGisSatellite_V2"`, garantizando un directorio de almacenamiento en disco limpio y descartando la caché corrupta anterior.

---

## 4. Archivos Respaldados
- `RESPALDO_ANTERIOR/20260929_1537_MapTileSourceManager.kt`

## 5. Resultados de Verificación
- **Compilación Gradle:** `./gradlew assembleDebug` completado exitosamente (`BUILD SUCCESSFUL in 50s`).
- **Despliegue ADB:** APK instalado (`Performing Streamed Install -> Success`) y aplicación **BiTaVi** reiniciada en limpio en **Google Pixel 9 Pro XL**.
