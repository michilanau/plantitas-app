package org.mlanau.project.shared.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.mlanau.project.shared.ui.theme.ScreenGutter
import plantitas_app.shared.generated.resources.Res
import plantitas_app.shared.generated.resources.ic_back

/**
 * The top of every screen. Two shapes from one component:
 *  - a landing header (no [onNavigateBack]): a small caps [eyebrow] over a large [title], with
 *    [actions] to its right;
 *  - a sub-screen header (with [onNavigateBack]): a round back button, the [title] centred (or
 *    none, when the content below already names the screen), then [actions].
 */
@Composable
fun ScreenHeader(
    title: String?,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    backIcon: DrawableResource = Res.drawable.ic_back,
    backContentDescription: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    if (onNavigateBack != null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenGutter, vertical = 12.dp)
                .heightIn(min = 46.dp)
        ) {
            RoundIconButton(
                icon = backIcon,
                contentDescription = backContentDescription,
                onClick = onNavigateBack,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 96.dp)
                )
            }
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = ScreenGutter, end = ScreenGutter, top = 22.dp, bottom = 14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (eyebrow != null) {
                    Text(
                        text = eyebrow.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (title != null) {
                    Text(text = title, style = MaterialTheme.typography.headlineLarge)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
    }
}
