package org.mlanau.project.plant.infrastructure.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.jetbrains.compose.resources.getString
import org.mlanau.project.notification.NotificationReceiver
import org.mlanau.project.plant.domain.model.CareRuleId
import org.mlanau.project.plant.domain.model.CareType
import org.mlanau.project.plant.domain.service.CareNotificationScheduler
import org.mlanau.project.plant.domain.service.CareReminderRequest
import plantitas_app.shared.generated.resources.Res
import plantitas_app.shared.generated.resources.notification_fertilize
import plantitas_app.shared.generated.resources.notification_fertilize_overdue
import plantitas_app.shared.generated.resources.notification_repot
import plantitas_app.shared.generated.resources.notification_repot_overdue
import plantitas_app.shared.generated.resources.notification_title
import plantitas_app.shared.generated.resources.notification_water
import plantitas_app.shared.generated.resources.notification_water_overdue

/**
 * This adapter stays in `:androidApp` rather than `shared/androidMain` alongside its iOS
 * counterpart: [NotificationReceiver] — the `BroadcastReceiver` it targets — needs the app's own
 * `R` class (its notification icon and strings) and [org.mlanau.project.MainActivity], both of
 * which only exist once the final app module is assembled. Unlike the previous version of this
 * adapter, the scheduling *decision* (when, and what figure to show) isn't made here at all — it
 * arrives already resolved in [CareReminderRequest] from
 * [org.mlanau.project.plant.application.RescheduleCareReminder]; this class only turns that into
 * platform strings and an `AlarmManager` call.
 */
class AndroidCareNotificationScheduler(
    private val context: Context
) : CareNotificationScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override suspend fun schedule(request: CareReminderRequest) {
        // Resolved here (a suspend context already, since this method is itself suspend) rather
        // than in NotificationReceiver at display time, which would otherwise have to do
        // suspending resource lookups from a BroadcastReceiver's onReceive.
        val title = getString(Res.string.notification_title)
        val body = bodyFor(request)

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("ruleId", request.ruleId.value)
            putExtra("title", title)
            putExtra("body", body)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            request.ruleId.value,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = request.at.toEpochMilliseconds()

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    override fun cancel(ruleId: CareRuleId) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ruleId.value,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private suspend fun bodyFor(request: CareReminderRequest): String {
        val days = request.daysSinceLastCareAtFireTime
        return if (days == null) {
            when (request.type) {
                CareType.WATER -> getString(Res.string.notification_water, request.plantName)
                CareType.FERTILIZE -> getString(Res.string.notification_fertilize, request.plantName)
                CareType.REPOT -> getString(Res.string.notification_repot, request.plantName)
            }
        } else {
            when (request.type) {
                CareType.WATER -> getString(Res.string.notification_water_overdue, request.plantName, days)
                CareType.FERTILIZE -> getString(Res.string.notification_fertilize_overdue, request.plantName, days)
                CareType.REPOT -> getString(Res.string.notification_repot_overdue, request.plantName, days)
            }
        }
    }
}
