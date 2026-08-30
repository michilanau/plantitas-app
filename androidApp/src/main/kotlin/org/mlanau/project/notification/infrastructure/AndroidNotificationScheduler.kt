package org.mlanau.project.notification.infrastructure

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.mlanau.project.notification.NotificationReceiver
import org.mlanau.project.notification.domain.port.NotificationId
import org.mlanau.project.notification.domain.port.NotificationScheduler
import org.mlanau.project.notification.domain.port.ScheduledNotification

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

    override suspend fun schedule(notification: ScheduledNotification) {
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

    override fun cancel(id: NotificationId) {
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

    private fun requestCode(id: NotificationId): Int = id.value.hashCode()
}
