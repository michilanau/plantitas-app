package org.mlanau.project.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import org.mlanau.project.MainActivity
import org.mlanau.project.R

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val ruleId = intent.getIntExtra("ruleId", -1)
        val plantName = intent.getStringExtra("plantName") ?: "Planta"
        val careType = intent.getStringExtra("careType") ?: "Cuidado"

        showNotification(context, ruleId, plantName, careType)
    }

    private fun showNotification(context: Context, ruleId: Int, plantName: String, careType: String) {
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

        val contentText = when (careType) {
            "WaterCareRule" -> context.getString(R.string.notification_water, plantName)
            "FertilizeCareRule" -> context.getString(R.string.notification_fertilize, plantName)
            "RepotCareRule" -> context.getString(R.string.notification_repot, plantName)
            else -> context.getString(R.string.notification_generic, plantName)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(ruleId, notification)
    }
}
