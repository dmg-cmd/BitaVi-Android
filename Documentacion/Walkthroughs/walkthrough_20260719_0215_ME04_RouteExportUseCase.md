# Walkthrough ME-04: Migracion de RouteExportUseCase a domain/usecase
**Fecha**: 2026-07-19
**Hito**: ME-04 (Desacoplamiento de Exportacion/Importacion)

## Resumen
RouteExportUseCase movido de la capa ui a domain/usecase/, con API Result<T> unificada,
eliminacion total de Toasts, y desacoplamiento de Intents via SharedFlow reactivo.

## Archivos Creados
1. domain/usecase/RouteExportUseCase.kt - Nuevo use case con constructor @Inject(Context, Repository, Exchanger)
2. domain/usecase/RouteExportUseCaseTest.kt - 9 tests unitarios

## Archivos Modificados
1. di/AppModule.kt - import actualizado a domain.usecase.RouteExportUseCase
2. ui/RouteViewModel.kt - Nuevo _shareIntentTrigger, metodos L540-569 reescritos con onSuccess/onFailure
3. MainActivity.kt - Nuevo LaunchedEffect(shareIntentTrigger) que ejecuta startActivity()

## Archivos Eliminados
1. ui/RouteExportUseCase.kt - Reemplazado por version en domain/

## Decisiones de Diseno (Regla de Oro V)
- SRP estricto: UseCase serializa, ViewModel orquesta efectos, UI lanza Intents
- exportRouteAsZip() delega en exportZip() sin duplicacion
- Canal SharedFlow para Intents evita inyectar Context en ViewModel
- No se mezclo EXIF extraction (responsabilidad de ExtractPhotoLocationUseCase)

## Verificacion
- assembleDebug: BUILD SUCCESSFUL
- installDebug: Instalado en 2 dispositivos Pixel 9 Pro XL
- Tests compilan (OOM preexistente en CI)

## Backups
- RESPALDO_ANTERIOR/20260719_RouteExportUseCase.kt
- RESPALDO_ANTERIOR/20260719_RouteViewModel.kt
- RESPALDO_ANTERIOR/20260719_AppModule.kt
- RESPALDO_ANTERIOR/20260719_MainActivity.kt