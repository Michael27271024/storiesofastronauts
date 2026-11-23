package com.example.astronautstories.engine

import com.example.astronautstories.domain.model.EnemyKind
import com.example.astronautstories.domain.model.GameCatalog
import com.example.astronautstories.domain.model.GameMode
import com.example.astronautstories.domain.model.GraphicsQuality
import com.example.astronautstories.domain.model.HeroId
import com.example.astronautstories.domain.model.MissionObjective
import com.example.astronautstories.domain.model.SkillType
import com.example.astronautstories.domain.model.StageDefinition
import com.example.astronautstories.domain.model.WeaponType
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

data class InputControlsState(
    val left: Boolean = false,
    val right: Boolean = false,
    val jump: Boolean = false,
    val crouch: Boolean = false,
    val shoot: Boolean = false,
    val specialTrigger: Boolean = false
)

enum class PickupKind {
    GUN, BATTERY, POWERFUL_BATTERY, ROCKET_PART, AMMO_PACK, HEALTH_PACK, HOSTAGE, CRYSTAL
}

data class PlatformRect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

data class PlayerEntity(
    var x: Float = 120f,
    var y: Float = 520f,
    val width: Float = 56f,
    val height: Float = 74f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var facingRight: Boolean = true,
    var isJumping: Boolean = false,
    var isCrouching: Boolean = false,
    var isJetpacking: Boolean = false,
    var hasGun: Boolean = true,
    var health: Int = 5,
    var maxHealth: Int = 5,
    var shield: Int = 0,
    var maxShield: Int = 3,
    var ammo: Int = 30,
    var batteries: Int = 0,
    var partsCollected: Int = 0,
    var hostagesRescued: Int = 0,
    var invulnTimer: Float = 0f,
    var specialCooldown: Float = 0f
)

data class TeammateEntity(
    val name: String,
    var x: Float,
    var y: Float,
    var health: Int = 4,
    var shootTimer: Float = 0.8f,
    var facingRight: Boolean = true,
    val offsetIndex: Int
)

data class EnemyEntity(
    val id: Int,
    val kind: EnemyKind,
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    var hp: Int,
    val maxHp: Int,
    var speed: Float,
    var dir: Float = -1f,
    var shootTimer: Float = 1.6f,
    var phase: Int = 1,
    var bobPhase: Float = 0f,
    var callout: String = ""
)

data class BulletEntity(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val width: Float,
    val height: Float,
    val damage: Int,
    val fromPlayer: Boolean,
    val colorHex: Long
)

data class PickupEntity(
    val id: Int,
    val kind: PickupKind,
    var x: Float,
    var y: Float,
    val width: Float = 38f,
    val height: Float = 38f,
    var collected: Boolean = false
)

data class ParticleEntity(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    var maxLife: Float,
    val colorHex: Long,
    val size: Float
)

data class StarEntity(
    val x: Float,
    val y: Float,
    val size: Float,
    val parallax: Float,
    val alpha: Float
)

enum class ArenaPhase {
    BRIEFING,
    PLAYING,
    STORY_CHOICE,
    VICTORY,
    GAME_OVER
}

data class HudSnapshot(
    val mode: GameMode = GameMode.STORY,
    val stageNumber: Int = 0,
    val phase: ArenaPhase = ArenaPhase.BRIEFING,
    val stageTitle: String = "",
    val objectiveText: String = "",
    val health: Int = 5,
    val maxHealth: Int = 5,
    val shield: Int = 0,
    val ammo: Int = 30,
    val batteries: Int = 0,
    val targetBatteries: Int = 2,
    val enemiesRemaining: Int = 0,
    val score: Long = 0L,
    val rivalScore: Long = 0L,
    val wave: Int = 1,
    val timeRemainingSec: Int = 180,
    val bossHp: Int = 0,
    val bossMaxHp: Int = 0,
    val bossPhase: Int = 1,
    val specialReadyRatio: Float = 1f,
    val statusBanner: String = "",
    val isVictory: Boolean = false,
    val starsEarned: Int = 3,
    val xpEarned: Int = 0,
    val crystalsEarned: Int = 0,
    val enemiesKilled: Int = 0,
    val bossKilled: Boolean = false,
    val shotsFired: Int = 0,
    val shotsHit: Int = 0,
    val elapsedSeconds: Long = 0L,
    val betrayalChosen: Boolean = false,
    val horizontalProgress: Float = 0f
)

class GameSimulation(
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
    // Horizontal Landscape Side-Scrolling Corridor Architecture
    val worldWidth: Float = when (mode) {
        GameMode.ENDLESS, GameMode.CHALLENGE, GameMode.MULTIPLAYER -> 1680f
        else -> stageDef.worldWidth.coerceAtLeast(2600f)
    }
    val worldHeight: Float = 720f
    val groundY: Float = worldHeight - 110f

    val chapter = GameCatalog.chapters[(stageDef.chapterIndex - 1).coerceIn(0, GameCatalog.chapters.lastIndex)]
    private val gravity = chapter.gravity * 1500f

    val player = PlayerEntity()
    val teammates = mutableListOf<TeammateEntity>()
    val enemies = mutableListOf<EnemyEntity>()
    val bullets = mutableListOf<BulletEntity>()
    val pickups = mutableListOf<PickupEntity>()
    val particles = mutableListOf<ParticleEntity>()
    val platforms = mutableListOf<PlatformRect>()
    val stars = mutableListOf<StarEntity>()

    var phase: ArenaPhase = ArenaPhase.BRIEFING
    var cameraX: Float = 0f
    var score: Long = 0L
    var rivalScore: Long = 0L
    var wave: Int = 1
    var timeRemaining: Float = if (mode == GameMode.CHALLENGE) 90f else stageDef.timeLimitSeconds.toFloat()
    var elapsedSeconds: Float = 0f
    var enemiesKilled: Int = 0
    var bossKilled: Boolean = false
    var shotsFired: Int = 0
    var shotsHit: Int = 0
    var betrayalChosen: Boolean = false

    private var nextEntityId = 1
    private var fireCooldown = 0f
    private var partSpawned = false
    private var bossSpawned = false
    private val defeatedEnemyIds = HashSet<Int>()
    private var statusBanner = stageDef.briefingFa
    private var statusBannerTimer = 0f
    private var rivalTickTimer = 0f
    private var jetpackParticleTimer = 0f

    private val speedSkill = skillLevels[SkillType.SPEED] ?: 0
    private val fireSkill = skillLevels[SkillType.FIREPOWER] ?: 0
    private val armorSkill = skillLevels[SkillType.ARMOR] ?: 0
    private val oxygenSkill = skillLevels[SkillType.OXYGEN_CAP] ?: 0
    private val luckSkill = skillLevels[SkillType.LUCK] ?: 0
    private val droneSkill = skillLevels[SkillType.DRONE] ?: 0

    private val maxParticles = when (graphicsQuality) {
        GraphicsQuality.LOW -> 28
        GraphicsQuality.MEDIUM -> 70
        GraphicsQuality.HIGH -> 140
    }

    init {
        setupWorld()
    }

    private fun setupWorld() {
        val baseHp = maxOf(5, hero.bonusHp) + armorSkill + bonusMedkits
        player.maxHealth = baseHp
        player.health = baseHp
        player.maxShield = 2 + oxygenSkill
        player.shield = if (bonusShield) player.maxShield else (oxygenSkill / 2)
        player.ammo = if (stageDef.stageNumber == 1 && mode == GameMode.STORY) 0 else (45 + luckSkill * 8 + bonusAmmo)
        player.hasGun = !(stageDef.stageNumber == 1 && mode == GameMode.STORY)
        player.x = 120f
        player.y = groundY - player.height
        player.facingRight = true

        timeRemaining += oxygenSkill * 20f

        // Horizontal ground floor + stepped aerial platforms across the landscape planet surface
        platforms.clear()
        platforms.add(PlatformRect(0f, groundY, worldWidth, 130f))
        var px = 340f
        var idx = 0
        while (px < worldWidth - 360f) {
            val platWidth = 220f
            val py = if (idx % 2 == 0) groundY - 145f else groundY - 270f
            platforms.add(PlatformRect(px, py, platWidth, 24f))
            px += 310f
            idx++
        }
        // Final extraction platform at far right
        platforms.add(PlatformRect(worldWidth - 340f, groundY - 145f, 260f, 24f))

        // Horizontal starfield
        stars.clear()
        val starCount = when (graphicsQuality) {
            GraphicsQuality.LOW -> 60
            GraphicsQuality.MEDIUM -> 110
            GraphicsQuality.HIGH -> 180
        }
        repeat(starCount) {
            stars.add(
                StarEntity(
                    x = Random.nextFloat() * worldWidth,
                    y = Random.nextFloat() * (groundY - 40f),
                    size = Random.nextFloat() * 3f + 1.2f,
                    parallax = Random.nextFloat() * 0.45f + 0.15f,
                    alpha = Random.nextFloat() * 0.65f + 0.35f
                )
            )
        }

        // 3 AI Wingmates flanking the player in horizontal squad formation
        teammates.clear()
        val squadNames = listOf(HeroId.KOUROSH, HeroId.DARIUSH, HeroId.SOHRAB, HeroId.ROSTAM)
            .filter { it != hero }
            .take(3)
        squadNames.forEachIndexed { i, mate ->
            teammates.add(
                TeammateEntity(
                    name = mate.nameFa,
                    x = (player.x - 70f - i * 48f).coerceAtLeast(24f),
                    y = groundY - 64f,
                    health = 4 + droneSkill,
                    shootTimer = 0.45f + i * 0.25f,
                    offsetIndex = i + 1
                )
            )
        }

        // Stage 1: Gun placed visibly ahead on the initial platform/ground
        if (!player.hasGun) {
            pickups.add(PickupEntity(nextEntityId++, PickupKind.GUN, 370f, groundY - 185f))
        }

        if (stageDef.objective == MissionObjective.RESCUE_HOSTAGES && mode == GameMode.STORY) {
            val ratios = listOf(0.32f, 0.58f, 0.84f)
            ratios.forEachIndexed { i, ratio ->
                val hx = (worldWidth * ratio).coerceIn(500f, worldWidth - 220f)
                val hy = if (i % 2 == 0) groundY - 58f else groundY - 203f
                pickups.add(
                    PickupEntity(
                        id = nextEntityId++,
                        kind = PickupKind.HOSTAGE,
                        x = hx,
                        y = hy,
                        width = 42f,
                        height = 56f
                    )
                )
            }
        }

        spawnInitialWave()
    }

    private fun spawnInitialWave() {
        val pool = stageDef.enemyPool.ifEmpty { listOf(EnemyKind.ALIEN_SOLDIER) }
        val count = when (mode) {
            GameMode.ENDLESS -> 5 + wave * 2
            GameMode.CHALLENGE -> 10
            else -> stageDef.targetEnemies
        }
        val minSpawnX = 560f
        val maxSpawnX = (worldWidth - 220f).coerceAtLeast(700f)
        repeat(count) { i ->
            val kind = pool[i % pool.size]
            val spawnX = if (mode == GameMode.ENDLESS || mode == GameMode.CHALLENGE || mode == GameMode.MULTIPLAYER) {
                worldWidth - 140f - Random.nextFloat() * 520f
            } else {
                minSpawnX + (i.toFloat() / count.coerceAtLeast(1)) * (maxSpawnX - minSpawnX)
            }
            val spawnY = when (kind) {
                EnemyKind.FLYER, EnemyKind.ALIEN_SHIP -> 150f + Random.nextFloat() * 210f
                EnemyKind.LASER_TURRET -> groundY - 68f
                else -> if (i % 3 == 1) groundY - 210f else groundY - 68f
            }
            spawnEnemy(kind, spawnX, spawnY)
        }

        if (stageDef.hasBoss && mode == GameMode.STORY) {
            spawnBoss()
        } else if (mode == GameMode.ENDLESS && wave % 5 == 0) {
            spawnBoss()
        }

        if (stageDef.objective == MissionObjective.STORY_EXPEDITION && mode == GameMode.STORY) {
            pickups.add(PickupEntity(nextEntityId++, PickupKind.ROCKET_PART, worldWidth - 240f, groundY - 190f, 44f, 44f))
            partSpawned = true
        }
    }

    private fun spawnBoss() {
        if (bossSpawned) return
        bossSpawned = true
        val bossHp = 22 + stageDef.chapterIndex * 8 + wave * 4
        enemies.add(
            EnemyEntity(
                id = nextEntityId++,
                kind = EnemyKind.BOSS_OVERLORD,
                x = (worldWidth - 240f).coerceAtLeast(620f),
                y = groundY - 190f,
                width = 128f,
                height = 140f,
                hp = bossHp,
                maxHp = bossHp,
                speed = 135f + stageDef.chapterIndex * 12f,
                dir = -1f,
                callout = "فرمانده ناوگان ستاره‌زادگان!"
            )
        )
    }

    private fun spawnEnemy(kind: EnemyKind, x: Float, y: Float) {
        val hpBase = when (kind) {
            EnemyKind.ZOMBIE -> 2
            EnemyKind.ALIEN_SOLDIER -> 3
            EnemyKind.FLYER -> 2
            EnemyKind.SPLITTER -> 4
            EnemyKind.MINI_SPLITTER -> 1
            EnemyKind.ALIEN_SHIP -> 5
            EnemyKind.LASER_TURRET -> 4
            EnemyKind.BOSS_OVERLORD -> 30
        } + (stageDef.chapterIndex / 3) + (wave / 3)

        val speed = when (kind) {
            EnemyKind.LASER_TURRET -> 0f
            EnemyKind.MINI_SPLITTER -> 195f
            EnemyKind.FLYER -> 155f
            else -> 85f + Random.nextFloat() * 75f
        } * if (mode == GameMode.CHALLENGE) 1.25f else 1.0f

        enemies.add(
            EnemyEntity(
                id = nextEntityId++,
                kind = kind,
                x = x.coerceIn(260f, worldWidth - 96f),
                y = y.coerceIn(90f, groundY - 68f),
                width = if (kind == EnemyKind.ALIEN_SHIP) 84f else if (kind == EnemyKind.MINI_SPLITTER) 36f else 54f,
                height = if (kind == EnemyKind.ALIEN_SHIP) 58f else if (kind == EnemyKind.MINI_SPLITTER) 44f else 68f,
                hp = hpBase,
                maxHp = hpBase,
                speed = speed,
                dir = -1f,
                shootTimer = 0.8f + Random.nextFloat() * 1.7f
            )
        )
    }

    fun startMission() {
        if (phase == ArenaPhase.BRIEFING) {
            phase = ArenaPhase.PLAYING
            statusBanner = ""
        }
    }

    fun resolveStoryChoice(betrayal: Boolean) {
        betrayalChosen = betrayal
        phase = ArenaPhase.VICTORY
        safeSfx("rocket_launch")
    }

    fun step(dtRaw: Float, input: InputControlsState, viewportWorldWidth: Float) {
        if (phase != ArenaPhase.PLAYING) return
        val dt = dtRaw.coerceIn(0.001f, 0.05f)
        elapsedSeconds += dt

        if (statusBannerTimer > 0f) {
            statusBannerTimer -= dt
            if (statusBannerTimer <= 0f) statusBanner = ""
        }

        if (mode != GameMode.ENDLESS) {
            timeRemaining -= dt
            if (timeRemaining <= 0f) {
                timeRemaining = 0f
                triggerGameOver("زمان مأموریت به پایان رسید!")
                return
            }
        } else {
            score += (dt * 12).toLong()
        }

        if (mode == GameMode.MULTIPLAYER) {
            rivalTickTimer += dt
            if (rivalTickTimer >= 1.2f) {
                rivalTickTimer = 0f
                rivalScore += Random.nextLong(35L, 95L)
            }
        }

        updatePlayer(dt, input)
        if (phase != ArenaPhase.PLAYING) return
        updateTeammates(dt)
        if (phase != ArenaPhase.PLAYING) return
        updateBullets(dt, viewportWorldWidth)
        if (phase != ArenaPhase.PLAYING) return
        updateEnemies(dt)
        if (phase != ArenaPhase.PLAYING) return
        updatePickups(dt)
        if (phase != ArenaPhase.PLAYING) return
        updateParticles(dt)
        checkMissionCompletion()

        // Smooth Landscape Horizontal Camera following player
        val targetCamX = player.x - viewportWorldWidth * 0.34f
        cameraX = targetCamX.coerceIn(0f, (worldWidth - viewportWorldWidth).coerceAtLeast(0f))
    }

    private fun updatePlayer(dt: Float, input: InputControlsState) {
        if (phase != ArenaPhase.PLAYING) return
        if (player.invulnTimer > 0f) player.invulnTimer -= dt
        if (player.specialCooldown > 0f) player.specialCooldown = (player.specialCooldown - dt).coerceAtLeast(0f)
        if (fireCooldown > 0f) fireCooldown -= dt

        val moveSpeed = (360f * hero.bonusSpeed) * (1f + speedSkill * 0.08f)
        player.isCrouching = input.crouch && !player.isJumping

        player.vx = when {
            input.left && !input.right -> {
                player.facingRight = false
                -moveSpeed
            }
            input.right && !input.left -> {
                player.facingRight = true
                moveSpeed
            }
            else -> 0f
        }

        // Jump + Sustained Jetpack Lift while holding Jump
        if (input.jump) {
            if (!player.isJumping) {
                player.vy = -(560f + speedSkill * 20f)
                player.isJumping = true
                safeSfx("jump")
            } else {
                player.vy = (player.vy - 920f * dt).coerceAtLeast(-420f)
            }
            player.isJetpacking = true
            jetpackParticleTimer += dt
            if (jetpackParticleTimer >= 0.045f) {
                jetpackParticleTimer = 0f
                spawnExplosion(player.x + player.width / 2f, player.y + player.height, 0xFFE09658, 2)
            }
        } else {
            player.isJetpacking = false
            if (input.crouch && player.isJumping) {
                player.vy += 820f * dt
            }
        }

        val prevBottom = player.y + player.height
        player.x = (player.x + player.vx * dt).coerceIn(10f, worldWidth - player.width - 10f)
        player.vy += gravity * dt
        player.y = (player.y + player.vy * dt).coerceIn(48f, worldHeight - player.height)

        // One-way platform landing (drop down when holding crouch)
        var landed = false
        if (!input.crouch) {
            for (p in platforms) {
                val overlapsX = player.x + player.width > p.x + 8f && player.x < p.x + p.width - 8f
                if (overlapsX && player.vy >= 0f && prevBottom <= p.y + 14f && player.y + player.height >= p.y) {
                    player.y = p.y - player.height
                    player.vy = 0f
                    player.isJumping = false
                    landed = true
                    break
                }
            }
        }
        if (!landed && player.y + player.height >= groundY) {
            player.y = groundY - player.height
            player.vy = 0f
            player.isJumping = false
        }

        // Horizontal Side-Scrolling Fire (smart-aims at nearest enemy in view or fires straight ahead)
        if (input.shoot && player.hasGun && player.ammo > 0 && fireCooldown <= 0f) {
            fireCooldown = (weapon.fireRateMs / 1000f) * (1f - fireSkill * 0.05f)
            player.ammo--
            shotsFired++
            safeSfx("shoot")

            val muzzleX = if (player.facingRight) player.x + player.width + 4f else player.x - 4f
            val muzzleY = player.y + player.height * 0.42f
            val totalDmg = weapon.damage + fireSkill
            val speed = weapon.projectileSpeed * 58f

            val target = enemies
                .filter {
                    it.hp > 0 &&
                        !(it.kind == EnemyKind.BOSS_OVERLORD && bossKilled) &&
                        if (player.facingRight) it.x >= player.x - 40f else it.x <= player.x + 40f
                }
                .minByOrNull { hypot(it.x + it.width / 2f - muzzleX, it.y + it.height / 2f - muzzleY) }

            val aimAngle = if (target != null && hypot(target.x + target.width / 2f - muzzleX, target.y + target.height / 2f - muzzleY) < 620f) {
                atan2((target.y + target.height / 2f) - muzzleY, (target.x + target.width / 2f) - muzzleX)
            } else {
                if (player.facingRight) 0f else Math.PI.toFloat()
            }

            when (weapon.spreadCount) {
                1 -> bullets.add(
                    BulletEntity(muzzleX, muzzleY - 3f, cos(aimAngle) * speed, sin(aimAngle) * speed, 16f, 6f, totalDmg, true, weapon.colorHex)
                )
                2 -> {
                    listOf(-0.08f, 0.08f).forEach { offset ->
                        val a = aimAngle + offset
                        bullets.add(BulletEntity(muzzleX, muzzleY - 3f, cos(a) * speed, sin(a) * speed, 16f, 6f, totalDmg, true, weapon.colorHex))
                    }
                }
                else -> {
                    listOf(-0.18f, 0f, 0.18f).forEach { offset ->
                        val a = aimAngle + offset
                        bullets.add(BulletEntity(muzzleX, muzzleY - 3f, cos(a) * speed * 0.94f, sin(a) * speed * 0.94f, 15f, 6f, totalDmg, true, weapon.colorHex))
                    }
                }
            }
        }

        // Special Shockwave Attack
        if (input.specialTrigger && player.specialCooldown <= 0f) {
            player.specialCooldown = 8.0f
            safeSfx("explosion")
            spawnExplosion(player.x + player.width / 2f, player.y + player.height / 2f, hero.accentColor, 20)
            val defeated = mutableListOf<EnemyEntity>()
            val iter = enemies.iterator()
            while (iter.hasNext()) {
                val e = iter.next()
                if (e.hp <= 0) {
                    iter.remove()
                    continue
                }
                if (hypot(e.x - player.x, e.y - player.y) < 460f) {
                    e.hp -= (4 + fireSkill * 2)
                    if (e.hp <= 0) {
                        e.hp = 0
                        iter.remove()
                        defeated.add(e)
                    }
                }
            }
            defeated.forEach { onEnemyDefeated(it) }
            if (bossKilled && mode == GameMode.STORY && stageDef.objective == MissionObjective.BOSS_BATTLE) {
                finishBossBattleOnDefeat()
                return
            }
        }
    }

    private fun updateTeammates(dt: Float) {
        if (phase != ArenaPhase.PLAYING) return
        val squadBoost = 1f + droneSkill * 0.15f
        for (tm in teammates) {
            val formationX = (player.x - 75f * tm.offsetIndex).coerceIn(16f, worldWidth - 72f)
            val formationY = (player.y + 8f).coerceAtMost(groundY - 68f)
            tm.x += (formationX - tm.x) * (3.2f * dt)
            tm.y += (formationY - tm.y) * (3.2f * dt)

            var nearest: EnemyEntity? = null
            var bestDist = 920f
            for (e in enemies) {
                if (e.hp <= 0) continue
                if (e.kind == EnemyKind.BOSS_OVERLORD && bossKilled) continue
                val d = hypot(e.x - tm.x, e.y - tm.y)
                if (d < bestDist) {
                    bestDist = d
                    nearest = e
                }
            }
            if (nearest != null) {
                tm.facingRight = nearest.x >= tm.x
                tm.shootTimer -= dt * squadBoost
                if (tm.shootTimer <= 0f) {
                    tm.shootTimer = 1.05f + Random.nextFloat() * 0.65f
                    val angle = atan2((nearest.y + nearest.height / 2f) - (tm.y + 24f), (nearest.x + nearest.width / 2f) - (tm.x + 24f))
                    bullets.add(
                        BulletEntity(tm.x + 24f, tm.y + 24f, cos(angle) * 680f, sin(angle) * 680f, 12f, 5f, 1 + (droneSkill / 2), true, 0xFFF5E6C8)
                    )
                }
            }
        }
    }

    private fun updateBullets(dt: Float, viewportWorldWidth: Float) {
        if (phase != ArenaPhase.PLAYING) return
        val bIter = bullets.iterator()
        while (bIter.hasNext()) {
            if (phase != ArenaPhase.PLAYING) break
            val b = bIter.next()
            b.x += b.vx * dt
            b.y += b.vy * dt

            if (b.x < cameraX - 240f || b.x > cameraX + viewportWorldWidth + 240f || b.y < -80f || b.y > worldHeight + 80f) {
                bIter.remove()
                continue
            }

            if (b.fromPlayer) {
                var hit = false
                var defeatedEnemy: EnemyEntity? = null
                val eIter = enemies.iterator()
                while (eIter.hasNext()) {
                    val e = eIter.next()
                    if (e.hp <= 0) {
                        eIter.remove()
                        continue
                    }
                    if (rectsOverlap(b.x, b.y, b.width, b.height, e.x, e.y, e.width, e.height)) {
                        hit = true
                        shotsHit++
                        e.hp = (e.hp - b.damage).coerceAtLeast(0)
                        spawnExplosion(b.x, b.y, b.colorHex, 5)
                        if (e.kind == EnemyKind.BOSS_OVERLORD) {
                            safeSfx("boss_hit")
                            if (e.hp > 0 && e.hp <= e.maxHp / 2 && e.phase == 1) {
                                e.phase = 2
                                showBanner("باس وارد فاز دوم شد! حملات لیزری شدیدتر می‌شوند!")
                            }
                        } else {
                            safeSfx("hit")
                        }
                        if (e.hp <= 0) {
                            eIter.remove()
                            defeatedEnemy = e
                        }
                        break
                    }
                }
                if (hit) bIter.remove()
                if (defeatedEnemy != null) {
                    onEnemyDefeated(defeatedEnemy)
                    if (defeatedEnemy.kind == EnemyKind.BOSS_OVERLORD &&
                        mode == GameMode.STORY &&
                        stageDef.objective == MissionObjective.BOSS_BATTLE
                    ) {
                        checkMissionCompletion()
                        if (phase != ArenaPhase.PLAYING) break
                    }
                }
            } else {
                if (rectsOverlap(b.x, b.y, b.width, b.height, player.x, player.y, player.width, player.height)) {
                    bIter.remove()
                    damagePlayer(1)
                    if (phase != ArenaPhase.PLAYING) break
                }
            }
        }
        if (phase != ArenaPhase.PLAYING) {
            bullets.removeAll { !it.fromPlayer }
        }
    }

    private fun updateEnemies(dt: Float) {
        if (phase != ArenaPhase.PLAYING) return
        val snapshot = enemies.toList()
        for (e in snapshot) {
            if (phase != ArenaPhase.PLAYING) break
            if (e.hp <= 0) continue
            if (e.kind == EnemyKind.BOSS_OVERLORD && bossKilled) continue

            e.bobPhase += dt * 4f
            if (e.kind == EnemyKind.BOSS_OVERLORD) {
                e.x += e.speed * e.dir * dt
                val minBossX = (worldWidth - 480f).coerceAtLeast(360f)
                if (e.x < minBossX || e.x > worldWidth - e.width - 24f) e.dir *= -1f
                e.y = (groundY - 220f) + sin(e.bobPhase) * 44f
                e.shootTimer -= dt
                if (e.shootTimer <= 0f) {
                    e.shootTimer = if (e.phase == 2) 0.9f else 1.4f
                    val angle = atan2((player.y + 30f) - (e.y + e.height / 2f), (player.x + 25f) - e.x)
                    val spread = if (e.phase == 2) listOf(-0.24f, 0f, 0.24f) else listOf(-0.10f, 0.10f)
                    for (offset in spread) {
                        val a = angle + offset
                        bullets.add(
                            BulletEntity(e.x, e.y + e.height / 2f, cos(a) * 400f, sin(a) * 400f, 11f, 11f, 1, false, 0xFFE05252)
                        )
                    }
                }
            } else {
                if (mode == GameMode.ENDLESS || mode == GameMode.CHALLENGE || mode == GameMode.MULTIPLAYER) {
                    // Horizontal side-scroller ambush from the right toward player
                    val dx = player.x - e.x
                    e.dir = if (dx >= 0f) 1f else -1f
                    e.x += e.speed * e.dir * dt
                } else {
                    e.x += e.speed * e.dir * dt
                    if (e.x < 160f || e.x > worldWidth - e.width - 20f) e.dir *= -1f
                }

                if (e.kind == EnemyKind.FLYER || e.kind == EnemyKind.ALIEN_SHIP) {
                    e.y = (e.y + sin(e.bobPhase) * 32f * dt).coerceIn(80f, groundY - 90f)
                }

                e.shootTimer -= dt
                if (e.shootTimer <= 0f && hypot(player.x - e.x, player.y - e.y) < 920f) {
                    e.shootTimer = 1.9f + Random.nextFloat() * 1.4f
                    val angle = atan2((player.y + 30f) - (e.y + e.height / 2f), (player.x + 25f) - (e.x + e.width / 2f))
                    bullets.add(
                        BulletEntity(e.x + e.width / 2f, e.y + e.height / 2f, cos(angle) * 330f, sin(angle) * 330f, 10f, 7f, 1, false, 0xFFE09658)
                    )
                }
            }

            if (rectsOverlap(player.x, player.y, player.width, player.height, e.x, e.y, e.width, e.height)) {
                damagePlayer(1)
            }
        }
    }

    private fun onEnemyDefeated(e: EnemyEntity) {
        if (!defeatedEnemyIds.add(e.id)) return

        if (e.kind == EnemyKind.BOSS_OVERLORD) {
            bossKilled = true
            enemies.removeAll { it.kind == EnemyKind.BOSS_OVERLORD && it.hp <= 0 }
        }

        enemiesKilled++
        score += if (e.kind == EnemyKind.BOSS_OVERLORD) 850L else 120L
        player.ammo += (6 + luckSkill * 2)

        try {
            safeSfx("enemy_die")
            spawnExplosion(e.x + e.width / 2f, e.y + e.height / 2f, 0xFFC47B42, 14)

            if (e.kind == EnemyKind.SPLITTER) {
                repeat(2) { i ->
                    spawnEnemy(EnemyKind.MINI_SPLITTER, e.x + (if (i == 0) -25f else 25f), e.y)
                }
            }

            if (stageDef.objective == MissionObjective.OXYGEN_AND_PART && player.batteries < stageDef.targetBatteries) {
                pickups.add(PickupEntity(nextEntityId++, PickupKind.BATTERY, e.x, e.y))
            } else {
                val roll = Random.nextFloat()
                when {
                    roll < 0.36f -> pickups.add(PickupEntity(nextEntityId++, PickupKind.AMMO_PACK, e.x, e.y))
                    roll < 0.58f -> pickups.add(PickupEntity(nextEntityId++, PickupKind.CRYSTAL, e.x, e.y))
                    roll < 0.74f -> pickups.add(PickupEntity(nextEntityId++, PickupKind.HEALTH_PACK, e.x, e.y))
                }
            }
        } catch (_: Throwable) {
        }
    }

    private fun finishBossBattleOnDefeat() {
        try {
            enemies.removeAll { it.kind == EnemyKind.BOSS_OVERLORD || it.hp <= 0 }
            bullets.removeAll { !it.fromPlayer }
        } catch (_: Throwable) {
        }
        checkMissionCompletion()
    }

    private fun updatePickups(dt: Float) {
        val magnetRadius = 140f + luckSkill * 28f
        for (p in pickups.toList()) {
            if (p.collected) continue

            if (p.kind != PickupKind.GUN && p.kind != PickupKind.HOSTAGE && p.kind != PickupKind.ROCKET_PART) {
                val dx = (player.x + player.width / 2f) - (p.x + p.width / 2f)
                val dy = (player.y + player.height / 2f) - (p.y + p.height / 2f)
                val dist = hypot(dx, dy)
                if (dist < magnetRadius && dist > 1f) {
                    p.x += (dx / dist) * 320f * dt
                    p.y += (dy / dist) * 320f * dt
                }
            }

            if (rectsOverlap(player.x, player.y, player.width, player.height, p.x, p.y, p.width, p.height)) {
                p.collected = true
                when (p.kind) {
                    PickupKind.GUN -> {
                        player.hasGun = true
                        player.ammo += 25
                        safeSfx("gun")
                        showBanner("تفنگ لیزری برداشته شد! به سمت راست پیشروی کن و شلیک کن!")
                    }
                    PickupKind.BATTERY, PickupKind.POWERFUL_BATTERY -> {
                        player.batteries++
                        score += 100L
                        safeSfx("battery")
                        if (stageDef.objective == MissionObjective.OXYGEN_AND_PART &&
                            player.batteries >= stageDef.targetBatteries && !partSpawned
                        ) {
                            partSpawned = true
                            pickups.add(PickupEntity(nextEntityId++, PickupKind.ROCKET_PART, worldWidth - 230f, groundY - 190f, 44f, 44f))
                            showBanner("اکسیژن شارژ شد! به انتهای مسیر در سمت راست برو و قطعه موشک را بردار!")
                        }
                    }
                    PickupKind.ROCKET_PART -> {
                        player.partsCollected++
                        score += 350L
                        safeSfx("part")
                        if (stageDef.hasBetrayalChoice && mode == GameMode.STORY) {
                            phase = ArenaPhase.STORY_CHOICE
                        } else {
                            triggerVictory()
                        }
                    }
                    PickupKind.AMMO_PACK -> {
                        player.ammo += 20
                        safeSfx("powerup")
                    }
                    PickupKind.HEALTH_PACK -> {
                        player.health = (player.health + 2).coerceAtMost(player.maxHealth)
                        safeSfx("powerup")
                    }
                    PickupKind.HOSTAGE -> {
                        player.hostagesRescued++
                        score += 250L
                        safeSfx("part")
                        showBanner("یک فضانورد اسیر آزاد شد! (${player.hostagesRescued}/3)")
                    }
                    PickupKind.CRYSTAL -> {
                        score += 150L
                        safeSfx("battery")
                    }
                }
            }
        }
        pickups.removeAll { it.collected }
    }

    private fun checkMissionCompletion() {
        if (phase != ArenaPhase.PLAYING) return

        when (mode) {
            GameMode.ENDLESS -> {
                if (enemies.none { it.hp > 0 }) {
                    enemies.clear()
                    wave++
                    score += wave * 150L
                    showBanner("موج $wave آغاز شد!")
                    spawnInitialWave()
                }
            }
            GameMode.CHALLENGE, GameMode.MULTIPLAYER -> {
                if (enemies.none { it.hp > 0 }) {
                    enemies.clear()
                    triggerVictory()
                }
            }
            GameMode.STORY -> {
                val complete = when (stageDef.objective) {
                    MissionObjective.OXYGEN_AND_PART, MissionObjective.STORY_EXPEDITION -> false
                    MissionObjective.BOSS_BATTLE ->
                        bossKilled || enemies.none { it.kind == EnemyKind.BOSS_OVERLORD && it.hp > 0 }
                    MissionObjective.RESCUE_HOSTAGES ->
                        player.hostagesRescued >= 3 && enemies.none { it.hp > 0 }
                    MissionObjective.DEFEND_STATION, MissionObjective.ALIEN_PURGE ->
                        enemies.none { it.hp > 0 }
                }
                if (complete) {
                    if (stageDef.objective == MissionObjective.BOSS_BATTLE) {
                        bossKilled = true
                        enemies.removeAll { it.kind == EnemyKind.BOSS_OVERLORD || it.hp <= 0 }
                    }
                    bullets.removeAll { !it.fromPlayer }
                    if (stageDef.hasBetrayalChoice) {
                        phase = ArenaPhase.STORY_CHOICE
                    } else {
                        triggerVictory()
                    }
                }
            }
        }
    }

    private fun damagePlayer(amount: Int) {
        if (player.invulnTimer > 0f) return
        player.invulnTimer = 0.85f
        safeSfx("hit")
        spawnExplosion(player.x + player.width / 2f, player.y + player.height / 2f, 0xFFE05252, 8)
        if (player.shield > 0) {
            player.shield = (player.shield - amount).coerceAtLeast(0)
        } else {
            player.health = (player.health - amount).coerceAtLeast(0)
            if (player.health <= 0) {
                triggerGameOver("سلامت فضانورد به پایان رسید! مراقب شلیک بیگانه‌ها باش.")
            }
        }
    }

    private fun safeSfx(key: String) {
        try {
            onSfx(key)
        } catch (_: Throwable) {
        }
    }

    private fun triggerVictory() {
        if (phase == ArenaPhase.VICTORY) return
        phase = ArenaPhase.VICTORY
        safeSfx("rocket_launch")
    }

    private fun triggerGameOver(reason: String) {
        if (phase == ArenaPhase.GAME_OVER) return
        statusBanner = reason
        phase = ArenaPhase.GAME_OVER
        safeSfx("explosion")
    }

    private fun showBanner(msg: String) {
        statusBanner = msg
        statusBannerTimer = 4.5f
    }

    private fun spawnExplosion(x: Float, y: Float, colorHex: Long, count: Int) {
        try {
            val room = (maxParticles - particles.size).coerceAtLeast(0)
            val spawnCount = minOf(count, room)
            repeat(spawnCount) {
                val angle = Random.nextFloat() * (2f * Math.PI.toFloat())
                val speed = 60f + Random.nextFloat() * 190f
                particles.add(
                    ParticleEntity(
                        x = x,
                        y = y,
                        vx = cos(angle) * speed,
                        vy = sin(angle) * speed,
                        life = 0.45f,
                        maxLife = 0.45f,
                        colorHex = colorHex,
                        size = Random.nextFloat() * 5f + 3f
                    )
                )
            }
        } catch (_: Throwable) {
        }
    }

    private fun updateParticles(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val pt = iter.next()
            pt.x += pt.vx * dt
            pt.y += pt.vy * dt
            pt.life -= dt
            if (pt.life <= 0f) iter.remove()
        }
    }

    private fun rectsOverlap(ax: Float, ay: Float, aw: Float, ah: Float, bx: Float, by: Float, bw: Float, bh: Float): Boolean =
        ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by

    fun buildSnapshot(): HudSnapshot {
        val boss = enemies.firstOrNull { it.kind == EnemyKind.BOSS_OVERLORD && it.hp > 0 }
        val stars = when {
            phase != ArenaPhase.VICTORY -> 0
            player.health >= player.maxHealth - 1 && timeRemaining > 45f -> 3
            player.health >= 2 -> 2
            else -> 1
        }
        val xpEarned = if (phase == ArenaPhase.VICTORY) {
            stageDef.rewardXp + (enemiesKilled * 15)
        } else {
            (enemiesKilled * 10).coerceAtLeast(15)
        }
        val crystalsEarned = if (phase == ArenaPhase.VICTORY) {
            stageDef.rewardCrystals + (luckSkill * 12) + (if (mode == GameMode.CHALLENGE) 120 else 0)
        } else {
            (enemiesKilled * 5)
        }
        val objText = when (mode) {
            GameMode.ENDLESS -> "موج $wave | بقا و ثبت رکورد"
            GameMode.CHALLENGE -> "چالش زمان‌دار (${enemies.size} دشمن)"
            GameMode.MULTIPLAYER -> "دوئل با ربات ($score vs $rivalScore)"
            GameMode.STORY -> when (stageDef.objective) {
                MissionObjective.OXYGEN_AND_PART ->
                    if (!partSpawned) "باتری اکسیژن: ${player.batteries}/${stageDef.targetBatteries}"
                    else "به سمت راست برو و قطعه موشک را بردار!"
                MissionObjective.BOSS_BATTLE -> "شکست باس فصل (${boss?.hp ?: 0} HP)"
                MissionObjective.RESCUE_HOSTAGES -> "آزادی اسرا (${player.hostagesRescued}/3) و پاک‌سازی"
                MissionObjective.STORY_EXPEDITION -> "پیشروی به سمت کپسول در انتهای مسیر"
                else -> "دشمنان باقی‌مانده: ${enemies.size}"
            }
        }
        val horizProgress = ((player.x - 120f) / (worldWidth - 360f).coerceAtLeast(1f)).coerceIn(0f, 1f)

        return HudSnapshot(
            mode = mode,
            stageNumber = stageDef.stageNumber,
            phase = phase,
            stageTitle = stageDef.titleFa,
            objectiveText = objText,
            health = player.health,
            maxHealth = player.maxHealth,
            shield = player.shield,
            ammo = player.ammo,
            batteries = player.batteries,
            targetBatteries = stageDef.targetBatteries,
            enemiesRemaining = enemies.size,
            score = score,
            rivalScore = rivalScore,
            wave = wave,
            timeRemainingSec = timeRemaining.toInt(),
            bossHp = (boss?.hp ?: 0).coerceAtLeast(0),
            bossMaxHp = (boss?.maxHp ?: 0).coerceAtLeast(0),
            bossPhase = boss?.phase ?: 1,
            specialReadyRatio = (1f - (player.specialCooldown / 8.0f)).coerceIn(0f, 1f),
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
            horizontalProgress = horizProgress
        )
    }
}
