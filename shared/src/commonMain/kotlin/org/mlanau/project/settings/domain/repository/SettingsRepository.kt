package org.mlanau.project.settings.domain.repository

import kotlinx.coroutines.flow.Flow
import org.mlanau.project.settings.domain.ThemeMode

interface SettingsRepository {
    fun themeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)

    fun getLanguage(): Flow<String>
    suspend fun setLanguage(languageCode: String)
}
