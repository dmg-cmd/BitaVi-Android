## Estado de la solicitud

- **Archivo en raíz solicitado:** INFORME\_FODA\_Y\_SEGUNDO\_PLANO.md

- **Resultado:** No fue posible crear/escribir el archivo en la raíz del proyecto en este entorno debido a restricciones de permisos de escritura para comandos de creación de archivos. No se realizó ningún cambio en el código ni en la configuración del proyecto.

- **Entrega:** A continuación incluyo el informe completo (FODA + diagnóstico y guía para visibilidad/funcionamiento en segundo plano) para que puedas copiarlo y guardarlo manualmente con ese nombre en la raíz.


# Informe FODA y funcionamiento en segundo plano — BitaVi (BiTaVi-Android)

**Fecha de análisis:** 12/11/2026  
**Alcance:** Revisión estática del repositorio (código, manifiesto, configuración y documentación disponible).  
**Restricción aplicada:** No se realizaron cambios en el código ni en la configuración del proyecto.


## 1. Resumen ejecutivo

BitaVi es una aplicación Android de seguimiento GPS, telemetría táctica y análisis de rutas con arquitectura moderna (Kotlin, Jetpack Compose, Hilt, Room/SQLCipher, osmdroid) y capacidades avanzadas como modo automático, filtrado cinemático, favoritos georreferenciados y exportación/importación.

Respecto a la necesidad planteada, **el proyecto ya implementa un servicio en primer plano (Foreground Service)** para operar en segundo plano con notificación persistente, pero **la visibilidad del estado para el usuario es parcial**: existe indicación clara en la interfaz solo durante la grabación activa, mientras que el modo pasivo/vigilancia y ciertos escenarios de apagado del servicio pueden no resultar evidentes. Además, el arranque y la permanencia dependen de permisos, ajustes del sistema (batería/notificaciones) y del estado del "Modo Automático".

**Evidencia principal revisada:** app/src/main/AndroidManifest.xml, app/src/main/java/com/aistudio/localizador/location/LocationService.kt, app/src/main/java/com/aistudio/localizador/location/manager/ServiceNotificationManager.kt, app/src/main/java/com/aistudio/localizador/location/delegate/LocationNotificationDelegate.kt, app/src/main/java/com/aistudio/localizador/ui/container/MainScaffoldContainer.kt, app/src/main/java/com/aistudio/localizador/ui/LocationServiceManager.kt, app/src/main/java/com/aistudio/localizador/ui/RouteViewModel.kt, app/build.gradle.kts, README.md.


## 2. Análisis FODA

### 2.1 Fortalezas

- **Base técnica correcta para segundo plano:** servicio en primer plano declarado como `location|connectedDevice`, con `startForeground(...)`, notificación persistente y canal dedicado (`location\_tracking\_channel`).

- **Separación funcional del seguimiento:** `LocationService` delega partes en managers/delegados (GPS, notificaciones, sesión, filtros, auto-tracking, BLE, geocercas), con exposición reactiva a UI (`isTracking`, ubicación, distancia, tiempo).

- **Capacidades operativas avanzadas:** modo automático, vigilancia/pasivo, detección de actividad/transiciones, geocerca de salida, filtrado cinemático y métricas en tiempo real.

- **Arquitectura y stack moderno:** Kotlin, Jetpack Compose, Hilt, Room/SQLCipher, coroutines/flows, osmdroid y casos de uso en dominio.

- **Enfoque en privacidad y seguridad:** cifrado local (SQLCipher), preferencias cifradas, biometría y uso de mapas abiertos.

- **Documentación y trazabilidad:** README, documentación funcional/técnica, historial de cambios y respaldos en el repositorio.

### 2.2 Oportunidades

- **Observabilidad unificada del servicio:** mostrar un estado explícito y persistente (`Grabando`, `Vigilando en segundo plano`, `Detenido`, `Requiere acción`) en la app y coherente con la notificación.

- **Diagnóstico guiado para el usuario:** panel que indique permisos, notificaciones y optimización de batería, con accesos directos a ajustes del sistema.

- **Auto-recuperación controlada:** detectar al abrir la app si el servicio debería estar activo (según Modo Automático/preferencias) y ofrecer reactivación con un toque.

- **Mensajería preventiva:** advertir claramente que, si el Modo Automático está desactivado, el servicio puede apagarse al detener la grabación.

- **Mejora de robustez en equipos OEM restrictivos:** guías específicas para marcas con políticas agresivas de ahorro de energía.

### 2.3 Debilidades

- **Visibilidad incompleta en la interfaz:** en app/src/main/java/com/aistudio/localizador/ui/container/MainScaffoldContainer.kt el banner inferior "Servicio activo • Grabando ruta en búnker" se muestra solo cuando `isTrackingLive` es verdadero; no representa el estado de vigilancia pasiva.

- **Ambigüedad entre "grabando" y "vigilando":** en modo pasivo el servicio puede seguir activo con notificación (p. ej. "Vigilando salidas... Modo Automático activo" desde `startPassiveTracking()`), pero la app no lo diferencia visualmente.

- **Apagado por diseño sin señal interna explícita:** si el Modo Automático está desactivado, al detener el seguimiento o al cambiar el modo el servicio puede ejecutar apagado total (`shutdownService()`), lo que puede contradecir la expectativa de permanencia continua sin una advertencia clara.

- **Notificación potencialmente discreta:** el canal de seguimiento se crea con `IMPORTANCE\_LOW`, lo que puede reducir su visibilidad según configuración del dispositivo.

- **Complejidad de componentes críticos:** `LocationService`, `RouteViewModel` y pantallas/contenedores principales concentran mucha lógica, dificultando auditar estados operativos y casos borde.

- **Señales de higiene del repositorio:** existen archivos temporales (`tmp/`), múltiples respaldos históricos y documentación distribuida, lo que puede dificultar ubicar la "fuente de verdad" operativa.

### 2.4 Amenazas

- **Restricciones del sistema y fabricantes (OEM):** optimización de batería, Doze y políticas de cierre en segundo plano pueden detener o limitar el servicio.

- **Permisos y políticas de Android:** ubicación en segundo plano y notificaciones (Android 13+) pueden impedir el funcionamiento esperado si se revocan o no se conceden correctamente.

- **Condiciones de señal y hardware:** pérdida de GPS, interiores, baja precisión o consumo energético sostenido afectan la continuidad percibida.

- **Dependencias externas:** disponibilidad/calidad de servicios complementarios (p. ej. geocodificación/IA) no impactan el tracking base, pero sí funciones asociadas.

- **Riesgo de regresiones:** cambios futuros en flujo de arranque/parada o en UI pueden alterar la visibilidad del estado sin detectarse de inmediato.


## 3. Problemas y hallazgos relevantes (revisión estática)

| Hallazgo | Evidencia observada | Impacto | Prioridad |
| :-: | :-: | :-: | :-: |
| Indicador en app limitado a grabación activa | `MainScaffoldContainer` renderiza el banner de servicio solo si `viewModel.isTrackingLive` es `true` | No se distingue vigilancia pasiva de servicio detenido dentro de la app | Alta |
| Estado pasivo visible solo por notificación del sistema | `LocationService.startPassiveTracking()` publica notificación de vigilancia/servicio activo; no hay banner UI equivalente | El usuario puede interpretar que no está funcionando en segundo plano | Alta |
| Apagado intencional cuando no hay Modo Automático | `stopTracking()` y `handleUpdateAutoMode(false)` pueden llamar `shutdownService()` si no hay tracking activo | No permanece en segundo plano por diseño; requiere configuración/aviso explícito | Alta |
| Falta de diagnóstico visible de condiciones externas | No se observa indicador unificado de permisos, notificaciones o restricciones de batería en el flujo principal | Detenciones o bloqueos externos sin explicación clara para el usuario | Alta |
| Notificación de seguimiento potencialmente poco notoria | Canal `location\_tracking\_channel` creado con `IMPORTANCE\_LOW` | La señal principal de foreground puede pasar desapercibida | Media |
| Complejidad del flujo de control del servicio | Múltiples acciones (`START`, `STOP`, `START\_PASSIVE`, `UPDATE\_AUTO\_MODE`, etc.) y orígenes (UI, receptores, transiciones) | Mayor dificultad para diagnosticar estados y garantizar visibilidad consistente | Media |

> Nota: No se ejecutaron pruebas en dispositivo/emulador ni compilación durante esta revisión; los hallazgos se basan en análisis estático del código y recursos disponibles.


## 4. Estado actual del funcionamiento en segundo plano (según código)

### 4.1 Cómo funciona hoy

- **Tipo de servicio y permisos:** `LocationService` es un servicio en primer plano con tipo `location|connectedDevice`; el manifiesto declara permisos de ubicación fina/gruesa, ubicación en segundo plano, servicio en primer plano, notificaciones, wake lock y otros relacionados.

- **Inicio y permanencia:** en `onStartCommand` asegura foreground (salvo apagado explícito) y responde a acciones de inicio, pasivo, pausa, detención, actualización y eventos automáticos; retorna `START\_STICKY` en flujo normal.

- **Notificación persistente:** `ServiceNotificationManager`/`LocationNotificationDelegate` crean el canal y publican una notificación continua con acceso a la app y acción "Detener y Guardar".

- **Modos operativos:**

  - **Grabación activa (`isTracking = true`):** `startTracking()` inicia foreground, solicita actualizaciones GPS, temporizador/watchdog y actualiza notificación con distancia y tiempo.

  - **Pasivo/vigilancia:** `startPassiveTracking()` mantiene foreground con texto de servicio/vigilancia (p. ej. "Vigilando salidas... Modo Automático activo") y usa actualizaciones más espaciadas; puede gestionar vigilancia de salida.

  - **Apagado:** `shutdownService()` detiene actualizaciones y recursos, quita foreground y finaliza el servicio.

- **Puente con la UI:** `LocationServiceManager` envía intents de control; `RouteViewModel` expone `LocationService.isTracking`, ubicación, tiempo, distancia y modo de actividad como `StateFlow`.

### 4.2 Qué indicación tiene el usuario actualmente

- **En la app:** barra/banner en la parte inferior con "Servicio activo • Grabando ruta en búnker" **únicamente durante grabación activa**.

- **En el sistema Android:** notificación persistente del servicio en primer plano cuando está activo (contenido varía según estado: grabación, vigilancia o servicio activo).

- **En el mapa:** métricas de actividad/ubicación/GPS que ayudan a inferir funcionamiento, pero no constituyen un indicador explícito del ciclo de vida del servicio en todos los modos.

### 4.3 Por qué puede parecer que "no está funcionando" en segundo plano

- **Vigilancia pasiva sin banner interno:** el servicio puede estar activo y notificando, pero la app no muestra un estado dedicado de "vigilando".

- **Modo Automático desactivado:** al no grabar, el servicio puede apagarse por diseño; si la expectativa es permanencia constante, falta una advertencia/indicación clara de ese estado.

- **Notificación silenciada u oculta:** al ser un canal de importancia baja y depender de ajustes del usuario, la señal puede perderse.

- **Restricciones externas:** permisos insuficientes o ahorro de batería del sistema/fabricante pueden impedir continuidad sin que la app lo diagnostique visiblemente.


## 5. Cómo dejar visible y asegurar que funcione en segundo plano (sin cambiar código)

### 5.1 Interpretación correcta del estado según el comportamiento actual

- **Grabando ruta:** debe verse el banner verde en la app y la notificación con distancia/tiempo.

- **Vigilando en segundo plano (sin grabación):** debe existir la **notificación persistente** del servicio (texto de vigilancia/servicio activo). En el estado actual de la app, **puede no aparecer un banner específico** para este modo.

- **Detenido:** no hay notificación persistente del servicio ni banner de grabación; esto es esperable si el Modo Automático está desactivado y no hay ruta en curso.

### 5.2 Pasos recomendados en el teléfono

1. **Confirmar la notificación del servicio (señal principal)**

   - Activar notificaciones de BiTaVi.

   - Verificar que el canal del servicio de seguimiento no esté silenciado, minimizado u oculto.

   - Comprobar que la notificación persistente permanezca al salir de la app o bloquear pantalla (según el modo esperado).

2. **Configurar el modo correcto para permanencia**

   - Activar **"Modo Automático"** si se desea que la app permanezca en vigilancia cuando no hay ruta grabándose.

   - Tener en cuenta: con Modo Automático desactivado, al detener la grabación el servicio puede apagarse por diseño (ahorro de batería).

3. **Revisar permisos críticos**

   - **Ubicación:** permitir acceso preciso cuando sea posible.

   - **Ubicación en segundo plano:** seleccionar "Permitir todo el tiempo" (o equivalente según Android/fabricante).

   - **Notificaciones (Android 13+):** permitir para ver el estado del servicio.

   - Revisar permisos auxiliares que use la app (actividad física, Bluetooth, etc.) si aplican a tu flujo.

4. **Evitar cierres por ahorro de energía**

   - Excluir BiTaVi de la optimización de batería o permitir ejecución en segundo plano sin restricciones.

   - En equipos con gestión agresiva (Xiaomi/MIUI-HyperOS, Samsung/One UI, Huawei, OnePlus, etc.), habilitar auto-inicio, permitir actividad en segundo plano y evitar bloqueos desde la multitarea si el sistema lo ofrece.

5. **Rutina rápida de verificación y reactivación**

   - Abrir BiTaVi y comprobar si hay **banner de grabación** o, al menos, **notificación persistente**.

   - Si esperas vigilancia y no hay notificación: activar Modo Automático, volver a la pantalla principal para que el servicio pasivo se reactive (según flujo de la app) y revisar permisos/batería.

   - Si el servicio desaparece al bloquear pantalla o tras un tiempo: revisar permisos de ubicación en segundo plano, notificaciones y restricciones de batería del sistema/fabricante.

### 5.3 Recomendaciones de implementación futura (para hacerlo inequívoco en la app)

> Sugerencias para una próxima iteración (no implementadas en este análisis):

- **Indicador de estado del servicio siempre visible:** chip/barra con `Grabando`, `Vigilando salidas`, `Servicio detenido` y `Requiere acción`.

- **Diagnóstico integrado:** estado de permisos (ubicación, segundo plano, notificaciones), optimización de batería y último evento/señal del servicio.

- **Acción de reanudación:** botón "Iniciar/Reanudar vigilancia" cuando corresponda según configuración.

- **Aviso preventivo de configuración:** al desactivar Modo Automático, informar que la app no permanecerá en vigilancia al finalizar la grabación.

- **Coherencia UI-notificación:** unificar textos y estados; evaluar canales/estilos de notificación para distinguir grabación vs vigilancia sin perder claridad.

- **Registro local de salud del servicio (opcional):** motivo de inicio/detención para facilitar soporte y detección de cierres del sistema.

### 5.4 Criterios de aceptación sugeridos (para validar una mejora)

- El usuario identifica en pocos segundos si la app está **grabando**, **vigilando** o **detenida**.

- La app advierte claramente cuando no puede permanecer en segundo plano por configuración o restricciones del sistema.

- Existe una acción simple para reactivar el servicio cuando corresponda.

- La notificación del sistema y el indicador en la app reflejan el mismo estado operativo.


## 6. Conclusión

BitaVi dispone de una base técnica adecuada para operar en segundo plano mediante un servicio en primer plano con notificación persistente y lógica de grabación/vigilancia. La principal brecha frente a la necesidad planteada es la **falta de una indicación unificada y explícita del estado del servicio en todos los modos** (especialmente vigilancia pasiva) y la ausencia de **señales/acciones claras** cuando el servicio se detiene por diseño o por restricciones del sistema.

Para el uso actual, la referencia más confiable es la **notificación persistente del sistema** junto con la configuración de **Modo Automático**, permisos y ajustes de batería. Para resolver la necesidad de forma completa, se recomienda incorporar en la app un indicador de estado siempre visible y un diagnóstico guiado con opción de reanudación.

