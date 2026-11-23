package com.example.astronautstories.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.astronautstories.assets_loader.AssetVideoPlayer
import com.example.astronautstories.assets_loader.NativeBitmapRegistry
import com.example.astronautstories.data.local.AppSettings
import com.example.astronautstories.domain.model.GameCatalog
import com.example.astronautstories.domain.model.GameMode
import com.example.astronautstories.engine.ArenaPhase
import com.example.astronautstories.engine.GameCanvasView
import com.example.astronautstories.localization.AppLanguage
import com.example.astronautstories.localization.GameStrings
import com.example.astronautstories.ui.components.AssetSpriteImage
import com.example.astronautstories.ui.components.UniformAssetBadge
import com.example.astronautstories.ui.viewmodel.GameArenaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean

private enum class StoryTransitionState {
    IDLE,
    WAITING_SAVE,
    PLAYING_VIDEO,
    NAVIGATING_NEXT,
    FINISHED_FINALE
}

@Composable
fun GameArenaScreen(
    mode: GameMode,
    stageNumber: Int,
    arenaViewModel: GameArenaViewModel,
    bitmapRegistry: NativeBitmapRegistry,
    settings: AppSettings,
    strings: GameStrings,
    onNavigateNextStage: (Int) -> Unit,
    onExitArena: () -> Unit
) {
    val hud by arenaViewModel.hud.collectAsStateWithLifecycle()
    val frameTick by arenaViewModel.frameTick.collectAsStateWithLifecycle()
    val isPaused by arenaViewModel.isPaused.collectAsStateWithLifecycle()
    val isReady by arenaViewModel.isReady.collectAsStateWithLifecycle()
    val autoFire by arenaViewModel.autoFire.collectAsStateWithLifecycle()
    val outcomeSaved by arenaViewModel.outcomeSaved.collectAsStateWithLifecycle()
    val sessionId by arenaViewModel.sessionId.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val ownerToken = remember { arenaViewModel.allocateOwnerToken() }

    var activeTransitionVideo by remember(mode, stageNumber, sessionId) { mutableStateOf<String?>(null) }
    var transitionState by remember(mode, stageNumber, sessionId) { mutableStateOf(StoryTransitionState.IDLE) }
    val transitionGuard = remember(mode, stageNumber, sessionId) { AtomicBoolean(false) }
    val nextNavigateGuard = remember(mode, stageNumber, sessionId) { AtomicBoolean(false) }

    val simState by arenaViewModel.simulationState.collectAsStateWithLifecycle()
    val sim = simState
    val sessionMatchesTarget = isReady &&
        sim != null &&
        sim.mode == mode &&
        sim.stageDef.stageNumber == stageNumber &&
        hud.mode == mode &&
        hud.stageNumber == stageNumber

    fun completeStoryStageTransition() {
        activeTransitionVideo = null
        runCatching { arenaViewModel.setAudioMutedForCutscene(false, ownerToken) }
        runCatching { arenaViewModel.resetInputs() }
        if (stageNumber < GameCatalog.TOTAL_STAGES) {
            transitionState = StoryTransitionState.NAVIGATING_NEXT
            if (nextNavigateGuard.compareAndSet(false, true)) {
                runCatching {
                    onNavigateNextStage((stageNumber + 1).coerceAtMost(GameCatalog.TOTAL_STAGES))
                }
            }
        } else {
            transitionState = StoryTransitionState.FINISHED_FINALE
        }
    }

    LaunchedEffect(mode, stageNumber, ownerToken) {
        arenaViewModel.startSession(mode, stageNumber, ownerToken)
    }

    DisposableEffect(ownerToken) {
        onDispose {
            runCatching { arenaViewModel.setAudioMutedForCutscene(false, ownerToken) }
            runCatching { arenaViewModel.stopSessionIfOwner(ownerToken) }
        }
    }

    DisposableEffect(activeTransitionVideo, ownerToken) {
        if (activeTransitionVideo != null) {
            runCatching { arenaViewModel.setAudioMutedForCutscene(true, ownerToken) }
        }
        onDispose {
            if (activeTransitionVideo != null) {
                runCatching { arenaViewModel.setAudioMutedForCutscene(false, ownerToken) }
            }
        }
    }

    // Automatic Stage Completion -> Story Cutscene Playback (at Chapter Bosses) -> Automatic Next Stage Transition
    LaunchedEffect(hud.phase, mode, stageNumber, sessionId, sessionMatchesTarget) {
        if (sessionMatchesTarget &&
            hud.phase == ArenaPhase.VICTORY &&
            mode == GameMode.STORY &&
            transitionGuard.compareAndSet(false, true)
        ) {
            transitionState = StoryTransitionState.WAITING_SAVE
            delay(850L)
            withTimeoutOrNull(1800L) {
                arenaViewModel.outcomeSaved.first { it }
            }
            val currentSim = arenaViewModel.simulation
            val video = if (currentSim?.stageDef?.stageNumber == stageNumber) {
                currentSim.stageDef.transitionVideoAsset
            } else {
                GameCatalog.getStage(stageNumber).transitionVideoAsset
            }
            val videoExists = !video.isNullOrBlank() && runCatching {
                bitmapRegistry.assetExists(video)
            }.getOrDefault(false)
            if (videoExists) {
                transitionState = StoryTransitionState.PLAYING_VIDEO
                activeTransitionVideo = video
            } else {
                completeStoryStageTransition()
            }
        }
    }

    // Screen-level safety watchdog: prevents stalling in PLAYING_VIDEO or NAVIGATING_NEXT under any condition
    LaunchedEffect(transitionState, activeTransitionVideo, stageNumber, sessionId) {
        when (transitionState) {
            StoryTransitionState.PLAYING_VIDEO -> {
                delay(15000L)
                completeStoryStageTransition()
            }
            StoryTransitionState.NAVIGATING_NEXT -> {
                delay(1200L)
                if (stageNumber < GameCatalog.TOTAL_STAGES) {
                    runCatching {
                        onNavigateNextStage((stageNumber + 1).coerceAtMost(GameCatalog.TOTAL_STAGES))
                    }
                } else {
                    transitionState = StoryTransitionState.FINISHED_FINALE
                }
            }
            else -> Unit
        }
    }

    BackHandler {
        when {
            transitionState == StoryTransitionState.PLAYING_VIDEO ||
                transitionState == StoryTransitionState.NAVIGATING_NEXT ||
                activeTransitionVideo != null -> {
                // Wait for active cutscene playback or stage navigation
            }
            sessionMatchesTarget && hud.phase == ArenaPhase.PLAYING -> {
                arenaViewModel.togglePause(!isPaused)
            }
            transitionState != StoryTransitionState.WAITING_SAVE -> {
                onExitArena()
            }
        }
    }

    val currentVideo = activeTransitionVideo
    if (currentVideo != null) {
        AssetVideoPlayer(
            assetPath = currentVideo,
            statusLabel = strings.autoVideoStatusLabel,
            onCompleted = { completeStoryStageTransition() }
        )
        return
    }

    if (transitionState == StoryTransitionState.NAVIGATING_NEXT) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF010101))
        )
        return
    }

    if (!sessionMatchesTarget) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }
    val activeSim = sim ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF010101))
            .onSizeChanged { size ->
                arenaViewModel.updateViewportSize(size.width.toFloat(), size.height.toFloat())
            }
    ) {
        // 60 FPS Horizontal Side-Scrolling Canvas World
        GameCanvasView(
            sim = activeSim,
            frameTick = frameTick,
            bitmapRegistry = bitmapRegistry,
            modifier = Modifier.fillMaxSize()
        )

        // Slim Landscape Top HUD Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = Color(0xD609090D),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.2.dp, Color(0x88C47B42))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Text(
                            text = "❤ ${hud.health}/${hud.maxHealth}   🛡 ${hud.shield}   🔫 ${hud.ammo}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFF5E6C8),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = hud.objectiveText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            ),
                            color = Color(0xFFE09658),
                            maxLines = 2
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${strings.scoreLabel}: ${hud.score}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFF5E6C8),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (mode == GameMode.ENDLESS) "${strings.waveLabel} ${hud.wave}"
                            else if (mode == GameMode.MULTIPLAYER) "VS ${hud.rivalScore}"
                            else "⏱ ${hud.timeRemainingSec}s",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFC47B42)
                        )

                        FilledTonalButton(
                            onClick = { arenaViewModel.toggleAutoFire() },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (autoFire) Color(0xFFC47B42) else Color(0xFF1E1D26),
                                contentColor = if (autoFire) Color(0xFF09090D) else Color(0xFFF5E6C8)
                            ),
                            border = BorderStroke(1.dp, Color(0x88C47B42)),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.height(40.dp).testTag("arena_autofire_button")
                        ) {
                            Text(
                                text = if (autoFire) "AUTO ON" else "AUTO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { arenaViewModel.togglePause(true) },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xCC1E1D26))
                                .border(1.dp, Color(0x88C47B42), CircleShape)
                                .testTag("arena_pause_button")
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = strings.pauseTitle, tint = Color(0xFFF5E6C8))
                        }
                    }
                }
            }

            // Horizontal Stage Progress Indicator
            if (mode == GameMode.STORY) {
                LinearProgressIndicator(
                    progress = { hud.horizontalProgress },
                    color = Color(0xFFC47B42),
                    trackColor = Color(0x33F5E6C8),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }

            // Boss Health Bar when active
            if (hud.phase == ArenaPhase.PLAYING && hud.bossMaxHp > 0 && hud.bossHp > 0) {
                Surface(
                    color = Color(0xCC180A0D),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE05252)),
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "BOSS OVERLORD (Phase ${hud.bossPhase})",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE05252),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${hud.bossHp}/${hud.bossMaxHp}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF5E6C8)
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = { (hud.bossHp.toFloat() / hud.bossMaxHp.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) },
                            color = Color(0xFFE05252),
                            trackColor = Color(0x44F5E6C8),
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
            }

            // Dynamic Status Banner
            if (hud.statusBanner.isNotBlank() && hud.phase == ArenaPhase.PLAYING) {
                Surface(
                    color = Color(0xD613131A),
                    border = BorderStroke(1.dp, Color(0x88C47B42)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = hud.statusBanner,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFF5E6C8),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Ergonomic Horizontal Landscape Two-Thumb Touch Controls
        // Forced physical LTR layout so D-Pad stays Left (with true <- / -> arrows & physics)
        // and Action buttons stay Right regardless of RTL/LTR UI language.
        if (hud.phase == ArenaPhase.PLAYING && !isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .safeDrawingPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    val dpadCluster: @Composable () -> Unit = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            HoldControlButton(
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Move Left",
                                tag = "ctrl_left",
                                sizeDp = 56.dp,
                                onHoldChanged = { down ->
                                    arenaViewModel.updateTouchInput { it.copy(left = down) }
                                }
                            )
                            HoldControlButton(
                                icon = Icons.Default.ArrowDownward,
                                contentDescription = "Crouch / Drop",
                                tag = "ctrl_crouch",
                                sizeDp = 50.dp,
                                onHoldChanged = { down ->
                                    arenaViewModel.updateTouchInput { it.copy(crouch = down) }
                                }
                            )
                            HoldControlButton(
                                icon = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Move Right",
                                tag = "ctrl_right",
                                sizeDp = 56.dp,
                                onHoldChanged = { down ->
                                    arenaViewModel.updateTouchInput { it.copy(right = down) }
                                }
                            )
                        }
                    }

                    val actionCluster: @Composable () -> Unit = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val specialReady = hud.specialReadyRatio >= 0.99f
                            Surface(
                                onClick = {
                                    if (specialReady) {
                                        if (settings.hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        arenaViewModel.triggerSpecial()
                                    }
                                },
                                shape = CircleShape,
                                color = if (specialReady) Color(0xFFE09658) else Color(0xAA1E1D26),
                                border = BorderStroke(1.5.dp, Color(0xFFF5E6C8).copy(alpha = if (specialReady) 0.9f else 0.45f)),
                                modifier = Modifier
                                    .size(50.dp)
                                    .testTag("ctrl_special")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Bolt,
                                        contentDescription = "Special Shockwave",
                                        tint = if (specialReady) Color(0xFF09090D) else Color(0xFFF5E6C8)
                                    )
                                }
                            }

                            HoldControlButton(
                                icon = Icons.Default.ArrowUpward,
                                contentDescription = "Jetpack Jump",
                                tag = "ctrl_jump",
                                sizeDp = 56.dp,
                                containerColor = Color(0xDDC47B42),
                                borderColor = Color(0xFFF5E6C8),
                                iconTint = Color(0xFF09090D),
                                onHoldChanged = { down ->
                                    if (down && settings.hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    arenaViewModel.updateTouchInput { it.copy(jump = down) }
                                }
                            )

                            HoldControlButton(
                                icon = Icons.Default.GpsFixed,
                                contentDescription = "Shoot",
                                tag = "ctrl_shoot",
                                sizeDp = 58.dp,
                                containerColor = Color(0xDDE05252),
                                borderColor = Color(0xFFF5E6C8),
                                iconTint = Color(0xFFF5E6C8),
                                onHoldChanged = { down ->
                                    if (down && settings.hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    arenaViewModel.updateTouchInput { it.copy(shoot = down) }
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        if (settings.leftHandedControls) {
                            actionCluster()
                            dpadCluster()
                        } else {
                            dpadCluster()
                            actionCluster()
                        }
                    }
                }
            }
        }

        // Fixed Non-Scrolling Phase & Pause Modals
        when {
            hud.phase == ArenaPhase.BRIEFING -> {
                ArenaModalCard {
                    UniformAssetBadge(
                        bitmapRegistry = bitmapRegistry,
                        assetPath = "images/astronaut.png",
                        sizeDp = 50.dp
                    )
                    Text(
                        text = if (settings.language == AppLanguage.ENGLISH) activeSim.stageDef.titleEn else activeSim.stageDef.titleFa,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = activeSim.stageDef.briefingFa,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { arenaViewModel.startMissionFromBriefing() },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("arena_start_mission_button")
                    ) {
                        Text(strings.startMission, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onExitArena,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("arena_briefing_back_button")
                    ) {
                        Text(strings.exitToMapBtn)
                    }
                }
            }

            isPaused -> {
                ArenaModalCard {
                    Text(
                        text = strings.pauseTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { arenaViewModel.togglePause(false) },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("arena_resume_button")
                    ) {
                        Text(strings.resumeBtn)
                    }
                    FilledTonalButton(
                        onClick = { arenaViewModel.restartSession(mode, stageNumber, ownerToken) },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("arena_restart_button")
                    ) {
                        Text(strings.restartBtn)
                    }
                    OutlinedButton(
                        onClick = onExitArena,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("arena_pause_exit_button")
                    ) {
                        Text(strings.exitToMapBtn)
                    }
                }
            }

            hud.phase == ArenaPhase.STORY_CHOICE -> {
                ArenaModalCard {
                    AssetSpriteImage(
                        bitmapRegistry = bitmapRegistry,
                        assetPath = "images/astronauts_arguing.png",
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        maxDimension = 256,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Text(
                        text = strings.betrayalChoiceTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (settings.language == AppLanguage.ENGLISH)
                            "The Starspawn offered a forbidden core upgrade if you leave your squad behind."
                        else "فرمانده تاریکی پیشنهاد قدرت مطلق را داده است؛ آیا تیم را نجات می‌دهی یا مسیر رازآلود را برمی‌گزینی؟",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                    Button(
                        onClick = { arenaViewModel.resolveStoryChoice(betrayal = false) },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("choice_friendship_button")
                    ) {
                        Text(strings.friendshipPathBtn)
                    }
                    OutlinedButton(
                        onClick = { arenaViewModel.resolveStoryChoice(betrayal = true) },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("choice_betrayal_button")
                    ) {
                        Text(strings.betrayalPathBtn)
                    }
                }
            }

            hud.phase == ArenaPhase.VICTORY || hud.phase == ArenaPhase.GAME_OVER -> {
                val won = hud.phase == ArenaPhase.VICTORY
                val accuracy = if (hud.shotsFired > 0) ((hud.shotsHit * 100) / hud.shotsFired).coerceIn(0, 100) else 100
                val isAutoStoryTransition = won &&
                    mode == GameMode.STORY &&
                    transitionState != StoryTransitionState.FINISHED_FINALE &&
                    (stageNumber < GameCatalog.TOTAL_STAGES || activeSim.stageDef.transitionVideoAsset != null)

                ArenaModalCard {
                    if (won) {
                        AssetSpriteImage(
                            bitmapRegistry = bitmapRegistry,
                            assetPath = "images/astronauts_happy.png",
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            maxDimension = 256,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                    Text(
                        text = if (won) strings.victoryTitle else strings.gameOverTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (won) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    if (won) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(3) { idx ->
                                Icon(
                                    imageVector = if (idx < hud.starsEarned) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    } else if (hud.statusBanner.isNotBlank()) {
                        Text(
                            hud.statusBanner,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                    Text(
                        text = "${strings.scoreLabel}: ${hud.score}   •   +${hud.xpEarned} ${strings.xpLabel}   •   +${hud.crystalsEarned} 💎",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Kills: ${hud.enemiesKilled}   •   Accuracy: $accuracy%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isAutoStoryTransition) {
                        val videoAssetPath = activeSim.stageDef.transitionVideoAsset
                        val hasVideo = remember(videoAssetPath) {
                            !videoAssetPath.isNullOrBlank() && runCatching {
                                bitmapRegistry.assetExists(videoAssetPath)
                            }.getOrDefault(false)
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("arena_auto_transition_status")
                        ) {
                            Text(
                                text = if (hasVideo) strings.autoVideoStatusLabel else strings.advancingStageLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    } else {
                        FilledTonalButton(
                            onClick = { arenaViewModel.restartSession(mode, stageNumber, ownerToken) },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("arena_retry_button")
                        ) {
                            Text(strings.retryBtn)
                        }
                        OutlinedButton(
                            onClick = onExitArena,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("arena_exit_button")
                        ) {
                            Text(strings.exitToMapBtn)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HoldControlButton(
    icon: ImageVector,
    contentDescription: String,
    tag: String,
    sizeDp: Dp = 56.dp,
    containerColor: Color = Color(0xCC13131A),
    borderColor: Color = Color(0xAAC47B42),
    iconTint: Color = Color(0xFFF5E6C8),
    onHoldChanged: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(containerColor)
            .border(1.5.dp, borderColor, CircleShape)
            .pointerInput(tag) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onHoldChanged(true)
                    waitForUpOrCancellation()
                    onHoldChanged(false)
                }
            }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun ArenaModalCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC010101))
            .safeDrawingPadding()
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth(0.68f)
                .widthIn(min = 320.dp, max = 500.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
            }
        }
    }
}
