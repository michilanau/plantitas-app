package org.mlanau.project.shared.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource
import plantitas_app.shared.generated.resources.Res
import plantitas_app.shared.generated.resources.ic_leaf

/**
 * A plant's picture as a rounded tile, or — when it has none — a leaf mark on a tinted ground.
 * The single fallback (rather than per-screen emoji) is why the app can carry a real [FontFamily]
 * without the placeholder losing its glyph.
 */
@Composable
fun PlantThumbnail(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    cornerRadius: Dp = 17.dp
) {
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(cornerRadius),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(cornerRadius)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(Res.drawable.ic_leaf),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.size(size * 0.42f)
                )
            }
        }
    }
}
