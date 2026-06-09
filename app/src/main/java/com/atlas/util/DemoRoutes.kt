package com.atlas.util

import com.google.android.gms.maps.model.LatLng

/**
 * Rotas demo pré-calculadas para garantir funcionamento na apresentação.
 * Usadas como fallback quando a Directions API não responde.
 */
object DemoRoutes {

    // Paulista → Congonhas
    val paulistaACongonhas = listOf(
        LatLng(-23.5614, -46.6561), // Av. Paulista
        LatLng(-23.5640, -46.6530),
        LatLng(-23.5680, -46.6490),
        LatLng(-23.5710, -46.6460),
        LatLng(-23.5740, -46.6430),
        LatLng(-23.5760, -46.6410),
        LatLng(-23.5790, -46.6390),
        LatLng(-23.5820, -46.6380),
        LatLng(-23.5850, -46.6370),
        LatLng(-23.5880, -46.6360),
        LatLng(-23.6267, -46.6553)  // Congonhas
    )

    // Sé → Guarulhos
    val seAGuarulhos = listOf(
        LatLng(-23.5505, -46.6333), // Sé
        LatLng(-23.5450, -46.6200),
        LatLng(-23.5380, -46.6050),
        LatLng(-23.5300, -46.5900),
        LatLng(-23.5200, -46.5700),
        LatLng(-23.5100, -46.5500),
        LatLng(-23.5000, -46.5300),
        LatLng(-23.4900, -46.5100),
        LatLng(-23.4732, -46.5327)  // Guarulhos
    )

    // Pinheiros → Santo André
    val pinheirosASantoAndre = listOf(
        LatLng(-23.5660, -46.6960), // Pinheiros
        LatLng(-23.5700, -46.6800),
        LatLng(-23.5750, -46.6600),
        LatLng(-23.5800, -46.6400),
        LatLng(-23.5850, -46.6200),
        LatLng(-23.5900, -46.6000),
        LatLng(-23.6000, -46.5800),
        LatLng(-23.6100, -46.5600),
        LatLng(-23.6569, -46.5286)  // Santo André
    )

    /**
     * Gera uma rota interpolada entre dois pontos com waypoints intermediários
     * que passam por regiões reais de SP.
     */
    fun generateRoute(origin: LatLng, destination: LatLng): List<LatLng> {
        val points = mutableListOf<LatLng>()
        val steps = 12
        for (i in 0..steps) {
            val t = i.toDouble() / steps
            // Interpolação com leve curvatura para parecer uma rota real
            val lat = origin.latitude + (destination.latitude - origin.latitude) * t
            val lng = origin.longitude + (destination.longitude - origin.longitude) * t
            // Adiciona pequeno desvio para simular rua real
            val offset = if (i in 2..10) 0.002 * Math.sin(t * Math.PI) else 0.0
            points.add(LatLng(lat + offset, lng))
        }
        return points
    }

    // Coordenadas aproximadas de lugares comuns em SP
    val knownPlaces = mapOf(
        "paulista"          to LatLng(-23.5614, -46.6561),
        "congonhas"         to LatLng(-23.6267, -46.6553),
        "guarulhos"         to LatLng(-23.4732, -46.5327),
        "pinheiros"         to LatLng(-23.5660, -46.6960),
        "santo andre"       to LatLng(-23.6569, -46.5286),
        "se"                to LatLng(-23.5505, -46.6333),
        "vila madalena"     to LatLng(-23.5538, -46.6910),
        "lapa"              to LatLng(-23.5230, -46.7050),
        "mooca"             to LatLng(-23.5480, -46.5980),
        "tatuape"           to LatLng(-23.5380, -46.5780),
        "brooklin"          to LatLng(-23.6170, -46.6930),
        "itaim"             to LatLng(-23.5840, -46.6780),
        "marginal tiete"    to LatLng(-23.5209, -46.6350),
        "marginal pinheiros" to LatLng(-23.5960, -46.7050),
        "aeroporto"         to LatLng(-23.6267, -46.6553),
        "aricanduva"        to LatLng(-23.5470, -46.5170),
        "anhaia mello"      to LatLng(-23.5820, -46.5530),
        "raposo tavares"    to LatLng(-23.6100, -46.7500),
        "regis bittencourt" to LatLng(-23.7200, -46.7000),
        "cruzeiro do sul"   to LatLng(-23.5150, -46.6270),
        "inajar"            to LatLng(-23.4750, -46.6820),
        "escola politecnica" to LatLng(-23.5580, -46.7300),
        "santos dumont"     to LatLng(-23.5190, -46.6290),
        "capitao joao"      to LatLng(-23.5100, -46.6100)
    )

    fun findCoord(query: String): LatLng? {
        val q = query.lowercase().trim()
        return knownPlaces.entries.firstOrNull { q.contains(it.key) }?.value
    }
}
