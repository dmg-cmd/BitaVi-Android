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
import javax.inject.Inject

class ExtractPhotoLocationUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {

    data class LocationResult(val latitude: Double, val longitude: Double)

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
}
