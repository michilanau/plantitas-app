package org.mlanau.project.shared.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.mlanau.project.shared.ui.theme.ActionButtonHeight
import org.mlanau.project.shared.ui.theme.InlineIconSize

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppButtonStyle = AppButtonStyle.Primary,
    icon: DrawableResource? = null,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val active = enabled && !loading
    val (container, content) = when (style) {
        AppButtonStyle.Primary -> colors.primary to colors.onPrimary
        AppButtonStyle.Accent -> colors.primaryContainer to colors.onPrimaryContainer
        AppButtonStyle.Outline -> Color.Transparent to colors.onSurface
        AppButtonStyle.Danger -> Color.Transparent to colors.error
    }
    val filled = style == AppButtonStyle.Primary || style == AppButtonStyle.Accent
    val border = when {
        filled -> null
        !active -> BorderStroke(2.dp, colors.outline)
        style == AppButtonStyle.Danger -> BorderStroke(2.dp, colors.error)
        else -> BorderStroke(2.dp, colors.outline)
    }

    Button(
        onClick = onClick,
        modifier = modifier.height(ActionButtonHeight),
        enabled = active,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = if (filled) colors.surfaceContainer else Color.Transparent,
            disabledContentColor = colors.onSurfaceVariant
        ),
        border = border,
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = colors.onSurfaceVariant,
                strokeWidth = 2.dp
            )
        } else {
            if (icon != null) {
                Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(InlineIconSize))
                Spacer(Modifier.width(9.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
