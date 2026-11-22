package com.example.astronautstories.assets_loader

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface as ComposeSurface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Crash-proof cutscene video player built on TextureView + Android MediaPlayer.
 * Avoids SurfaceView window-layer / Compose clipToBounds crashes and ExoPlayer worker crashes,
 * supports direct AssetFileDescriptor playback with cached-file fallback, preserves aspect ratio,
 * and guarantees single-shot completion via listeners + dual stall/duration watchdogs.
 */
@Composable
fun AssetVideoPlayer(
    assetPath: String,
    statusLabel: String,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val latestOnCompleted by rememberUpdatedState(onCompleted)
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val completedOnce = remember(assetPath) { AtomicBoolean(false) }
    val disposed = remember(assetPath) { AtomicBoolean(false) }
    val playerLock = remember(assetPath) { Any() }

    var mediaPlayerRef by remember(assetPath) { mutableStateOf<MediaPlayer?>(null) }
    var activeSurface by remember(assetPath) { mutableStateOf<Surface?>(null) }
    var prepared by remember(assetPath) { mutableStateOf(false) }
    var videoDims by remember(assetPath) { mutableStateOf(1280 to 720) }

    fun completeOnceSafely() {
        if (completedOnce.compareAndSet(false, true)) {
            mainHandler.post {
                latestOnCompleted()
            }
        }
    }

    fun releasePlayerSafely() {
        synchronized(playerLock) {
            val mp = mediaPlayerRef
            mediaPlayerRef = null
            if (mp != null) {
                runCatching { mp.setSurface(null) }
                runCatching {
                    if (mp.isPlaying) mp.stop()
                }
                runCatching { mp.reset() }
                runCatching { mp.release() }
            }
            val surf = activeSurface
            activeSurface = null
            if (surf != null) {
                runCatching { surf.release() }
            }
        }
    }

    val assetValid = remember(assetPath) {
        try {
            context.assets.open(assetPath).use { true }
        } catch (_: Exception) {
            false
        }
    }

    if (!assetValid) {
        LaunchedEffect(assetPath) {
            completeOnceSafely()
        }
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF010101))
        )
        return
    }

    // Prepare data source off UI thread if needed, then bind and prepareAsync MediaPlayer cleanly on Main thread
    LaunchedEffect(assetPath) {
        val appCtx = context.applicationContext
        val cachedFallbackPath = withContext(Dispatchers.IO) {
            resolveCompressedAssetFallbackIfNeeded(appCtx, assetPath)
        }

        if (disposed.get() || completedOnce.get()) return@LaunchedEffect

        val player = try {
            MediaPlayer()
        } catch (_: Exception) {
            completeOnceSafely()
            return@LaunchedEffect
        }

        try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()
            )
            player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
            player.isLooping = false

            if (cachedFallbackPath == null) {
                appCtx.assets.openFd(assetPath).use { afd ->
                    player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
            } else {
                player.setDataSource(cachedFallbackPath)
            }

            player.setOnVideoSizeChangedListener { _, w, h ->
                if (w > 0 && h > 0) {
                    videoDims = w to h
                }
            }
            player.setOnPreparedListener { mp ->
                synchronized(playerLock) {
                    if (disposed.get() || completedOnce.get() || mediaPlayerRef !== mp) {
                        return@synchronized
                    }
                    prepared = true
                    runCatching {
                        if (!mp.isPlaying) mp.start()
                    }.onFailure {
                        completeOnceSafely()
                    }
                }
            }
            player.setOnCompletionListener {
                completeOnceSafely()
            }
            player.setOnErrorListener { _, _, _ ->
                completeOnceSafely()
                true
            }

            synchronized(playerLock) {
                if (disposed.get() || completedOnce.get()) {
                    runCatching { player.release() }
                    return@LaunchedEffect
                }
                mediaPlayerRef = player
                activeSurface?.let { surf ->
                    if (surf.isValid) {
                        runCatching { player.setSurface(surf) }
                    }
                }
            }

            player.prepareAsync()
        } catch (_: Exception) {
            synchronized(playerLock) {
                if (mediaPlayerRef === player) mediaPlayerRef = null
            }
            runCatching { player.setSurface(null) }
            runCatching { player.release() }
            completeOnceSafely()
        }
    }

    DisposableEffect(assetPath) {
        onDispose {
            disposed.set(true)
            releasePlayerSafely()
        }
    }

    // Dual Watchdog: completes cutscene if prep stalls > 4s, playback stalls > 4.5s, reaches end, or exceeds ceiling
    LaunchedEffect(assetPath) {
        val startMs = System.currentTimeMillis()
        var lastPos = -1
        var lastAdvanceMs = startMs
        while (!completedOnce.get() && !disposed.get()) {
            delay(300L)
            val nowMs = System.currentTimeMillis()
            val mp = synchronized(playerLock) { mediaPlayerRef }
            if (mp == null) {
                if (nowMs - startMs > 4000L) {
                    completeOnceSafely()
                    break
                }
                continue
            }

            if (!prepared) {
                if (nowMs - startMs > 4500L) {
                    completeOnceSafely()
                    break
                }
                continue
            }

            val dur = runCatching { mp.duration }.getOrDefault(-1)
            val pos = runCatching { mp.currentPosition }.getOrDefault(0)
            val playing = runCatching { mp.isPlaying }.getOrDefault(false)

            if (dur > 0 && pos >= dur - 80) {
                completeOnceSafely()
                break
            }

            if (pos > lastPos) {
                lastPos = pos
                lastAdvanceMs = nowMs
            }

            val stalledPlaying = (nowMs - lastAdvanceMs > 4500L) || (!playing && nowMs - lastAdvanceMs > 2500L)
            val exceededCeiling = nowMs - startMs > 13500L
            if (stalledPlaying || exceededCeiling) {
                completeOnceSafely()
                break
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF010101))
            .testTag("story_cutscene_player")
    ) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    isOpaque = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                            applyVideoAspectTransform(this@apply, width, height, videoDims.first, videoDims.second)
                            val surf = Surface(st)
                            synchronized(playerLock) {
                                val old = activeSurface
                                activeSurface = surf
                                if (old != null && old !== surf) {
                                    runCatching { old.release() }
                                }
                                val mp = mediaPlayerRef
                                if (mp != null && !disposed.get()) {
                                    runCatching { mp.setSurface(surf) }
                                    if (prepared) {
                                        runCatching {
                                            if (!mp.isPlaying) mp.start()
                                        }
                                    }
                                }
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
                            applyVideoAspectTransform(this@apply, width, height, videoDims.first, videoDims.second)
                        }

                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                            synchronized(playerLock) {
                                runCatching { mediaPlayerRef?.setSurface(null) }
                                val surf = activeSurface
                                activeSurface = null
                                if (surf != null) {
                                    runCatching { surf.release() }
                                }
                            }
                            return true
                        }

                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
                    }
                }
            },
            update = { tv ->
                if (tv.width > 0 && tv.height > 0) {
                    applyVideoAspectTransform(tv, tv.width, tv.height, videoDims.first, videoDims.second)
                }
            },
            onRelease = { tv ->
                tv.surfaceTextureListener = null
            },
            modifier = Modifier.fillMaxSize()
        )

        ComposeSurface(
            color = Color(0xCC09090D),
            border = BorderStroke(1.dp, Color(0x88C47B42)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .safeDrawingPadding()
                .padding(16.dp)
        ) {
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF5E6C8),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

private fun applyVideoAspectTransform(
    textureView: TextureView,
    viewW: Int,
    viewH: Int,
    videoW: Int,
    videoH: Int
) {
    if (viewW <= 0 || viewH <= 0 || videoW <= 0 || videoH <= 0) return
    val viewAspect = viewW.toFloat() / viewH.toFloat()
    val videoAspect = videoW.toFloat() / videoH.toFloat()
    var scaleX = 1f
    var scaleY = 1f
    if (videoAspect > viewAspect) {
        scaleY = viewAspect / videoAspect
    } else if (videoAspect < viewAspect) {
        scaleX = videoAspect / viewAspect
    }
    val matrix = Matrix()
    matrix.setScale(scaleX, scaleY, viewW / 2f, viewH / 2f)
    textureView.setTransform(matrix)
}

private fun resolveCompressedAssetFallbackIfNeeded(
    context: Context,
    assetPath: String
): String? {
    try {
        context.assets.openFd(assetPath).use { return null }
    } catch (_: Exception) {
        // Fallback if APK asset was compressed
    }

    return try {
        val safeName = assetPath.replace('/', '_')
        val cachedFile = File(context.cacheDir, "cutscene_$safeName")
        if (!cachedFile.exists() || cachedFile.length() == 0L) {
            context.assets.open(assetPath).use { input ->
                cachedFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        cachedFile.absolutePath
    } catch (_: Exception) {
        null
    }
}
