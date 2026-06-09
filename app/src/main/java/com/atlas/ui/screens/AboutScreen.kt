package com.atlas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atlas.ui.theme.*

// cores locais para nao poluir o theme
private val AzulEscuro  = Color(0xFF0D3B6E)
private val AzulMedio   = Color(0xFF1A6BB5)
private val CinzaFundo  = Color(0xFFF8FAFC)
private val CinzaCard   = Color(0xFFFFFFFF)
private val CinzaTexto  = Color(0xFF64748B)
private val AzulTexto   = Color(0xFF1A6BB5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sobre o Atlas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AzulMedio,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CinzaFundo)
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- CABECALHO COM GRADIENTE ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(AzulMedio, AzulEscuro)
                        )
                    )
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // icone do app como emoji grande dentro de circulo branco
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "📍", fontSize = 38.sp)
                    }

                    Text(
                        text = "Atlas",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    )

                    Text(
                        text = "Navegação com dados climáticos",
                        color = Color.White.copy(alpha = 0.80f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    // badge de versao
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.20f)
                    ) {
                        Text(
                            text = "v1.0  •  FIAP Global Solution 2025",
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // --- CARDS DE CONTEUDO ---
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                AboutCard(
                    emoji = "🗺️",
                    title = "O que é o Atlas?",
                    body = "Atlas é um protótipo acadêmico desenvolvido para a Global Solution da FIAP 2025. " +
                            "O app exibe em tempo real áreas de risco climático em São Paulo, " +
                            "sugere rotas seguras evitando queimadas e alagamentos, " +
                            "e mostra o clima atual na origem e no destino da viagem."
                )

                AboutCard(
                    emoji = "🔥",
                    title = "Focos de Queimada — NASA FIRMS",
                    body = "O Fire Information for Resource Management System (FIRMS) distribui dados " +
                            "globais de focos de queimada detectados pelo satélite VIIRS SNPP nas últimas 24 horas. " +
                            "Os marcadores vermelhos no mapa representam esses pontos reais.\n\n" +
                            "Fonte: firms.modaps.eosdis.nasa.gov"
                )

                AboutCard(
                    emoji = "🌊",
                    title = "Pontos de Alagamento — OpenWeatherMap",
                    body = "A API de clima atual da OpenWeatherMap fornece dados de precipitação em tempo real. " +
                            "Quando a chuva em São Paulo supera 5 mm/h, o Atlas ativa marcadores azuis nos " +
                            "pontos históricos de alagamento da cidade.\n\n" +
                            "Fonte: openweathermap.org"
                )

                AboutCard(
                    emoji = "🛣️",
                    title = "Rotas — Mapbox Directions",
                    body = "O cálculo de rotas usa a API Mapbox Directions, que fornece a rota principal " +
                            "e possíveis alternativas. O Atlas analisa se alguma rota passa por áreas de risco " +
                            "e sugere automaticamente o caminho mais seguro ao usuário."
                )

                AboutCard(
                    emoji = "⚙️",
                    title = "Tecnologias utilizadas",
                    body = "• Kotlin + Jetpack Compose\n" +
                            "• Material Design 3\n" +
                            "• Mapbox Maps SDK v11 + Directions API\n" +
                            "• Retrofit + OkHttp (chamadas REST)\n" +
                            "• ViewModel + StateFlow + Coroutines\n" +
                            "• NASA FIRMS (CSV de queimadas)\n" +
                            "• OpenWeatherMap (clima em tempo real)\n" +
                            "• Nominatim / OpenStreetMap (geocoding)"
                )

                // --- RODAPE ---
                Spacer(modifier = Modifier.height(8.dp))

                // linha divisoria
                HorizontalDivider(color = Color(0xFFE2E8F0))

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Desenvolvido como projeto acadêmico para a\nFIAP — Global Solution 2025",
                    fontSize = 12.sp,
                    color = CinzaTexto,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Os dados de focos de queimada são fornecidos pela NASA FIRMS e " +
                            "podem apresentar atraso de até 3 horas em relação ao tempo real.",
                    fontSize = 11.sp,
                    color = CinzaTexto.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// card reutilizavel com emoji, titulo e descricao
@Composable
private fun AboutCard(emoji: String, title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CinzaCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // circulo com emoji
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = AzulMedio.copy(alpha = 0.10f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 18.sp)
            }

            // textos
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = AzulTexto
                )
                Text(
                    text = body,
                    fontSize = 13.sp,
                    color = Color(0xFF334155),
                    lineHeight = 19.sp
                )
            }
        }
    }
}
