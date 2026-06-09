package com.atlas.data.model

import com.google.gson.annotations.SerializedName

data class NominatimResult(
    @SerializedName("lat") val lat: String,
    @SerializedName("lon") val lon: String,
    @SerializedName("display_name") val displayName: String
)
