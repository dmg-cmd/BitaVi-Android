# Reglas del Proyecto Localizador

## Protocolo de Documentación y Versionado Automático
- **Archivos Walkthrough**: Al finalizar un plan de trabajo o una sesión de desarrollo que genere un informe de verificación (walkthrough.md), el agente debe guardar dicho informe en la carpeta `Documentacion/Walkthroughs/` del espacio de trabajo. El archivo debe nombrarse utilizando la fecha y hora de ocurrencia bajo el formato `walkthrough_AAAAMMDD_HHMM_[tema].md`.
- **Incremento Automático de Versión (OBLIGATORIO)**: Cada vez que se apliquen modificaciones en el código de la aplicación y se compile/despliegue una nueva versión, el agente DEBE incrementar automáticamente la versión de forma sincronizada en los 3 puntos de referencia sin esperar a que el usuario lo pida:
  1. `app/build.gradle.kts` (`versionCode` +1 y `versionName = "X.Y.Z"`)
  2. `app/src/main/java/com/aistudio/localizador/ui/components/TopBarLocalizador.kt` (`"Infraestructura Suite — vX.Y.Z-RC1"`)
  3. `app/src/main/java/com/aistudio/localizador/ui/components/BrandWatermark.kt` (`version: String = "vX.Y.Z-RC1"`)

## Catálogo de Subagentes Especializados Disponibles

1. **`doc-custodian`**:
   - Guardián de bitácoras, respaldos en `RESPALDO_ANTERIOR/`, actualización de `HISTORIAL_CAMBIOS.md`, `DOCUMENTACION.md` y generación de informes en `Documentacion/Walkthroughs/`.
2. **`telemetry-gps-specialist`**:
   - Especialista en `LocationService.kt`, filtrado cinemático, despachos en `Dispatchers.IO` y optimización de memoria para capas y marcadores de `osmdroid` (Anti-OOM).
3. **`compose-ui-tactical`**:
   - Especialista en Jetpack Compose, codificación `PANTALLA.BARRA.BOTON` (`ESQUEMA_PANTALLAS.md`), constantes semánticas de color (`Color.kt`) y ergonomía táctica (32.sp para métricas principales).
4. **`gradle-compiler-qa`**:
   - Auditor de compilación real con Gradle, política de "Cero Advertencias", depuración de imports no usados y pruebas automatizadas.
5. **`groq-route-analyst`**:
   - Especialista en integración analítica con Groq (Llama 3.3), prompts estructurados, validación de JSON de telemetría y estrategias offline.
6. **`git-release-guardian`**:
   - Especialista en seguridad de repositorios Git/GitHub, prevención estricta de fugas de secretos (`.env`, keystores, API keys, bases de datos locales), commits atómicos, publicación segura y generación de Releases semánticos en GitHub.
7. **`scope-boundary-auditor`**:
   - Auditor de contención de alcance y fronteras modulares. Verifica que cada modificación se restrinja únicamente a los archivos estrictamente involucrados, evitando efectos colaterales (blast radius) y alteraciones no deseadas en flujos periféricos.
8. **`version-sync-guardian`**:
   - Guardián de versionado automático. Incrementa y sincroniza automáticamente la versión en `app/build.gradle.kts`, `TopBarLocalizador.kt` y `BrandWatermark.kt` ante cada modificación compilada o desplegada.


