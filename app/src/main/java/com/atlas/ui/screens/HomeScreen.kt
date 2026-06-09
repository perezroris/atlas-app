package com.atlas.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atlas.data.model.RiskPoint
import com.atlas.data.model.RiskType
import com.atlas.ui.theme.*
import com.atlas.viewmodel.HomeViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.google.android.gms.maps.model.LatLng
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.atlas.data.api.RetrofitClient
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

private val SP_CENTER = Point.fromLngLat(-46.6333, -23.5505)

@Composable
fun HomeScreen(
    onMarkerClick: (RiskPoint) -> Unit,
    onAboutClick: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val historico by viewModel.historicoBuscas.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val destFocus = remember { FocusRequester() }
    var originText by remember { mutableStateOf("") }
    var destText   by remember { mutableStateOf("") }
    var alertExpanded by remember { mutableStateOf(false) }
    var buscandoGps by remember { mutableStateOf(false) }
    var campoFocado by remember { mutableStateOf("") }

    val riscoClidado = remember { mutableStateOf<RiskPoint?>(null) }

    var sugestoesOrigem by remember { mutableStateOf<List<String>>(emptyList()) }
    var sugestoesDestino by remember { mutableStateOf<List<String>>(emptyList()) }

    // flows com debounce para nao chamar a api a cada letra digitada
    val flowOrigem  = remember { MutableStateFlow("") }
    val flowDestino = remember { MutableStateFlow("") }

    // scope declarado antes do gpsLauncher pois ele usa o scope internamente
    val scope = rememberCoroutineScope()

    val gpsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            scope.launch {
                buscandoGps = true
                val localizacao = pegarUltimaLocalizacao(context)
                if (localizacao != null) {
                    val nome = viewModel.resolverNomePorCoordenadas(localizacao.first, localizacao.second)
                    if (nome != null) originText = nome
                }
                buscandoGps = false
            }
        }
    }

    @OptIn(FlowPreview::class)
    LaunchedEffect(Unit) {
        flowOrigem
            .debounce(400)
            .filter { it.length >= 3 }
            .distinctUntilChanged()
            .collect { texto ->
                try {
                    val resultados = RetrofitClient.nominatimApi.geocode(
                        query = "$texto, São Paulo",
                        limit = 5
                    )
                    sugestoesOrigem = resultados.map { it.displayName }
                } catch (e: Exception) {
                    sugestoesOrigem = emptyList()
                }
            }
    }

    @OptIn(FlowPreview::class)
    LaunchedEffect(Unit) {
        flowDestino
            .debounce(400)
            .filter { it.length >= 3 }
            .distinctUntilChanged()
            .collect { texto ->
                try {
                    val resultados = RetrofitClient.nominatimApi.geocode(
                        query = "$texto, São Paulo",
                        limit = 5
                    )
                    sugestoesDestino = resultados.map { it.displayName }
                } catch (e: Exception) {
                    sugestoesDestino = emptyList()
                }
            }
    }

    LaunchedEffect(uiState.activeRoute) { alertExpanded = false }

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(SP_CENTER)
            zoom(11.0)
        }
    }


    LaunchedEffect(uiState.activeRoute) {
        val route = uiState.activeRoute
        if (route.size >= 2) {
            val avgLat = route.map { it.latitude }.average()
            val avgLng = route.map { it.longitude }.average()
            val latSpan = route.maxOf { it.latitude } - route.minOf { it.latitude }
            val zoom = (13.5 - latSpan * 40).coerceIn(9.0, 14.0)
            mapViewportState.flyTo(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(avgLng, avgLat))
                    .zoom(zoom)
                    .build()
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState
        ) {
            MapEffect(Unit) { mapView ->
                val polylineManager = mapView.annotations.createPolylineAnnotationManager()
                val pointManager    = mapView.annotations.createPointAnnotationManager()

                val fireBitmap = riskMarkerBitmap(
                    fillColor = android.graphics.Color.rgb(220, 38, 38),
                    ringColor = android.graphics.Color.argb(120, 239, 68, 68),
                    size = 72
                )
                val floodBitmap = riskMarkerBitmap(
                    fillColor = android.graphics.Color.rgb(37, 99, 235),
                    ringColor = android.graphics.Color.argb(120, 96, 165, 250),
                    size = 72
                )
                val originBitmap = pinMarkerBitmap(android.graphics.Color.rgb(22, 163, 74), "A")
                val destBitmap   = pinMarkerBitmap(android.graphics.Color.rgb(220, 38, 38), "B")

                // guarda id da annotation -> riskpoint para o clique encontrar o ponto certo
                val mapaAnnotationRisco = mutableMapOf<String, RiskPoint>()

                // listener registrado uma vez so, fora do collect
                pointManager.addClickListener { annotation ->
                    val clicado = mapaAnnotationRisco[annotation.id]
                    if (clicado != null) riscoClidado.value = clicado
                    true
                }

                snapshotFlow {
                    Triple(
                        uiState.riskPoints,
                        Triple(uiState.mainRoute, uiState.activeRoute, uiState.safeRoute),
                        Triple(uiState.usingAlternative, uiState.originLatLng, uiState.destLatLng)
                    )
                }.distinctUntilChanged().collect { (risks, routes, extras) ->
                    val (mainRoute, activeRoute, safeRoute) = routes
                    val (usingAlt, originLatLng, destLatLng) = extras

                    polylineManager.deleteAll()

                    // rota principal em cinza quando a alternativa esta ativa
                    if (safeRoute.isNotEmpty() && mainRoute.isNotEmpty() && usingAlt) {
                        polylineManager.create(
                            PolylineAnnotationOptions()
                                .withPoints(mainRoute.toMapbox())
                                .withLineColor("#BBBBBB")
                                .withLineWidth(7.0)
                                .withLineOpacity(0.6)
                        )
                    }

                    if (activeRoute.isNotEmpty()) {
                        // sombra embaixo da rota para dar profundidade
                        val shadowColor = if (usingAlt) "#0D6B35" else "#0D4A8C"
                        polylineManager.create(
                            PolylineAnnotationOptions()
                                .withPoints(activeRoute.toMapbox())
                                .withLineColor(shadowColor)
                                .withLineWidth(16.0)
                                .withLineOpacity(0.35)
                        )
                        val color = if (usingAlt) "#16A34A" else "#1D6FD8"
                        polylineManager.create(
                            PolylineAnnotationOptions()
                                .withPoints(activeRoute.toMapbox())
                                .withLineColor(color)
                                .withLineWidth(10.0)
                        )
                    }

                    pointManager.deleteAll()
                    mapaAnnotationRisco.clear()

                    risks.forEach { risk ->
                        val bitmap = if (risk.type == RiskType.FIRE) fireBitmap else floodBitmap
                        // salva id -> riskpoint para o listener de clique usar depois
                        val annotation = pointManager.create(
                            PointAnnotationOptions()
                                .withPoint(Point.fromLngLat(risk.longitude, risk.latitude))
                                .withIconImage(bitmap)
                        )
                        mapaAnnotationRisco[annotation.id] = risk
                    }

                    if (originLatLng != null) {
                        pointManager.create(
                            PointAnnotationOptions()
                                .withPoint(Point.fromLngLat(originLatLng.longitude, originLatLng.latitude))
                                .withIconImage(originBitmap)
                        )
                    }
                    if (destLatLng != null) {
                        pointManager.create(
                            PointAnnotationOptions()
                                .withPoint(Point.fromLngLat(destLatLng.longitude, destLatLng.latitude))
                                .withIconImage(destBitmap)
                        )
                    }
                }
            }
        }

        if (riscoClidado.value != null) {
            RiscoDetalheSheet(
                risco = riscoClidado.value!!,
                onDismiss = { riscoClidado.value = null }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 44.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp,
                            color = Primary
                        )
                        Text("Carregando...", fontSize = 11.sp, color = TextSecondary)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFEF4444), CircleShape)
                        )
                        Text("${uiState.fireCount}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFEF4444))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF2563EB), CircleShape)
                        )
                        Text("${uiState.floodCount}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2563EB))
                        Text("alertas ativos", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                IconButton(
                    onClick = onAboutClick,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Sobre", tint = Primary, modifier = Modifier.size(20.dp))
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            AnimatedVisibility(visible = uiState.hasRoute) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    uiState.routeInfo?.let { info ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (uiState.usingAlternative) Color(0xFF16A34A) else Primary,
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Text(info.duration, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("·", color = Color.White.copy(alpha = 0.5f))
                                Text(info.distance, fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
                            }
                        }
                    } ?: Spacer(Modifier.size(1.dp))

                    if (!alertExpanded) {
                        val fabColor = when {
                            uiState.usingAlternative -> Color(0xFF16A34A)
                            uiState.hasRiskOnRoute && uiState.noAlternativeAvailable -> Color(0xFFF59E0B)
                            uiState.hasRiskOnRoute -> Color(0xFFDC2626)
                            else -> Color.Transparent
                        }
                        val fabIcon = if (uiState.usingAlternative) Icons.Default.CheckCircle else Icons.Default.Warning
                        val fabLabel = if (uiState.usingAlternative) "Segura" else "Risco"

                        if (uiState.hasRiskOnRoute) {
                            FloatingActionButton(
                                onClick = { alertExpanded = true },
                                containerColor = fabColor,
                                contentColor = Color.White,
                                shape = CircleShape,
                                modifier = Modifier.size(62.dp),
                                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(fabIcon, contentDescription = fabLabel, modifier = Modifier.size(22.dp))
                                    Text(fabLabel, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = uiState.hasRiskOnRoute && alertExpanded,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                val isGreen = uiState.usingAlternative
                val cardBg      = if (isGreen) Color(0xFF052E16) else Color(0xFF1A0A0A)
                val accentColor = if (isGreen) Color(0xFF4ADE80)  else Color(0xFFF87171)
                val iconBg      = if (isGreen) Color(0xFF16A34A)  else Color(0xFFDC2626)
                val divColor    = if (isGreen) Color(0xFF166534).copy(alpha = 0.5f) else Color(0xFF7F1D1D).copy(alpha = 0.5f)
                val listBg      = if (isGreen) Color(0xFF0D3321)  else Color(0xFF2D0F0F)
                val descColor   = if (isGreen) Color(0xFF6EE7B7)  else Color(0xFFD1D5DB)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(10.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(shape = CircleShape, color = iconBg, modifier = Modifier.size(42.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        if (isGreen) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (isGreen) "✅ Rota mais segura ativa" else "⚠ Risco na rota",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = accentColor
                                )
                                Text(
                                    if (isGreen)
                                        "Evitando ${uiState.risksOnRoute.size} área${if (uiState.risksOnRoute.size > 1) "s" else ""} de risco"
                                    else
                                        "${uiState.risksOnRoute.size} área${if (uiState.risksOnRoute.size > 1) "s" else ""} climática${if (uiState.risksOnRoute.size > 1) "s" else ""} no caminho",
                                    fontSize = 11.sp,
                                    color = accentColor.copy(alpha = 0.75f)
                                )
                            }
                            IconButton(onClick = { alertExpanded = false }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimizar", tint = accentColor)
                            }
                        }

                        HorizontalDivider(color = divColor)

                        // mostra a diferenca de tempo entre a rota original e a segura
                        uiState.safeRouteDurationMin?.let { duracaoSegura ->
                            val diffMin = duracaoSegura - uiState.mainRouteDurationMin
                            val textoDiff = when {
                                diffMin > 0  -> "⏱ +$diffMin min a mais que a rota original"
                                diffMin < 0  -> "⚡ ${-diffMin} min mais rápida que a rota original!"
                                else         -> "⏱ Mesmo tempo que a rota original"
                            }
                            val corDiff = if (diffMin <= 0) Color(0xFF4ADE80) else accentColor.copy(alpha = 0.75f)
                            Surface(shape = RoundedCornerShape(8.dp), color = if (isGreen) Color(0xFF0A2E1A) else Color(0xFF1A0A0A)) {
                                Text(
                                    text = textoDiff,
                                    fontSize = 11.sp,
                                    color = corDiff,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Text(
                            if (isGreen) "Riscos evitados na rota original:" else "Áreas de risco no caminho:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor.copy(alpha = 0.8f)
                        )
                        Surface(shape = RoundedCornerShape(10.dp), color = listBg) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                uiState.risksOnRoute.forEach { risk ->
                                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(if (risk.type == RiskType.FIRE) "🔥" else "🌊", fontSize = 15.sp)
                                        Column {
                                            Text(
                                                if (risk.type == RiskType.FIRE) "Foco de Incêndio" else "Zona de Alagamento",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                color = if (risk.type == RiskType.FIRE) Color(0xFFFBBF24) else Color(0xFF60A5FA)
                                            )
                                            Text(risk.description, fontSize = 11.sp, color = descColor, lineHeight = 16.sp)
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = divColor)

                        if (!isGreen && uiState.safeRoute.isNotEmpty()) {
                            Button(
                                onClick = { viewModel.usarRotaSegura() },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Mudar para Rota Segura", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        } else if (!isGreen && uiState.noAlternativeAvailable) {
                            Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF451A03)) {
                                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                                    Text("Sem alternativa — proceda com cautela.", fontSize = 11.sp, color = Color(0xFFFDE68A), lineHeight = 16.sp)
                                }
                            }
                        } else if (isGreen) {
                            OutlinedButton(
                                onClick = { viewModel.usarRotaOriginal() },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5)),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5).copy(alpha = 0.4f))
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Ver rota original (com risco)", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            uiState.routeError?.let { err ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1006))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(22.dp))
                        Text(err, fontSize = 13.sp, color = Color(0xFFFDE68A), lineHeight = 18.sp)
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(Color(0xFFE0E0E0), RoundedCornerShape(2.dp))
                            .align(Alignment.CenterHorizontally)
                    )

                    AnimatedVisibility(visible = uiState.originWeather != null || uiState.destWeather != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.originWeather?.let { w ->
                                    WeatherChip(
                                        label = "Saída",
                                        weather = w,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                uiState.destWeather?.let { w ->
                                    WeatherChip(
                                        label = "Chegada",
                                        weather = w,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    CampoEnderecoComSugestoes(
                        value = originText,
                        onValueChange = {
                            originText = it
                            scope.launch { flowOrigem.emit(it) }
                            if (it.length < 3) sugestoesOrigem = emptyList()
                        },
                        placeholder = "De onde você vai sair?",
                        letraLabel = "A",
                        corLabel = Color(0xFF16A34A),
                        corBorda = Color(0xFF16A34A),
                        corFundo = Color(0xFFF0FDF4),
                        sugestoes = if (originText.length >= 3 && campoFocado == "origem") sugestoesOrigem else emptyList(),
                        historico = if (originText.isEmpty() && campoFocado == "origem") historico else emptyList(),
                        onFocus = { campoFocado = "origem" },
                        onSugestaoSelecionada = { sugestao ->
                            originText = sugestao
                            sugestoesOrigem = emptyList()
                            campoFocado = ""
                            focusManager.clearFocus()
                        },
                        onClear = { originText = ""; sugestoesOrigem = emptyList() },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = {
                            sugestoesOrigem = emptyList()
                            campoFocado = "destino"
                            destFocus.requestFocus()
                        }),
                        modifier = Modifier.fillMaxWidth(),
                        onGpsClick = {
                            val permissaoJaConcedida = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                            if (permissaoJaConcedida) {
                                scope.launch {
                                    buscandoGps = true
                                    val loc = pegarUltimaLocalizacao(context)
                                    if (loc != null) {
                                        val nome = viewModel.resolverNomePorCoordenadas(loc.first, loc.second)
                                        if (nome != null) originText = nome
                                    }
                                    buscandoGps = false
                                }
                            } else {
                                gpsLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        },
                        carregandoGps = buscandoGps
                    )

                    CampoEnderecoComSugestoes(
                        value = destText,
                        onValueChange = {
                            destText = it
                            scope.launch { flowDestino.emit(it) }
                            if (it.length < 3) sugestoesDestino = emptyList()
                        },
                        placeholder = "Qual o seu destino?",
                        letraLabel = "B",
                        corLabel = Color(0xFFDC2626),
                        corBorda = Color(0xFFDC2626),
                        corFundo = Color(0xFFFFF5F5),
                        sugestoes = if (destText.length >= 3 && campoFocado == "destino") sugestoesDestino else emptyList(),
                        historico = if (destText.isEmpty() && campoFocado == "destino") historico else emptyList(),
                        onFocus = { campoFocado = "destino" },
                        onSugestaoSelecionada = { sugestao ->
                            destText = sugestao
                            sugestoesDestino = emptyList()
                            campoFocado = ""
                            focusManager.clearFocus()
                            viewModel.calcularRota(originText, sugestao)
                        },
                        onClear = { destText = ""; sugestoesDestino = emptyList() },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            sugestoesDestino = emptyList()
                            campoFocado = ""
                            focusManager.clearFocus()
                            viewModel.calcularRota(originText, destText)
                        }),
                        modifier = Modifier.fillMaxWidth().focusRequester(destFocus)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                sugestoesOrigem = emptyList()
                                sugestoesDestino = emptyList()
                                campoFocado = ""
                                focusManager.clearFocus()
                                viewModel.calcularRota(originText, destText)
                            },
                            enabled = originText.isNotBlank() && destText.isNotBlank() && !uiState.isCalculatingRoute,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            if (uiState.isCalculatingRoute) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp, color = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Calculando...", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            } else {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Traçar Rota", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                        if (uiState.hasRoute) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.limparRota()
                                    originText = ""
                                    destText = ""
                                },
                                modifier = Modifier.height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Limpar", fontSize = 14.sp)
                            }
                        }
                    }

                    AnimatedVisibility(visible = uiState.hasRoute) {
                        Button(
                            onClick = { /* TODO: navegacao gps */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0F172A)
                            )
                        ) {
                            Icon(
                                Icons.Default.Navigation,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = Color(0xFF38BDF8)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Iniciar Navegação",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Spacer(Modifier.width(10.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF38BDF8).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "Em breve",
                                    fontSize = 10.sp,
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (!uiState.hasRoute && !uiState.isLoading) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LegendDot(Color(0xFFEF4444))
                            Spacer(Modifier.width(4.dp))
                            Text("Queimada", fontSize = 11.sp, color = TextSecondary)
                            Spacer(Modifier.width(16.dp))
                            LegendDot(Color(0xFF2563EB))
                            Spacer(Modifier.width(4.dp))
                            Text("Alagamento", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CampoEnderecoComSugestoes(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    letraLabel: String,
    corLabel: Color,
    corBorda: Color,
    corFundo: Color,
    sugestoes: List<String>,
    historico: List<String> = emptyList(),
    onSugestaoSelecionada: (String) -> Unit,
    onClear: () -> Unit,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    modifier: Modifier = Modifier,
    onFocus: () -> Unit = {},
    onGpsClick: (() -> Unit)? = null,
    carregandoGps: Boolean = false
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 14.sp, color = TextSecondary) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state -> if (state.isFocused) onFocus() },
            leadingIcon = {
                Surface(shape = CircleShape, color = corLabel, modifier = Modifier.size(28.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(letraLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            },
            trailingIcon = {
                when {
                    value.isNotEmpty() -> IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, contentDescription = "Limpar", modifier = Modifier.size(16.dp), tint = TextSecondary)
                    }
                    onGpsClick != null -> IconButton(onClick = onGpsClick) {
                        if (carregandoGps) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = corLabel)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = "Minha localização", modifier = Modifier.size(18.dp), tint = corLabel)
                        }
                    }
                }
            },
            singleLine = true,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = corBorda,
                unfocusedBorderColor = Color(0xFFE5E7EB),
                focusedContainerColor = corFundo,
                unfocusedContainerColor = Color(0xFFFAFAFA)
            )
        )

        // se tem sugestoes mostra elas, senao mostra o historico
        val itensParaMostrar = sugestoes.ifEmpty { historico }
        val ehHistorico = sugestoes.isEmpty() && historico.isNotEmpty()

        if (itensParaMostrar.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column {
                    if (ehHistorico) {
                        Text(
                            "Buscas recentes",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }

                    itensParaMostrar.forEachIndexed { index, item ->
                        val nomeResumido = item.split(",").take(2).joinToString(",").trim()
                        val icone = if (ehHistorico) Icons.Default.History else Icons.Default.LocationOn

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSugestaoSelecionada(nomeResumido) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(icone, contentDescription = null, tint = corLabel, modifier = Modifier.size(16.dp))
                            Text(text = nomeResumido, fontSize = 13.sp, color = Color(0xFF1F2937), maxLines = 1)
                        }

                        if (index < itensParaMostrar.size - 1) {
                            HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(horizontal = 14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherChip(label: String, weather: com.atlas.data.model.LocationWeather, modifier: Modifier = Modifier) {
    val hasRain = weather.rainMm > 0
    val rainChance = when {
        hasRain && weather.rainMm > 5 -> 90
        hasRain -> 70
        weather.humidity > 85 -> 50
        weather.humidity > 70 -> 25
        else -> 0
    }
    val bgColor     = if (hasRain) Color(0xFFFFF7ED) else Color(0xFFF0F9FF)
    val borderColor = if (hasRain) Color(0xFFFB923C) else Color(0xFF7DD3FC)
    val labelColor  = if (hasRain) Color(0xFF9A3412) else Color(0xFF0369A1)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(weather.emoji, fontSize = 22.sp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = labelColor)
                Text(
                    "${weather.tempC.toInt()}°C",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(
                    if (rainChance > 0) "🌧 $rainChance% de chuva" else "☀ Sem chuva",
                    fontSize = 11.sp,
                    color = if (rainChance > 0) Color(0xFFEA580C) else Color(0xFF16A34A),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(color, CircleShape)
    )
}

@Composable
private fun InfoChip(
    text: String,
    background: Color = Color(0xFFF3F4F6),
    textColor: Color = TextSecondary
) {
    Surface(shape = RoundedCornerShape(20.dp), color = background) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = textColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RiscoDetalheSheet(risco: RiskPoint, onDismiss: () -> Unit) {

    val corPrincipal  = if (risco.type == RiskType.FIRE) Color(0xFFDC2626) else Color(0xFF2563EB)
    val corFundo      = if (risco.type == RiskType.FIRE) Color(0xFFFFF1F1) else Color(0xFFF0F5FF)
    val corBadge      = if (risco.type == RiskType.FIRE) Color(0xFFFFEDE0) else Color(0xFFDCEAFD)
    val emojiTipo     = if (risco.type == RiskType.FIRE) "🔥" else "🌊"
    val nomeTipo      = if (risco.type == RiskType.FIRE) "Foco de Queimada" else "Zona de Alagamento"
    val fonteDados    = if (risco.type == RiskType.FIRE) "NASA FIRMS — VIIRS SNPP" else "OpenWeatherMap"
    val dicaSeguranca = if (risco.type == RiskType.FIRE) {
        "Evite a área e mantenha as janelas fechadas. Fumaça densa pode ser prejudicial à saúde."
    } else {
        "Não trafegue por vias alagadas. Apenas 30 cm de água em movimento pode arrastar um carro."
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color(0xFFE0E0E0), RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = corFundo,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = corPrincipal,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(emojiTipo, fontSize = 26.sp)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = nomeTipo,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = corPrincipal
                        )
                        Surface(shape = RoundedCornerShape(6.dp), color = corBadge) {
                            Text(
                                text = fonteDados,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = corPrincipal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFE5E7EB))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "📋  Descrição",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
                Text(
                    text = risco.description,
                    fontSize = 13.sp,
                    color = Color(0xFF1F2937),
                    lineHeight = 20.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetalheCard(
                    emoji = "🕐",
                    label = "Detectado em",
                    valor = risco.detectedAt,
                    modifier = Modifier.weight(1f)
                )
                DetalheCard(
                    emoji = "📍",
                    label = "Coordenadas",
                    valor = "${"%.4f".format(risco.latitude)}, ${"%.4f".format(risco.longitude)}",
                    modifier = Modifier.weight(1f)
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFFBEB),
                border = BorderStroke(1.dp, Color(0xFFFDE68A))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("⚠️", fontSize = 16.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Dica de segurança",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = dicaSeguranca,
                            fontSize = 12.sp,
                            color = Color(0xFF78350F),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
            ) {
                Text("Fechar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun DetalheCard(
    emoji: String,
    label: String,
    valor: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(emoji, fontSize = 13.sp)
                Text(label, fontSize = 10.sp, color = Color(0xFF9CA3AF), fontWeight = FontWeight.Medium)
            }
            Text(valor, fontSize = 11.sp, color = Color(0xFF374151), fontWeight = FontWeight.SemiBold, lineHeight = 16.sp)
        }
    }
}

// converte lista de latlng para o formato que o mapbox usa
private fun List<LatLng>.toMapbox() =
    map { Point.fromLngLat(it.longitude, it.latitude) }

// cria o bitmap do marcador de risco: circulo colorido com halo externo
private fun riskMarkerBitmap(fillColor: Int, ringColor: Int, size: Int = 72): Bitmap {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val cx = size / 2f
    val cy = size / 2f

    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ringColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, size / 2f - 1f, haloPaint)

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
    }
    val r = size / 2f - 14f
    canvas.drawCircle(cx, cy, r, fillPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    canvas.drawCircle(cx, cy, r, borderPaint)

    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
    }
    canvas.drawCircle(cx, cy, 5f, dotPaint)

    return bmp
}

// cria o bitmap do pin de origem/destino: forma de gota com letra dentro
private fun pinMarkerBitmap(color: Int, letter: String, size: Int = 80): Bitmap {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val cx = size / 2f

    val bodyRadius = size * 0.32f
    val bodyTop = size * 0.05f
    val bodyBottom = bodyTop + bodyRadius * 2

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.argb(60, 0, 0, 0)
    }
    canvas.drawOval(
        RectF(cx - bodyRadius * 0.7f, bodyBottom + 2f, cx + bodyRadius * 0.7f, bodyBottom + 10f),
        shadowPaint
    )

    // triangulo da ponta do pin
    val pinPath = Path().apply {
        moveTo(cx - bodyRadius * 0.5f, bodyBottom - 2f)
        lineTo(cx, size * 0.92f)
        lineTo(cx + bodyRadius * 0.5f, bodyBottom - 2f)
        close()
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    canvas.drawPath(pinPath, fillPaint)

    canvas.drawCircle(cx, bodyTop + bodyRadius, bodyRadius, fillPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    canvas.drawCircle(cx, bodyTop + bodyRadius, bodyRadius, borderPaint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        textSize = bodyRadius * 1.1f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    val textY = bodyTop + bodyRadius - (textPaint.ascent() + textPaint.descent()) / 2f
    canvas.drawText(letter, cx, textY, textPaint)

    return bmp
}

// tenta pegar gps, se nao tiver usa a rede. retorna null se nao tiver permissao
@Suppress("MissingPermission")
private fun pegarUltimaLocalizacao(context: Context): Pair<Double, Double>? {
    return try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val locGps  = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val locRede = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        val melhor = locGps ?: locRede
        if (melhor != null) Pair(melhor.latitude, melhor.longitude) else null
    } catch (e: Exception) {
        null
    }
}
