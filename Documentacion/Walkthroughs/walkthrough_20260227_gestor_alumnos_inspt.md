# Walkthrough: Gestor de Alumnos INSPT (Antigravity)

He finalizado la aplicación siguiendo todos tus requerimientos. Ahora tienes una plataforma profesional desde la cual puedes gestionar el envío de credenciales.

## 🚀 Cómo iniciar la aplicación
La aplicación ya está corriendo. Para el futuro, puedes usar los archivos de arranque rápido que he creado:

- **Windows**: Haz doble clic en `Ejecutar_App.bat`.
- **Linux/Mac**: Ejecuta `./Ejecutar_App.sh`.

*Nota: La página web se abrirá automáticamente en tu navegador predeterminado al iniciar.*

## 🛠️ Funcionalidades Implementadas

### 1. Panel de Control Inteligente
- **Filtrado Automático**: Solo verás a los alumnos que no han recibido el mail (columna `Enviado` vacía o con una "x").
- **Detección de Colores**: Si la contraseña (`CInicial`) está en rojo en el Excel, la aplicación mostrará automáticamente "cuenta entregada anteriormente" en el mail.

### 2. Gestión de Nombres y Género
- **Hoja Nombres**: La aplicación consulta la hoja `nombres` para determinar si saludar como "Estimada" o "Estimado".
- **Detección Automática**: Si un nombre no existe pero termina en "a" u "o", asigna el género automáticamente.
- **Corrección en Vivo**: Si el nombre es ambiguo o nuevo, aparecerá un botón amarillo **"Corregir Nombre"**. Al pulsar y elegir el género, se guardará permanentemente en tu archivo Excel.

### 3. Envío de Correos Seguro
- **Vista Previa**: Puedes ver exactamente cómo quedó el mail de cada alumno antes de enviarlo.
- **Mail de Prueba**: Hay un botón para enviarte un mail a ti mismo (`delmiro.gil@inspt.utn.edu.ar`) antes de enviárselo al alumno real.
- **Envío Masivo**: Un botón principal para enviar todos los correos pendientes de una sola vez.

### 4. Actualización Automática
- Al enviar un correo, la aplicación escribe la **fecha y hora actual** en la columna `Enviado` de tu archivo Excel.

## ⚠️ Nota Importante sobre Configuración
Para que los correos salgan desde tu cuenta de Outlook, debes configurar tu contraseña en el archivo `Antigravity/server.cjs` (línea 21) o contactarme para integrar un sistema de login más avanzado. Actualmente el sistema está preparado para usar la cuenta `delmiro.gil@inspt.utn.edu.ar`.

---
¡Espero que esta herramienta te sea de muchísima utilidad para agilizar el inicio del ciclo lectivo!
