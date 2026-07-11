# Walkthrough - AI Kahoot Local

He completado el desarrollo de tu aplicación de Kahoot local con inteligencia artificial. Aquí tienes un resumen de lo que puedes hacer con ella.

## Características Implementadas

- **Generación de Preguntas con IA**: Utiliza Google Gemini para crear cuestionarios personalizados basados en un tema, público objetivo y estilo de presentación.
- **Red Local**: La aplicación detecta automáticamente la IP de tu máquina y genera un código QR para que los alumnos se conecten desde sus móviles dentro de la misma Wi-Fi.
- **Tiempo Real**: Sincronización instantánea mediante Socket.io para una experiencia fluida.
- **Diseño Premium**: Interfaz moderna con animaciones, modo oscuro y estética inspirada en Kahoot.

## Demostración de Flujo

### 1. Configuración del Profesor
El profesor ingresa el tema (ej: "Sistemas Operativos"), el público ("Estudiantes técnicos") y el estilo ("Dinámico").
La IA devuelve 5 preguntas estructuradas instantáneamente.

### 2. Sala de Espera (Lobby)
Se muestra el PIN del juego y el código QR. Los alumnos escanean el código y aparecen en pantalla gigante.

### 3. Juego
El profesor controla el avance de las preguntas. Los alumnos ven los colores de las opciones en sus móviles y responden. Al finalizar cada pregunta se muestra el podio.

## Cómo empezar

> [!IMPORTANT]
> Debes configurar tu `GEMINI_API_KEY` en el archivo `server/.env`.

1. **Servidor**: Inicia `node index.js` en la carpeta `server`.
2. **Cliente**: Inicia `npm run dev -- --host` en la carpeta `client`.

Consulta el archivo [README.md](file:///d:/Antigravity/PlayGround/TallerTecnologiaEducativa/kahoot-local-ai/README.md) para instrucciones detalladas paso a paso.
