package com.aistudio.localizador.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class NominatimAddressDto(
    @Json(name = "country") val country: String?,
    @Json(name = "city") val city: String?,
    @Json(name = "town") val town: String?,
    @Json(name = "village") val village: String?,
    @Json(name = "suburb") val suburb: String?,
    @Json(name = "road") val road: String?
) {
    fun getBestCityName(): String? {
        return city ?: town ?: village ?: suburb
    }
}

@JsonClass(generateAdapter = true)
data class NominatimResponseDto(
    @Json(name = "display_name") val displayName: String?,
    @Json(name = "address") val address: NominatimAddressDto?
)

interface NominatimApi {
    @GET("reverse")
    fun reverseGeocode(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("format") format: String = "json",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("accept-language") acceptLanguage: String = "es"
    ): Call<NominatimResponseDto>
}