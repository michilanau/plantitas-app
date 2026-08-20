package org.mlanau.project.plant.application.fixtures

import org.mlanau.project.plant.domain.model.CareRuleId
import org.mlanau.project.plant.domain.service.CareNotificationScheduler
import org.mlanau.project.plant.domain.service.CareReminderRequest

/** Records calls instead of touching any real platform notification API. */
class FakeCareNotificationScheduler : CareNotificationScheduler {
    val scheduledRequests = mutableListOf<CareReminderRequest>()
    val cancelledRuleIds = mutableListOf<CareRuleId>()

    override suspend fun schedule(request: CareReminderRequest) {
        scheduledRequests += request
    }

    override fun cancel(ruleId: CareRuleId) {
        cancelledRuleIds += ruleId
    }
}
