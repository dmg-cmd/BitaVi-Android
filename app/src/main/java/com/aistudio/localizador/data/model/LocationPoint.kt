package com.aistudio.localizador.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "location_points",
    foreignKeys = [
        ForeignKey(
            entity = Route::class,
            parentColumns = ["id"],
            childColumns = ["route_id"], // Mapeo físico explícito
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["route_id"])]
)
data class LocationPoint(
    @PrimaryKey(autoGenerate = true) 
    @ColumnInfo(name = "id") val id: Long = 0,
    
    @ColumnInfo(name = "route_id") val routeId: Int,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "altitude") val altitude: Double,
    @ColumnInfo(name = "speed") val speed: Float = 0f,
    @ColumnInfo(name = "timestamp") val timestamp: Long
)
