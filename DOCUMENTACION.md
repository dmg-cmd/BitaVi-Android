# Guía de Uso y Documentación Técnica: Localizador Pro

## Descripción General
Localizador Pro es una herramienta avanzada de seguimiento GPS para Android, diseñada para usuarios que requieren precisión técnica, automatización inteligente y un diario multimedia de sus recorridos. Funciona de forma autónoma, sin depender de servicios en la nube para el almacenamiento de rutas.

## Guía de Uso (Manual del Usuario)

### 1. Gestión de Rutas
- **Grabación Manual:** Pulsa el botón "Grabar" en el mapa. Puedes ajustar la frecuencia del GPS antes de iniciar.
- **Grabación Automática (Modo Automático):** Actívalo en el menú lateral. La app vigila el movimiento en segundo plano y grabará sola al detectar un desplazamiento real (>25m confirmado). El sistema ignora ruidos estáticos del GPS y se detiene automáticamente tras 15 minutos de inactividad total.
- **Modo Avión:** Actívalo para registrar vuelos comerciales (hasta 1200 km/h). Si está apagado, filtra ruidos por encima de 160 km/h.

### 2. Puntos de Interés y Multimedia
- **Marcar Punto:** Durante una ruta, usa el botón del marcador para guardar un lugar. Puedes añadir un comentario, el nombre del autor y capturar una foto (Cámara o Galería).
- **Favoritos:** Mantén pulsado cualquier punto del mapa para guardarlo. Se organizarán solos por País y Ciudad. Ahora puedes elegir el **color de la estrella** y editar cualquier dato de un favorito existente (nombre, contacto, color) tocando el icono de lápiz.
- **Importar Contactos (Agenda):** (Novedad Fase 2) Puedes importar datos directamente de la agenda de tu teléfono. El botón de "Importar contacto" lee automáticamente el nombre, dirección, teléfono y correo electrónico para crear un favorito al instante.
- **Modo Cerca de mí:** (Novedad Fase 3) Si activas este ajuste en el menú lateral, la app te notificará cuando estés a menos de 100 metros de una de tus estrellas guardadas.
- **Compartir Trayecto con Mapa:** Envía un resumen visual a WhatsApp que incluye una captura de tu mapa de ruta y todas las fotos de los puntos marcados.
- **Inteligencia Anti-Error:** La app filtra automáticamente "saltos" de GPS y solo arranca el modo automático si detecta que te mueves de verdad a más de 1.5 km/h.
- **GPS en Fotos:** Al elegir una foto de la galería, la app puede detectar su ubicación original (GPS EXIF) y ofrecerte guardar el favorito allí.
- **Contactos:** Marca la casilla "Es un Contacto" para vincular una dirección con un teléfono y email.

### 3. Seguridad y Respaldo
- **Protección Biométrica:** Actívala en el menú lateral para proteger el acceso a configuraciones críticas y el borrado de datos usando tu huella o rostro. Sustituye al antiguo sistema de PIN para una experiencia más fluida y moderna.
- **Copia de Seguridad:** Uso de ZIP con fotos integradas.

### 4. Análisis de Datos
- **IA Narrativa (Groq)**: En el detalle de la ruta, pulsa "Analizar con IA". El sistema enviará tus estadísticas técnicas (Km, Tiempo, Velocidades) y tus marcadores a Llama 3 para generar un resumen profesional y motivador de tu viaje. Requiere una Groq API Key configurada en los ajustes.
- **Exportación:** 
    - **GPX:** Para usar en Google Earth o Strava.
    - **Excel (CSV):** Para analizar velocidades y altitudes en tu PC.

## Especificaciones Técnicas
- **Arquitectura:** MVVM (Model-View-ViewModel) con Jetpack Compose.
- **Base de Datos:** Room (Versión 6), con migraciones para soporte multimedia.
- **Motor de IA:** Groq API (Llama 3.3 70B) vía Retrofit.
- **Mapas:** OpenStreetMap (osmdroid) con caché persistente de 500MB.
- **Seguridad:** Gestión de permisos dinámicos y almacenamiento privado para archivos multimedia.

---
*Documentación actualizada al 13 de Junio de 2024.*
