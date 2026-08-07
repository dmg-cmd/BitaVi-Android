# Walkthrough: HOTFIX - SecurityException en LocationService
**Fecha:** 19/07/2026

## Diagnostico
App se cerraba solo al abrir. Causa: SecurityException en 
ContactUseCase.getAllContactPhones() sin permiso READ_CONTACTS.

## Cambios
- **ProximityBleManager.kt**: Hashing de contactos movido DENTRO de 
  ensureBleProximity(), solo se ejecuta al activar BLE, con try-catch
- **LocationService.kt**: Bloque BLE simplificado, bleFirstInitDone eliminado
- **ProximityBleManagerTest.kt**: init() recibe mock ContactUseCase

## Verificacion
- assembleDebug: BUILD SUCCESSFUL
- testDebugUnitTest: 31/31 passing
- installDebug: OK en 2 dispositivos