package com.example.astronautstories.boss3d

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Exact zero-allocation View-Plane Parallel Billboard Matrix builder.
 * Extracts the orthonormal Camera Right (R), Camera Up (U), and Camera Back (F)
 * basis directly from the OpenGL column-major View Matrix so that:
 *
 *   ViewMatrix * ModelMatrix = [ W  0  0  x_view ]
 *                              [ 0  H  0  y_view ]
 *                              [ 0  0  1  z_view ]
 *                              [ 0  0  0    1    ]
 *
 * Every corner of the billboard quad keeps an identical z_view and stays 100% parallel
 * to the screen plane with zero perspective tilt, zero skew, and exact aspect ratio W:H.
 */
internal object Boss3DBillboardMath {

    fun buildCameraFacingMatrix(
        viewMatrix: FloatArray,
        worldX: Float,
        worldY: Float,
        worldZ: Float,
        width: Float,
        height: Float,
        rollRad: Float = 0f,
        depthBiasTowardCamera: Float = 0f,
        outModelMatrix: FloatArray
    ) {
        // Row 0 of column-major viewMatrix = Camera Right vector in world space
        var rx = viewMatrix[0]
        var ry = viewMatrix[4]
        var rz = viewMatrix[8]
        val rLen = sqrt(rx * rx + ry * ry + rz * rz).coerceAtLeast(1e-6f)
        rx /= rLen; ry /= rLen; rz /= rLen

        // Row 1 of column-major viewMatrix = Camera Up vector in world space
        var ux = viewMatrix[1]
        var uy = viewMatrix[5]
        var uz = viewMatrix[9]
        val uLen = sqrt(ux * ux + uy * uy + uz * uz).coerceAtLeast(1e-6f)
        ux /= uLen; uy /= uLen; uz /= uLen

        // Row 2 of column-major viewMatrix = Camera +Z (toward viewer) vector in world space
        var fx = viewMatrix[2]
        var fy = viewMatrix[6]
        var fz = viewMatrix[10]
        val fLen = sqrt(fx * fx + fy * fy + fz * fz).coerceAtLeast(1e-6f)
        fx /= fLen; fy /= fLen; fz /= fLen

        val rFinalX: Float
        val rFinalY: Float
        val rFinalZ: Float
        val uFinalX: Float
        val uFinalY: Float
        val uFinalZ: Float

        if (rollRad != 0f) {
            val c = cos(rollRad)
            val s = sin(rollRad)
            rFinalX = rx * c + ux * s
            rFinalY = ry * c + uy * s
            rFinalZ = rz * c + uz * s
            uFinalX = -rx * s + ux * c
            uFinalY = -ry * s + uy * c
            uFinalZ = -rz * s + uz * c
        } else {
            rFinalX = rx; rFinalY = ry; rFinalZ = rz
            uFinalX = ux; uFinalY = uy; uFinalZ = uz
        }

        // Column 0: width * Right'
        outModelMatrix[0] = rFinalX * width
        outModelMatrix[1] = rFinalY * width
        outModelMatrix[2] = rFinalZ * width
        outModelMatrix[3] = 0f

        // Column 1: height * Up'
        outModelMatrix[4] = uFinalX * height
        outModelMatrix[5] = uFinalY * height
        outModelMatrix[6] = uFinalZ * height
        outModelMatrix[7] = 0f

        // Column 2: Normal pointing strictly toward viewer
        outModelMatrix[8] = fx
        outModelMatrix[9] = fy
        outModelMatrix[10] = fz
        outModelMatrix[11] = 0f

        // Column 3: World position + optional bias along view normal
        outModelMatrix[12] = worldX + fx * depthBiasTowardCamera
        outModelMatrix[13] = worldY + fy * depthBiasTowardCamera
        outModelMatrix[14] = worldZ + fz * depthBiasTowardCamera
        outModelMatrix[15] = 1f
    }

    /** Flat horizontal XZ quad matrix (normal = +Y) for ground contact shadows & decals */
    fun buildGroundDiscMatrix(
        worldX: Float,
        worldY: Float,
        worldZ: Float,
        radiusX: Float,
        radiusZ: Float,
        outModelMatrix: FloatArray
    ) {
        // Map Quad X -> World +X, Quad Y -> World -Z, Quad Z (normal) -> World +Y
        outModelMatrix[0] = radiusX * 2f; outModelMatrix[1] = 0f; outModelMatrix[2] = 0f; outModelMatrix[3] = 0f
        outModelMatrix[4] = 0f; outModelMatrix[5] = 0f; outModelMatrix[6] = -radiusZ * 2f; outModelMatrix[7] = 0f
        outModelMatrix[8] = 0f; outModelMatrix[9] = 1f; outModelMatrix[10] = 0f; outModelMatrix[11] = 0f
        outModelMatrix[12] = worldX; outModelMatrix[13] = worldY; outModelMatrix[14] = worldZ; outModelMatrix[15] = 1f
    }
}

/**
 * Indexed GPU VBO + IBO mesh containing interleaved [x, y, z, nx, ny, nz, u, v] attributes
 * (8 floats = 32 bytes stride) for smooth-shaded, texture-mapped 3D surfaces and billboards.
 * Created once in onSurfaceCreated(), reused across all objects, and freed cleanly on exit.
 */
internal class GpuIndexedMesh(
    vertices: FloatArray,
    indices: ShortArray,
    val drawMode: Int = GLES20.GL_TRIANGLES
) {
    val indexCount: Int = indices.size
    var vboId: Int = 0
        private set
    var iboId: Int = 0
        private set

    init {
        val ids = IntArray(2)
        GLES20.glGenBuffers(2, ids, 0)
        vboId = ids[0]
        iboId = ids[1]

        val vb: FloatBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(vertices)
                position(0)
            }
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertices.size * 4, vb, GLES20.GL_STATIC_DRAW)

        val ib: ShortBuffer = ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply {
                put(indices)
                position(0)
            }
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, iboId)
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indices.size * 2, ib, GLES20.GL_STATIC_DRAW)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    fun draw(posAttr: Int, normalAttr: Int = -1, uvAttr: Int = -1) {
        if (vboId == 0 || iboId == 0 || indexCount <= 0 || posAttr < 0) return
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, iboId)

        GLES20.glEnableVertexAttribArray(posAttr)
        GLES20.glVertexAttribPointer(posAttr, 3, GLES20.GL_FLOAT, false, STRIDE_BYTES, 0)
        if (normalAttr >= 0) {
            GLES20.glEnableVertexAttribArray(normalAttr)
            GLES20.glVertexAttribPointer(normalAttr, 3, GLES20.GL_FLOAT, false, STRIDE_BYTES, 12)
        }
        if (uvAttr >= 0) {
            GLES20.glEnableVertexAttribArray(uvAttr)
            GLES20.glVertexAttribPointer(uvAttr, 2, GLES20.GL_FLOAT, false, STRIDE_BYTES, 24)
        }

        GLES20.glDrawElements(drawMode, indexCount, GLES20.GL_UNSIGNED_SHORT, 0)

        GLES20.glDisableVertexAttribArray(posAttr)
        if (normalAttr >= 0) GLES20.glDisableVertexAttribArray(normalAttr)
        if (uvAttr >= 0) GLES20.glDisableVertexAttribArray(uvAttr)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    fun release() {
        val ids = IntArray(2)
        var count = 0
        if (vboId != 0) {
            ids[count++] = vboId
            vboId = 0
        }
        if (iboId != 0) {
            ids[count++] = iboId
            iboId = 0
        }
        if (count > 0) {
            GLES20.glDeleteBuffers(count, ids, 0)
        }
    }

    companion object {
        private const val FLOATS_PER_VERTEX = 8
        private const val STRIDE_BYTES = FLOATS_PER_VERTEX * 4
    }
}

/**
 * Single-VBO line/point mesh ([x, y, z] tightly packed) for starfield & arena grid.
 */
internal class GpuSimpleLinesMesh(
    points: FloatArray,
    val drawMode: Int
) {
    val vertexCount: Int = points.size / 3
    var vboId: Int = 0
        private set

    init {
        val ids = IntArray(1)
        GLES20.glGenBuffers(1, ids, 0)
        vboId = ids[0]
        val vb: FloatBuffer = ByteBuffer.allocateDirect(points.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(points)
                position(0)
            }
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, points.size * 4, vb, GLES20.GL_STATIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    fun draw(posAttr: Int) {
        if (vboId == 0 || vertexCount <= 0 || posAttr < 0) return
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
        GLES20.glEnableVertexAttribArray(posAttr)
        GLES20.glVertexAttribPointer(posAttr, 3, GLES20.GL_FLOAT, false, 12, 0)
        GLES20.glDrawArrays(drawMode, 0, vertexCount)
        GLES20.glDisableVertexAttribArray(posAttr)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    fun release() {
        if (vboId != 0) {
            val ids = intArrayOf(vboId)
            GLES20.glDeleteBuffers(1, ids, 0)
            vboId = 0
        }
    }
}

internal object Boss3DMeshFactory {

    /**
     * Unit billboard quad [-0.5..0.5, -0.5..0.5, 0] facing +Z with upright Bitmap UVs.
     */
    fun createBillboardQuadMesh(): GpuIndexedMesh {
        val verts = floatArrayOf(
            // x,    y,   z,  nx, ny, nz,  u,  v
            -0.5f, -0.5f, 0f, 0f, 0f, 1f, 0f, 1f, // Bottom-Left
            0.5f, -0.5f, 0f, 0f, 0f, 1f, 1f, 1f,  // Bottom-Right
            0.5f, 0.5f, 0f, 0f, 0f, 1f, 1f, 0f,   // Top-Right
            -0.5f, 0.5f, 0f, 0f, 0f, 1f, 0f, 0f   // Top-Left
        )
        val inds = shortArrayOf(0, 1, 2, 0, 2, 3)
        return GpuIndexedMesh(verts, inds)
    }

    /** Smooth high-tessellation unit sphere with spherical UVs */
    fun createUnitSphere(stacks: Int = 20, slices: Int = 28): GpuIndexedMesh {
        val verts = ArrayList<Float>((stacks + 1) * (slices + 1) * 8)
        val inds = ArrayList<Short>(stacks * slices * 6)

        for (i in 0..stacks) {
            val v = i.toFloat() / stacks.toFloat()
            val phi = v * PI.toFloat()
            val sinPhi = sin(phi)
            val cosPhi = cos(phi)
            for (j in 0..slices) {
                val u = j.toFloat() / slices.toFloat()
                val theta = u * (2f * PI.toFloat())
                val nx = sinPhi * cos(theta)
                val ny = cosPhi
                val nz = sinPhi * sin(theta)
                verts.add(nx); verts.add(ny); verts.add(nz)
                verts.add(nx); verts.add(ny); verts.add(nz)
                verts.add(u); verts.add(v)
            }
        }

        val ring = slices + 1
        for (i in 0 until stacks) {
            for (j in 0 until slices) {
                val a = (i * ring + j).toShort()
                val b = ((i + 1) * ring + j).toShort()
                val c = ((i + 1) * ring + j + 1).toShort()
                val d = (i * ring + j + 1).toShort()
                inds.add(a); inds.add(b); inds.add(d)
                inds.add(b); inds.add(c); inds.add(d)
            }
        }
        return GpuIndexedMesh(verts.toFloatArray(), inds.toShortArray())
    }

    /** High-tessellation unit cylinder centered vertically in [-0.5, 0.5] with planar cap UVs */
    fun createUnitCylinder(segments: Int = 48): GpuIndexedMesh {
        val verts = ArrayList<Float>()
        val inds = ArrayList<Short>()

        for (ySign in listOf(-0.5f, 0.5f)) {
            val vCoord = if (ySign < 0f) 1f else 0f
            for (i in 0..segments) {
                val u = i.toFloat() / segments.toFloat()
                val a = u * (2f * PI.toFloat())
                val nx = cos(a)
                val nz = sin(a)
                verts.add(nx); verts.add(ySign); verts.add(nz)
                verts.add(nx); verts.add(0f); verts.add(nz)
                verts.add(u * 2f); verts.add(vCoord)
            }
        }
        val stride = segments + 1
        for (i in 0 until segments) {
            val b0 = i.toShort()
            val b1 = (i + 1).toShort()
            val t0 = (stride + i).toShort()
            val t1 = (stride + i + 1).toShort()
            inds.add(b0); inds.add(t0); inds.add(b1)
            inds.add(b1); inds.add(t0); inds.add(t1)
        }

        val topCenterIdx = (verts.size / 8).toShort()
        verts.add(0f); verts.add(0.5f); verts.add(0f)
        verts.add(0f); verts.add(1f); verts.add(0f)
        verts.add(0.5f); verts.add(0.5f)
        val topRingStart = verts.size / 8
        for (i in 0..segments) {
            val a = (i.toFloat() / segments.toFloat()) * (2f * PI.toFloat())
            val cx = cos(a)
            val cz = sin(a)
            verts.add(cx); verts.add(0.5f); verts.add(cz)
            verts.add(0f); verts.add(1f); verts.add(0f)
            verts.add(cx * 0.5f + 0.5f); verts.add(cz * 0.5f + 0.5f)
        }
        for (i in 0 until segments) {
            inds.add(topCenterIdx)
            inds.add((topRingStart + i + 1).toShort())
            inds.add((topRingStart + i).toShort())
        }

        val botCenterIdx = (verts.size / 8).toShort()
        verts.add(0f); verts.add(-0.5f); verts.add(0f)
        verts.add(0f); verts.add(-1f); verts.add(0f)
        verts.add(0.5f); verts.add(0.5f)
        val botRingStart = verts.size / 8
        for (i in 0..segments) {
            val a = (i.toFloat() / segments.toFloat()) * (2f * PI.toFloat())
            val cx = cos(a)
            val cz = sin(a)
            verts.add(cx); verts.add(-0.5f); verts.add(cz)
            verts.add(0f); verts.add(-1f); verts.add(0f)
            verts.add(cx * 0.5f + 0.5f); verts.add(cz * 0.5f + 0.5f)
        }
        for (i in 0 until segments) {
            inds.add(botCenterIdx)
            inds.add((botRingStart + i).toShort())
            inds.add((botRingStart + i + 1).toShort())
        }

        return GpuIndexedMesh(verts.toFloatArray(), inds.toShortArray())
    }

    /** Unit cube centered at origin with [-0.5, 0.5] extent, flat face normals, and full [0..1] UVs */
    fun createUnitBox(): GpuIndexedMesh {
        val faceVerts = floatArrayOf(
            // Front (+Z)
            -0.5f, -0.5f, 0.5f, 0f, 0f, 1f, 0f, 1f,
            0.5f, -0.5f, 0.5f, 0f, 0f, 1f, 1f, 1f,
            0.5f, 0.5f, 0.5f, 0f, 0f, 1f, 1f, 0f,
            -0.5f, 0.5f, 0.5f, 0f, 0f, 1f, 0f, 0f,
            // Back (-Z)
            0.5f, -0.5f, -0.5f, 0f, 0f, -1f, 0f, 1f,
            -0.5f, -0.5f, -0.5f, 0f, 0f, -1f, 1f, 1f,
            -0.5f, 0.5f, -0.5f, 0f, 0f, -1f, 1f, 0f,
            0.5f, 0.5f, -0.5f, 0f, 0f, -1f, 0f, 0f,
            // Left (-X)
            -0.5f, -0.5f, -0.5f, -1f, 0f, 0f, 0f, 1f,
            -0.5f, -0.5f, 0.5f, -1f, 0f, 0f, 1f, 1f,
            -0.5f, 0.5f, 0.5f, -1f, 0f, 0f, 1f, 0f,
            -0.5f, 0.5f, -0.5f, -1f, 0f, 0f, 0f, 0f,
            // Right (+X)
            0.5f, -0.5f, 0.5f, 1f, 0f, 0f, 0f, 1f,
            0.5f, -0.5f, -0.5f, 1f, 0f, 0f, 1f, 1f,
            0.5f, 0.5f, -0.5f, 1f, 0f, 0f, 1f, 0f,
            0.5f, 0.5f, 0.5f, 1f, 0f, 0f, 0f, 0f,
            // Top (+Y)
            -0.5f, 0.5f, 0.5f, 0f, 1f, 0f, 0f, 1f,
            0.5f, 0.5f, 0.5f, 0f, 1f, 0f, 1f, 1f,
            0.5f, 0.5f, -0.5f, 0f, 1f, 0f, 1f, 0f,
            -0.5f, 0.5f, -0.5f, 0f, 1f, 0f, 0f, 0f,
            // Bottom (-Y)
            -0.5f, -0.5f, -0.5f, 0f, -1f, 0f, 0f, 1f,
            0.5f, -0.5f, -0.5f, 0f, -1f, 0f, 1f, 1f,
            0.5f, -0.5f, 0.5f, 0f, -1f, 0f, 1f, 0f,
            -0.5f, -0.5f, 0.5f, 0f, -1f, 0f, 0f, 0f
        )
        val inds = ShortArray(36)
        for (f in 0 until 6) {
            val base = (f * 4).toShort()
            val idx = f * 6
            inds[idx] = base
            inds[idx + 1] = (base + 1).toShort()
            inds[idx + 2] = (base + 2).toShort()
            inds[idx + 3] = base
            inds[idx + 4] = (base + 2).toShort()
            inds[idx + 5] = (base + 3).toShort()
        }
        return GpuIndexedMesh(faceVerts, inds)
    }

    /** Smooth flat annular ring in XZ plane: innerRadius..outerRadius */
    fun createFlatRing(innerRadius: Float = 0.86f, outerRadius: Float = 1.0f, segments: Int = 64): GpuIndexedMesh {
        val verts = ArrayList<Float>((segments + 1) * 2 * 8)
        val inds = ArrayList<Short>(segments * 6)
        for (i in 0..segments) {
            val u = i.toFloat() / segments.toFloat()
            val a = u * (2f * PI.toFloat())
            val c = cos(a)
            val s = sin(a)
            verts.add(c * innerRadius); verts.add(0f); verts.add(s * innerRadius)
            verts.add(0f); verts.add(1f); verts.add(0f)
            verts.add(u); verts.add(0f)

            verts.add(c * outerRadius); verts.add(0f); verts.add(s * outerRadius)
            verts.add(0f); verts.add(1f); verts.add(0f)
            verts.add(u); verts.add(1f)
        }
        for (i in 0 until segments) {
            val i0 = (i * 2).toShort()
            val i1 = (i * 2 + 1).toShort()
            val i2 = (i * 2 + 2).toShort()
            val i3 = (i * 2 + 3).toShort()
            inds.add(i0); inds.add(i1); inds.add(i2)
            inds.add(i1); inds.add(i3); inds.add(i2)
        }
        return GpuIndexedMesh(verts.toFloatArray(), inds.toShortArray())
    }

    /** Starfield dome of anti-aliased stars drawn in 1 single draw call */
    fun createStarfield(starCount: Int = 340): GpuSimpleLinesMesh {
        val pts = FloatArray(starCount * 3)
        val rng = Random(20250621L)
        for (i in 0 until starCount) {
            val theta = rng.nextFloat() * (2f * PI.toFloat())
            val phi = (rng.nextFloat() - 0.15f) * PI.toFloat() * 0.72f
            val r = 72f + rng.nextFloat() * 26f
            pts[i * 3] = r * cos(phi) * cos(theta)
            pts[i * 3 + 1] = r * sin(phi) + 8f
            pts[i * 3 + 2] = r * cos(phi) * sin(theta)
        }
        return GpuSimpleLinesMesh(pts, GLES20.GL_POINTS)
    }

    /** Concentric + radial cybernetic deck lines drawn in 1 single draw call */
    fun createDeckGrid(rings: Int = 4, spokes: Int = 16): GpuSimpleLinesMesh {
        val segsPerRing = 48
        val list = ArrayList<Float>()
        for (rIdx in 1..rings) {
            val r = rIdx.toFloat() / rings.toFloat()
            for (s in 0 until segsPerRing) {
                val a0 = (s.toFloat() / segsPerRing) * (2f * PI.toFloat())
                val a1 = ((s + 1).toFloat() / segsPerRing) * (2f * PI.toFloat())
                list.add(cos(a0) * r); list.add(0.03f); list.add(sin(a0) * r)
                list.add(cos(a1) * r); list.add(0.03f); list.add(sin(a1) * r)
            }
        }
        for (sp in 0 until spokes) {
            val a = (sp.toFloat() / spokes) * (2f * PI.toFloat())
            list.add(0f); list.add(0.03f); list.add(0f)
            list.add(cos(a)); list.add(0.03f); list.add(sin(a))
        }
        return GpuSimpleLinesMesh(list.toFloatArray(), GLES20.GL_LINES)
    }
}
