package com.aistudio.localizador.ui.map

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.ui.graphics.vector.ImageVector
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import javax.inject.Inject
import javax.inject.Singleton

enum class MapLayerType(val id: String, val title: String, val description: String) {
    OSM_STANDARD("osm_standard", "Estándar", "OpenStreetMap clásico con calles y nombres"),
    OPENTOPO("opentopo", "Topográfico", "Curvas de nivel y relieve montañoso"),
    SATELLITE("satellite", "Satélite", "Imágenes aéreas reales de alta resolución"),
    TACTICAL_DARK("tactical_dark", "Táctico Oscuro", "Modo noche de alto contraste y ahorro OLED"),
    CYCLOSM("cyclosm", "Ciclovías", "Senderos, vías verdes y ciclovías")
}

data class MapLayerOption(
    val type: MapLayerType,
    val icon: ImageVector
)

@Singleton
class MapTileSourceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "localizador_map_prefs"
        private const val KEY_MAP_LAYER = "selected_map_layer"

        val ESRI_SATELLITE: ITileSource = XYTileSource(
            "EsriSatellite",
            0, 19, 256, ".jpg",
            arrayOf(
                "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"
            ),
            "© Esri, Maxar, Earthstar Geographics"
        )

        val CARTO_DARK: ITileSource = XYTileSource(
            "CartoDark",
            0, 20, 256, ".png",
            arrayOf(
                "https://a.basemaps.cartocdn.com/dark_all/",
                "https://b.basemaps.cartocdn.com/dark_all/",
                "https://c.basemaps.cartocdn.com/dark_all/",
                "https://d.basemaps.cartocdn.com/dark_all/"
            ),
            "© OpenStreetMap contributors © CARTO"
        )

        val CYCLOSM: ITileSource = XYTileSource(
            "CyclOSM",
            0, 18, 256, ".png",
            arrayOf(
                "https://a.tile-cyclosm.openstreetmap.fr/cyclosm/",
                "https://b.tile-cyclosm.openstreetmap.fr/cyclosm/",
                "https://c.tile-cyclosm.openstreetmap.fr/cyclosm/"
            ),
            "© OpenStreetMap contributors, CyclOSM"
        )
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentLayer = MutableStateFlow(loadSavedLayer())
    val currentLayer: StateFlow<MapLayerType> = _currentLayer.asStateFlow()

    private fun loadSavedLayer(): MapLayerType {
        val savedId = prefs.getString(KEY_MAP_LAYER, MapLayerType.OSM_STANDARD.id)
        return MapLayerType.entries.firstOrNull { it.id == savedId } ?: MapLayerType.OSM_STANDARD
    }

    fun setLayer(type: MapLayerType) {
        _currentLayer.value = type
        prefs.edit().putString(KEY_MAP_LAYER, type.id).apply()
    }

    fun getTileSource(type: MapLayerType): ITileSource {
        return when (type) {
            MapLayerType.OSM_STANDARD -> TileSourceFactory.MAPNIK
            MapLayerType.OPENTOPO -> TileSourceFactory.OpenTopo
            MapLayerType.SATELLITE -> ESRI_SATELLITE
            MapLayerType.TACTICAL_DARK -> CARTO_DARK
            MapLayerType.CYCLOSM -> CYCLOSM
        }
    }

    fun getCurrentTileSource(): ITileSource {
        return getTileSource(_currentLayer.value)
    }

    fun getAvailableLayers(): List<MapLayerOption> {
        return listOf(
            MapLayerOption(MapLayerType.OSM_STANDARD, Icons.Default.Map),
            MapLayerOption(MapLayerType.SATELLITE, Icons.Default.Satellite),
            MapLayerOption(MapLayerType.OPENTOPO, Icons.Default.Terrain),
            MapLayerOption(MapLayerType.TACTICAL_DARK, Icons.Default.DarkMode),
            MapLayerOption(MapLayerType.CYCLOSM, Icons.Default.DirectionsBike)
        )
    }
}
