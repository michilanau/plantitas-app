package org.mlanau.project.plant.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.mlanau.project.plant.domain.model.CareType

/**
 * A kind of care's icon on its soft ground, leading every care rule, task and next-care block. An
 * [overdue] badge sits on an overdue card, which already carries the soft raspberry, so it stands
 * out on the card colour instead.
 */
@Composable
fun CareTypeBadge(
    type: CareType,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    overdue: Boolean = false
) {
    val content = if (overdue) overdueColor() else careColor(type)
    val container = if (overdue) MaterialTheme.colorScheme.surface else careContainerColor(type)
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(container),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(type.icon),
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(size * 0.48f)
        )
    }
}
