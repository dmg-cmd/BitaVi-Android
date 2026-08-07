package com.aistudio.localizador.domain.usecase

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class ExtractPhotoLocationUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {

    data class PhotoMetadata(
        val latitude: Double,
        val longitude: Double,
        val timestamp: Long?
    )

    suspend fun execute(uri: Uri): PhotoMetadata? = withContext(Dispatchers.IO) {
        try {
            val tempFile = File(context.cacheDir, "temp_exif_robust_buffer.jpg")

            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val exif = ExifInterface(tempFile.absolutePath)
            val latLong = exif.latLong

            val dateString = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)

            val timestamp = dateString?.let { parseExifDate(it) }

            if (tempFile.exists()) {
                tempFile.delete()
            }

            if (latLong != null && (latLong[0] != 0.0 || latLong[1] != 0.0)) {
                Log.d("Bitaru_EXIF", "Metadatos extraidos con exito: Lat=, Lon=, TS=")
                PhotoMetadata(
                    latitude = latLong[0],
                    longitude = latLong[1],
                    timestamp = timestamp
                )
            } else {
                Log.w("Bitaru_EXIF", "El archivo fisico local carece de metadatos GPS validos.")
                null
            }
        } catch (e: Exception) {
            Log.e("Bitaru_EXIF", "Error critico en el pipeline de transcodificacion", e)
            null
        }
    }

    private fun parseExifDate(dateStr: String): Long? {
        return try {
            val sdf = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getDefault()
            }
            sdf.parse(dateStr)?.time
        } catch (e: Exception) {
            null
        }
    }
}
