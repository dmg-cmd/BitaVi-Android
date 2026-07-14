# Walkthrough: Correccion de Contraste en Placeholder AsyncImage
**Fecha:** 2026/07/12 14:50

## Diagnostico
El placeholder de carga de foto de waypoint (DetailScreen.kt:170) usaba MaterialTheme.colorScheme.surfaceVariant, que es demasiado claro en modo diurno y solo se distingue en modo nocturno.

## Cambio
- **Antes**: .background(MaterialTheme.colorScheme.surfaceVariant)
- **Despues**: .background(MaterialTheme.colorScheme.surfaceContainerHigh)
- **Efecto**: Visible en modo diurno (gris medio) y nocturno (gris claro). Token semantico MD3 disponible desde M3 1.2+.

## Verificacion
- Build: BUILD SUCCESSFUL (1m 22s)
- Lint: 0 errores, 47 warnings, 12 hints