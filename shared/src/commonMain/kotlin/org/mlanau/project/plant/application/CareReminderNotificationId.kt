package org.mlanau.project.plant.application

import org.mlanau.project.notification.domain.port.NotificationId
import org.mlanau.project.plant.domain.model.CareRuleId

internal fun careReminderNotificationId(ruleId: CareRuleId): NotificationId =
    NotificationId("care-rule-${ruleId.value}")
