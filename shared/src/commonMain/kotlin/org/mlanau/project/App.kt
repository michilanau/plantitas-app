package org.mlanau.project

import androidx.compose.runtime.*
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.mlanau.project.navigation.*
import org.mlanau.project.plant.application.MigrateBase64PlantImages
import org.mlanau.project.plant.application.RescheduleAllCareReminders
import org.mlanau.project.plant.presentation.home.HomeViewModel
import org.mlanau.project.plant.presentation.calendar.CalendarViewModel
import org.mlanau.project.plant.presentation.form.PlantFormViewModel
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
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import org.jetbrains.compose.resources.stringResource
import plantitas_app.shared.generated.resources.*

@Composable
fun App() {
    // SettingsViewModel is hoisted here (unlike every other ViewModel, which is scoped to its own
    // NavHost destination below): it drives the app shell itself — theme and locale — not just the
    // Settings screen, so it needs to outlive any single destination.
    val settingsViewModel = koinViewModel<SettingsViewModel>()
    val settingsState by settingsViewModel.uiState.collectAsState()

    val darkTheme = settingsState.isDarkMode

    // One-time (idempotent) migration of plant photos still stored as a data: URL in the database
    // to files on disk, see MigrateBase64PlantImages.
    val migrateBase64PlantImages = koinInject<MigrateBase64PlantImages>()
    LaunchedEffect(Unit) { migrateBase64PlantImages() }

    // Catches up every rule's reminder on launch. On Android this covers a rule edited while the
    // app was closed (BootReceiver only handles the "device just rebooted" case); on iOS it's the
    // *only* rescheduling path between mutations, since that platform has no hook to chain the next
    // alarm the moment one fires — see RescheduleAllCareReminders.
    val rescheduleAllCareReminders = koinInject<RescheduleAllCareReminders>()
    LaunchedEffect(Unit) { rescheduleAllCareReminders() }

    AppLocaleWrapper(languageCode = settingsState.languageCode) {
        PlantitasTheme(darkTheme = darkTheme) {
            val navController = rememberNavController()
            val currentDestination = navController.currentBackStackEntryAsState().value?.destination
            val isHome = currentDestination?.hasRoute<Home>() == true
            val isCalendar = currentDestination?.hasRoute<Calendar>() == true

            Scaffold(
                bottomBar = {
                    if (isHome || isCalendar) {
                        NavigationBar {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                label = { Text(stringResource(Res.string.nav_plants)) },
                                selected = isHome,
                                onClick = { navController.navigateAsTab(Home) }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                                label = { Text(stringResource(Res.string.nav_calendar)) },
                                selected = isCalendar,
                                onClick = { navController.navigateAsTab(Calendar) }
                            )
                        }
                    }
                }
            ) { paddingValues ->
                NavHost(
                    navController = navController,
                    startDestination = Home,
                    modifier = Modifier.padding(paddingValues)
                ) {
                    composable<Home> {
                        val viewModel = koinViewModel<HomeViewModel>()
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToSettings = { navController.navigate(Settings) },
                            onNavigateToPlantDetail = { plantId -> navController.navigate(PlantDetail(plantId)) },
                            onNavigateToPlantForm = { navController.navigate(PlantForm()) }
                        )
                    }
                    composable<Calendar> {
                        val viewModel = koinViewModel<CalendarViewModel>()
                        // The tab is reached via navigateAsTab's popUpTo/restoreState, which reuses
                        // this ViewModel instance across visits rather than recreating it — so
                        // "always show today when the tab is (re)selected" needs an explicit reset
                        // here, the same way the previous ad-hoc navigation stack did.
                        LaunchedEffect(Unit) { viewModel.resetToToday() }
                        CalendarScreen(
                            viewModel = viewModel,
                            onNavigateToPlantDetail = { plantId -> navController.navigate(PlantDetail(plantId)) }
                        )
                    }
                    composable<PlantDetail> { backStackEntry ->
                        val route = backStackEntry.toRoute<PlantDetail>()
                        val viewModel = koinViewModel<PlantDetailViewModel>()
                        PlantDetailScreen(
                            viewModel = viewModel,
                            plantId = route.plantId,
                            onBack = { navController.popBackStack() },
                            onEdit = { plantId -> navController.navigate(PlantForm(plantId)) }
                        )
                    }
                    composable<PlantForm> { backStackEntry ->
                        val route = backStackEntry.toRoute<PlantForm>()
                        val viewModel = koinViewModel<PlantFormViewModel>()
                        PlantFormScreen(
                            viewModel = viewModel,
                            plantId = route.plantId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable<Settings> {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() },
                            onNavigateToAbout = { navController.navigate(About) }
                        )
                    }
                    composable<About> {
                        AboutScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}

/**
 * Navigates to a bottom-nav tab, replacing whatever was pushed on top of the previous tab and
 * preserving each tab's own state (e.g. calendar month, scroll position) across switches — the
 * standard Navigation Compose pattern for a bottom bar.
 */
private fun androidx.navigation.NavHostController.navigateAsTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
