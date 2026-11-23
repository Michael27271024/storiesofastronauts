package com.example.astronautstories.ui.viewmodel

import android.util.Log
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
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicLong

class GameArenaViewModel(
    private val container: AppContainer
) : ViewModel() {

    private val _simulationState = MutableStateFlow<GameSimulation?>(null)
    val simulationState: StateFlow<GameSimulation?> = _simulationState.asStateFlow()
    val simulation: GameSimulation? get() = _simulationState.value

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

    private val _sessionId = MutableStateFlow(0L)
    val sessionId: StateFlow<Long> = _sessionId.asStateFlow()

    private val ownerTokenGen = AtomicLong(0L)
    @Volatile
    private var activeOwnerToken: Long = 0L

    private var touchInput = InputControlsState()
    private var hardwareInput = InputControlsState()
    private var viewportWorldWidth = 1280f
    private var startJob: Job? = null
    private var loopJob: Job? = null
    private var outcomeJob: Job? = null
    private var matchResultRecorded = false

    fun allocateOwnerToken(): Long = ownerTokenGen.incrementAndGet()

    fun startSession(mode: GameMode, stageNumber: Int, ownerToken: Long? = null) {
        if (ownerToken != null) {
            activeOwnerToken = ownerToken
        }

        startJob?.cancel()
        loopJob?.cancel()
        outcomeJob?.cancel()

        val clampedStage = stageNumber.coerceIn(1, GameCatalog.TOTAL_STAGES)
        val nextSessionId = _sessionId.value + 1L
        _sessionId.value = nextSessionId
        matchResultRecorded = false
        _outcomeSaved.value = false
        _isPaused.value = false
        _isReady.value = false
        _simulationState.value = null
        _hud.value = HudSnapshot(
            mode = mode,
            stageNumber = clampedStage,
            phase = ArenaPhase.BRIEFING
        )
        resetInputs()

        startJob = viewModelScope.launch {
            var hero = HeroId.KOUROSH
            var weapon = WeaponType.STANDARD_BLASTER
            var skillLevels = emptyMap<SkillType, Int>()
            var graphicsQuality = com.example.astronautstories.domain.model.GraphicsQuality.MEDIUM
            var bonusMedkits = 0
            var bonusAmmo = 0
            var bonusShield = false

            try {
                withTimeoutOrNull(2500L) {
                    container.gameRepository.ensureInitialized()
                    val progress = container.gameRepository.progressFlow.first()
                    val settings = container.settingsRepository.settingsFlow.first()
                    val skillMapRaw = container.gameRepository.skillLevelsFlow.first()
                    skillLevels = SkillType.entries.associateWith { skillMapRaw[it.id] ?: 0 }
                    hero = HeroId.entries.firstOrNull { it.id == progress.selectedHeroId } ?: HeroId.KOUROSH
                    weapon = WeaponType.entries.firstOrNull { it.id == progress.equippedWeaponId } ?: WeaponType.STANDARD_BLASTER
                    graphicsQuality = settings.graphicsQuality
                    val boosters = container.gameRepository.consumeMissionBoosters()
                    bonusMedkits = boosters.first
                    bonusAmmo = boosters.second
                    bonusShield = boosters.third
                }
            } catch (e: Exception) {
                Log.w("GameArenaVM", "Session init fallback due to storage error: ${e.message}")
            }

            if (!isActive || _sessionId.value != nextSessionId) return@launch

            val stageDef = GameCatalog.getStage(clampedStage)
            val sim = GameSimulation(
                mode = mode,
                stageDef = stageDef,
                hero = hero,
                weapon = weapon,
                skillLevels = skillLevels,
                graphicsQuality = graphicsQuality,
                bonusMedkits = bonusMedkits,
                bonusAmmo = bonusAmmo,
                bonusShield = bonusShield,
                onSfx = { key -> container.audioEngine.playSfx(key) }
            )
            _simulationState.value = sim
            _hud.value = sim.buildSnapshot()
            _isReady.value = true

            container.audioEngine.onResumeLifecycle()
            container.audioEngine.playTheme(stageDef.musicKey, loop = true)
            runGameLoop(nextSessionId)
        }
    }

    fun restartSession(mode: GameMode, stageNumber: Int, ownerToken: Long? = null) {
        if (ownerToken != null) {
            activeOwnerToken = ownerToken
        }

        val clampedStage = stageNumber.coerceIn(1, GameCatalog.TOTAL_STAGES)
        val currentSim = _simulationState.value
        if (currentSim == null || currentSim.mode != mode || currentSim.stageDef.stageNumber != clampedStage) {
            startSession(mode, clampedStage, ownerToken)
            return
        }

        startJob?.cancel()
        loopJob?.cancel()
        outcomeJob?.cancel()

        val nextSessionId = _sessionId.value + 1L
        _sessionId.value = nextSessionId
        matchResultRecorded = false
        _outcomeSaved.value = false
        _isPaused.value = false
        resetInputs()

        val stageDef = GameCatalog.getStage(clampedStage)
        val freshSim = GameSimulation(
            mode = mode,
            stageDef = stageDef,
            hero = currentSim.hero,
            weapon = currentSim.weapon,
            skillLevels = currentSim.skillLevels,
            graphicsQuality = currentSim.graphicsQuality,
            bonusMedkits = currentSim.bonusMedkits,
            bonusAmmo = currentSim.bonusAmmo,
            bonusShield = currentSim.bonusShield,
            onSfx = { key -> container.audioEngine.playSfx(key) }
        )

        _simulationState.value = freshSim
        _hud.value = freshSim.buildSnapshot()
        _frameTick.value = _frameTick.value + 1L
        _isReady.value = true

        container.audioEngine.playSfx("click")
        container.audioEngine.onResumeLifecycle()
        container.audioEngine.playTheme(stageDef.musicKey, loop = true)
        runGameLoop(nextSessionId)
    }

    private fun runGameLoop(activeSessionId: Long) {
        loopJob?.cancel()
        loopJob = viewModelScope.launch {
            var lastNs = System.nanoTime()
            while (isActive && _sessionId.value == activeSessionId) {
                delay(16L)
                if (_sessionId.value != activeSessionId) break

                val nowNs = System.nanoTime()
                val dt = ((nowNs - lastNs) / 1_000_000_000f).coerceIn(0.008f, 0.045f)
                lastNs = nowNs

                val sim = _simulationState.value ?: continue
                if (!_isPaused.value && sim.phase == ArenaPhase.PLAYING) {
                    val combined = mergeInputs()
                    try {
                        sim.step(dt, combined, viewportWorldWidth)
                    } catch (e: Throwable) {
                        Log.e("GameArenaVM", "Simulation step error", e)
                    }

                    if (touchInput.specialTrigger) touchInput = touchInput.copy(specialTrigger = false)
                    if (hardwareInput.specialTrigger) hardwareInput = hardwareInput.copy(specialTrigger = false)
                }

                val snap = try {
                    sim.buildSnapshot()
                } catch (e: Throwable) {
                    Log.e("GameArenaVM", "Snapshot build error", e)
                    HudSnapshot(mode = sim.mode, stageNumber = sim.stageDef.stageNumber)
                }
                _hud.value = snap
                _frameTick.value = _frameTick.value + 1L

                if ((snap.phase == ArenaPhase.VICTORY || snap.phase == ArenaPhase.GAME_OVER) && !matchResultRecorded) {
                    matchResultRecorded = true
                    resetInputs()
                    persistMatchOutcome(sim, snap, activeSessionId)
                    break
                }
            }
        }
    }

    private fun persistMatchOutcome(sim: GameSimulation, snap: HudSnapshot, activeSessionId: Long) {
        outcomeJob?.cancel()
        outcomeJob = viewModelScope.launch {
            try {
                try {
                    container.audioEngine.playTheme(if (snap.isVictory) "victory" else "game_over", loop = false)
                } catch (_: Throwable) {
                }
                withTimeoutOrNull(2200L) {
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
                }
            } catch (e: Throwable) {
                Log.w("GameArenaVM", "Outcome save error: ${e.message}")
            } finally {
                if (_sessionId.value == activeSessionId) {
                    _outcomeSaved.value = true
                }
            }
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

    fun setAudioMutedForCutscene(muted: Boolean, ownerToken: Long? = null) {
        if (ownerToken != null && activeOwnerToken != ownerToken) return
        if (muted) {
            container.audioEngine.onPauseLifecycle()
        } else {
            container.audioEngine.onResumeLifecycle()
        }
    }

    fun updateViewportSize(widthPx: Float, heightPx: Float) {
        val sim = _simulationState.value ?: return
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

    fun resetInputs() {
        touchInput = InputControlsState()
        hardwareInput = InputControlsState()
    }

    fun triggerSpecial() {
        touchInput = touchInput.copy(specialTrigger = true)
    }

    fun startMissionFromBriefing() {
        resetInputs()
        _isPaused.value = false
        _simulationState.value?.let { sim ->
            sim.startMission()
            _hud.value = sim.buildSnapshot()
        }
        container.audioEngine.playSfx("click")
    }

    fun resolveStoryChoice(betrayal: Boolean) {
        resetInputs()
        _simulationState.value?.let { sim ->
            sim.resolveStoryChoice(betrayal)
            _hud.value = sim.buildSnapshot()
        }
    }

    fun togglePause(paused: Boolean) {
        if (paused) resetInputs()
        _isPaused.value = paused
        container.audioEngine.playSfx("click")
    }

    fun stopSessionIfOwner(ownerToken: Long) {
        if (activeOwnerToken != ownerToken) return
        stopSession()
    }

    fun stopSession() {
        startJob?.cancel()
        loopJob?.cancel()
        resetInputs()
    }

    override fun onCleared() {
        stopSession()
        outcomeJob?.cancel()
        super.onCleared()
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GameArenaViewModel(container) as T
        }
    }
}
