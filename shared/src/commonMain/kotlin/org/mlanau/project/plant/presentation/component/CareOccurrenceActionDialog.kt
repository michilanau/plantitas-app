package org.mlanau.project.plant.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlin.time.Clock
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.plant.domain.service.CareOccurrence
import plantitas_app.shared.generated.resources.*

/**
 * The only three actions that make sense on a predicted occurrence — see the product discussion
 * that arrived at this set: mark done, dismiss ("not this time"), and view the plant. Unlike the
 * old CareEvent-based dialog, there's no reset and no delete: an occurrence has no state to reset
 * (it's not persisted at all), and "delete" was never well-defined for something that's re-derived
 * on every read.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareOccurrenceActionDialog(
    occurrence: CareOccurrence,
    onDismissRequest: () -> Unit,
    onMarkDone: (CareOccurrence) -> Unit,
    onDismissOccurrence: (CareOccurrence) -> Unit,
    onViewPlant: ((Int) -> Unit)? = null,
    clock: Clock = Clock.System
) {
    // A future occurrence can't be marked done — see the product requirement this enforces at the
    // domain level too, in CareLog.create.
    val canMarkDone = occurrence.scheduledAt <= clock.now()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(Res.string.care_occurrence_options_title)) },
        text = {
            Column {
                if (onViewPlant != null) {
                    ListItem(
                        headlineContent = { Text(stringResource(Res.string.care_action_view_plant)) },
                        leadingContent = { Icon(Icons.Default.Visibility, contentDescription = null) },
                        modifier = Modifier.clickable {
                            onViewPlant(occurrence.plantId.value)
                            onDismissRequest()
                        }
                    )
                }

                if (canMarkDone) {
                    ListItem(
                        headlineContent = { Text(stringResource(Res.string.care_action_done)) },
                        leadingContent = { Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50)) },
                        modifier = Modifier.clickable {
                            onMarkDone(occurrence)
                            onDismissRequest()
                        }
                    )
                }

                ListItem(
                    headlineContent = { Text(stringResource(Res.string.care_action_skip)) },
                    leadingContent = { Icon(Icons.Default.SkipNext, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                    modifier = Modifier.clickable {
                        onDismissOccurrence(occurrence)
                        onDismissRequest()
                    }
                )
            }
        },
        confirmButton = {}
    )
}
