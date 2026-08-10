package org.mlanau.project.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlin.time.Clock
import kotlinx.datetime.*
import org.mlanau.project.plant.application.GenerateCareEvents
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.shared.notification.NotificationService

class AndroidNotificationService(
    private val context: Context,
    private val generateCareEvents: GenerateCareEvents
) : NotificationService {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun scheduleNextNotification(rule: CareRule, plantName: String) {
        if (!rule.active || !rule.notificationsEnabled) {
            cancelNotifications(rule)
            return
        }

        val now = Clock.System.now()
        val timeZone = TimeZone.currentSystemDefault()
        
        // We look ahead a year to find the next event
        val until = now.plus(365, DateTimeUnit.DAY, timeZone)
        
        // Use GenerateCareEvents to find the next virtual event
        val events = generateCareEvents.generate(listOf(rule), emptyList(), now, until)
        val nextEvent = events.firstOrNull { it.scheduledAt > now }

        if (nextEvent != null) {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("ruleId", rule.id)
                putExtra("plantName", plantName)
                putExtra("careType", rule::class.simpleName)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                rule.id ?: 0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerAtMillis = nextEvent.scheduledAt.toEpochMilliseconds()

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
    }

    override fun cancelNotifications(rule: CareRule) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            rule.id ?: 0,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
