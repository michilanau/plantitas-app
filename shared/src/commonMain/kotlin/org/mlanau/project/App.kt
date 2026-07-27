package org.mlanau.project

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import org.koin.compose.viewmodel.koinViewModel
import org.mlanau.project.plant.application.HomeViewModel
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.ui.HomeScreen
import org.mlanau.project.plant.ui.PlantFormScreen
import org.mlanau.project.settings.application.SettingsViewModel
import org.mlanau.project.settings.ui.AboutScreen
import org.mlanau.project.settings.ui.SettingsScreen
import org.mlanau.project.ui.AppLocaleWrapper
import org.mlanau.project.ui.theme.PlantitasTheme

sealed class Screen {
    data object Home : Screen()
    data class PlantForm(val plant: Plant? = null) : Screen()
    data object Settings : Screen()
    data object About : Screen()
}

@Composable
fun App() {
    val settingsViewModel = koinViewModel<SettingsViewModel>()
    val settingsState by settingsViewModel.uiState.collectAsState()
    
    val darkTheme = settingsState.isDarkMode

    AppLocaleWrapper(languageCode = settingsState.languageCode) {
        PlantitasTheme(darkTheme = darkTheme) {
            var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
            val homeViewModel = koinViewModel<HomeViewModel>()

            when (val screen = currentScreen) {
                is Screen.Home -> HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToSettings = { currentScreen = Screen.Settings },
                    onNavigateToPlantForm = { plant -> currentScreen = Screen.PlantForm(plant) }
                )
                is Screen.PlantForm -> PlantFormScreen(
                    viewModel = homeViewModel,
                    initialPlant = screen.plant,
                    onBack = { currentScreen = Screen.Home }
                )
                is Screen.Settings -> SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { currentScreen = Screen.Home },
                    onNavigateToAbout = { currentScreen = Screen.About }
                )
                is Screen.About -> AboutScreen(
                    onBack = { currentScreen = Screen.Settings }
                )
            }
        }
    }
}
