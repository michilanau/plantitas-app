package org.mlanau.project

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import org.koin.compose.viewmodel.koinViewModel
import org.mlanau.project.plant.application.HomeViewModel
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.ui.HomeScreen
import org.mlanau.project.plant.ui.PlantFormScreen
import org.mlanau.project.ui.theme.PlantitasTheme

sealed class Screen {
    data object Home : Screen()
    data class PlantForm(val plant: Plant? = null) : Screen()
}

@Composable
fun App() {
    val systemInDarkTheme = isSystemInDarkTheme()
    var isDarkModeOverride by remember { mutableStateOf<Boolean?>(null) }
    val darkTheme = isDarkModeOverride ?: systemInDarkTheme

    PlantitasTheme(darkTheme = darkTheme) {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
        val viewModel = koinViewModel<HomeViewModel>()

        when (val screen = currentScreen) {
            is Screen.Home -> HomeScreen(
                viewModel = viewModel,
                isDarkMode = darkTheme,
                onToggleTheme = { isDarkModeOverride = !darkTheme },
                onNavigateToPlantForm = { plant -> currentScreen = Screen.PlantForm(plant) }
            )
            is Screen.PlantForm -> PlantFormScreen(
                viewModel = viewModel,
                initialPlant = screen.plant,
                onBack = { currentScreen = Screen.Home }
            )
        }
    }
}
