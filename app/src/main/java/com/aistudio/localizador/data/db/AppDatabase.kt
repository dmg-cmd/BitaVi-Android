package com.aistudio.localizador.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import com.aistudio.localizador.data.model.LocationPoint
import com.aistudio.localizador.data.model.Route
import com.aistudio.localizador.data.model.RouteFavoriteCrossRef
import com.aistudio.localizador.data.model.Waypoint
import com.aistudio.localizador.data.model.SavedLocation

@Database(
    entities = [Route::class, LocationPoint::class, Waypoint::class, SavedLocation::class, RouteFavoriteCrossRef::class],
    version = 13,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun routeDao(): RouteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private const val DATABASE_NAME = "localizador_database"

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
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
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routes ADD COLUMN activityMode TEXT NOT NULL DEFAULT 'WALKING'")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE saved_locations ADD COLUMN iconCategory TEXT NOT NULL DEFAULT 'DEFAULT'")
                android.util.Log.i("ROOM_MIGRATION", "Columna iconCategory inyectada en version 12.")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routes ADD COLUMN color TEXT NOT NULL DEFAULT '#3498DB'")
                android.util.Log.i("ROOM_MIGRATION", "Columna color inyectada en routes version 13.")
            }
        }

        fun getInstance(context: Context, passphrase: ByteArray? = null): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = buildDatabase(context, passphrase)
                    INSTANCE = instance
                }
                instance
            }
        }

        private fun buildDatabase(context: Context, passphrase: ByteArray?): AppDatabase {
            try {
                val factory = if (passphrase != null) {
                    System.loadLibrary("sqlcipher")
                    SupportOpenHelperFactory(passphrase)
                } else {
                    null
                }

                val databaseBuilder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )

                if (factory != null) {
                    databaseBuilder.openHelperFactory(factory)
                }

                return databaseBuilder
                    .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                    .build()
            } catch (ex: Exception) {
                handleDatabaseCorruption(context, ex)
                throw ex
            }
        }

        private fun handleDatabaseCorruption(context: Context, exception: Exception) {
            android.util.Log.e("AppDatabase", "Fallo critico al abrir BD cifrada: ${exception.message}", exception)
        }

        fun destroyInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
