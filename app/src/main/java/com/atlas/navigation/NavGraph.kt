package com.atlas.navigation

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.atlas.data.model.RiskPoint
import com.atlas.ui.screens.*

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val DETAIL = "detail"
    const val ABOUT = "about"
}

@Composable
fun AtlasNavGraph() {
    val navController = rememberNavController()
    // guarda o ponto clicado para passar para a tela de detalhe
    var selectedPoint by remember { mutableStateOf<RiskPoint?>(null) }

    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            SplashScreen(onFinished = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            })
        }

        composable(Routes.HOME) {
            HomeScreen(
                onMarkerClick = { point ->
                    selectedPoint = point
                    navController.navigate(Routes.DETAIL)
                },
                onAboutClick = { navController.navigate(Routes.ABOUT) }
            )
        }

        composable(Routes.DETAIL) {
            val point = selectedPoint
            if (point != null) {
                RiskDetailScreen(
                    riskPoint = point,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
