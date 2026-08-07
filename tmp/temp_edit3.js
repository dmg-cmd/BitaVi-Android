const fs = require('fs');
const path = 'D:/Google IA Studio/localizador/app/src/main/java/com/aistudio/localizador/location/LocationService.kt';
let content = fs.readFileSync(path, 'utf8');

content = content.replace(/\r\n/g, '\n');

// 1. Remove import
content = content.replace('import java.util.concurrent.ConcurrentLinkedDeque\n', '');

// 2. Remove field declarations
content = content.replace('\n    private var activeRouteId: Int = -1', '');
content = content.replace('\n    private var totalDistanceMeters: Double = 0.0', '');

// 3. Change sessionManager init
content = content.replace('private val sessionManager = TrackingSessionManager()', 'private lateinit var sessionManager: TrackingSessionManager');

// 4. Remove preRecordingBuffer comment + declaration
content = content.replace('\n    // Buffer el\u00e1stico de pre-grabaci\u00f3n (Fase 3A)', '');
content = content.replace('\n    private val preRecordingBuffer = ConcurrentLinkedDeque<Location>()', '');

// 5. Remove handlePreRecording function
const hprStart = '    private fun handlePreRecording(newLocation: Location) {';
let idx = content.indexOf(hprStart);
if (idx >= 0) {
    let braceCount = 0;
    let endIdx = idx + hprStart.length;
    for (let i = endIdx; i < content.length; i++) {
        if (content[i] === '{') braceCount++;
        else if (content[i] === '}') { braceCount--; if (braceCount === 0) { endIdx = i + 1; break; } }
    }
    content = content.substring(0, idx) + content.substring(endIdx);
}

// 6. Add sessionManager init after repository init
content = content.replace(
    '        repository = RouteRepository(database.routeDao())',
    '        repository = RouteRepository(database.routeDao())\n\n        sessionManager = TrackingSessionManager(repository)'
);

// 7. preRecordingBuffer.clear()
content = content.replace('preRecordingBuffer.clear()', 'sessionManager.clearPreRecordingBuffer()');

// 8. activeRouteId = routeId
content = content.replace('        activeRouteId = routeId', '        sessionManager.startSession(routeId.toLong())');

// 9. totalDistanceMeters = existingRoute?.
content = content.replace('totalDistanceMeters = existingRoute?.distanceMeters ?: 0.0', 'sessionManager.setInitialDistance(existingRoute?.distanceMeters ?: 0.0)');

// 10. _currentDistanceMeters = totalDistanceMeters
content = content.replace('            _currentDistanceMeters = totalDistanceMeters\n', '            _currentDistanceMeters = sessionManager.getTotalDistance()\n');

// 11. handlePreRecording(anchoredLocation)
content = content.replace(
    'handlePreRecording(anchoredLocation)',
    'val bp = TrackingSessionManager.BufferPoint(\n                    latitude = anchoredLocation.latitude,\n                    longitude = anchoredLocation.longitude,\n                    accuracy = anchoredLocation.accuracy,\n                    speed = anchoredLocation.speed,\n                    altitude = anchoredLocation.altitude,\n                    timestamp = anchoredLocation.time\n                )\n                sessionManager.handlePreRecording(bp)'
);

// 12. triggerAutomaticTracking(preRecordingBuffer) call with snapshot
const oldCall = /(\s+)triggerAutomaticTracking\(preRecordingBuffer\) \{ filteredLocations ->\n\s+val pointsToBackfill = filteredLocations\.map \{ it\.toBufferPoint\(0\) \}\n\s+startAutoNewRoute\(pointsToBackfill\)/;
content = content.replace(oldCall, (match, ws) => {
    return ws + 'val snapshot = sessionManager.getPreRecordingBufferSnapshot()\n' +
           ws + 'triggerAutomaticTracking(snapshot) { filteredBpList ->\n' +
           ws + '    val pointsToBackfill = filteredBpList.map { bp ->\n' +
           ws + '        LocationPoint(\n' +
           ws + '            id = 0,\n' +
           ws + '            routeId = 0,\n' +
           ws + '            latitude = bp.latitude,\n' +
           ws + '            longitude = bp.longitude,\n' +
           ws + '            accuracy = bp.accuracy,\n' +
           ws + '            speed = bp.speed,\n' +
           ws + '            altitude = bp.altitude,\n' +
           ws + '            timestamp = bp.timestamp\n' +
           ws + '        )\n' +
           ws + '    }\n' +
           ws + '    startAutoNewRoute(pointsToBackfill)\n' +
           ws + '}';
});

// 13. Odometer references
content = content.replace('totalDistanceMeters += distanceStep', 'sessionManager.addDistanceFromStep(distanceStep.toDouble())');
content = content.replace('repository.updateRoute(currentRoute.copy(distanceMeters = totalDistanceMeters))', 'repository.updateRoute(currentRoute.copy(distanceMeters = sessionManager.getTotalDistance()))');
content = content.replace('val km = totalDistanceMeters / 1000.0', 'val km = sessionManager.getTotalDistance() / 1000.0');

// 14. stopActiveRoute blocks
content = content.replace(
    /if \(activeRouteId != -1\) \{\n\s+repository\.stopActiveRoute\(activeRouteId, totalDistanceMeters, currentMode\.name\)\n\s+\}/g,
    'if (sessionManager.getActiveRouteId() != null) {\n                repository.stopActiveRoute(sessionManager.getActiveRouteId()!!, sessionManager.getTotalDistance(), currentMode.name)\n            }'
);

// 15. activeRouteId = -1 resets
content = content.replace(/^                activeRouteId = -1$/gm, '                sessionManager.endSession()');

// 16. totalDistanceMeters assignments
content = content.replace(/^                totalDistanceMeters = initialDistance$/gm, '                sessionManager.setInitialDistance(initialDistance)');
content = content.replace(/^                totalDistanceMeters = 0\.0$/gm, '                sessionManager.setInitialDistance(0.0)');

// 17. Remove old triggerAutomaticTracking function
const funcStart = '    private fun triggerAutomaticTracking(';
const funcEndStr = '    private fun startAutoNewRoute(backfillPoints: List<LocationPoint> = emptyList()) {';
let si = content.indexOf(funcStart);
let ei = content.indexOf(funcEndStr);
if (si >= 0 && ei > si) {
    content = content.substring(0, si) + '\n' + content.substring(ei);
}

// 18. Insert new triggerAutomaticTracking function
const newFunc = '    private fun triggerAutomaticTracking(\n        bufferSnapshot: List<TrackingSessionManager.BufferPoint>,\n        onHistoryProcessed: (List<TrackingSessionManager.BufferPoint>) -> Unit\n    ) {\n        if (bufferSnapshot.isEmpty()) return\n\n        serviceScope.launch(Dispatchers.IO) {\n            // Bridge BufferPoint -> Location para alimentar el filtro cinem\u00e1tico intacto\n            val platformLocations = bufferSnapshot.map { bp ->\n                Location(\u0022gps\u0022).apply {\n                    latitude = bp.latitude\n                    longitude = bp.longitude\n                    accuracy = bp.accuracy\n                    speed = bp.speed\n                    altitude = bp.altitude\n                    time = bp.timestamp\n                }\n            }\n\n            // Aplicar filtro cinem\u00e1tico lineal con una ventana limpia para depurar la traza inicial\n            val cleanCinematicFilter = com.aistudio.localizador.location.manager.CinematicFilterManager(\n                maxAllowedSpeedMps = AppConstants.Location.MAX_SPEED_METERS_PER_SECOND.toDouble(),\n                autoStopSpeedMps = AppConstants.Location.AUTO_STOP_SPEED_MPS.toDouble()\n            )\n            val filteredHistory = platformLocations.mapNotNull { location ->\n                if (location.accuracy <= MAX_GPS_ACCURACY) {\n                    cleanCinematicFilter.filterAndProcess(location)\n                } else {\n                    null\n                }\n            }\n\n            // Mapear Location filtradas de vuelta a BufferPoint\n            val resultBp = filteredHistory.map { loc ->\n                TrackingSessionManager.BufferPoint(\n                    latitude = loc.latitude,\n                    longitude = loc.longitude,\n                    accuracy = loc.accuracy,\n                    speed = loc.speed,\n                    altitude = loc.altitude,\n                    timestamp = loc.time\n                )\n            }\n\n            // Regresar al hilo principal para conmutar el hardware e iniciar la ruta\n            withContext(Dispatchers.Main) {\n                onHistoryProcessed(resultBp)\n            }\n        }\n    }\n\n';
content = content.replace('    private fun startAutoNewRoute(backfillPoints: List<LocationPoint> = emptyList()) {', newFunc + '    private fun startAutoNewRoute(backfillPoints: List<LocationPoint> = emptyList()) {');

// Clean up
content = content.replace(/\n{3,}/g, '\n\n');
content = content.replace(/^[ \t]+\n/gm, '');

fs.writeFileSync(path, content, 'utf8');
console.log('Complete. Size:', content.length);

// Verify no old refs remain
const checks = ['activeRouteId[^s]', 'totalDistanceMeters[^.]', 'preRecordingBuffer[^p]', 'handlePreRecording\\\\(', 'toBufferPoint', 'ConcurrentLinkedDeque'];
const lines = content.split('\n');
checks.forEach(pattern => {
    const re = new RegExp(pattern);
    lines.forEach((line, i) => {
        if (re.test(line)) console.log('  REMAINING at L' + (i+1) + ': ' + line.trim());
    });
});
