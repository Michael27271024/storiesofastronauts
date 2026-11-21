package com.example.astronautstories.engine

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.astronautstories.assets_loader.NativeBitmapRegistry
import com.example.astronautstories.domain.model.EnemyKind
import kotlin.math.roundToInt

@Composable
fun GameCanvasView(
    sim: GameSimulation,
    frameTick: Long,
    bitmapRegistry: NativeBitmapRegistry,
    modifier: Modifier = Modifier
) {
    val bgBitmap = bitmapRegistry.getBitmap(sim.stageDef.bgAssetPath, 1024)

    Canvas(modifier = modifier.fillMaxSize()) {
        @Suppress("UNUSED_EXPRESSION")
        frameTick

        // Strictly horizontal landscape scaling locked to 720f virtual height
        val scale = size.height / sim.worldHeight
        val viewportWorldWidth = size.width / scale

        // 1. Full-screen Sky Gradient + Cosmic Backdrop
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(sim.chapter.skyTopHex), Color(sim.chapter.skyBottomHex))
            )
        )
        if (bgBitmap != null) {
            drawImage(
                image = bgBitmap,
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                alpha = 0.34f
            )
        }

        // 2. Full-screen Horizontal Parallax Stars
        for (star in sim.stars) {
            val sx = ((star.x - sim.cameraX * star.parallax) % viewportWorldWidth + viewportWorldWidth) % viewportWorldWidth
            drawCircle(
                color = Color.White.copy(alpha = star.alpha),
                radius = star.size * scale,
                center = Offset(sx * scale, star.y * scale)
            )
        }

        withTransform({
            scale(scale, scale, pivot = Offset.Zero)
            translate(left = -sim.cameraX, top = 0f)
        }) {
            // 3. Planetary Ground Surface & Elevated Horizontal Platforms
            val groundColor = Color(sim.chapter.groundColorHex)
            val accentColor = Color(sim.chapter.accentHex)
            for ((index, p) in sim.platforms.withIndex()) {
                drawRect(
                    color = groundColor,
                    topLeft = Offset(p.x, p.y),
                    size = Size(p.width, p.height)
                )
                drawRect(
                    color = accentColor.copy(alpha = if (index == 0) 0.65f else 0.9f),
                    topLeft = Offset(p.x, p.y),
                    size = Size(p.width, 5f)
                )
            }

            // Planetary landmarks using real images from assets/images/
            val landmarkPath = when (sim.stageDef.chapterIndex) {
                1, 5 -> "images/rocket_saturn.png"
                4 -> "images/iran_flag_on_saturn.png"
                else -> "images/alien_base.png"
            }
            val landmarkBmp = bitmapRegistry.getBitmap(landmarkPath, 320)
            if (landmarkBmp != null) {
                drawSpriteOrRect(
                    bmp = landmarkBmp,
                    x = sim.worldWidth - 270f,
                    y = sim.groundY - 148f,
                    w = 180f,
                    h = 148f,
                    flipHorizontal = false,
                    fallbackColor = groundColor,
                    alpha = 0.85f
                )
            }

            // Planetary craters along the ground floor
            var cx = 90f
            while (cx < sim.worldWidth - 90f) {
                drawOval(
                    color = Color.Black.copy(alpha = 0.22f),
                    topLeft = Offset(cx, sim.groundY + 18f),
                    size = Size(72f, 18f)
                )
                cx += 260f
            }

            // 4. Pickups
            for (item in sim.pickups) {
                val path = when (item.kind) {
                    PickupKind.GUN -> "images/gun.png"
                    PickupKind.BATTERY -> "images/battery.png"
                    PickupKind.POWERFUL_BATTERY -> "images/powerful_battery.png"
                    PickupKind.ROCKET_PART -> "images/rocket_part.png"
                    PickupKind.AMMO_PACK -> "images/ammo_pack.png"
                    PickupKind.HEALTH_PACK -> "images/health_pack.png"
                    PickupKind.HOSTAGE -> "images/alien_hostage.png"
                    PickupKind.CRYSTAL -> "images/powerful_battery.png"
                }
                val bmp = bitmapRegistry.getBitmap(path, 128)
                if (item.kind == PickupKind.ROCKET_PART) {
                    drawCircle(
                        color = Color(0xFFE09658).copy(alpha = 0.34f),
                        radius = 44f,
                        center = Offset(item.x + item.width / 2f, item.y + item.height / 2f)
                    )
                }
                drawSpriteOrRect(
                    bmp = bmp,
                    x = item.x - 6f,
                    y = item.y - 4f,
                    w = item.width + 12f,
                    h = item.height + 8f,
                    flipHorizontal = false,
                    fallbackColor = Color(0xFFC47B42),
                    bottomAlign = false
                )
            }

            // 5. AI Wingmates using real astronaut.png & squad sprites
            val mateLeaderBmp = bitmapRegistry.getBitmap("images/astronaut.png", 160)
            val mateBmp = bitmapRegistry.getBitmap("images/player_idle_gun.png", 160)
            for (tm in sim.teammates) {
                val chosenMateBmp = if (tm.offsetIndex == 0 && mateLeaderBmp != null) mateLeaderBmp else mateBmp
                drawSpriteOrRect(
                    bmp = chosenMateBmp,
                    x = tm.x,
                    y = tm.y,
                    w = 56f,
                    h = 68f,
                    flipHorizontal = !tm.facingRight,
                    fallbackColor = Color(0xFFF5E6C8),
                    alpha = 0.9f,
                    bottomAlign = true
                )
            }

            // 6. Player Character & Jetpack Flame
            if (sim.player.isJetpacking) {
                drawOval(
                    color = Color(0xFFE09658).copy(alpha = 0.78f),
                    topLeft = Offset(sim.player.x + 12f, sim.player.y + sim.player.height - 6f),
                    size = Size(28f, 34f)
                )
            }

            val stateKey = when {
                sim.player.isJumping -> "jump"
                sim.player.isCrouching -> "crouch"
                sim.player.vx != 0f -> "walk"
                else -> "idle"
            } + if (sim.player.hasGun) "_gun" else "_no_gun"

            val playerBmp = bitmapRegistry.getBitmap("images/player_${stateKey}.png", 180)
            val playerAlpha = if (sim.player.invulnTimer > 0f && ((sim.player.invulnTimer * 12).toInt() % 2 == 0)) 0.45f else 1f
            drawSpriteOrRect(
                bmp = playerBmp,
                x = sim.player.x,
                y = sim.player.y,
                w = sim.player.width + 14f,
                h = sim.player.height + 4f,
                flipHorizontal = !sim.player.facingRight,
                fallbackColor = Color(sim.hero.accentColor),
                alpha = playerAlpha,
                bottomAlign = true
            )

            if (sim.player.shield > 0) {
                drawCircle(
                    color = Color(0xFFC47B42).copy(alpha = 0.32f),
                    radius = 44f,
                    center = Offset(sim.player.x + sim.player.width / 2f, sim.player.y + sim.player.height / 2f)
                )
            }

            // 7. Enemies
            for (e in sim.enemies) {
                val ePath = when (e.kind) {
                    EnemyKind.ZOMBIE -> "images/zombie.png"
                    EnemyKind.ALIEN_SOLDIER -> "images/alien_soldier.png"
                    EnemyKind.FLYER -> "images/flyer.png"
                    EnemyKind.SPLITTER, EnemyKind.MINI_SPLITTER -> "images/splitter.png"
                    EnemyKind.ALIEN_SHIP -> "images/alien_ship.png"
                    EnemyKind.LASER_TURRET -> "images/alien_ship2.png"
                    EnemyKind.BOSS_OVERLORD -> "images/alien_boss.png"
                }
                val eBmp = bitmapRegistry.getBitmap(ePath, if (e.kind == EnemyKind.BOSS_OVERLORD) 320 else 160)
                val groundAligned = e.kind != EnemyKind.FLYER && e.kind != EnemyKind.ALIEN_SHIP
                drawSpriteOrRect(
                    bmp = eBmp,
                    x = e.x,
                    y = e.y,
                    w = e.width + 8f,
                    h = e.height + 4f,
                    flipHorizontal = e.dir > 0f,
                    fallbackColor = Color(0xFFE05252),
                    bottomAlign = groundAligned
                )

                if (e.hp < e.maxHp) {
                    val ratio = (e.hp.toFloat() / e.maxHp.toFloat()).coerceIn(0f, 1f)
                    drawRect(Color.Black.copy(alpha = 0.65f), Offset(e.x, e.y - 10f), Size(e.width, 5f))
                    drawRect(Color(0xFFE05252), Offset(e.x, e.y - 10f), Size(e.width * ratio, 5f))
                }
            }

            // 8. Bullets & Particles
            for (b in sim.bullets) {
                drawCircle(
                    color = Color(b.colorHex).copy(alpha = 0.35f),
                    radius = maxOf(b.width, b.height) * 0.7f,
                    center = Offset(b.x + b.width / 2f, b.y + b.height / 2f)
                )
                drawRect(
                    color = Color(b.colorHex),
                    topLeft = Offset(b.x, b.y),
                    size = Size(b.width, b.height)
                )
            }

            for (pt in sim.particles) {
                val alpha = (pt.life / pt.maxLife).coerceIn(0f, 1f)
                drawCircle(
                    color = Color(pt.colorHex).copy(alpha = alpha),
                    radius = pt.size,
                    center = Offset(pt.x, pt.y)
                )
            }
        }

        // 9. Mercury & Citadel Dynamic Darkness / Flashlight Beam
        if (sim.stageDef.hasDynamicDarkness) {
            val px = (sim.player.x - sim.cameraX + sim.player.width / 2f) * scale
            val py = (sim.player.y + sim.player.height / 2f) * scale
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xAA020612),
                        Color(0xE8010308)
                    ),
                    center = Offset(px, py),
                    radius = size.height * 0.72f
                )
            )
        }
    }
}

private fun DrawScope.drawSpriteOrRect(
    bmp: ImageBitmap?,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    flipHorizontal: Boolean,
    fallbackColor: Color,
    alpha: Float = 1f,
    bottomAlign: Boolean = true
) {
    if (bmp == null) {
        drawRect(fallbackColor.copy(alpha = alpha), Offset(x, y), Size(w, h))
        return
    }
    val bmpW = bmp.width.coerceAtLeast(1).toFloat()
    val bmpH = bmp.height.coerceAtLeast(1).toFloat()
    val fitScale = minOf(w / bmpW, h / bmpH)
    val drawW = (bmpW * fitScale).coerceAtLeast(1f)
    val drawH = (bmpH * fitScale).coerceAtLeast(1f)
    val drawX = x + (w - drawW) / 2f
    val drawY = if (bottomAlign) (y + h - drawH) else (y + (h - drawH) / 2f)

    if (flipHorizontal) {
        withTransform({
            scale(scaleX = -1f, scaleY = 1f, pivot = Offset(drawX + drawW / 2f, drawY + drawH / 2f))
        }) {
            drawImage(
                image = bmp,
                dstOffset = IntOffset(drawX.roundToInt(), drawY.roundToInt()),
                dstSize = IntSize(drawW.roundToInt(), drawH.roundToInt()),
                alpha = alpha
            )
        }
    } else {
        drawImage(
            image = bmp,
            dstOffset = IntOffset(drawX.roundToInt(), drawY.roundToInt()),
            dstSize = IntSize(drawW.roundToInt(), drawH.roundToInt()),
            alpha = alpha
        )
    }
}
