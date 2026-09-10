package org.mlanau.project.plant.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.plant.domain.model.CareTask
import org.mlanau.project.shared.ui.LocalDateFormatter
import org.mlanau.project.shared.ui.component.AppCard
import org.mlanau.project.shared.ui.component.RoundIconButton
import plantitas_app.shared.generated.resources.Res
import plantitas_app.shared.generated.resources.care_action_undo
import plantitas_app.shared.generated.resources.ic_undo

/** One logged care in a plant's history, with the option to undo it. */
@Composable
fun CareHistoryRow(
    task: CareTask.Done,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = LocalDateFormatter.current
    AppCard(modifier = modifier) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(careColor(task.type)))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getCareTypeString(task.type),
                    style = MaterialTheme.typography.titleSmall,
                    textDecoration = TextDecoration.LineThrough,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateFormatter.formatDateTime(task.performedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                task.note?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            RoundIconButton(
                icon = Res.drawable.ic_undo,
                contentDescription = stringResource(Res.string.care_action_undo),
                onClick = onUndo,
                size = 40.dp,
                iconSize = 20.dp,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
