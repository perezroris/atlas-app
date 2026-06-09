package com.atlas.data.model

data class RiskPoint(
    val latitude: Double,
    val longitude: Double,
    val type: RiskType,
    val source: String,
    val detectedAt: String,
    val description: String
)

enum class RiskType { FIRE, FLOOD_RISK }
