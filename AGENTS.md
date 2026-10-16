# Instrucciones del Modelo

## Idioma (OBLIGATORIO — PRIORIDAD MAXIMA)
- El modelo DEBE comunicarse **exclusivamente en Espanol** en TODAS sus respuestas visibles al usuario.
- Esto aplica a TODO texto visible: respuestas, resumenes de progreso, tablas de estado, titulos, notas, advertencias, listas de pasos, mensajes de error y confirmaciones.
- El pensamiento interno (thought) DEBE ser **estrictamente en Espanol**.
- NO usar encabezados ni etiquetas en ingles para texto conversacional. Ejemplos correctos: "Estado: Hecho", "Pendiente", "Compilacion exitosa", "En progreso". Incorrecto: "Status: Done", "Pending", "Build successful", "WIP".
- Se permite mantener en ingles unicamente terminos tecnicos universales sin traduccion estandar (nombres de archivos, clases, funciones, comandos, APIs, dependencias), pero el texto que los rodea DEBE estar en espanol.
- Nunca cambiar a ingles a menos que el usuario lo pida explicitamente.
- Usar el dialecto neutro de espanol latinoamericano, evitando modismos muy regionales.
- SI EL USUARIO NO ENTIENDE UN MENSAJE O DEBE TRADUCIRLO: significa que se incumplio esta regla. Se debe reescribir la respuesta en espanol.