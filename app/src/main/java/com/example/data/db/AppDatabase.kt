package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.LocationPoint
import com.example.data.model.Route
import com.example.data.model.Waypoint

/**
 * Base de datos principal de la aplicación, que contiene las tablas de rutas,
 * ubicaciones y waypoints.
 */
@Database(
    entities = [Route::class, LocationPoint::class, Waypoint::class, com.example.data.model.SavedLocation::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun routeDao(): RouteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "localizador_database"
                )
                .fallbackToDestructiveMigration() // Limpia y recrea si hay conflictos de versión
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
