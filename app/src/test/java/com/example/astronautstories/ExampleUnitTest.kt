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
    fun storyTransitionVideos_matchChapterProgressionOrder() {
        assertEquals("videos/moon_to_mars.mp4", GameCatalog.getStage(10).transitionVideoAsset)
        assertNull(GameCatalog.getStage(1).transitionVideoAsset)
        assertNull(GameCatalog.getStage(20).transitionVideoAsset)
        assertEquals("videos/mars_to_saturn.mp4", GameCatalog.getStage(30).transitionVideoAsset)
        assertEquals("videos/saturn_to_jupiter.mp4", GameCatalog.getStage(40).transitionVideoAsset)
        assertEquals("videos/jupiter_to_mercury.mp4", GameCatalog.getStage(50).transitionVideoAsset)
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
}