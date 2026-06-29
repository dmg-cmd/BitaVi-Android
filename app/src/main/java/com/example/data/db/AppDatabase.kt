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
import com.example.data.model.RouteFavoriteCrossRef
import com.example.data.model.Waypoint

/**
 * Base de datos principal de la aplicación, que contiene las tablas de rutas,
 * ubicaciones y waypoints.
 */
@Database(
    entities = [Route::class, LocationPoint::class, Waypoint::class, com.example.data.model.SavedLocation::class, RouteFavoriteCrossRef::class],
    version = 11,
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

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `route_favorite_cross_ref` (
                        `routeId` INTEGER NOT NULL,
                        `favoriteId` INTEGER NOT NULL,
                        PRIMARY KEY(`routeId`, `favoriteId`),
                        FOREIGN KEY(`routeId`) REFERENCES `routes`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`favoriteId`) REFERENCES `saved_locations`(`id`) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_route_favorite_cross_ref_routeId` ON `route_favorite_cross_ref`(`routeId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_route_favorite_cross_ref_favoriteId` ON `route_favorite_cross_ref`(`favoriteId`)")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Sin cambios estructurales, solo para permitir el salto de versión
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routes ADD COLUMN activityMode TEXT NOT NULL DEFAULT 'WALKING'")
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
                    .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                    .fallbackToDestructiveMigration()
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
