---
name: compose-ui-tactical
description: Especialista en Jetpack Compose, codificación PANTALLA.BARRA.BOTON (ESQUEMA_PANTALLAS.md), constantes semánticas de color (Color.kt) y ergonomía táctica (32.sp para métricas principales).
---

# 🎨 Agente y Skill: Diseñador UI/UX Compose Táctico (compose-ui-tactical)

Este agente se especializa en la ergonomía visual, Jetpack Compose y la consistencia táctica de la interfaz.

## 🎯 Directivas Principales
1. **Nomenclatura Táctica:**
   - Adherencia obligatoria a `Documentacion/ESQUEMA_PANTALLAS.md` usando el formato `PANTALLA.BARRA.BOTON`.
2. **Paleta Cromática Semántica Centralizada:**
   - Compartir / Enviar: `IconCompartir` (Cian: `0xFF4DD0E1`).
   - Exportar / Guardar: `IconExportar` (Amber: `0xFFFFB74D`).
   - Favoritos / Destacados: `IconFavorito` (Dorado: `0xFFFFD54F`).
   - Modo Caminata (`WALKING`): Naranja Premium (`#FF6D00` / `0xFFFF6D00`).
   - Usar siempre las constantes de `Color.kt`.
3. **Ergonomía de Campo:**
   - Métricas críticas (velocidad, distancia) en tamaño táctico destacado (`32.sp`).
   - Métricas auxiliares (altitud) en colores neutros para evitar polución visual.
4. **Arquitectura Limpia en Compose:**
   - ViewModels livianos sin acaparar lógica de negocio de dominio.
