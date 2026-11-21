package com.example.astronautstories.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GameAudioEngine(context: Context) {
    private val appContext = context.applicationContext
    private val assetManager = appContext.assets
    private val ioScope = CoroutineScope(Dispatchers.IO)

    private var mediaPlayer: MediaPlayer? = null
    private var currentThemeKey: String? = null
    private var musicVolume: Float = 0.55f
    private var sfxVolume: Float = 0.80f
    private var isPausedByLifecycle = false

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(10)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

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
        ioScope.launch {
            sfxMap.forEach { (key, path) ->
                try {
                    assetManager.openFd(path).use { afd ->
                        sfxIds[key] = soundPool.load(afd, 1)
                    }
                } catch (e: Exception) {
                    Log.w("GameAudio", "Failed loading SFX $key: ${e.message}")
                }
            }
        }
    }

    fun updateVolumes(music: Float, sfx: Float) {
        musicVolume = music.coerceIn(0f, 1f)
        sfxVolume = sfx.coerceIn(0f, 1f)
        try {
            mediaPlayer?.setVolume(musicVolume, musicVolume)
        } catch (_: Exception) {}
    }

    fun playTheme(key: String, loop: Boolean = true) {
        if (currentThemeKey == key && mediaPlayer != null) return
        val path = themeMap[key] ?: themeMap["ambient"] ?: return
        currentThemeKey = key
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

            val player = MediaPlayer()
            assetManager.openFd(path).use { afd ->
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            player.isLooping = loop
            player.setVolume(musicVolume, musicVolume)
            player.prepare()
            if (!isPausedByLifecycle) {
                player.start()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.w("GameAudio", "Failed playing music $key: ${e.message}")
        }
    }

    fun playSfx(key: String, rate: Float = 1.0f) {
        if (sfxVolume <= 0.01f) return
        val id = sfxIds[key] ?: return
        soundPool.play(id, sfxVolume, sfxVolume, 1, 0, rate)
    }

    fun onPauseLifecycle() {
        isPausedByLifecycle = true
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (_: Exception) {}
    }

    fun onResumeLifecycle() {
        isPausedByLifecycle = false
        try {
            if (mediaPlayer != null && mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
            }
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            soundPool.release()
        } catch (_: Exception) {}
    }
}
