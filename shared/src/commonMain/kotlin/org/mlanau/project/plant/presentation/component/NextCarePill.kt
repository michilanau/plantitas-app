package org.mlanau.project.plant.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.plant.domain.model.CareTask
import org.mlanau.project.shared.ui.theme.MetaIconSize
import plantitas_app.shared.generated.resources.Res
import plantitas_app.shared.generated.resources.plant_detail_no_upcoming_care

/**
 * A plant's next care on its card: "Regar · en 2 días". Its weight grows with urgency — tinted
 * while it is still ahead, solid in the care's colour on the day, solid raspberry once overdue.
 */
@Composable
fun NextCarePill(
    task: CareTask.Pending?,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val container = when {
        task == null -> colors.surfaceContainer
        task.isOverdue -> overdueColor()
        task.daysUntilDue() == 0 -> careColor(task.type)
        else -> careContainerColor(task.type)
    }
    val content = when {
        task == null -> colors.onSurfaceVariant
        task.isOverdue || task.daysUntilDue() == 0 -> onCareAccentColor()
        else -> careColor(task.type)
    }
    val text = if (task == null) {
        stringResource(Res.string.plant_detail_no_upcoming_care)
    } else {
        "${getCareActionString(task.type)} · ${dueLabel(task)}"
    }

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(container)
            .padding(start = if (task == null) 11.dp else 9.dp, end = 11.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (task != null) {
            Icon(
                painter = painterResource(task.type.icon),
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(MetaIconSize)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
