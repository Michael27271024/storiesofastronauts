package com.example.astronautstories.boss3d

import com.example.astronautstories.domain.model.GameCatalog
import com.example.astronautstories.domain.model.GameMode
import com.example.astronautstories.domain.model.GraphicsQuality
import com.example.astronautstories.domain.model.HeroId
import com.example.astronautstories.domain.model.SkillType
import com.example.astronautstories.domain.model.StageDefinition
import com.example.astronautstories.domain.model.WeaponType
import com.example.astronautstories.engine.ArenaPhase
import com.example.astronautstories.engine.HudSnapshot
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

enum class Boss3DCombatState {
    BRIEFING,
    INTRO_ENTRANCE,
    ACTIVE_COMBAT,
    STORY_CHOICE,
    VICTORY_FINALE,
    DEFEAT
}

data class Boss3DInput(
    val moveX: Float = 0f,     // -1f (left) .. 1f (right) in camera-relative XZ
    val moveY: Float = 0f,     // -1f (down/back) .. 1f (up/forward) in camera-relative XZ
    val jump: Boolean = false,
    val shoot: Boolean = false,
    val dashTrigger: Boolean = false,
    val specialTrigger: Boolean = false,
    val aimYawDelta: Float = 0f,
    val aimPitchDelta: Float = 0f
)

class PooledBullet3D {
    var active = false
    var x = 0f
    var y = 0f
    var z = 0f
    var vx = 0f
    var vy = 0f
    var vz = 0f
    var radius = 0.28f
    var damage = 1
    var fromPlayer = true
    var life = 0f
    var r = 1f
    var g = 1f
    var b = 0.2f
}

class PooledTelegraph3D {
    var active = false
    var isBeam = false // false = circular meteor/impact zone, true = linear sweep beam
    var x = 0f
    var z = 0f
    var yaw = 0f
    var length = 18f
    var radius = 2.6f
    var timer = 0f
    var maxTimer = 1.1f
    var firingActive = false
    var damageApplied = false
}

class PooledShockRing3D {
    var active = false
    var x = 0f
    var y = 0.35f
    var z = 0f
    var radius = 0.6f
    var maxRadius = 22f
    var expandSpeed = 10f
    var fromPlayer = false
    var hitPlayer = false
    var r = 1f
    var g = 0.3f
    var b = 0.2f
}

class PooledParticle3D {
    var active = false
    var x = 0f
    var y = 0f
    var z = 0f
    var vx = 0f
    var vy = 0f
    var vz = 0f
    var life = 0f
    var maxLife = 0.5f
    var size = 0.24f
    var r = 1f
    var g = 0.6f
    var b = 0.2f
}

class PooledPickup3D {
    var active = false
    var isHealth = false // false = ammo cell, true = med cell
    var x = 0f
    var z = 0f
    var bobPhase = 0f
}

class PooledSatellite3D {
    var active = false
    var angle = 0f
    var x = 0f
    var y = 0f
    var z = 0f
    var fireTimer = 1.8f
}

class CoverPillar3D {
    var x = 0f
    var z = 0f
    var radius = 1.25f
    var height = 2.2f
}

/**
 * Fixed-capacity pre-allocated render state snapshot copied under a micro-lock
 * so OpenGL thread reads zero-allocation deterministic data without tearing.
 */
class Boss3DRenderBuffer {
    var stageNumber = 5
    var combatState = Boss3DCombatState.BRIEFING
    var timeSec = 0f
    var arenaRadius = 18f
    var bossScale = 1.65f
    var bossX = 0f
    var bossY = 3.2f
    var bossZ = -8f
    var bossYaw = 0f
    var bossPhase = 1
    var bossHpRatio = 1f
    var bossHitFlash = 0f
    var hasOrbitalRings = false
    var hasDualWingCannons = false
    var isDarkEclipse = false
    var isGrandFinale = false

    var playerX = 0f
    var playerY = 0f
    var playerZ = 9f
    var playerYaw = 0f
    var playerSpeed = 0f
    var playerMoveRightDot = 0f
    var playerAimRightDot = 1f
    var playerLandImpulse = 0f
    var playerInvuln = false
    var playerJetpacking = false
    var playerHasShield = false
    var playerDashing = false

    var droneActive = false
    var droneX = 0f
    var droneY = 2.2f
    var droneZ = 9f

    var camYaw = 0f
    var camPitch = 0.34f
    var screenShake = 0f

    val skyTop = floatArrayOf(0.03f, 0.05f, 0.11f, 1f)
    val skyBottom = floatArrayOf(0.09f, 0.12f, 0.22f, 1f)
    val deckColor = floatArrayOf(0.28f, 0.30f, 0.36f, 1f)
    val deckTrim = floatArrayOf(0f, 0.85f, 1f, 1f)
    val planetColor = floatArrayOf(0.22f, 0.56f, 0.92f, 1f)
    var hasPlanetRing = false
    val planetRingColor = floatArrayOf(0.95f, 0.80f, 0.45f, 1f)
    val hullColor = floatArrayOf(0.45f, 0.49f, 0.56f, 1f)
    val coreColor = floatArrayOf(0.05f, 0.90f, 1f, 1f)
    val hazardColor = floatArrayOf(1f, 0.32f, 0.26f, 1f)
    val heroAccent = floatArrayOf(0f, 0.85f, 1f, 1f)

    // Packed primitive arrays for zero-allocation GL draw loops
    var pillarCount = 0
    val pillarXZR = FloatArray(8 * 3)

    var satelliteCount = 0
    val satelliteXYZ = FloatArray(6 * 3)

    var pickupCount = 0
    val pickupXZType = FloatArray(10 * 3) // x, z, isHealth (1f/0f)

    var bulletCount = 0
    val bulletData = FloatArray(144 * 7) // x, y, z, r, g, b, radius

    var shockCount = 0
    val shockData = FloatArray(12 * 7) // x, y, z, radius, r, g, b

    var telegraphCount = 0
    val telegraphData = FloatArray(16 * 7) // isBeam(1/0), x, z, yaw, radiusOrWidth, length, progress(0..1 + firing>1)

    var particleCount = 0
    val particleData = FloatArray(140 * 7) // x, y, z, r, g, b, size
}

class Boss3DSimulation(
    val mode: GameMode,
    val stageDef: StageDefinition,
    val hero: HeroId,
    val weapon: WeaponType,
    val skillLevels: Map<SkillType, Int>,
    val graphicsQuality: GraphicsQuality,
    val bonusMedkits: Int = 0,
    val bonusAmmo: Int = 0,
    val bonusShield: Boolean = false,
    private val onSfx: (String) -> Unit
) {
    val spec: Boss3DSpec = Boss3DCatalog.getSpec(stageDef.stageNumber)
    val chapter = GameCatalog.chapters[(stageDef.chapterIndex - 1).coerceIn(0, GameCatalog.chapters.lastIndex)]

    private val speedSkill = skillLevels[SkillType.SPEED] ?: 0
    private val fireSkill = skillLevels[SkillType.FIREPOWER] ?: 0
    private val armorSkill = skillLevels[SkillType.ARMOR] ?: 0
    private val oxygenSkill = skillLevels[SkillType.OXYGEN_CAP] ?: 0
    private val luckSkill = skillLevels[SkillType.LUCK] ?: 0
    private val droneSkill = skillLevels[SkillType.DRONE] ?: 0

    private val maxParticlePool = when (graphicsQuality) {
        GraphicsQuality.LOW -> 28
        GraphicsQuality.MEDIUM -> 72
        GraphicsQuality.HIGH -> 136
    }

    var phase: ArenaPhase = ArenaPhase.BRIEFING
        private set
    var combatState: Boss3DCombatState = Boss3DCombatState.BRIEFING
        private set

    // Player 3D state
    var playerX = 0f
    var playerY = 0f
    var playerZ = spec.arenaRadius * 0.62f
    var playerVy = 0f
    var playerYaw = 0f
    private var playerVelX = 0f
    private var playerVelZ = 0f
    private var playerLandImpulse = 0f
    private var walkDustAccum = 0f
    var playerHealth = maxOf(5, hero.bonusHp) + armorSkill + bonusMedkits
    val playerMaxHealth = playerHealth
    var playerMaxShield = 2 + oxygenSkill
    var playerShield = if (bonusShield) playerMaxShield else (oxygenSkill / 2)
    var playerAmmo = 65 + luckSkill * 10 + bonusAmmo
    var playerInvulnTimer = 0f
    var playerDashTimer = 0f
    var playerDashCooldown = 0f
    var dashDirX = 0f
    var dashDirZ = -1f
    var specialCooldown = 0f
    private var fireCooldown = 0f
    private var ammoRegenTimer = 0f

    // Camera state (camera orbits smoothly behind astronaut looking toward arena center/boss)
    var cameraYaw = 0f
    var cameraPitch = 0.34f
    private var screenShakeTimer = 0f

    // Boss 3D state
    val bossMaxHp: Int = spec.baseHp
    var bossHp: Int = spec.baseHp
    var bossPhase: Int = 1
    var bossX = 0f
    var bossY = 3.5f
    var bossZ = -spec.arenaRadius * 0.42f
    var bossYaw = 0f
    var bossHitFlashTimer = 0f
    var bossKilled = false
    private var bossMoveAngle = 0f
    private var bossAttackTimer = 1.4f
    private var patternIndex = 0
    private var spiralAngle = 0f
    private var introTimer = 0f

    // Support Drone
    private var droneTimer = 1.4f
    private var droneAngle = 0f
    private var droneX = 0f
    private var droneY = 2.4f
    private var droneZ = playerZ

    // Match stats
    var score: Long = 0L
    var enemiesKilled: Int = 0
    var shotsFired: Int = 0
    var shotsHit: Int = 0
    var elapsedSeconds: Float = 0f
    var timeRemaining: Float = (stageDef.timeLimitSeconds + oxygenSkill * 20).toFloat()
    var betrayalChosen: Boolean = false
    var statusBanner: String = spec.introLineFa
    private var statusBannerTimer = 4.0f
    private var pickupSpawnTimer = 4.5f
    private var totalSimTime = 0f

    // Fixed Object Pools (Zero allocations during step())
    private val playerBullets = Array(48) { PooledBullet3D() }
    private val enemyBullets = Array(96) { PooledBullet3D() }
    private val telegraphs = Array(16) { PooledTelegraph3D() }
    private val shockRings = Array(12) { PooledShockRing3D() }
    private val particles = Array(140) { PooledParticle3D() }
    private val pickups = Array(10) { PooledPickup3D() }
    private val satellites = Array(6) { PooledSatellite3D() }
    private val pillars = Array(8) { CoverPillar3D() }
    private var activePillarCount = 0

    // Double-buffered render snapshot
    private val renderLock = Any()
    private val stagingBuffer = Boss3DRenderBuffer()
    private val publicBuffer = Boss3DRenderBuffer()

    init {
        setupArena()
        writeRenderBuffer(stagingBuffer)
        synchronized(renderLock) {
            copyRenderBuffer(stagingBuffer, publicBuffer)
        }
    }

    private fun setupArena() {
        val count = spec.coverPillarCount.coerceIn(0, pillars.size)
        activePillarCount = count
        val ringR = spec.arenaRadius * 0.56f
        for (i in 0 until count) {
            val angle = (i.toFloat() / count.toFloat()) * (2f * PI.toFloat()) + 0.45f
            pillars[i].x = cos(angle) * ringR
            pillars[i].z = sin(angle) * ringR
            pillars[i].radius = 1.25f
            pillars[i].height = 2.4f
        }

        val satCount = spec.satelliteCount.coerceIn(0, satellites.size)
        for (i in 0 until satellites.size) {
            satellites[i].active = i < satCount
            satellites[i].angle = (i.toFloat() / max(1, satCount)) * (2f * PI.toFloat())
            satellites[i].fireTimer = 1.6f + i * 0.55f
        }
        updateSatellitePositions()

        spawnPickup(isHealth = false, x = -spec.arenaRadius * 0.35f, z = spec.arenaRadius * 0.28f)
        if (spec.stageNumber >= 20) {
            spawnPickup(isHealth = true, x = spec.arenaRadius * 0.35f, z = spec.arenaRadius * 0.28f)
        }
    }

    fun startMission() {
        if (phase != ArenaPhase.BRIEFING) return
        phase = ArenaPhase.PLAYING
        combatState = Boss3DCombatState.INTRO_ENTRANCE
        introTimer = 1.35f
        statusBanner = spec.introLineFa
        statusBannerTimer = 3.5f
        safeSfx("boss_hit")
        publishRenderFrame()
    }

    fun resolveStoryChoice(betrayal: Boolean) {
        betrayalChosen = betrayal
        combatState = Boss3DCombatState.VICTORY_FINALE
        phase = ArenaPhase.VICTORY
        publishRenderFrame()
    }

    fun copyLatestRenderBuffer(target: Boss3DRenderBuffer) {
        synchronized(renderLock) {
            copyRenderBuffer(publicBuffer, target)
        }
    }

    fun step(dtRaw: Float, input: Boss3DInput) {
        val dt = dtRaw.coerceIn(0.001f, 0.05f)
        totalSimTime += dt

        if (phase != ArenaPhase.PLAYING) {
            updateParticles(dt)
            publishRenderFrame()
            return
        }

        if (combatState == Boss3DCombatState.BRIEFING) {
            combatState = Boss3DCombatState.INTRO_ENTRANCE
            introTimer = 1.35f
        }

        if (statusBannerTimer > 0f) {
            statusBannerTimer = (statusBannerTimer - dt).coerceAtLeast(0f)
            if (statusBannerTimer <= 0f) statusBanner = ""
        }
        if (screenShakeTimer > 0f) {
            screenShakeTimer = (screenShakeTimer - dt).coerceAtLeast(0f)
        }
        if (bossHitFlashTimer > 0f) {
            bossHitFlashTimer = (bossHitFlashTimer - dt).coerceAtLeast(0f)
        }

        // Camera control + gentle auto-align toward boss if player is not dragging camera
        if (abs(input.aimYawDelta) > 0.0001f) {
            cameraYaw += input.aimYawDelta
        } else {
            val desiredYaw = atan2(bossX - playerX, -(bossZ - playerZ))
            val diff = normalizeAngle(desiredYaw - cameraYaw)
            cameraYaw += diff * min(1f, dt * 1.45f)
        }
        cameraPitch = (cameraPitch + input.aimPitchDelta).coerceIn(0.16f, 0.68f)

        // Intro entrance phase (1.35s smooth hover rise-and-settle with zero vertical pop)
        if (combatState == Boss3DCombatState.INTRO_ENTRANCE) {
            introTimer -= dt
            val t = (1f - (introTimer / 1.35f)).coerceIn(0f, 1f)
            bossY = 3.5f + sin(t * PI.toFloat()) * 1.45f
            bossYaw = atan2(playerX - bossX, playerZ - bossZ)
            updateSatellitePositions()
            if (introTimer <= 0f) {
                combatState = Boss3DCombatState.ACTIVE_COMBAT
                bossY = 3.5f
                statusBanner = ""
            }
            publishRenderFrame()
            return
        }

        if (combatState != Boss3DCombatState.ACTIVE_COMBAT) {
            publishRenderFrame()
            return
        }

        elapsedSeconds += dt
        timeRemaining -= dt
        if (timeRemaining <= 0f) {
            timeRemaining = 0f
            triggerDefeat("اکسیژن و زمان نبرد با باس به پایان رسید!")
            publishRenderFrame()
            return
        }

        updatePlayer(dt, input)
        if (combatState != Boss3DCombatState.ACTIVE_COMBAT) {
            publishRenderFrame()
            return
        }

        updateBoss(dt)
        updateSatellites(dt)
        updateTelegraphs(dt)
        updateShockRings(dt)
        updateBullets(dt)
        updatePickups(dt)
        updateParticles(dt)

        if (bossHp <= 0 || bossKilled) {
            handleBossDefeated()
        }

        publishRenderFrame()
    }

    private fun updatePlayer(dt: Float, input: Boss3DInput) {
        if (playerInvulnTimer > 0f) playerInvulnTimer = (playerInvulnTimer - dt).coerceAtLeast(0f)
        if (playerDashCooldown > 0f) playerDashCooldown = (playerDashCooldown - dt).coerceAtLeast(0f)
        if (specialCooldown > 0f) specialCooldown = (specialCooldown - dt).coerceAtLeast(0f)
        if (fireCooldown > 0f) fireCooldown = (fireCooldown - dt).coerceAtLeast(0f)

        // Camera-relative movement on XZ deck
        val sinYaw = sin(cameraYaw)
        val cosYaw = cos(cameraYaw)
        val forwardX = sinYaw
        val forwardZ = -cosYaw
        val rightX = cosYaw
        val rightZ = sinYaw

        var wishX = rightX * input.moveX + forwardX * input.moveY
        var wishZ = rightZ * input.moveX + forwardZ * input.moveY
        val wishLen = hypot(wishX, wishZ)
        if (wishLen > 1f) {
            wishX /= wishLen
            wishZ /= wishLen
        }

        // Dash trigger
        if (input.dashTrigger && playerDashCooldown <= 0f && playerDashTimer <= 0f) {
            playerDashTimer = 0.22f
            playerDashCooldown = 1.15f
            playerInvulnTimer = max(playerInvulnTimer, 0.28f)
            if (wishLen > 0.15f) {
                dashDirX = wishX / wishLen
                dashDirZ = wishZ / wishLen
            } else {
                dashDirX = forwardX
                dashDirZ = forwardZ
            }
            safeSfx("jump")
            spawnBurst(playerX, playerY + 0.8f, playerZ, spec.deckTrimColor, 8)
        }

        val moveSpeed = (7.2f * hero.bonusSpeed) + (speedSkill * 0.48f)
        if (playerDashTimer > 0f) {
            playerDashTimer -= dt
            playerVelX = dashDirX * (moveSpeed * 2.4f)
            playerVelZ = dashDirZ * (moveSpeed * 2.4f)
        } else {
            val targetVx = wishX * moveSpeed
            val targetVz = wishZ * moveSpeed
            val accel = if (wishLen > 0.05f) min(1f, dt * 15f) else min(1f, dt * 13f)
            playerVelX += (targetVx - playerVelX) * accel
            playerVelZ += (targetVz - playerVelZ) * accel
            if (wishLen <= 0.02f && hypot(playerVelX, playerVelZ) < 0.08f) {
                playerVelX = 0f
                playerVelZ = 0f
            }
        }
        playerX += playerVelX * dt
        playerZ += playerVelZ * dt

        // Keep player inside circular 3D arena deck
        val maxWalkR = spec.arenaRadius - 1.15f
        val distCenter = hypot(playerX, playerZ)
        if (distCenter > maxWalkR && distCenter > 0.001f) {
            val scale = maxWalkR / distCenter
            playerX *= scale
            playerZ *= scale
        }

        // Collide softly with cover pillars
        for (i in 0 until activePillarCount) {
            val p = pillars[i]
            val dx = playerX - p.x
            val dz = playerZ - p.z
            val minD = p.radius + 0.65f
            val dSq = dx * dx + dz * dz
            if (dSq < minD * minD && dSq > 0.0001f) {
                val d = sqrt(dSq)
                playerX = p.x + (dx / d) * minD
                playerZ = p.z + (dz / d) * minD
            }
        }

        // Vertical Jetpack Jump & natural landing cushion
        val prevY = playerY
        if (playerLandImpulse > 0f) {
            playerLandImpulse = (playerLandImpulse - dt * 4.5f).coerceAtLeast(0f)
        }
        val gravity = 22f * chapter.gravity.coerceIn(0.32f, 0.56f) * 1.9f
        if (input.jump) {
            if (playerY <= 0.05f) {
                playerVy = 9.4f + speedSkill * 0.35f
            } else if (playerVy < 3.6f) {
                playerVy += 19f * dt
            }
        }
        playerVy -= gravity * dt
        playerY += playerVy * dt
        if (playerY <= 0f) {
            if (prevY > 0.22f) {
                playerLandImpulse = 1f
                spawnBurst(playerX, 0.08f, playerZ, spec.deckColor, 4)
            }
            playerY = 0f
            playerVy = 0f
        } else if (playerY > 5.8f) {
            playerY = 5.8f
            if (playerVy > 0f) playerVy = 0f
        }

        // Rhythmic footstep dust when grounded and running
        val currentSpeed = hypot(playerVelX, playerVelZ)
        if (playerY <= 0.05f && currentSpeed > 1.2f && playerDashTimer <= 0f) {
            walkDustAccum += currentSpeed * dt
            if (walkDustAccum >= 1.85f) {
                walkDustAccum = 0f
                spawnBurst(playerX, 0.06f, playerZ, spec.deckColor, 2)
            }
        } else if (playerY > 0.05f) {
            walkDustAccum = 0f
        }

        // Face toward boss naturally when shooting, or toward movement direction
        val dxBoss = bossX - playerX
        val dzBoss = bossZ - playerZ
        playerYaw = if (input.shoot || wishLen < 0.1f) {
            atan2(dxBoss, dzBoss)
        } else {
            atan2(wishX, wishZ)
        }

        // Emergency recharge if ammo reaches 0 so player can never soft-lock
        if (playerAmmo <= 2) {
            ammoRegenTimer += dt
            if (ammoRegenTimer >= 1.6f) {
                ammoRegenTimer = 0f
                playerAmmo += 8
            }
        } else {
            ammoRegenTimer = 0f
        }

        // Player Weapon Fire (3D smart-aimed bolts toward boss)
        if (input.shoot && fireCooldown <= 0f && playerAmmo > 0) {
            val rateSec = (weapon.fireRateMs / 1000f) * (1f - speedSkill * 0.03f).coerceAtLeast(0.7f)
            fireCooldown = rateSec
            playerAmmo = (playerAmmo - 1).coerceAtLeast(0)
            shotsFired++
            safeSfx("shoot")

            val bDamage = weapon.damage + fireSkill
            val bSpeed = weapon.projectileSpeed * 2.25f
            val tx = bossX
            val ty = bossY + 0.35f
            val tz = bossZ
            val vxRaw = tx - playerX
            val vyRaw = ty - (playerY + 1.15f)
            val vzRaw = tz - playerZ
            val norm = sqrt(vxRaw * vxRaw + vyRaw * vyRaw + vzRaw * vzRaw).coerceAtLeast(0.01f)
            val dirX = vxRaw / norm
            val dirY = vyRaw / norm
            val dirZ = vzRaw / norm

            val spread = weapon.spreadCount.coerceIn(1, 3)
            val r = ((weapon.colorHex shr 16) and 0xFF) / 255f
            val g = ((weapon.colorHex shr 8) and 0xFF) / 255f
            val b = (weapon.colorHex and 0xFF) / 255f

            for (s in 0 until spread) {
                val offsetAngle = if (spread == 1) 0f else (s - (spread - 1) / 2f) * 0.08f
                val c = cos(offsetAngle)
                val sn = sin(offsetAngle)
                val sx = dirX * c - dirZ * sn
                val sz = dirX * sn + dirZ * c
                spawnBullet(
                    pool = playerBullets,
                    x = playerX + dirX * 0.9f,
                    y = playerY + 1.15f,
                    z = playerZ + dirZ * 0.9f,
                    vx = sx * bSpeed,
                    vy = dirY * bSpeed,
                    vz = sz * bSpeed,
                    radius = if (weapon == WeaponType.GRAVITY_LAUNCHER) 0.42f else 0.28f,
                    damage = bDamage,
                    fromPlayer = true,
                    r = r,
                    g = g,
                    b = b
                )
            }
        }

        // Special Shockwave Ability (clears enemy bullets nearby + damages boss!)
        if (input.specialTrigger && specialCooldown <= 0f) {
            specialCooldown = 7.5f
            safeSfx("powerup")
            screenShakeTimer = 0.22f
            spawnShockRing(playerX, 0.3f, playerZ, maxRadius = 16f, expandSpeed = 24f, fromPlayer = true, spec.deckTrimColor)
            // Clear nearby enemy projectiles for tactical defense
            for (eb in enemyBullets) {
                if (eb.active) {
                    val d = hypot(eb.x - playerX, eb.z - playerZ)
                    if (d <= 11f) {
                        eb.active = false
                        spawnBurst(eb.x, eb.y, eb.z, spec.deckTrimColor, 3)
                    }
                }
            }
            val shockDmg = 8 + fireSkill * 2
            damageBoss(shockDmg)
        }

        // Support Drone when DRONE skill > 0 (or Stage 50 squad assist!)
        if (droneSkill > 0 || spec.isGrandFinale) {
            droneAngle += dt * 2.1f
            droneX = playerX + cos(droneAngle) * 1.65f
            droneY = playerY + 2.25f + sin(totalSimTime * 4f) * 0.15f
            droneZ = playerZ + sin(droneAngle) * 1.65f
            droneTimer -= dt
            if (droneTimer <= 0f) {
                droneTimer = (1.6f - droneSkill * 0.15f).coerceAtLeast(0.7f)
                val dx = bossX - droneX
                val dy = bossY - droneY
                val dz = bossZ - droneZ
                val len = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.01f)
                spawnBullet(
                    pool = playerBullets,
                    x = droneX,
                    y = droneY,
                    z = droneZ,
                    vx = (dx / len) * 28f,
                    vy = (dy / len) * 28f,
                    vz = (dz / len) * 28f,
                    radius = 0.22f,
                    damage = 1 + (droneSkill / 2),
                    fromPlayer = true,
                    r = 0.2f,
                    g = 0.95f,
                    b = 1f
                )
            }
        }
    }

    private fun updateBoss(dt: Float) {
        // Compute current phase from remaining HP ratio
        val ratio = bossHp.toFloat() / bossMaxHp.coerceAtLeast(1).toFloat()
        val computedPhase = when (spec.phaseCount) {
            4 -> when {
                ratio <= 0.25f -> 4
                ratio <= 0.50f -> 3
                ratio <= 0.75f -> 2
                else -> 1
            }
            3 -> when {
                ratio <= 0.33f -> 3
                ratio <= 0.66f -> 2
                else -> 1
            }
            else -> if (ratio <= 0.50f) 2 else 1
        }

        if (computedPhase > bossPhase) {
            bossPhase = computedPhase
            safeSfx("boss_hit")
            screenShakeTimer = 0.35f
            spawnShockRing(bossX, 0.35f, bossZ, maxRadius = spec.arenaRadius, expandSpeed = 16f, fromPlayer = false, spec.hazardColor)
            spawnBurst(bossX, bossY, bossZ, spec.coreGlowColor, 18)
            statusBanner = if (bossPhase == spec.phaseCount && spec.isGrandFinale) {
                "فاز نهایی امپراتور امگا! تمام سپرها از کار افتاد!"
            } else {
                "${spec.titleFa} • فاز $bossPhase!"
            }
            statusBannerTimer = 3.0f
        }

        // Boss orbital hovering figure-8 / circle movement
        val speedScale = 1f + (bossPhase - 1) * 0.22f
        bossMoveAngle += dt * (spec.bossMoveSpeed / spec.arenaRadius) * speedScale
        val orbitR = spec.arenaRadius * (if (spec.isGrandFinale) 0.36f else 0.42f)
        bossX = sin(bossMoveAngle) * orbitR
        bossZ = -cos(bossMoveAngle * 0.7f) * (orbitR * 0.78f) - 2.2f
        bossY = (3.4f * (spec.bossScale / 2f).coerceIn(0.85f, 1.35f)) + sin(totalSimTime * 2.4f) * 0.45f
        bossYaw = atan2(playerX - bossX, playerZ - bossZ)

        // Attack cadence accelerates cleanly with phase
        val cadence = (spec.attackIntervalSec / (1f + (bossPhase - 1) * 0.20f)).coerceAtLeast(0.65f)
        bossAttackTimer -= dt
        if (bossAttackTimer <= 0f) {
            bossAttackTimer = cadence
            triggerNextBossAttack()
        }
    }

    private fun triggerNextBossAttack() {
        val pattern = spec.allowedPatterns[patternIndex % spec.allowedPatterns.size]
        patternIndex++
        val dx = playerX - bossX
        val dy = (playerY + 0.9f) - bossY
        val dz = playerZ - bossZ
        val baseYaw = atan2(dx, dz)

        when (pattern) {
            BossAttackPattern.ORB_VOLLEY -> {
                val count = 3 + (bossPhase - 1) * 2
                val spd = 13.5f + bossPhase * 1.8f
                for (i in 0 until count) {
                    val spreadAngle = (i - (count - 1) / 2f) * 0.22f
                    val a = baseYaw + spreadAngle
                    val vx = sin(a) * spd
                    val vz = cos(a) * spd
                    val distHoriz = hypot(dx, dz).coerceAtLeast(1f)
                    val vy = (dy / distHoriz) * spd
                    spawnBullet(
                        pool = enemyBullets,
                        x = bossX,
                        y = bossY - 0.3f,
                        z = bossZ,
                        vx = vx,
                        vy = vy,
                        vz = vz,
                        radius = 0.42f,
                        damage = 1,
                        fromPlayer = false,
                        r = spec.hazardColor[0],
                        g = spec.hazardColor[1],
                        b = spec.hazardColor[2]
                    )
                }
            }

            BossAttackPattern.SHOCK_RING -> {
                spawnShockRing(
                    x = bossX,
                    y = 0.3f,
                    z = bossZ,
                    maxRadius = spec.arenaRadius * 1.15f,
                    expandSpeed = 10.5f + bossPhase * 1.4f,
                    fromPlayer = false,
                    color = spec.hazardColor
                )
            }

            BossAttackPattern.METEOR_STRIKE -> {
                val strikes = 2 + bossPhase
                for (i in 0 until strikes) {
                    val tx = if (i == 0) playerX else (Random.nextFloat() * 2f - 1f) * (spec.arenaRadius * 0.72f)
                    val tz = if (i == 0) playerZ else (Random.nextFloat() * 2f - 1f) * (spec.arenaRadius * 0.72f)
                    spawnTelegraph(isBeam = false, x = tx, z = tz, yaw = 0f, radius = 2.75f, length = 0f, duration = 1.05f)
                }
            }

            BossAttackPattern.SWEEP_BEAM -> {
                spawnTelegraph(
                    isBeam = true,
                    x = bossX,
                    z = bossZ,
                    yaw = baseYaw,
                    radius = 1.65f,
                    length = spec.arenaRadius * 1.9f,
                    duration = 1.15f
                )
                if (spec.isGrandFinale && bossPhase >= 3) {
                    spawnTelegraph(
                        isBeam = true,
                        x = bossX,
                        z = bossZ,
                        yaw = baseYaw + 0.55f,
                        radius = 1.45f,
                        length = spec.arenaRadius * 1.9f,
                        duration = 1.15f
                    )
                }
            }

            BossAttackPattern.SPIRAL_BARRAGE -> {
                val arms = 5 + bossPhase * 2
                val spd = 11.5f + bossPhase * 1.2f
                spiralAngle += 0.42f
                for (i in 0 until arms) {
                    val a = spiralAngle + (i.toFloat() / arms) * (2f * PI.toFloat())
                    spawnBullet(
                        pool = enemyBullets,
                        x = bossX,
                        y = 1.15f,
                        z = bossZ,
                        vx = sin(a) * spd,
                        vy = 0f,
                        vz = cos(a) * spd,
                        radius = 0.36f,
                        damage = 1,
                        fromPlayer = false,
                        r = spec.coreGlowColor[0],
                        g = spec.coreGlowColor[1],
                        b = spec.coreGlowColor[2]
                    )
                }
            }
        }
    }

    private fun updateSatellites(dt: Float) {
        val satOrbit = spec.bossScale * 2.35f
        for (i in 0 until satellites.size) {
            val s = satellites[i]
            if (!s.active) continue
            s.angle += dt * (1.2f + bossPhase * 0.25f)
            s.x = bossX + cos(s.angle) * satOrbit
            s.y = bossY + sin(s.angle * 2f) * 0.55f
            s.z = bossZ + sin(s.angle) * satOrbit
            s.fireTimer -= dt
            if (s.fireTimer <= 0f) {
                s.fireTimer = (2.6f - bossPhase * 0.25f).coerceAtLeast(1.2f)
                val dx = playerX - s.x
                val dy = (playerY + 0.9f) - s.y
                val dz = playerZ - s.z
                val len = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.01f)
                val spd = 12.5f
                spawnBullet(
                    pool = enemyBullets,
                    x = s.x,
                    y = s.y,
                    z = s.z,
                    vx = (dx / len) * spd,
                    vy = (dy / len) * spd,
                    vz = (dz / len) * spd,
                    radius = 0.32f,
                    damage = 1,
                    fromPlayer = false,
                    r = spec.coreGlowColor[0],
                    g = spec.coreGlowColor[1],
                    b = spec.coreGlowColor[2]
                )
            }
        }
    }

    private fun updateSatellitePositions() {
        val satOrbit = spec.bossScale * 2.35f
        for (i in 0 until satellites.size) {
            val s = satellites[i]
            if (!s.active) continue
            s.x = bossX + cos(s.angle) * satOrbit
            s.y = bossY
            s.z = bossZ + sin(s.angle) * satOrbit
        }
    }

    private fun updateTelegraphs(dt: Float) {
        for (t in telegraphs) {
            if (!t.active) continue
            t.timer -= dt
            if (t.timer <= 0f) {
                if (!t.firingActive) {
                    t.firingActive = true
                    t.timer = 0.30f
                    t.damageApplied = false
                    safeSfx("explosion")
                    if (!t.isBeam) {
                        spawnBurst(t.x, 0.5f, t.z, spec.hazardColor, 12)
                    }
                } else {
                    t.active = false
                    continue
                }
            }

            if (t.firingActive && !t.damageApplied) {
                if (!t.isBeam) {
                    val d = hypot(playerX - t.x, playerZ - t.z)
                    if (d <= t.radius && playerY < 2.1f) {
                        t.damageApplied = true
                        damagePlayer(1)
                    }
                } else {
                    // Distance from player to beam ray
                    val dirX = sin(t.yaw)
                    val dirZ = cos(t.yaw)
                    val relX = playerX - t.x
                    val relZ = playerZ - t.z
                    val proj = relX * dirX + relZ * dirZ
                    if (proj in 0f..t.length) {
                        val perp = abs(relX * dirZ - relZ * dirX)
                        if (perp <= t.radius && playerY < 2.2f) {
                            t.damageApplied = true
                            damagePlayer(1)
                        }
                    }
                }
            }
        }
    }

    private fun updateShockRings(dt: Float) {
        for (sr in shockRings) {
            if (!sr.active) continue
            sr.radius += sr.expandSpeed * dt
            if (sr.radius >= sr.maxRadius) {
                sr.active = false
                continue
            }
            if (!sr.fromPlayer && !sr.hitPlayer) {
                val dist = hypot(playerX - sr.x, playerZ - sr.z)
                // Player can jump with jetpack over ground shockwave rings!
                if (abs(dist - sr.radius) <= 0.95f && playerY < 1.15f) {
                    sr.hitPlayer = true
                    damagePlayer(1)
                }
            }
        }
    }

    private fun updateBullets(dt: Float) {
        val bossHitRadius = spec.bossScale * 1.15f
        for (b in playerBullets) {
            if (!b.active) continue
            b.x += b.vx * dt
            b.y += b.vy * dt
            b.z += b.vz * dt
            b.life -= dt
            if (b.life <= 0f || hypot(b.x, b.z) > spec.arenaRadius + 10f) {
                b.active = false
                continue
            }
            val dx = b.x - bossX
            val dy = b.y - bossY
            val dz = b.z - bossZ
            if (dx * dx + dy * dy + dz * dz <= (bossHitRadius + b.radius) * (bossHitRadius + b.radius)) {
                b.active = false
                shotsHit++
                damageBoss(b.damage)
                spawnBurst(b.x, b.y, b.z, spec.coreGlowColor, 5)
            }
        }

        for (b in enemyBullets) {
            if (!b.active) continue
            b.x += b.vx * dt
            b.y += b.vy * dt
            b.z += b.vz * dt
            b.life -= dt
            if (b.life <= 0f || b.y < 0f || hypot(b.x, b.z) > spec.arenaRadius + 4f) {
                b.active = false
                continue
            }
            // Cover pillar shield absorption
            var blocked = false
            for (i in 0 until activePillarCount) {
                val p = pillars[i]
                if (b.y <= p.height && hypot(b.x - p.x, b.z - p.z) <= p.radius + b.radius) {
                    blocked = true
                    break
                }
            }
            if (blocked) {
                b.active = false
                spawnBurst(b.x, b.y, b.z, spec.deckTrimColor, 3)
                continue
            }
            val dx = b.x - playerX
            val dy = b.y - (playerY + 0.9f)
            val dz = b.z - playerZ
            if (dx * dx + dy * dy + dz * dz <= (0.65f + b.radius) * (0.65f + b.radius)) {
                b.active = false
                damagePlayer(b.damage)
            }
        }
    }

    private fun updatePickups(dt: Float) {
        pickupSpawnTimer -= dt
        if (pickupSpawnTimer <= 0f) {
            pickupSpawnTimer = if (playerAmmo <= 12) 4.2f else 8.0f
            val angle = Random.nextFloat() * (2f * PI.toFloat())
            val r = spec.arenaRadius * (0.35f + Random.nextFloat() * 0.42f)
            val needHealth = playerHealth < playerMaxHealth && Random.nextFloat() < 0.45f
            spawnPickup(isHealth = needHealth, x = cos(angle) * r, z = sin(angle) * r)
        }

        for (p in pickups) {
            if (!p.active) continue
            p.bobPhase += dt * 3f
            if (hypot(playerX - p.x, playerZ - p.z) <= 1.35f && playerY < 1.8f) {
                p.active = false
                if (p.isHealth) {
                    playerHealth = (playerHealth + 1).coerceAtMost(playerMaxHealth)
                    safeSfx("powerup")
                } else {
                    playerAmmo += 25 + luckSkill * 4
                    safeSfx("battery")
                }
                score += 60L
                spawnBurst(p.x, 1.0f, p.z, spec.deckTrimColor, 6)
            }
        }
    }

    private fun damageBoss(amount: Int) {
        if (bossKilled || bossHp <= 0) return
        bossHp = (bossHp - amount).coerceAtLeast(0)
        bossHitFlashTimer = 0.10f
        score += 35L * amount
        if (bossHp <= 0) {
            handleBossDefeated()
        }
    }

    private fun handleBossDefeated() {
        if (bossKilled) return
        bossKilled = true
        enemiesKilled = 1
        score += if (spec.isGrandFinale) 5000L else 1200L
        safeSfx("rocket_launch")
        screenShakeTimer = 0.55f
        spawnBurst(bossX, bossY, bossZ, spec.coreGlowColor, 28)
        for (b in enemyBullets) b.active = false
        for (t in telegraphs) t.active = false
        for (s in satellites) s.active = false

        if (stageDef.hasBetrayalChoice) {
            combatState = Boss3DCombatState.STORY_CHOICE
            phase = ArenaPhase.STORY_CHOICE
        } else {
            combatState = Boss3DCombatState.VICTORY_FINALE
            phase = ArenaPhase.VICTORY
        }
    }

    private fun damagePlayer(amount: Int) {
        if (playerInvulnTimer > 0f || combatState != Boss3DCombatState.ACTIVE_COMBAT) return
        playerInvulnTimer = 0.85f
        screenShakeTimer = 0.22f
        safeSfx("hit")
        spawnBurst(playerX, playerY + 1.0f, playerZ, spec.hazardColor, 9)
        if (playerShield > 0) {
            playerShield = (playerShield - amount).coerceAtLeast(0)
        } else {
            playerHealth = (playerHealth - amount).coerceAtLeast(0)
            if (playerHealth <= 0) {
                triggerDefeat("سلامت فضانورد در نبرد سه‌بعدی به پایان رسید!")
            }
        }
    }

    private fun triggerDefeat(reason: String) {
        if (phase == ArenaPhase.GAME_OVER) return
        statusBanner = reason
        combatState = Boss3DCombatState.DEFEAT
        phase = ArenaPhase.GAME_OVER
        safeSfx("explosion")
    }

    private fun spawnBullet(
        pool: Array<PooledBullet3D>,
        x: Float, y: Float, z: Float,
        vx: Float, vy: Float, vz: Float,
        radius: Float, damage: Int, fromPlayer: Boolean,
        r: Float, g: Float, b: Float
    ) {
        for (item in pool) {
            if (!item.active) {
                item.active = true
                item.x = x; item.y = y; item.z = z
                item.vx = vx; item.vy = vy; item.vz = vz
                item.radius = radius
                item.damage = damage
                item.fromPlayer = fromPlayer
                item.life = 3.6f
                item.r = r; item.g = g; item.b = b
                return
            }
        }
    }

    private fun spawnTelegraph(
        isBeam: Boolean, x: Float, z: Float, yaw: Float, radius: Float, length: Float, duration: Float
    ) {
        for (t in telegraphs) {
            if (!t.active) {
                t.active = true
                t.isBeam = isBeam
                t.x = x; t.z = z; t.yaw = yaw
                t.radius = radius; t.length = length
                t.timer = duration; t.maxTimer = duration
                t.firingActive = false
                t.damageApplied = false
                return
            }
        }
    }

    private fun spawnShockRing(
        x: Float, y: Float, z: Float, maxRadius: Float, expandSpeed: Float, fromPlayer: Boolean, color: FloatArray
    ) {
        for (sr in shockRings) {
            if (!sr.active) {
                sr.active = true
                sr.x = x; sr.y = y; sr.z = z
                sr.radius = 0.7f
                sr.maxRadius = maxRadius
                sr.expandSpeed = expandSpeed
                sr.fromPlayer = fromPlayer
                sr.hitPlayer = false
                sr.r = color[0]; sr.g = color[1]; sr.b = color[2]
                return
            }
        }
    }

    private fun spawnPickup(isHealth: Boolean, x: Float, z: Float) {
        var activeCount = 0
        for (p in pickups) if (p.active) activeCount++
        if (activeCount >= 3) return
        for (p in pickups) {
            if (!p.active) {
                p.active = true
                p.isHealth = isHealth
                p.x = x; p.z = z; p.bobPhase = 0f
                return
            }
        }
    }

    private fun spawnBurst(x: Float, y: Float, z: Float, color: FloatArray, count: Int) {
        var spawned = 0
        var activeCount = 0
        for (pt in particles) if (pt.active) activeCount++
        val room = (maxParticlePool - activeCount).coerceAtLeast(0)
        val limit = min(count, room)
        if (limit <= 0) return
        for (pt in particles) {
            if (!pt.active) {
                val theta = Random.nextFloat() * (2f * PI.toFloat())
                val phi = (Random.nextFloat() - 0.3f) * PI.toFloat() * 0.6f
                val spd = 3f + Random.nextFloat() * 7.5f
                pt.active = true
                pt.x = x; pt.y = y; pt.z = z
                pt.vx = cos(theta) * cos(phi) * spd
                pt.vy = sin(phi) * spd + 2.5f
                pt.vz = sin(theta) * cos(phi) * spd
                pt.maxLife = 0.45f
                pt.life = 0.45f
                pt.size = 0.18f + Random.nextFloat() * 0.22f
                pt.r = color[0]; pt.g = color[1]; pt.b = color[2]
                spawned++
                if (spawned >= limit) break
            }
        }
    }

    private fun updateParticles(dt: Float) {
        for (pt in particles) {
            if (!pt.active) continue
            pt.x += pt.vx * dt
            pt.y += pt.vy * dt
            pt.z += pt.vz * dt
            pt.vy -= 9f * dt
            pt.life -= dt
            if (pt.life <= 0f) pt.active = false
        }
    }

    private fun normalizeAngle(a: Float): Float {
        var res = a
        val twoPi = (2.0 * PI).toFloat()
        while (res > PI) res -= twoPi
        while (res < -PI) res += twoPi
        return res
    }

    private fun safeSfx(key: String) {
        try {
            onSfx(key)
        } catch (_: Throwable) {
        }
    }

    fun buildHudSnapshot(): HudSnapshot {
        val stars = when {
            phase != ArenaPhase.VICTORY -> 0
            playerHealth >= playerMaxHealth - 1 && timeRemaining > 35f -> 3
            playerHealth >= 2 -> 2
            else -> 1
        }
        val xpEarned = if (phase == ArenaPhase.VICTORY) {
            stageDef.rewardXp + (if (spec.isGrandFinale) 600 else 180)
        } else {
            (((bossMaxHp - bossHp) * 100 / bossMaxHp.coerceAtLeast(1)) * 2).coerceAtLeast(25)
        }
        val crystalsEarned = if (phase == ArenaPhase.VICTORY) {
            stageDef.rewardCrystals + (luckSkill * 15) + (if (spec.isGrandFinale) 300 else 50)
        } else {
            ((bossMaxHp - bossHp) / 4).coerceAtLeast(5)
        }
        val objText = "${spec.titleFa} • فاز $bossPhase/${spec.phaseCount}"
        return HudSnapshot(
            mode = mode,
            stageNumber = stageDef.stageNumber,
            phase = phase,
            stageTitle = stageDef.titleFa,
            objectiveText = objText,
            health = playerHealth,
            maxHealth = playerMaxHealth,
            shield = playerShield,
            ammo = playerAmmo,
            batteries = 0,
            targetBatteries = 0,
            enemiesRemaining = if (bossKilled) 0 else 1,
            score = score,
            rivalScore = 0L,
            wave = spec.chapterIndex,
            timeRemainingSec = timeRemaining.toInt(),
            bossHp = bossHp.coerceAtLeast(0),
            bossMaxHp = bossMaxHp,
            bossPhase = bossPhase,
            specialReadyRatio = (1f - (specialCooldown / 7.5f)).coerceIn(0f, 1f),
            statusBanner = statusBanner,
            isVictory = phase == ArenaPhase.VICTORY,
            starsEarned = stars,
            xpEarned = xpEarned,
            crystalsEarned = crystalsEarned,
            enemiesKilled = enemiesKilled,
            bossKilled = bossKilled,
            shotsFired = shotsFired,
            shotsHit = shotsHit,
            elapsedSeconds = elapsedSeconds.toLong(),
            betrayalChosen = betrayalChosen,
            horizontalProgress = (1f - (bossHp.toFloat() / bossMaxHp.coerceAtLeast(1).toFloat())).coerceIn(0f, 1f)
        )
    }

    private fun publishRenderFrame() {
        writeRenderBuffer(stagingBuffer)
        synchronized(renderLock) {
            copyRenderBuffer(stagingBuffer, publicBuffer)
        }
    }

    private fun writeRenderBuffer(out: Boss3DRenderBuffer) {
        out.stageNumber = stageDef.stageNumber
        out.combatState = combatState
        out.timeSec = totalSimTime
        out.arenaRadius = spec.arenaRadius
        out.bossScale = spec.bossScale
        out.bossX = bossX
        out.bossY = bossY
        out.bossZ = bossZ
        out.bossYaw = bossYaw
        out.bossPhase = bossPhase
        out.bossHpRatio = (bossHp.toFloat() / bossMaxHp.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
        out.bossHitFlash = (bossHitFlashTimer / 0.10f).coerceIn(0f, 1f)
        out.hasOrbitalRings = spec.hasOrbitalRings
        out.hasDualWingCannons = spec.hasDualWingCannons
        out.isDarkEclipse = spec.isDarkEclipse
        out.isGrandFinale = spec.isGrandFinale

        out.playerX = playerX
        out.playerY = playerY
        out.playerZ = playerZ
        out.playerYaw = playerYaw
        out.playerSpeed = hypot(playerVelX, playerVelZ)
        val camRightX = cos(cameraYaw)
        val camRightZ = sin(cameraYaw)
        out.playerMoveRightDot = playerVelX * camRightX + playerVelZ * camRightZ
        out.playerAimRightDot = (bossX - playerX) * camRightX + (bossZ - playerZ) * camRightZ
        out.playerLandImpulse = playerLandImpulse
        out.playerInvuln = playerInvulnTimer > 0f && ((totalSimTime * 14f).toInt() % 2 == 0)
        out.playerJetpacking = playerY > 0.12f
        out.playerHasShield = playerShield > 0
        out.playerDashing = playerDashTimer > 0f

        out.droneActive = droneSkill > 0 || spec.isGrandFinale
        out.droneX = droneX
        out.droneY = droneY
        out.droneZ = droneZ

        out.camYaw = cameraYaw
        out.camPitch = cameraPitch
        out.screenShake = (screenShakeTimer * 0.7f).coerceIn(0f, 0.45f)

        spec.skyTopColor.copyInto(out.skyTop)
        spec.skyBottomColor.copyInto(out.skyBottom)
        spec.deckColor.copyInto(out.deckColor)
        spec.deckTrimColor.copyInto(out.deckTrim)
        spec.planetColor.copyInto(out.planetColor)
        if (spec.planetRingColor != null) {
            out.hasPlanetRing = true
            spec.planetRingColor.copyInto(out.planetRingColor)
        } else {
            out.hasPlanetRing = false
        }
        spec.hullColor.copyInto(out.hullColor)
        spec.coreGlowColor.copyInto(out.coreColor)
        spec.hazardColor.copyInto(out.hazardColor)
        out.heroAccent[0] = ((hero.accentColor shr 16) and 0xFF) / 255f
        out.heroAccent[1] = ((hero.accentColor shr 8) and 0xFF) / 255f
        out.heroAccent[2] = (hero.accentColor and 0xFF) / 255f
        out.heroAccent[3] = 1f

        out.pillarCount = activePillarCount
        for (i in 0 until activePillarCount) {
            val base = i * 3
            out.pillarXZR[base] = pillars[i].x
            out.pillarXZR[base + 1] = pillars[i].z
            out.pillarXZR[base + 2] = pillars[i].radius
        }

        var sCount = 0
        for (s in satellites) {
            if (!s.active || sCount >= 6) continue
            val base = sCount * 3
            out.satelliteXYZ[base] = s.x
            out.satelliteXYZ[base + 1] = s.y
            out.satelliteXYZ[base + 2] = s.z
            sCount++
        }
        out.satelliteCount = sCount

        var pkCount = 0
        for (pk in pickups) {
            if (!pk.active || pkCount >= 10) continue
            val base = pkCount * 3
            out.pickupXZType[base] = pk.x
            out.pickupXZType[base + 1] = pk.z
            out.pickupXZType[base + 2] = if (pk.isHealth) 1f else 0f
            pkCount++
        }
        out.pickupCount = pkCount

        var bCount = 0
        for (b in playerBullets) {
            if (!b.active || bCount >= 144) continue
            val base = bCount * 7
            out.bulletData[base] = b.x; out.bulletData[base + 1] = b.y; out.bulletData[base + 2] = b.z
            out.bulletData[base + 3] = b.r; out.bulletData[base + 4] = b.g; out.bulletData[base + 5] = b.b
            out.bulletData[base + 6] = b.radius
            bCount++
        }
        for (b in enemyBullets) {
            if (!b.active || bCount >= 144) continue
            val base = bCount * 7
            out.bulletData[base] = b.x; out.bulletData[base + 1] = b.y; out.bulletData[base + 2] = b.z
            out.bulletData[base + 3] = b.r; out.bulletData[base + 4] = b.g; out.bulletData[base + 5] = b.b
            out.bulletData[base + 6] = b.radius
            bCount++
        }
        out.bulletCount = bCount

        var srCount = 0
        for (sr in shockRings) {
            if (!sr.active || srCount >= 12) continue
            val base = srCount * 7
            out.shockData[base] = sr.x; out.shockData[base + 1] = sr.y; out.shockData[base + 2] = sr.z
            out.shockData[base + 3] = sr.radius
            out.shockData[base + 4] = sr.r; out.shockData[base + 5] = sr.g; out.shockData[base + 6] = sr.b
            srCount++
        }
        out.shockCount = srCount

        var tCount = 0
        for (t in telegraphs) {
            if (!t.active || tCount >= 16) continue
            val base = tCount * 7
            out.telegraphData[base] = if (t.isBeam) 1f else 0f
            out.telegraphData[base + 1] = t.x
            out.telegraphData[base + 2] = t.z
            out.telegraphData[base + 3] = t.yaw
            out.telegraphData[base + 4] = t.radius
            out.telegraphData[base + 5] = t.length
            out.telegraphData[base + 6] = if (t.firingActive) 2f else (1f - (t.timer / t.maxTimer.coerceAtLeast(0.01f))).coerceIn(0f, 1f)
            tCount++
        }
        out.telegraphCount = tCount

        var pCount = 0
        for (pt in particles) {
            if (!pt.active || pCount >= 140) continue
            val base = pCount * 7
            out.particleData[base] = pt.x; out.particleData[base + 1] = pt.y; out.particleData[base + 2] = pt.z
            out.particleData[base + 3] = pt.r; out.particleData[base + 4] = pt.g; out.particleData[base + 5] = pt.b
            out.particleData[base + 6] = pt.size * (pt.life / pt.maxLife.coerceAtLeast(0.01f)).coerceIn(0.15f, 1f)
            pCount++
        }
        out.particleCount = pCount
    }

    private fun copyRenderBuffer(src: Boss3DRenderBuffer, dst: Boss3DRenderBuffer) {
        dst.stageNumber = src.stageNumber
        dst.combatState = src.combatState
        dst.timeSec = src.timeSec
        dst.arenaRadius = src.arenaRadius
        dst.bossScale = src.bossScale
        dst.bossX = src.bossX; dst.bossY = src.bossY; dst.bossZ = src.bossZ; dst.bossYaw = src.bossYaw
        dst.bossPhase = src.bossPhase; dst.bossHpRatio = src.bossHpRatio; dst.bossHitFlash = src.bossHitFlash
        dst.hasOrbitalRings = src.hasOrbitalRings
        dst.hasDualWingCannons = src.hasDualWingCannons
        dst.isDarkEclipse = src.isDarkEclipse
        dst.isGrandFinale = src.isGrandFinale

        dst.playerX = src.playerX; dst.playerY = src.playerY; dst.playerZ = src.playerZ; dst.playerYaw = src.playerYaw
        dst.playerSpeed = src.playerSpeed
        dst.playerMoveRightDot = src.playerMoveRightDot
        dst.playerAimRightDot = src.playerAimRightDot
        dst.playerLandImpulse = src.playerLandImpulse
        dst.playerInvuln = src.playerInvuln; dst.playerJetpacking = src.playerJetpacking
        dst.playerHasShield = src.playerHasShield; dst.playerDashing = src.playerDashing

        dst.droneActive = src.droneActive; dst.droneX = src.droneX; dst.droneY = src.droneY; dst.droneZ = src.droneZ
        dst.camYaw = src.camYaw; dst.camPitch = src.camPitch; dst.screenShake = src.screenShake

        src.skyTop.copyInto(dst.skyTop)
        src.skyBottom.copyInto(dst.skyBottom)
        src.deckColor.copyInto(dst.deckColor)
        src.deckTrim.copyInto(dst.deckTrim)
        src.planetColor.copyInto(dst.planetColor)
        dst.hasPlanetRing = src.hasPlanetRing
        src.planetRingColor.copyInto(dst.planetRingColor)
        src.hullColor.copyInto(dst.hullColor)
        src.coreColor.copyInto(dst.coreColor)
        src.hazardColor.copyInto(dst.hazardColor)
        src.heroAccent.copyInto(dst.heroAccent)

        dst.pillarCount = src.pillarCount
        src.pillarXZR.copyInto(dst.pillarXZR, 0, 0, src.pillarCount * 3)
        dst.satelliteCount = src.satelliteCount
        src.satelliteXYZ.copyInto(dst.satelliteXYZ, 0, 0, src.satelliteCount * 3)
        dst.pickupCount = src.pickupCount
        src.pickupXZType.copyInto(dst.pickupXZType, 0, 0, src.pickupCount * 3)
        dst.bulletCount = src.bulletCount
        src.bulletData.copyInto(dst.bulletData, 0, 0, src.bulletCount * 7)
        dst.shockCount = src.shockCount
        src.shockData.copyInto(dst.shockData, 0, 0, src.shockCount * 7)
        dst.telegraphCount = src.telegraphCount
        src.telegraphData.copyInto(dst.telegraphData, 0, 0, src.telegraphCount * 7)
        dst.particleCount = src.particleCount
        src.particleData.copyInto(dst.particleData, 0, 0, src.particleCount * 7)
    }
}
