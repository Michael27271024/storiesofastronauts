package com.example.astronautstories.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.astronautstories.domain.model.GraphicsQuality
import com.example.astronautstories.localization.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "fleet_settings")

data class AppSettings(
    val language: AppLanguage = AppLanguage.PERSIAN,
    val isDarkTheme: Boolean = true,
    val musicVolume: Float = 0.55f,
    val sfxVolume: Float = 0.80f,
    val graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH,
    val hapticsEnabled: Boolean = true,
    val accessibilityMode: Boolean = false,
    val reduceMotion: Boolean = false,
    val leftHandedControls: Boolean = false,
    val remindersEnabled: Boolean = false,
    val playerDisplayName: String = "فرمانده کوروش"
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val MUSIC_VOLUME = floatPreferencesKey("music_volume")
        val SFX_VOLUME = floatPreferencesKey("sfx_volume")
        val GRAPHICS_QUALITY = stringPreferencesKey("graphics_quality")
        val HAPTICS = booleanPreferencesKey("haptics")
        val ACCESSIBILITY = booleanPreferencesKey("accessibility")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val LEFT_HANDED = booleanPreferencesKey("left_handed")
        val REMINDERS = booleanPreferencesKey("reminders")
        val PLAYER_NAME = stringPreferencesKey("player_name")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE] ?: AppLanguage.PERSIAN.code),
            isDarkTheme = prefs[Keys.DARK_THEME] ?: true,
            musicVolume = prefs[Keys.MUSIC_VOLUME] ?: 0.55f,
            sfxVolume = prefs[Keys.SFX_VOLUME] ?: 0.80f,
            graphicsQuality = GraphicsQuality.entries.firstOrNull {
                it.name == (prefs[Keys.GRAPHICS_QUALITY] ?: GraphicsQuality.HIGH.name)
            } ?: GraphicsQuality.HIGH,
            hapticsEnabled = prefs[Keys.HAPTICS] ?: true,
            accessibilityMode = prefs[Keys.ACCESSIBILITY] ?: false,
            reduceMotion = prefs[Keys.REDUCE_MOTION] ?: false,
            leftHandedControls = prefs[Keys.LEFT_HANDED] ?: false,
            remindersEnabled = prefs[Keys.REMINDERS] ?: false,
            playerDisplayName = prefs[Keys.PLAYER_NAME] ?: "فرمانده کوروش"
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.settingsDataStore.edit { it[Keys.DARK_THEME] = isDark }
    }

    suspend fun setMusicVolume(volume: Float) {
        context.settingsDataStore.edit { it[Keys.MUSIC_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setSfxVolume(volume: Float) {
        context.settingsDataStore.edit { it[Keys.SFX_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setGraphicsQuality(quality: GraphicsQuality) {
        context.settingsDataStore.edit { it[Keys.GRAPHICS_QUALITY] = quality.name }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.HAPTICS] = enabled }
    }

    suspend fun setAccessibilityMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.ACCESSIBILITY] = enabled }
    }

    suspend fun setReduceMotion(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.REDUCE_MOTION] = enabled }
    }

    suspend fun setLeftHandedControls(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.LEFT_HANDED] = enabled }
    }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.REMINDERS] = enabled }
    }

    suspend fun setPlayerDisplayName(name: String) {
        context.settingsDataStore.edit { it[Keys.PLAYER_NAME] = name.take(40).ifBlank { "فرمانده" } }
    }
}
