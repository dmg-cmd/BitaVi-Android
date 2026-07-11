# Guía del Extractor y Traductor de Subtítulos MKV

Esta herramienta te permite extraer subtítulos de archivos MKV, traducirlos y opcionalmente incrustarlos de nuevo en el archivo de video.

## Requisitos Previos

> [!IMPORTANT]
> **Se requiere FFmpeg** para que esta herramienta funcione.
> Por favor instálalo y asegúrate de que esté en el PATH de tu sistema.
> - Windows: `winget install ffmpeg` (reinicia la terminal después de instalar)
> - Mac: `brew install ffmpeg`
> - Linux: `sudo apt install ffmpeg`

## Cómo Usar

### Opción 1: Aplicación Web (Recomendada)

1. Abre una terminal en el directorio `subtitle_extractor`.
2. Ejecuta la aplicación web:
   ```bash
   python app.py
   ```
3. Abre tu navegador en **http://127.0.0.1:5000**.
4. Sigue los pasos en la interfaz:
   - Sube tu archivo MKV.
   - Selecciona la pista de subtítulos.
   - Elige el idioma y la acción (guardar o incrustar).

### Opción 2: Línea de Comandos (CLI)

1. Ejecuta la aplicación CLI:
   ```bash
   python main.py
   ```
2. Sigue las instrucciones interactivas en la terminal.

## Instalación en Linux

Si vas a ejecutar esto en Linux, usa el script de instalación automática:

```bash
chmod +x setup.sh
./setup.sh
```

## Características verificadas
- [x] Detección de pistas de subtítulos
- [x] Extracción de subtítulos
- [x] Lógica de traducción (Google Translate)
- [x] Incrustación de subtítulos
- [x] Interfaz Web Moderna
- [x] Compatibilidad con Linux

## Solución de Problemas
- **"ffmpeg no está instalado"**: Asegúrate de haber instalado ffmpeg y reiniciado tu terminal.
- **Errores en Linux**: Si ves errores de "bad interpreter", ejecuta `sed -i 's/\r$//' setup.sh` para arreglar los finales de línea de Windows.
