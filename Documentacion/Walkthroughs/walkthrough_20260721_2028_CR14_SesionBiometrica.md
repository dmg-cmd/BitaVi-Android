# Walkthrough CR-14: Gestión de Sesión Biométrica Reactiva

**Fecha:** 2026-07-21
**Build:** 2m 52s
**Resultado:** compileDebugKotlin OK, assembleDebug BUILD SUCCESSFUL, testDebugUnitTest 74 tests (4 nuevos pasan, 9 fallos pre-existentes)

## Resumen de Cambios

### Archivos Creados (2)
1. **ui/security/BiometricSessionManager.kt** — @Singleton con StateFlow<Boolean> isUnlocked, ventana de gracia 30s, lockExplicitly(), onAppBackgrounded(), onAppForegrounded()
2. **test/.../ui/security/BiometricSessionManagerTest.kt** — 4 tests JVM puros (inicial, desbloqueo, gracia 30s, bloqueo explícito)

### Archivos Modificados (1)
3. **MainActivity.kt**:
   - Inyectado biometricSessionManager
   - Eliminado isAppUnlocked (variable local mutableStateOf)
   - Gating: iewModel.isBiometricEnabled && !biometricSessionManager.isUnlocked
   - onStart → onAppForegrounded() + triggerBiometricCheck()
   - onStop → onAppBackgrounded()
   - triggerBiometricCheck refactorizada con early-return condicional
   - Drawer 8.7 cableado: activar → lockExplicitly(), desactivar → onAuthenticationSuccess()

### Archivos NO Modificados (intencionalmente)
- RouteViewModel.kt — 0 cambios (toggleBiometric intacto)
- BiometricAuthUseCase.kt — 0 cambios
- BiometricLockScreen.kt / BitaruBrandHeader.kt — 0 cambios
- PreferencesManager.kt — 0 cambios

## Reglas de Oro cumplidas
- **Regla I (Modularidad)**: Toda la lógica de sesión en BiometricSessionManager. Cero en ViewModel.
- **Regla II (Código Seguro)**: 4 tests JVM puros.
- **Regla III (Cero Advertencias)**: Compilación limpia.
- **Regla IV (UX M3)**: Gracia 30s evita re-autenticaciones molestas.
- **Regla V (Justificación)**: lockExplicitly cableado a Drawer 8.7.

## Comandos verificados
.\gradlew.bat testDebugUnitTest assembleDebug — COMPILACIÓN OK (74 tests, 4 nuevos pasan)
