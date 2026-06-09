package com.atlas.data.api

import retrofit2.http.GET
import retrofit2.http.Path

interface FirmsApi {

    @GET("api/area/csv/{mapKey}/{area}")
    suspend fun getFirePoints(
        @Path("mapKey") mapKey: String,
        @Path("area", encoded = true) area: String
    ): String
}
