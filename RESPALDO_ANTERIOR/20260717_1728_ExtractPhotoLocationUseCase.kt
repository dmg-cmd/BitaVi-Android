package com.aistudio.localizador.domain.usecase

import android.net.Uri
import com.aistudio.localizador.ui.PhotoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ExtractPhotoLocationUseCase @Inject constructor(
    private val photoManager: PhotoManager
) {
    suspend fun execute(photoUri: Uri): Pair<Double, Double>? {
        return withContext(Dispatchers.IO) {
            photoManager.getPhotoLocation(photoUri)
        }
    }
}
