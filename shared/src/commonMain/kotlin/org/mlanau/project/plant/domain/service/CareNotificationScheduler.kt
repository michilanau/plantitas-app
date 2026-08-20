package org.mlanau.project.plant.domain.service

import kotlin.time.Instant
import org.mlanau.project.plant.domain.model.CareRuleId
import org.mlanau.project.plant.domain.model.CareType
import org.mlanau.project.plant.domain.model.PlantId

/**
 * Everything a platform adapter needs to put up a single reminder. The DECISION of when and with
 * what figure to remind is made entirely by [org.mlanau.project.plant.application.RescheduleCareReminder]
 * — the only application-layer code with access to the logged-care anchor — so an adapter never
 * touches [CareOccurrenceScheduler] itself; it only resolves platform strings and calls the native
 * scheduling API.
 */
data class CareReminderRequest(
    val ruleId: CareRuleId,
    val plantId: PlantId,
    val plantName: String,
    val type: CareType,
    val at: Instant,
    /** Days between the last logged care and [at], computed once at schedule time rather than
     * when the reminder fires — both instants are already fixed, so this doesn't change between
     * scheduling and firing. `null` means there's no logged care yet for this (plant, type): the
     * adapter falls back to a "first time" message instead of "N days without care". */
    val daysSinceLastCareAtFireTime: Int?
)

/**
 * A port for scheduling the platform reminder for a care rule's next occurrence. Lives in the
 * `plant` domain (not a generic `shared` package) because it's already shaped around domain types
 * rather than being a general-purpose notification API.
 */
interface CareNotificationScheduler {
    /** Schedules (replacing any previous one) the reminder described by [request]. Suspends
     * because the notification text is resolved — in the user's chosen app language — at
     * scheduling time rather than when the notification is actually shown; on Android that keeps
     * the display-time `BroadcastReceiver` fully synchronous instead of needing `goAsync()` for a
     * suspending resource lookup. */
    suspend fun schedule(request: CareReminderRequest)

    /** Cancels any pending reminder for a care rule. */
    fun cancel(ruleId: CareRuleId)
}
