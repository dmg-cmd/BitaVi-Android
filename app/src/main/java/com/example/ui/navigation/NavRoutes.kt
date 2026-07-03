package com.example.ui.navigation

object NavRoutes {
    const val TRACKING = "tracking"
    const val HISTORY = "history"
    const val SAVED_LOCATIONS = "saved_locations"
    const val DETAIL = "detail/{routeId}"
    const val EXPERIENCE_MAP = "experience_map/{routeId}"

    fun detail(routeId: Int) = "detail/$routeId"
    fun experienceMap(routeId: Int) = "experience_map/$routeId"

    val bottomBarRoutes = setOf(TRACKING, HISTORY, SAVED_LOCATIONS)
}
