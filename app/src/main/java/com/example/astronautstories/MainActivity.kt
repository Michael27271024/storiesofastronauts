package com.example.astronautstories

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.astronautstories.engine.ArenaPhase
import com.example.astronautstories.ui.navigation.AstronautNavHost
import com.example.astronautstories.ui.theme.AstronautStoriesTheme
import com.example.astronautstories.ui.theme.VoidBlack
import com.example.astronautstories.ui.viewmodel.AppContainer
import com.example.astronautstories.ui.viewmodel.GameArenaViewModel
import com.example.astronautstories.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private lateinit var appContainer: AppContainer

    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.Factory(appContainer)
    }
    private val arenaViewModel: GameArenaViewModel by viewModels {
        GameArenaViewModel.Factory(appContainer)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        super.onCreate(savedInstanceState)

        appContainer = AppContainer(applicationContext)

        setContent {
            val settings by mainViewModel.settings.collectAsStateWithLifecycle()

            CompositionLocalProvider(LocalLayoutDirection provides settings.language.layoutDirection) {
                AstronautStoriesTheme(
                    darkTheme = settings.isDarkTheme,
                    accessibilityHighContrast = settings.accessibilityMode,
                    largeTextScale = settings.accessibilityMode
                ) {
                    // Hard Landscape-Only Viewport Lock: even in split-screen, foldables, or tall containers,
                    // lock the entire app and canvas inside a centered 16:9 horizontal landscape container.
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(VoidBlack),
                        contentAlignment = Alignment.Center
                    ) {
                        val landscapeFrameModifier = if (maxHeight > maxWidth) {
                            val clampedLandscapeHeight = (maxWidth * 0.5625f).coerceAtMost(maxHeight)
                            Modifier
                                .fillMaxWidth()
                                .height(clampedLandscapeHeight)
                                .clipToBounds()
                        } else {
                            Modifier.fillMaxSize()
                        }

                        Box(modifier = landscapeFrameModifier) {
                            AstronautNavHost(
                                container = appContainer,
                                mainViewModel = mainViewModel,
                                arenaViewModel = arenaViewModel
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        if (::appContainer.isInitialized) {
            appContainer.audioEngine.onResumeLifecycle()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    override fun onPause() {
        super.onPause()
        if (::appContainer.isInitialized) {
            appContainer.audioEngine.onPauseLifecycle()
        }
    }

    override fun onDestroy() {
        if (::appContainer.isInitialized && isFinishing) {
            appContainer.audioEngine.release()
        }
        super.onDestroy()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val sim = arenaViewModel.simulation
        if (sim != null && sim.phase == ArenaPhase.PLAYING && !arenaViewModel.isPaused.value) {
            val pressed = event.action == KeyEvent.ACTION_DOWN
            val handled = when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> {
                    arenaViewModel.updateHardwareInput { it.copy(left = pressed) }
                    true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> {
                    arenaViewModel.updateHardwareInput { it.copy(right = pressed) }
                    true
                }
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_BUTTON_A -> {
                    arenaViewModel.updateHardwareInput { it.copy(jump = pressed) }
                    true
                }
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_BUTTON_B -> {
                    arenaViewModel.updateHardwareInput { it.copy(crouch = pressed) }
                    true
                }
                KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_R1 -> {
                    arenaViewModel.updateHardwareInput { it.copy(shoot = pressed) }
                    true
                }
                KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_BUTTON_L1 -> {
                    if (pressed) arenaViewModel.updateHardwareInput { it.copy(specialTrigger = true) }
                    true
                }
                else -> false
            }
            if (handled) return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val sim = arenaViewModel.simulation
        if (sim != null &&
            sim.phase == ArenaPhase.PLAYING &&
            (event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK &&
            event.action == MotionEvent.ACTION_MOVE
        ) {
            val axisX = event.getAxisValue(MotionEvent.AXIS_X)
            val axisY = event.getAxisValue(MotionEvent.AXIS_Y)
            arenaViewModel.updateHardwareInput {
                it.copy(
                    left = axisX < -0.35f,
                    right = axisX > 0.35f,
                    crouch = axisY > 0.55f
                )
            }
            return true
        }
        return super.onGenericMotionEvent(event)
    }
}
