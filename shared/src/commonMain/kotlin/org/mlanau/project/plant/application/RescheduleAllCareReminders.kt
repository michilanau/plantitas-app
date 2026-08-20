package org.mlanau.project.plant.application

import kotlinx.coroutines.flow.first
import org.mlanau.project.plant.domain.repository.CareRepository

/**
 * Re-schedules every rule's reminder. The shared catch-up path for the two moments a platform
 * can't rely on a chained alarm alone:
 * - Android after a device reboot, where [org.mlanau.project.notification.BootReceiver] triggers it
 *   because every `AlarmManager` alarm is cleared on boot.
 * - App startup on both platforms, alongside [MigrateBase64PlantImages] in `App`, so a rule edited
 *   or left untouched while the app was closed is caught up rather than waiting for its next own
 *   mutation. This is also iOS's only rescheduling path between mutations, since its platform
 *   adapter has no receiver hook to chain the next alarm the moment one fires.
 */
class RescheduleAllCareReminders(
    private val careRepository: CareRepository,
    private val rescheduleCareReminder: RescheduleCareReminder
) {
    suspend operator fun invoke() {
        careRepository.getAllCareRules().first().forEach { rule ->
            rule.id?.let { rescheduleCareReminder(it) }
        }
    }
}
