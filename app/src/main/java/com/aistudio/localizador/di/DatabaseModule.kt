package com.aistudio.localizador.di

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.aistudio.localizador.data.db.AppDatabase
import com.aistudio.localizador.util.AppConstants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providePassphrase(@ApplicationContext context: Context): ByteArray {
        val masterKey = MasterKey.Builder(context, AppConstants.Preferences.MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setUserAuthenticationRequired(false)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            context,
            AppConstants.Preferences.ENCRYPTED_PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val passphraseString = prefs.getString(AppConstants.Preferences.DB_PASSPHRASE_KEY, null)
            ?: throw IllegalStateException("DB passphrase not initialized. First launch?")

        return passphraseString.toByteArray(Charsets.UTF_8)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        passphrase: ByteArray
    ): AppDatabase {
        return AppDatabase.getInstance(context, passphrase)
    }

    @Provides
    @Singleton
    fun provideRouteDao(database: AppDatabase) = database.routeDao()

    @Provides
    @Singleton
    fun provideRouteRepository(routeDao: com.aistudio.localizador.data.db.RouteDao): com.aistudio.localizador.data.repository.RouteRepository {
        return com.aistudio.localizador.data.repository.RouteRepository(routeDao)
    }
}