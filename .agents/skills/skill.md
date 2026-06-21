**Metadata**  
Nombre: android-security-audit  
   
 Descripción: Auditoría de seguridad para app de localización Android (Kotlin/Jetpack Compose).  
   
 Alcance: Local (Solo proyecto Localizador)  
**Instrucciones para el Agente**  
- **Idioma:** Procesar todas las peticiones, razonamientos y artefactos en español.  
- **Seguridad (Crítico):**  
  - No guardar claves de API en texto plano.  
  - Verificar uso de Android Keystore y EncryptedSharedPreferences.  
  - Asegurar que el acceso a datos sensibles requiera androidx.biometric.  
- **Arquitectura:**  
  - Mantener la abstracción de API Key para permitir alternancia entre proveedores (Groq, OpenRouter, etc.).  
  - Respetar el Filtro Anti-Salto (velocidad < 180 km/h) y el Buffer de Pre-Grabación.  
- **Protocolo de Edición:**  
  - Actuar como consultor: proponer cambios mediante artefactos (Code diffs o Review Report).  
  - Nunca sobreescribir archivos sin aprobación humana mediante el flujo de Review Changes.  
  - Solo brindar un prompt especializado con las modificaciones para que android studio maneje la programación del mismo  
**Skills y Contexto del Proyecto - Localizador**  
**Tecnologías y Arquitectura Base**  
- **Plataforma:** Aplicación móvil nativa en Android utilizando   **Kotlin** y   **Jetpack Compose**.  
- **Actividad Principal:**MainActivity hereda de FragmentActivity para dar soporte nativo a los diálogos de autenticación biométrica del sistema[cite: 1].  
- **Control de Versiones:** Git con repositorio remoto privado en GitHub.  
**Gestión de IA (Motores y Configuración)**  
- **Motor Actual:** Integración unificada con la API de   **Groq (Llama 3.3)** mediante Retrofit para el análisis estadístico avanzado de rutas[cite: 1].  
- **Consistencia de Variables:** La clave de la API está centralizada bajo la variable arquitectónica groqApiKey (anteriormente geminiApiKey)[cite: 1].  
- **Flexibilidad Futura:** El código debe estar diseñado de forma abstracta para permitir al usuario final alternar o ingresar su propia API Key (sea de Groq, Cerebras, OpenRouter o Gemini API).  
**Seguridad y Cifrado Obligatorio**  
- **Almacenamiento Seguro:** Está estrictamente prohibido guardar claves de API o datos sensibles en texto plano. Se debe utilizar el   **Android Keystore** y EncryptedSharedPreferences.  
- **Protección Biométrica:** El acceso a la visualización/modificación de la API Key y las funciones de borrado masivo de datos están protegidos globalmente por androidx.biometric (Huella Digital o Rostro)[cite: 1]. Se requiere autenticación de hardware activa. Se eliminó por completo el viejo sistema de PIN[cite: 1].  
**Funcionalidades Críticas a Respetar**  
- **Modo Automático Blindado:** Servicio persistente con 3 confirmaciones consecutivas de movimiento (>25m) y filtro de velocidad real (>1.5 km/h) junto a un   **Buffer de Pre-Grabación** circular de 30 min[cite: 1].  
- **Filtro Anti-Salto (Drift):** Algoritmo de verosimilitud que ignora puntos con velocidades imposibles (>180 km/h) o saltos de señal de más de 500m[cite: 1].  
- **Geocodificación Robusta:** Sistema que prioriza nombres geográficos o náuticos (ej. ríos) ante la falta de calles, y blindaje contra errores gRPC UNAVAILABLE mostrando coordenadas GPS (Lat, Lon)[cite: 1].  
- **Gestión de Caché Segura:** Actualización de file_paths.xml para permitir compartir capturas temporales del mapa de forma segura a WhatsApp/Telegram[cite: 1].  
**Protocolo de Interacción con la IA**  
- **Modo Consultor Estricto:** La IA externa (Antigravity) tiene prohibido editar o sobreescribir archivos directamente en el disco. Su función es auditar el código existente y proponer mejoras en formato de texto o mediante prompts específicos para Android Studio.  
- **Red de Seguridad:** Antes de realizar cualquier cambio sugerido en el código, el desarrollador generará un hito de guardado (*commit*) en Git.  
 ![](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAnEAAAACCAYAAAA3pIp+AAAABmJLR0QA/wD/AP+gvaeTAAAACXBIWXMAAA7EAAAOxAGVKw4bAAAANklEQVR4nO3OMQ2AABAAsSNBACv2EMH8NpGACyywEZJWQZeZ2aszAAD+4l6rrTq+ngAA8Nr1AL++BEmYw0RQAAAAAElFTkSuQmCC)  
**Historial Reciente de Desarrollo (Contexto de Trabajo)**  
**[Fase 3: Social, Proximidad y Estabilidad Extrema]**  
- **Mapa en WhatsApp:** Implementación de captura dinámica del mapa (Bitmap) que se adjunta automáticamente al compartir el trayecto[cite: 1].  
- **Modo Automático:** Filtro de velocidad real y 3 confirmaciones para eliminar inicios falsos por ruido de GPS[cite: 1].  
- **Avisos "Cerca de mí":** Motor de notificaciones de proximidad que alerta al pasar a menos de 100m de un favorito[cite: 1].  
- **Buffer:** Sistema de memoria circular de 30 min que recupera el inicio de la ruta[cite: 1].  
**[Fase 2: Funciones Sociales y Contactos]**  
- **Importación Inteligente:** Botón de acción rápida (PersonAdd) que abre el selector de contactos del sistema para extraer Nombre, Teléfono, Email y Dirección[cite: 1].  
- **Compartir Ubicación Pro:** Envío de favoritos mediante Intent de texto plano formateado para WhatsApp y Telegram[cite: 1].  
- **Permisos:** Incorporación de READ_CONTACTS y WRITE_CONTACTS[cite: 1].  
**Guía de Ejecución de Implementaciones en Chats de Android Studio**  
- **Migración a EncryptedSharedPreferences (Prompt 1):** Se puede realizar perfectamente en un  **chat nuevo** de Android Studio, ya que es una refactorización autocontenida que utiliza APIs estándar de Android Jetpack (security-crypto) y solo requiere el contexto actual de lectura/escritura de preferencias de MainActivity y RouteViewModel.  
- **Cifrado Biométrico Real con Keystore (Prompt 2):** Es  **altamente recomendado realizarlo en el mismo chat** a continuación del Prompt 1. Al depender directamente de las variables generadas para el almacenamiento cifrado anterior, hacerlo en el mismo hilo evita inconsistencias arquitectónicas o que el asistente de IA asuma APIs de guardado diferentes. Si se inicia en un chat nuevo, se debe proveer el código actualizado de MainActivity resultante del paso anterior.  
