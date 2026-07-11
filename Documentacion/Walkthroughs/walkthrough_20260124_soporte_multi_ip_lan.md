# Walkthrough Final: Soporte Multi-IP y LAN

He actualizado la aplicación **SAT** para manejar escenarios complejos donde el servidor tiene múltiples tarjetas de red (como Ethernet y WiFi).

## 🌐 Nueva Funcionalidad: Selector de Red
Ahora, al arrancar la aplicación, el sistema detectará todas tus direcciones IP disponibles y te permitirá elegir cuál usar. 

Esto garantiza que el **Código QR** que verán tus alumnos apunte siempre a la red correcta (por ejemplo, la red WiFi compartida del aula).

### Archivos Modificados:
- **`scripts/choose_ip.js`**: El nuevo cerebro que gestiona la selección interactiva.
- **`start.bat`**: Actualizado para iniciar el selector en Windows.
- **`start.sh`**: Actualizado para iniciar el selector en Linux.

## 🚀 Cómo usarlo ahora:
1.  Ejecuta `start.bat` o `./start.sh`.
2.  Verás una lista numerada de tus direcciones IP.
3.  Escribe el **número** de la IP que quieres usar y pulsa Enter.
4.  La app arrancará y los alumnos que escaneen el QR entrarán directamente a esa red.

---

## 🎁 Resumen de Capacidades
- **Investigación Científica**: IA basada en el modelo ABC.
- **Registro Automático**: QR para alumnos.
- **Dashboard Premium**: Semáforos de riesgo académico.
- **Multi-OS**: Windows y Linux.
- **Multi-Red**: Selector de IP inteligente.

¡Todo listo para tu primera clase!
