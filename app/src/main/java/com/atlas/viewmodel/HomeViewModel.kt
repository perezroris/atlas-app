package com.atlas.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atlas.data.api.RetrofitClient
import com.atlas.data.model.LocationWeather
import com.atlas.data.model.RiskPoint
import com.atlas.data.model.RiskType
import com.atlas.data.repository.RiskRepository
import com.atlas.util.Constants
import com.atlas.util.PolylineDecoder
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.*

data class RouteInfo(
    val distance: String,
    val duration: String,
    val destination: String
)

data class HomeUiState(
    val isLoading: Boolean = false,
    val isCalculatingRoute: Boolean = false,
    val riskPoints: List<RiskPoint> = emptyList(),
    val mainRoute: List<LatLng> = emptyList(),
    val safeRoute: List<LatLng> = emptyList(),
    val activeRoute: List<LatLng> = emptyList(),
    val routeInfo: RouteInfo? = null,
    val risksOnRoute: List<RiskPoint> = emptyList(),
    val usingAlternative: Boolean = false,
    val noAlternativeAvailable: Boolean = false,
    val error: String? = null,
    val routeError: String? = null,
    val originLatLng: LatLng? = null,
    val destLatLng: LatLng? = null,
    val originWeather: LocationWeather? = null,
    val destWeather: LocationWeather? = null,
    val mainRouteDurationMin: Int = 0,
    val safeRouteDurationMin: Int? = null
) {
    val fireCount: Int get() = riskPoints.count { it.type == RiskType.FIRE }
    val floodCount: Int get() = riskPoints.count { it.type == RiskType.FLOOD_RISK }
    val hasRoute: Boolean get() = activeRoute.isNotEmpty()
    val hasRiskOnRoute: Boolean get() = risksOnRoute.isNotEmpty()
}

class HomeViewModel : ViewModel() {

    private val repository = RiskRepository()
    private val nominatim = RetrofitClient.nominatimApi
    private val directionsApi = RetrofitClient.mapboxDirectionsApi
    private val weatherApi = RetrofitClient.weatherApi

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // historico de buscas fica em memoria durante a sessao
    private val _historicoBuscas = MutableStateFlow<List<String>>(emptyList())
    val historicoBuscas: StateFlow<List<String>> = _historicoBuscas.asStateFlow()

    init {
        carregarPontos()
    }

    fun carregarPontos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val listaPontos = repository.buscarPontosDeRisco()
                Log.d("Atlas", "pontos carregados: ${listaPontos.size}")
                _uiState.value = _uiState.value.copy(isLoading = false, riskPoints = listaPontos)
            } catch (e: Exception) {
                Log.e("Atlas", "erro ao carregar pontos: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Erro ao carregar dados climáticos.")
            }
        }
    }

    fun calcularRota(origin: String, destination: String) {
        if (origin.isBlank() || destination.isBlank()) return
        Log.d("Atlas", "calculando rota de '$origin' ate '$destination'")

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCalculatingRoute = true,
                routeError = null,
                mainRoute = emptyList(),
                safeRoute = emptyList(),
                activeRoute = emptyList(),
                risksOnRoute = emptyList(),
                usingAlternative = false,
                noAlternativeAvailable = false
            )

            try {
                // converte os enderecos em coordenadas usando nominatim (openstreetmap)
                val resultadoOrigem = nominatim.geocode("$origin, São Paulo, Brasil")
                val resultadoDestino = nominatim.geocode("$destination, São Paulo, Brasil")

                if (resultadoOrigem.isEmpty() || resultadoDestino.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isCalculatingRoute = false,
                        routeError = "Endereço não encontrado. Seja mais específico."
                    )
                    return@launch
                }

                val latOrigem = resultadoOrigem.first().lat.toDouble()
                val lngOrigem = resultadoOrigem.first().lon.toDouble()
                val latDestino = resultadoDestino.first().lat.toDouble()
                val lngDestino = resultadoDestino.first().lon.toDouble()

                val coordOrigem = LatLng(latOrigem, lngOrigem)
                val coordDestino = LatLng(latDestino, lngDestino)

                _uiState.value = _uiState.value.copy(originLatLng = coordOrigem, destLatLng = coordDestino)

                // busca clima nos dois pontos
                val climaOrigem = buscarClima(latOrigem, lngOrigem)
                val climaDestino = buscarClima(latDestino, lngDestino)
                _uiState.value = _uiState.value.copy(originWeather = climaOrigem, destWeather = climaDestino)

                val nomeDestino = resultadoDestino.first().displayName.split(",").take(2).joinToString(",")

                // mapbox espera longitude primeiro, depois latitude
                val coordsParaAPI = "$lngOrigem,$latOrigem;$lngDestino,$latDestino"

                val respostaAPI = directionsApi.getDirections(
                    coordinates = coordsParaAPI,
                    accessToken = Constants.MAPBOX_ACCESS_TOKEN,
                    alternatives = true
                )

                if (respostaAPI.code != "Ok" || respostaAPI.routes.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isCalculatingRoute = false,
                        routeError = "Não foi possível calcular a rota."
                    )
                    return@launch
                }

                // decodifica a geometria que vem como polyline encoded
                val rotaPrincipalDecodificada = PolylineDecoder.decodificar(respostaAPI.routes[0].geometry)

                val dadosRota = respostaAPI.routes[0]
                val distanciaKm = (dadosRota.distance / 1000 * 10).toInt() / 10.0
                val tempoMinutos = (dadosRota.duration / 60).toInt()

                // formata o tempo: se for mais de 60 min mostra em horas
                val tempoFormatado = if (tempoMinutos >= 60) {
                    val horas = tempoMinutos / 60
                    val minutosRestantes = tempoMinutos % 60
                    if (minutosRestantes == 0) "${horas}h" else "${horas}h ${minutosRestantes}min"
                } else {
                    "$tempoMinutos min"
                }

                val infoRota = RouteInfo("$distanciaKm km", tempoFormatado, nomeDestino)

                adicionarAoHistorico(origin)
                adicionarAoHistorico(destination)

                // verifica se a rota passa por alguma area de risco
                val todosOsRiscos = _uiState.value.riskPoints
                val coordenadasDeRisco = todosOsRiscos.map { LatLng(it.latitude, it.longitude) }
                val riscosNaRotaPrincipal = encontrarRiscosNaRota(rotaPrincipalDecodificada, todosOsRiscos, coordenadasDeRisco)

                Log.d("Atlas", "riscos na rota principal: ${riscosNaRotaPrincipal.size}")

                // sem risco: mostra a rota e para por aqui
                if (riscosNaRotaPrincipal.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isCalculatingRoute = false,
                        mainRoute = rotaPrincipalDecodificada,
                        safeRoute = emptyList(),
                        activeRoute = rotaPrincipalDecodificada,
                        routeInfo = infoRota,
                        risksOnRoute = emptyList(),
                        mainRouteDurationMin = tempoMinutos,
                        safeRouteDurationMin = null
                    )
                    return@launch
                }

                // riscos que estao longe o suficiente da origem/destino pra poder desviar
                val riscosQuePodemSerEvitados = riscosNaRotaPrincipal.filter { pontoDeRisco ->
                    val distanciaAteOrigem = PolylineDecoder.distanciaEmMetros(
                        coordOrigem.latitude, coordOrigem.longitude,
                        pontoDeRisco.latitude, pontoDeRisco.longitude
                    )
                    val distanciaAteDestino = PolylineDecoder.distanciaEmMetros(
                        coordDestino.latitude, coordDestino.longitude,
                        pontoDeRisco.latitude, pontoDeRisco.longitude
                    )
                    distanciaAteOrigem > 1500.0 && distanciaAteDestino > 1500.0
                }

                if (riscosQuePodemSerEvitados.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isCalculatingRoute = false,
                        mainRoute = rotaPrincipalDecodificada,
                        safeRoute = emptyList(),
                        activeRoute = rotaPrincipalDecodificada,
                        routeInfo = infoRota,
                        risksOnRoute = riscosNaRotaPrincipal,
                        usingAlternative = false,
                        noAlternativeAvailable = true,
                        mainRouteDurationMin = tempoMinutos,
                        safeRouteDurationMin = null
                    )
                    return@launch
                }

                // testa as rotas alternativas que o mapbox ja retornou
                var melhorRota = emptyList<LatLng>()
                var menorQuantidadeDeRiscos = riscosNaRotaPrincipal.size
                var duracaoRotaSeguraMin: Int? = null

                for (i in 1 until respostaAPI.routes.size) {
                    val rotaAlternativa = PolylineDecoder.decodificar(respostaAPI.routes[i].geometry)
                    val riscosNaAlternativa = encontrarRiscosNaRota(rotaAlternativa, todosOsRiscos, coordenadasDeRisco)

                    if (riscosNaAlternativa.size < menorQuantidadeDeRiscos) {
                        melhorRota = rotaAlternativa
                        menorQuantidadeDeRiscos = riscosNaAlternativa.size
                        duracaoRotaSeguraMin = (respostaAPI.routes[i].duration / 60).toInt()
                    }
                }

                // se as alternativas do mapbox nao foram suficientes, tenta desvios forcados
                val melhorDesvio = encontrarMelhorDesvio(
                    coordOrigem, coordDestino,
                    riscosQuePodemSerEvitados,
                    todosOsRiscos, coordenadasDeRisco,
                    menorQuantidadeDeRiscos
                )

                if (melhorDesvio != null && melhorDesvio.second < menorQuantidadeDeRiscos) {
                    melhorRota = melhorDesvio.first
                    menorQuantidadeDeRiscos = melhorDesvio.second
                }

                _uiState.value = _uiState.value.copy(
                    isCalculatingRoute = false,
                    mainRoute = rotaPrincipalDecodificada,
                    safeRoute = melhorRota,
                    activeRoute = rotaPrincipalDecodificada,
                    routeInfo = infoRota,
                    risksOnRoute = riscosNaRotaPrincipal,
                    usingAlternative = false,
                    noAlternativeAvailable = melhorRota.isEmpty(),
                    mainRouteDurationMin = tempoMinutos,
                    safeRouteDurationMin = duracaoRotaSeguraMin
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCalculatingRoute = false,
                    routeError = "Erro ao calcular rota. Verifique sua conexão."
                )
            }
        }
    }

    private suspend fun buscarClima(lat: Double, lon: Double): LocationWeather? {
        return try {
            val resposta = weatherApi.getWeatherByCoords(
                lat = lat,
                lon = lon,
                apiKey = Constants.OPENWEATHER_API_KEY
            )

            val descricao = resposta.weather.firstOrNull()?.description
                ?.replaceFirstChar { it.uppercase() } ?: "Sem dados"

            val chuvaUltimaHora = resposta.rain?.lastHour ?: 0.0
            // converte m/s para km/h
            val velocidadeVento = (resposta.wind?.speed ?: 0.0) * 3.6

            val iconeCodigo = resposta.weather.firstOrNull()?.icon ?: ""
            val emoji = pegarEmojiDoClima(iconeCodigo)

            LocationWeather(
                description = descricao,
                tempC = resposta.main?.temp ?: 0.0,
                humidity = resposta.main?.humidity ?: 0,
                windKmh = velocidadeVento,
                rainMm = chuvaUltimaHora,
                emoji = emoji
            )
        } catch (e: Exception) {
            null
        }
    }

    // converte o codigo do icone do openweathermap em emoji
    private fun pegarEmojiDoClima(codigoIcone: String): String {
        if (codigoIcone.startsWith("01")) return "☀️"
        if (codigoIcone.startsWith("02")) return "🌤"
        if (codigoIcone.startsWith("03")) return "⛅"
        if (codigoIcone.startsWith("04")) return "☁️"
        if (codigoIcone.startsWith("09")) return "🌧"
        if (codigoIcone.startsWith("10")) return "🌦"
        if (codigoIcone.startsWith("11")) return "⛈"
        if (codigoIcone.startsWith("13")) return "❄️"
        if (codigoIcone.startsWith("50")) return "🌫"
        return "🌡"
    }

    // testa varios pontos ao redor do risco pra achar uma rota com menos areas de perigo
    private suspend fun encontrarMelhorDesvio(
        origem: LatLng,
        destino: LatLng,
        riscos: List<RiskPoint>,
        todosOsRiscos: List<RiskPoint>,
        coordenadasDeRisco: List<LatLng>,
        melhorAtual: Int
    ): Pair<List<LatLng>, Int>? {

        val centroDoRisco = LatLng(
            riscos.map { it.latitude }.average(),
            riscos.map { it.longitude }.average()
        )

        // testa 12 angulos de 30 em 30 graus
        val angulos = listOf(0.0, 30.0, 60.0, 90.0, 120.0, 150.0, 180.0, 210.0, 240.0, 270.0, 300.0, 330.0)
        val distanciasEmKm = listOf(2.0, 4.0, 6.0, 9.0, 13.0)

        var melhorRota: List<LatLng> = emptyList()
        var menorRisco = melhorAtual

        for (distancia in distanciasEmKm) {
            for (angulo in angulos) {
                val pontoDeDesvio = calcularPontoDesvio(centroDoRisco, angulo, distancia)
                val rotaCandidata = buscarRotaMapbox(origem, destino, pontoDeDesvio) ?: continue
                val quantidadeRiscos = encontrarRiscosNaRota(rotaCandidata, todosOsRiscos, coordenadasDeRisco).size

                if (quantidadeRiscos < menorRisco) {
                    menorRisco = quantidadeRiscos
                    melhorRota = rotaCandidata
                    if (menorRisco == 0) return Pair(melhorRota, 0)
                }
            }
        }

        return if (melhorRota.isNotEmpty()) Pair(melhorRota, menorRisco) else null
    }

    private suspend fun buscarRotaMapbox(origem: LatLng, destino: LatLng, via: LatLng? = null): List<LatLng>? {
        return try {
            var coordenadas = "${origem.longitude},${origem.latitude}"
            if (via != null) coordenadas += ";${via.longitude},${via.latitude}"
            coordenadas += ";${destino.longitude},${destino.latitude}"

            val resposta = directionsApi.getDirections(
                coordinates = coordenadas,
                accessToken = Constants.MAPBOX_ACCESS_TOKEN,
                alternatives = false
            )

            if (resposta.code == "Ok" && resposta.routes.isNotEmpty()) {
                PolylineDecoder.decodificar(resposta.routes[0].geometry)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // calcula um ponto deslocado a partir de um centro usando trigonometria
    private fun calcularPontoDesvio(centro: LatLng, anguloGraus: Double, distanciaKm: Double): LatLng {
        val anguloRadianos = Math.toRadians(anguloGraus)
        val grausPorKm = 1.0 / 111.0  // aproximadamente 111km por grau de latitude

        val novaLat = centro.latitude + cos(anguloRadianos) * distanciaKm * grausPorKm
        val novaLng = centro.longitude + sin(anguloRadianos) * distanciaKm * grausPorKm

        return LatLng(novaLat, novaLng)
    }

    private fun encontrarRiscosNaRota(
        rota: List<LatLng>,
        todosOsRiscos: List<RiskPoint>,
        coordenadasDeRisco: List<LatLng>
    ): List<RiskPoint> {
        val riscosProximosARota = PolylineDecoder.riscosPertoDaRota(rota, coordenadasDeRisco, Constants.RISK_RADIUS_METERS)
        return todosOsRiscos.filter { risco ->
            riscosProximosARota.any { it.latitude == risco.latitude && it.longitude == risco.longitude }
        }
    }

    // adiciona ao historico sem duplicatas, maximo 5 itens
    fun adicionarAoHistorico(endereco: String) {
        if (endereco.isBlank()) return
        val listaAtual = _historicoBuscas.value.toMutableList()
        listaAtual.remove(endereco)
        listaAtual.add(0, endereco)
        _historicoBuscas.value = listaAtual.take(5)
    }

    // geocodificacao reversa: coordenadas gps -> nome do lugar
    suspend fun resolverNomePorCoordenadas(lat: Double, lon: Double): String? {
        return try {
            val resultado = nominatim.reverseGeocode(lat = lat, lon = lon)
            resultado.displayName.split(",").take(2).joinToString(",").trim()
        } catch (e: Exception) {
            null
        }
    }

    fun limparRota() {
        _uiState.value = _uiState.value.copy(
            mainRoute = emptyList(),
            safeRoute = emptyList(),
            activeRoute = emptyList(),
            routeInfo = null,
            risksOnRoute = emptyList(),
            usingAlternative = false,
            noAlternativeAvailable = false,
            routeError = null,
            originLatLng = null,
            destLatLng = null,
            originWeather = null,
            destWeather = null
        )
    }

    fun usarRotaOriginal() {
        _uiState.value = _uiState.value.copy(
            activeRoute = _uiState.value.mainRoute,
            usingAlternative = false
        )
    }

    fun usarRotaSegura() {
        _uiState.value = _uiState.value.copy(
            activeRoute = _uiState.value.safeRoute,
            usingAlternative = true
        )
    }
}
