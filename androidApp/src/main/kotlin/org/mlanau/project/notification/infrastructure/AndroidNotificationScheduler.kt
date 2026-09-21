package org.mlanau.project.notification.infrastructure

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.jetbrains.compose.resources.getString
import org.mlanau.project.notification.NotificationReceiver
import org.mlanau.project.notification.domain.port.NotificationId
import org.mlanau.project.notification.domain.port.NotificationScheduler
import org.mlanau.project.notification.domain.port.ScheduledNotification
import plantitas_app.shared.generated.resources.Res
import plantitas_app.shared.generated.resources.notification_channel_description
import plantitas_app.shared.generated.resources.notification_channel_name

/**
 * Stays in `:androidApp` rather than `shared/androidMain` alongside its iOS counterpart:
 * [NotificationReceiver] — the `BroadcastReceiver` it targets — needs the app's own `R` class (its
 * notification icon) and [org.mlanau.project.MainActivity], both of which only exist once the
 * final app module is assembled. This class knows nothing about care rules or plants — the title
 * and body already arrive fully resolved in [ScheduledNotification] — it only turns them into an
 * `AlarmManager` call.
 */
class AndroidNotificationScheduler(
    private val context: Context
) : NotificationScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // Android wakes NotificationReceiver as each alarm fires, and it arms the next one from there,
    // so only the head of the series is ever registered: the rest of the run would be rewritten by
    // that receiver anyway, and setting it would just fill AlarmManager with alarms to overwrite.
    override fun seriesLengthFor(activeRuleCount: Int): Int = 1

    override suspend fun schedule(series: List<ScheduledNotification>) {
        val notification = series.firstOrNull() ?: return

        // Created (and its name/description refreshed to the current app language) here rather than
        // in the receiver: this method is suspending, so it can resolve the localized channel copy,
        // and it always runs before the receiver that would post into the channel.
        ensureChannel()

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("notificationId", notification.id.value)
            putExtra("title", notification.title)
            putExtra("body", notification.body)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode(notification.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = notification.at.toEpochMilliseconds()

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

    // Only index 0 is ever scheduled here, but the whole range is swept so the contract holds for
    // an install that has carried alarms over from a build scheduling series of its own.
    override fun cancel(id: NotificationId) {
        repeat(NotificationScheduler.MAX_SERIES_LENGTH) { index ->
            cancelOne(id.inSeries(index))
        }
    }

    private fun cancelOne(id: NotificationId) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode(id),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private suspend fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CARE_CHANNEL_ID,
            getString(Res.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(Res.string.notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    private fun requestCode(id: NotificationId): Int = id.value.hashCode()

    companion object {
        const val CARE_CHANNEL_ID = "plant_care_notifications"
    }
}
