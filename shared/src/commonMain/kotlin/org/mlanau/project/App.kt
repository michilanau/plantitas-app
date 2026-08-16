package org.mlanau.project

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import org.koin.compose.viewmodel.koinViewModel
import org.mlanau.project.plant.presentation.home.HomeViewModel
import org.mlanau.project.plant.presentation.calendar.CalendarViewModel
import org.mlanau.project.plant.presentation.form.PlantFormViewModel
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.presentation.home.HomeScreen
import org.mlanau.project.plant.presentation.calendar.CalendarScreen
import org.mlanau.project.plant.presentation.form.PlantFormScreen
import org.mlanau.project.plant.presentation.detail.PlantDetailScreen
import org.mlanau.project.plant.presentation.detail.PlantDetailViewModel
import org.mlanau.project.settings.presentation.SettingsViewModel
import org.mlanau.project.settings.presentation.AboutScreen
import org.mlanau.project.settings.presentation.SettingsScreen
import org.mlanau.project.shared.ui.AppLocaleWrapper
import org.mlanau.project.shared.ui.theme.PlantitasTheme
import org.mlanau.project.shared.platform.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import org.jetbrains.compose.resources.stringResource
import plantitas_app.shared.generated.resources.*

sealed class Screen {
    data object Home : Screen()
    data object Calendar : Screen()
    data class PlantDetail(val plantId: Int) : Screen()
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
            var navigationStack by remember { mutableStateOf(listOf<Screen>(Screen.Home)) }
            val currentScreen = navigationStack.last()

            fun navigateTo(screen: Screen) {
                if (screen is Screen.Home || screen is Screen.Calendar) {
                    navigationStack = listOf(screen)
                } else {
                    navigationStack = navigationStack + screen
                }
            }

            fun goBack() {
                if (navigationStack.size > 1) {
                    navigationStack = navigationStack.dropLast(1)
                } else if (currentScreen is Screen.Calendar) {
                    navigateTo(Screen.Home)
                }
            }

            BackHandler(enabled = navigationStack.size > 1 || currentScreen is Screen.Calendar) {
                goBack()
            }

            val homeViewModel = koinViewModel<HomeViewModel>()
            val calendarViewModel = koinViewModel<CalendarViewModel>()
            val plantFormViewModel = koinViewModel<PlantFormViewModel>()
            val plantDetailViewModel = koinViewModel<PlantDetailViewModel>()

            Scaffold(
                bottomBar = {
                    if (currentScreen is Screen.Home || currentScreen is Screen.Calendar) {
                        NavigationBar {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                label = { Text(stringResource(Res.string.nav_plants)) },
                                selected = currentScreen is Screen.Home,
                                onClick = { navigateTo(Screen.Home) }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                                label = { Text(stringResource(Res.string.nav_calendar)) },
                                selected = currentScreen is Screen.Calendar,
                                onClick = { 
                                    navigateTo(Screen.Calendar)
                                    calendarViewModel.resetToToday()
                                }
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    when (val screen = currentScreen) {
                        is Screen.Home -> HomeScreen(
                            viewModel = homeViewModel,
                            onNavigateToSettings = { navigateTo(Screen.Settings) },
                            onNavigateToPlantDetail = { plantId -> navigateTo(Screen.PlantDetail(plantId)) },
                            onNavigateToPlantForm = { plant -> navigateTo(Screen.PlantForm(plant)) }
                        )
                        is Screen.PlantDetail -> PlantDetailScreen(
                            viewModel = plantDetailViewModel,
                            plantId = screen.plantId,
                            onBack = { goBack() },
                            onEdit = { plant -> navigateTo(Screen.PlantForm(plant)) }
                        )
                        is Screen.Calendar -> CalendarScreen(
                            viewModel = calendarViewModel,
                            onNavigateToPlantDetail = { plantId -> navigateTo(Screen.PlantDetail(plantId)) }
                        )
                        is Screen.PlantForm -> PlantFormScreen(
                            viewModel = plantFormViewModel,
                            initialPlant = screen.plant,
                            onBack = { goBack() }
                        )
                        is Screen.Settings -> SettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = { goBack() },
                            onNavigateToAbout = { navigateTo(Screen.About) }
                        )
                        is Screen.About -> AboutScreen(
                            onBack = { goBack() }
                        )
                    }
                }
            }
        }
    }
}
