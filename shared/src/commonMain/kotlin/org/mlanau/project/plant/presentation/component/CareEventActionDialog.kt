package org.mlanau.project.plant.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.plant.domain.model.CareEvent
import org.mlanau.project.plant.domain.model.CareEventStatus
import plantitas_app.shared.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareEventActionDialog(
    event: CareEvent,
    onDismissRequest: () -> Unit,
    onToggleStatus: (CareEvent) -> Unit,
    onSkip: (CareEvent) -> Unit,
    onReschedule: (CareEvent) -> Unit,
    onDelete: (CareEvent) -> Unit,
    onResetStatus: (CareEvent) -> Unit,
    onViewPlant: ((Int) -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(Res.string.care_rules_title)) },
        text = {
            Column {
                if (onViewPlant != null) {
                    ListItem(
                        headlineContent = { Text(stringResource(Res.string.care_action_view_plant)) },
                        leadingContent = { Icon(Icons.Default.Visibility, contentDescription = null) },
                        modifier = Modifier.clickable {
                            onViewPlant(event.plantId)
                            onDismissRequest()
                        }
                    )
                }
                
                if (event.status == CareEventStatus.PENDING) {
                    ListItem(
                        headlineContent = { Text(stringResource(Res.string.care_action_done)) },
                        leadingContent = { Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50)) },
                        modifier = Modifier.clickable {
                            onToggleStatus(event)
                            onDismissRequest()
                        }
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(Res.string.care_action_skip)) },
                        leadingContent = { Icon(Icons.Default.SkipNext, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                        modifier = Modifier.clickable {
                            onSkip(event)
                            onDismissRequest()
                        }
                    )
                } else {
                    ListItem(
                        headlineContent = { Text(stringResource(Res.string.care_action_reset)) },
                        leadingContent = { Icon(Icons.Default.Refresh, contentDescription = null) },
                        modifier = Modifier.clickable {
                            onResetStatus(event)
                            onDismissRequest()
                        }
                    )
                }

                ListItem(
                    headlineContent = { Text(stringResource(Res.string.care_action_reschedule)) },
                    leadingContent = { Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable {
                        onReschedule(event)
                        onDismissRequest()
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                ListItem(
                    headlineContent = { 
                        Text(
                            stringResource(Res.string.care_action_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable {
                        onDelete(event)
                        onDismissRequest()
                    }
                )
            }
        },
        confirmButton = {}
    )
}
