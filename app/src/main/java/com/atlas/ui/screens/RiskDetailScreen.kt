package com.atlas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atlas.data.model.RiskPoint
import com.atlas.data.model.RiskType
import com.atlas.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiskDetailScreen(
    riskPoint: RiskPoint,
    onBack: () -> Unit
) {
    val isfire = riskPoint.type == RiskType.FIRE
    val typeLabel = if (isfire) "Queimada" else "Risco de Alagamento"
    val tagColor = if (isfire) RiskHigh else RiskMedium

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhes do Risco", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Type badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = tagColor
            ) {
                Text(
                    text = typeLabel,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // Detail card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailRow(label = "Tipo", value = typeLabel)
                    HorizontalDivider()
                    DetailRow(
                        label = "Coordenadas",
                        value = "Lat: ${"%.4f".format(riskPoint.latitude)}, " +
                                "Lon: ${"%.4f".format(riskPoint.longitude)}"
                    )
                    HorizontalDivider()
                    DetailRow(label = "Fonte", value = riskPoint.source)
                    HorizontalDivider()
                    DetailRow(label = "Detectado em", value = riskPoint.detectedAt)
                    HorizontalDivider()
                    DetailRow(label = "Descrição", value = riskPoint.description)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Voltar ao mapa", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            letterSpacing = 0.8.sp
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = TextPrimary
        )
    }
}
