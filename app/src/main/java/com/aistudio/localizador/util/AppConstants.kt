package com.aistudio.localizador.util

import java.util.concurrent.TimeUnit

object AppConstants {

    object Location {
        const val MAX_GPS_ACCURACY_METERS = 20.0f
        const val MAX_POCKET_ACCURACY_METERS = 50f
        const val MAX_SPEED_METERS_PER_SECOND = 45.0f
        const val MAX_SPEED_THRESHOLD_MPS = MAX_SPEED_METERS_PER_SECOND
        const val TERRESTRIAL_MAX_ACCURACY_METERS = MAX_POCKET_ACCURACY_METERS
        const val FLIGHT_MAX_SPEED_MPS = 333.33f
        const val FLIGHT_TRIGGER_SPEED_MPS = 41.67f
        const val FLIGHT_HYSTERESIS_SPEED_MPS = 33.33f
        const val FLIGHT_MIN_ALTITUDE_METERS = 100.0
        const val FLIGHT_MAX_ACCURACY_METERS = 80.0f
        const val MIN_TIME_DELTA_MILLIS = 3000L
        const val MIN_DISTANCE_THRESHOLD_METERS = 5.0f
        const val MOVEMENT_TRIGGER_THRESHOLD_METERS = 25.0f
        const val PRE_RECORDING_BUFFER_DURATION_MILLIS = 60000L
        const val AUTO_START_MIN_SPEED_KMH = 2.5f
        const val AUTO_START_SUSTAINED_MS = 10000L
        const val STOP_SPEED_THRESHOLD_KMH = 2.5f
        const val AUTO_STOP_SPEED_MPS = 0.3f
        val INACTIVITY_THRESHOLD_MILLIS = TimeUnit.MINUTES.toMillis(5)
        const val GEOFENCE_DEPARTURE_RADIUS_METERS = 50f
        const val GEOFENCE_DEPARTURE_ID = "bitavi_departure_geofence"
        const val TOLERANT_GPS_ACCURACY_METERS = 45f
    }

    object Map {
        const val OFFLINE_CACHE_MAX_BYTES = 500L * 1024 * 1024
        const val MAX_TILE_DOWNLOAD_THREADS = 5
    }

    object Ble {
        const val COMPANY_ID = 0xFFFF
        const val HASH_BYTES = 4
        const val SCAN_INTERVAL_MILLIS = 60000L
        const val SCAN_WINDOW_MILLIS = 10000L
        const val RSSI_THRESHOLD_100M = -88
        const val TX_POWER_1M = -59
        const val PATH_LOSS_EXPONENT = 3.0
        const val NOTIFICATION_COOLDOWN_MILLIS = 600000L
    }

    object AudioVisual {
        const val PROXIMITY_TONE = android.media.ToneGenerator.TONE_PROP_BEEP
        const val TONE_DURATION_MS = 300
        val PROXIMITY_VIBRATE_PATTERN = longArrayOf(0, 300, 200, 300)
    }

    object Backup {
        const val FILE_EXTENSION = ".zip"
        const val MIME_TYPE = "application/zip"
        const val MULTIMEDIA_FOLDER_NAME = "multimedia"
    }

    object Preferences {
        const val ENCRYPTED_PREFS_FILE = "secure_api_credentials_v2"
        const val MASTER_KEY_ALIAS = "localizador_pro_master_key_alias"
        const val GROQ_API_KEY_KEY = "groq_api_key_v2"
        const val GEMINI_API_KEY_KEY = "gemini_api_key_v2"
        const val GROQ_API_KEY_IV_KEY = "groq_api_key_iv"
        const val DB_PASSPHRASE_KEY = "room_sqlcipher_key"
    }

    object Geofencing {
        const val MIN_LATITUDE = -90.0
        const val MAX_LATITUDE = 90.0
        const val MIN_LONGITUDE = -180.0
        const val MAX_LONGITUDE = 180.0

        fun isWithinBounds(lat: Double, lon: Double): Boolean {
            return lat in MIN_LATITUDE..MAX_LATITUDE &&
                    lon in MIN_LONGITUDE..MAX_LONGITUDE &&
                    !(lat == 0.0 && lon == 0.0)
        }
    }
}