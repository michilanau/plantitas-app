package org.mlanau.project.shared.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The small caps olive heading that opens each block of content on a screen ("Próximo cuidado",
 * "Cuidados", "Historial", "Apariencia"). Was an ad-hoc `Text` with `titleMedium` + primary colour
 * repeated across screens.
 */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}
