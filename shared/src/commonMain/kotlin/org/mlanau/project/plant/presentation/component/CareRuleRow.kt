package org.mlanau.project.plant.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.shared.ui.LocalDateFormatter
import org.mlanau.project.shared.ui.component.AppCard
import org.mlanau.project.shared.ui.component.RoundIconButton
import org.mlanau.project.shared.ui.theme.RowIconSize
import plantitas_app.shared.generated.resources.Res
import plantitas_app.shared.generated.resources.care_recurrence_periodic
import plantitas_app.shared.generated.resources.care_rule_no_reminder
import plantitas_app.shared.generated.resources.common_delete
import plantitas_app.shared.generated.resources.ic_close
import plantitas_app.shared.generated.resources.ic_pencil

/**
 * One of a plant's care rules: "Riego · Cada 5 días · 09:00". Tapping it edits the rule; with
 * [onRemove] (the plant form) it also offers to drop it.
 */
@Composable
fun CareRuleRow(
    rule: CareRule,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null
) {
    val dateFormatter = LocalDateFormatter.current
    val recurrence = pluralStringResource(Res.plurals.care_recurrence_periodic, rule.everyDays, rule.everyDays)
    val noReminder = stringResource(Res.string.care_rule_no_reminder)
    val schedule = listOfNotNull(
        recurrence,
        dateFormatter.formatTime(rule.notificationTime),
        noReminder.takeUnless { rule.notificationsEnabled }
    ).joinToString(" · ")

    AppCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CareTypeBadge(rule.type)
            Column(modifier = Modifier.weight(1f)) {
                Text(getCareTypeString(rule.type), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = schedule,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onRemove != null) {
                RoundIconButton(
                    icon = Res.drawable.ic_close,
                    contentDescription = stringResource(Res.string.common_delete),
                    onClick = onRemove,
                    size = 40.dp,
                    iconSize = 20.dp,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Icon(
                    painter = painterResource(Res.drawable.ic_pencil),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 10.dp).size(RowIconSize)
                )
            }
        }
    }
}
