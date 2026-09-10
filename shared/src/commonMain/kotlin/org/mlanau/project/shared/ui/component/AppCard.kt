package org.mlanau.project.shared.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import org.mlanau.project.shared.ui.theme.LocalCareColors

/**
 * Every card and row in the app, borderless on the paper. [shape] follows the app's rule:
 * `shapes.large` for cards, `shapes.medium` (the default) for rows. [border] is only for a card
 * that needs to shout on top of its tone, such as a plant whose care is overdue.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    tone: AppCardTone = AppCardTone.Default,
    shape: Shape = MaterialTheme.shapes.medium,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val color = when (tone) {
        AppCardTone.Default -> MaterialTheme.colorScheme.surface
        AppCardTone.Alert -> LocalCareColors.current.overdueContainer
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = color,
        border = border,
        content = content
    )
}
