package org.mlanau.project.settings.infrastructure.repository

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import kotlinx.coroutines.flow.Flow
import org.mlanau.project.settings.domain.repository.SettingsRepository

@OptIn(ExperimentalSettingsApi::class)
class PersistentSettingsRepository(
    private val settings: ObservableSettings
) : SettingsRepository {

    private val flowSettings = settings.toFlowSettings()

    override fun isDarkMode(): Flow<Boolean> =
        flowSettings.getBooleanFlow(KEY_DARK_MODE, false)

    override suspend fun setDarkMode(enabled: Boolean) {
        settings.putBoolean(KEY_DARK_MODE, enabled)
    }

    override fun getLanguage(): Flow<String> =
        flowSettings.getStringFlow(KEY_LANGUAGE, "es")

    override suspend fun setLanguage(languageCode: String) {
        settings.putString(KEY_LANGUAGE, languageCode)
    }

    companion object {
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_LANGUAGE = "language"
    }
}
