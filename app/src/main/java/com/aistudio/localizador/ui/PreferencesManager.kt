package com.aistudio.localizador.ui

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class PreferencesManager(context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    fun isTourSeen(key: String): Boolean = prefs.getBoolean("tour_v2_$key", false)
    fun markTourSeen(key: String) { prefs.edit { putBoolean("tour_v2_$key", true) } }

    fun isBiometricEnabled(): Boolean = prefs.getBoolean("biometric_enabled", false)
    fun setBiometricEnabled(enabled: Boolean) { prefs.edit { putBoolean("biometric_enabled", enabled) } }


    fun isAutoMode(): Boolean = prefs.getBoolean("auto_mode", false)
    fun setAutoMode(enabled: Boolean) { prefs.edit { putBoolean("auto_mode", enabled) } }

    fun isNearMeEnabled(): Boolean = prefs.getBoolean("near_me_enabled", false)
    fun setNearMeEnabled(enabled: Boolean) { prefs.edit { putBoolean("near_me_enabled", enabled) } }

    fun isBleEnabled(): Boolean = prefs.getBoolean("ble_enabled", false)
    fun setBleEnabled(enabled: Boolean) { prefs.edit { putBoolean("ble_enabled", enabled) } }
    fun getMyPhoneNumber(): String = prefs.getString("my_phone", "") ?: ""
    fun setMyPhoneNumber(phone: String) { prefs.edit { putString("my_phone", phone) } }

    fun getDefaultLatitude(): Double = prefs.getFloat("default_lat", -34.6037f).toDouble()
    fun getDefaultLongitude(): Double = prefs.getFloat("default_lon", -58.3816f).toDouble()
    fun setDefaultLocation(lat: Double, lon: Double) {
        prefs.edit { putFloat("default_lat", lat.toFloat()); putFloat("default_lon", lon.toFloat()) }
    }
    fun setStrokeWidth(w: Float) { prefs.edit { putFloat("map_stroke_width", w) } }
    fun getStrokeWidth(): Float = prefs.getFloat("map_stroke_width", 8f)

    fun setHeaderAlpha(alpha: Float) { prefs.edit { putFloat("top_bar_alpha", alpha) } }
    fun getHeaderAlpha(): Float = prefs.getFloat("top_bar_alpha", 0.45f)
}
