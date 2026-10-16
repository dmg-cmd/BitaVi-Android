package com.aistudio.localizador.domain.usecase

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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

            val sourceUri = resolveOriginalUri(uri)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val exif = ExifInterface(tempFile.absolutePath)
            var latLong = exif.latLong?.takeIf { it[0] != 0.0 || it[1] != 0.0 }

            if (latLong == null) {
                latLong = readLatLongFromStream(sourceUri)
            }

            val dateString = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)

            val timestamp = dateString?.let { parseExifDate(it) }

            if (tempFile.exists()) {
                tempFile.delete()
            }

            if (latLong != null) {
                Log.d("BitaVi_EXIF", "Metadatos extraidos con exito: Lat=, Lon=, TS=")
                PhotoMetadata(
                    latitude = latLong[0],
                    longitude = latLong[1],
                    timestamp = timestamp
                )
            } else {
                Log.w("BitaVi_EXIF", "El archivo fisico local carece de metadatos GPS validos.")
                null
            }
        } catch (e: Exception) {
            Log.e("BitaVi_EXIF", "Error critico en el pipeline de transcodificacion", e)
            null
        }
    }

    private fun resolveOriginalUri(uri: Uri): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                MediaStore.setRequireOriginal(uri)
            } catch (e: Exception) {
                Log.w("BitaVi_EXIF", "setRequireOriginal no disponible, usando URI original", e)
                uri
            }
        } else {
            uri
        }
    }

    private fun readLatLongFromStream(uri: Uri): DoubleArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).latLong?.takeIf { it[0] != 0.0 || it[1] != 0.0 }
            }
        } catch (e: Exception) {
            Log.e("BitaVi_EXIF", "Fallo lectura EXIF directa desde stream", e)
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