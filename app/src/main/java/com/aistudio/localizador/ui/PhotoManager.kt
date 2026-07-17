package com.aistudio.localizador.ui

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PhotoManager(private val context: Context) {

    fun getPhotoLocation(uri: Uri): Pair<Double, Double>? {
        return try {
            val photoUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) android.provider.MediaStore.setRequireOriginal(uri) else uri
            context.contentResolver.openInputStream(photoUri)?.use { input ->
                val exif = ExifInterface(input)
                val latLong = exif.latLong
                if (latLong != null && latLong.size >= 2) Pair(latLong[0], latLong[1]) else null
            }
        } catch (_: Exception) { null }
    }

    fun createImageFile(): Uri? {
        return try {
            val storageDir = File(context.filesDir, "images")
            if (!storageDir.exists()) storageDir.mkdirs()
            val file = File.createTempFile("IMG_${System.currentTimeMillis()}_", ".jpg", storageDir)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) { null }
    }

    suspend fun saveImageToInternalStorage(uri: Uri): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val storageDir = File(context.filesDir, "images")
                if (!storageDir.exists()) storageDir.mkdirs()
                val file = File(storageDir, "IMG_GALLERY_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (_: Exception) { null }
        }
    }
}
