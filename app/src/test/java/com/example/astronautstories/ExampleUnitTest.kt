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
}