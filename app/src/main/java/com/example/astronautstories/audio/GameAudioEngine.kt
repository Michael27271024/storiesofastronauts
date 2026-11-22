package com.example.astronautstories.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class GameAudioEngine(context: Context) {
    private val appContext = context.applicationContext
    private val assetManager = appContext.assets
    private val audioExecutor = Executors.newSingleThreadExecutor()
    private val playerLock = Any()

    private var mediaPlayer: MediaPlayer? = null
    private var currentThemeKey: String? = null
    @Volatile
    private var musicVolume: Float = 0.55f
    @Volatile
    private var sfxVolume: Float = 0.80f
    @Volatile
    private var isPausedByLifecycle = false
    @Volatile
    private var isReleased = false

    private val soundPool: SoundPool = try {
        SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
    } catch (e: Exception) {
        Log.e("GameAudio", "Failed to build SoundPool: ${e.message}")
        @Suppress("DEPRECATION")
        SoundPool(10, android.media.AudioManager.STREAM_MUSIC, 0)
    }

    private val sfxIds = ConcurrentHashMap<String, Int>()

    private val themeMap = mapOf(
        "menu" to "audio/music/menu_bgm_loop.wav",
        "level_select" to "audio/music/level_select_loop.wav",
        "stage1" to "audio/music/stage1_loop.wav",
        "stage2" to "audio/music/stage2_loop.wav",
        "stage3" to "audio/music/stage3_loop.wav",
        "stage4" to "audio/music/stage4_loop.wav",
        "stage5" to "audio/music/stage5_loop.wav",
        "stage6" to "audio/music/stage6_loop.wav",
        "stage7" to "audio/music/stage7_loop.wav",
        "stage8" to "audio/music/stage8_loop.wav",
        "stage9" to "audio/music/stage9_loop.wav",
        "boss" to "audio/music/boss_theme_loop.wav",
        "victory" to "audio/music/victory_fanfare.wav",
        "game_over" to "audio/music/game_over_theme.wav",
        "ambient" to "audio/music/ambient_space_loop.wav"
    )

    private val sfxMap = mapOf(
        "click" to "audio/sfx/sfx_ui_click.wav",
        "select" to "audio/sfx/sfx_ui_select.wav",
        "locked" to "audio/sfx/sfx_ui_locked.wav",
        "shoot" to "audio/sfx/sfx_shoot.wav",
        "hit" to "audio/sfx/sfx_hit.wav",
        "explosion" to "audio/sfx/sfx_explosion.wav",
        "jump" to "audio/sfx/sfx_jump.wav",
        "footstep" to "audio/sfx/sfx_footstep_1.wav",
        "battery" to "audio/sfx/sfx_pickup_battery.wav",
        "gun" to "audio/sfx/sfx_pickup_gun.wav",
        "part" to "audio/sfx/sfx_pickup_part.wav",
        "powerup" to "audio/sfx/sfx_powerup.wav",
        "boss_hit" to "audio/sfx/sfx_boss_hit.wav",
        "rocket_launch" to "audio/sfx/sfx_rocket_launch.wav",
        "enemy_die" to "audio/sfx/sfx_enemy_die.wav"
    )

    init {
        runCatching {
            audioExecutor.execute {
                sfxMap.forEach { (key, path) ->
                    if (isReleased) return@forEach
                    try {
                        assetManager.openFd(path).use { afd ->
                            val id = soundPool.load(afd, 1)
                            if (id > 0) {
                                sfxIds[key] = id
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("GameAudio", "Failed loading SFX $key: ${e.message}")
                    }
                }
            }
        }
    }

    private fun releaseMediaPlayerLocked() {
        val old = mediaPlayer
        mediaPlayer = null
        if (old != null) {
            runCatching { old.setOnErrorListener(null) }
            runCatching { old.setOnCompletionListener(null) }
            runCatching { if (old.isPlaying) old.stop() }
            runCatching { old.reset() }
            runCatching { old.release() }
        }
    }

    fun updateVolumes(music: Float, sfx: Float) {
        val m = music.coerceIn(0f, 1f)
        val s = sfx.coerceIn(0f, 1f)
        musicVolume = m
        sfxVolume = s
        if (isReleased) return
        runCatching {
            audioExecutor.execute {
                synchronized(playerLock) {
                    if (isReleased) return@synchronized
                    runCatching { mediaPlayer?.setVolume(m, m) }
                }
            }
        }
    }

    fun playTheme(key: String, loop: Boolean = true) {
        if (isReleased) return
        val path = themeMap[key] ?: themeMap["ambient"] ?: return
        runCatching {
            audioExecutor.execute {
                synchronized(playerLock) {
                    if (isReleased) return@synchronized
                    val existing = mediaPlayer
                    if (currentThemeKey == key && existing != null) {
                        val resumedCleanly = try {
                            if (!existing.isPlaying && !isPausedByLifecycle) {
                                existing.start()
                            }
                            true
                        } catch (_: Exception) {
                            false
                        }
                        if (resumedCleanly) return@synchronized
                        releaseMediaPlayerLocked()
                    }

                    currentThemeKey = key
                    releaseMediaPlayerLocked()

                    var player: MediaPlayer? = null
                    try {
                        player = MediaPlayer().apply {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_GAME)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build()
                            )
                            setOnErrorListener { mp, _, _ ->
                                synchronized(playerLock) {
                                    if (mediaPlayer === mp) {
                                        releaseMediaPlayerLocked()
                                        currentThemeKey = null
                                    }
                                }
                                true
                            }
                            assetManager.openFd(path).use { afd ->
                                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                            }
                            isLooping = loop
                            setVolume(musicVolume, musicVolume)
                            prepare()
                            if (!isPausedByLifecycle && !isReleased) {
                                start()
                            }
                        }
                        mediaPlayer = player
                    } catch (e: Exception) {
                        Log.w("GameAudio", "Failed playing music $key: ${e.message}")
                        if (player != null) {
                            runCatching { player.reset() }
                            runCatching { player.release() }
                        }
                        mediaPlayer = null
                        currentThemeKey = null
                    }
                }
            }
        }
    }

    fun playSfx(key: String, rate: Float = 1.0f) {
        if (isReleased || sfxVolume <= 0.01f) return
        val id = sfxIds[key] ?: return
        try {
            soundPool.play(id, sfxVolume, sfxVolume, 1, 0, rate)
        } catch (e: Exception) {
            Log.w("GameAudio", "Failed playing sfx $key: ${e.message}")
        }
    }

    fun onPauseLifecycle() {
        isPausedByLifecycle = true
        if (isReleased) return
        runCatching {
            audioExecutor.execute {
                synchronized(playerLock) {
                    if (isReleased) return@synchronized
                    runCatching {
                        if (mediaPlayer?.isPlaying == true) {
                            mediaPlayer?.pause()
                        }
                    }
                }
            }
        }
    }

    fun onResumeLifecycle() {
        isPausedByLifecycle = false
        if (isReleased) return
        runCatching {
            audioExecutor.execute {
                synchronized(playerLock) {
                    if (isReleased || isPausedByLifecycle) return@synchronized
                    runCatching {
                        val mp = mediaPlayer
                        if (mp != null && !mp.isPlaying) {
                            mp.start()
                        }
                    }
                }
            }
        }
    }

    fun release() {
        isReleased = true
        runCatching {
            audioExecutor.execute {
                synchronized(playerLock) {
                    releaseMediaPlayerLocked()
                    currentThemeKey = null
                    runCatching { soundPool.release() }
                }
            }
        }
    }
}
