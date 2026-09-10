package org.mlanau.project.plant.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.plant.domain.model.CareTask
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.presentation.component.NextCarePill
import org.mlanau.project.plant.presentation.component.getLightNeedString
import org.mlanau.project.plant.presentation.component.overdueColor
import org.mlanau.project.plant.presentation.localizedMessage
import org.mlanau.project.shared.ui.component.AppButton
import org.mlanau.project.shared.ui.component.AppButtonStyle
import org.mlanau.project.shared.ui.component.AppCard
import org.mlanau.project.shared.ui.component.AppSnackbarHost
import org.mlanau.project.shared.ui.component.EmptyState
import org.mlanau.project.shared.ui.component.PlantAvatar
import org.mlanau.project.shared.ui.component.RoundIconButton
import org.mlanau.project.shared.ui.component.ScreenHeader
import org.mlanau.project.shared.ui.monthName
import org.mlanau.project.shared.ui.theme.ContentMaxWidth
import org.mlanau.project.shared.ui.theme.ScreenGutter
import org.mlanau.project.shared.ui.theme.TabBarClearance
import org.mlanau.project.shared.ui.weekdayName
import plantitas_app.shared.generated.resources.*

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToPlantDetail: (Int) -> Unit,
    onNavigateToPlantForm: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    HomeContent(
        uiState = uiState,
        onNavigateToSettings = onNavigateToSettings,
        onClearError = { viewModel.clearError() },
        onNavigateToPlantDetail = onNavigateToPlantDetail,
        onNavigateToPlantForm = onNavigateToPlantForm
    )
}

@Composable
fun HomeContent(
    uiState: HomeUiState,
    onNavigateToSettings: () -> Unit,
    onClearError: () -> Unit,
    onNavigateToPlantDetail: (Int) -> Unit,
    onNavigateToPlantForm: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    uiState.error?.let { error ->
        val errorMessage = error.localizedMessage()
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(errorMessage)
            onClearError()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { AppSnackbarHost(snackbarHostState, Modifier.padding(bottom = TabBarClearance)) }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(modifier = Modifier.fillMaxSize().widthIn(max = ContentMaxWidth)) {
                ScreenHeader(
                    title = stringResource(Res.string.home_title),
                    eyebrow = todayLabel(),
                    actions = {
                        RoundIconButton(
                            icon = Res.drawable.ic_settings,
                            contentDescription = stringResource(Res.string.settings_title),
                            onClick = onNavigateToSettings
                        )
                    }
                )

                when {
                    uiState.isLoading -> {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    uiState.plants.isEmpty() -> {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth().padding(bottom = TabBarClearance),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyState(
                                title = stringResource(Res.string.home_welcome),
                                description = stringResource(Res.string.home_empty_description),
                                action = {
                                    AppButton(
                                        text = stringResource(Res.string.home_add_plant),
                                        onClick = onNavigateToPlantForm,
                                        style = AppButtonStyle.Accent,
                                        icon = Res.drawable.ic_plus
                                    )
                                }
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = ScreenGutter,
                                end = ScreenGutter,
                                top = 4.dp,
                                bottom = TabBarClearance
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.plants, key = { plant -> plant.id?.value ?: plant.hashCode() }) { plant ->
                                PlantCard(
                                    plant = plant,
                                    nextCare = plant.id?.let { uiState.nextCareByPlant[it] },
                                    onClick = { plant.id?.let { onNavigateToPlantDetail(it.value) } }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun todayLabel(): String {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return "${weekdayName(today.dayOfWeek)} · ${today.day} ${monthName(today.month)}"
}

@Composable
private fun PlantCard(
    plant: Plant,
    nextCare: CareTask.Pending?,
    onClick: () -> Unit
) {
    val lightLabel = plant.lightNeed?.let {
        stringResource(Res.string.plant_light_value, getLightNeedString(it).lowercase())
    }
    val subtitle = listOfNotNull(plant.location?.takeIf { it.isNotBlank() }, lightLabel)
        .joinToString(" · ")
        .ifBlank { plant.description.orEmpty() }

    AppCard(
        shape = MaterialTheme.shapes.large,
        border = if (nextCare?.isOverdue == true) BorderStroke(2.dp, overdueColor()) else null,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PlantAvatar(
                imageUrl = plant.imageUrl,
                seed = plant.id?.value ?: 0,
                modifier = Modifier.size(64.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = plant.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                NextCarePill(task = nextCare, modifier = Modifier.padding(top = 4.dp))
            }

            Icon(
                painter = painterResource(Res.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
