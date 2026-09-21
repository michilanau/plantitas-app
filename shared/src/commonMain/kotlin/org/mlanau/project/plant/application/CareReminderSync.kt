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
import org.mlanau.project.plant.domain.model.CareReminder
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.CareRuleId
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
    // in the app on an identical run of instants.
    private val scheduledAt = mutableMapOf<CareRuleId, List<Instant>>()

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

        // How long a series each rule gets is a share of the platform's budget, so how many rules
        // want reminders at all has to be settled before any of them is planned. This first pass
        // only applies the cheap eligibility checks; the scheduling maths runs once, below.
        val eligible = rules.mapNotNull { rule -> eligibleRule(rule, plantNames) }
        val seriesLength = notifications.seriesLengthFor(eligible.size)

        val desired = eligible
            .mapNotNull { candidate -> plannedReminders(candidate, lastCareDates, now, timeZone, seriesLength) }
            .associateBy { it.ruleId }

        (scheduledAt.keys - desired.keys).forEach { ruleId ->
            notifications.cancel(careReminderNotificationId(ruleId))
        }

        for (planned in desired.values) {
            if (scheduledAt[planned.ruleId] == planned.instants) continue
            notifications.schedule(careReminderNotifications(planned.series, planned.plantName, timeZone))
        }

        scheduledAt.clear()
        desired.values.forEach { scheduledAt[it.ruleId] = it.instants }
    }

    private fun eligibleRule(rule: CareRule, plantNames: Map<PlantId?, String>): EligibleRule? {
        val ruleId = rule.id ?: return null
        if (!rule.notificationsEnabled) return null
        val plantId = rule.plantId ?: return null
        val plantName = plantNames[plantId] ?: return null
        return EligibleRule(rule, ruleId, plantId, plantName)
    }

    private fun plannedReminders(
        candidate: EligibleRule,
        lastCareDates: Map<PlantId, Map<CareType, Instant>>,
        now: Instant,
        timeZone: TimeZone,
        seriesLength: Int
    ): PlannedReminders? {
        val lastCareAt = lastCareDates[candidate.plantId]?.get(candidate.rule.type)
        // Every instant in the series comes from CareScheduler, which alone guarantees strictly
        // future ones. An overdue task's own instant is in the past, and an alarm set in the past
        // fires at once — then its receiver schedules "the next one" the same way, and the loop
        // never ends.
        val series = scheduler.reminderSeries(candidate.rule, lastCareAt, now, seriesLength, timeZone)
        if (series.isEmpty()) return null
        return PlannedReminders(candidate.ruleId, series, candidate.plantName)
    }

    private data class EligibleRule(
        val rule: CareRule,
        val ruleId: CareRuleId,
        val plantId: PlantId,
        val plantName: String
    )

    private data class PlannedReminders(
        val ruleId: CareRuleId,
        val series: List<CareReminder>,
        val plantName: String
    ) {
        val instants: List<Instant> = series.map { it.at }
    }
}
