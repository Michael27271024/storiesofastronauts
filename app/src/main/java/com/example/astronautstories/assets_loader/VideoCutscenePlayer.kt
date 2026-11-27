package com.example.astronautstories.assets_loader

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.astronautstories.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Crash-proof mandatory cutscene video player backed by Jetpack Media3 ExoPlayer + PlayerView (texture_view).
 * Plays APK assets directly via asset:/// URI with automatic cached-file retry on decode/source error,
 * pauses/resumes cleanly with Android Lifecycle, never calls onCompleted() prematurely on setup race,
 * and guarantees single-shot completion upon real playback end or safe error recovery (no skip allowed).
 */
@OptIn(UnstableApi::class)
@Composable
fun AssetVideoPlayer(
    assetPath: String,
    statusLabel: String,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnCompleted by rememberUpdatedState(onCompleted)
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val normalizedAssetPath = remember(assetPath) { assetPath.trimStart('/') }
    val completedOnce = remember(normalizedAssetPath) { AtomicBoolean(false) }
    val disposed = remember(normalizedAssetPath) { AtomicBoolean(false) }
    val triedFileFallback = remember(normalizedAssetPath) { AtomicBoolean(false) }

    var needsFileFallbackCount by remember(normalizedAssetPath) { mutableIntStateOf(0) }
    var reachedReady by remember(normalizedAssetPath) { mutableStateOf(false) }
    var hasStartedPlaying by remember(normalizedAssetPath) { mutableStateOf(false) }
    var knownDurationMs by remember(normalizedAssetPath) { mutableStateOf(-1L) }
    var prepStartMs by remember(normalizedAssetPath) { mutableStateOf(System.currentTimeMillis()) }
    var lastAdvanceMs by remember(normalizedAssetPath) { mutableStateOf(System.currentTimeMillis()) }
    var isLifecyclePaused by remember(normalizedAssetPath) { mutableStateOf(false) }

    fun completeOnceSafely() {
        if (completedOnce.compareAndSet(false, true)) {
            mainHandler.post {
                try {
                    latestOnCompleted()
                } catch (_: Throwable) {
                }
            }
        }
    }

    val assetValid = remember(normalizedAssetPath) {
        cutsceneAssetExists(appContext, normalizedAssetPath)
    }

    if (!assetValid) {
        LaunchedEffect(normalizedAssetPath) {
            Log.w("AssetVideoPlayer", "Missing cutscene asset: $normalizedAssetPath")
            completeOnceSafely()
        }
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF010101))
        )
        return
    }

    val exoPlayer = remember(normalizedAssetPath) {
        try {
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()
            ExoPlayer.Builder(appContext)
                .setAudioAttributes(audioAttrs, true)
                .build()
                .apply {
                    repeatMode = Player.REPEAT_MODE_OFF
                    volume = 1f
                    playWhenReady = true
                }
        } catch (t: Throwable) {
            Log.e("AssetVideoPlayer", "Failed to create ExoPlayer: ${t.message}", t)
            null
        }
    }

    if (exoPlayer == null) {
        LaunchedEffect(normalizedAssetPath) {
            completeOnceSafely()
        }
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF010101))
        )
        return
    }

    DisposableEffect(exoPlayer, normalizedAssetPath) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (disposed.get() || completedOnce.get()) return
                when (playbackState) {
                    Player.STATE_READY -> {
                        reachedReady = true
                        val now = System.currentTimeMillis()
                        lastAdvanceMs = now
                        val dur = try { exoPlayer.duration } catch (_: Throwable) { C.TIME_UNSET }
                        if (dur != C.TIME_UNSET && dur > 0L) {
                            knownDurationMs = dur
                        }
                    }
                    Player.STATE_ENDED -> {
                        completeOnceSafely()
                    }
                    else -> Unit
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying && !disposed.get() && !completedOnce.get()) {
                    hasStartedPlaying = true
                    lastAdvanceMs = System.currentTimeMillis()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (disposed.get() || completedOnce.get()) return
                Log.w(
                    "AssetVideoPlayer",
                    "ExoPlayer error on $normalizedAssetPath (fallbackTried=${triedFileFallback.get()}): ${error.errorCodeName}",
                    error
                )
                if (triedFileFallback.compareAndSet(false, true)) {
                    needsFileFallbackCount++
                } else {
                    completeOnceSafely()
                }
            }
        }

        exoPlayer.addListener(listener)
        try {
            val useDirectAsset = runCatching {
                appContext.assets.openFd(normalizedAssetPath).use { true }
            }.getOrDefault(false)

            if (useDirectAsset) {
                exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse("asset:///$normalizedAssetPath")))
                exoPlayer.playWhenReady = true
                exoPlayer.prepare()
            } else if (triedFileFallback.compareAndSet(false, true)) {
                needsFileFallbackCount++
            }
        } catch (t: Throwable) {
            Log.w("AssetVideoPlayer", "Initial asset URI prepare error: ${t.message}", t)
            if (triedFileFallback.compareAndSet(false, true)) {
                needsFileFallbackCount++
            } else {
                completeOnceSafely()
            }
        }

        onDispose {
            disposed.set(true)
            try { exoPlayer.removeListener(listener) } catch (_: Throwable) {}
            try { exoPlayer.stop() } catch (_: Throwable) {}
            try { exoPlayer.clearMediaItems() } catch (_: Throwable) {}
            try { exoPlayer.release() } catch (_: Throwable) {}
        }
    }

    // Fallback: extract asset MP4 to cacheDir if asset:/// URI failed or asset is compressed
    LaunchedEffect(needsFileFallbackCount, normalizedAssetPath) {
        if (needsFileFallbackCount <= 0 || disposed.get() || completedOnce.get()) return@LaunchedEffect
        val diskPath = withContext(Dispatchers.IO) {
            copyAssetToCacheFile(appContext, normalizedAssetPath)
        }
        if (disposed.get() || completedOnce.get()) return@LaunchedEffect
        if (diskPath == null) {
            completeOnceSafely()
            return@LaunchedEffect
        }
        try {
            prepStartMs = System.currentTimeMillis()
            lastAdvanceMs = System.currentTimeMillis()
            reachedReady = false
            hasStartedPlaying = false
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(diskPath))))
            exoPlayer.playWhenReady = true
            exoPlayer.prepare()
        } catch (t: Throwable) {
            Log.w("AssetVideoPlayer", "Fallback file prepare error: ${t.message}", t)
            completeOnceSafely()
        }
    }

    // Sync ExoPlayer pause/resume with Android Activity Lifecycle
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    isLifecyclePaused = true
                    try { exoPlayer.pause() } catch (_: Throwable) {}
                }
                Lifecycle.Event.ON_RESUME -> {
                    isLifecyclePaused = false
                    lastAdvanceMs = System.currentTimeMillis()
                    if (!disposed.get() && !completedOnce.get()) {
                        try {
                            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                                completeOnceSafely()
                            } else {
                                exoPlayer.play()
                            }
                        } catch (_: Throwable) {}
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Anti-deadlock Watchdog: only counts preparation stall before READY, playback stall after READY,
    // and enforces a generous hard ceiling beyond the real video duration.
    LaunchedEffect(exoPlayer, normalizedAssetPath) {
        var lastPos = -1L
        var pausedAtMs = 0L
        while (!completedOnce.get() && !disposed.get()) {
            delay(250L)
            val nowMs = System.currentTimeMillis()
            if (isLifecyclePaused) {
                if (pausedAtMs == 0L) pausedAtMs = nowMs
                continue
            } else if (pausedAtMs != 0L) {
                val pauseDuration = (nowMs - pausedAtMs).coerceAtLeast(0L)
                prepStartMs += pauseDuration
                lastAdvanceMs += pauseDuration
                pausedAtMs = 0L
            }

            val playbackState = try { exoPlayer.playbackState } catch (_: Throwable) { Player.STATE_IDLE }
            if (playbackState == Player.STATE_ENDED) {
                completeOnceSafely()
                break
            }

            if (!reachedReady) {
                if (nowMs - prepStartMs > 8000L) {
                    Log.w("AssetVideoPlayer", "Prep watchdog expired for $normalizedAssetPath")
                    completeOnceSafely()
                    break
                }
                continue
            }

            val pos = try { exoPlayer.currentPosition } catch (_: Throwable) { 0L }
            val dur = if (knownDurationMs > 0L) {
                knownDurationMs
            } else {
                val d = try { exoPlayer.duration } catch (_: Throwable) { C.TIME_UNSET }
                if (d != C.TIME_UNSET && d > 0L) {
                    knownDurationMs = d
                    d
                } else -1L
            }

            if (pos > lastPos) {
                lastPos = pos
                lastAdvanceMs = nowMs
            }

            if (hasStartedPlaying && dur > 500L && pos >= dur - 80L) {
                completeOnceSafely()
                break
            }

            val stalledAfterReady = nowMs - lastAdvanceMs > 6000L
            val maxCeilingMs = if (dur > 0L) dur + 10000L else 28000L
            val exceededCeiling = nowMs - prepStartMs > maxCeilingMs
            if (stalledAfterReady || exceededCeiling) {
                Log.w("AssetVideoPlayer", "Playback watchdog completed cutscene $normalizedAssetPath (stalled=$stalledAfterReady, ceiling=$exceededCeiling)")
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
                val pv = try {
                    LayoutInflater.from(ctx).inflate(R.layout.view_cutscene_player, null, false) as PlayerView
                } catch (_: Throwable) {
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShutterBackgroundColor(android.graphics.Color.parseColor("#010101"))
                        keepScreenOn = true
                    }
                }
                pv.apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    keepScreenOn = true
                    player = exoPlayer
                }
            },
            update = { pv ->
                if (pv.player !== exoPlayer) {
                    pv.player = exoPlayer
                }
            },
            onRelease = { pv ->
                try { pv.player = null } catch (_: Throwable) {}
            },
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0xCC09090D),
                border = BorderStroke(1.dp, Color(0x88C47B42)),
                shape = RoundedCornerShape(12.dp)
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
}

private fun cutsceneAssetExists(context: Context, assetPath: String): Boolean {
    return try {
        context.assets.openFd(assetPath).use { true }
    } catch (_: Throwable) {
        try {
            context.assets.open(assetPath).use { true }
        } catch (_: Throwable) {
            false
        }
    }
}

private fun copyAssetToCacheFile(
    context: Context,
    assetPath: String
): String? {
    return try {
        val safeName = assetPath.replace('/', '_')
        val cachedFile = File(context.cacheDir, "cutscene_$safeName")
        if (!cachedFile.exists() || cachedFile.length() == 0L) {
            val tmpFile = File(context.cacheDir, "cutscene_${safeName}.tmp")
            context.assets.open(assetPath).use { input ->
                tmpFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (tmpFile.exists() && tmpFile.length() > 0L) {
                if (cachedFile.exists()) cachedFile.delete()
                tmpFile.renameTo(cachedFile)
            }
        }
        if (cachedFile.exists() && cachedFile.length() > 0L) cachedFile.absolutePath else null
    } catch (_: Throwable) {
        null
    }
}
