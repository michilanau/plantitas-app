package org.mlanau.project.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.mlanau.project.plant.application.RescheduleAllCareReminders

class BootReceiver : BroadcastReceiver(), KoinComponent {

    private val rescheduleAllCareReminders: RescheduleAllCareReminders by inject()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scope.launch {
                rescheduleAllCareReminders()
            }
        }
    }
}
