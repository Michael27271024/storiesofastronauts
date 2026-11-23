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

private class CutscenePlayerHolder {
    val lock = Any()
    var mediaPlayer: MediaPlayer? = null
    var surface: Surface? = null
    var prepared: Boolean = false
    var surfaceAttached: Boolean = false
}

/**
 * Crash-proof cutscene video player built on TextureView + Android MediaPlayer.
 * Attaches a valid Surface before starting playback, supports direct AssetFileDescriptor
 * playback with cached-file fallback, catches all Throwables across native boundaries,
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
    val holder = remember(assetPath) { CutscenePlayerHolder() }

    var videoDims by remember(assetPath) { mutableStateOf(1280 to 720) }

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

    fun releasePlayerSafely() {
        synchronized(holder.lock) {
            val mp = holder.mediaPlayer
            holder.mediaPlayer = null
            holder.prepared = false
            holder.surfaceAttached = false
            if (mp != null) {
                try { mp.setOnPreparedListener(null) } catch (_: Throwable) {}
                try { mp.setOnCompletionListener(null) } catch (_: Throwable) {}
                try { mp.setOnErrorListener(null) } catch (_: Throwable) {}
                try { mp.setOnVideoSizeChangedListener(null) } catch (_: Throwable) {}
                try { mp.setSurface(null) } catch (_: Throwable) {}
                try {
                    if (mp.isPlaying) mp.stop()
                } catch (_: Throwable) {}
                try { mp.reset() } catch (_: Throwable) {}
                try { mp.release() } catch (_: Throwable) {}
            }
            val surf = holder.surface
            holder.surface = null
            if (surf != null) {
                try { surf.release() } catch (_: Throwable) {}
            }
        }
    }

    fun attachSurfaceAndStartLocked(mp: MediaPlayer) {
        if (disposed.get() || completedOnce.get() || holder.mediaPlayer !== mp) return
        val surf = holder.surface
        if (surf == null || !surf.isValid) return

        if (!holder.surfaceAttached) {
            try {
                mp.setSurface(surf)
                holder.surfaceAttached = true
            } catch (_: Throwable) {
                completeOnceSafely()
                return
            }
        }

        if (holder.prepared) {
            try {
                if (!mp.isPlaying) {
                    mp.start()
                }
            } catch (_: Throwable) {
                completeOnceSafely()
            }
        }
    }

    val assetValid = remember(assetPath) {
        try {
            context.assets.openFd(assetPath).use { true }
        } catch (_: Throwable) {
            try {
                context.assets.open(assetPath).use { true }
            } catch (_: Throwable) {
                false
            }
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
        } catch (_: Throwable) {
            completeOnceSafely()
            return@LaunchedEffect
        }

        try {
            player.setOnErrorListener { _, _, _ ->
                completeOnceSafely()
                true
            }
            player.setOnCompletionListener {
                completeOnceSafely()
            }
            player.setOnVideoSizeChangedListener { _, w, h ->
                if (w > 0 && h > 0) {
                    mainHandler.post {
                        if (!disposed.get() && !completedOnce.get()) {
                            videoDims = w to h
                        }
                    }
                }
            }
            player.setOnPreparedListener { mp ->
                synchronized(holder.lock) {
                    if (disposed.get() || completedOnce.get() || holder.mediaPlayer !== mp) {
                        return@synchronized
                    }
                    holder.prepared = true
                    attachSurfaceAndStartLocked(mp)
                }
            }

            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()
            )
            player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
            player.isLooping = false

            var dataSourceBound = false
            if (cachedFallbackPath == null) {
                try {
                    appCtx.assets.openFd(assetPath).use { afd ->
                        player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        dataSourceBound = true
                    }
                } catch (_: Throwable) {
                    dataSourceBound = false
                }
            }

            if (!dataSourceBound) {
                val diskPath = cachedFallbackPath ?: withContext(Dispatchers.IO) {
                    copyAssetToCacheFile(appCtx, assetPath)
                }
                if (diskPath == null || disposed.get() || completedOnce.get()) {
                    try { player.release() } catch (_: Throwable) {}
                    completeOnceSafely()
                    return@LaunchedEffect
                }
                player.setDataSource(diskPath)
            }

            synchronized(holder.lock) {
                if (disposed.get() || completedOnce.get()) {
                    try { player.release() } catch (_: Throwable) {}
                    return@LaunchedEffect
                }
                holder.mediaPlayer = player
                holder.prepared = false
                holder.surfaceAttached = false
                val surf = holder.surface
                if (surf != null && surf.isValid) {
                    try {
                        player.setSurface(surf)
                        holder.surfaceAttached = true
                    } catch (_: Throwable) {
                    }
                }
            }

            player.prepareAsync()
        } catch (_: Throwable) {
            synchronized(holder.lock) {
                if (holder.mediaPlayer === player) {
                    holder.mediaPlayer = null
                    holder.prepared = false
                    holder.surfaceAttached = false
                }
            }
            try { player.setSurface(null) } catch (_: Throwable) {}
            try { player.reset() } catch (_: Throwable) {}
            try { player.release() } catch (_: Throwable) {}
            completeOnceSafely()
        }
    }

    DisposableEffect(assetPath) {
        onDispose {
            disposed.set(true)
            releasePlayerSafely()
        }
    }

    // Dual Watchdog: completes cutscene if prep stalls > 4.5s, playback stalls > 4.5s, reaches end, or exceeds ceiling
    LaunchedEffect(assetPath) {
        val startMs = System.currentTimeMillis()
        var lastPos = -1
        var lastAdvanceMs = startMs
        while (!completedOnce.get() && !disposed.get()) {
            delay(300L)
            val nowMs = System.currentTimeMillis()
            val (mp, isPrepared) = synchronized(holder.lock) {
                holder.mediaPlayer to holder.prepared
            }
            if (mp == null) {
                if (nowMs - startMs > 4000L) {
                    completeOnceSafely()
                    break
                }
                continue
            }

            if (!isPrepared) {
                if (nowMs - startMs > 4500L) {
                    completeOnceSafely()
                    break
                }
                continue
            }

            val dur = try { mp.duration } catch (_: Throwable) { -1 }
            val pos = try { mp.currentPosition } catch (_: Throwable) { 0 }
            val playing = try { mp.isPlaying } catch (_: Throwable) { false }

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
                    isOpaque = true
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                            try {
                                applyVideoAspectTransform(this@apply, width, height, videoDims.first, videoDims.second)
                            } catch (_: Throwable) {
                            }
                            val surf = try {
                                Surface(st)
                            } catch (_: Throwable) {
                                null
                            }
                            if (surf == null) {
                                completeOnceSafely()
                                return
                            }
                            synchronized(holder.lock) {
                                val old = holder.surface
                                holder.surface = surf
                                holder.surfaceAttached = false
                                if (old != null && old !== surf) {
                                    try { old.release() } catch (_: Throwable) {}
                                }
                                val mp = holder.mediaPlayer
                                if (mp != null && !disposed.get() && !completedOnce.get()) {
                                    attachSurfaceAndStartLocked(mp)
                                }
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
                            try {
                                applyVideoAspectTransform(this@apply, width, height, videoDims.first, videoDims.second)
                            } catch (_: Throwable) {
                            }
                        }

                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                            synchronized(holder.lock) {
                                try { holder.mediaPlayer?.setSurface(null) } catch (_: Throwable) {}
                                holder.surfaceAttached = false
                                val surf = holder.surface
                                holder.surface = null
                                if (surf != null) {
                                    try { surf.release() } catch (_: Throwable) {}
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
                    try {
                        applyVideoAspectTransform(tv, tv.width, tv.height, videoDims.first, videoDims.second)
                    } catch (_: Throwable) {
                    }
                }
            },
            onRelease = { tv ->
                try { tv.surfaceTextureListener = null } catch (_: Throwable) {}
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
    } catch (_: Throwable) {
        // Fallback if APK asset was compressed
    }
    return copyAssetToCacheFile(context, assetPath)
}

private fun copyAssetToCacheFile(
    context: Context,
    assetPath: String
): String? {
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
        if (cachedFile.exists() && cachedFile.length() > 0L) cachedFile.absolutePath else null
    } catch (_: Throwable) {
        null
    }
}
