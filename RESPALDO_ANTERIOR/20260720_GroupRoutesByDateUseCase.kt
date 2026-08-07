package com.aistudio.localizador.domain.usecase

import com.aistudio.localizador.data.model.Route
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class DayGroupUiModel(
    val date: LocalDate,
    val formattedDate: String,
    val colorInt: Int,
    val routes: List<Route>
)

class GroupRoutesByDateUseCase @Inject constructor() {

    private val dayPalette = listOf(
        0xFF1E88E5.toInt(),
        0xFF43A047.toInt(),
        0xFFFB8C00.toInt(),
        0xFF8E24AA.toInt(),
        0xFF00ACC1.toInt(),
        0xFFD81B60.toInt(),
        0xFF3949AB.toInt()
    )

    private val dateFormatter = DateTimeFormatter
        .ofPattern("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "ES"))

    operator fun invoke(routes: List<Route>): List<DayGroupUiModel> {
        if (routes.isEmpty()) return emptyList()

        return routes
            .groupBy { route ->
                Instant.ofEpochMilli(route.startTime)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
            .toSortedMap(Comparator.reverseOrder())
            .map { (date, dayRoutes) ->
                val paletteIndex = Math.abs(date.toEpochDay().hashCode()) % dayPalette.size
                DayGroupUiModel(
                    date = date,
                    formattedDate = date.format(dateFormatter).replaceFirstChar { it.uppercase() },
                    colorInt = dayPalette[paletteIndex],
                    routes = dayRoutes.sortedByDescending { it.startTime }
                )
            }
    }
}
