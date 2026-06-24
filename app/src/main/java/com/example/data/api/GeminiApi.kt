package com.example.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GeminiApi {
    @POST("https://api.groq.com/openai/v1/chat/completions")
    suspend fun generateContent(
        @Header("Authorization") auth: String,
        @Body request: GroqRequest
    ): Response<GroqResponse>
}
