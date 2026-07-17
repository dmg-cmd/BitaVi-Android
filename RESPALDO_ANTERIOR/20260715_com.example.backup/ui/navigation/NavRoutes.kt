package com.example.ui.navigation

object NavRoutes {
    const val TRACKING = "tracking"
    const val HISTORY = "history"
    const val SAVED_LOCATIONS = "saved_locations"
    const val DETAIL = "detail/{routeId}"
    const val EXPERIENCE_MAP = "experience_map/{routeId}"
    const val ROUTE_ANALYTICS = "route_analytics/{routeId}"

    const val FOCUS_FAVORITE = "focus_favorite/{favoriteId}"
    fun detail(routeId: Int) = "detail/$routeId"
    fun experienceMap(routeId: Int) = "experience_map/$routeId"
    fun routeAnalytics(routeId: Int) = "route_analytics/$routeId"
    fun focusFavorite(favoriteId: Int) = "focus_favorite/$favoriteId"

    val bottomBarRoutes = setOf(TRACKING, HISTORY, SAVED_LOCATIONS, DETAIL, EXPERIENCE_MAP)
    val baseRoutes = setOf(TRACKING, HISTORY, SAVED_LOCATIONS)
    val auxiliaryRoutes = setOf(DETAIL, EXPERIENCE_MAP, ROUTE_ANALYTICS, FOCUS_FAVORITE)
}
