package com.aistudio.localizador.domain.usecase

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

import javax.inject.Inject

class ExtractPhotoLocationUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {

    data class LocationResult(val latitude: Double, val longitude: Double)

    data class PhotoExifResult(
        val latitude: Double?,
        val longitude: Double?,
        val dateTakenMillis: Long?
    )

    suspend fun execute(galleryUri: Uri): LocationResult? = withContext(Dispatchers.IO) {
        val uriToProcess = try {
            MediaStore.setRequireOriginal(galleryUri)
        } catch (e: SecurityException) {
            Log.w("Bitaru_EXIF", "Falta el permiso ACCESS_MEDIA_LOCATION. Android filtrar\u00e1 el GPS de forma nativa.")
            galleryUri
        } catch (e: IllegalArgumentException) {
            galleryUri
        }

        return@withContext try {
            context.contentResolver.openInputStream(uriToProcess)?.use { inputStream ->
                val exifInterface = ExifInterface(inputStream)
                val latLong = exifInterface.latLong

                if (latLong != null && latLong.size >= 2) {
                    Log.d("Bitaru_EXIF", "\u00c9XITO: Ubicaci\u00f3n extra\u00edda del URI original: [${latLong[0]}, ${latLong[1]}]")
                    LocationResult(latitude = latLong[0], longitude = latLong[1])
                } else {
                    Log.w("Bitaru_EXIF", "AVISO: Estructura EXIF le\u00edda correctamente pero no contiene datos GPS.")
                    null
                }
            }
        } catch (e: IOException) {
            Log.e("Bitaru_EXIF", "Error f\u00edsico de E/S en el almacenamiento al leer las cabeceras", e)
            null
        } catch (e: Exception) {
            Log.e("Bitaru_EXIF", "Fallo inesperado en el procesador anal\u00edtico EXIF", e)
            null
        }
    }


    suspend fun executeRobust(uri: Uri): PhotoExifResult? = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "exif_temp_" + System.currentTimeMillis() + ".jpg")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            val exif = ExifInterface(tempFile.absolutePath)
            val latLong = exif.latLong
            val latitude = if (latLong != null && latLong.size >= 2) latLong[0] else null
            val longitude = if (latLong != null && latLong.size >= 2) latLong[1] else null

            var dateTakenMillis: Long? = null
            val dateStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)
            if (dateStr != null) {
                try {
                    val sdf = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
                    dateTakenMillis = sdf.parse(dateStr)?.time
                } catch (e: Exception) {
                    Log.w("Bitaru_EXIF", "Formato de fecha EXIF inesperado: " + dateStr)
                }
            }

            if (latitude != null && longitude != null) {
                Log.d("Bitaru_EXIF", "EXIF robusto - GPS: [" + latitude + ", " + longitude + "], fecha: " + dateStr)
            } else {
                Log.w("Bitaru_EXIF", "EXIF robusto - sin datos GPS, fecha: " + dateStr)
            }

            PhotoExifResult(latitude, longitude, dateTakenMillis)
        } catch (e: Exception) {
            Log.e("Bitaru_EXIF", "Error en extraccion robusta EXIF", e)
            null
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

}