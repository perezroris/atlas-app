package com.atlas.data.api

import com.atlas.data.model.MapboxDirectionsResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MapboxDirectionsApi {
    @GET("directions/v5/mapbox/driving/{coordinates}")
    suspend fun getDirections(
        @Path("coordinates", encoded = true) coordinates: String,
        @Query("access_token") accessToken: String,
        @Query("alternatives") alternatives: Boolean = true,
        @Query("geometries") geometries: String = "polyline",
        @Query("overview") overview: String = "full",
        @Query("language") language: String = "pt-BR",
        @Query("steps") steps: Boolean = false
    ): MapboxDirectionsResponse
}
