# Reporte de Implementación: Filtro de Ruido GPS Inteligente

Se ha refactorizado `LocationService.kt` para mejorar la calidad de los datos recolectados y evitar el efecto de "manchones" (acumulación de puntos erráticos en un mismo lugar) cuando el dispositivo está estático o recibe señales de baja calidad.

## Cambios Implementados

### 1. Constantes de Configuración
Se definieron umbrales estrictos para la validación de cada punto de ubicación:
- **MIN_DISTANCE_THRESHOLD (10.0m):** Los puntos que no superen esta distancia respecto al último punto guardado son ignorados. Esto elimina el ruido que genera el GPS al intentar triangular la posición mientras el usuario está detenido (por ejemplo, dentro de un edificio o en un semáforo).
- **MAX_ACCURACY_THRESHOLD (20.0m):** Se descartan puntos con un radio de error mayor a 20 metros. Esto previene la inclusión de ubicaciones aproximadas por redes móviles o señales GPS rebotadas (efecto cañón urbano).
- **MAX_REALISTIC_SPEED (30.0 m/s):** Se calcula la velocidad entre el último punto válido y el actual. Si supera los 108 km/h, el punto se descarta como un "salto" errático del sensor.

### 2. Lógica de Validación "Antes de Persistir"
La validación ahora ocurre justo antes de llamar a `repository.addLocationPoint`:
- El flujo verifica secuencialmente precisión, distancia y velocidad.
- Si alguna prueba falla, el punto se registra en los logs pero no se guarda en la base de datos ni se suma a la distancia total de la ruta.

### 3. Actualización Condicional de Referencia
La variable `lastRecordedLocation` solo se actualiza cuando un punto supera todas las validaciones. Esto garantiza que la siguiente comparación de distancia se haga contra el último punto **real y confiable**, evitando que una serie de puntos ruidosos desplacen la trayectoria gradualmente.

## Beneficios Esperados
1. **Rutas más limpias:** El mapa no mostrará marañas de líneas en los puntos de parada.
2. **Precisión en la Distancia:** Al no sumar micro-movimientos de ruido, la distancia total recorrida será mucho más fiel a la realidad.
3. **Eficiencia en Almacenamiento:** Se guardan menos puntos innecesarios en la base de datos Room, optimizando el rendimiento a largo plazo.
