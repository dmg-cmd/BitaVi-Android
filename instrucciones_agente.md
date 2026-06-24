# Instrucciones del Agente - Protocolo de Trabajo

## Idioma y Pensamiento
- El agente debe comunicarse exclusivamente en **español**.
- El agente debe realizar su proceso de pensamiento (internal monologue/thought) **estrictamente en español**.

## Protocolo de Documentación Automática
Al terminar cada tarea o sesión, el agente DEBE:
1.  **Actualizar `HISTORIAL_CAMBIOS.md`**: Añadir los cambios realizados sin borrar lo anterior.
2.  **Actualizar `DOCUMENTACION.md`**: Reflejar cambios en el uso o funcionamiento de la app.
3.  **Actualizar `SUGERENCIAS.md`**: Marcar lo hecho y moverlo a `SUGERENCIAS_COMPLETADAS.md`.
4.  **No borrar historial previo**: Siempre añadir información nueva al principio o final según corresponda, manteniendo la trazabilidad.

## Plan de Acción Paso a Paso (Hoja de Ruta)

### Fase 1: Estabilización y Visualización (Prioridad 1)
- [x] **Tarea 1.1: Refinamiento del Modo Automático**: Mejorar sensibilidad de detección de movimiento (>25m) y pausa por inactividad.
- [x] **Tarea 1.2: Estrellas de Colores**:
    - Añadir campo `color` a las entidades.
    - Implementar selector de color en diálogos.
    - Renderizar marcadores con colores específicos en el mapa.

### Fase 2: Funciones Sociales y Contactos (Prioridad 2)
- [x] **Tarea 2.1: Integración con Contactos**: Botón para exportar favorito a la agenda del teléfono.
- [x] **Tarea 2.2: Interacción Directa**: Clic en teléfono para llamar o email para redactar correo.

### Fase 3: Mapas Offline y UX (Prioridad 3)
- [x] **Tarea 3.1: Descarga de Mapas**: Utilidad para pre-descargar áreas específicas del mapa.
- [x] **Tarea 3.2: Biometría**: Añadir soporte para Huella/Cara en el acceso a ajustes protegidos.

### Fase 4: Inteligencia Artificial y Multimedia
- [ ] **Tarea 4.1: Consistencia de IA (Groq)**: Unificar prompts, usar datos técnicos reales de la ruta y corregir la integración con la API de Groq.
- [ ] **Tarea 4.2: Análisis Multimodal**: Intentar que la IA analice fotos para el "Diario de Viaje".

---
*Este archivo es la guía maestra para el desarrollo en cualquier máquina.*
