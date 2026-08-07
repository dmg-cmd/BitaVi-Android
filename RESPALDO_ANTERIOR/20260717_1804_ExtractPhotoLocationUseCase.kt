package com.aistudio.localizador.domain.usecase

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExtractPhotoLocationUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    data class LocationResult(val latitude: Double, val longitude: Double)

    suspend fun execute(photoUri: Uri): LocationResult? = withContext(Dispatchers.IO) {
        val safeUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                MediaStore.setRequireOriginal(photoUri)
            } catch (e: SecurityException) {
                Log.w("Bitaru_EXIF", "Falta el permiso ACCESS_MEDIA_LOCATION. Android filtr\u00f3 el GPS.")
                photoUri
            } catch (e: IllegalArgumentException) {
                photoUri
            }
        } else {
            photoUri
        }

        try {
            context.contentResolver.openInputStream(safeUri)?.use { inputStream ->
                val exifInterface = ExifInterface(inputStream)
                val latLong = exifInterface.latLong

                if (latLong != null && latLong.size >= 2) {
                    Log.d("Bitaru_EXIF", "Coordenadas extra\u00eddas: [${latLong[0]}, ${latLong[1]}]")
                    LocationResult(latitude = latLong[0], longitude = latLong[1])
                } else {
                    Log.w("Bitaru_EXIF", "Stream le\u00eddo correctamente, pero la imagen carece de metadatos GPS.")
                    null
                }
            }
        } catch (e: IOException) {
            Log.e("Bitaru_EXIF", "Error f\u00edsico de E/S al abrir stream del archivo", e)
            null
        } catch (e: SecurityException) {
            Log.e("Bitaru_EXIF", "Permiso denegado al acceder al stream", e)
            null
        } catch (e: Exception) {
            Log.e("Bitaru_EXIF", "Error cr\u00edtico inesperado en procesador EXIF", e)
            null
        }
    }
}

