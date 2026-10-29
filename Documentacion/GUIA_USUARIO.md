# 📱 Guía de Usuario - BitaVi (Bitácora Táctica de Rutas)

¡Bienvenido a **BitaVi**! Tu aplicación táctica y profesional para el seguimiento geográfico, telemetría de campo, gestión estructurada de favoritos y análisis inteligente de expediciones.

---

## 🗺️ 1. Mapa y Ventana Superior de Control

La pantalla principal es tu centro de operaciones. Cuenta con cartografía dinámica de alto rendimiento ([`osmdroid`](https://github.com/osmdroid/osmdroid)) y una **ventana de control superior táctica**.

### Ventana Superior Táctica (Telemetría en Vivo):
*   **Métricas Primarias:**
    *   **Modo de Tráfico:** Detección en tiempo real con indicador semántico de color (*🛑 Detenido*, *🚶 Caminata*, *🚴 Bicicleta*, *🚗 Vehículo*, *✈️ Vuelo*).
    *   **Velocidad:** Medición cinemática precisa en km/h.
    *   **Altura:** Altitud sobre el nivel del mar en metros en vivo.
*   **Métricas Complementarias:**
    *   **Distancia 1º Punto:** Cálculo geodésico dinámico continuo respecto al punto de partida de la ruta activa.
    *   **Tiempo Activo:** Horas y minutos de funcionamiento de la sesión de grabación.
*   **Botones al Extremo Superior Derecho:**
    *   **Atenuador de Fondo (`Icons.Default.Opacity`):** Abre el control de transparencia de la ventana superior. Permite regular desde 15% (ultra translúcido) hasta 85% (alto contraste) con preajustes rápidos (*Sutil 25%*, *Equilibrado 45%*, *Contraste 70%*).
    *   **Consultar Ubicación (`Icons.Default.LocationSearching`):** Despliega la dirección física geocodificada en tiempo real de tu posición.

### Cómo Grabar una Ruta:
1.  Pulsa el botón flotante **"Grabar"** (icono `+`).
2.  Ingresa el nombre de tu ruta o acepta el nombre sugerido automáticamente.
3.  Ajusta el intervalo de tiempo/distancia si lo requieres y pulsa **"Iniciar"**.
4.  La traza táctica se dibujará en el mapa con escala cromática según tu velocidad.
5.  Para pausar o concluir, pulsa los controles flotantes laterales de grabación.

### Modos Especiales (en el Menú Lateral):
*   **✈️ Modo Vuelo Automático:** Detección aerodinámica automática al superar 150 km/h y 30 m de ascenso; registra sin interrupciones ni líneas discontinuas hasta 1200 km/h.
*   **⚡ Modo Automático:** Inicia la grabación autónoma al detectar movimiento sostenido (>25 m) y pausa tras 15 minutos de reposo prolongado para preservar batería.
*   **📶 Mapas Fuera de Línea:** Navega exclusivamente con la caché local de mapas sin consumir datos móviles.
*   **📍 Avisos "Cerca de mí":** Recibe alertas sonoras/vibratorias al aproximarte a menos de 100 m de tus favoritos.
*   **🎨 Grosor del Trazado y Atenuador:** Regula el ancho de la línea de ruta y la transparencia de la ventana superior desde los controles deslizantes de ajustes.

---

## ⭐ 2. Favoritos y Lugares Destacados

Organiza tus sitios de interés agrupados por **País** y **Ciudad**, con soporte multimedia y vinculación a contactos.

### Acciones Rápidas en Favoritos:
*   **Contraer Todo (`UnfoldLess`):** En la barra superior, pliega de un solo toque todos los países y ciudades para una vista compacta.
*   **Expandir Todo (`UnfoldMore`):** Despliega simultáneamente todas las tarjetas y detalles completos.
*   **Guardar Punto Actual (`AddLocation`):** Guarda tus coordenadas GPS activas con foto, notas, categoría y color personalizado.
*   **Importar Contacto (`PersonAdd`):** Extrae nombre y dirección de tu agenda de contactos para crear un favorito instantáneo.
*   **Visor Fotográfico:** Toca la miniatura de cualquier foto para abrirla a pantalla completa con soporte de zoom interactivo.
*   **Acciones por Tarjeta:** Llama, envía correo, abre en mapa, enfoca con rumbo o edita directamente desde la tarjeta.

---

## 📜 3. Historial ("Mis Recorridos")

Tus recorridos se agrupan ordenados cronológicamente por día con resúmenes de distancia, tiempo y ritmo.

### Acciones Rápidas en Rutas:
*   **Contraer y Expandir Todo:** En la cabecera "Mis Recorridos", repliega o despliega todas las fechas de la bitácora con un solo clic.
*   **Selección Múltiple (Pulsación Larga):** Mantén presionada cualquier ruta para activar el modo de selección en bloque y exportar o eliminar varias rutas simultáneamente.
*   **Importar Recorridos (`UploadFile`):** Carga archivos externos en formato **GPX** o paquetes completos **ZIP** de BitaVi directamente a tu base de datos.
*   **Mapa de Experiencia 3D:** En la pantalla de detalle, reproduce el viaje con sincronización de altimetría, velocidad y panel flotante de fotos geolocalizadas.
*   **Exportación Multiformato:** Descarga archivos **GPX** (Google Earth/Strava), **CSV** (Excel) o copias de seguridad íntegras en formato **ZIP**.

---

## 🤖 4. Inteligencia Artificial (Groq - Llama 3.3)

BitaVi integra modelos de lenguaje avanzados para auditar tu rendimiento:
*   **Diagnóstico Táctico:** Analiza esfuerzo físico, velocidad media, picos de aceleración y perfil de desniveles de tus rutas.
*   **Seguridad de Claves:** Tu API Key de Groq se resguarda bajo cifrado de hardware mediante Android Keystore con protección biométrica.

---

## 🛡️ 5. Seguridad y Copias de Seguridad

*   **Protección Biométrica:** Exige autenticación por huella digital o reconocimiento facial para acceder a configuraciones confidenciales o borrar datos.
*   **Respaldo Completo ZIP:** Empaqueta en un único archivo comprimido todas tus rutas, waypoints, favoritos y álbum fotográfico para almacenarlo en Google Drive o tu PC.
*   **Restauración Dinámica:** Recupera toda tu información en un nuevo dispositivo seleccionando el archivo ZIP previamente generado.

---

## ⚡ 6. Catálogo de Teclas y Atajos Rápidos

Puedes consultar la guía interactiva en cualquier momento pulsando el botón **Ayuda** en la barra de navegación inferior:
*   **1 Toque:** Contraer/Expandir grupos, abrir fotos en pantalla completa, desplegar dirección en cabecera.
*   **Pulsación Larga en Mapa:** Crear un Waypoint o guardar un favorito en esa coordenada exacta.
*   **Pulsación Larga en Ruta:** Activar la selección múltiple en el historial.
*   **Botón de Opacidad:** Ajustar la transparencia de la ventana de control superior en tiempo real.

---
*BitaVi — Versión 3.2.17-RC1 (Octubre 2026)*
