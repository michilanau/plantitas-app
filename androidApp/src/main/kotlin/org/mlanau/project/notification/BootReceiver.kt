package org.mlanau.project.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.mlanau.project.plant.domain.repository.CareRepository
import org.mlanau.project.plant.domain.repository.PlantRepository
import org.mlanau.project.shared.notification.NotificationService

class BootReceiver : BroadcastReceiver(), KoinComponent {

    private val careRepository: CareRepository by inject()
    private val plantRepository: PlantRepository by inject()
    private val notificationService: NotificationService by inject()
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scope.launch {
                val allRules = careRepository.getAllCareRules().first()
                val allPlants = plantRepository.findAll().first()
                
                allRules.forEach { rule ->
                    if (rule.active && rule.notificationsEnabled) {
                        val plant = allPlants.find { it.id == rule.plantId }
                        plant?.let {
                            notificationService.scheduleNextNotification(rule, it.name)
                        }
                    }
                }
            }
        }
    }
}
