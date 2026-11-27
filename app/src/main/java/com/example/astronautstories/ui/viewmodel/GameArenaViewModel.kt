package com.example.astronautstories.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.astronautstories.boss3d.Boss3DCatalog
import com.example.astronautstories.boss3d.Boss3DInput
import com.example.astronautstories.boss3d.Boss3DSimulation
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

    private val _boss3DSimulationState = MutableStateFlow<Boss3DSimulation?>(null)
    val boss3DSimulationState: StateFlow<Boss3DSimulation?> = _boss3DSimulationState.asStateFlow()

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
    private var bossMoveX = 0f
    private var bossMoveY = 0f
    private var bossDashTrigger = false
    private var bossAimYawDelta = 0f
    private var bossAimPitchDelta = 0f
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
        _boss3DSimulationState.value = null
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

            activateStageSession(
                sessionId = nextSessionId,
                mode = mode,
                clampedStage = clampedStage,
                hero = hero,
                weapon = weapon,
                skillLevels = skillLevels,
                graphicsQuality = graphicsQuality,
                bonusMedkits = bonusMedkits,
                bonusAmmo = bonusAmmo,
                bonusShield = bonusShield
            )
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

        container.audioEngine.playSfx("click")
        activateStageSession(
            sessionId = nextSessionId,
            mode = mode,
            clampedStage = clampedStage,
            hero = currentSim.hero,
            weapon = currentSim.weapon,
            skillLevels = currentSim.skillLevels,
            graphicsQuality = currentSim.graphicsQuality,
            bonusMedkits = currentSim.bonusMedkits,
            bonusAmmo = currentSim.bonusAmmo,
            bonusShield = currentSim.bonusShield
        )
    }

    private fun activateStageSession(
        sessionId: Long,
        mode: GameMode,
        clampedStage: Int,
        hero: HeroId,
        weapon: WeaponType,
        skillLevels: Map<SkillType, Int>,
        graphicsQuality: com.example.astronautstories.domain.model.GraphicsQuality,
        bonusMedkits: Int,
        bonusAmmo: Int,
        bonusShield: Boolean
    ) {
        val stageDef = GameCatalog.getStage(clampedStage)
        val freshSim = GameSimulation(
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

        _simulationState.value = freshSim
        if (mode == GameMode.STORY && Boss3DCatalog.isBoss3DStage(stageDef.stageNumber)) {
            val freshBoss3D = Boss3DSimulation(
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
            _boss3DSimulationState.value = freshBoss3D
            _hud.value = freshBoss3D.buildHudSnapshot()
        } else {
            _boss3DSimulationState.value = null
            _hud.value = freshSim.buildSnapshot()
        }
        _frameTick.value = _frameTick.value + 1L
        _isReady.value = true

        container.audioEngine.onResumeLifecycle()
        container.audioEngine.playTheme(stageDef.musicKey, loop = true)
        runGameLoop(sessionId)
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
                val boss3D = _boss3DSimulationState.value
                val snap = if (boss3D != null && boss3D.stageDef.stageNumber == sim.stageDef.stageNumber) {
                    if (!_isPaused.value && boss3D.phase == ArenaPhase.PLAYING) {
                        val bInput = mergeBoss3DInput()
                        try {
                            boss3D.step(dt, bInput)
                        } catch (e: Throwable) {
                            Log.e("GameArenaVM", "Boss3D step error", e)
                        }
                        if (touchInput.specialTrigger) touchInput = touchInput.copy(specialTrigger = false)
                        if (hardwareInput.specialTrigger) hardwareInput = hardwareInput.copy(specialTrigger = false)
                        bossDashTrigger = false
                        bossAimYawDelta = 0f
                        bossAimPitchDelta = 0f
                    }
                    try {
                        boss3D.buildHudSnapshot()
                    } catch (e: Throwable) {
                        Log.e("GameArenaVM", "Boss3D snapshot error", e)
                        HudSnapshot(
                            mode = boss3D.mode,
                            stageNumber = boss3D.stageDef.stageNumber,
                            phase = boss3D.phase,
                            score = boss3D.score,
                            isVictory = boss3D.phase == ArenaPhase.VICTORY,
                            enemiesKilled = boss3D.enemiesKilled,
                            bossKilled = boss3D.bossKilled,
                            shotsFired = boss3D.shotsFired,
                            shotsHit = boss3D.shotsHit,
                            elapsedSeconds = boss3D.elapsedSeconds.toLong(),
                            betrayalChosen = boss3D.betrayalChosen
                        )
                    }
                } else {
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

                    try {
                        sim.buildSnapshot()
                    } catch (e: Throwable) {
                        Log.e("GameArenaVM", "Snapshot build error", e)
                        HudSnapshot(
                            mode = sim.mode,
                            stageNumber = sim.stageDef.stageNumber,
                            phase = sim.phase,
                            score = sim.score,
                            isVictory = sim.phase == ArenaPhase.VICTORY,
                            enemiesKilled = sim.enemiesKilled,
                            bossKilled = sim.bossKilled,
                            shotsFired = sim.shotsFired,
                            shotsHit = sim.shotsHit,
                            elapsedSeconds = sim.elapsedSeconds.toLong(),
                            betrayalChosen = sim.betrayalChosen
                        )
                    }
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

    private fun mergeBoss3DInput(): Boss3DInput {
        val hwX = (if (hardwareInput.right) 1f else 0f) - (if (hardwareInput.left) 1f else 0f)
        val hwY = if (hardwareInput.crouch) -1f else 0f
        return Boss3DInput(
            moveX = (bossMoveX + hwX).coerceIn(-1f, 1f),
            moveY = (bossMoveY + hwY).coerceIn(-1f, 1f),
            jump = touchInput.jump || hardwareInput.jump,
            shoot = touchInput.shoot || hardwareInput.shoot || _autoFire.value,
            dashTrigger = bossDashTrigger,
            specialTrigger = touchInput.specialTrigger || hardwareInput.specialTrigger,
            aimYawDelta = bossAimYawDelta,
            aimPitchDelta = bossAimPitchDelta
        )
    }

    fun updateBoss3DMove(x: Float, y: Float) {
        bossMoveX = x.coerceIn(-1f, 1f)
        bossMoveY = y.coerceIn(-1f, 1f)
    }

    fun addBoss3DAimDelta(yawDelta: Float, pitchDelta: Float) {
        bossAimYawDelta += yawDelta
        bossAimPitchDelta += pitchDelta
    }

    fun triggerBoss3DDash() {
        bossDashTrigger = true
    }

    fun toggleAutoFire() {
        _autoFire.value = !_autoFire.value
        runCatching { container.audioEngine.playSfx("click") }
    }

    fun setAudioMutedForCutscene(muted: Boolean, ownerToken: Long? = null) {
        if (ownerToken != null && activeOwnerToken != ownerToken) return
        runCatching {
            if (muted) {
                container.audioEngine.onPauseLifecycle()
            } else {
                container.audioEngine.onResumeLifecycle()
            }
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
        bossMoveX = 0f
        bossMoveY = 0f
        bossDashTrigger = false
        bossAimYawDelta = 0f
        bossAimPitchDelta = 0f
    }

    fun triggerSpecial() {
        touchInput = touchInput.copy(specialTrigger = true)
    }

    fun startMissionFromBriefing() {
        resetInputs()
        _isPaused.value = false
        val boss3D = _boss3DSimulationState.value
        if (boss3D != null) {
            boss3D.startMission()
            _hud.value = boss3D.buildHudSnapshot()
        } else {
            _simulationState.value?.let { sim ->
                sim.startMission()
                _hud.value = sim.buildSnapshot()
            }
        }
        runCatching { container.audioEngine.playSfx("click") }
    }

    fun resolveStoryChoice(betrayal: Boolean) {
        resetInputs()
        val boss3D = _boss3DSimulationState.value
        if (boss3D != null) {
            boss3D.resolveStoryChoice(betrayal)
            _hud.value = boss3D.buildHudSnapshot()
        } else {
            _simulationState.value?.let { sim ->
                sim.resolveStoryChoice(betrayal)
                _hud.value = sim.buildSnapshot()
            }
        }
    }

    fun togglePause(paused: Boolean) {
        if (paused) resetInputs()
        _isPaused.value = paused
        runCatching { container.audioEngine.playSfx("click") }
    }

    fun stopSessionIfOwner(ownerToken: Long) {
        if (activeOwnerToken != ownerToken) return
        stopSession()
    }

    fun stopSession() {
        startJob?.cancel()
        loopJob?.cancel()
        _boss3DSimulationState.value = null
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
