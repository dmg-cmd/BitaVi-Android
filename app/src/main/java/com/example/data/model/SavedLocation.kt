package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_locations",
    indices = [Index(value = ["latitude", "longitude"], unique = true)]
)
data class SavedLocation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val address: String = "",
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val comment: String = "",
    val photoPath: String? = null,
    val author: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val isContact: Boolean = false,
    val country: String = "Desconocido",
    val city: String = "Desconocida",
    val color: String = "#FFD700",
    val iconCategory: String = "DEFAULT"
)
