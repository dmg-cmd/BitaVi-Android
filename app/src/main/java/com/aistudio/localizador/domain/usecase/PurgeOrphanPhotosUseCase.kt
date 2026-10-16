package com.aistudio.localizador.domain.usecase

import android.content.Context
import android.util.Log
import com.aistudio.localizador.data.repository.RouteRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class PurgeOrphanPhotosUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: RouteRepository
) {

    data class PurgeResult(
        val scanned: Int,
        val purged: Int
    )

    suspend fun execute(): Result<PurgeResult> = withContext(Dispatchers.IO) {
        runCatching {
            val imageDir = File(context.filesDir, "images")
            if (!imageDir.exists() || !imageDir.isDirectory) {
                return@runCatching PurgeResult(scanned = 0, purged = 0)
            }

            val referencedNames = repository.getAllReferencedPhotoPaths()
                .mapNotNull { path -> path.substringAfterLast('/').takeIf { it.isNotBlank() } }
                .toHashSet()

            var scanned = 0
            var purged = 0
            imageDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.isImageFile()) {
                    scanned++
                    if (file.name !in referencedNames) {
                        if (file.delete()) {
                            purged++
                            Log.d("BitaVi_Purge", "Foto huerfana purgada: ${file.name}")
                        }
                    }
                }
            }
            Log.d("BitaVi_Purge", "Purga completada: escaneadas=$scanned, purgadas=$purged")
            PurgeResult(scanned = scanned, purged = purged)
        }
    }

    private fun String.isImageFile(): Boolean {
        val lower = lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
    }
}