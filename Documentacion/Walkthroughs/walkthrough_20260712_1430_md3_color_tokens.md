# Walkthrough: Purga de Colores Hardcodeados — Tokens Semánticos MD3
**Fecha:** 2026/07/12 14:30

## Diagnóstico Inicial
DetailScreen.kt contenía 3 literales de color estáticos que rompían la adaptación al tema dinámico MD3:
- Linea 170: Color.LightGray en placeholder de foto de waypoint
- Linea 373: Color.Green y Color.Gray en toggle de vinculación de favoritos
- Lineas 475, 479: AndroidColor.GRAY en ejes de MPAndroidChart (preservado: API externa)

## Cambios Realizados
### Linea 170 — Placeholder AsyncImage
- Antes: .background(Color.LightGray)
- Despues: .background(MaterialTheme.colorScheme.surfaceVariant)
- Efecto: El placeholder de carga adopta el color secundario del tema (claro/oscuro segun corresponda)

### Linea 373 — Toggle Vincular/Desvincular en ModalBottomSheet
- Antes: 	int = if (isLinked) Color.Green else Color.Gray
- Despues: 	int = if (isLinked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
- Efecto: El icono usa primary (vinculado) o onSurfaceVariant (desvinculado), adaptandose al tema diurno/nocturno con contraste garantizado

### Lineas 475, 479 — Ejes MPAndroidChart
- Sin cambios: AndroidColor.GRAY preservado por ser API externa al ecosistema Compose (Regla de Oro II)

## Verificacion
- Lint: 0 errores, 47 warnings, 12 hints (sin regresion)
- Compilacion: BUILD SUCCESSFUL (25s)