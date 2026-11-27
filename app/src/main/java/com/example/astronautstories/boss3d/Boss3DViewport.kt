package com.example.astronautstories.boss3d

import android.opengl.GLSurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun Boss3DViewport(
    bossSim: Boss3DSimulation,
    isPaused: Boolean,
    onAimDrag: (yawDelta: Float, pitchDelta: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val renderer = remember(bossSim.stageDef.stageNumber) { Boss3DGLRenderer(context, bossSim) }
    val glSurfaceRef = remember(bossSim.stageDef.stageNumber) { AtomicReference<GLSurfaceView?>(null) }
    var surfaceReady by remember(bossSim.stageDef.stageNumber) { mutableStateOf<GLSurfaceView?>(null) }
    var isLifecycleResumed by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    val latestOnAimDrag by rememberUpdatedState(onAimDrag)

    LaunchedEffect(bossSim, isPaused, isLifecycleResumed, surfaceReady) {
        renderer.bindSimulation(bossSim)
        val surface = surfaceReady ?: glSurfaceRef.get()
        if (surface != null) {
            if (isLifecycleResumed) {
                runCatching { surface.onResume() }
                val targetMode = if (isPaused) {
                    GLSurfaceView.RENDERMODE_WHEN_DIRTY
                } else {
                    GLSurfaceView.RENDERMODE_CONTINUOUSLY
                }
                if (surface.renderMode != targetMode) {
                    surface.renderMode = targetMode
                }
                runCatching { surface.requestRender() }
            } else {
                surface.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                runCatching { surface.onPause() }
            }
        }
    }

    DisposableEffect(lifecycleOwner, renderer) {
        isLifecycleResumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    isLifecycleResumed = true
                    glSurfaceRef.get()?.let { surface ->
                        runCatching { surface.onResume() }
                        runCatching { surface.requestRender() }
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    isLifecycleResumed = false
                    runCatching { glSurfaceRef.get()?.onPause() }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            val surface = glSurfaceRef.getAndSet(null)
            surfaceReady = null
            runCatching {
                surface?.queueEvent {
                    renderer.releaseGlResources()
                }
                surface?.onPause()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(bossSim) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    latestOnAimDrag(dragAmount.x * 0.0045f, dragAmount.y * 0.0032f)
                }
            }
            .testTag("boss3d_viewport")
    ) {
        AndroidView(
            factory = { ctx ->
                GLSurfaceView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setEGLContextClientVersion(2)
                    preserveEGLContextOnPause = true
                    setRenderer(renderer)
                    renderMode = if (isPaused || !isLifecycleResumed) {
                        GLSurfaceView.RENDERMODE_WHEN_DIRTY
                    } else {
                        GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    }
                    glSurfaceRef.set(this)
                    surfaceReady = this
                    if (isLifecycleResumed) {
                        runCatching { onResume() }
                    }
                    runCatching { requestRender() }
                }
            },
            update = { view ->
                if (glSurfaceRef.get() !== view) {
                    glSurfaceRef.set(view)
                }
                if (surfaceReady !== view) {
                    surfaceReady = view
                }
                val targetMode = if (isPaused || !isLifecycleResumed) {
                    GLSurfaceView.RENDERMODE_WHEN_DIRTY
                } else {
                    GLSurfaceView.RENDERMODE_CONTINUOUSLY
                }
                if (view.renderMode != targetMode) {
                    view.renderMode = targetMode
                    if (targetMode == GLSurfaceView.RENDERMODE_CONTINUOUSLY && isLifecycleResumed) {
                        runCatching { view.onResume() }
                        runCatching { view.requestRender() }
                    }
                }
            },
            onRelease = { view ->
                glSurfaceRef.compareAndSet(view, null)
                if (surfaceReady === view) {
                    surfaceReady = null
                }
                runCatching {
                    view.queueEvent { renderer.releaseGlResources() }
                    view.onPause()
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun Boss3DControlsOverlay(
    specialReadyRatio: Float,
    leftHandedControls: Boolean,
    hapticsEnabled: Boolean,
    onMoveChanged: (Float, Float) -> Unit,
    onJumpChanged: (Boolean) -> Unit,
    onShootChanged: (Boolean) -> Unit,
    onDashTriggered: () -> Unit,
    onSpecialTriggered: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val clampedCharge = specialReadyRatio.coerceIn(0f, 1f)
    val specialReady = clampedCharge >= 0.99f
    val latestOnMoveChanged by rememberUpdatedState(onMoveChanged)
    val latestOnJumpChanged by rememberUpdatedState(onJumpChanged)
    val latestOnShootChanged by rememberUpdatedState(onShootChanged)

    DisposableEffect(Unit) {
        onDispose {
            latestOnMoveChanged(0f, 0f)
            latestOnJumpChanged(false)
            latestOnShootChanged(false)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            val joystickCluster: @Composable () -> Unit = {
                BossVirtualJoystick(
                    onMoveChanged = onMoveChanged
                )
            }

            val actionCluster: @Composable () -> Unit = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Dodge Dash Button (real rocket thruster part asset)
                    Surface(
                        onClick = {
                            if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDashTriggered()
                        },
                        shape = CircleShape,
                        color = Color(0xD9101828),
                        border = BorderStroke(1.6.dp, Color(0xCC38BDF8)),
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("boss3d_dash")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0x5538BDF8), Color.Transparent)
                                    )
                                )
                                .padding(8.dp)
                        ) {
                            Boss3DAssetIcon(
                                primaryAsset = Boss3DAssetMapper.DASH_PRIMARY,
                                secondaryAsset = Boss3DAssetMapper.DASH_SECONDARY,
                                contentDescription = "Dodge Dash",
                                fallbackGlowColor = Color(0xFF38BDF8),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Special Shockwave Button (real energy battery asset + charge arc)
                    Surface(
                        onClick = {
                            if (specialReady) {
                                if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSpecialTriggered()
                            }
                        },
                        shape = CircleShape,
                        color = if (specialReady) Color(0xEB2A1C10) else Color(0xCC14151E),
                        border = BorderStroke(
                            1.6.dp,
                            if (specialReady) Color(0xFFF5E6C8) else Color(0x66C47B42)
                        ),
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("boss3d_special")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize().padding(3.dp)) {
                                drawArc(
                                    color = if (specialReady) Color(0xFFE09658) else Color(0xAA38BDF8),
                                    startAngle = -90f,
                                    sweepAngle = 360f * clampedCharge,
                                    useCenter = false,
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Boss3DAssetIcon(
                                primaryAsset = Boss3DAssetMapper.SPECIAL_PRIMARY,
                                secondaryAsset = Boss3DAssetMapper.SPECIAL_SECONDARY,
                                contentDescription = "Special Shockwave",
                                fallbackGlowColor = if (specialReady) Color(0xFFE09658) else Color(0xFF38BDF8),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(9.dp)
                            )
                        }
                    }

                    // Jetpack Jump Button (real astronaut jetpack jump asset)
                    Boss3DHoldButton(
                        primaryAsset = Boss3DAssetMapper.JUMP_PRIMARY,
                        secondaryAsset = Boss3DAssetMapper.JUMP_SECONDARY,
                        contentDescription = "Jetpack Jump",
                        tag = "boss3d_jump",
                        sizeDp = 58.dp,
                        glowColor = Color(0xFFE09658),
                        borderColor = Color(0xFFF5E6C8),
                        imagePaddingDp = 8.dp,
                        onHoldChanged = { down ->
                            if (down && hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onJumpChanged(down)
                        }
                    )

                    // Primary Fire Button (real hero weapon gun asset)
                    Boss3DHoldButton(
                        primaryAsset = Boss3DAssetMapper.SHOOT_PRIMARY,
                        secondaryAsset = Boss3DAssetMapper.SHOOT_SECONDARY,
                        contentDescription = "Shoot",
                        tag = "boss3d_shoot",
                        sizeDp = 64.dp,
                        glowColor = Color(0xFFE05252),
                        borderColor = Color(0xFFF5E6C8),
                        imagePaddingDp = 9.dp,
                        onHoldChanged = { down ->
                            if (down && hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onShootChanged(down)
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                if (leftHandedControls) {
                    actionCluster()
                    joystickCluster()
                } else {
                    joystickCluster()
                    actionCluster()
                }
            }
        }
    }
}

@Composable
private fun BossVirtualJoystick(
    onMoveChanged: (Float, Float) -> Unit
) {
    val outerRadiusDp = 58.dp
    val maxDragPx = with(LocalDensity.current) { 36.dp.toPx() }
    var knobX by remember { mutableFloatStateOf(0f) }
    var knobY by remember { mutableFloatStateOf(0f) }
    val latestOnMoveChanged by rememberUpdatedState(onMoveChanged)

    DisposableEffect(Unit) {
        onDispose {
            knobX = 0f
            knobY = 0f
            latestOnMoveChanged(0f, 0f)
        }
    }

    Box(
        modifier = Modifier
            .size(outerRadiusDp * 2)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xBB181B28),
                        Color(0x990C0E16)
                    )
                )
            )
            .border(1.6.dp, Color(0xBBC47B42), CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f

                    fun updateKnob(posX: Float, posY: Float) {
                        var dx = posX - centerX
                        var dy = posY - centerY
                        val dist = hypot(dx, dy)
                        if (dist > maxDragPx && dist > 0.001f) {
                            dx = (dx / dist) * maxDragPx
                            dy = (dy / dist) * maxDragPx
                        }
                        knobX = dx
                        knobY = dy
                        latestOnMoveChanged(
                            (dx / maxDragPx).coerceIn(-1f, 1f),
                            (-dy / maxDragPx).coerceIn(-1f, 1f)
                        )
                    }

                    updateKnob(down.position.x, down.position.y)
                    do {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (pointer.pressed) {
                            pointer.consume()
                            updateKnob(pointer.position.x, pointer.position.y)
                        } else {
                            break
                        }
                    } while (true)

                    knobX = 0f
                    knobY = 0f
                    latestOnMoveChanged(0f, 0f)
                }
            }
            .testTag("boss3d_joystick"),
        contentAlignment = Alignment.Center
    ) {
        // Subtle cardinal alignment guide dots inside the pad
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension * 0.38f
            val c = Offset(size.width * 0.5f, size.height * 0.5f)
            val dotR = 2.2.dp.toPx()
            val dotColor = Color(0x66F5E6C8)
            drawCircle(dotColor, dotR, Offset(c.x, c.y - r))
            drawCircle(dotColor, dotR, Offset(c.x, c.y + r))
            drawCircle(dotColor, dotR, Offset(c.x - r, c.y))
            drawCircle(dotColor, dotR, Offset(c.x + r, c.y))
        }

        // Thumbstick knob displaying real astronaut hero asset with FilterQuality.High
        Box(
            modifier = Modifier
                .offset { IntOffset(knobX.roundToInt(), knobY.roundToInt()) }
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFE09658), Color(0xFFC47B42))
                    )
                )
                .border(1.6.dp, Color(0xFFF5E6C8), CircleShape)
                .padding(5.dp),
            contentAlignment = Alignment.Center
        ) {
            Boss3DAssetIcon(
                primaryAsset = Boss3DAssetMapper.HERO_PRIMARY,
                secondaryAsset = Boss3DAssetMapper.HERO_SECONDARY,
                drawableRes = Boss3DAssetMapper.HERO_DRAWABLE_FALLBACK,
                contentDescription = "Move Hero",
                contentScale = ContentScale.Crop,
                fallbackGlowColor = Color(0xFFF5E6C8),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
private fun Boss3DHoldButton(
    primaryAsset: String?,
    secondaryAsset: String? = null,
    @DrawableRes drawableRes: Int? = null,
    contentDescription: String,
    tag: String,
    sizeDp: Dp,
    glowColor: Color,
    borderColor: Color,
    imagePaddingDp: Dp = 8.dp,
    onHoldChanged: (Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val latestOnHoldChanged by rememberUpdatedState(onHoldChanged)

    DisposableEffect(tag) {
        onDispose {
            if (pressed) {
                pressed = false
                latestOnHoldChanged(false)
            }
        }
    }

    Box(
        modifier = Modifier
            .size(sizeDp)
            .scale(if (pressed) 0.93f else 1f)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = if (pressed) 0.85f else 0.42f),
                        Color(0xE011131C)
                    )
                )
            )
            .border(1.8.dp, if (pressed) Color.White else borderColor, CircleShape)
            .pointerInput(tag) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    latestOnHoldChanged(true)
                    waitForUpOrCancellation()
                    pressed = false
                    latestOnHoldChanged(false)
                }
            }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Boss3DAssetIcon(
            primaryAsset = primaryAsset,
            secondaryAsset = secondaryAsset,
            drawableRes = drawableRes,
            contentDescription = contentDescription,
            fallbackGlowColor = glowColor,
            modifier = Modifier
                .fillMaxSize()
                .padding(imagePaddingDp)
        )
    }
}
