package com.atlas.data.api

import com.atlas.data.model.NominatimResult
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface NominatimApi {
    @Headers("User-Agent: AtlasApp/1.0")
    @GET("search")
    suspend fun geocode(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 1,
        @Query("countrycodes") country: String = "br"
    ): List<NominatimResult>


    @Headers("User-Agent: AtlasApp/1.0")
    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "json"
    ): NominatimResult
}
