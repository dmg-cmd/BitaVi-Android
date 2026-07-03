# Guía de Uso y Documentación Técnica: Localizador Pro

## Descripción General
Localizador Pro es una herramienta avanzada de seguimiento GPS para Android, diseñada para usuarios que requieren precisión técnica, automatización inteligente y un diario multimedia de sus recorridos. Funciona de forma autónoma, sin depender de servicios en la nube para el almacenamiento de rutas.

## Guía de Uso (Manual del Usuario)

### 1. Gestión de Rutas
- **Grabación Manual:** Pulsa el botón "Grabar" en el mapa. Puedes ajustar la frecuencia del GPS antes de iniciar.
- **Grabación Automática (Modo Automático):** Actívalo en el menú lateral. La app vigila el movimiento en segundo plano y grabará sola al detectar un desplazamiento real (>25m confirmado). El sistema ignora ruidos estáticos del GPS y se detiene automáticamente tras 5 minutos de inactividad total (estándar de la industria).
- **Modo Avión:** Actívalo para registrar vuelos comerciales (hasta 1200 km/h). Si está apagado, filtra ruidos por encima de 162 km/h.
- **Menú de Acciones `⋮` (Detalle de Ruta):** Las acciones de compartir, exportar y eliminar están agrupadas en un menú de tres puntos. Exportar ofrece submenú con GPX, CSV (Excel) y ZIP. Eliminar ruta muestra un diálogo de confirmación antes de borrar permanentemente los datos.
- **Exportación ZIP Persistente:** Los ZIP exportados se guardan en la carpeta **Documentos** del dispositivo con nombres únicos (ej: `ROUTE_5_Paseo_del_Rio_20260702_143052.zip`) y contienen un árbol estructurado: `track.gpx` (coordenadas estándar GPX 1.1), `telemetry.csv` (matriz con timestamp, lat, lon, altitud, speed_ms, speed_kmh, activity_mode) y `media/` (fotos de la ruta). Al guardar desde el mapa de experiencia, el nombre también sigue el formato ROUTE_{id}_{nombre}_{timestamp}.zip y se conservan incluso tras reiniciar la app.
- **Ayuda en Pantalla:** El menú `⋮` incluye un botón de ayuda que muestra un diálogo con la descripción de cada acción disponible en la pantalla de detalle.

### 2. Puntos de Interés y Multimedia
- **Marcar Punto:** Durante una ruta, usa el botón del marcador para guardar un lugar. Puedes añadir un comentario, el nombre del autor y capturar una foto (Cámara o Galería).
- **Favoritos:** Mantén pulsado cualquier punto del mapa para guardarlo. Se organizarán solos por País y Ciudad. Ahora puedes elegir el **color de la estrella** y editar cualquier dato de un favorito existente (nombre, contacto, color) tocando el icono de lápiz.
- **Importar Contactos (Agenda):** (Novedad Fase 2) Puedes importar datos directamente de la agenda de tu teléfono. El botón de "Importar contacto" lee automáticamente el nombre, dirección, teléfono y correo electrónico para crear un favorito al instante.
- **Modo Cerca de mí:** (Novedad Fase 3) Si activas este ajuste en el menú lateral, la app te notificará cuando estés a menos de 100 metros de una de tus estrellas guardadas.
- **Compartir Trayecto con Mapa:** Envía un resumen visual a WhatsApp que incluye una captura de tu mapa de ruta y todas las fotos de los puntos marcados. Accesible desde el menú `⋮` en el detalle de la ruta.
- **Inteligencia Anti-Error:** La app filtra automáticamente "saltos" de GPS mediante un sistema multicapa: descarta puntos con precisión >20m, velocidad >162 km/h, o demasiado próximos en tiempo y distancia. Solo arranca el modo automático si detectas un paso firme y continuo (>2.5 km/h) durante al menos 10 segundos, eliminando falsos positivos por ruido GPS.
- **GPS en Fotos:** Al elegir una foto de la galería, la app puede detectar su ubicación original (GPS EXIF) y ofrecerte guardar el favorito allí.
- **Contactos:** Marca la casilla "Es un Contacto" para vincular una dirección con un teléfono y email.

### 3. Seguridad y Respaldo
- **Protección Biométrica:** Actívala en el menú lateral para proteger el acceso a configuraciones críticas y el borrado de datos usando tu huella o rostro. Sustituye al antiguo sistema de PIN para una experiencia más fluida y moderna.
- **Copia de Seguridad Pro:** Sistema de respaldo en ZIP con fotos integradas. Totalmente compatible con versiones anteriores; el motor de restauración detecta automáticamente el formato del archivo (de 4 a 13+ columnas de datos) y rellena la información faltante (`country`, `city`, etc.) de forma inteligente.
- **Control Inteligente de Centrado del Mapa:** Al navegar a un favorito desde la lista, el mapa bloqueará el seguimiento automático del GPS por 10 segundos para permitirte ver el lugar sin saltos de cámara. Se ha implementado un bloqueo de hardware sincronizado para evitar que el GPS interrumpa la animación de aproximación. El botón "Mi ubicación" restablece inmediatamente el seguimiento en tiempo real.

### 4. Análisis de Datos
- **IA Narrativa (Groq)**: En el detalle de la ruta, pulsa "Analizar con IA". El sistema enviará tus estadísticas técnicas (Km, Tiempo, Velocidades) y tus marcadores a Llama 3 para generar un resumen profesional y motivador de tu viaje. Requiere una Groq API Key configurada en los ajustes.
- **Exportación (Menú `⋮` > Exportar):** 
    - **GPX:** Para usar en Google Earth o Strava.
    - **CSV (Excel):** Para analizar velocidades y altitudes en tu PC.
    - **ZIP:** Respaldo completo con datos y fotos.
- **Eliminar Ruta (Menú `⋮`):** Borra permanentemente la ruta, sus puntos GPS, waypoints y fotos asociadas tras confirmación del usuario.

## Especificaciones Técnicas
- **Arquitectura:** MVVM (Model-View-ViewModel) con Jetpack Compose.
- **Base de Datos:** Room (Versión 10), con SQLCipher y 5 tablas (routes, location_points, waypoints, saved_locations, route_favorite_cross_ref).
- **Motor de IA:** Groq API (Llama 3.3 70B) vía Retrofit.
- **Mapas:** OpenStreetMap (osmdroid) con caché persistente de 500MB.
- **Filtro GPS:** Sistema multicapa con precisión máxima de 20m, velocidad máxima de 45 m/s (~162 km/h) y delta temporal mínimo de 3s.
- **Seguridad:** Gestión de permisos dinámicos y almacenamiento privado para archivos multimedia cifrados mediante hardware Keystore (API Key).

---
*Documentación actualizada al 29 de Junio de 2026.*