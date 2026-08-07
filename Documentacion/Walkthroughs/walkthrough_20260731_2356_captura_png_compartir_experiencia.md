# Walkthrough CR-26: Captura PNG de Experiencia — 20260731_2356

## Objetivo
Validar que el botón Compartir (5.2.1, cian) de la Pantalla 5 (ExperienceMapScreen) comparte una captura PNG HD del mapa de experiencia en lugar de enviar fotos de waypoints sueltas.

## Cambios Aplicados (ExperienceMapScreen.kt)
1. Imports nuevos: rememberCoroutineScope, Dispatchers, launch, drawWithContent, asAndroidBitmap, layer.drawLayer, rememberGraphicsLayer, FileProvider.
2. Estados: val composeGraphicsLayer = rememberGraphicsLayer() y val coroutineScope = rememberCoroutineScope().
3. Box capturable interno con Modifier.fillMaxSize().drawWithContent { composeGraphicsLayer.record { this@drawWithContent.drawContent() }; drawLayer(composeGraphicsLayer) } envolviendo mapa (AndroidView osmdroid), panel de fotos y controles flotantes; cerrado antes de la Surface de la barra inferior.
4. Botón Compartir: coroutineScope.launch(Dispatchers.IO) { runCatching { toImageBitmap() → asAndroidBitmap() → PNG (calidad 100) en cacheDir/shared_experiences/experiencia_{routeId}.png → FileProvider.getUriForFile → ACTION_SEND image/png con chooser "Compartir Experiencia..." } }.
5. Eliminada la variable huérfana currentPhotoPaths.

## Verificación Automatizada
- testDebugUnitTest: 77 tests, 9 fallidos (preexistentes, baseline conocido). Sin tests nuevos fallidos.
- assembleDebug: BUILD SUCCESSFUL.

## Verificación Física (Dispositivo 48201FDAS001L5)
- Deploy: adb install -r app-debug.apk → Success.
- App lanzada: com.aistudio.localizador.vqtspk/com.aistudio.localizador.MainActivity.

## Prueba Manual (por el usuario)
- [ ] Navegar a una ruta con waypoints → Pantalla 5 (Mapa de Experiencia).
- [ ] Pulsar Compartir (cian) → debe abrirse el selector nativo con imagen PNG (mapa + ruta + fotos, sin barra inferior).
- [ ] Enviar a WhatsApp/otros → la imagen llega como "tarjeta postal".
- [ ] Pulsar Guardar (ámbar) → sigue exportando el ZIP GPX/CSV sin cambios.

## Resultado
- Backend y UI validados por compilación y deploy. Captura física pendiente de validación manual del usuario.