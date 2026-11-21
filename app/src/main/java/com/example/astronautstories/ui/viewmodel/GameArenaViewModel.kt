package com.example.astronautstories.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.astronautstories.domain.model.GameCatalog
import com.example.astronautstories.domain.model.GameMode
import com.example.astronautstories.domain.model.HeroId
import com.example.astronautstories.domain.model.SkillType
import com.example.astronautstories.domain.model.WeaponType
import com.example.astronautstories.engine.ArenaPhase
import com.example.astronautstories.engine.GameSimulation
import com.example.astronautstories.engine.HudSnapshot
import com.example.astronautstories.engine.InputControlsState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameArenaViewModel(
    private val container: AppContainer
) : ViewModel() {

    private var _simulation: GameSimulation? = null
    val simulation: GameSimulation? get() = _simulation

    private val _hud = MutableStateFlow(HudSnapshot())
    val hud: StateFlow<HudSnapshot> = _hud.asStateFlow()

    private val _frameTick = MutableStateFlow(0L)
    val frameTick: StateFlow<Long> = _frameTick.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _autoFire = MutableStateFlow(false)
    val autoFire: StateFlow<Boolean> = _autoFire.asStateFlow()

    private val _outcomeSaved = MutableStateFlow(false)
    val outcomeSaved: StateFlow<Boolean> = _outcomeSaved.asStateFlow()

    private var touchInput = InputControlsState()
    private var hardwareInput = InputControlsState()
    private var viewportWorldWidth = 1280f
    private var loopJob: Job? = null
    private var matchResultRecorded = false

    fun startSession(mode: GameMode, stageNumber: Int) {
        loopJob?.cancel()
        matchResultRecorded = false
        _outcomeSaved.value = false
        _isPaused.value = false
        _isReady.value = false

        viewModelScope.launch {
            container.gameRepository.ensureInitialized()
            val progress = container.gameRepository.progressFlow.first()
            val settings = container.settingsRepository.settingsFlow.first()
            val skillMapRaw = container.gameRepository.skillLevelsFlow.first()
            val skillLevels = SkillType.entries.associateWith { skillMapRaw[it.id] ?: 0 }

            val hero = HeroId.entries.firstOrNull { it.id == progress.selectedHeroId } ?: HeroId.KOUROSH
            val weapon = WeaponType.entries.firstOrNull { it.id == progress.equippedWeaponId } ?: WeaponType.STANDARD_BLASTER
            val stageDef = GameCatalog.getStage(stageNumber)

            val (bonusMedkits, bonusAmmo, bonusShield) = container.gameRepository.consumeMissionBoosters()

            val sim = GameSimulation(
                mode = mode,
                stageDef = stageDef,
                hero = hero,
                weapon = weapon,
                skillLevels = skillLevels,
                graphicsQuality = settings.graphicsQuality,
                bonusMedkits = bonusMedkits,
                bonusAmmo = bonusAmmo,
                bonusShield = bonusShield,
                onSfx = { key -> container.audioEngine.playSfx(key) }
            )
            _simulation = sim
            _hud.value = sim.buildSnapshot()
            _isReady.value = true

            container.audioEngine.onResumeLifecycle()
            container.audioEngine.playTheme(stageDef.musicKey, loop = true)
            runGameLoop()
        }
    }

    private fun runGameLoop() {
        loopJob?.cancel()
        loopJob = viewModelScope.launch {
            var lastNs = System.nanoTime()
            while (isActive) {
                delay(16L)
                val nowNs = System.nanoTime()
                val dt = ((nowNs - lastNs) / 1_000_000_000f).coerceIn(0.008f, 0.045f)
                lastNs = nowNs

                val sim = _simulation ?: continue
                if (!_isPaused.value && sim.phase == ArenaPhase.PLAYING) {
                    val combined = mergeInputs()
                    sim.step(dt, combined, viewportWorldWidth)

                    if (touchInput.specialTrigger) touchInput = touchInput.copy(specialTrigger = false)
                    if (hardwareInput.specialTrigger) hardwareInput = hardwareInput.copy(specialTrigger = false)
                }

                val snap = sim.buildSnapshot()
                _hud.value = snap
                _frameTick.value = _frameTick.value + 1L

                if ((snap.phase == ArenaPhase.VICTORY || snap.phase == ArenaPhase.GAME_OVER) && !matchResultRecorded) {
                    matchResultRecorded = true
                    persistMatchOutcome(sim, snap)
                }
            }
        }
    }

    private fun persistMatchOutcome(sim: GameSimulation, snap: HudSnapshot) {
        viewModelScope.launch {
            container.audioEngine.playTheme(if (snap.isVictory) "victory" else "game_over", loop = false)
            val settings = container.settingsRepository.settingsFlow.first()
            container.gameRepository.recordMatchResult(
                mode = sim.mode,
                stageNumber = sim.stageDef.stageNumber,
                won = snap.isVictory,
                stars = snap.starsEarned,
                score = snap.score,
                xpEarned = snap.xpEarned,
                crystalsEarned = snap.crystalsEarned,
                enemiesKilled = snap.enemiesKilled,
                bossKilled = snap.bossKilled,
                shotsFired = snap.shotsFired,
                shotsHit = snap.shotsHit,
                durationSeconds = snap.elapsedSeconds,
                timeRemaining = snap.timeRemainingSec,
                betrayalChosen = snap.betrayalChosen,
                playerDisplayName = settings.playerDisplayName
            )
            _outcomeSaved.value = true
        }
    }

    private fun mergeInputs(): InputControlsState = InputControlsState(
        left = touchInput.left || hardwareInput.left,
        right = touchInput.right || hardwareInput.right,
        jump = touchInput.jump || hardwareInput.jump,
        crouch = touchInput.crouch || hardwareInput.crouch,
        shoot = touchInput.shoot || hardwareInput.shoot || _autoFire.value,
        specialTrigger = touchInput.specialTrigger || hardwareInput.specialTrigger
    )

    fun toggleAutoFire() {
        _autoFire.value = !_autoFire.value
        container.audioEngine.playSfx("click")
    }

    fun setAudioMutedForCutscene(muted: Boolean) {
        if (muted) {
            container.audioEngine.onPauseLifecycle()
        } else {
            container.audioEngine.onResumeLifecycle()
        }
    }

    fun updateViewportSize(widthPx: Float, heightPx: Float) {
        val sim = _simulation ?: return
        if (widthPx > 1f && heightPx > 1f) {
            val scale = (heightPx / sim.worldHeight).coerceAtLeast(0.1f)
            viewportWorldWidth = (widthPx / scale).coerceAtLeast(960f)
        }
    }

    fun updateTouchInput(transform: (InputControlsState) -> InputControlsState) {
        touchInput = transform(touchInput)
    }

    fun updateHardwareInput(transform: (InputControlsState) -> InputControlsState) {
        hardwareInput = transform(hardwareInput)
    }

    fun triggerSpecial() {
        touchInput = touchInput.copy(specialTrigger = true)
    }

    fun startMissionFromBriefing() {
        _simulation?.startMission()
        _simulation?.let { _hud.value = it.buildSnapshot() }
        container.audioEngine.playSfx("click")
    }

    fun resolveStoryChoice(betrayal: Boolean) {
        _simulation?.resolveStoryChoice(betrayal)
        _simulation?.let { _hud.value = it.buildSnapshot() }
    }

    fun togglePause(paused: Boolean) {
        _isPaused.value = paused
        container.audioEngine.playSfx("click")
    }

    fun stopSession() {
        loopJob?.cancel()
        touchInput = InputControlsState()
        hardwareInput = InputControlsState()
    }

    override fun onCleared() {
        stopSession()
        super.onCleared()
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GameArenaViewModel(container) as T
        }
    }
}
