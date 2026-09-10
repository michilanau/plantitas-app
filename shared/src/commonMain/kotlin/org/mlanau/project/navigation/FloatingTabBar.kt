package org.mlanau.project.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.shared.ui.theme.ContentMaxWidth
import org.mlanau.project.shared.ui.theme.InlineIconSize
import org.mlanau.project.shared.ui.theme.ScreenGutter
import plantitas_app.shared.generated.resources.Res
import plantitas_app.shared.generated.resources.home_add_plant_description
import plantitas_app.shared.generated.resources.ic_calendar
import plantitas_app.shared.generated.resources.ic_leaf
import plantitas_app.shared.generated.resources.ic_plus
import plantitas_app.shared.generated.resources.nav_calendar
import plantitas_app.shared.generated.resources.nav_plants

/**
 * The pill that floats over the two tab screens: Plants and Calendar either side of the one action
 * the app is organised around, adding a plant. Screens under it leave
 * [org.mlanau.project.shared.ui.theme.TabBarClearance] at the bottom so nothing hides behind it.
 */
@Composable
fun FloatingTabBar(
    isPlantsSelected: Boolean,
    isCalendarSelected: Boolean,
    onPlantsClick: () -> Unit,
    onAddPlantClick: () -> Unit,
    onCalendarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .navigationBarsPadding()
            .padding(start = ScreenGutter, end = ScreenGutter, bottom = 20.dp)
            .widthIn(max = ContentMaxWidth)
            .fillMaxWidth()
            .height(66.dp)
            .clip(CircleShape)
            .background(colors.inverseSurface)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TabItem(
            icon = Res.drawable.ic_leaf,
            label = stringResource(Res.string.nav_plants),
            selected = isPlantsSelected,
            onClick = onPlantsClick,
            modifier = Modifier.weight(1f)
        )
        Surface(
            onClick = onAddPlantClick,
            shape = CircleShape,
            color = colors.primaryContainer,
            contentColor = colors.onPrimaryContainer,
            modifier = Modifier.size(50.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(Res.drawable.ic_plus),
                    contentDescription = stringResource(Res.string.home_add_plant_description),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        TabItem(
            icon = Res.drawable.ic_calendar,
            label = stringResource(Res.string.nav_calendar),
            selected = isCalendarSelected,
            onClick = onCalendarClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TabItem(
    icon: DrawableResource,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = CircleShape,
        color = if (selected) colors.primaryContainer else Color.Transparent,
        contentColor = if (selected) colors.onPrimaryContainer else colors.inverseOnSurface
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(InlineIconSize))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp),
                maxLines = 1
            )
        }
    }
}
