package org.mlanau.project.plant.application

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.TimeZone
import org.mlanau.project.notification.domain.port.NotificationScheduler
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.CareRuleId
import org.mlanau.project.plant.domain.model.CareTask
import org.mlanau.project.plant.domain.model.CareType
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.port.CareRuleRepository
import org.mlanau.project.plant.domain.port.CareTaskRepository
import org.mlanau.project.plant.domain.port.PlantRepository
import org.mlanau.project.plant.domain.service.CareScheduler
import org.mlanau.project.shared.time.SystemTimeZoneProvider
import org.mlanau.project.shared.time.TimeZoneProvider

/**
 * Reconciles the platform's scheduled reminders with what the current rules, care history and
 * plants require, instead of relying on every write to remember to call [SyncCareReminder]. Any
 * change to a rule, a logged care, or a plant's name flows through the same [combine] and comes
 * out the other side as the same desired state, so nothing can drift out of sync by a call site
 * forgetting to re-sync.
 */
class CareReminderSync(
    private val careRuleRepository: CareRuleRepository,
    private val careTaskRepository: CareTaskRepository,
    private val plantRepository: PlantRepository,
    private val scheduler: CareScheduler,
    private val notifications: NotificationScheduler,
    private val clock: Clock = Clock.System,
    private val timeZoneProvider: TimeZoneProvider = SystemTimeZoneProvider
) {
    // What this instance last scheduled, by rule. Without it, an unrelated change elsewhere in the
    // combined state (editing a plant's description, say) would re-emit and reschedule every alarm
    // in the app on an identical instant.
    private val scheduledAt = mutableMapOf<CareRuleId, Instant>()

    fun start(scope: CoroutineScope) {
        combine(
            careRuleRepository.observeAll(),
            careTaskRepository.observeLastCareDates(),
            plantRepository.observeAll()
        ) { rules, lastCareDates, plants -> Triple(rules, lastCareDates, plants) }
            .onEach { (rules, lastCareDates, plants) -> reconcile(rules, lastCareDates, plants) }
            .launchIn(scope)
    }

    // A single reconciliation pass with no prior in-memory state to diff against, for platform
    // entry points that run once and exit (BootReceiver) rather than staying subscribed.
    suspend fun reconcileNow() {
        reconcile(
            rules = careRuleRepository.observeAll().first(),
            lastCareDates = careTaskRepository.observeLastCareDates().first(),
            plants = plantRepository.observeAll().first()
        )
    }

    private suspend fun reconcile(
        rules: List<CareRule>,
        lastCareDates: Map<PlantId, Map<CareType, Instant>>,
        plants: List<Plant>
    ) {
        val plantNames = plants.associate { it.id to it.name }
        val now = clock.now()
        val timeZone = timeZoneProvider()

        val desired = rules.mapNotNull { rule -> plannedReminder(rule, plantNames, lastCareDates, now, timeZone) }
            .associateBy { it.ruleId }

        (scheduledAt.keys - desired.keys).forEach { ruleId ->
            notifications.cancel(careReminderNotificationId(ruleId))
        }

        for (reminder in desired.values) {
            if (scheduledAt[reminder.ruleId] == reminder.at) continue
            notifications.schedule(
                careReminderNotification(reminder.task, reminder.plantName, reminder.at, timeZone)
            )
        }

        scheduledAt.clear()
        desired.values.forEach { scheduledAt[it.ruleId] = it.at }
    }

    private fun plannedReminder(
        rule: CareRule,
        plantNames: Map<PlantId?, String>,
        lastCareDates: Map<PlantId, Map<CareType, Instant>>,
        now: Instant,
        timeZone: TimeZone
    ): PlannedReminder? {
        val ruleId = rule.id ?: return null
        if (!rule.notificationsEnabled) return null
        val plantId = rule.plantId ?: return null
        val plantName = plantNames[plantId] ?: return null
        val lastCareAt = lastCareDates[plantId]?.get(rule.type)
        // nextPending decides *what* to say (due, or overdue and how far behind); nextReminderAt
        // decides *when*, and is the only source of an alarm instant because it alone guarantees a
        // strictly-future one. An overdue task's own instant is in the past, and an alarm set in
        // the past fires at once — then its receiver schedules "the next one" the same way, and
        // the loop never ends.
        val task = scheduler.nextPending(rule, lastCareAt, now, timeZone) ?: return null
        val at = scheduler.nextReminderAt(rule, lastCareAt, now, timeZone) ?: return null
        return PlannedReminder(ruleId, task, plantName, at)
    }

    private data class PlannedReminder(
        val ruleId: CareRuleId,
        val task: CareTask.Pending,
        val plantName: String,
        val at: Instant
    )
}
