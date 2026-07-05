---
name: android-security-audit
description: Auditoría de seguridad para app de localización Android (Kotlin/Jetpack Compose). Úsalo cuando se trabaje con seguridad, API keys, cifrado, biometría, o arquitectura del proyecto Localizador.
---

# Android Security Audit - Localizador

## Instrucciones Generales
- **Idioma:** Procesar todas las peticiones, razonamientos y artefactos en español.
- **Seguridad (Crítico):**
  - No guardar claves de API en texto plano.
  - Verificar uso de Android Keystore y EncryptedSharedPreferences.
  - Asegurar que el acceso a datos sensibles requiera androidx.biometric.
- **Arquitectura:**
  - Mantener la abstracción de API Key para permitir alternancia entre proveedores (Groq, OpenRouter, etc.).
  - Respetar el Filtro Anti-Salto (velocidad < 180 km/h) y el Buffer de Pre-Grabación.
- **Protocolo de Edición:**
  - Actuar como consultor y editor: proponer cambios mediante artefactos (Code diffs o Review Report).

## Tecnologías y Arquitectura Base
- **Plataforma:** Aplicación móvil nativa en Android con Kotlin y Jetpack Compose.
- **Actividad Principal:** MainActivity hereda de FragmentActivity para soporte nativo a diálogos de autenticación biométrica.
- **Control de Versiones:** Git con repositorio remoto privado en GitHub.

## Gestión de IA (Motores y Configuración)
- **Motor Actual:** Integración unificada con la API de Groq (Llama 3.3) mediante Retrofit.
- **Consistencia de Variables:** La clave de la API está centralizada bajo la variable arquitectónica groqApiKey.
- **Flexibilidad Futura:** Código diseñado de forma abstracta para permitir alternar o ingresar API Key propia (Groq, Cerebras, OpenRouter o Gemini API).

## Seguridad y Cifrado Obligatorio
- **Almacenamiento Seguro:** Prohibido guardar claves de API o datos sensibles en texto plano. Usar Android Keystore y EncryptedSharedPreferences.
- **Protección Biométrica:** Acceso a visualización/modificación de API Key y borrado masivo protegido por androidx.biometric (Huella Digital o Rostro). Se eliminó el viejo sistema de PIN.

## Funcionalidades Críticas a Respetar
- **Modo Automático Blindado:** Servicio persistente con 3 confirmaciones consecutivas de movimiento (>25m) y filtro de velocidad real (>1.5 km/h) con Buffer de Pre-Grabación circular de 30 min.
- **Filtro Anti-Salto (Drift):** Algoritmo que ignora puntos con velocidades imposibles (>180 km/h) o saltos de señal >500m.
- **Geocodificación Robusta:** Prioriza nombres geográficos o náuticos ante falta de calles, y blindaje contra errores gRPC UNAVAILABLE mostrando coordenadas GPS.
- **Gestión de Caché Segura:** Actualización de file_paths.xml para compartir capturas temporales del mapa de forma segura.