package com.aistudio.localizador.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "route_favorite_cross_ref",
    primaryKeys = ["routeId", "favoriteId"],
    foreignKeys = [
        ForeignKey(
            entity = Route::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SavedLocation::class,
            parentColumns = ["id"],
            childColumns = ["favoriteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["routeId"]),
        Index(value = ["favoriteId"])
    ]
)
data class RouteFavoriteCrossRef(
    val routeId: Int,
    val favoriteId: Int
)
