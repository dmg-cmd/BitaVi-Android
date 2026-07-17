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

@Database(
    entities = [Route::class, LocationPoint::class, Waypoint::class, com.example.data.model.SavedLocation::class, RouteFavoriteCrossRef::class],
    version = 12,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun routeDao(): RouteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

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

        fun getDatabase(context: Context, passphrase: ByteArray? = null): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                System.loadLibrary("sqlcipher")

                val databaseBuilder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "localizador_database"
                )

                passphrase?.let {
                    val factory = SupportOpenHelperFactory(it)
                    databaseBuilder.openHelperFactory(factory)
                }

                val instance = databaseBuilder
                    .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun destroyInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
