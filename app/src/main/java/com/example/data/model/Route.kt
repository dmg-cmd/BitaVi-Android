package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de Room que representa una ruta realizada por el usuario.
 */
@Entity(tableName = "routes")
data class Route(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = 0,
    val distanceMeters: Double = 0.0,
    val isActive: Boolean = false,
    val maxSpeed: Float = 0f,
    val avgSpeed: Float = 0f
)
