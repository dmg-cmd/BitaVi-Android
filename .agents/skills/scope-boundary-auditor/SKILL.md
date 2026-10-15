# Skill: scope-boundary-auditor

## Propósito y Rol
Auditor de contención de alcance y fronteras modulares. Su objetivo primordial es garantizar que toda modificación en el código fuente se limite única y estrictamente a los módulos y archivos asociados al requerimiento, evitando el fenómeno de "blast radius" o efecto dominó donde arreglar una función rompe o altera módulos no relacionados.

## Directivas Operativas Obligatorias

1. **Delimitación Previa de Alcance (Allowlist de Archivos):**
   - Antes de iniciar cualquier implementación, se debe listar explícitamente qué archivos tienen autorización de ser modificados.
   - Todo archivo fuera de la lista queda estrictamente bloqueado.

2. **Auditoría de Acoplamiento y Dependencias:**
   - Si se modifica la persistencia (`RouteDao`, `RouteRepository`), validar que los contratos con `LocationService` o ViewModels no alteren las llamadas o asunciones sobre el estado del GPS.
   - Si se ajusta un comportamiento de background (`LocationService`, `AutoTrackingManager`), auditar que no se modifiquen interfaces gráficas (`screens/`, `components/`) a menos que haya sido explícitamente ordenado.

3. **Verificación de Impacto en Modos Críticos:**
   - Toda alteración en lógica de paradas debe verificar que el ciclo de reactivación (arranque automático por alejamiento o movimiento sostenido) permanezca 100% funcional.

4. **Principio de Mínima Modificación:**
   - Priorizar cambios atómicos y puntuales sobre refactorizaciones masivas o limpiezas no solicitadas.
