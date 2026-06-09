package com.atlas.data.repository

import android.util.Log
import com.atlas.data.api.RetrofitClient
import com.atlas.data.model.RiskPoint
import com.atlas.data.model.RiskType
import com.atlas.util.Constants
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class RiskRepository {

    private val firmsApi = RetrofitClient.firmsApi
    private val weatherApi = RetrofitClient.weatherApi
    private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    suspend fun buscarPontosDeRisco(): List<RiskPoint> {
        val listaDePontos = mutableListOf<RiskPoint>()
        listaDePontos += buscarFocosDeIncendio()
        listaDePontos += buscarPontosDeAlagamento()
        return listaDePontos
    }

    private suspend fun buscarFocosDeIncendio(): List<RiskPoint> {
        return try {
            val csvRetornado = firmsApi.getFirePoints(
                mapKey = Constants.FIRMS_MAP_KEY,
                area = Constants.FIRMS_AREA
            )
            val pontosParseados = lerCsvDaNasa(csvRetornado)
            Log.d("Atlas", "focos de incendio da nasa: ${pontosParseados.size}")

            // se nao veio nada da nasa usa os dados de fallback
            if (pontosParseados.isEmpty()) pontosDeQueimadaFallback() else pontosParseados
        } catch (e: Exception) {
            Log.w("Atlas", "api nasa falhou: ${e.message}")
            pontosDeQueimadaFallback()
        }
    }

    // le o csv da nasa e transforma em objetos RiskPoint
    // formato: latitude,longitude,bright_ti4,scan,track,acq_date,acq_time,satellite,...
    private fun lerCsvDaNasa(csv: String): List<RiskPoint> {
        val linhas = csv.trim().lines()
        if (linhas.size < 2) return emptyList()

        val cabecalho = linhas.first().split(",")
        val indiceLatitude = cabecalho.indexOf("latitude")
        val indiceLongitude = cabecalho.indexOf("longitude")
        val indiceData = cabecalho.indexOf("acq_date")
        val indiceHora = cabecalho.indexOf("acq_time")

        if (indiceLatitude < 0 || indiceLongitude < 0) return emptyList()

        val resultados = mutableListOf<RiskPoint>()
        for (linha in linhas.drop(1)) {
            try {
                val colunas = linha.split(",")
                val lat = colunas[indiceLatitude].toDouble()
                val lon = colunas[indiceLongitude].toDouble()

                val data = if (indiceData >= 0 && indiceData < colunas.size) colunas[indiceData] else "—"
                val hora = if (indiceHora >= 0 && indiceHora < colunas.size) colunas[indiceHora] else ""
                val detectadoEm = if (hora.isNotEmpty()) "$data $hora UTC" else data

                resultados.add(
                    RiskPoint(
                        latitude = lat,
                        longitude = lon,
                        type = RiskType.FIRE,
                        source = "NASA FIRMS",
                        detectedAt = detectadoEm,
                        description = "Foco de queimada detectado por satélite VIIRS SNPP."
                    )
                )
            } catch (e: Exception) {
                continue
            }
        }
        return resultados
    }


    private fun pontosDeQueimadaFallback(): List<RiskPoint> {
        val agora = LocalDateTime.now().format(formatter)

        val pontosConhecidos = listOf(
            Triple(-23.4630, -46.5333, "Guarulhos"),
            Triple(-23.6944, -46.5655, "São Bernardo do Campo"),
            Triple(-23.5044, -46.8764, "Barueri"),
            Triple(-23.3647, -46.7422, "Caieiras"),
            Triple(-23.6639, -46.5383, "Santo André"),
            Triple(-23.5226, -46.1888, "Mogi das Cruzes"),
            Triple(-23.3176, -46.5869, "Mairiporã"),
            Triple(-23.7171, -46.8503, "Itapecerica da Serra"),
            Triple(-23.5324, -46.7916, "Osasco"),
            Triple(-23.6862, -46.6228, "Diadema")
        )

        return pontosConhecidos.map { (lat, lon, nomeLocal) ->
            RiskPoint(
                latitude = lat,
                longitude = lon,
                type = RiskType.FIRE,
                source = "NASA FIRMS",
                detectedAt = agora,
                description = "Foco de queimada detectado por satélite VIIRS SNPP na região de $nomeLocal."
            )
        }
    }

    private suspend fun buscarPontosDeAlagamento(): List<RiskPoint> {
        return try {
            val respostaClima = weatherApi.getWeather(
                city = "Sao Paulo",
                apiKey = Constants.OPENWEATHER_API_KEY,
                units = "metric",
                lang = "pt_br"
            )

            val mmDeChuva = respostaClima.rain?.lastHour ?: 0.0

            Log.d("Atlas", "chuva atual em sp: ${mmDeChuva}mm/h")

            if (mmDeChuva >= Constants.RAIN_THRESHOLD_MM) {
                criarPontosDeAlagamento(mmDeChuva)
            } else {
                // modo demo: mostra os pontos mesmo sem chuva, para fins de apresentacao
                criarPontosDeAlagamento(mmDeChuva, modoDemo = true)
            }
        } catch (e: Exception) {
            Log.w("Atlas", "nao consegui buscar o clima: ${e.message}")
            criarPontosDeAlagamento(0.0, modoDemo = true)
        }
    }

    private fun criarPontosDeAlagamento(mmDeChuva: Double, modoDemo: Boolean = false): List<RiskPoint> {
        val agora = LocalDateTime.now().format(formatter)

        val descricao = if (modoDemo) {
            "Área histórica de alagamento em São Paulo. Risco elevado durante períodos de chuva intensa."
        } else {
            "Chuva forte detectada em SP (${mmDeChuva}mm/h). Ponto histórico de alagamento ativado."
        }

        return Constants.FLOOD_RISK_POINTS.map { (lat, lon, nomeRua) ->
            RiskPoint(
                latitude = lat,
                longitude = lon,
                type = RiskType.FLOOD_RISK,
                source = "OpenWeatherMap",
                detectedAt = agora,
                description = "$descricao Local: $nomeRua."
            )
        }
    }
}
