package com.example.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.export.RouteExchanger
import com.example.data.repository.RouteRepository

class RouteViewModelFactory(
    private val application: Application,
    private val repository: RouteRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val context = application.applicationContext
        val prefsManager = PreferencesManager(context)
        val geocoderUseCase = GeocoderUseCase(context)
        val photoManager = PhotoManager(context)
        val groqAiUseCase = GroqAiUseCase()
        val geminiAiUseCase = GeminiAiUseCase()
        val exchanger = RouteExchanger(context, repository)
        val routeExportUseCase = RouteExportUseCase(context, repository, exchanger)
        val backupUseCase = BackupUseCase(context, repository, geocoderUseCase)
        val contactUseCase = ContactUseCase(context)
        val shareUseCase = ShareUseCase(context)
        val locationServiceManager = LocationServiceManager(context)

        @Suppress("UNCHECKED_CAST")
        return RouteViewModel(
            repository = repository,
            prefsManager = prefsManager,
            geocoderUseCase = geocoderUseCase,
            photoManager = photoManager,
            groqAiUseCase = groqAiUseCase,
            geminiAiUseCase = geminiAiUseCase,
            routeExportUseCase = routeExportUseCase,
            backupUseCase = backupUseCase,
            contactUseCase = contactUseCase,
            shareUseCase = shareUseCase,
            locationServiceManager = locationServiceManager
        ) as T
    }
}
