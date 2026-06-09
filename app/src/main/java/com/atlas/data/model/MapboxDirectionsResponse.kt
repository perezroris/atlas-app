package com.atlas.data.model

import com.google.gson.annotations.SerializedName

data class MapboxDirectionsResponse(
    val code: String,
    val routes: List<MapboxRoute> = emptyList(),
    val waypoints: List<MapboxWaypoint> = emptyList()
)

data class MapboxRoute(
    val geometry: String,
    val distance: Double,
    val duration: Double,
    val legs: List<MapboxLeg> = emptyList()
)

data class MapboxLeg(
    val distance: Double,
    val duration: Double,
    val summary: String = ""
)

data class MapboxWaypoint(
    val name: String = "",
    val location: List<Double> = emptyList()
)
