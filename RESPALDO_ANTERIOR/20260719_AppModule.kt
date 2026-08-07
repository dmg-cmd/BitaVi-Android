package com.aistudio.localizador.di

import android.content.Context
import com.aistudio.localizador.data.export.RouteExchanger
import com.aistudio.localizador.data.repository.RouteRepository
import com.aistudio.localizador.domain.usecase.ExtractPhotoLocationUseCase
import com.aistudio.localizador.ui.BackupUseCase
import com.aistudio.localizador.ui.ContactUseCase
import com.aistudio.localizador.ui.GeminiAiUseCase
import com.aistudio.localizador.ui.GeocoderUseCase
import com.aistudio.localizador.ui.GroqAiUseCase
import com.aistudio.localizador.ui.LocationServiceManager
import com.aistudio.localizador.ui.PhotoManager
import com.aistudio.localizador.ui.PreferencesManager
import com.aistudio.localizador.ui.RouteExportUseCase
import com.aistudio.localizador.ui.ShareUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideGeocoderUseCase(@ApplicationContext context: Context): GeocoderUseCase {
        return GeocoderUseCase(context)
    }

    @Provides
    @Singleton
    fun providePhotoManager(@ApplicationContext context: Context): PhotoManager {
        return PhotoManager(context)
    }

    @Provides
    @Singleton
    fun provideGroqAiUseCase(): GroqAiUseCase = GroqAiUseCase()

    @Provides
    @Singleton
    fun provideGeminiAiUseCase(): GeminiAiUseCase = GeminiAiUseCase()

    @Provides
    @Singleton
    fun provideExtractPhotoLocationUseCase(
        @ApplicationContext context: Context
    ): ExtractPhotoLocationUseCase = ExtractPhotoLocationUseCase(context)

    @Provides
    @Singleton
    fun provideRouteExchanger(
        @ApplicationContext context: Context,
        repository: RouteRepository
    ): RouteExchanger = RouteExchanger(context, repository)

    @Provides
    @Singleton
    fun provideRouteExportUseCase(
        @ApplicationContext context: Context,
        repository: RouteRepository,
        exchanger: RouteExchanger
    ): RouteExportUseCase = RouteExportUseCase(context, repository, exchanger)

    @Provides
    @Singleton
    fun provideBackupUseCase(
        @ApplicationContext context: Context,
        repository: RouteRepository,
        geocoderUseCase: GeocoderUseCase
    ): BackupUseCase = BackupUseCase(context, repository, geocoderUseCase)

    @Provides
    @Singleton
    fun provideContactUseCase(@ApplicationContext context: Context): ContactUseCase {
        return ContactUseCase(context)
    }

    @Provides
    @Singleton
    fun provideShareUseCase(@ApplicationContext context: Context): ShareUseCase {
        return ShareUseCase(context)
    }

    @Provides
    @Singleton
    fun provideLocationServiceManager(@ApplicationContext context: Context): LocationServiceManager {
        return LocationServiceManager(context)
    }
}

