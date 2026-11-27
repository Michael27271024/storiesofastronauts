package com.example.astronautstories.boss3d

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Log
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.astronautstories.R
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

internal data class Boss3DGlTexture(
    val texId: Int,
    val aspectRatio: Float = 1f
)

/**
 * Smart multi-tier asset mapper & high-quality bitmap/OpenGL texture loader
 * strictly scoped to the isolated 3D Boss Fight module.
 */
internal object Boss3DAssetMapper {

    fun bossPortraitPrimaryAsset(stageNumber: Int): String? = when (stageNumber) {
        10, 35, 40, 45, 50 -> null // Uses dedicated high-res generated boss drawables
        20 -> "images/alien_ship.png"
        25 -> "images/splitter.png"
        else -> "images/alien_boss.png"
    }

    @DrawableRes
    fun bossPortraitDrawableFallback(stageNumber: Int): Int = when (stageNumber) {
        10, 40 -> R.drawable.img_boss3d_orbital_titan_1795636223873
        35 -> R.drawable.img_boss3d_cryo_hydra_1795636223465
        45, 50 -> R.drawable.img_boss3d_omega_emperor_1795636223076
        else -> R.drawable.img_boss_citadel_1795189793321
    }

    fun bossHullTextureAsset(stageNumber: Int): String? = when (stageNumber) {
        10, 35, 40, 45, 50 -> null
        20 -> "images/alien_ship.png"
        25 -> "images/splitter.png"
        else -> "images/alien_boss.png"
    }

    fun bossHullSecondaryAsset(stageNumber: Int): String = when (stageNumber) {
        10, 40 -> "images/alien_ship2.png"
        20 -> "images/alien_ship.png"
        25 -> "images/splitter.png"
        35 -> "images/flyer.png"
        else -> "images/alien_boss.png"
    }

    fun bossSatelliteTextureAsset(stageNumber: Int): String = when (stageNumber) {
        10, 40 -> "images/flyer.png"
        20, 25 -> "images/alien_ship2.png"
        35 -> "images/splitter.png"
        45, 50 -> "images/alien_ship.png"
        else -> "images/flyer.png"
    }

    // Animated full-body Hero frames consistent with the main 2D game sprites
    const val HERO_IDLE_ASSET = "images/player_idle_gun.png"
    const val HERO_WALK_ASSET = "images/player_walk_gun.png"
    const val HERO_JUMP_ASSET = "images/player_jump_gun.png"
    const val HERO_PRIMARY = "images/astronaut.png"
    const val HERO_SECONDARY = "images/player_idle_gun.png"
    val HERO_DRAWABLE_FALLBACK = R.drawable.img_hero_cosmos_1795189684517

    const val DRONE_PRIMARY = "images/rocket_saturn.png"
    const val DRONE_SECONDARY = "images/alien_ship.png"

    const val SHOOT_PRIMARY = "images/gun.png"
    const val SHOOT_SECONDARY = "images/player_idle_gun.png"

    const val JUMP_PRIMARY = "images/player_jump_gun.png"
    const val JUMP_SECONDARY = "images/rocket_saturn.png"

    const val DASH_PRIMARY = "images/rocket_part.png"
    const val DASH_SECONDARY = "images/player_walk_gun.png"

    const val SPECIAL_PRIMARY = "images/powerful_battery.png"
    const val SPECIAL_SECONDARY = "images/battery.png"

    const val AMMO_PRIMARY = "images/ammo_pack.png"
    const val HEALTH_PRIMARY = "images/health_pack.png"
}

/**
 * Bounded Compose ImageBitmap cache with power-of-2 inSampleSize decoding
 * and guaranteed FilterQuality.High rendering (never pixelated).
 */
internal object Boss3DBitmapCache {
    private val cache = object : LruCache<String, ImageBitmap>(36) {}

    fun loadUiBitmap(
        context: Context,
        primaryAsset: String?,
        secondaryAsset: String? = null,
        @DrawableRes drawableRes: Int? = null,
        maxDimension: Int = 256
    ): ImageBitmap? {
        val key = "${primaryAsset.orEmpty()}|${secondaryAsset.orEmpty()}|${drawableRes ?: 0}|$maxDimension"
        cache.get(key)?.let { return it }

        val appCtx = context.applicationContext
        val decoded = decodeAssetSampled(appCtx, primaryAsset, maxDimension)
            ?: decodeAssetSampled(appCtx, secondaryAsset, maxDimension)
            ?: decodeDrawableSampled(appCtx, drawableRes, maxDimension)

        return decoded?.asImageBitmap()?.also { cache.put(key, it) }
    }

    fun decodeRawBitmapForGl(
        context: Context,
        primaryAsset: String?,
        secondaryAsset: String? = null,
        @DrawableRes drawableRes: Int? = null,
        maxDimension: Int = 512
    ): Bitmap? {
        val appCtx = context.applicationContext
        return decodeAssetSampled(appCtx, primaryAsset, maxDimension)
            ?: decodeAssetSampled(appCtx, secondaryAsset, maxDimension)
            ?: decodeDrawableSampled(appCtx, drawableRes, maxDimension)
    }

    private fun decodeAssetSampled(context: Context, assetPath: String?, maxDimension: Int): Bitmap? {
        if (assetPath.isNullOrBlank()) return null
        val normalized = assetPath.trimStart('/')
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.assets.open(normalized).use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null
            val sampleSize = computePowerOfTwoSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, maxDimension)
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.assets.open(normalized).use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            }
        } catch (t: Throwable) {
            Log.w("Boss3DAssets", "Could not decode asset $normalized: ${t.message}")
            null
        }
    }

    private fun decodeDrawableSampled(context: Context, @DrawableRes resId: Int?, maxDimension: Int): Bitmap? {
        if (resId == null || resId == 0) return null
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(context.resources, resId, boundsOpts)
            if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null
            val sampleSize = computePowerOfTwoSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, maxDimension)
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeResource(context.resources, resId, decodeOpts)
        } catch (t: Throwable) {
            Log.w("Boss3DAssets", "Could not decode drawable $resId: ${t.message}")
            null
        }
    }

    private fun computePowerOfTwoSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var sample = 1
        val largest = maxOf(width, height)
        if (largest > maxDim) {
            while ((largest / sample) > maxDim) {
                sample *= 2
            }
        }
        return sample.coerceAtLeast(1)
    }
}

/**
 * Uploads Power-of-Two (POT) anti-aliased OpenGL ES 2.0 textures with:
 * - Seamless equirectangular procedural planet textures per chapter (zero flat-background stretch)
 * - Radial brushed-alloy Sci-Fi arena combat deck textures per chapter
 * - Automatic exterior & black-pocket background removal for opaque-backdrop sprites
 * - Tight subject cropping + transparent safety border + preserved natural aspect ratio
 * - Multi-pass edge RGB dilation into alpha=0 texels (eliminates dark/white edge halos)
 * - Direct RGBA upload + GL_LINEAR_MIPMAP_LINEAR / GL_LINEAR filtering (never GL_NEAREST)
 */
internal object Boss3DGlTextureFactory {

    /**
     * Generates a seamless equirectangular spherical planet map (512x256 POT)
     * using periodic 3D spherical coordinates so u=0 and u=1 meet seamlessly.
     */
    fun createProceduralEquirectangularPlanetTexture(chapterIndex: Int, spec: Boss3DSpec): Int {
        val w = 512
        val h = 256
        val pixels = IntArray(w * h)
        val baseR = spec.planetColor[0].coerceIn(0f, 1f)
        val baseG = spec.planetColor[1].coerceIn(0f, 1f)
        val baseB = spec.planetColor[2].coerceIn(0f, 1f)
        val accentR = spec.coreGlowColor[0].coerceIn(0f, 1f)
        val accentG = spec.coreGlowColor[1].coerceIn(0f, 1f)
        val accentB = spec.coreGlowColor[2].coerceIn(0f, 1f)
        val isGasGiant = chapterIndex == 4 || chapterIndex == 5 || chapterIndex == 7 || chapterIndex == 8
        val isEarthOrOrbital = chapterIndex == 2 || chapterIndex == 10
        val isEclipse = chapterIndex == 6 || chapterIndex == 9

        for (y in 0 until h) {
            val v = (y + 0.5f) / h.toFloat()
            val lat = (v - 0.5f) * PI.toFloat()
            val cosLat = cos(lat)
            val sinLat = sin(lat)
            val rowOff = y * w
            for (x in 0 until w) {
                val u = (x + 0.5f) / w.toFloat()
                val lon = u * (2f * PI.toFloat())
                val sx = cosLat * cos(lon)
                val sy = sinLat
                val sz = cosLat * sin(lon)

                val n1 = fbm3D(sx * 2.4f + chapterIndex * 1.7f, sy * 2.4f, sz * 2.4f)
                val n2 = fbm3D(sx * 5.8f - 3.1f, sy * 5.8f, sz * 5.8f)

                var r: Float
                var g: Float
                var b: Float

                when {
                    isEarthOrOrbital -> {
                        val landMask = smoothstep(0.49f, 0.56f, n1 * 0.72f + n2 * 0.28f)
                        val cloudMask = smoothstep(0.56f, 0.74f, fbm3D(sx * 3.6f + 7f, sy * 4.2f, sz * 3.6f))
                        val oceanR = 0.06f + n2 * 0.08f
                        val oceanG = 0.34f + n2 * 0.20f
                        val oceanB = 0.78f + n2 * 0.16f
                        val landR = 0.20f + n2 * 0.22f
                        val landG = 0.54f + n2 * 0.22f
                        val landB = 0.24f + n2 * 0.10f
                        r = oceanR + (landR - oceanR) * landMask
                        g = oceanG + (landG - oceanG) * landMask
                        b = oceanB + (landB - oceanB) * landMask
                        // Polar ice caps
                        val polar = smoothstep(0.76f, 0.92f, abs(sinLat))
                        r += (0.94f - r) * polar
                        g += (0.97f - g) * polar
                        b += (1.00f - b) * polar
                        // Swirling white clouds
                        r += (0.98f - r) * cloudMask * 0.72f
                        g += (0.99f - g) * cloudMask * 0.72f
                        b += (1.00f - b) * cloudMask * 0.72f
                    }
                    isGasGiant -> {
                        val band = sin(sy * 16f + n1 * 4.2f) * 0.5f + 0.5f
                        val swirl = n2 * 0.35f + band * 0.65f
                        r = (baseR * (0.62f + swirl * 0.52f) + accentR * (1f - band) * 0.14f).coerceIn(0f, 1f)
                        g = (baseG * (0.62f + swirl * 0.52f) + accentG * (1f - band) * 0.14f).coerceIn(0f, 1f)
                        b = (baseB * (0.62f + swirl * 0.52f) + accentB * (1f - band) * 0.14f).coerceIn(0f, 1f)
                    }
                    isEclipse -> {
                        val ridge = abs(n1 * 2f - 1f)
                        val fissure = (1f - smoothstep(0.02f, 0.16f, ridge)) * 0.85f
                        val crust = 0.48f + n2 * 0.52f
                        r = (baseR * crust + accentR * fissure).coerceIn(0f, 1f)
                        g = (baseG * crust + accentG * fissure).coerceIn(0f, 1f)
                        b = (baseB * crust + accentB * fissure).coerceIn(0f, 1f)
                    }
                    else -> {
                        // Rocky / cratered Moon & Mars surfaces
                        val detail = n1 * 0.65f + n2 * 0.35f
                        val crater = smoothstep(0.66f, 0.78f, n2) * 0.25f
                        val shade = (0.52f + detail * 0.62f - crater).coerceIn(0.28f, 1.15f)
                        r = (baseR * shade).coerceIn(0f, 1f)
                        g = (baseG * shade).coerceIn(0f, 1f)
                        b = (baseB * shade).coerceIn(0f, 1f)
                    }
                }

                val ir = (r * 255f).toInt().coerceIn(0, 255)
                val ig = (g * 255f).toInt().coerceIn(0, 255)
                val ib = (b * 255f).toInt().coerceIn(0, 255)
                pixels[rowOff + x] = (0xFF shl 24) or (ir shl 16) or (ig shl 8) or ib
            }
        }
        return uploadRgbaPixelsAsMipmappedTexture(pixels, w, h, repeatWrap = true)
    }

    /**
     * Generates a crisp radial brushed-metal Sci-Fi arena deck texture (512x512 POT)
     * with concentric plating rings, radial deck seams, and glowing trim conduits.
     */
    fun createProceduralSciFiDeckTexture(spec: Boss3DSpec): Int {
        val size = 512
        val pixels = IntArray(size * size)
        val baseR = spec.deckColor[0].coerceIn(0f, 1f)
        val baseG = spec.deckColor[1].coerceIn(0f, 1f)
        val baseB = spec.deckColor[2].coerceIn(0f, 1f)
        val trimR = spec.deckTrimColor[0].coerceIn(0f, 1f)
        val trimG = spec.deckTrimColor[1].coerceIn(0f, 1f)
        val trimB = spec.deckTrimColor[2].coerceIn(0f, 1f)
        val half = (size - 1) * 0.5f

        for (y in 0 until size) {
            val dy = (y - half) / half
            val rowOff = y * size
            for (x in 0 until size) {
                val dx = (x - half) / half
                val r = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                val angle = kotlin.math.atan2(dy.toDouble(), dx.toDouble()).toFloat()

                // Concentric armor plates & subtle radial bevels
                val ringWave = abs(((r * 6f) % 1f) - 0.5f) * 2f
                val ringSeam = 1f - smoothstep(0.92f, 0.99f, ringWave)
                val spokeFrac = abs((((angle / (2f * PI.toFloat()) + 0.5f) * 12f) % 1f) - 0.5f) * 2f
                val spokeSeam = if (r > 0.16f) 1f - smoothstep(0.95f, 0.995f, spokeFrac) else 1f
                val microGrain = valueNoise2D(x * 0.14f, y * 0.14f) * 0.08f

                // Glowing energy conduit rings near perimeter & inner core
                val innerGlow = expDist(r, 0.24f, 0.014f) * 0.55f
                val midGlow = expDist(r, 0.62f, 0.011f) * 0.32f
                val outerGlow = expDist(r, 0.92f, 0.016f) * 0.72f
                val glow = (innerGlow + midGlow + outerGlow).coerceIn(0f, 1f)

                val plateShade = (0.72f + microGrain + (1f - r * 0.22f) * 0.25f) *
                    (0.68f + 0.32f * min(ringSeam, spokeSeam))

                val cr = (baseR * plateShade + trimR * glow * 0.85f).coerceIn(0f, 1f)
                val cg = (baseG * plateShade + trimG * glow * 0.85f).coerceIn(0f, 1f)
                val cb = (baseB * plateShade + trimB * glow * 0.85f).coerceIn(0f, 1f)

                val ir = (cr * 255f).toInt().coerceIn(0, 255)
                val ig = (cg * 255f).toInt().coerceIn(0, 255)
                val ib = (cb * 255f).toInt().coerceIn(0, 255)
                pixels[rowOff + x] = (0xFF shl 24) or (ir shl 16) or (ig shl 8) or ib
            }
        }
        return uploadRgbaPixelsAsMipmappedTexture(pixels, size, size, repeatWrap = false)
    }

    private fun expDist(value: Float, center: Float, width: Float): Float {
        val d = abs(value - center) / width
        return if (d > 4f) 0f else kotlin.math.exp((-d * d).toDouble()).toFloat()
    }

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0).coerceAtLeast(1e-5f)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun fbm3D(x: Float, y: Float, z: Float): Float {
        var total = 0f
        var amp = 0.5f
        var freq = 1f
        for (i in 0 until 3) {
            total += valueNoise3D(x * freq, y * freq, z * freq) * amp
            freq *= 2.05f
            amp *= 0.5f
        }
        return total / 0.875f
    }

    private fun valueNoise3D(x: Float, y: Float, z: Float): Float {
        val ix = floor(x.toDouble()).toInt()
        val iy = floor(y.toDouble()).toInt()
        val iz = floor(z.toDouble()).toInt()
        val fx = x - ix; val fy = y - iy; val fz = z - iz
        val ux = fx * fx * (3f - 2f * fx)
        val uy = fy * fy * (3f - 2f * fy)
        val uz = fz * fz * (3f - 2f * fz)

        fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
        val n000 = hash3(ix, iy, iz); val n100 = hash3(ix + 1, iy, iz)
        val n010 = hash3(ix, iy + 1, iz); val n110 = hash3(ix + 1, iy + 1, iz)
        val n001 = hash3(ix, iy, iz + 1); val n101 = hash3(ix + 1, iy, iz + 1)
        val n011 = hash3(ix, iy + 1, iz + 1); val n111 = hash3(ix + 1, iy + 1, iz + 1)

        val nx00 = lerp(n000, n100, ux); val nx10 = lerp(n010, n110, ux)
        val nx01 = lerp(n001, n101, ux); val nx11 = lerp(n011, n111, ux)
        return lerp(lerp(nx00, nx10, uy), lerp(nx01, nx11, uy), uz)
    }

    private fun valueNoise2D(x: Float, y: Float): Float {
        val ix = floor(x.toDouble()).toInt()
        val iy = floor(y.toDouble()).toInt()
        val fx = x - ix; val fy = y - iy
        val ux = fx * fx * (3f - 2f * fx)
        val uy = fy * fy * (3f - 2f * fy)
        val a = hash3(ix, iy, 0)
        val b = hash3(ix + 1, iy, 0)
        val c = hash3(ix, iy + 1, 0)
        val d = hash3(ix + 1, iy + 1, 0)
        return (a + (b - a) * ux) + ((c + (d - c) * ux) - (a + (b - a) * ux)) * uy
    }

    private fun hash3(x: Int, y: Int, z: Int): Float {
        var h = x * 374761393 + y * 668265263 + z * 2147483647
        h = (h xor (h ushr 13)) * 1274126177
        h = h xor (h ushr 16)
        return ((h and 0x7FFFFFFF).toFloat() / 2147483647f)
    }

    /**
     * Loads a transparent cutout sprite texture specifically for Camera-Facing Billboards:
     * preserves true subject aspect ratio, removes solid exterior & dark-pocket background if opaque,
     * dilates boundary colors to prevent white/black edge halos under bilinear/mipmap filtering,
     * and uploads straight RGBA bytes directly to OpenGL ES 2.0.
     */
    fun loadBillboardSpriteTexture(
        context: Context,
        primaryAsset: String?,
        secondaryAsset: String? = null,
        @DrawableRes drawableRes: Int? = null,
        potSize: Int = 512,
        fallbackArgb: Int = 0xFF38BDF8.toInt()
    ): Boss3DGlTexture {
        val raw = Boss3DBitmapCache.decodeRawBitmapForGl(
            context = context,
            primaryAsset = primaryAsset,
            secondaryAsset = secondaryAsset,
            drawableRes = drawableRes,
            maxDimension = 768
        ) ?: return createProceduralGlowOrbTexture(128, fallbackArgb)

        val mutable = if (raw.config == Bitmap.Config.ARGB_8888 && raw.isMutable) {
            raw
        } else {
            val copy = raw.copy(Bitmap.Config.ARGB_8888, true)
            raw.recycle()
            copy
        }

        val w = mutable.width.coerceAtLeast(1)
        val h = mutable.height.coerceAtLeast(1)
        val pixels = IntArray(w * h)
        mutable.getPixels(pixels, 0, w, 0, 0, w, h)
        mutable.recycle()

        cleanOrRemoveExteriorBackground(pixels, w, h)

        var minX = w
        var minY = h
        var maxX = -1
        var maxY = -1
        for (y in 0 until h) {
            val rowOff = y * w
            for (x in 0 until w) {
                val a = (pixels[rowOff + x] ushr 24) and 0xFF
                if (a > 10) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        val cropW: Int
        val cropH: Int
        val croppedBmp: Bitmap
        if (maxX >= minX && maxY >= minY) {
            val tightW = maxX - minX + 1
            val tightH = maxY - minY + 1
            val padX = max(2, (tightW * 0.04f).toInt())
            val padY = max(2, (tightH * 0.04f).toInt())
            cropW = tightW + padX * 2
            cropH = tightH + padY * 2

            val tightBmp = Bitmap.createBitmap(cropW, cropH, Bitmap.Config.ARGB_8888)
            val paddedPixels = IntArray(cropW * cropH)
            for (sy in 0 until tightH) {
                val srcRow = (minY + sy) * w + minX
                val dstRow = (sy + padY) * cropW + padX
                System.arraycopy(pixels, srcRow, paddedPixels, dstRow, tightW)
            }
            tightBmp.setPixels(paddedPixels, 0, cropW, 0, 0, cropW, cropH)
            croppedBmp = tightBmp
        } else {
            cropW = w
            cropH = h
            croppedBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, w, 0, 0, w, h)
            }
        }

        val subjectAspect = (cropW.toFloat() / cropH.toFloat()).coerceIn(0.25f, 4.0f)

        val potBmp = ensurePotBitmap(croppedBmp, potSize)
        val uploadW = potBmp.width.coerceAtLeast(1)
        val uploadH = potBmp.height.coerceAtLeast(1)
        val potPixels = IntArray(uploadW * uploadH)
        potBmp.getPixels(potPixels, 0, uploadW, 0, 0, uploadW, uploadH)
        potBmp.recycle()

        dilateRgbIntoTransparentTexels(potPixels, uploadW, uploadH, passes = 4)

        val texId = uploadRgbaPixelsAsMipmappedTexture(potPixels, uploadW, uploadH, repeatWrap = false)
        return Boss3DGlTexture(texId = texId, aspectRatio = subjectAspect)
    }

    /** Generates a silky radial Gaussian glow orb texture for energy auras, particles, and contact shadows */
    fun createProceduralGlowOrbTexture(size: Int = 128, tintArgb: Int = 0xFFFFFFFF.toInt()): Boss3DGlTexture {
        val pixels = IntArray(size * size)
        val tr = (tintArgb shr 16) and 0xFF
        val tg = (tintArgb shr 8) and 0xFF
        val tb = tintArgb and 0xFF
        val half = (size - 1) * 0.5f

        for (y in 0 until size) {
            val dy = (y - half) / half
            for (x in 0 until size) {
                val dx = (x - half) / half
                val r = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                val raw = (1f - r).coerceIn(0f, 1f)
                val smooth = raw * raw * (3f - 2f * raw)
                val glow = smooth.pow(1.45f)
                val a = (glow * 255f).toInt().coerceIn(0, 255)
                pixels[y * size + x] = (a shl 24) or (tr shl 16) or (tg shl 8) or tb
            }
        }
        val texId = uploadRgbaPixelsAsMipmappedTexture(pixels, size, size, repeatWrap = false)
        return Boss3DGlTexture(texId = texId, aspectRatio = 1f)
    }

    /** Generates a crisp anti-aliased circular tactical target reticle texture for Boss lock-on */
    fun createProceduralReticleTexture(size: Int = 256): Boss3DGlTexture {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val c = size * 0.5f
        val rOuter = size * 0.42f
        val rInner = size * 0.28f

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = size * 0.032f
            color = android.graphics.Color.WHITE
            strokeCap = Paint.Cap.ROUND
        }
        for (q in 0 until 4) {
            val start = q * 90f + 16f
            canvas.drawArc(
                c - rOuter, c - rOuter, c + rOuter, c + rOuter,
                start, 58f, false, ringPaint
            )
        }

        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = size * 0.018f
            color = android.graphics.Color.argb(200, 255, 255, 255)
        }
        canvas.drawCircle(c, c, rInner, innerPaint)

        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = size * 0.028f
            strokeCap = Paint.Cap.ROUND
            color = android.graphics.Color.WHITE
        }
        for (k in 0 until 4) {
            val angle = k * Math.PI * 0.5
            val cosA = cos(angle).toFloat()
            val sinA = sin(angle).toFloat()
            canvas.drawLine(
                c + cosA * (rInner * 0.85f), c + sinA * (rInner * 0.85f),
                c + cosA * (rOuter * 1.08f), c + sinA * (rOuter * 1.08f),
                tickPaint
            )
        }

        val pixels = IntArray(size * size)
        bmp.getPixels(pixels, 0, size, 0, 0, size, size)
        bmp.recycle()
        dilateRgbIntoTransparentTexels(pixels, size, size, passes = 2)
        val texId = uploadRgbaPixelsAsMipmappedTexture(pixels, size, size, repeatWrap = false)
        return Boss3DGlTexture(texId = texId, aspectRatio = 1f)
    }

    fun createSolidWhiteGlTexture(): Int {
        val bmp = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(android.graphics.Color.WHITE)
        val texIds = IntArray(1)
        GLES20.glGenTextures(1, texIds, 0)
        val texId = texIds[0]
        if (texId != 0) {
            while (GLES20.glGetError() != GLES20.GL_NO_ERROR) {
                // Drain prior GL errors
            }
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        }
        bmp.recycle()
        return texId
    }

    private fun ensurePotBitmap(src: Bitmap, potSize: Int): Bitmap {
        return try {
            if (src.width == potSize && src.height == potSize && src.config == Bitmap.Config.ARGB_8888) {
                src
            } else {
                val scaled = Bitmap.createScaledBitmap(src, potSize, potSize, true)
                if (scaled !== src) src.recycle()
                scaled
            }
        } catch (_: Throwable) {
            src
        }
    }

    private fun uploadRgbaPixelsAsMipmappedTexture(
        pixels: IntArray,
        width: Int,
        height: Int,
        repeatWrap: Boolean = false
    ): Int {
        val texIds = IntArray(1)
        GLES20.glGenTextures(1, texIds, 0)
        val texId = texIds[0]
        if (texId == 0) return 0

        val byteBuf = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
        for (i in pixels.indices) {
            val argb = pixels[i]
            byteBuf.put(((argb ushr 16) and 0xFF).toByte()) // R
            byteBuf.put(((argb ushr 8) and 0xFF).toByte())  // G
            byteBuf.put((argb and 0xFF).toByte())           // B
            byteBuf.put(((argb ushr 24) and 0xFF).toByte()) // A
        }
        byteBuf.position(0)

        while (GLES20.glGetError() != GLES20.GL_NO_ERROR) {
            // Clear stale GL errors before texture upload
        }

        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
        val wrapS = if (repeatWrap) GLES20.GL_REPEAT else GLES20.GL_CLAMP_TO_EDGE
        val wrapT = GLES20.GL_CLAMP_TO_EDGE
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, wrapS)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, wrapT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            width,
            height,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            byteBuf
        )
        val uploadErr = GLES20.glGetError()
        if (uploadErr != GLES20.GL_NO_ERROR) {
            Log.w("Boss3DAssets", "glTexImage2D error $uploadErr for ${width}x$height")
        }

        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        val mipErr = GLES20.glGetError()
        if (mipErr != GLES20.GL_NO_ERROR) {
            // If GPU driver rejects mipmap generation, downgrade min filter to GL_LINEAR so texture never renders black
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        return texId
    }

    /**
     * Cleans residual alpha dust on transparent PNGs, or removes connected exterior
     * uniform background (plus enclosed pure-black pockets on dark studio cutouts)
     * via border BFS flood-fill while preserving interior subject detail.
     */
    private fun cleanOrRemoveExteriorBackground(pixels: IntArray, w: Int, h: Int) {
        var borderTotal = 0
        var borderTransparent = 0
        var sumR = 0L
        var sumG = 0L
        var sumB = 0L

        for (x in 0 until w) {
            val top = pixels[x]
            val bot = pixels[(h - 1) * w + x]
            borderTotal += 2
            if (((top ushr 24) and 0xFF) < 32) borderTransparent++
            if (((bot ushr 24) and 0xFF) < 32) borderTransparent++
            sumR += ((top ushr 16) and 0xFF) + ((bot ushr 16) and 0xFF)
            sumG += ((top ushr 8) and 0xFF) + ((bot ushr 8) and 0xFF)
            sumB += (top and 0xFF) + (bot and 0xFF)
        }
        for (y in 1 until h - 1) {
            val left = pixels[y * w]
            val right = pixels[y * w + w - 1]
            borderTotal += 2
            if (((left ushr 24) and 0xFF) < 32) borderTransparent++
            if (((right ushr 24) and 0xFF) < 32) borderTransparent++
            sumR += ((left ushr 16) and 0xFF) + ((right ushr 16) and 0xFF)
            sumG += ((left ushr 8) and 0xFF) + ((right ushr 8) and 0xFF)
            sumB += (left and 0xFF) + (right and 0xFF)
        }

        // Case A: Already has transparent exterior alpha -> clean faint alpha dust only
        if (borderTransparent >= (borderTotal * 0.15f).toInt()) {
            for (i in pixels.indices) {
                val p = pixels[i]
                val a = (p ushr 24) and 0xFF
                if (a <= 8) {
                    pixels[i] = p and 0x00FFFFFF
                }
            }
            return
        }

        // Case B: Opaque border -> check color uniformity along border
        val avgR = (sumR / borderTotal.coerceAtLeast(1)).toInt()
        val avgG = (sumG / borderTotal.coerceAtLeast(1)).toInt()
        val avgB = (sumB / borderTotal.coerceAtLeast(1)).toInt()
        val isPureBlackBackdrop = avgR <= 16 && avgG <= 16 && avgB <= 16
        val matchThreshold = if (isPureBlackBackdrop) 28 else 42

        var uniformMatch = 0
        for (x in 0 until w) {
            if (colorDist(pixels[x], avgR, avgG, avgB) < matchThreshold) uniformMatch++
            if (colorDist(pixels[(h - 1) * w + x], avgR, avgG, avgB) < matchThreshold) uniformMatch++
        }
        for (y in 1 until h - 1) {
            if (colorDist(pixels[y * w], avgR, avgG, avgB) < matchThreshold) uniformMatch++
            if (colorDist(pixels[y * w + w - 1], avgR, avgG, avgB) < matchThreshold) uniformMatch++
        }

        if (uniformMatch >= (borderTotal * 0.55f).toInt()) {
            val total = w * h
            val visited = BooleanArray(total)
            val removed = BooleanArray(total)
            val queue = IntArray(total)
            var head = 0
            var tail = 0
            val bfsThreshold = if (isPureBlackBackdrop) 30 else 44

            fun tryEnqueue(idx: Int) {
                if (visited[idx]) return
                visited[idx] = true
                if (colorDist(pixels[idx], avgR, avgG, avgB) <= bfsThreshold) {
                    removed[idx] = true
                    queue[tail++] = idx
                }
            }

            for (x in 0 until w) {
                tryEnqueue(x)
                tryEnqueue((h - 1) * w + x)
            }
            for (y in 1 until h - 1) {
                tryEnqueue(y * w)
                tryEnqueue(y * w + w - 1)
            }

            while (head < tail) {
                val idx = queue[head++]
                val x = idx % w
                val y = idx / w
                if (x > 0) tryEnqueue(idx - 1)
                if (x + 1 < w) tryEnqueue(idx + 1)
                if (y > 0) tryEnqueue(idx - w)
                if (y + 1 < h) tryEnqueue(idx + w)
            }

            for (i in 0 until total) {
                if (removed[i]) {
                    pixels[i] = pixels[i] and 0x00FFFFFF
                } else if (isPureBlackBackdrop) {
                    val p = pixels[i]
                    val r = (p ushr 16) and 0xFF
                    val g = (p ushr 8) and 0xFF
                    val b = p and 0xFF
                    val maxCh = max(r, max(g, b))
                    if (maxCh <= 10) {
                        // Clear enclosed pure-black gaps (between legs/wings/weapons)
                        pixels[i] = p and 0x00FFFFFF
                    } else if (maxCh <= 24) {
                        val featherA = ((maxCh - 10) * 255 / 14).coerceIn(32, 235)
                        pixels[i] = (featherA shl 24) or (p and 0x00FFFFFF)
                    }
                } else {
                    val x = i % w
                    val y = i / w
                    val touchesRemoved =
                        (x > 0 && removed[i - 1]) ||
                            (x + 1 < w && removed[i + 1]) ||
                            (y > 0 && removed[i - w]) ||
                            (y + 1 < h && removed[i + w])
                    if (touchesRemoved) {
                        val d = colorDist(pixels[i], avgR, avgG, avgB)
                        val featherA = ((d - 28).coerceIn(0, 60) * 255 / 60).coerceIn(48, 220)
                        pixels[i] = (featherA shl 24) or (pixels[i] and 0x00FFFFFF)
                    }
                }
            }
        } else {
            // Full-bleed fallback artwork -> smooth elliptical vignette mask
            val halfW = (w - 1) * 0.5f
            val halfH = (h - 1) * 0.5f
            for (y in 0 until h) {
                val ny = (y - halfH) / halfH
                val rowOff = y * w
                for (x in 0 until w) {
                    val nx = (x - halfW) / halfW
                    val r = hypot(nx.toDouble(), ny.toDouble()).toFloat()
                    val t = ((1f - r) / 0.34f).coerceIn(0f, 1f)
                    val vigAlpha = (t * t * (3f - 2f * t) * 255f).toInt().coerceIn(0, 255)
                    val p = pixels[rowOff + x]
                    val origA = (p ushr 24) and 0xFF
                    val finalA = min(origA, vigAlpha)
                    pixels[rowOff + x] = (finalA shl 24) or (p and 0x00FFFFFF)
                }
            }
        }
    }

    private fun colorDist(argb: Int, r0: Int, g0: Int, b0: Int): Int {
        val r = (argb ushr 16) and 0xFF
        val g = (argb ushr 8) and 0xFF
        val b = argb and 0xFF
        return max(abs(r - r0), max(abs(g - g0), abs(b - b0)))
    }

    /**
     * Propagates nearest opaque/semi-opaque RGB into neighboring alpha=0 texels
     * so hardware GL_LINEAR and mipmaps never blend against black or white background fringe.
     */
    private fun dilateRgbIntoTransparentTexels(pixels: IntArray, w: Int, h: Int, passes: Int) {
        val scratch = IntArray(pixels.size)
        System.arraycopy(pixels, 0, scratch, 0, pixels.size)
        for (pass in 0 until passes) {
            val src = if (pass % 2 == 0) pixels else scratch
            val dst = if (pass % 2 == 0) scratch else pixels
            for (y in 0 until h) {
                val rowOff = y * w
                for (x in 0 until w) {
                    val idx = rowOff + x
                    val p = src[idx]
                    val a = (p ushr 24) and 0xFF
                    if (a > 0) {
                        dst[idx] = p
                        continue
                    }
                    var bestRgb = 0
                    var bestAlpha = 0
                    for (dy in -1..1) {
                        val ny = y + dy
                        if (ny < 0 || ny >= h) continue
                        val nRow = ny * w
                        for (dx in -1..1) {
                            if (dx == 0 && dy == 0) continue
                            val nx = x + dx
                            if (nx < 0 || nx >= w) continue
                            val np = src[nRow + nx]
                            val na = (np ushr 24) and 0xFF
                            if (na > bestAlpha) {
                                bestAlpha = na
                                bestRgb = np and 0x00FFFFFF
                            }
                        }
                    }
                    dst[idx] = if (bestAlpha > 0) bestRgb else p
                }
            }
        }
        if (passes % 2 != 0) {
            System.arraycopy(scratch, 0, pixels, 0, pixels.size)
        }
    }
}

/**
 * High-quality anti-aliased Compose Image slot using FilterQuality.High
 * with multi-tier game asset lookup and smooth radial glow fallback.
 */
@Composable
internal fun Boss3DAssetIcon(
    primaryAsset: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    secondaryAsset: String? = null,
    @DrawableRes drawableRes: Int? = null,
    maxDimension: Int = 256,
    contentScale: ContentScale = ContentScale.Fit,
    fallbackGlowColor: Color = Color(0xFFE09658)
) {
    val context = LocalContext.current
    val bmp = remember(primaryAsset, secondaryAsset, drawableRes, maxDimension) {
        Boss3DBitmapCache.loadUiBitmap(
            context = context,
            primaryAsset = primaryAsset,
            secondaryAsset = secondaryAsset,
            drawableRes = drawableRes,
            maxDimension = maxDimension
        )
    }

    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = contentDescription,
            contentScale = contentScale,
            filterQuality = FilterQuality.High,
            modifier = modifier
        )
    } else {
        Canvas(modifier = modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        fallbackGlowColor.copy(alpha = 0.95f),
                        fallbackGlowColor.copy(alpha = 0.42f),
                        Color.Transparent
                    )
                ),
                radius = size.minDimension * 0.46f
            )
        }
    }
}
