package org.mlanau.project.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.mlanau.project.MainActivity
import org.mlanau.project.R
import org.mlanau.project.plant.application.RescheduleCareReminder
import org.mlanau.project.plant.domain.model.CareRuleId

class NotificationReceiver : BroadcastReceiver(), KoinComponent {

    private val rescheduleCareReminder: RescheduleCareReminder by inject()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val ruleId = intent.getIntExtra("ruleId", -1)
        // Title/body are already fully resolved (in the user's language) by
        // AndroidCareNotificationScheduler when the alarm was scheduled — this receiver only
        // displays them, so it stays synchronous instead of needing a suspending resource lookup.
        val title = intent.getStringExtra("title") ?: return
        val body = intent.getStringExtra("body") ?: return

        showNotification(context, ruleId, title, body)

        // A one-shot AlarmManager alarm doesn't repeat on its own, so the rule's next reminder has
        // to be chained here, right after this one fires — otherwise a Periodic rule the user never
        // opens the app for would only ever notify once. This part does need DB access, so it runs
        // on goAsync() rather than blocking onReceive. RescheduleCareReminder always computes a
        // strictly-future instant (see its doc on the anti-loop guarantee), so this can never
        // re-trigger itself immediately.
        if (ruleId != -1) {
            val pendingResult = goAsync()
            scope.launch {
                try {
                    rescheduleCareReminder(CareRuleId(ruleId))
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun showNotification(context: Context, ruleId: Int, title: String, body: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "plant_care_notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recordatorios de Cuidado",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para el riego y cuidado de tus plantas"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Notifying with the ruleId as the id means a later notification for the same rule
        // replaces this one instead of stacking a new one in the tray.
        notificationManager.notify(ruleId, notification)
    }
}
