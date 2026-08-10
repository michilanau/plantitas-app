package org.mlanau.project.plant.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.plant.domain.model.*
import plantitas_app.shared.generated.resources.*
import kotlinx.datetime.*
import org.mlanau.project.shared.ui.theme.PlantitasTheme
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.graphics.vector.ImageVector
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlantDetailScreen(
    viewModel: PlantDetailViewModel,
    plantId: Int,
    onBack: () -> Unit,
    onEdit: (Plant) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(plantId) {
        viewModel.loadPlant(plantId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.plant?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(Res.string.common_back))
                    }
                },
                actions = {
                    uiState.plant?.let { plant ->
                        IconButton(onClick = { onEdit(plant) }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.care_edit_rule))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            uiState.plant?.let { plant ->
                PlantDetailContent(
                    plant = plant,
                    nextEvent = uiState.nextEvent,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlantDetailContent(
    plant: Plant,
    nextEvent: CareEvent?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header / Icon
        Surface(
            modifier = Modifier.fillMaxWidth().height(240.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            if (plant.imageUrl != null) {
                AsyncImage(
                    model = plant.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Text("🌿", style = MaterialTheme.typography.displayLarge)
                }
            }
        }

        // Info Grid
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            FlowRow(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                plant.location?.takeIf { it.isNotBlank() }?.let {
                    DetailAttribute(Icons.Default.LocationOn, stringResource(Res.string.home_plant_location), it)
                }
                plant.lightNeed?.let {
                    DetailAttribute(
                        Icons.Default.WbSunny,
                        stringResource(Res.string.home_plant_light),
                        when(it) {
                            LightNeed.LOW -> stringResource(Res.string.light_low)
                            LightNeed.MEDIUM -> stringResource(Res.string.light_medium)
                            LightNeed.HIGH -> stringResource(Res.string.light_high)
                        }
                    )
                }
                plant.potSize?.let {
                    DetailAttribute(
                        Icons.Default.Yard,
                        stringResource(Res.string.home_plant_pot),
                        when(it) {
                            PotSize.SMALL -> stringResource(Res.string.pot_small)
                            PotSize.MEDIUM -> stringResource(Res.string.pot_medium)
                            PotSize.LARGE -> stringResource(Res.string.pot_large)
                            PotSize.EXTRA_LARGE -> stringResource(Res.string.pot_extra_large)
                        }
                    )
                }
            }
        }

        // Description Section
        plant.description?.takeIf { it.isNotBlank() }?.let {
            Column {
                Text(
                    text = stringResource(Res.string.home_plant_description),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Next Care Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(Res.string.plant_detail_next_care),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (nextEvent != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val icon = when (nextEvent) {
                            is WaterCareEvent -> Icons.Default.WaterDrop
                            is FertilizeCareEvent -> Icons.Default.Science
                            is RepotCareEvent -> Icons.Default.HomeRepairService
                        }
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = when (nextEvent) {
                                    is WaterCareEvent -> stringResource(Res.string.care_type_water)
                                    is FertilizeCareEvent -> stringResource(Res.string.care_type_fertilize)
                                    is RepotCareEvent -> stringResource(Res.string.care_type_repot)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            val timeZone = TimeZone.currentSystemDefault()
                            val dateTime = nextEvent.scheduledAt.toLocalDateTime(timeZone)
                            Text(
                                text = "${dateTime.dayOfMonth}/${dateTime.monthNumber}/${dateTime.year} ${dateTime.hour}:${dateTime.minute.toString().padStart(2, '0')}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(Res.string.calendar_no_tasks),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailAttribute(icon: ImageVector, label: String, value: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
    }
}
