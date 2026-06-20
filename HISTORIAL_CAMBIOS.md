**Historial de Cambios - Proyecto Localizador**

**[Sesión Actual - Fase 3: Social, Proximidad y Estabilidad Extrema]**
- **Mapa en WhatsApp**: Implementación de captura dinámica del mapa (Bitmap) que se adjunta automáticamente como primera imagen al compartir el trayecto.
- **Modo Automático Blindado**: Refinamiento con 3 confirmaciones consecutivas y filtro de velocidad real (>1.5 km/h) para eliminar inicios falsos por ruido de GPS.
- **Avisos "Cerca de mí"**: Motor de notificaciones de proximidad que alerta al usuario cuando pasa a menos de 100m de un favorito guardado.
- **Buffer de Pre-Grabación**: Sistema de memoria circular de 30 min que recupera el inicio de la ruta si el usuario olvida pulsar grabar o tarda en detectarse el movimiento.
- **Filtro Anti-Salto (Drift)**: Algoritmo de verosimilitud que ignora puntos con velocidades imposibles (>180 km/h en tierra) o saltos de señal de más de 500m.
- **Gestión de Caché Segura**: Actualización de `file_paths.xml` para permitir compartir capturas temporales del mapa de forma segura.

**[Sesión Anterior - Fase 2: Funciones Sociales y Contactos]**
- **Importación Inteligente desde Agenda**: Se ha añadido un nuevo botón de acción rápida (PersonAdd) que abre el selector de contactos del sistema. Al elegir un contacto, la app extrae automáticamente su **Nombre, Teléfono, Email y Dirección Postal**, precargando el diálogo de creación de Favorito para mayor agilidad.
- **Compartir Ubicación Pro**: Nueva función para compartir cualquier favorito mediante un Intent de texto plano. Genera un mensaje formateado con el nombre, dirección, nota y un enlace directo a Google Maps, optimizado para WhatsApp y Telegram.
- **Mejora de Interacción (Quick Actions)**: Los clics en teléfono y email en la lista de favoritos han sido rediseñados usando `FilterChip`. Ahora son botones visuales más claros ("Llamar", "Email") que mejoran la accesibilidad y evitan toques accidentales.
- **Permisos de Contactos**: Actualización del Manifiesto con los permisos `READ_CONTACTS` y `WRITE_CONTACTS` para soportar la integración bidireccional con la agenda.
- **Corrección de GPS en Fotos**: Se reparó el error que ignoraba la ubicación de la foto al guardar un favorito desde el mapa. Ahora, si se activa la opción, el punto se guarda en las coordenadas EXIF de la imagen.
- **Rediseño de Tarjetas**: Botón de compartir movido a la cabecera y visualización de coordenadas GPS sobre la foto del favorito.

**[Sesión Anterior - Refinamiento de Modo Automático e IA Groq]**
- **Detección de Movimiento Inteligente**: Se ha mejorado la fiabilidad del inicio automático mediante 2 confirmaciones consecutivas de movimiento (>25m).
- **Pausa Automática Pro**: Optimización del temporizador de 15 minutos detectando velocidad para evitar pausas accidentales.
- **Migración y Consistencia de IA (Groq)**: Se ha unificado el motor de IA para usar exclusivamente Groq (Llama 3.3). Se corrigieron los endpoints de Retrofit y se implementó un prompt técnico avanzado que analiza distancia, duración, velocidades y waypoints reales. Se ha optimizado el consumo de tokens reduciendo la verbosidad del prompt y la respuesta.
- **Corrección de Orientación en Mapa**: Se habilitó `enableFollowLocation()` en el overlay de ubicación y se forzó la actualización del rumbo (bearing) durante el movimiento. Ahora la flecha roja de dirección rotará correctamente para apuntar hacia donde te desplazas.
- **Limpieza de Código**: Eliminación de modelos redundantes y renombrado de variables `geminiApiKey` a `groqApiKey` para mayor claridad arquitectónica.

**[Sesión Anterior - Estabilización y UX]**
- **Limpieza de Código**: Se ha removido la funcionalidad experimental de descarga de mapas offline debido a problemas de compatibilidad con las políticas de servidores de tiles. La app vuelve a su estado estable anterior.
- **Refactorización de ViewModel**: Reparación y limpieza de `RouteViewModel.kt` para asegurar la compilación correcta y restaurar funciones de exportación y gestión de datos.

**[Sesión Anterior - Seguridad Biométrica y UX]**
- **Soporte Biométrico**: Implementación de `androidx.biometric`. Ahora el usuario puede proteger el acceso a la API Key y el borrado masivo de datos mediante Huella Digital o Rostro.
- **Eliminación del PIN**: Se ha removido el sistema de PIN de 4 dígitos, simplificando la seguridad para que dependa exclusivamente de la biometría del dispositivo (o el método de desbloqueo del sistema).
- **Toggle de Seguridad**: Se añadió un interruptor en el menú lateral para activar/desactivar la protección biométrica globalmente.
- **MainActivity Refactor**: Migración de `ComponentActivity` a `FragmentActivity` para dar soporte a los diálogos biométricos del sistema.

**[Sesión Anterior - Funciones Sociales y Personalización Total]**
- **Selector de Color Libre**: Se ha añadido un Slider de Matiz (Hue) en todos los diálogos de creación y edición de puntos. Ahora el usuario puede elegir CUALQUIER color para sus estrellas, no solo los 6 predefinidos.
- **Exportación a Contactos**: Nuevo botón "A Contactos" en los favoritos que permite guardar los datos directamente en la agenda del teléfono (Nombre, Tel, Email, Dirección y Comentario).
- **Interacción Directa**: Los campos de teléfono y email en la lista de favoritos ahora son clicables; al tocarlos se abre el marcador telefónico o el redactor de correos automáticamente.
- **Consolidación de Protocolo**: Revisión y actualización de los archivos de instrucciones y sugerencias.

**[Sesión Anterior - Eliminación Definitiva de Errores Geográficos]**
- **Blindaje contra gRPC UNAVAILABLE**: Se eliminó totalmente la posibilidad de que aparezcan errores técnicos en las direcciones. Ahora, si el servicio de Google falla o está saturado, la app muestra automáticamente las coordenadas **GPS (Lat, Lon)** formateadas.
- **Limpieza de Favoritos**: El botón de refresco (lápiz -> actualizar) ahora detecta automáticamente si el favorito tiene un error viejo y lo sustituye por la ubicación real.
- **Inteligencia en Ríos**: Mejora en la prioridad de nombres para zonas de agua y kilómetros de ruta.

**[Sesión Anterior - Edición de Favoritos y Geocodificación Náutica]**
- **Navegación en Ríos y Campo**: Se mejoró el sistema de identificación de lugares. Ahora, si no hay una calle cerca (ej. estás en un río o zona rural), la app prioriza el nombre del lugar o río (ej. "Río Paraná Km 400") en lugar de dar error.
- **Corrección de "UNAVAILABLE"**: Se eliminó el mensaje de error técnico. Si el servicio de Google falla, la app ahora muestra las coordenadas GPS directamente para mantener la utilidad.
- **Herramienta de Refresco**: Se añadió un botón de **"Actualizar País/Ciudad"** dentro del panel de edición (lápiz) para corregir favoritos que hayan quedado mal guardados por falta de señal.
- **Edición de Favoritos**: Funcionalidad completa para cambiar nombre, autor, comentarios y color de estrella en puntos ya existentes.

**[Sesión Anterior - Estrellas de Colores y Perfeccionamiento de Fotos]**
- **Personalización de Colores**: Implementación de un selector de colores (Dorado, Verde, Azul, Rojo, Violeta, Naranja) para Favoritos y Puntos de Ruta. Los marcadores en el mapa ahora reflejan estas preferencias.
- **Visualización en Lista**: Las tarjetas de Favoritos muestran el icono de la estrella en el color correspondiente.
- **Mejora de Fotos (EXIF)**: La app detecta automáticamente las coordenadas GPS de las fotos y permite al usuario guardar el punto en la ubicación original de la imagen.
- **Filtro de Duplicados**: El sistema de restauración de backup ahora evita duplicar favoritos y rutas existentes.
- **Base de Datos V7**: Actualización del esquema para incluir el campo `color`.
- **Protocolo de Idioma**: Configuración del agente para comunicación y pensamiento exclusivo en Español.

**[Sesión: Consolidación de Proyecto y Protocolo de Agente]**
- **Unificación de Historiales**: Se consolidaron los archivos `historial_cambios.md`, `historial-cambios.md` y `HISTORIAL_CAMBIOS.md` en este único documento centralizado.
- **Protocolo de Documentación**: Establecimiento de la norma de actualización obligatoria de `DOCUMENTACION.md`, `SUGERENCIAS.md` e `HISTORIAL_CAMBIOS.md` tras cada tarea.
- **Gestión de Sugerencias**: Creación de `SUGERENCIAS_COMPLETADAS.md` para mantener limpio el archivo de tareas pendientes sin perder el registro de lo logrado.
- **Manual de Instrucciones**: Actualización de `instrucciones_agente.md` con pautas estrictas de idioma (Español) y hoja de ruta paso a paso.

**[Sesión Anterior - Blindaje Multimedia y Restauración Total]**
- **Respaldo Físico de Fotos**: Se rediseñó el motor de exportación ZIP para incluir físicamente los archivos de imagen. Ahora las fotos viajan dentro del respaldo y se restauran automáticamente en el nuevo dispositivo.  
- **Gestión Inteligente de Galería**: Al elegir una foto de la galería (Google Fotos), la app ahora realiza una copia local en su almacenamiento privado. Esto evita que la foto se pierda en la app si el usuario la borra de su carrete personal.  
- **Corrección de Cámara y Permisos**: Implementación de un flujo de solicitud de permisos explícito para la cámara, eliminando cierres inesperados en Android 13/14. Se optimizó el FileProvider para compartir archivos de imagen de forma segura.  
- **Control de Mapa y Zoom**: Se implementó un temporizador de 12-15 segundos que suspende el auto-centrado del GPS cuando el usuario interactúa manualmente con el mapa o selecciona un favorito.  
- **Flecha de Grabación Resaltada**: El icono de ubicación cambia a color  **ROJO** intenso durante la grabación para mejorar la visibilidad y diferenciar el estado de seguimiento activo.  
- **Integración con Google Reviews**: Nuevo botón "Publicar Opinión" que copia el comentario del favorito al portapapeles y abre la ficha del lugar en Google Maps para facilitar la reseña.  
**[Sesión Anterior - Estabilización, Seguridad e IA]**  
- **Organización por Regiones**: Los favoritos ahora se agrupan automáticamente por  **País** y  **Ciudad** mediante geocodificación inversa.  
- **Seguridad por PIN**: Protección de la API Key y funciones de borrado masivo mediante un PIN de 4 dígitos personalizable (por defecto: 0000).  
- **Migración a Llama 3 (Groq)**: Sustitución de Gemini por Llama 3 para un análisis de rutas instantáneo y sin errores de región/versión.  
- **Centro de Control Lateral**: Creación del menú de "3 rayitas" para organizar todos los ajustes de forma profesional.  
- **Guía de Ayuda Visual**: Inclusión de un manual de usuario con iconos reales dentro de la aplicación.  
**[Sesión Anterior - Automatización y GPX]**  
- **Vigilancia Inteligente**: El Modo Automático ahora mantiene un servicio persistente que inicia la grabación al detectar movimiento (>25m) y la detiene tras 15 min de inactividad.  
- **Importador Profesional**: Procesamiento de archivos GPX gigantes mediante Streaming (XmlPullParser), evitando errores de memoria.  
- **Estadísticas Técnicas**: Cálculo automático de Velocidad Máxima y Promedio, visualizadas en el historial y en gráficos de altitud profesionales (MPAndroidChart).  
