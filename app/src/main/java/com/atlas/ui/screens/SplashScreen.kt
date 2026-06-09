package com.atlas.ui.screens

// imports para animacao e composables
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import com.atlas.R
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atlas.ui.theme.Primary
import kotlinx.coroutines.delay

// mesma cor do icone do app
private val CorFundo = Color(0xFF0F2547)

@Composable
fun SplashScreen(onFinished: () -> Unit) {

    // controla quando comecar cada animacao
    var iniciarAnimacao by remember { mutableStateOf(false) }

    // animacao de escala do icone: comeca pequeno e cresce
    val escalaIcone by animateFloatAsState(
        targetValue = if (iniciarAnimacao) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow
        ),
        label = "escala_icone"
    )

    // animacao de opacidade do texto: aparece gradualmente
    val opacidadeTexto by animateFloatAsState(
        targetValue = if (iniciarAnimacao) 1f else 0f,
        animationSpec = tween(durationMillis = 700, delayMillis = 400),
        label = "opacidade_texto"
    )

    // animacao de opacidade do subtitulo: aparece um pouco depois
    val opacidadeSubtitulo by animateFloatAsState(
        targetValue = if (iniciarAnimacao) 1f else 0f,
        animationSpec = tween(durationMillis = 700, delayMillis = 700),
        label = "opacidade_subtitulo"
    )

    // animacao de pulso no halo do icone
    val pulso by rememberInfiniteTransition(label = "pulso").animateFloat(
        initialValue = 1f,
        targetValue  = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "escala_pulso"
    )

    // dispara a animacao logo que a tela aparecer e espera 2.5s para navegar
    LaunchedEffect(Unit) {
        iniciarAnimacao = true
        delay(2500L)
        onFinished()
    }

    // fundo azul marinho solido — igual ao icone do app
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CorFundo),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            // --- ICONE COM HALO PULSANTE ---
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                // halo externo pulsante
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(pulso)
                        .background(
                            color = Color.White.copy(alpha = 0.06f),
                            shape = CircleShape
                        )
                )

                // icone dedicado da splash (usa evenOdd pra mostrar a grade do globo)
                Icon(
                    painter = painterResource(id = R.drawable.ic_splash),
                    contentDescription = "Atlas",
                    tint = Color.Unspecified,  // preserva as cores originais do XML
                    modifier = Modifier
                        .size(130.dp)
                        .scale(escalaIcone)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- NOME DO APP ---
            Text(
                text = "Atlas",
                color = Color.White,
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp,
                modifier = Modifier.alpha(opacidadeTexto)
            )

            Spacer(modifier = Modifier.height(60.dp))

            // --- RODAPE FIAP ---
            Text(
                text = "FIAP — Global Solution 2025",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.alpha(opacidadeSubtitulo)
            )
        }
    }
}
