package com.example.astronautstories.boss3d

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.astronautstories.domain.model.HeroId
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

class Boss3DGLRenderer(
    context: Context,
    initialSimulation: Boss3DSimulation
) : GLSurfaceView.Renderer {

    private val appContext: Context = context.applicationContext

    @Volatile
    private var activeSimulation: Boss3DSimulation = initialSimulation

    @Volatile
    private var pendingTextureReload = false

    @Volatile
    private var resetVisualStateRequested = false

    @Volatile
    private var loadedStageNumber = -1

    @Volatile
    private var loadedHero: HeroId? = null

    private var lastBoundSim: Boss3DSimulation? = null

    private val frame = Boss3DRenderBuffer()

    // Pre-allocated matrices (zero allocation per frame)
    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // Reusable color & camera scratch values
    private val scratchColor = FloatArray(4)
    private var camX = 0f
    private var camY = 5f
    private var camZ = 15f

    // Smooth visual fade / intro-exit & natural walking animation state
    private var lastFrameNano = 0L
    private var smoothBossAlpha = 0f
    private var smoothBossIntroProgress = 0.85f
    private var walkCyclePhase = 0f
    private var walkBlend = 0f
    private var heroFacingSign = 1f

    // Shared GPU Meshes (created once on surface creation, deleted on release)
    private var billboardQuadMesh: GpuIndexedMesh? = null
    private var sphereMesh: GpuIndexedMesh? = null
    private var cylinderMesh: GpuIndexedMesh? = null
    private var boxMesh: GpuIndexedMesh? = null
    private var ringMesh: GpuIndexedMesh? = null
    private var starfieldMesh: GpuSimpleLinesMesh? = null
    private var deckGridMesh: GpuSimpleLinesMesh? = null

    // Environment & Surface Textures (POT mipmapped GL_LINEAR)
    private var texWhite = 0
    private var texPlanet = 0
    private var texDeck = 0

    // Transparent Cutout Camera-Facing Sprite Textures (aspect-ratio preserved, zero edge halo)
    private var texBoss = Boss3DGlTexture(0, 1f)
    private var texSatellite = Boss3DGlTexture(0, 1f)
    private var texHeroIdle = Boss3DGlTexture(0, 1f)
    private var texHeroWalk = Boss3DGlTexture(0, 1f)
    private var texHeroJump = Boss3DGlTexture(0, 1f)
    private var texDrone = Boss3DGlTexture(0, 1f)
    private var texAmmo = Boss3DGlTexture(0, 1f)
    private var texHealth = Boss3DGlTexture(0, 1f)
    private var texGlowOrb = Boss3DGlTexture(0, 1f)
    private var texReticle = Boss3DGlTexture(0, 1f)

    // Pre-allocated back-to-front sprite sort pool (max 24 simultaneous sprite billboards)
    private class SpriteDrawSlot {
        var tex = Boss3DGlTexture(0, 1f)
        var x = 0f
        var y = 0f
        var z = 0f
        var height = 1f
        var widthScale = 1f
        var rollRad = 0f
        var depthBias = 0f
        var flipX = false
        var tintR = 1f
        var tintG = 1f
        var tintB = 1f
        var tintMix = 0.16f
        var flash = 0f
        var opacity = 1f
        var distSq = 0f
    }

    private val spritePool = Array(MAX_SPRITE_SLOTS) { SpriteDrawSlot() }
    private val spriteOrder = IntArray(MAX_SPRITE_SLOTS)
    private var spriteCount = 0

    // Lit 3D Environment Shader Handles
    private var litProgram = 0
    private var litPosAttr = -1
    private var litNormalAttr = -1
    private var litUvAttr = -1
    private var litMvpLoc = -1
    private var litModelLoc = -1
    private var litColorLoc = -1
    private var litEmissiveLoc = -1
    private var litLightDirLoc = -1
    private var litCamPosLoc = -1
    private var litRimColorLoc = -1
    private var litFogColorLoc = -1
    private var litFogDensityLoc = -1
    private var litBossLightPosLoc = -1
    private var litBossLightColorLoc = -1
    private var litTextureLoc = -1
    private var litTexMixLoc = -1

    // Camera-Facing Billboard Cutout & Glow Shader Handles
    private var billboardProgram = 0
    private var bbPosAttr = -1
    private var bbUvAttr = -1
    private var bbMvpLoc = -1
    private var bbFlipXLoc = -1
    private var bbTextureLoc = -1
    private var bbTintLoc = -1
    private var bbFlashLoc = -1
    private var bbOpacityLoc = -1
    private var bbSoftCutLoc = -1
    private var bbFogColorLoc = -1
    private var bbFogFactorLoc = -1

    // Unlit FX Shader Handles (Grid, Telegraphs, Shockwaves, Shield)
    private var unlitProgram = 0
    private var unlitPosAttr = -1
    private var unlitMvpLoc = -1
    private var unlitColorLoc = -1
    private var unlitPointSizeLoc = -1

    // Anti-Aliased Circular Starfield Shader Handles
    private var pointProgram = 0
    private var pointPosAttr = -1
    private var pointMvpLoc = -1
    private var pointColorLoc = -1
    private var pointSizeLoc = -1

    private var surfaceWidth = 1
    private var surfaceHeight = 1

    /**
     * Safely binds a fresh simulation instance on Stage Restart or transition without
     * tearing down the GLSurfaceView/EGLContext or leaking/corrupting OpenGL texture IDs.
     */
    fun bindSimulation(newSimulation: Boss3DSimulation) {
        activeSimulation = newSimulation
        resetVisualStateRequested = true
        if (newSimulation.stageDef.stageNumber != loadedStageNumber || newSimulation.hero != loadedHero) {
            pendingTextureReload = true
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)

        val sim = activeSimulation
        pendingTextureReload = false
        resetVisualStateRequested = false
        lastBoundSim = sim

        resetVisualStateInternal()
        compileShaders()
        createMeshes()
        loadTextures(sim)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceWidth = width.coerceAtLeast(1)
        surfaceHeight = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
        val aspect = surfaceWidth.toFloat() / surfaceHeight.toFloat()
        Matrix.perspectiveM(projMatrix, 0, 52f, aspect, 0.4f, 180f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val sim = activeSimulation
        val simInstanceChanged = sim !== lastBoundSim
        if (simInstanceChanged) {
            lastBoundSim = sim
            resetVisualStateRequested = true
        }

        if (resetVisualStateRequested) {
            resetVisualStateRequested = false
            resetVisualStateInternal()
        }

        if (litProgram == 0 || unlitProgram == 0 || billboardProgram == 0) {
            compileShaders()
        }
        if (billboardQuadMesh == null || sphereMesh == null || cylinderMesh == null || ringMesh == null) {
            createMeshes()
        }

        val needsTextureReload = pendingTextureReload ||
            sim.stageDef.stageNumber != loadedStageNumber ||
            sim.hero != loadedHero ||
            !hasValidGpuTextures()
        if (needsTextureReload) {
            pendingTextureReload = false
            loadTextures(sim)
        }

        val nowNano = System.nanoTime()
        val dt = if (lastFrameNano == 0L) 0.016f else ((nowNano - lastFrameNano) * 1e-9f).coerceIn(0.001f, 0.05f)
        lastFrameNano = nowNano

        sim.copyLatestRenderBuffer(frame)
        updateVisualTransitions(dt)

        GLES20.glClearColor(frame.skyTop[0], frame.skyTop[1], frame.skyTop[2], 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        if (litProgram == 0 || unlitProgram == 0 || billboardProgram == 0) return

        // Camera behind player looking toward arena & boss with refined damped impact shake
        val shakeAmp = frame.screenShake * 0.60f
        val shakeX = if (shakeAmp > 0.001f) sin(frame.timeSec * 58f) * shakeAmp else 0f
        val shakeY = if (shakeAmp > 0.001f) cos(frame.timeSec * 47f) * shakeAmp * 0.75f else 0f
        val camDist = 11.2f
        val horizDist = camDist * cos(frame.camPitch)
        val camHeight = 2.6f + camDist * sin(frame.camPitch)
        camX = frame.playerX - sin(frame.camYaw) * horizDist + shakeX
        camY = frame.playerY + camHeight + shakeY
        camZ = frame.playerZ + cos(frame.camYaw) * horizDist

        val lookX = frame.playerX + sin(frame.camYaw) * 6.5f
        val lookY = frame.playerY + 1.85f
        val lookZ = frame.playerZ - cos(frame.camYaw) * 6.5f

        Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, lookX, lookY, lookZ, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        val fogDensity = if (frame.isDarkEclipse) 0.020f else 0.006f

        // 1. Anti-Aliased Circular Starfield (1 draw call)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, frame.timeSec * 1.3f, 0f, 1f, 0f)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        if (pointProgram != 0) {
            GLES20.glUseProgram(pointProgram)
            GLES20.glUniformMatrix4fv(pointMvpLoc, 1, false, mvpMatrix, 0)
            GLES20.glUniform4f(pointColorLoc, 0.92f, 0.96f, 1.0f, 0.90f)
            GLES20.glUniform1f(pointSizeLoc, 4.8f)
            starfieldMesh?.draw(pointPosAttr)
        } else {
            GLES20.glUseProgram(unlitProgram)
            GLES20.glUniformMatrix4fv(unlitMvpLoc, 1, false, mvpMatrix, 0)
            GLES20.glUniform4f(unlitColorLoc, 0.90f, 0.94f, 1.0f, 0.85f)
            GLES20.glUniform1f(unlitPointSizeLoc, 3.0f)
            starfieldMesh?.draw(unlitPosAttr)
        }
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        // 2. Cosmic Nebula Backdrop Glow behind Chapter Planet (Additive Camera-Facing Billboard)
        val planetRadius = if (frame.isGrandFinale) 17.5f else 12.8f
        GLES20.glDepthMask(false)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glUseProgram(billboardProgram)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glUniform1i(bbTextureLoc, 0)
        GLES20.glEnable(GLES20.GL_BLEND)
        drawCameraFacingBillboard(
            texture = texGlowOrb,
            worldX = -10f, worldY = 9f, worldZ = -52f,
            baseHeight = planetRadius * 3.0f,
            depthBiasTowardCamera = -planetRadius * 1.15f,
            tintR = frame.coreColor[0], tintG = frame.coreColor[1], tintB = frame.coreColor[2],
            tintMix = 1f,
            opacity = 0.34f,
            softCutout = false,
            additiveBlend = true,
            fogDensity = 0f
        )
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDepthMask(true)
        GLES20.glEnable(GLES20.GL_CULL_FACE)

        // 3. Lit 3D Cosmic Arena & Environmental Architecture (Dynamic Boss Core Lighting)
        GLES20.glUseProgram(litProgram)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glUniform1i(litTextureLoc, 0)
        val lightAngle = frame.timeSec * 0.25f
        GLES20.glUniform3f(litLightDirLoc, 0.42f + sin(lightAngle) * 0.12f, 0.82f, 0.38f + cos(lightAngle) * 0.10f)
        GLES20.glUniform3f(litCamPosLoc, camX, camY, camZ)
        GLES20.glUniform4fv(litFogColorLoc, 1, frame.skyBottom, 0)
        GLES20.glUniform1f(litFogDensityLoc, fogDensity)
        GLES20.glUniform3f(litBossLightPosLoc, frame.bossX, frame.bossY, frame.bossZ)
        val coreLightIntensity = smoothBossAlpha * (0.65f + frame.bossPhase * 0.18f + frame.bossHitFlash * 0.55f)
        GLES20.glUniform3f(
            litBossLightColorLoc,
            frame.coreColor[0] * coreLightIntensity,
            frame.coreColor[1] * coreLightIntensity,
            frame.coreColor[2] * coreLightIntensity
        )

        // Giant Chapter Planet / Earth (mapped seamlessly via equirectangular planet texture)
        drawLitMesh(
            mesh = sphereMesh,
            tx = -10f, ty = 9f, tz = -52f,
            rx = 0f, ry = frame.timeSec * 4f, rz = 12f,
            sx = planetRadius, sy = planetRadius, sz = planetRadius,
            color = frame.planetColor,
            emissive = 0.26f,
            rim = frame.coreColor,
            textureId = texPlanet,
            texMix = 0.92f
        )
        if (frame.hasPlanetRing) {
            GLES20.glDisable(GLES20.GL_CULL_FACE)
            drawLitMesh(
                mesh = ringMesh,
                tx = -10f, ty = 9f, tz = -52f,
                rx = 22f, ry = frame.timeSec * 3f, rz = 14f,
                sx = planetRadius * 1.75f, sy = 1f, sz = planetRadius * 1.75f,
                color = frame.planetRingColor,
                emissive = 0.38f,
                rim = frame.planetRingColor,
                textureId = texWhite,
                texMix = 0f
            )
            GLES20.glEnable(GLES20.GL_CULL_FACE)
        }

        // Main Floating Planetary Arena Combat Deck (radial Sci-Fi platform texture)
        drawLitMesh(
            mesh = cylinderMesh,
            tx = 0f, ty = -0.6f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = frame.arenaRadius, sy = 1.2f, sz = frame.arenaRadius,
            color = frame.deckColor,
            emissive = 0.10f,
            rim = frame.deckTrim,
            textureId = texDeck,
            texMix = 0.88f
        )

        // Glowing Perimeter Ring around Deck
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        drawLitMesh(
            mesh = ringMesh,
            tx = 0f, ty = 0.03f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = frame.arenaRadius * 1.015f, sy = 1f, sz = frame.arenaRadius * 1.015f,
            color = frame.deckTrim,
            emissive = 0.72f,
            rim = frame.deckTrim,
            textureId = texWhite,
            texMix = 0f
        )
        GLES20.glEnable(GLES20.GL_CULL_FACE)

        // Tactical Cover Pillars
        for (i in 0 until frame.pillarCount) {
            val base = i * 3
            val px = frame.pillarXZR[base]
            val pz = frame.pillarXZR[base + 1]
            val pr = frame.pillarXZR[base + 2]
            drawLitMesh(
                mesh = cylinderMesh,
                tx = px, ty = 1.15f, tz = pz,
                rx = 0f, ry = 0f, rz = 0f,
                sx = pr * 0.9f, sy = 2.3f, sz = pr * 0.9f,
                color = frame.hullColor,
                emissive = 0.10f,
                rim = frame.deckTrim,
                textureId = texDeck,
                texMix = 0.60f
            )
            drawLitMesh(
                mesh = boxMesh,
                tx = px, ty = 2.35f, tz = pz,
                rx = 0f, ry = frame.timeSec * 25f + i * 45f, rz = 0f,
                sx = pr * 1.3f, sy = 0.28f, sz = pr * 1.3f,
                color = frame.deckTrim,
                emissive = 0.50f,
                rim = frame.deckTrim,
                textureId = texWhite,
                texMix = 0f
            )
        }

        // Boss 3D Orbital Rings & Grand Finale Crown Architecture (3D frame around Boss Sprite)
        if (smoothBossAlpha > 0.02f) {
            drawBoss3DFrame()
        }

        // 4. Flat Ground Grid, Telegraphs & Shockwave Rings
        GLES20.glUseProgram(unlitProgram)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glDepthMask(false)

        // Deck Radial Grid
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.scaleM(modelMatrix, 0, frame.arenaRadius * 0.98f, 1f, frame.arenaRadius * 0.98f)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(unlitMvpLoc, 1, false, mvpMatrix, 0)
        GLES20.glUniform4f(unlitColorLoc, frame.deckTrim[0], frame.deckTrim[1], frame.deckTrim[2], 0.20f)
        deckGridMesh?.draw(unlitPosAttr)

        // Boss Ground Projection Ring
        if (smoothBossAlpha > 0.02f) {
            val groundPulse = 1f + sin(frame.timeSec * 3.5f) * 0.06f
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, frame.bossX, 0.045f, frame.bossZ)
            Matrix.scaleM(modelMatrix, 0, frame.bossScale * 1.25f * groundPulse, 1f, frame.bossScale * 1.25f * groundPulse)
            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
            GLES20.glUniformMatrix4fv(unlitMvpLoc, 1, false, mvpMatrix, 0)
            GLES20.glUniform4f(
                unlitColorLoc,
                frame.coreColor[0], frame.coreColor[1], frame.coreColor[2],
                0.42f * smoothBossAlpha
            )
            ringMesh?.draw(unlitPosAttr)
        }

        // Telegraph Hazard Circles & Sweeping Beams
        for (i in 0 until frame.telegraphCount) {
            val base = i * 7
            val isBeam = frame.telegraphData[base] > 0.5f
            val tx = frame.telegraphData[base + 1]
            val tz = frame.telegraphData[base + 2]
            val yaw = frame.telegraphData[base + 3]
            val radius = frame.telegraphData[base + 4]
            val length = frame.telegraphData[base + 5]
            val progress = frame.telegraphData[base + 6]
            val firing = progress > 1.5f

            if (!isBeam) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, tx, 0.06f, tz)
                Matrix.scaleM(modelMatrix, 0, radius, 1f, radius)
                Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
                GLES20.glUniformMatrix4fv(unlitMvpLoc, 1, false, mvpMatrix, 0)
                GLES20.glUniform4f(
                    unlitColorLoc,
                    frame.hazardColor[0], frame.hazardColor[1], frame.hazardColor[2],
                    if (firing) 0.95f else 0.78f
                )
                ringMesh?.draw(unlitPosAttr)

                val fillScale = if (firing) radius else (radius * progress.coerceIn(0.1f, 1f))
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, tx, 0.05f, tz)
                Matrix.scaleM(modelMatrix, 0, fillScale, 0.04f, fillScale)
                Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
                GLES20.glUniformMatrix4fv(unlitMvpLoc, 1, false, mvpMatrix, 0)
                GLES20.glUniform4f(
                    unlitColorLoc,
                    frame.hazardColor[0], frame.hazardColor[1], frame.hazardColor[2],
                    if (firing) 0.72f else 0.28f
                )
                cylinderMesh?.draw(unlitPosAttr)
            } else {
                val yawDeg = Math.toDegrees(yaw.toDouble()).toFloat()
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, tx, if (firing) 1.0f else 0.07f, tz)
                Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
                Matrix.translateM(modelMatrix, 0, 0f, 0f, length * 0.5f)
                Matrix.scaleM(modelMatrix, 0, radius * 1.4f, if (firing) 1.2f else 0.05f, length)
                Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
                GLES20.glUniformMatrix4fv(unlitMvpLoc, 1, false, mvpMatrix, 0)
                GLES20.glUniform4f(
                    unlitColorLoc,
                    frame.hazardColor[0], frame.hazardColor[1], frame.hazardColor[2],
                    if (firing) 0.85f else (0.22f + progress * 0.35f)
                )
                boxMesh?.draw(unlitPosAttr)
            }
        }

        // Expanding 3D Shockwave Rings
        for (i in 0 until frame.shockCount) {
            val base = i * 7
            val sx = frame.shockData[base]
            val sy = frame.shockData[base + 1]
            val sz = frame.shockData[base + 2]
            val sr = frame.shockData[base + 3]
            val r = frame.shockData[base + 4]
            val g = frame.shockData[base + 5]
            val b = frame.shockData[base + 6]

            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, sx, sy, sz)
            Matrix.scaleM(modelMatrix, 0, sr, 1f, sr)
            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
            GLES20.glUniformMatrix4fv(unlitMvpLoc, 1, false, mvpMatrix, 0)
            GLES20.glUniform4f(unlitColorLoc, r, g, b, 0.88f)
            ringMesh?.draw(unlitPosAttr)
        }

        // 5. Ground Contact Shadows & Pickup Glow Beacons (on deck surface)
        GLES20.glUseProgram(billboardProgram)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glUniform1i(bbTextureLoc, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texGlowOrb.texId)
        GLES20.glUniform1f(bbFlipXLoc, 0f)
        GLES20.glUniform1f(bbSoftCutLoc, 0f)
        GLES20.glUniform1f(bbFlashLoc, 0f)
        GLES20.glUniform4fv(bbFogColorLoc, 1, frame.skyBottom, 0)

        // Hero contact shadow disc
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        val heroShadowRadius = (0.78f / (1f + frame.playerY * 0.25f)).coerceIn(0.32f, 0.82f)
        drawGroundDisc(
            x = frame.playerX, y = 0.042f, z = frame.playerZ,
            radius = heroShadowRadius,
            r = 0.02f, g = 0.03f, b = 0.06f,
            opacity = 0.56f
        )

        // Boss contact shadow disc
        if (smoothBossAlpha > 0.02f) {
            drawGroundDisc(
                x = frame.bossX, y = 0.040f, z = frame.bossZ,
                radius = frame.bossScale * 1.35f,
                r = 0.02f, g = 0.03f, b = 0.07f,
                opacity = 0.52f * smoothBossAlpha
            )
        }

        // Pickup glow beacons
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE)
        for (i in 0 until frame.pickupCount) {
            val base = i * 3
            val px = frame.pickupXZType[base]
            val pz = frame.pickupXZType[base + 1]
            val isHealth = frame.pickupXZType[base + 2] > 0.5f
            val pulse = 0.85f + sin(frame.timeSec * 4.5f + i) * 0.15f
            drawGroundDisc(
                x = px, y = 0.046f, z = pz,
                radius = 0.72f * pulse,
                r = if (isHealth) 0.18f else 0.15f,
                g = if (isHealth) 0.96f else 0.85f,
                b = if (isHealth) 0.45f else 1.0f,
                opacity = 0.52f
            )
        }

        // 6. Boss Core Energy Aura & Wing Thruster Flares (Additive Camera-Facing Billboards)
        if (smoothBossAlpha > 0.02f) {
            val auraPulse = 1f + sin(frame.timeSec * (3.6f + frame.bossPhase * 1.6f)) * 0.08f
            val auraHeight = frame.bossScale * 2.55f * auraPulse * smoothBossIntroProgress
            drawCameraFacingBillboard(
                texture = texGlowOrb,
                worldX = frame.bossX, worldY = frame.bossY + 0.15f, worldZ = frame.bossZ,
                baseHeight = auraHeight,
                depthBiasTowardCamera = -0.35f,
                tintR = frame.coreColor[0], tintG = frame.coreColor[1], tintB = frame.coreColor[2],
                tintMix = 1f,
                flash = frame.bossHitFlash * 0.45f,
                opacity = (0.44f + frame.bossPhase * 0.08f) * smoothBossAlpha,
                softCutout = false,
                additiveBlend = true,
                fogDensity = fogDensity
            )

            if (frame.hasDualWingCannons) {
                val camRightX = viewMatrix[0]
                val camRightY = viewMatrix[4]
                val camRightZ = viewMatrix[8]
                val wingOffset = frame.bossScale * 1.12f * smoothBossIntroProgress
                for (side in -1..1 step 2) {
                    drawCameraFacingBillboard(
                        texture = texGlowOrb,
                        worldX = frame.bossX + camRightX * wingOffset * side,
                        worldY = frame.bossY + camRightY * wingOffset * side - 0.1f,
                        worldZ = frame.bossZ + camRightZ * wingOffset * side,
                        baseHeight = frame.bossScale * 0.85f * auraPulse,
                        depthBiasTowardCamera = -0.18f,
                        tintR = frame.hazardColor[0], tintG = frame.hazardColor[1], tintB = frame.hazardColor[2],
                        tintMix = 1f,
                        opacity = 0.42f * smoothBossAlpha,
                        softCutout = false,
                        additiveBlend = true,
                        fogDensity = fogDensity
                    )
                }
            }
        }

        // Hero Jetpack Thruster Glow
        if (frame.playerJetpacking) {
            val flicker = 0.85f + sin(frame.timeSec * 38f) * 0.15f
            drawCameraFacingBillboard(
                texture = texGlowOrb,
                worldX = frame.playerX, worldY = frame.playerY + 0.25f, worldZ = frame.playerZ,
                baseHeight = 0.96f * flicker,
                depthBiasTowardCamera = -0.10f,
                tintR = 1.0f, tintG = 0.56f, tintB = 0.16f,
                tintMix = 1f,
                opacity = 0.78f,
                softCutout = false,
                additiveBlend = true,
                fogDensity = fogDensity
            )
        }

        // 7. Depth-Sorted Camera-Facing 2D Character / Boss / Pickup / Satellite Cutout Sprites
        renderSortedCutoutBillboards(fogDensity)

        // 8. Hero Shield Dome (Translucent 3D Sphere)
        if (frame.playerHasShield) {
            GLES20.glUseProgram(unlitProgram)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, frame.playerX, frame.playerY + 0.95f, frame.playerZ)
            Matrix.scaleM(modelMatrix, 0, 1.08f, 1.18f, 1.08f)
            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
            GLES20.glUniformMatrix4fv(unlitMvpLoc, 1, false, mvpMatrix, 0)
            val shieldAlpha = if (frame.playerInvuln) 0.16f else 0.24f
            GLES20.glUniform4f(unlitColorLoc, frame.deckTrim[0], frame.deckTrim[1], frame.deckTrim[2], shieldAlpha)
            sphereMesh?.draw(unlitPosAttr)
        }

        // 9. Luminous Bullets & Soft Glow Particles (Additive Camera-Facing Billboards)
        GLES20.glUseProgram(billboardProgram)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glUniform1i(bbTextureLoc, 0)
        GLES20.glUniform4fv(bbFogColorLoc, 1, frame.skyBottom, 0)

        for (i in 0 until frame.bulletCount) {
            val base = i * 7
            val bx = frame.bulletData[base]
            val by = frame.bulletData[base + 1]
            val bz = frame.bulletData[base + 2]
            val r = frame.bulletData[base + 3]
            val g = frame.bulletData[base + 4]
            val b = frame.bulletData[base + 5]
            val rad = frame.bulletData[base + 6]

            drawCameraFacingBillboard(
                texture = texGlowOrb,
                worldX = bx, worldY = by, worldZ = bz,
                baseHeight = rad * 4.0f,
                tintR = r, tintG = g, tintB = b,
                tintMix = 1f,
                opacity = 0.72f,
                softCutout = false,
                additiveBlend = true,
                fogDensity = fogDensity
            )
            drawCameraFacingBillboard(
                texture = texGlowOrb,
                worldX = bx, worldY = by, worldZ = bz,
                baseHeight = rad * 1.80f,
                depthBiasTowardCamera = 0.04f,
                tintR = 1f, tintG = 0.98f, tintB = 0.92f,
                tintMix = 1f,
                opacity = 0.95f,
                softCutout = false,
                additiveBlend = true,
                fogDensity = fogDensity
            )
        }

        for (i in 0 until frame.particleCount) {
            val base = i * 7
            val px = frame.particleData[base]
            val py = frame.particleData[base + 1]
            val pz = frame.particleData[base + 2]
            val r = frame.particleData[base + 3]
            val g = frame.particleData[base + 4]
            val b = frame.particleData[base + 5]
            val sz = frame.particleData[base + 6]

            drawCameraFacingBillboard(
                texture = texGlowOrb,
                worldX = px, worldY = py, worldZ = pz,
                baseHeight = sz * 2.3f,
                tintR = r, tintG = g, tintB = b,
                tintMix = 1f,
                opacity = 0.80f,
                softCutout = false,
                additiveBlend = true,
                fogDensity = fogDensity
            )
        }

        GLES20.glDepthMask(true)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    private fun resetVisualStateInternal() {
        lastFrameNano = 0L
        smoothBossAlpha = if (activeSimulation.combatState == Boss3DCombatState.BRIEFING) 0.72f else 1f
        smoothBossIntroProgress = if (activeSimulation.combatState == Boss3DCombatState.BRIEFING) 0.85f else 1f
        walkCyclePhase = 0f
        walkBlend = 0f
        heroFacingSign = 1f
        spriteCount = 0
    }

    private fun hasValidGpuTextures(): Boolean {
        return texWhite != 0 &&
            texPlanet != 0 &&
            texDeck != 0 &&
            texBoss.texId != 0 &&
            texHeroIdle.texId != 0 &&
            texGlowOrb.texId != 0 &&
            GLES20.glIsTexture(texWhite) &&
            GLES20.glIsTexture(texDeck) &&
            GLES20.glIsTexture(texBoss.texId) &&
            GLES20.glIsTexture(texHeroIdle.texId)
    }

    private fun updateVisualTransitions(dt: Float) {
        val bossAlive = frame.combatState != Boss3DCombatState.VICTORY_FINALE && frame.bossHpRatio > 0f
        val targetAlpha = when {
            !bossAlive -> 0f
            frame.combatState == Boss3DCombatState.BRIEFING -> 0.58f
            else -> 1f
        }
        val fadeSpeed = if (targetAlpha > smoothBossAlpha) 3.2f else 2.2f
        smoothBossAlpha = approach(smoothBossAlpha, targetAlpha, dt * fadeSpeed)

        val targetScale = if (frame.combatState == Boss3DCombatState.BRIEFING) 0.85f else 1f
        smoothBossIntroProgress = approach(smoothBossIntroProgress, targetScale, dt * 2.8f)

        // Natural human gait & directional facing update
        val grounded = frame.playerY <= 0.08f
        val speed = if (grounded) frame.playerSpeed else 0f
        val targetWalkBlend = if (grounded && speed > 0.35f) 1f else 0f
        walkBlend = approach(walkBlend, targetWalkBlend, dt * 9.5f)
        if (speed > 0.25f) {
            walkCyclePhase += speed * 2.15f * dt
        }

        // Smooth directional facing with deadzone hysteresis (prevents flickering)
        if (abs(frame.playerMoveRightDot) > 0.65f) {
            heroFacingSign = if (frame.playerMoveRightDot >= 0f) 1f else -1f
        } else if (abs(frame.playerAimRightDot) > 0.45f) {
            heroFacingSign = if (frame.playerAimRightDot >= 0f) 1f else -1f
        }
    }

    private fun approach(current: Float, target: Float, maxDelta: Float): Float {
        return when {
            current < target -> (current + maxDelta).coerceAtMost(target)
            current > target -> (current - maxDelta).coerceAtLeast(target)
            else -> target
        }
    }

    private fun drawBoss3DFrame() {
        val bx = frame.bossX
        val by = frame.bossY
        val bz = frame.bossZ
        val s = frame.bossScale * smoothBossIntroProgress
        val yawDeg = Math.toDegrees(frame.bossYaw.toDouble()).toFloat()

        // Grand Finale Stage 50 Imperial Crown Spires
        if (frame.isGrandFinale) {
            for (k in 0 until 3) {
                val offsetX = (k - 1) * (s * 0.65f)
                drawLitMesh(
                    mesh = cylinderMesh,
                    tx = bx + offsetX * cos(frame.bossYaw),
                    ty = by + s * 0.92f,
                    tz = bz - offsetX * sin(frame.bossYaw),
                    rx = 0f, ry = yawDeg, rz = 0f,
                    sx = s * 0.16f, sy = s * (if (k == 1) 1.0f else 0.72f), sz = s * 0.16f,
                    color = frame.coreColor,
                    emissive = 0.65f * smoothBossAlpha,
                    rim = frame.hazardColor,
                    textureId = texWhite,
                    texMix = 0f
                )
            }
        }

        // Rotating Orbital Shield Rings
        if (frame.hasOrbitalRings) {
            GLES20.glDisable(GLES20.GL_CULL_FACE)
            drawLitMesh(
                mesh = ringMesh,
                tx = bx, ty = by, tz = bz,
                rx = 24f + sin(frame.timeSec * 1.8f) * 14f,
                ry = frame.timeSec * (45f + frame.bossPhase * 22f),
                rz = 15f,
                sx = s * 1.72f, sy = 1f, sz = s * 1.72f,
                color = frame.coreColor,
                emissive = 0.58f * smoothBossAlpha,
                rim = frame.coreColor,
                textureId = texWhite,
                texMix = 0f
            )
            if (frame.isGrandFinale) {
                drawLitMesh(
                    mesh = ringMesh,
                    tx = bx, ty = by, tz = bz,
                    rx = -32f,
                    ry = -frame.timeSec * 65f,
                    rz = -18f,
                    sx = s * 2.02f, sy = 1f, sz = s * 2.02f,
                    color = frame.hazardColor,
                    emissive = 0.58f * smoothBossAlpha,
                    rim = frame.hazardColor,
                    textureId = texWhite,
                    texMix = 0f
                )
            }
            GLES20.glEnable(GLES20.GL_CULL_FACE)
        }
    }

    /**
     * Collects all 2D game sprites (Boss, Boss Reticle, Satellites, Pickups, Animated Hero, Drone),
     * normalizes their visual area proportionally across aspect ratios, sorts back-to-front,
     * and renders every sprite parallel to the camera screen plane via Boss3DBillboardMath.
     */
    private fun renderSortedCutoutBillboards(fogDensity: Float) {
        spriteCount = 0

        // 1. Boss Main 2D Sprite + Floating Tactical Reticle (area-normalized by sqrt(aspect))
        if (smoothBossAlpha > 0.02f) {
            val hoverBob = sin(frame.timeSec * 2.2f) * 0.14f
            val targetSize = frame.bossScale * 2.15f * smoothBossIntroProgress
            val bossHeight = targetSize / sqrt(texBoss.aspectRatio.coerceIn(0.60f, 1.85f))
            val deathExpand = if (frame.bossHpRatio <= 0f || frame.combatState == Boss3DCombatState.VICTORY_FINALE) {
                1f + (1f - smoothBossAlpha) * 0.45f
            } else {
                1f
            }
            enqueueSprite(
                tex = texBoss,
                x = frame.bossX,
                y = frame.bossY + 0.18f + hoverBob,
                z = frame.bossZ,
                height = bossHeight * deathExpand,
                widthScale = 1f,
                rollRad = 0f,
                depthBias = 0.10f,
                flipX = false,
                tintR = 0.76f + frame.coreColor[0] * 0.24f,
                tintG = 0.76f + frame.coreColor[1] * 0.24f,
                tintB = 0.76f + frame.coreColor[2] * 0.24f,
                tintMix = 0.18f,
                flash = frame.bossHitFlash,
                opacity = smoothBossAlpha
            )

            // Crisp camera-facing Target Reticle over Boss
            enqueueSprite(
                tex = texReticle,
                x = frame.bossX,
                y = frame.bossY + 0.18f + hoverBob,
                z = frame.bossZ,
                height = targetSize * 0.56f,
                widthScale = 1f,
                rollRad = frame.timeSec * 0.55f,
                depthBias = 0.34f,
                flipX = false,
                tintR = frame.hazardColor[0],
                tintG = frame.hazardColor[1],
                tintB = frame.hazardColor[2],
                tintMix = 0.85f,
                flash = frame.bossHitFlash * 0.5f,
                opacity = 0.38f * smoothBossAlpha
            )
        }

        // 2. Orbiting Attack Satellites (proportional ~0.85m drone sprites)
        val satHeight = 0.85f / sqrt(texSatellite.aspectRatio.coerceIn(0.65f, 1.85f))
        for (i in 0 until frame.satelliteCount) {
            val base = i * 3
            val sx = frame.satelliteXYZ[base]
            val sy = frame.satelliteXYZ[base + 1]
            val sz = frame.satelliteXYZ[base + 2]
            enqueueSprite(
                tex = texSatellite,
                x = sx, y = sy, z = sz,
                height = satHeight,
                widthScale = 1f,
                rollRad = 0f,
                depthBias = 0f,
                flipX = sx < frame.bossX,
                tintR = 0.78f + frame.coreColor[0] * 0.22f,
                tintG = 0.78f + frame.coreColor[1] * 0.22f,
                tintB = 0.78f + frame.coreColor[2] * 0.22f,
                tintMix = 0.18f,
                flash = 0f,
                opacity = smoothBossAlpha.coerceAtLeast(0.01f)
            )
        }

        // 3. Ammo & Medkit Pickups (proportional ~0.72m supply packs)
        for (i in 0 until frame.pickupCount) {
            val base = i * 3
            val px = frame.pickupXZType[base]
            val pz = frame.pickupXZType[base + 1]
            val isHealth = frame.pickupXZType[base + 2] > 0.5f
            val tex = if (isHealth) texHealth else texAmmo
            val pkHeight = 0.72f / sqrt(tex.aspectRatio.coerceIn(0.60f, 1.95f))
            val bobY = 0.42f + pkHeight * 0.5f + sin(frame.timeSec * 3.5f + i) * 0.15f
            enqueueSprite(
                tex = tex,
                x = px, y = bobY, z = pz,
                height = pkHeight,
                widthScale = 1f,
                rollRad = 0f,
                depthBias = 0f,
                flipX = false,
                tintR = 1f, tintG = 1f, tintB = 1f,
                tintMix = 0f,
                flash = 0f,
                opacity = 1f
            )
        }

        // 4. Full-Body Animated Hero Astronaut (bottom-foot anchored, natural walk gait & jetpack lean)
        val heroOpacity = if (frame.playerInvuln) 0.44f else 1f
        val heroDashFlash = if (frame.playerDashing) 0.35f else 0f
        val airborne = frame.playerY > 0.14f || frame.playerJetpacking
        val gaitSin = sin(walkCyclePhase)
        val gaitCos = cos(walkCyclePhase * 2f)

        val chosenHeroTex = when {
            airborne -> texHeroJump
            walkBlend > 0.15f && abs(gaitSin) >= 0.28f -> texHeroWalk
            else -> texHeroIdle
        }

        // Natural stride vertical lift (2 peaks per full step cycle), subtle body lean, and landing cushion
        val strideBobY = if (airborne) 0f else abs(gaitSin) * 0.11f * walkBlend
        val idleBreath = if (airborne || walkBlend > 0.15f) 0f else sin(frame.timeSec * 2.6f) * 0.015f
        val landSquash = frame.playerLandImpulse * 0.09f
        val squashY = 1f + gaitCos * 0.024f * walkBlend + idleBreath - landSquash
        val squashX = 1f - gaitCos * 0.014f * walkBlend + landSquash * 0.75f

        val heroRoll = if (airborne) {
            -(frame.playerMoveRightDot / 10f).coerceIn(-0.09f, 0.09f)
        } else {
            (gaitSin * 0.042f * walkBlend) - (frame.playerMoveRightDot / 24f).coerceIn(-0.04f, 0.04f) * walkBlend
        }

        val heroBaseHeight = 1.85f
        val actualHeroHeight = heroBaseHeight * squashY
        val heroCenterY = frame.playerY + strideBobY + actualHeroHeight * 0.5f

        enqueueSprite(
            tex = chosenHeroTex,
            x = frame.playerX,
            y = heroCenterY,
            z = frame.playerZ,
            height = actualHeroHeight,
            widthScale = squashX / squashY,
            rollRad = heroRoll,
            depthBias = 0f,
            flipX = heroFacingSign < 0f,
            tintR = 0.88f + frame.heroAccent[0] * 0.12f,
            tintG = 0.88f + frame.heroAccent[1] * 0.12f,
            tintB = 0.88f + frame.heroAccent[2] * 0.12f,
            tintMix = 0.10f,
            flash = heroDashFlash,
            opacity = heroOpacity
        )

        // 5. Support Drone (~0.58m mini-companion)
        if (frame.droneActive) {
            val droneHeight = 0.58f / sqrt(texDrone.aspectRatio.coerceIn(0.65f, 1.85f))
            enqueueSprite(
                tex = texDrone,
                x = frame.droneX, y = frame.droneY, z = frame.droneZ,
                height = droneHeight,
                widthScale = 1f,
                rollRad = sin(frame.timeSec * 3.5f) * 0.06f,
                depthBias = 0f,
                flipX = frame.droneX > frame.bossX,
                tintR = 0.82f + frame.deckTrim[0] * 0.18f,
                tintG = 0.82f + frame.deckTrim[1] * 0.18f,
                tintB = 0.82f + frame.deckTrim[2] * 0.18f,
                tintMix = 0.16f,
                flash = 0f,
                opacity = 1f
            )
        }

        // Insertion sort back-to-front by distSq (zero heap allocation)
        for (i in 0 until spriteCount) {
            spriteOrder[i] = i
        }
        for (i in 1 until spriteCount) {
            val keyIdx = spriteOrder[i]
            val keyDist = spritePool[keyIdx].distSq
            var j = i - 1
            while (j >= 0 && spritePool[spriteOrder[j]].distSq < keyDist) {
                spriteOrder[j + 1] = spriteOrder[j]
                j--
            }
            spriteOrder[j + 1] = keyIdx
        }

        GLES20.glUseProgram(billboardProgram)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glUniform1i(bbTextureLoc, 0)
        GLES20.glUniform4fv(bbFogColorLoc, 1, frame.skyBottom, 0)

        for (i in 0 until spriteCount) {
            val s = spritePool[spriteOrder[i]]
            drawCameraFacingBillboard(
                texture = s.tex,
                worldX = s.x,
                worldY = s.y,
                worldZ = s.z,
                baseHeight = s.height,
                widthScale = s.widthScale,
                rollRad = s.rollRad,
                depthBiasTowardCamera = s.depthBias,
                flipX = s.flipX,
                tintR = s.tintR,
                tintG = s.tintG,
                tintB = s.tintB,
                tintMix = s.tintMix,
                flash = s.flash,
                opacity = s.opacity,
                softCutout = true,
                additiveBlend = false,
                fogDensity = fogDensity
            )
        }
    }

    private fun enqueueSprite(
        tex: Boss3DGlTexture,
        x: Float, y: Float, z: Float,
        height: Float,
        widthScale: Float,
        rollRad: Float,
        depthBias: Float,
        flipX: Boolean,
        tintR: Float, tintG: Float, tintB: Float,
        tintMix: Float,
        flash: Float,
        opacity: Float
    ) {
        if (tex.texId == 0 || opacity <= 0.01f || spriteCount >= MAX_SPRITE_SLOTS) return
        val slot = spritePool[spriteCount++]
        slot.tex = tex
        slot.x = x; slot.y = y; slot.z = z
        slot.height = height
        slot.widthScale = widthScale
        slot.rollRad = rollRad
        slot.depthBias = depthBias
        slot.flipX = flipX
        slot.tintR = tintR; slot.tintG = tintG; slot.tintB = tintB
        slot.tintMix = tintMix
        slot.flash = flash
        slot.opacity = opacity
        val dx = x - camX
        val dy = y - camY
        val dz = z - camZ
        slot.distSq = dx * dx + dy * dy + dz * dz
    }

    /**
     * Dedicated Exact Camera-Facing Billboard Renderer:
     * Uses Boss3DBillboardMath.buildCameraFacingMatrix to orient the Quad parallel
     * to the screen plane with zero perspective tilt/skew and exact aspect ratio W:H.
     */
    private fun drawCameraFacingBillboard(
        texture: Boss3DGlTexture,
        worldX: Float,
        worldY: Float,
        worldZ: Float,
        baseHeight: Float,
        widthScale: Float = 1f,
        rollRad: Float = 0f,
        depthBiasTowardCamera: Float = 0f,
        flipX: Boolean = false,
        tintR: Float = 1f,
        tintG: Float = 1f,
        tintB: Float = 1f,
        tintMix: Float = 0.18f,
        flash: Float = 0f,
        opacity: Float = 1f,
        softCutout: Boolean = true,
        additiveBlend: Boolean = false,
        fogDensity: Float = 0.006f
    ) {
        val quad = billboardQuadMesh ?: return
        if (texture.texId == 0 || opacity <= 0.005f) return

        val width = baseHeight * texture.aspectRatio * widthScale
        Boss3DBillboardMath.buildCameraFacingMatrix(
            viewMatrix = viewMatrix,
            worldX = worldX,
            worldY = worldY,
            worldZ = worldZ,
            width = width,
            height = baseHeight,
            rollRad = rollRad,
            depthBiasTowardCamera = depthBiasTowardCamera,
            outModelMatrix = modelMatrix
        )
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        val dx = worldX - camX
        val dy = worldY - camY
        val dz = worldZ - camZ
        val dist = sqrt(dx * dx + dy * dy + dz * dz)
        val fogFactor = if (fogDensity > 0f) (1f - exp(-dist * fogDensity)).coerceIn(0f, 0.55f) else 0f

        if (additiveBlend) {
            GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE)
        } else {
            GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture.texId)
        GLES20.glUniformMatrix4fv(bbMvpLoc, 1, false, mvpMatrix, 0)
        GLES20.glUniform1f(bbFlipXLoc, if (flipX) 1f else 0f)
        GLES20.glUniform4f(bbTintLoc, tintR, tintG, tintB, tintMix)
        GLES20.glUniform1f(bbFlashLoc, flash)
        GLES20.glUniform1f(bbOpacityLoc, opacity)
        GLES20.glUniform1f(bbSoftCutLoc, if (softCutout) 1f else 0f)
        GLES20.glUniform1f(bbFogFactorLoc, fogFactor)

        quad.draw(bbPosAttr, -1, bbUvAttr)
    }

    private fun drawGroundDisc(
        x: Float,
        y: Float,
        z: Float,
        radius: Float,
        r: Float,
        g: Float,
        b: Float,
        opacity: Float
    ) {
        val quad = billboardQuadMesh ?: return
        Boss3DBillboardMath.buildGroundDiscMatrix(x, y, z, radius, radius, modelMatrix)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(bbMvpLoc, 1, false, mvpMatrix, 0)
        GLES20.glUniform4f(bbTintLoc, r, g, b, 1f)
        GLES20.glUniform1f(bbOpacityLoc, opacity)
        GLES20.glUniform1f(bbFogFactorLoc, 0f)
        quad.draw(bbPosAttr, -1, bbUvAttr)
    }

    private fun drawLitMesh(
        mesh: GpuIndexedMesh?,
        tx: Float, ty: Float, tz: Float,
        rx: Float, ry: Float, rz: Float,
        sx: Float, sy: Float, sz: Float,
        color: FloatArray,
        emissive: Float,
        rim: FloatArray,
        textureId: Int,
        texMix: Float
    ) {
        if (mesh == null) return
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, tx, ty, tz)
        if (ry != 0f) Matrix.rotateM(modelMatrix, 0, ry, 0f, 1f, 0f)
        if (rx != 0f) Matrix.rotateM(modelMatrix, 0, rx, 1f, 0f, 0f)
        if (rz != 0f) Matrix.rotateM(modelMatrix, 0, rz, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        val effectiveTex = if (textureId != 0) textureId else texWhite
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, effectiveTex)
        GLES20.glUniform1f(litTexMixLoc, if (textureId != 0) texMix else 0f)
        GLES20.glUniformMatrix4fv(litMvpLoc, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(litModelLoc, 1, false, modelMatrix, 0)
        GLES20.glUniform4fv(litColorLoc, 1, color, 0)
        GLES20.glUniform1f(litEmissiveLoc, emissive)
        GLES20.glUniform4fv(litRimColorLoc, 1, rim, 0)
        mesh.draw(litPosAttr, litNormalAttr, litUvAttr)
    }

    private fun loadTextures(sim: Boss3DSimulation) {
        releaseTextures()
        val stageNum = sim.stageDef.stageNumber
        val chapterIdx = sim.stageDef.chapterIndex
        val spec = sim.spec
        loadedStageNumber = stageNum
        loadedHero = sim.hero

        texWhite = Boss3DGlTextureFactory.createSolidWhiteGlTexture()
        texPlanet = Boss3DGlTextureFactory.createProceduralEquirectangularPlanetTexture(chapterIdx, spec)
        texDeck = Boss3DGlTextureFactory.createProceduralSciFiDeckTexture(spec)

        // Transparent cutout camera-facing billboard textures (zero background & halo-free)
        texBoss = Boss3DGlTextureFactory.loadBillboardSpriteTexture(
            context = appContext,
            primaryAsset = Boss3DAssetMapper.bossHullTextureAsset(stageNum),
            secondaryAsset = Boss3DAssetMapper.bossHullSecondaryAsset(stageNum),
            drawableRes = Boss3DAssetMapper.bossPortraitDrawableFallback(stageNum),
            potSize = 512,
            fallbackArgb = floatRgbToArgb(spec.coreGlowColor)
        )
        texSatellite = Boss3DGlTextureFactory.loadBillboardSpriteTexture(
            context = appContext,
            primaryAsset = Boss3DAssetMapper.bossSatelliteTextureAsset(stageNum),
            secondaryAsset = "images/alien_ship.png",
            potSize = 256,
            fallbackArgb = floatRgbToArgb(spec.hazardColor)
        )
        texHeroIdle = Boss3DGlTextureFactory.loadBillboardSpriteTexture(
            context = appContext,
            primaryAsset = Boss3DAssetMapper.HERO_IDLE_ASSET,
            secondaryAsset = Boss3DAssetMapper.HERO_PRIMARY,
            drawableRes = Boss3DAssetMapper.HERO_DRAWABLE_FALLBACK,
            potSize = 512,
            fallbackArgb = sim.hero.accentColor.toInt()
        )
        texHeroWalk = Boss3DGlTextureFactory.loadBillboardSpriteTexture(
            context = appContext,
            primaryAsset = Boss3DAssetMapper.HERO_WALK_ASSET,
            secondaryAsset = Boss3DAssetMapper.HERO_IDLE_ASSET,
            drawableRes = Boss3DAssetMapper.HERO_DRAWABLE_FALLBACK,
            potSize = 512,
            fallbackArgb = sim.hero.accentColor.toInt()
        )
        texHeroJump = Boss3DGlTextureFactory.loadBillboardSpriteTexture(
            context = appContext,
            primaryAsset = Boss3DAssetMapper.HERO_JUMP_ASSET,
            secondaryAsset = Boss3DAssetMapper.HERO_IDLE_ASSET,
            drawableRes = Boss3DAssetMapper.HERO_DRAWABLE_FALLBACK,
            potSize = 512,
            fallbackArgb = sim.hero.accentColor.toInt()
        )
        texDrone = Boss3DGlTextureFactory.loadBillboardSpriteTexture(
            context = appContext,
            primaryAsset = Boss3DAssetMapper.DRONE_PRIMARY,
            secondaryAsset = Boss3DAssetMapper.DRONE_SECONDARY,
            potSize = 256,
            fallbackArgb = floatRgbToArgb(spec.deckTrimColor)
        )
        texAmmo = Boss3DGlTextureFactory.loadBillboardSpriteTexture(
            context = appContext,
            primaryAsset = Boss3DAssetMapper.AMMO_PRIMARY,
            potSize = 256,
            fallbackArgb = 0xFF26D9FF.toInt()
        )
        texHealth = Boss3DGlTextureFactory.loadBillboardSpriteTexture(
            context = appContext,
            primaryAsset = Boss3DAssetMapper.HEALTH_PRIMARY,
            potSize = 256,
            fallbackArgb = 0xFF33F273.toInt()
        )
        texGlowOrb = Boss3DGlTextureFactory.createProceduralGlowOrbTexture(128)
        texReticle = Boss3DGlTextureFactory.createProceduralReticleTexture(256)
    }

    private fun releaseTextures() {
        val ids = intArrayOf(
            texWhite, texPlanet, texDeck,
            texBoss.texId, texSatellite.texId,
            texHeroIdle.texId, texHeroWalk.texId, texHeroJump.texId,
            texDrone.texId, texAmmo.texId, texHealth.texId,
            texGlowOrb.texId, texReticle.texId
        )
        for (id in ids) {
            if (id != 0) {
                GLES20.glDeleteTextures(1, intArrayOf(id), 0)
            }
        }
        texWhite = 0
        texPlanet = 0
        texDeck = 0
        texBoss = Boss3DGlTexture(0, 1f)
        texSatellite = Boss3DGlTexture(0, 1f)
        texHeroIdle = Boss3DGlTexture(0, 1f)
        texHeroWalk = Boss3DGlTexture(0, 1f)
        texHeroJump = Boss3DGlTexture(0, 1f)
        texDrone = Boss3DGlTexture(0, 1f)
        texAmmo = Boss3DGlTexture(0, 1f)
        texHealth = Boss3DGlTexture(0, 1f)
        texGlowOrb = Boss3DGlTexture(0, 1f)
        texReticle = Boss3DGlTexture(0, 1f)
        loadedStageNumber = -1
        loadedHero = null
    }

    private fun floatRgbToArgb(rgb: FloatArray): Int {
        val r = (rgb.getOrElse(0) { 0.5f }.coerceIn(0f, 1f) * 255f).toInt()
        val g = (rgb.getOrElse(1) { 0.5f }.coerceIn(0f, 1f) * 255f).toInt()
        val b = (rgb.getOrElse(2) { 0.5f }.coerceIn(0f, 1f) * 255f).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun createMeshes() {
        releaseMeshes()
        billboardQuadMesh = Boss3DMeshFactory.createBillboardQuadMesh()
        sphereMesh = Boss3DMeshFactory.createUnitSphere(20, 28)
        cylinderMesh = Boss3DMeshFactory.createUnitCylinder(48)
        boxMesh = Boss3DMeshFactory.createUnitBox()
        ringMesh = Boss3DMeshFactory.createFlatRing(0.84f, 1.0f, 64)
        starfieldMesh = Boss3DMeshFactory.createStarfield(340)
        deckGridMesh = Boss3DMeshFactory.createDeckGrid(4, 16)
    }

    private fun releaseMeshes() {
        billboardQuadMesh?.release(); billboardQuadMesh = null
        sphereMesh?.release(); sphereMesh = null
        cylinderMesh?.release(); cylinderMesh = null
        boxMesh?.release(); boxMesh = null
        ringMesh?.release(); ringMesh = null
        starfieldMesh?.release(); starfieldMesh = null
        deckGridMesh?.release(); deckGridMesh = null
    }

    private fun compileShaders() {
        releaseShaders()
        litProgram = buildProgram(LIT_VERT, LIT_FRAG)
        if (litProgram != 0) {
            litPosAttr = GLES20.glGetAttribLocation(litProgram, "aPos")
            litNormalAttr = GLES20.glGetAttribLocation(litProgram, "aNormal")
            litUvAttr = GLES20.glGetAttribLocation(litProgram, "aUv")
            litMvpLoc = GLES20.glGetUniformLocation(litProgram, "uMvp")
            litModelLoc = GLES20.glGetUniformLocation(litProgram, "uModel")
            litColorLoc = GLES20.glGetUniformLocation(litProgram, "uColor")
            litEmissiveLoc = GLES20.glGetUniformLocation(litProgram, "uEmissive")
            litLightDirLoc = GLES20.glGetUniformLocation(litProgram, "uLightDir")
            litCamPosLoc = GLES20.glGetUniformLocation(litProgram, "uCamPos")
            litRimColorLoc = GLES20.glGetUniformLocation(litProgram, "uRimColor")
            litFogColorLoc = GLES20.glGetUniformLocation(litProgram, "uFogColor")
            litFogDensityLoc = GLES20.glGetUniformLocation(litProgram, "uFogDensity")
            litBossLightPosLoc = GLES20.glGetUniformLocation(litProgram, "uBossLightPos")
            litBossLightColorLoc = GLES20.glGetUniformLocation(litProgram, "uBossLightColor")
            litTextureLoc = GLES20.glGetUniformLocation(litProgram, "uTexture")
            litTexMixLoc = GLES20.glGetUniformLocation(litProgram, "uTexMix")
        }

        billboardProgram = buildProgram(BILLBOARD_VERT, BILLBOARD_FRAG)
        if (billboardProgram != 0) {
            bbPosAttr = GLES20.glGetAttribLocation(billboardProgram, "aPos")
            bbUvAttr = GLES20.glGetAttribLocation(billboardProgram, "aUv")
            bbMvpLoc = GLES20.glGetUniformLocation(billboardProgram, "uMvp")
            bbFlipXLoc = GLES20.glGetUniformLocation(billboardProgram, "uFlipX")
            bbTextureLoc = GLES20.glGetUniformLocation(billboardProgram, "uTexture")
            bbTintLoc = GLES20.glGetUniformLocation(billboardProgram, "uTint")
            bbFlashLoc = GLES20.glGetUniformLocation(billboardProgram, "uFlash")
            bbOpacityLoc = GLES20.glGetUniformLocation(billboardProgram, "uOpacity")
            bbSoftCutLoc = GLES20.glGetUniformLocation(billboardProgram, "uSoftCut")
            bbFogColorLoc = GLES20.glGetUniformLocation(billboardProgram, "uFogColor")
            bbFogFactorLoc = GLES20.glGetUniformLocation(billboardProgram, "uFogFactor")
        }

        unlitProgram = buildProgram(UNLIT_VERT, UNLIT_FRAG)
        if (unlitProgram != 0) {
            unlitPosAttr = GLES20.glGetAttribLocation(unlitProgram, "aPos")
            unlitMvpLoc = GLES20.glGetUniformLocation(unlitProgram, "uMvp")
            unlitColorLoc = GLES20.glGetUniformLocation(unlitProgram, "uColor")
            unlitPointSizeLoc = GLES20.glGetUniformLocation(unlitProgram, "uPointSize")
        }

        pointProgram = buildProgram(POINT_VERT, POINT_FRAG)
        if (pointProgram != 0) {
            pointPosAttr = GLES20.glGetAttribLocation(pointProgram, "aPos")
            pointMvpLoc = GLES20.glGetUniformLocation(pointProgram, "uMvp")
            pointColorLoc = GLES20.glGetUniformLocation(pointProgram, "uColor")
            pointSizeLoc = GLES20.glGetUniformLocation(pointProgram, "uPointSize")
        }
    }

    private fun releaseShaders() {
        if (litProgram != 0) {
            GLES20.glDeleteProgram(litProgram)
            litProgram = 0
        }
        if (billboardProgram != 0) {
            GLES20.glDeleteProgram(billboardProgram)
            billboardProgram = 0
        }
        if (unlitProgram != 0) {
            GLES20.glDeleteProgram(unlitProgram)
            unlitProgram = 0
        }
        if (pointProgram != 0) {
            GLES20.glDeleteProgram(pointProgram)
            pointProgram = 0
        }
    }

    fun releaseGlResources() {
        releaseTextures()
        releaseMeshes()
        releaseShaders()
    }

    private fun buildProgram(vertSrc: String, fragSrc: String): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertSrc)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragSrc)
        if (vs == 0 || fs == 0) {
            if (vs != 0) GLES20.glDeleteShader(vs)
            if (fs != 0) GLES20.glDeleteShader(fs)
            return 0
        }
        val prog = GLES20.glCreateProgram()
        if (prog == 0) return 0
        GLES20.glAttachShader(prog, vs)
        GLES20.glAttachShader(prog, fs)
        GLES20.glLinkProgram(prog)
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
        val status = IntArray(1)
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            GLES20.glDeleteProgram(prog)
            return 0
        }
        return prog
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) return 0
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    companion object {
        private const val MAX_SPRITE_SLOTS = 24

        private const val LIT_VERT = """
            uniform mat4 uMvp;
            uniform mat4 uModel;
            attribute vec3 aPos;
            attribute vec3 aNormal;
            attribute vec2 aUv;
            varying vec3 vWorldPos;
            varying vec3 vNormal;
            varying vec2 vUv;
            void main() {
                vec4 worldPos = uModel * vec4(aPos, 1.0);
                vWorldPos = worldPos.xyz;
                vNormal = normalize(mat3(uModel) * aNormal);
                vUv = aUv;
                gl_Position = uMvp * vec4(aPos, 1.0);
            }
        """

        private const val LIT_FRAG = """
            precision mediump float;
            uniform vec4 uColor;
            uniform float uEmissive;
            uniform vec3 uLightDir;
            uniform vec3 uCamPos;
            uniform vec4 uRimColor;
            uniform vec4 uFogColor;
            uniform float uFogDensity;
            uniform vec3 uBossLightPos;
            uniform vec3 uBossLightColor;
            uniform sampler2D uTexture;
            uniform float uTexMix;
            varying vec3 vWorldPos;
            varying vec3 vNormal;
            varying vec2 vUv;
            void main() {
                vec3 N = normalize(vNormal);
                vec3 L = normalize(uLightDir);
                vec3 V = normalize(uCamPos - vWorldPos);
                vec4 texSample = texture2D(uTexture, vUv);
                float blendAmt = clamp(texSample.a * uTexMix, 0.0, 1.0);
                vec3 tintedTex = texSample.rgb * mix(vec3(1.0), uColor.rgb, 0.25);
                vec3 albedo = mix(uColor.rgb, tintedTex, blendAmt);
                float diff = max(dot(N, L), 0.0);
                vec3 H = normalize(L + V);
                float spec = pow(max(dot(N, H), 0.0001), 28.0) * 0.42;
                float fresnel = smoothstep(0.08, 0.92, 1.0 - max(dot(N, V), 0.0));
                float rim = pow(max(fresnel, 0.0001), 2.1) * 0.56;
                vec3 toBoss = uBossLightPos - vWorldPos;
                float bossDist = length(toBoss);
                vec3 bossL = toBoss / max(bossDist, 0.001);
                float bossAtten = 1.0 / (1.0 + bossDist * bossDist * 0.032);
                float bossDiff = max(dot(N, bossL), 0.0) * bossAtten;
                vec3 baseLit = albedo * (0.36 + diff * 0.60 + uEmissive)
                             + albedo * uBossLightColor * bossDiff * 0.62
                             + vec3(spec)
                             + uRimColor.rgb * rim;
                float dist = length(uCamPos - vWorldPos);
                float fog = clamp(1.0 - exp(-dist * uFogDensity), 0.0, 0.72);
                vec3 finalRgb = mix(baseLit, uFogColor.rgb, fog);
                gl_FragColor = vec4(clamp(finalRgb, 0.0, 1.0), uColor.a);
            }
        """

        private const val BILLBOARD_VERT = """
            uniform mat4 uMvp;
            uniform float uFlipX;
            attribute vec3 aPos;
            attribute vec2 aUv;
            varying vec2 vUv;
            void main() {
                float ux = mix(aUv.x, 1.0 - aUv.x, step(0.5, uFlipX));
                vUv = vec2(ux, aUv.y);
                gl_Position = uMvp * vec4(aPos, 1.0);
            }
        """

        private const val BILLBOARD_FRAG = """
            precision mediump float;
            uniform sampler2D uTexture;
            uniform vec4 uTint;
            uniform float uFlash;
            uniform float uOpacity;
            uniform float uSoftCut;
            uniform vec4 uFogColor;
            uniform float uFogFactor;
            varying vec2 vUv;
            void main() {
                vec4 tex = texture2D(uTexture, vUv);
                float cutMask = mix(1.0, smoothstep(0.02, 0.14, tex.a), uSoftCut);
                float alpha = tex.a * cutMask * uOpacity;
                if (alpha <= 0.006) discard;
                vec3 tinted = mix(tex.rgb, tex.rgb * uTint.rgb, uTint.a);
                vec3 flashed = mix(tinted, vec3(1.0, 0.96, 0.88), clamp(uFlash, 0.0, 0.85));
                vec3 finalRgb = mix(flashed, uFogColor.rgb, uFogFactor);
                gl_FragColor = vec4(finalRgb * alpha, alpha);
            }
        """

        private const val UNLIT_VERT = """
            uniform mat4 uMvp;
            uniform float uPointSize;
            attribute vec3 aPos;
            void main() {
                gl_PointSize = uPointSize;
                gl_Position = uMvp * vec4(aPos, 1.0);
            }
        """

        private const val UNLIT_FRAG = """
            precision mediump float;
            uniform vec4 uColor;
            void main() {
                gl_FragColor = uColor;
            }
        """

        private const val POINT_VERT = """
            uniform mat4 uMvp;
            uniform float uPointSize;
            attribute vec3 aPos;
            void main() {
                gl_PointSize = uPointSize;
                gl_Position = uMvp * vec4(aPos, 1.0);
            }
        """

        private const val POINT_FRAG = """
            precision mediump float;
            uniform vec4 uColor;
            void main() {
                vec2 c = gl_PointCoord - vec2(0.5);
                float d = length(c);
                float alpha = 1.0 - smoothstep(0.14, 0.50, d);
                if (alpha <= 0.01) discard;
                gl_FragColor = vec4(uColor.rgb, uColor.a * alpha);
            }
        """
    }
}
