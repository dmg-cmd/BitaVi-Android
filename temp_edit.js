const fs = require('fs');
const path = 'D:/Google IA Studio/localizador/app/src/main/java/com/aistudio/localizador/location/LocationService.kt';
let content = fs.readFileSync(path, 'utf8');

// Add sessionManager init after repository init
content = content.replace(
    '        repository = RouteRepository(database.routeDao())',
    '        repository = RouteRepository(database.routeDao())\n\n        sessionManager = TrackingSessionManager(repository)'
);

// Replace preRecordingBuffer.clear() with TSM call
content = content.replace('preRecordingBuffer.clear()', 'sessionManager.clearPreRecordingBuffer()');

// Replace activeRouteId = routeId with TSM call
content = content.replace('        activeRouteId = routeId', '        sessionManager.startSession(routeId.toLong())');

// Replace totalDistanceMeters = existingRoute?.
content = content.replace('totalDistanceMeters = existingRoute?.distanceMeters ?: 0.0', 'sessionManager.setInitialDistance(existingRoute?.distanceMeters ?: 0.0)');

// Replace _currentDistanceMeters = totalDistanceMeters (in startTracking)
content = content.replace('            _currentDistanceMeters = totalDistanceMeters', '            _currentDistanceMeters = sessionManager.getTotalDistance()');

// Replace handlePreRecording(anchoredLocation) with BufferPoint
content = content.replace(
    'handlePreRecording(anchoredLocation)',
    'val bp = TrackingSessionManager.BufferPoint(\n' +
    '                    latitude = anchoredLocation.latitude,\n' +
    '                    longitude = anchoredLocation.longitude,\n' +
    '                    accuracy = anchoredLocation.accuracy,\n' +
    '                    speed = anchoredLocation.speed,\n' +
    '                    altitude = anchoredLocation.altitude,\n' +
    '                    timestamp = anchoredLocation.time\n' +
    '                )\n' +
    '                sessionManager.handlePreRecording(bp)'
);

// Replace triggerAutomaticTracking(preRecordingBuffer) with snapshot
const oldTriggerCall = 'triggerAutomaticTracking(preRecordingBuffer) { filteredLocations ->\n' +
    '                                val pointsToBackfill = filteredLocations.map { it.toBufferPoint(0) }\n' +
    '                                startAutoNewRoute(pointsToBackfill)';
const newTriggerCall = 'val snapshot = sessionManager.getPreRecordingBufferSnapshot()\n' +
    '                            triggerAutomaticTracking(snapshot) { filteredBpList ->\n' +
    '                                val pointsToBackfill = filteredBpList.map { bp ->\n' +
    '                                    LocationPoint(\n' +
    '                                        id = 0,\n' +
    '                                        routeId = 0,\n' +
    '                                        latitude = bp.latitude,\n' +
    '                                        longitude = bp.longitude,\n' +
    '                                        accuracy = bp.accuracy,\n' +
    '                                        speed = bp.speed,\n' +
    '                                        altitude = bp.altitude,\n' +
    '                                        timestamp = bp.timestamp\n' +
    '                                    )\n' +
    '                                }\n' +
    '                                startAutoNewRoute(pointsToBackfill)';
content = content.replace(oldTriggerCall, newTriggerCall);

// Replace odometer references
content = content.replace('totalDistanceMeters += distanceStep', 'sessionManager.addDistanceFromStep(distanceStep.toDouble())');
content = content.replace('repository.updateRoute(currentRoute.copy(distanceMeters = totalDistanceMeters))', 'repository.updateRoute(currentRoute.copy(distanceMeters = sessionManager.getTotalDistance()))');
content = content.replace('val km = totalDistanceMeters / 1000.0', 'val km = sessionManager.getTotalDistance() / 1000.0');

// Replace stopActiveRoute blocks (3 occurrences)
content = content.replace(
    /if \(activeRouteId != -1\) \{\s+repository\.stopActiveRoute\(activeRouteId, totalDistanceMeters, currentMode\.name\)\s+\}/g,
    'if (sessionManager.getActiveRouteId() != null) {\n' +
    '                repository.stopActiveRoute(sessionManager.getActiveRouteId()!!, sessionManager.getTotalDistance(), currentMode.name)\n' +
    '            }'
);

// Replace activeRouteId = -1 resets
content = content.replace(/^                activeRouteId = -1$/gm, '                sessionManager.endSession()');

// Replace totalDistanceMeters = initialDistance / 0.0 in startAutoNewRoute
content = content.replace(/^                totalDistanceMeters = initialDistance$/gm, '                sessionManager.setInitialDistance(initialDistance)');
content = content.replace(/^                totalDistanceMeters = 0\.0$/gm, '                sessionManager.setInitialDistance(0.0)');

// Remove old triggerAutomaticTracking function
const funcStart = '    private fun triggerAutomaticTracking(';
const funcEnd = '    private fun startAutoNewRoute(backfillPoints: List<LocationPoint> = emptyList()) {';

let startIdx = content.indexOf(funcStart);
let endIdx = content.indexOf(funcEnd);
if (startIdx >= 0 && endIdx > startIdx) {
    content = content.substring(0, startIdx) + '\n' + content.substring(endIdx);
    console.log('Old triggerAutomaticTracking removed');
}

// Insert new triggerAutomaticTracking function before startAutoNewRoute
const newFunc = '    private fun triggerAutomaticTracking(\n' +
    '        bufferSnapshot: List<TrackingSessionManager.BufferPoint>,\n' +
    '        onHistoryProcessed: (List<TrackingSessionManager.BufferPoint>) -> Unit\n' +
    '    ) {\n' +
    '        if (bufferSnapshot.isEmpty()) return\n' +
    '\n' +
    '        serviceScope.launch(Dispatchers.IO) {\n' +
    '            // Bridge BufferPoint -> Location para alimentar el filtro cinemático intacto\n' +
    '            val platformLocations = bufferSnapshot.map { bp ->\n' +
    '                Location("gps").apply {\n' +
    '                    latitude = bp.latitude\n' +
    '                    longitude = bp.longitude\n' +
    '                    accuracy = bp.accuracy\n' +
    '                    speed = bp.speed\n' +
    '                    altitude = bp.altitude\n' +
    '                    time = bp.timestamp\n' +
    '                }\n' +
    '            }\n' +
    '\n' +
    '            // Aplicar filtro cinemático lineal con una ventana limpia para depurar la traza inicial\n' +
    '            val cleanCinematicFilter = com.aistudio.localizador.location.manager.CinematicFilterManager(\n' +
    '                maxAllowedSpeedMps = AppConstants.Location.MAX_SPEED_METERS_PER_SECOND.toDouble(),\n' +
    '                autoStopSpeedMps = AppConstants.Location.AUTO_STOP_SPEED_MPS.toDouble()\n' +
    '            )\n' +
    '            val filteredHistory = platformLocations.mapNotNull { location ->\n' +
    '                if (location.accuracy <= MAX_GPS_ACCURACY) {\n' +
    '                    cleanCinematicFilter.filterAndProcess(location)\n' +
    '                } else {\n' +
    '                    null\n' +
    '                }\n' +
    '            }\n' +
    '\n' +
    '            // Mapear Location filtradas de vuelta a BufferPoint\n' +
    '            val resultBp = filteredHistory.map { loc ->\n' +
    '                TrackingSessionManager.BufferPoint(\n' +
    '                    latitude = loc.latitude,\n' +
    '                    longitude = loc.longitude,\n' +
    '                    accuracy = loc.accuracy,\n' +
    '                    speed = loc.speed,\n' +
    '                    altitude = loc.altitude,\n' +
    '                    timestamp = loc.time\n' +
    '                )\n' +
    '            }\n' +
    '\n' +
    '            // Regresar al hilo principal para conmutar el hardware e iniciar la ruta\n' +
    '            withContext(Dispatchers.Main) {\n' +
    '                onHistoryProcessed(resultBp)\n' +
    '            }\n' +
    '        }\n' +
    '    }\n' +
    '\n';
content = content.replace('    private fun startAutoNewRoute(backfillPoints: List<LocationPoint> = emptyList()) {', newFunc + '    private fun startAutoNewRoute(backfillPoints: List<LocationPoint> = emptyList()) {');

// Remove triple+ blank lines
content = content.replace(/\n{3,}/g, '\n\n');

fs.writeFileSync(path, content, 'utf8');
console.log('Done. File size:', content.length);
