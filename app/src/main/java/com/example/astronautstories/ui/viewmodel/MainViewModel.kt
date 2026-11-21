package com.example.astronautstories.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.astronautstories.data.local.AchievementEntity
import com.example.astronautstories.data.local.AppSettings
import com.example.astronautstories.data.local.PlayerProgressEntity
import com.example.astronautstories.data.local.StageRecordEntity
import com.example.astronautstories.domain.model.GraphicsQuality
import com.example.astronautstories.domain.model.HeroId
import com.example.astronautstories.domain.model.ShopItemCatalog
import com.example.astronautstories.domain.model.SkillType
import com.example.astronautstories.localization.AppLanguage
import com.example.astronautstories.notifications.MissionReminderWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val container: AppContainer
) : ViewModel() {

    val settings: StateFlow<AppSettings> = container.settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val progress: StateFlow<PlayerProgressEntity> = container.gameRepository.progressFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerProgressEntity())

    val stageRecords: StateFlow<Map<Int, StageRecordEntity>> = container.gameRepository.stageRecordsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val ownedItemIds: StateFlow<Set<String>> = container.gameRepository.ownedItemIdsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), setOf("weapon_blaster"))

    val skillLevels: StateFlow<Map<String, Int>> = container.gameRepository.skillLevelsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val achievements: StateFlow<List<AchievementEntity>> = container.gameRepository.achievementsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            container.gameRepository.ensureInitialized()
            container.bitmapRegistry.preloadGameplaySprites()
        }
        viewModelScope.launch {
            container.settingsRepository.settingsFlow.collect { s ->
                container.audioEngine.updateVolumes(s.musicVolume, s.sfxVolume)
            }
        }
    }

    fun selectHero(heroId: HeroId) {
        viewModelScope.launch {
            val owned = ownedItemIds.value
            if (heroId.unlockCost == 0 || owned.contains("hero_${heroId.id}")) {
                container.gameRepository.selectHero(heroId)
                container.audioEngine.playSfx("select")
            } else {
                container.audioEngine.playSfx("locked")
            }
        }
    }

    fun purchaseOrEquip(item: ShopItemCatalog, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = container.gameRepository.purchaseOrEquipItem(item)
            container.audioEngine.playSfx(if (ok) "powerup" else "locked")
            onResult(ok)
        }
    }

    fun upgradeSkill(skill: SkillType, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = container.gameRepository.upgradeSkill(skill)
            container.audioEngine.playSfx(if (ok) "powerup" else "locked")
            onResult(ok)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            container.settingsRepository.setLanguage(language)
            container.audioEngine.playSfx("click")
        }
    }

    fun setDarkTheme(isDark: Boolean) {
        viewModelScope.launch {
            container.settingsRepository.setDarkTheme(isDark)
            container.audioEngine.playSfx("click")
        }
    }

    fun setMusicVolume(volume: Float) {
        viewModelScope.launch {
            container.settingsRepository.setMusicVolume(volume)
        }
    }

    fun setSfxVolume(volume: Float) {
        viewModelScope.launch {
            container.settingsRepository.setSfxVolume(volume)
        }
    }

    fun setGraphicsQuality(quality: GraphicsQuality) {
        viewModelScope.launch {
            container.settingsRepository.setGraphicsQuality(quality)
            container.audioEngine.playSfx("click")
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setHapticsEnabled(enabled) }
    }

    fun setAccessibilityMode(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setAccessibilityMode(enabled) }
    }

    fun setReduceMotion(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setReduceMotion(enabled) }
    }

    fun setLeftHandedControls(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setLeftHandedControls(enabled) }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            container.settingsRepository.setRemindersEnabled(enabled)
            MissionReminderWorker.scheduleDailyReminder(container.appContext, enabled)
        }
    }

    fun setDisplayName(name: String) {
        viewModelScope.launch {
            container.settingsRepository.setPlayerDisplayName(name)
        }
    }

    fun resetStoryProgress() {
        viewModelScope.launch {
            container.gameRepository.resetStoryProgress()
            container.audioEngine.playSfx("click")
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(container) as T
        }
    }
}
