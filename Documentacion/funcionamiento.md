# Funcionamiento del Proyecto - Localizador Pro

## Propósito
Aplicación profesional de seguimiento GPS con automatización inteligente, gestión de contactos geolocalizados y diario de viaje multimedia de alta seguridad.

## Módulos de Inteligencia
1. **Detección de Movimiento Proactiva**: El servicio dispara la grabación al superar los 25 metros con 3 confirmaciones consecutivas.
2. **Geocodificación Inversa**: Identifica calle, altura, ciudad y país automáticamente. Al restaurar un respaldo, si faltan estos datos, se reconstruyen a partir de las coordenadas.
3. **Análisis por IA (Llama 3)**: Genera resúmenes técnicos del trayecto vía Groq API.
4. **Filtro GPS Multicapa**: Validación de puntos por precisión (>20m descartados), velocidad (>45 m/s descartados) y delta temporal (<3s combinado con distancia). Función de validación pura sin efectos secundarios.

## Gestión de Datos y Multimedia
- **Respaldo ZIP Total**: El motor de exportación empaqueta la base de datos completa junto con los archivos físicos de imagen (.jpg).
- **Restauración Inteligente**: El sistema detecta automáticamente el formato (ZIP o GPX) y reconstruye datos geográficos faltantes.
- **Persistencia de Fotos**: Al importar desde la galería, la app clona la imagen a su directorio privado para evitar pérdidas.
- **Exportaciones**: GPX (intercambio), CSV (Excel), ZIP (respaldo completo). Accesibles desde el menú `⋮` en detalle de ruta.

## Seguridad
- **Protección Biométrica**: La API Key y el borrado masivo están protegidos por huella dactilar o reconocimiento facial mediante Android BiometricPrompt.
- **Cifrado de Base de Datos**: SQLCipher con passphrase almacenada en EncryptedSharedPreferences (AES256-GCM).
- **Cifrado de API Key**: Clave de Groq cifrada con AES-GCM y autenticación biométrica mediante Android Keystore.

## Interfaz de Usuario
- **Mapa principal**: Seguimiento GPS en vivo con polyline, waypoints, marcadores de favoritos y panel de telemetría.
- **Menú de acciones `⋮`**: Acciones de compartir, exportar (con submenú), eliminar y ayuda agrupadas en la pantalla de detalle de ruta.
- **Diálogo de ayuda**: Explicación de cada acción disponible con iconos Material Design.
