package org.mlanau.project.settings.infrastructure.persistence

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mlanau.project.settings.domain.ThemeMode
import org.mlanau.project.settings.domain.repository.SettingsRepository

@OptIn(ExperimentalSettingsApi::class)
class PersistentSettingsRepository(
    private val settings: ObservableSettings
) : SettingsRepository {

    private val flowSettings = settings.toFlowSettings()

    override fun themeMode(): Flow<ThemeMode> =
        flowSettings.getStringFlow(KEY_THEME, migratedThemeDefault().name).map { stored ->
            runCatching { ThemeMode.valueOf(stored) }.getOrDefault(ThemeMode.SYSTEM)
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        settings.putString(KEY_THEME, mode.name)
    }

    override fun getLanguage(): Flow<String> =
        flowSettings.getStringFlow(KEY_LANGUAGE, "es")

    override suspend fun setLanguage(languageCode: String) {
        settings.putString(KEY_LANGUAGE, languageCode)
    }

    /**
     * Users upgrading from the boolean `dark_mode` setting keep the look they picked: a stored
     * `true` becomes [ThemeMode.DARK], `false` becomes [ThemeMode.LIGHT]. A fresh install has
     * neither key and falls through to [ThemeMode.SYSTEM].
     */
    private fun migratedThemeDefault(): ThemeMode = when {
        !settings.hasKey(KEY_DARK_MODE) -> ThemeMode.SYSTEM
        settings.getBoolean(KEY_DARK_MODE, false) -> ThemeMode.DARK
        else -> ThemeMode.LIGHT
    }

    companion object {
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_LANGUAGE = "language"
    }
}
