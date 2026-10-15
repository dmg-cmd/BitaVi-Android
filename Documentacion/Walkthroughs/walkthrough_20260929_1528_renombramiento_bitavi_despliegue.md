# Informe Walkthrough: Renombramiento a BiTaVi y Despliegue en Pixel 9 Pro XL

- **Fecha y Hora:** 2026-09-29 15:28
- **Tema:** Actualización de Identidad a BiTaVi (Bitácora Táctica de Viaje), Compilación y Despliegue
- **Estado:** Desplegado e Iniciado Exitosamente en Dispositivo Real

---

## 1. Cambios de Identidad Aplicados
Se actualizaron los recursos de cadenas en `app/src/main/res/values/strings.xml` para reflejar la nueva denominación oficial solicitada:
- **`app_name`:** `BiTaVi`
- **`drawer_title_header`:** `BiTaVi`
- **`drawer_subtitle_header`:** `Bitácora Táctica de Viaje`
- **`notification_service_title`:** `BiTaVi activo`
- **`notification_service_description`:** `Monitoreando tu bitácora de viaje en segundo plano…`

---

## 2. Ajuste de Entorno de Compilación
- Se corrigió la propiedad `sdk.dir` en `local.properties` apuntando a la ruta local de Windows (`C\:\\Users\\delmi\\AppData\\Local\\Android\\Sdk`), permitiendo la compilación directa del APK.

---

## 3. Compilación y Despliegue en Dispositivo
- **Compilación Gradle:** `./gradlew assembleDebug` finalizado con éxito (`BUILD SUCCESSFUL in 2m 7s`).
- **Instalación:** Desinstalación limpia de residuo de firma anterior (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`) e instalación exitosa del APK `app-debug.apk` vía ADB en el dispositivo **Google Pixel 9 Pro XL** (`48201FDAS001L5`).
- **Lanzamiento:** Actividad principal `MainActivity` iniciada correctamente con PID activo.
