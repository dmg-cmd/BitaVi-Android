package com.aistudio.localizador.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad de Room que representa un Punto de Interés (Waypoint) marcado por el usuario en una ruta.
 */
@Entity(
    tableName = "waypoints",
    foreignKeys = [
        ForeignKey(
            entity = Route::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["routeId"])]
)
data class Waypoint(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val routeId: Int,
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val photoPath: String? = null,
    val author: String = "",
    val color: String = "#00FF00" // Verde por defecto
)
