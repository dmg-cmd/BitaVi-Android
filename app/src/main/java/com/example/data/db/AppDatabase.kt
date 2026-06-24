package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import com.example.data.model.LocationPoint
import com.example.data.model.Route
import com.example.data.model.Waypoint

/**
 * Base de datos principal de la aplicación, que contiene las tablas de rutas,
 * ubicaciones y waypoints.
 */
@Database(
    entities = [Route::class, LocationPoint::class, Waypoint::class, com.example.data.model.SavedLocation::class],
    version = 8,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun routeDao(): RouteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Objeto de migración vacío para proteger los datos al subir de versión (7 a 8).
         * Hallazgo de auditoría A02.
         */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Sin cambios estructurales, solo protege datos existentes
            }
        }

        fun getDatabase(context: Context, passphrase: ByteArray? = null): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Cargar librería nativa para SQLCipher (Nuevo en sqlcipher-android)
                System.loadLibrary("sqlcipher")

                val databaseBuilder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "localizador_database"
                )
                
                // Cifrado de base de datos con SQLCipher (Hallazgo A01)
                passphrase?.let {
                    val factory = SupportOpenHelperFactory(it)
                    databaseBuilder.openHelperFactory(factory)
                }

                val instance = databaseBuilder
                    .addMigrations(MIGRATION_7_8)
                    .fallbackToDestructiveMigration() // Agregado para mayor robustez
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Fuerza el cierre de la conexión y limpia la instancia para permitir reinicialización
         */
        fun destroyInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
