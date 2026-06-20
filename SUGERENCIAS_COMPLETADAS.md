# Sugerencias Completadas

## Fase 3: Funciones Sociales Avanzadas y Estabilidad (Sesión Actual)
- **Mapa en WhatsApp (Tarea 3.1)**: Captura automática del mapa del recorrido para reportes visuales en WhatsApp.
- **Avisos "Cerca de mí" (Tarea 3.3)**: Notificaciones automáticas al pasar por favoritos guardados.
- **Estabilidad Automática**: Blindaje del modo automático con 3 confirmaciones y filtro de velocidad mínima.
- **Filtro GPS Drift**: Eliminación de saltos de señal imposibles.
- **Buffer de 30 min**: Recuperación de puntos previos al inicio de la grabación.

## Fase 2: Funciones Sociales y Contactos (Sesión Anterior)
- **Importación desde Agenda (Tarea 2.1)**: Botón "Importar Contacto" que usa el Picker de sistema y extrae automáticamente Nombre, Teléfono, Email y Dirección Postal.
- **Compartir Ubicación (Tarea 2.2)**: Generación de mensaje formateado con enlace a Google Maps para WhatsApp/Telegram.
- **Mejora de Interacción Directa**: Uso de `FilterChips` ("Llamar", "Email") en las tarjetas de favoritos para una acción más rápida y profesional.
- **Permisos de Contactos**: Integración total de los permisos `READ_CONTACTS` y `WRITE_CONTACTS`.

## Sesión Anterior
- **Refinamiento del Modo Automático (Tarea 1.1)**: Mejora de la fiabilidad del inicio mediante confirmaciones consecutivas de movimiento y optimización de la pausa por inactividad detectando velocidad. Sincronización de persistencia en ajustes.
- **Descarga de Áreas Offline (Sin éxito)**: Se intentó implementar la descarga masiva de tiles mediante `CacheManager`, pero fue descartada debido a bloqueos de política de los servidores de OpenStreetMap y cierres de la librería osmdroid.
- **Exportar a Contactos**: Botón para crear un contacto real en Android desde un favorito (Uso de Intents de Sistema).
- **Interacción Directa**: Clic en teléfono para llamar o email para redactar correo (Implementado en Favoritos).
- **Edición de Favoritos**: Implementación de la funcionalidad para modificar datos y color de puntos ya existentes.
- **Soporte Biométrico**: Uso de Huella/Cara para proteger API Keys y borrado masivo de datos (Integración con `FragmentActivity`).
- **Estrellas de Colores (Personalización)**: Implementación de selector de colores predefinidos y **selector libre (Hue Slider)** con guardado en base de datos.
- **Interacción Directa**: Llamada telefónica y redacción de email con un solo toque desde la lista de favoritos.
- **Exportar a Contactos**: Botón para crear un contacto real en Android desde un favorito (Uso de Intents de Sistema).
- **Detección GPS en Fotos**: Uso de metadatos EXIF para ubicar favoritos y puntos de ruta.
- **Protección contra Duplicados**: Filtro inteligente durante la restauración de backups.
- **Idioma del Agente**: Pensamiento y comunicación estrictamente en Español.

- **Arreglo del Arranque Automático**: Implementación de vigilancia proactiva y sensibilidad de 25m.
- **Estrellas de Colores (Básico)**: Diferenciación entre favoritos fijos (Dorado) y puntos de ruta (Verde).
- **Diario de Viaje Multimedia**: 
    - Captura de fotos desde Cámara.
    - Importación de fotos desde Galería/Google Fotos.
    - Extracción de coordenadas GPS de fotos (Metadatos EXIF).
    - Campos de Comentario y Autor.
- **Sistema de Contactos**: 
    - Opción "Es un Contacto" en favoritos.
    - Almacenamiento manual de Teléfono y Email vinculado a la dirección.
- **Backup y Restauración Avanzada**:
    - Respaldo ZIP con archivos físicos de imagen.
    - Restauración inteligente que evita duplicados.
    - Reconstrucción de fechas reales en el historial desde el backup.
- **Organización Geográfica**: Clasificación automática de favoritos por País y Ciudad mediante Geocoder.
- **Integración con Google Reviews**: Botón para copiar comentario y abrir sección de opinión en Maps.
