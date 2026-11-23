package com.example.astronautstories

import com.example.astronautstories.domain.model.GameCatalog
import com.example.astronautstories.domain.model.GameMode
import com.example.astronautstories.domain.model.GraphicsQuality
import com.example.astronautstories.domain.model.HeroId
import com.example.astronautstories.domain.model.WeaponType
import com.example.astronautstories.engine.ArenaPhase
import com.example.astronautstories.engine.GameSimulation
import com.example.astronautstories.engine.InputControlsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun storyCampaign_hasTenChaptersWithFiveStagesAndDedicatedVideos() {
        assertEquals(10, GameCatalog.chapters.size)
        assertEquals(5, GameCatalog.STAGES_PER_CHAPTER)
        assertEquals(50, GameCatalog.allStages.size)
        assertEquals(150, GameCatalog.MAX_STARS)

        for (chapter in 1..GameCatalog.TOTAL_CHAPTERS) {
            val chapterStages = GameCatalog.allStages.filter { it.chapterIndex == chapter }
            assertEquals(5, chapterStages.size)
            chapterStages.take(4).forEach { stage ->
                assertNull("Non-boss stage ${stage.stageNumber} should not have transition video", stage.transitionVideoAsset)
                assertTrue(!stage.hasBoss)
            }
            val bossStage = chapterStages.last()
            assertTrue(bossStage.hasBoss)
            assertEquals(chapter * 5, bossStage.stageNumber)
            assertEquals(GameCatalog.chapters[chapter - 1].transitionVideoAsset, bossStage.transitionVideoAsset)
        }

        assertEquals("videos/moon_to_mars.mp4", GameCatalog.getStage(5).transitionVideoAsset)
        assertEquals("videos/station_to_mars.mp4", GameCatalog.getStage(10).transitionVideoAsset)
        assertEquals("videos/mars_to_saturn.mp4", GameCatalog.getStage(15).transitionVideoAsset)
        assertEquals("videos/saturn_to_jupiter.mp4", GameCatalog.getStage(20).transitionVideoAsset)
        assertEquals("videos/jupiter_to_mercury.mp4", GameCatalog.getStage(25).transitionVideoAsset)
        assertEquals("videos/mercury_to_uranus.mp4", GameCatalog.getStage(30).transitionVideoAsset)
        assertEquals("videos/uranus_to_neptune.mp4", GameCatalog.getStage(35).transitionVideoAsset)
        assertEquals("videos/neptune_to_citadel.mp4", GameCatalog.getStage(40).transitionVideoAsset)
        assertEquals("videos/citadel_to_earth.mp4", GameCatalog.getStage(45).transitionVideoAsset)
        assertEquals("videos/earth_return.mp4", GameCatalog.getStage(50).transitionVideoAsset)
    }

    @Test
    fun stageCompletion_marksVictoryStateProperly() {
        val stage4 = GameCatalog.getStage(4) // ALIEN_PURGE stage
        val sim = GameSimulation(
            mode = GameMode.STORY,
            stageDef = stage4,
            hero = HeroId.KOUROSH,
            weapon = WeaponType.PHOTON_SNIPER,
            skillLevels = emptyMap(),
            graphicsQuality = GraphicsQuality.MEDIUM,
            onSfx = {}
        )
        sim.startMission()
        assertEquals(ArenaPhase.PLAYING, sim.phase)

        sim.enemies.clear()
        sim.step(0.016f, InputControlsState(), 1280f)

        val snap = sim.buildSnapshot()
        assertEquals(ArenaPhase.VICTORY, snap.phase)
        assertTrue(snap.isVictory)
    }

    @Test
    fun movementControls_rightMovesPositiveAndLeftMovesNegative() {
        val sim = GameSimulation(
            mode = GameMode.STORY,
            stageDef = GameCatalog.getStage(1),
            hero = HeroId.KOUROSH,
            weapon = WeaponType.STANDARD_BLASTER,
            skillLevels = emptyMap(),
            graphicsQuality = GraphicsQuality.MEDIUM,
            onSfx = {}
        )
        sim.startMission()
        val startX = sim.player.x

        sim.step(0.05f, InputControlsState(right = true), 1280f)
        val afterRightX = sim.player.x
        assertTrue(afterRightX > startX)
        assertTrue(sim.player.facingRight)

        sim.step(0.05f, InputControlsState(left = true), 1280f)
        assertTrue(sim.player.x < afterRightX)
        assertTrue(!sim.player.facingRight)
    }

    @Test
    fun specialShockwave_defeatingSplitters_doesNotThrowConcurrentModification() {
        val sim = GameSimulation(
            mode = GameMode.STORY,
            stageDef = GameCatalog.getStage(2),
            hero = HeroId.KOUROSH,
            weapon = WeaponType.STANDARD_BLASTER,
            skillLevels = emptyMap(),
            graphicsQuality = GraphicsQuality.MEDIUM,
            onSfx = {}
        )
        sim.startMission()
        sim.enemies.clear()
        val px = sim.player.x
        val py = sim.player.y
        sim.enemies.add(
            com.example.astronautstories.engine.EnemyEntity(
                id = 9001,
                kind = com.example.astronautstories.domain.model.EnemyKind.SPLITTER,
                x = px + 40f,
                y = py,
                width = 48f,
                height = 48f,
                hp = 1,
                maxHp = 1,
                speed = 0f,
                shootTimer = 99f
            )
        )
        sim.step(0.016f, InputControlsState(specialTrigger = true), 1280f)
        assertTrue(sim.enemies.any { it.kind == com.example.astronautstories.domain.model.EnemyKind.MINI_SPLITTER })
    }

    @Test
    fun stageTransition_snapshotCarriesTargetModeAndStageNumber() {
        val stage1 = GameCatalog.getStage(1)
        val sim1 = GameSimulation(
            mode = GameMode.STORY,
            stageDef = stage1,
            hero = HeroId.KOUROSH,
            weapon = WeaponType.STANDARD_BLASTER,
            skillLevels = emptyMap(),
            graphicsQuality = GraphicsQuality.MEDIUM,
            onSfx = {}
        )
        val snap1 = sim1.buildSnapshot()
        assertEquals(GameMode.STORY, snap1.mode)
        assertEquals(1, snap1.stageNumber)
        assertEquals(ArenaPhase.BRIEFING, snap1.phase)

        val stage2 = GameCatalog.getStage(2)
        val sim2 = GameSimulation(
            mode = GameMode.STORY,
            stageDef = stage2,
            hero = HeroId.KOUROSH,
            weapon = WeaponType.STANDARD_BLASTER,
            skillLevels = emptyMap(),
            graphicsQuality = GraphicsQuality.MEDIUM,
            onSfx = {}
        )
        val snap2 = sim2.buildSnapshot()
        assertEquals(GameMode.STORY, snap2.mode)
        assertEquals(2, snap2.stageNumber)
        assertEquals(ArenaPhase.BRIEFING, snap2.phase)
    }

    @Test
    fun batteryCollection_withExtraPickupsPresent_spawnsRocketPartSafely() {
        val stage1 = GameCatalog.getStage(1)
        val sim = GameSimulation(
            mode = GameMode.STORY,
            stageDef = stage1,
            hero = HeroId.KOUROSH,
            weapon = WeaponType.STANDARD_BLASTER,
            skillLevels = emptyMap(),
            graphicsQuality = GraphicsQuality.MEDIUM,
            onSfx = {}
        )
        sim.startMission()
        sim.player.batteries = stage1.targetBatteries - 1
        sim.pickups.clear()
        sim.pickups.add(
            com.example.astronautstories.engine.PickupEntity(
                id = 8001,
                kind = com.example.astronautstories.engine.PickupKind.BATTERY,
                x = sim.player.x,
                y = sim.player.y
            )
        )
        sim.pickups.add(
            com.example.astronautstories.engine.PickupEntity(
                id = 8002,
                kind = com.example.astronautstories.engine.PickupKind.CRYSTAL,
                x = sim.player.x + 800f,
                y = sim.player.y
            )
        )

        sim.step(0.016f, InputControlsState(), 1280f)
        assertEquals(stage1.targetBatteries, sim.player.batteries)
        assertTrue(sim.pickups.any { it.kind == com.example.astronautstories.engine.PickupKind.ROCKET_PART })
    }

    @Test
    fun playerInitialAndMaxHealth_isAtLeastFiveAcrossStagesAndHeroes() {
        HeroId.entries.forEach { hero ->
            assertTrue("Hero ${hero.id} bonusHp must be at least 5", hero.bonusHp >= 5)
            val sim = GameSimulation(
                mode = GameMode.STORY,
                stageDef = GameCatalog.getStage(1),
                hero = hero,
                weapon = WeaponType.STANDARD_BLASTER,
                skillLevels = emptyMap(),
                graphicsQuality = GraphicsQuality.MEDIUM,
                onSfx = {}
            )
            val snap = sim.buildSnapshot()
            assertTrue(snap.health >= 5)
            assertTrue(snap.maxHealth >= 5)
            assertEquals(snap.maxHealth, snap.health)
        }

        val kouroshStage1 = GameSimulation(
            mode = GameMode.STORY,
            stageDef = GameCatalog.getStage(1),
            hero = HeroId.KOUROSH,
            weapon = WeaponType.STANDARD_BLASTER,
            skillLevels = emptyMap(),
            graphicsQuality = GraphicsQuality.MEDIUM,
            onSfx = {}
        )
        assertEquals(5, kouroshStage1.player.health)
        assertEquals(5, kouroshStage1.player.maxHealth)

        val kouroshStage2 = GameSimulation(
            mode = GameMode.STORY,
            stageDef = GameCatalog.getStage(2),
            hero = HeroId.KOUROSH,
            weapon = WeaponType.STANDARD_BLASTER,
            skillLevels = emptyMap(),
            graphicsQuality = GraphicsQuality.MEDIUM,
            onSfx = {}
        )
        assertEquals(5, kouroshStage2.player.health)
        assertEquals(5, kouroshStage2.player.maxHealth)
    }

    @Test
    fun stage5BossDefeat_triggersVictoryWithChapterVideoAndStage6Initializes() {
        val stage5 = GameCatalog.getStage(5)
        assertTrue(stage5.hasBoss)
        assertEquals("videos/moon_to_mars.mp4", stage5.transitionVideoAsset)

        val sim5 = GameSimulation(
            mode = GameMode.STORY,
            stageDef = stage5,
            hero = HeroId.KOUROSH,
            weapon = WeaponType.PHOTON_SNIPER,
            skillLevels = emptyMap(),
            graphicsQuality = GraphicsQuality.MEDIUM,
            onSfx = { throw RuntimeException("Simulated audio failure must not crash victory") }
        )
        sim5.startMission()
        assertEquals(ArenaPhase.PLAYING, sim5.phase)

        val boss = sim5.enemies.first { it.kind == com.example.astronautstories.domain.model.EnemyKind.BOSS_OVERLORD }
        boss.hp = 1
        sim5.player.x = boss.x - 120f
        sim5.cameraX = (boss.x - 300f).coerceAtLeast(0f)
        sim5.bullets.add(
            com.example.astronautstories.engine.BulletEntity(
                x = boss.x + 10f,
                y = boss.y + 10f,
                vx = 0f,
                vy = 0f,
                width = 20f,
                height = 20f,
                damage = 5,
                fromPlayer = true,
                colorHex = 0xFFFFFFFFL
            )
        )

        sim5.step(0.016f, InputControlsState(), 1280f)
        val snap5 = sim5.buildSnapshot()
        assertEquals(ArenaPhase.VICTORY, snap5.phase)
        assertTrue(snap5.isVictory)
        assertTrue(snap5.bossKilled)

        val stage6 = GameCatalog.getStage(6)
        assertEquals(2, stage6.chapterIndex)
        val sim6 = GameSimulation(
            mode = GameMode.STORY,
            stageDef = stage6,
            hero = HeroId.KOUROSH,
            weapon = WeaponType.PHOTON_SNIPER,
            skillLevels = emptyMap(),
            graphicsQuality = GraphicsQuality.MEDIUM,
            onSfx = {}
        )
        val snap6 = sim6.buildSnapshot()
        assertEquals(6, snap6.stageNumber)
        assertEquals(ArenaPhase.BRIEFING, snap6.phase)
        assertTrue(snap6.health >= 5)
    }

    @Test
    fun stage5BossDefeat_viaShockwave_stopsBossAndMinionAttacksOnDeathFrame() {
        val stage5 = GameCatalog.getStage(5)
        val sim5 = GameSimulation(
            mode = GameMode.STORY,
            stageDef = stage5,
            hero = HeroId.KOUROSH,
            weapon = WeaponType.PHOTON_SNIPER,
            skillLevels = emptyMap(),
            graphicsQuality = GraphicsQuality.MEDIUM,
            onSfx = {}
        )
        sim5.startMission()
        val boss = sim5.enemies.first { it.kind == com.example.astronautstories.domain.model.EnemyKind.BOSS_OVERLORD }
        boss.hp = 1
        sim5.player.x = boss.x - 100f
        sim5.player.y = boss.y
        sim5.player.health = 1
        sim5.player.shield = 0
        // Enemy bullet overlapping player on the exact frame boss dies via shockwave
        sim5.bullets.add(
            com.example.astronautstories.engine.BulletEntity(
                x = sim5.player.x + 10f,
                y = sim5.player.y + 10f,
                vx = 0f,
                vy = 0f,
                width = 12f,
                height = 12f,
                damage = 1,
                fromPlayer = false,
                colorHex = 0xFFE05252L
            )
        )

        sim5.step(0.016f, InputControlsState(specialTrigger = true), 1280f)
        val snap = sim5.buildSnapshot()
        assertEquals(ArenaPhase.VICTORY, snap.phase)
        assertTrue(snap.bossKilled)
        assertEquals(0, snap.bossHp)
        assertEquals(0, snap.bossMaxHp)
        assertTrue(sim5.bullets.none { !it.fromPlayer })
    }

    @Test
    fun stage5BossDefeat_multiBulletHit_countsSingleBossKillAndClearsBoss() {
        val stage5 = GameCatalog.getStage(5)
        val sim5 = GameSimulation(
            mode = GameMode.STORY,
            stageDef = stage5,
            hero = HeroId.KOUROSH,
            weapon = WeaponType.LASER_SHOTGUN,
            skillLevels = emptyMap(),
            graphicsQuality = GraphicsQuality.MEDIUM,
            onSfx = {}
        )
        sim5.startMission()
        val boss = sim5.enemies.first { it.kind == com.example.astronautstories.domain.model.EnemyKind.BOSS_OVERLORD }
        boss.hp = 1
        boss.phase = 1
        sim5.player.x = boss.x - 140f
        sim5.cameraX = (boss.x - 300f).coerceAtLeast(0f)

        repeat(3) {
            sim5.bullets.add(
                com.example.astronautstories.engine.BulletEntity(
                    x = boss.x + 15f,
                    y = boss.y + 15f,
                    vx = 0f,
                    vy = 0f,
                    width = 18f,
                    height = 18f,
                    damage = 2,
                    fromPlayer = true,
                    colorHex = 0xFFFF6D00L
                )
            )
        }

        sim5.step(0.016f, InputControlsState(), 1280f)
        val snap = sim5.buildSnapshot()
        assertEquals(ArenaPhase.VICTORY, snap.phase)
        assertTrue(snap.bossKilled)
        assertEquals(1, snap.enemiesKilled)
        assertEquals(1, boss.phase)
        assertEquals(0, snap.bossHp)
    }
}