package com.example.data.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton para proporcionar las instancias de red de forma eficiente.
 * Evita la duplicación de sockets e hilos de red.
 */
object RetrofitClient {
    private const val GROQ_BASE_URL = "https://api.groq.com/"
    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val groqRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(GROQ_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    private val geminiRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    val groqApi: GroqApi by lazy {
        groqRetrofit.create(GroqApi::class.java)
    }

    val geminiApi: GeminiApi by lazy {
        geminiRetrofit.create(GeminiApi::class.java)
    }
}
