package com.atlas.util

import com.google.android.gms.maps.model.LatLng
import kotlin.math.*

object PolylineDecoder {

    // decodifica o formato google encoded polyline para lista de latlng
    fun decodificar(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            poly.add(LatLng(lat / 1E5, lng / 1E5))
        }
        return poly
    }

    fun distanciaEmMetros(a: LatLng, b: LatLng): Double =
        distanciaEmMetros(a.latitude, a.longitude, b.latitude, b.longitude)

    // calcula distancia em metros entre dois pontos usando haversine
    fun distanciaEmMetros(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val sinDLat = sin(dLat / 2)
        val sinDLon = sin(dLon / 2)
        val c = sinDLat * sinDLat +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sinDLon * sinDLon
        return r * 2 * atan2(sqrt(c), sqrt(1 - c))
    }

    // verifica se algum ponto de risco esta dentro do raio de algum segmento da rota
    fun riscosPertoDaRota(
        route: List<LatLng>,
        riskPoints: List<LatLng>,
        radiusMeters: Double = 600.0
    ): List<LatLng> {
        if (route.isEmpty()) return emptyList()
        // pega um ponto a cada 3 para nao checar todos (melhora a performance)
        val sampled = route.filterIndexed { i, _ -> i % 3 == 0 }
        return riskPoints.filter { risk ->
            sampled.any { routePt -> distanciaEmMetros(routePt, risk) <= radiusMeters }
        }
    }
}
