# BitaVi — Bitácora Táctica de Rutas & IA (v3.2.17)

**BitaVi** es una suite profesional de seguimiento geográfico, telemetría táctica de campo y análisis de rutas mediante Inteligencia Artificial (Llama 3.3). Desarrollada con estándares de arquitectura limpia en Jetpack Compose para Android.

---

### 📖 Documentación para el Usuario
Para aprender a utilizar todas las funciones, atajos y modos tácticos, consulta la **[Guía de Usuario Detallada](Documentacion/GUIA_USUARIO.md)**.

---

## 🚀 Características Principales

### 🛰️ Seguimiento y Telemetría en Tiempo Real
*   **Ventana de Control Superior Táctica:** Presenta en vivo el Modo de Tráfico (*Caminata*, *Bicicleta*, *Vehículo*, *Vuelo*), Velocidad cinemática, Altitud GPS en metros, Distancia geodésica acumulada respecto al primer punto y Horas de funcionamiento de la sesión activa.
*   **Atenuador de Fondo Configurable:** Regulación dinámica y persistente de la transparencia de la ventana superior (de 15% a 85%) con preajustes de acceso directo.
*   **Modo Automático Inteligente:** Detección de desplazamientos sostenidos (>25 m) con inicio autónomo de grabación y pausa tras 15 minutos en reposo.
*   **Modo Vuelo Continuo:** Algoritmo aeronáutico automático para registrar trayectos comerciales hasta 1200 km/h sin saltos de señal.
*   **Mapas de Alto Rendimiento:** Integración con `osmdroid` (OpenStreetMap), fotografía satelital de alta resolución (ArcGIS) y cartografía topográfica con caché persistente offline.

### ⭐ Favoritos y Recorridos Estructurados
*   **Contraer y Expandir Todo:** Controles tácticos de un solo toque en Favoritos y Rutas para plegar y desplegar carpetas, países y fechas completas.
*   **Organización Geográfica:** Clasificación automática de favoritos por País y Ciudad con visor fotográfico en alta definición.
*   **Selección Múltiple en Historial:** Pulsación prolongada para exportar o depurar múltiples rutas por lote.
*   **Importación y Exportación:** Soporte estándar para archivos **GPX**, **CSV** (Excel) y paquetes completos **ZIP** de BitaVi.

### 🧠 Inteligencia Artificial (Llama 3.3)
*   **Análisis Táctico Groq:** Diagnóstico exhaustivo de rendimiento físico, velocidades, aceleraciones y perfil de elevación mediante IA.
*   **Geocodificación WiFi Automática:** Resolución de direcciones pendientes en segundo plano al conectarse a redes Wi-Fi.

### 🛡️ Seguridad y Cifrado
*   **Android Keystore:** Llaves criptográficas protegidas por hardware con cifrado AES-GCM.
*   **Protección Biométrica:** Verificación por huella dactilar o reconocimiento facial para acceso a configuraciones críticas y purgas de datos.
*   **Copias de Seguridad Cifradas:** Respaldos ZIP completos de bases de datos Room y fotografías para almacenamiento local o en la nube.

---

## 🛠️ Stack Tecnológico
*   **Lenguaje:** Kotlin 2.4.0 (K2 Compiler)
*   **UI:** Jetpack Compose con Material Design 3
*   **Arquitectura:** Clean Architecture + MVVM + StateFlow + Kotlin Coroutines + Hilt DI
*   **Persistencia:** Room Database con migraciones automatizadas y EncryptedSharedPreferences
*   **Red:** Retrofit 2 + OkHttp + API Groq (Llama 3.3) + Nominatim OpenStreetMap
*   **Mapas:** osmdroid 6.1.18

---

## ⚙️ Configuración y Compilación
1. Android Studio Ladybug o superior con JVM Toolchain 17.
2. Android SDK 35 (mínimo SDK 24).
3. Clave API de Groq configurada mediante variables de entorno o desde el menú de la aplicación.
