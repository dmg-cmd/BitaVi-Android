# Walkthrough - Aislamiento del Botón de Menú con zIndex Global 99f

Este walkthrough describe el despliegue técnico y la homologación visual del aislamiento táctil del botón de menú en `MainActivity.kt` y la remoción de la lógica duplicada en `MapScreen.kt`.

## Cambios Realizados

1. **Aislamiento en `MainActivity.kt`**:
   - **Contenedor Box Maestro**: Envoltura del `Scaffold` completo en un `Box(modifier = Modifier.fillMaxSize())` maestro para establecer la jerarquía absoluta de apilamiento en la actividad.
   - **Contenedor Box Autoportante**: Inyección de un `Box` flotante local de tamaño completo (`fillMaxSize()`) con padding adaptativo de barra de estado (`statusBarsPadding()`).
   - **Disparador Externo Superior**: Reubicación del `IconButton` (icono `Icons.Default.Menu`) dentro de este `Box` flotante independiente de Compose, alineado en la esquina superior izquierda (`align(Alignment.TopStart)`).
   - **Área Táctil Inmune**: Configuración de un **zIndex de nivel 99f** (`.zIndex(99f)`) y un fondo translúcido oscuro con círculo recortado (`CircleShape`) para que el botón flote sobre la lona asíncrona de OsmDroid sin riesgo de secuestro táctil.

2. **Remoción de Duplicidad en `MapScreen.kt`**:
   - Remoción del `IconButton` de la hamburguesa interna en la pantalla del mapa para delegar la interactividad del Drawer en la actividad principal, pasando el callback `onMenuClick` en la firma de `MapScreen`.

## Estado del Build y Verificación Estática

- Comando ejecutado:
  ```powershell
  .\gradlew assembleDebug
  ```
- **Resultado del Build**: **BUILD SUCCESSFUL** en 30 segundos sin errores ni advertencias de compilación.
