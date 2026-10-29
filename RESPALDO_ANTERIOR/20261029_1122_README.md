# Localizador Pro - Tracker GPS Inteligente & IA  

Localizador Pro es una aplicación avanzada de seguimiento geográfico para Android. Combina precisión técnica, automatización basada en movimiento y análisis de telemetría mediante Inteligencia Artificial (Llama 3.3).

---
### 📖 Documentación para el Usuario
Si eres un usuario de la aplicación, consulta nuestra **[Guía de Usuario Detallada](GUIA_USUARIO.md)** para aprender a usar todas las funciones.
---

## 🚀 Características Principales  
### 🛰️ Seguimiento y Navegación  
•Modo Automático Inteligente: Vigilancia en segundo plano que inicia la grabación al detectar desplazamientos reales (>25m) y pausa tras 15 min de inactividad, filtrando el ruido del GPS.  
•Mapas de Alto Rendimiento: Integración con osmdroid y OpenStreetMap con sistema de caché persistente de 500MB para uso eficiente de datos.  
•Modo Avión: Algoritmo optimizado para registrar trayectos en vuelos comerciales de hasta 1200 km/h.  
### 🧠 Inteligencia Artificial (IA)  
•Análisis Técnico con Groq: Generación de informes de telemetría profesional (velocidades medias, picos, regularidad de ritmo) utilizando modelos Llama 3.3 a través de la API de Groq.  
•Diario Multimedia: Vinculación de fotografías con coordenadas exactas mediante extracción de metadatos EXIF.  
### 🛡️ Seguridad de Grado Bancario  
•Hardware-Backed Encryption: Las API Keys se almacenan en el Android Keystore utilizando cifrado simétrico AES-GCM-NoPadding.  
•Autenticación Biométrica: Acceso protegido a configuraciones críticas y descifrado de llaves mediante Huella Digital o Reconocimiento Facial (CryptoObject).  
•Almacenamiento Privado: Uso de EncryptedSharedPreferences y bases de datos Room con migraciones seguras.  
### 📂 Gestión de Datos y Social  
•Copia de Seguridad Pro: Sistema de respaldo en archivos ZIP que incluye fotos locales y base de datos, con motor de restauración dinámico compatible con versiones anteriores.  
•Integración con Contactos: Exportación de favoritos directamente a la agenda de Android y acciones rápidas para llamadas o correos desde la app.  
•Avisos "Cerca de mí": Notificaciones de proximidad al pasar a menos de 100m de puntos marcados.  
### 🛠️ Stack Tecnológico  
•Lenguaje: Kotlin 2.4.0 (K2 Compiler)  
•UI: Jetpack Compose (BOM 2026.06.00) con Material Design 3  
•Arquitectura: MVVM con StateFlow y Coroutines  
•Base de Datos: Room Persistence Library (V7)  
•Red: Retrofit 2 + OkHttp (Integración Groq API)  
•Mapas: osmdroid (OpenStreetMap)  
•Seguridad: Jetpack Security (Crypto) & Biometric Library  
•Gráficos: MPAndroidChart (Perfiles de altitud y velocidad)  
## ⚙️ Configuración del Entorno  
Para ejecutar este proyecto localmente, asegúrate de tener:  
1.Android Studio (Versión Ladybug o superior).  
2.SDK 36 instalado.  
3.Un archivo .env en la raíz con la siguiente clave:  
TOML  
GROQ_API_KEY=tu_clave_aquí  
