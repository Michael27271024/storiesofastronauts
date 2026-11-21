package com.example.astronautstories.ui.viewmodel

import android.content.Context
import com.example.astronautstories.assets_loader.NativeBitmapRegistry
import com.example.astronautstories.audio.GameAudioEngine
import com.example.astronautstories.data.local.GameDatabase
import com.example.astronautstories.data.local.SettingsRepository
import com.example.astronautstories.data.repository.GameRepository

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val database: GameDatabase by lazy { GameDatabase.getInstance(appContext) }
    val gameRepository: GameRepository by lazy { GameRepository(database.gameDao()) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    val audioEngine: GameAudioEngine by lazy { GameAudioEngine(appContext) }
    val bitmapRegistry: NativeBitmapRegistry by lazy { NativeBitmapRegistry(appContext) }
}
