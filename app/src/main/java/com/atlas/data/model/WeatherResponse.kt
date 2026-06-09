package com.atlas.data.model

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    val rain: RainData? = null,
    val weather: List<WeatherDesc> = emptyList(),
    val main: MainData? = null,
    val wind: WindData? = null,
    val name: String = ""
)

data class RainData(
    @SerializedName("1h") val lastHour: Double? = null
)

data class WeatherDesc(
    val description: String = "",
    val icon: String = ""
)

data class MainData(
    val temp: Double = 0.0,
    @SerializedName("feels_like") val feelsLike: Double = 0.0,
    val humidity: Int = 0
)

data class WindData(
    val speed: Double = 0.0
)


data class LocationWeather(
    val description: String,
    val tempC: Double,
    val humidity: Int,
    val windKmh: Double,
    val rainMm: Double,
    val emoji: String
)
