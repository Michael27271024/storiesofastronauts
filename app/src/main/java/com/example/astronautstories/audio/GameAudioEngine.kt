package com.example.astronautstories.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import java.util.concurrent.Executors

class GameAudioEngine(context: Context) {
    private val appContext = context.applicationContext
    private val assetManager = appContext.assets
    private val audioExecutor = Executors.newSingleThreadExecutor()
    private val playerLock = Any()

    private var mediaPlayer: MediaPlayer? = null
    private var currentThemeKey: String? = null
    private var musicVolume: Float = 0.55f
    private var sfxVolume: Float = 0.80f
    private var isPausedByLifecycle = false
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

    private val sfxIds = mutableMapOf<String, Int>()

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

    fun updateVolumes(music: Float, sfx: Float) {
        val m = music.coerceIn(0f, 1f)
        val s = sfx.coerceIn(0f, 1f)
        musicVolume = m
        sfxVolume = s
        audioExecutor.execute {
            synchronized(playerLock) {
                try {
                    mediaPlayer?.setVolume(m, m)
                } catch (_: Exception) {}
            }
        }
    }

    fun playTheme(key: String, loop: Boolean = true) {
        val path = themeMap[key] ?: themeMap["ambient"] ?: return
        audioExecutor.execute {
            synchronized(playerLock) {
                if (isReleased) return@synchronized
                if (currentThemeKey == key && mediaPlayer != null) {
                    try {
                        if (mediaPlayer?.isPlaying == false && !isPausedByLifecycle) {
                            mediaPlayer?.start()
                        }
                    } catch (_: Exception) {}
                    return@synchronized
                }

                currentThemeKey = key
                try {
                    mediaPlayer?.apply {
                        try { stop() } catch (_: Exception) {}
                        try { release() } catch (_: Exception) {}
                    }
                    mediaPlayer = null

                    val player = MediaPlayer()
                    assetManager.openFd(path).use { afd ->
                        player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    }
                    player.isLooping = loop
                    player.setVolume(musicVolume, musicVolume)
                    player.prepare()
                    if (!isPausedByLifecycle && !isReleased) {
                        player.start()
                    }
                    mediaPlayer = player
                } catch (e: Exception) {
                    Log.w("GameAudio", "Failed playing music $key: ${e.message}")
                    mediaPlayer = null
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
        audioExecutor.execute {
            synchronized(playerLock) {
                try {
                    if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer?.pause()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun onResumeLifecycle() {
        isPausedByLifecycle = false
        audioExecutor.execute {
            synchronized(playerLock) {
                if (isReleased || isPausedByLifecycle) return@synchronized
                try {
                    if (mediaPlayer != null && mediaPlayer?.isPlaying == false) {
                        mediaPlayer?.start()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun release() {
        isReleased = true
        audioExecutor.execute {
            synchronized(playerLock) {
                try {
                    mediaPlayer?.stop()
                } catch (_: Exception) {}
                try {
                    mediaPlayer?.release()
                } catch (_: Exception) {}
                mediaPlayer = null
                try {
                    soundPool.release()
                } catch (_: Exception) {}
            }
        }
    }
}
