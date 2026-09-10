package org.mlanau.project.shared.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource
import org.mlanau.project.shared.ui.theme.PlantTintContent
import org.mlanau.project.shared.ui.theme.PlantTints
import plantitas_app.shared.generated.resources.Res
import plantitas_app.shared.generated.resources.ic_leaf
import plantitas_app.shared.generated.resources.ic_plant_blades
import plantitas_app.shared.generated.resources.ic_plant_cactus
import plantitas_app.shared.generated.resources.ic_plant_palm
import plantitas_app.shared.generated.resources.ic_plant_round

private val PlantSilhouettes = listOf(
    Res.drawable.ic_leaf,
    Res.drawable.ic_plant_blades,
    Res.drawable.ic_plant_round,
    Res.drawable.ic_plant_cactus,
    Res.drawable.ic_plant_palm
)

/**
 * A plant's photo, or — when it has none — a plant silhouette on a bright ground. [seed] (the
 * plant's id) picks the ground and the silhouette, so a plant keeps the same look everywhere.
 * [modifier] decides the size: a disc on a card, a full-width block as the detail hero.
 */
@Composable
fun PlantAvatar(
    imageUrl: String?,
    seed: Int,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    iconSize: Dp = 30.dp,
    onClick: (() -> Unit)? = null
) {
    val index = seed.mod(PlantTints.size)
    Box(
        modifier = modifier
            .clip(shape)
            .background(PlantTints[index])
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                painter = painterResource(PlantSilhouettes[index % PlantSilhouettes.size]),
                contentDescription = null,
                tint = PlantTintContent,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
