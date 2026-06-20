# Funcionamiento del Proyecto - Localizador Pro

## Propósito
Aplicación profesional de seguimiento GPS con automatización inteligente, gestión de contactos geolocalizados y diario de viaje multimedia de alta seguridad.

## Módulos de Inteligencia
1. **Detección de Movimiento Proactiva**: El servicio dispara la grabación al superar los 25 metros.
2. **Geocodificación Inversa**: Identifica calle, altura, ciudad y país automáticamente. Al restaurar un respaldo, si faltan estos datos, se reconstruyen a partir de las coordenadas.
3. **Análisis por IA (Llama 3)**: Genera resúmenes motivadores del trayecto.

## Gestión de Datos y Multimedia
- **Respaldo ZIP Total**: El motor de exportación empaqueta la base de datos completa junto con los archivos físicos de imagen (.jpg). 
- **Restauración Inteligente**: El sistema detecta automáticamente la ubicación geográfica de los puntos restaurados para mantener el orden por País y Ciudad.
- **Persistencia de Fotos**: Al importar desde la galería, la app clona la imagen a su directorio privado para evitar pérdidas si el usuario borra el archivo original del carrete.

## Seguridad
- **Protección de Datos Sensibles**: La API Key y el borrado masivo están bloqueados por un PIN de 4 dígitos reactivo.
