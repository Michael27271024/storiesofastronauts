package com.example.astronautstories.assets_loader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

class NativeBitmapRegistry(context: Context) {
    private val assetManager = context.applicationContext.assets

    // Bounded LRU cache to prevent OOM & memory leaks across 100 stages
    private val cache = object : LruCache<String, ImageBitmap>(64) {}

    fun assetExists(assetPath: String): Boolean = try {
        assetManager.open(assetPath).use { true }
    } catch (_: Exception) {
        false
    }

    fun getBitmap(assetPath: String, maxDimension: Int = 512): ImageBitmap? {
        val cacheKey = "${assetPath}_$maxDimension"
        cache.get(cacheKey)?.let { return it }

        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            assetManager.open(assetPath).use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            var sampleSize = 1
            val maxSide = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
            if (maxSide > maxDimension) {
                while ((maxSide / sampleSize) > maxDimension) {
                    sampleSize *= 2
                }
            }
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bmp = assetManager.open(assetPath).use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            }
            bmp?.asImageBitmap()?.also { cache.put(cacheKey, it) }
        } catch (e: Exception) {
            Log.w("BitmapRegistry", "Missing asset $assetPath: ${e.message}")
            null
        }
    }

    fun preloadGameplaySprites() {
        val coreSprites = listOf(
            "images/astronaut.png",
            "images/player_idle_no_gun.png",
            "images/player_idle_gun.png",
            "images/player_walk_no_gun.png",
            "images/player_walk_gun.png",
            "images/player_jump_no_gun.png",
            "images/player_jump_gun.png",
            "images/player_crouch_no_gun.png",
            "images/player_crouch_gun.png",
            "images/zombie.png",
            "images/alien_soldier.png",
            "images/flyer.png",
            "images/splitter.png",
            "images/alien_ship.png",
            "images/alien_ship2.png",
            "images/alien_boss.png",
            "images/alien_hostage.png",
            "images/alien_base.png",
            "images/rocket_saturn.png",
            "images/iran_flag_on_saturn.png",
            "images/gun.png",
            "images/battery.png",
            "images/powerful_battery.png",
            "images/rocket_part.png",
            "images/ammo_pack.png",
            "images/health_pack.png"
        )
        coreSprites.forEach { getBitmap(it, 256) }
    }

    fun clear() {
        cache.evictAll()
    }
}
