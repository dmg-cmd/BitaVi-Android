package com.aistudio.localizador.data.api

import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GroqApi {
    @POST("openai/v1/chat/completions")
    suspend fun generateContent(
        @Header("Authorization") auth: String,
        @Body request: GroqRequest
    ): Response<GroqResponse>
}

@JsonClass(generateAdapter = true)
data class GroqRequest(
    val model: String = "llama-3.3-70b-versatile",
    val messages: List<GroqMessage>
)

@JsonClass(generateAdapter = true)
data class GroqMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class GroqResponse(
    val choices: List<GroqChoice>
)

@JsonClass(generateAdapter = true)
data class GroqChoice(
    val message: GroqMessage
)
