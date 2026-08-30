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
import org.mlanau.project.plant.application.CareReminderSync

class BootReceiver : BroadcastReceiver(), KoinComponent {

    private val careReminderSync: CareReminderSync by inject()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // AlarmManager wipes every alarm on reboot, and with the app closed there's no
            // CareReminderSync.start() subscription alive to notice — so this one-shot pass is
            // what actually restores them.
            val pendingResult = goAsync()
            scope.launch {
                try {
                    careReminderSync.reconcileNow()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
