package com.example.game.engine

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class GlMesh(
  vertices: FloatArray,
  indices: ShortArray,
) {
  val indexCount: Int = indices.size
  private val vaoId: Int
  private val vboId: Int
  private val iboId: Int

  init {
    val vaos = IntArray(1)
    val buffers = IntArray(2)
    GLES30.glGenVertexArrays(1, vaos, 0)
    GLES30.glGenBuffers(2, buffers, 0)
    vaoId = vaos[0]
    vboId = buffers[0]
    iboId = buffers[1]

    val vBuf: FloatBuffer =
      ByteBuffer.allocateDirect(vertices.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .put(vertices)
    vBuf.position(0)

    val iBuf: ShortBuffer =
      ByteBuffer.allocateDirect(indices.size * 2)
        .order(ByteOrder.nativeOrder())
        .asShortBuffer()
        .put(indices)
    iBuf.position(0)

    GLES30.glBindVertexArray(vaoId)
    GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
    GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * 4, vBuf, GLES30.GL_STATIC_DRAW)

    val stride = 11 * 4
    // aPos (3)
    GLES30.glEnableVertexAttribArray(0)
    GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
    // aNormal (3)
    GLES30.glEnableVertexAttribArray(1)
    GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
    // aColor (3)
    GLES30.glEnableVertexAttribArray(2)
    GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, stride, 6 * 4)
    // aExtra (2)
    GLES30.glEnableVertexAttribArray(3)
    GLES30.glVertexAttribPointer(3, 2, GLES30.GL_FLOAT, false, stride, 9 * 4)

    GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, iboId)
    GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indices.size * 2, iBuf, GLES30.GL_STATIC_DRAW)

    GLES30.glBindVertexArray(0)
  }

  fun draw() {
    GLES30.glBindVertexArray(vaoId)
    GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_SHORT, 0)
    GLES30.glBindVertexArray(0)
  }

  fun release() {
    GLES30.glDeleteVertexArrays(1, intArrayOf(vaoId), 0)
    GLES30.glDeleteBuffers(2, intArrayOf(vboId, iboId), 0)
  }
}

class MeshDataBuilder {
  private val verts = ArrayList<Float>(1024)
  private val inds = ArrayList<Short>(1024)

  fun addVertex(
    x: Float,
    y: Float,
    z: Float,
    nx: Float,
    ny: Float,
    nz: Float,
    r: Float,
    g: Float,
    b: Float,
    matType: Float = 0f,
    shine: Float = 24f,
  ): Short {
    val idx = (verts.size / 11).toShort()
    verts.add(x); verts.add(y); verts.add(z)
    verts.add(nx); verts.add(ny); verts.add(nz)
    verts.add(r); verts.add(g); verts.add(b)
    verts.add(matType); verts.add(shine)
    return idx
  }

  fun addQuad(
    x0: Float, y0: Float, z0: Float,
    x1: Float, y1: Float, z1: Float,
    x2: Float, y2: Float, z2: Float,
    x3: Float, y3: Float, z3: Float,
    r: Float, g: Float, b: Float,
    matType: Float = 0f,
    shine: Float = 24f,
  ) {
    val ux = x1 - x0; val uy = y1 - y0; val uz = z1 - z0
    val vx = x2 - x0; val vy = y2 - y0; val vz = z2 - z0
    var nx = uy * vz - uz * vy
    var ny = uz * vx - ux * vz
    var nz = ux * vy - uy * vx
    val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)
    nx /= len; ny /= len; nz /= len

    val i0 = addVertex(x0, y0, z0, nx, ny, nz, r, g, b, matType, shine)
    val i1 = addVertex(x1, y1, z1, nx, ny, nz, r, g, b, matType, shine)
    val i2 = addVertex(x2, y2, z2, nx, ny, nz, r, g, b, matType, shine)
    val i3 = addVertex(x3, y3, z3, nx, ny, nz, r, g, b, matType, shine)
    inds.add(i0); inds.add(i1); inds.add(i2)
    inds.add(i0); inds.add(i2); inds.add(i3)
  }

  fun addBox(
    cx: Float, cy: Float, cz: Float,
    sx: Float, sy: Float, sz: Float,
    r: Float, g: Float, b: Float,
    matType: Float = 0f,
    shine: Float = 24f,
  ) {
    val hx = sx * 0.5f; val hy = sy * 0.5f; val hz = sz * 0.5f
    val x0 = cx - hx; val x1 = cx + hx
    val y0 = cy - hy; val y1 = cy + hy
    val z0 = cz - hz; val z1 = cz + hz
    // +Y top
    addQuad(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, r, g, b, matType, shine)
    // -Y bottom
    addQuad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r * 0.7f, g * 0.7f, b * 0.7f, matType, shine)
    // +Z front
    addQuad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, matType, shine)
    // -Z back
    addQuad(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, r, g, b, matType, shine)
    // +X right
    addQuad(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, r * 0.94f, g * 0.94f, b * 0.94f, matType, shine)
    // -X left
    addQuad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r * 0.94f, g * 0.94f, b * 0.94f, matType, shine)
  }

  fun addTaperedBox(
    cx: Float, cy: Float, cz: Float,
    botX: Float, botZ: Float,
    topX: Float, topZ: Float,
    sy: Float,
    zShiftTop: Float = 0f,
    r: Float, g: Float, b: Float,
    matType: Float = 0f,
    shine: Float = 48f,
  ) {
    val bx0 = cx - botX * 0.5f; val bx1 = cx + botX * 0.5f
    val bz0 = cz - botZ * 0.5f; val bz1 = cz + botZ * 0.5f
    val tx0 = cx - topX * 0.5f; val tx1 = cx + topX * 0.5f
    val tz0 = cz - topZ * 0.5f + zShiftTop; val tz1 = cz + topZ * 0.5f + zShiftTop
    val y0 = cy - sy * 0.5f; val y1 = cy + sy * 0.5f

    addQuad(tx0, y1, tz1, tx1, y1, tz1, tx1, y1, tz0, tx0, y1, tz0, r, g, b, matType, shine)
    addQuad(bx0, y0, bz0, bx1, y0, bz0, bx1, y0, bz1, bx0, y0, bz1, r * 0.7f, g * 0.7f, b * 0.7f, matType, shine)
    addQuad(bx0, y0, bz1, bx1, y0, bz1, tx1, y1, tz1, tx0, y1, tz1, r, g, b, matType, shine)
    addQuad(bx1, y0, bz0, bx0, y0, bz0, tx0, y1, tz0, tx1, y1, tz0, r, g, b, matType, shine)
    addQuad(bx1, y0, bz1, bx1, y0, bz0, tx1, y1, tz0, tx1, y1, tz1, r * 0.94f, g * 0.94f, b * 0.94f, matType, shine)
    addQuad(bx0, y0, bz0, bx0, y0, bz1, tx0, y1, tz1, tx0, y1, tz0, r * 0.94f, g * 0.94f, b * 0.94f, matType, shine)
  }

  fun addCylinderAlongX(
    cx: Float, cy: Float, cz: Float,
    radius: Float, width: Float, segments: Int,
    tireR: Float, tireG: Float, tireB: Float,
    rimR: Float, rimG: Float, rimB: Float,
  ) {
    val hx = width * 0.5f
    val step = (2.0 * PI / segments).toFloat()
    for (i in 0 until segments) {
      val a0 = i * step
      val a1 = (i + 1) * step
      val y0 = cy + cos(a0) * radius; val z0 = cz + sin(a0) * radius
      val y1 = cy + cos(a1) * radius; val z1 = cz + sin(a1) * radius
      // Tread
      addQuad(cx - hx, y0, z0, cx + hx, y0, z0, cx + hx, y1, z1, cx - hx, y1, z1, tireR, tireG, tireB, 0f, 12f)
      // Outer rim face (+X)
      val cPlus = addVertex(cx + hx, cy, cz, 1f, 0f, 0f, rimR, rimG, rimB, 0f, 72f)
      val p0 = addVertex(cx + hx, y0, z0, 1f, 0f, 0f, rimR * 0.65f, rimG * 0.65f, rimB * 0.65f, 0f, 64f)
      val p1 = addVertex(cx + hx, y1, z1, 1f, 0f, 0f, rimR * 0.65f, rimG * 0.65f, rimB * 0.65f, 0f, 64f)
      inds.add(cPlus); inds.add(p1); inds.add(p0)
      // Inner rim face (-X)
      val cMin = addVertex(cx - hx, cy, cz, -1f, 0f, 0f, rimR, rimG, rimB, 0f, 72f)
      val q0 = addVertex(cx - hx, y0, z0, -1f, 0f, 0f, rimR * 0.65f, rimG * 0.65f, rimB * 0.65f, 0f, 64f)
      val q1 = addVertex(cx - hx, y1, z1, -1f, 0f, 0f, rimR * 0.65f, rimG * 0.65f, rimB * 0.65f, 0f, 64f)
      inds.add(cMin); inds.add(q0); inds.add(q1)
    }
  }

  fun addCylinderAlongY(
    cx: Float, cy0: Float, cz: Float,
    botR: Float, topR: Float, height: Float, segments: Int,
    r: Float, g: Float, b: Float,
    matType: Float = 0f,
    shine: Float = 16f,
  ) {
    val step = (2.0 * PI / segments).toFloat()
    val cy1 = cy0 + height
    for (i in 0 until segments) {
      val a0 = i * step
      val a1 = (i + 1) * step
      val bx0 = cx + cos(a0) * botR; val bz0 = cz + sin(a0) * botR
      val bx1 = cx + cos(a1) * botR; val bz1 = cz + sin(a1) * botR
      val tx0 = cx + cos(a0) * topR; val tz0 = cz + sin(a0) * topR
      val tx1 = cx + cos(a1) * topR; val tz1 = cz + sin(a1) * topR
      addQuad(bx0, cy0, bz0, bx1, cy0, bz1, tx1, cy1, tz1, tx0, cy1, tz0, r, g, b, matType, shine)
    }
  }

  fun build(): GlMesh = GlMesh(verts.toFloatArray(), inds.toShortArray())
}

object MeshFactory {
  fun createSkyQuad(): GlMesh {
    val b = MeshDataBuilder()
    val i0 = b.addVertex(-1f, -1f, 0f, 0f, 0f, 1f, 1f, 1f, 1f)
    val i1 = b.addVertex(1f, -1f, 0f, 0f, 0f, 1f, 1f, 1f, 1f)
    val i2 = b.addVertex(1f, 1f, 0f, 0f, 0f, 1f, 1f, 1f, 1f)
    val i3 = b.addVertex(-1f, 1f, 0f, 0f, 0f, 1f, 1f, 1f, 1f)
    // Use build manually via addQuad
    val b2 = MeshDataBuilder()
    b2.addQuad(-1f, -1f, 0f, 1f, -1f, 0f, 1f, 1f, 0f, -1f, 1f, 0f, 1f, 1f, 1f)
    return b2.build()
  }

  fun createGroundPlane(): GlMesh {
    val b = MeshDataBuilder()
    // Land covers X: -360..175, Z: -360..360
    b.addQuad(-360f, 0f, 360f, 175f, 0f, 360f, 175f, 0f, -360f, -360f, 0f, -360f, 1f, 1f, 1f, 0f, 24f)
    return b.build()
  }

  fun createOceanPlane(): GlMesh {
    val b = MeshDataBuilder()
    b.addQuad(172f, -0.35f, 440f, 520f, -0.35f, 440f, 520f, -0.35f, -440f, 172f, -0.35f, -440f, 1f, 1f, 1f, 1f, 96f)
    return b.build()
  }

  fun createUnitBuildingMesh(): GlMesh {
    // Unit box 1x1x1 centered in XZ, base at Y=0..1, matType = 2.0 (window facade!)
    val b = MeshDataBuilder()
    b.addBox(0f, 0.5f, 0f, 1f, 1f, 1f, 1f, 1f, 1f, matType = 2.0f, shine = 48f)
    return b.build()
  }

  fun createRampMesh(): GlMesh {
    val b = MeshDataBuilder()
    // Unit wedge ramp: width=1 (-0.5..0.5 X), length=1 (-0.5..0.5 Z), height=1 (0 at -0.5Z to 1 at +0.5Z)
    // Top slope
    b.addQuad(-0.5f, 0f, -0.5f, 0.5f, 0f, -0.5f, 0.5f, 1f, 0.5f, -0.5f, 1f, 0.5f, 0.96f, 0.58f, 0.14f, 0f, 32f)
    // Back vertical face
    b.addQuad(0.5f, 0f, 0.5f, -0.5f, 0f, 0.5f, -0.5f, 1f, 0.5f, 0.5f, 1f, 0.5f, 0.25f, 0.27f, 0.32f, 0f, 16f)
    // Right side triangle (via degenerate quad)
    b.addQuad(0.5f, 0f, -0.5f, 0.5f, 0f, 0.5f, 0.5f, 1f, 0.5f, 0.5f, 0f, -0.5f, 0.28f, 0.30f, 0.35f, 0f, 16f)
    // Left side triangle
    b.addQuad(-0.5f, 0f, 0.5f, -0.5f, 0f, -0.5f, -0.5f, 0f, -0.5f, -0.5f, 1f, 0.5f, 0.28f, 0.30f, 0.35f, 0f, 16f)
    return b.build()
  }

  fun createPalmTreeMesh(): GlMesh {
    val b = MeshDataBuilder()
    // Trunk
    b.addCylinderAlongY(0f, 0f, 0f, 0.32f, 0.16f, 1.0f, 7, 0.44f, 0.30f, 0.18f, 0f, 8f)
    // 6 lush fronds at Y=1.0
    for (i in 0 until 6) {
      val a = (i * PI / 3.0).toFloat()
      val dx = cos(a)
      val dz = sin(a)
      val px = -dz * 0.14f
      val pz = dx * 0.14f
      b.addQuad(
        px, 0.98f, pz,
        -px, 0.98f, -pz,
        dx * 0.48f - px * 0.4f, 0.84f, dz * 0.48f - pz * 0.4f,
        dx * 0.48f + px * 0.4f, 0.84f, dz * 0.48f + pz * 0.4f,
        0.16f, 0.56f, 0.20f, 0f, 16f,
      )
    }
    return b.build()
  }

  fun createStreetLampMesh(): GlMesh {
    val b = MeshDataBuilder()
    // Steel pole 6.8m high
    b.addBox(0f, 3.4f, 0f, 0.16f, 6.8f, 0.16f, 0.30f, 0.33f, 0.38f, 0f, 48f)
    // Horizontal overhang arm extending along +X
    b.addBox(0.85f, 6.72f, 0f, 1.7f, 0.12f, 0.14f, 0.30f, 0.33f, 0.38f, 0f, 48f)
    // Emissive warm lamp bulb
    b.addBox(1.55f, 6.60f, 0f, 0.46f, 0.14f, 0.28f, 1.0f, 0.88f, 0.60f, matType = 3.0f, shine = 16f)
    return b.build()
  }

  fun createSupercarChassis(): GlMesh {
    val b = MeshDataBuilder()
    // Lower splitter & rocker skirt (carbon fiber dark)
    b.addBox(0f, 0.26f, 0.0f, 2.02f, 0.14f, 4.42f, 0.10f, 0.11f, 0.13f, 0f, 36f)
    // Sculpted wedge body (matType 1.0 = metallic paint!)
    b.addTaperedBox(0f, 0.56f, 0.0f, 1.98f, 4.32f, 1.78f, 3.85f, 0.48f, -0.06f, 0.88f, 0.14f, 0.18f, matType = 1.0f, shine = 96f)
    // Aerodynamic tinted glass cabin
    b.addTaperedBox(0f, 0.98f, -0.18f, 1.56f, 2.18f, 1.18f, 1.28f, 0.42f, -0.14f, 0.08f, 0.11f, 0.16f, matType = 0f, shine = 120f)
    // Roof painted panel
    b.addBox(0f, 1.20f, -0.30f, 1.16f, 0.05f, 1.18f, 0.88f, 0.14f, 0.18f, matType = 1.0f, shine = 96f)
    // GT Rear Wing Spoiler
    b.addBox(-0.56f, 0.92f, -1.96f, 0.08f, 0.28f, 0.20f, 0.10f, 0.10f, 0.12f, 0f, 32f)
    b.addBox(0.56f, 0.92f, -1.96f, 0.08f, 0.28f, 0.20f, 0.10f, 0.10f, 0.12f, 0f, 32f)
    b.addBox(0f, 1.06f, -2.02f, 1.84f, 0.07f, 0.34f, 0.12f, 0.13f, 0.16f, 0f, 64f)
    // LED Headlights (+Z front)
    b.addBox(-0.68f, 0.54f, 2.14f, 0.42f, 0.12f, 0.10f, 0.98f, 0.98f, 0.86f, matType = 3.0f, shine = 32f)
    b.addBox(0.68f, 0.54f, 2.14f, 0.42f, 0.12f, 0.10f, 0.98f, 0.98f, 0.86f, matType = 3.0f, shine = 32f)
    // LED Taillight bar (-Z rear)
    b.addBox(0f, 0.62f, -2.14f, 1.62f, 0.11f, 0.10f, 0.98f, 0.14f, 0.18f, matType = 3.0f, shine = 32f)
    return b.build()
  }

  fun createHypercarChassis(): GlMesh {
    val b = MeshDataBuilder()
    // Wide carbon aero splitter & rear diffuser
    b.addBox(0f, 0.24f, 0.0f, 2.08f, 0.12f, 4.50f, 0.08f, 0.09f, 0.11f, 0f, 48f)
    // Low aggressive hypercar wedge nose & haunches
    b.addTaperedBox(0f, 0.50f, 0.05f, 2.04f, 4.38f, 1.82f, 3.70f, 0.42f, -0.10f, 0.04f, 0.76f, 0.94f, matType = 1.0f, shine = 120f)
    // Teardrop canopy glass
    b.addTaperedBox(0f, 0.89f, -0.10f, 1.48f, 2.25f, 1.04f, 1.25f, 0.38f, -0.16f, 0.06f, 0.09f, 0.14f, matType = 0f, shine = 128f)
    // Carbon roof spine & air scoop
    b.addBox(0f, 1.12f, -0.45f, 0.34f, 0.10f, 1.35f, 0.10f, 0.11f, 0.13f, 0f, 72f)
    // Twin GT vertical fins + wide carbon wing
    b.addBox(-0.82f, 0.96f, -1.92f, 0.07f, 0.36f, 0.48f, 0.10f, 0.11f, 0.13f, 0f, 48f)
    b.addBox(0.82f, 0.96f, -1.92f, 0.07f, 0.36f, 0.48f, 0.10f, 0.11f, 0.13f, 0f, 48f)
    b.addBox(0f, 1.12f, -2.06f, 1.94f, 0.06f, 0.36f, 0.10f, 0.11f, 0.14f, 0f, 72f)
    // Razor LED headlights & quad rear tail strips
    b.addBox(-0.72f, 0.48f, 2.18f, 0.44f, 0.08f, 0.10f, 0.92f, 0.98f, 1.0f, matType = 3.0f, shine = 32f)
    b.addBox(0.72f, 0.48f, 2.18f, 0.44f, 0.08f, 0.10f, 0.92f, 0.98f, 1.0f, matType = 3.0f, shine = 32f)
    b.addBox(-0.62f, 0.58f, -2.18f, 0.46f, 0.09f, 0.10f, 1.0f, 0.15f, 0.18f, matType = 3.0f, shine = 32f)
    b.addBox(0.62f, 0.58f, -2.18f, 0.46f, 0.09f, 0.10f, 1.0f, 0.15f, 0.18f, matType = 3.0f, shine = 32f)
    return b.build()
  }

  fun createMuscleOrSedanChassis(isMuscle: Boolean): GlMesh {
    val b = MeshDataBuilder()
    b.addBox(0f, 0.28f, 0f, 1.98f, 0.16f, 4.46f, 0.12f, 0.13f, 0.15f, 0f, 32f)
    b.addTaperedBox(0f, 0.62f, 0f, 1.98f, 4.40f, 1.86f, 4.15f, 0.54f, 0f, 0.85f, 0.62f, 0.12f, matType = 1.0f, shine = 88f)
    // Cabin
    b.addTaperedBox(0f, 1.08f, -0.20f, 1.66f, 2.25f, 1.38f, 1.55f, 0.46f, -0.08f, 0.10f, 0.13f, 0.18f, 0f, 110f)
    b.addBox(0f, 1.32f, -0.26f, 1.36f, 0.06f, 1.48f, 0.85f, 0.62f, 0.12f, matType = 1.0f, shine = 88f)
    if (isMuscle) {
      // Hood blower scoop
      b.addBox(0f, 0.93f, 1.15f, 0.54f, 0.12f, 0.95f, 0.14f, 0.15f, 0.18f, 0f, 64f)
    }
    // Headlights & Taillights
    b.addBox(-0.68f, 0.60f, 2.20f, 0.36f, 0.14f, 0.10f, 0.98f, 0.96f, 0.82f, matType = 3.0f, shine = 32f)
    b.addBox(0.68f, 0.60f, 2.20f, 0.36f, 0.14f, 0.10f, 0.98f, 0.96f, 0.82f, matType = 3.0f, shine = 32f)
    b.addBox(-0.68f, 0.64f, -2.20f, 0.40f, 0.14f, 0.10f, 0.96f, 0.12f, 0.16f, matType = 3.0f, shine = 32f)
    b.addBox(0.68f, 0.64f, -2.20f, 0.40f, 0.14f, 0.10f, 0.96f, 0.12f, 0.16f, matType = 3.0f, shine = 32f)
    return b.build()
  }

  fun createPoliceChassis(): GlMesh {
    val b = MeshDataBuilder()
    // Black & white Vice PD livery
    b.addBox(0f, 0.28f, 0f, 2.02f, 0.16f, 4.55f, 0.10f, 0.11f, 0.14f, 0f, 36f)
    // Front & rear dark sections + white doors
    b.addBox(0f, 0.62f, 1.35f, 1.98f, 0.54f, 1.75f, 0.08f, 0.10f, 0.14f, 0f, 80f)
    b.addBox(0f, 0.62f, -1.35f, 1.98f, 0.54f, 1.75f, 0.08f, 0.10f, 0.14f, 0f, 80f)
    b.addBox(0f, 0.62f, 0.0f, 2.00f, 0.54f, 1.15f, 0.94f, 0.95f, 0.97f, 0f, 80f)
    // Cabin + white roof
    b.addTaperedBox(0f, 1.08f, -0.18f, 1.66f, 2.25f, 1.38f, 1.55f, 0.46f, -0.06f, 0.08f, 0.11f, 0.16f, 0f, 110f)
    b.addBox(0f, 1.32f, -0.22f, 1.36f, 0.06f, 1.48f, 0.94f, 0.95f, 0.97f, 0f, 80f)
    // Pushbar
    b.addBox(0f, 0.48f, 2.32f, 1.15f, 0.34f, 0.14f, 0.16f, 0.18f, 0.20f, 0f, 48f)
    // Headlights & Taillights
    b.addBox(-0.68f, 0.60f, 2.25f, 0.36f, 0.14f, 0.10f, 0.98f, 0.96f, 0.85f, matType = 3.0f, shine = 32f)
    b.addBox(0.68f, 0.60f, 2.25f, 0.36f, 0.14f, 0.10f, 0.98f, 0.96f, 0.85f, matType = 3.0f, shine = 32f)
    b.addBox(-0.68f, 0.64f, -2.25f, 0.42f, 0.14f, 0.10f, 0.96f, 0.12f, 0.16f, matType = 3.0f, shine = 32f)
    b.addBox(0.68f, 0.64f, -2.25f, 0.42f, 0.14f, 0.10f, 0.96f, 0.12f, 0.16f, matType = 3.0f, shine = 32f)
    return b.build()
  }

  fun createMotorbikeMesh(isDirtBike: Boolean): GlMesh {
    val b = MeshDataBuilder()
    // Engine block & swingarm frame
    b.addBox(0f, 0.48f, -0.05f, 0.34f, 0.36f, 0.82f, 0.18f, 0.20f, 0.24f, 0f, 48f)
    // Dual exhaust pipes
    b.addBox(-0.22f, 0.38f, -0.58f, 0.10f, 0.12f, 0.78f, 0.68f, 0.72f, 0.76f, 0f, 96f)
    b.addBox(0.22f, 0.38f, -0.58f, 0.10f, 0.12f, 0.78f, 0.68f, 0.72f, 0.76f, 0f, 96f)
    // Sculpted Fuel Tank & Fairing (matType = 1.0 metallic paint!)
    if (isDirtBike) {
      b.addTaperedBox(0f, 0.78f, 0.22f, 0.32f, 0.66f, 0.24f, 0.45f, 0.26f, 0f, 0.1f, 0.8f, 0.4f, matType = 1.0f, shine = 72f)
      // High front fender
      b.addBox(0f, 0.76f, 0.76f, 0.20f, 0.06f, 0.48f, 0.1f, 0.8f, 0.4f, matType = 1.0f, shine = 72f)
    } else {
      b.addTaperedBox(0f, 0.74f, 0.28f, 0.44f, 0.86f, 0.32f, 0.62f, 0.30f, 0.08f, 0.04f, 0.76f, 0.94f, matType = 1.0f, shine = 110f)
      // Smoked aero windscreen
      b.addTaperedBox(0f, 0.98f, 0.56f, 0.34f, 0.36f, 0.26f, 0.20f, 0.20f, -0.10f, 0.08f, 0.11f, 0.16f, 0f, 120f)
    }
    // Tail cowl & Seat
    b.addBox(0f, 0.74f, -0.34f, 0.28f, 0.12f, 0.64f, 0.12f, 0.13f, 0.15f, 0f, 18f)
    b.addTaperedBox(0f, 0.82f, -0.74f, 0.28f, 0.44f, 0.16f, 0.32f, 0.16f, -0.06f, 0.04f, 0.76f, 0.94f, matType = 1.0f, shine = 96f)
    // Front forks + Handlebars
    b.addBox(-0.14f, 0.68f, 0.64f, 0.07f, 0.62f, 0.08f, 0.78f, 0.76f, 0.72f, 0f, 80f)
    b.addBox(0.14f, 0.68f, 0.64f, 0.07f, 0.62f, 0.08f, 0.78f, 0.76f, 0.72f, 0f, 80f)
    b.addBox(0f, 0.98f, 0.52f, 0.68f, 0.06f, 0.07f, 0.14f, 0.15f, 0.18f, 0f, 48f)
    // Headlight & Taillight
    b.addBox(0f, 0.78f, 0.74f, 0.22f, 0.14f, 0.08f, 1.0f, 0.98f, 0.86f, matType = 3.0f, shine = 32f)
    b.addBox(0f, 0.80f, -0.96f, 0.18f, 0.09f, 0.08f, 0.98f, 0.12f, 0.18f, matType = 3.0f, shine = 32f)

    // Articulated Rider sitting & leaning forward on the bike!
    // Rider legs hugging frame
    b.addBox(-0.20f, 0.58f, -0.08f, 0.13f, 0.48f, 0.18f, 0.12f, 0.14f, 0.20f, 0f, 16f)
    b.addBox(0.20f, 0.58f, -0.08f, 0.13f, 0.48f, 0.18f, 0.12f, 0.14f, 0.20f, 0f, 16f)
    // Rider leather jacket torso (crouched forward)
    b.addTaperedBox(0f, 1.08f, -0.14f, 0.38f, 0.24f, 0.44f, 0.26f, 0.48f, 0.18f, 0.16f, 0.18f, 0.22f, 0f, 24f)
    // Rider arms reaching handlebars
    b.addBox(-0.26f, 1.06f, 0.24f, 0.11f, 0.12f, 0.54f, 0.16f, 0.18f, 0.22f, 0f, 24f)
    b.addBox(0.26f, 1.06f, 0.24f, 0.11f, 0.12f, 0.54f, 0.16f, 0.18f, 0.22f, 0f, 24f)
    // Full-face sport helmet + tinted visor
    b.addBox(0f, 1.44f, 0.10f, 0.27f, 0.28f, 0.30f, 0.92f, 0.94f, 0.96f, 0f, 96f)
    b.addBox(0f, 1.45f, 0.23f, 0.22f, 0.12f, 0.08f, 0.05f, 0.08f, 0.12f, 0f, 120f)
    return b.build()
  }

  fun createWheelMesh(radius: Float, width: Float): GlMesh {
    val b = MeshDataBuilder()
    b.addCylinderAlongX(
      cx = 0f, cy = 0f, cz = 0f,
      radius = radius, width = width, segments = 14,
      tireR = 0.09f, tireG = 0.09f, tireB = 0.10f,
      rimR = 0.72f, rimG = 0.75f, rimB = 0.80f,
    )
    return b.build()
  }

  fun createUnitBoxMesh(r: Float, g: Float, bCol: Float, matType: Float = 0f): GlMesh {
    val b = MeshDataBuilder()
    b.addBox(0f, 0f, 0f, 1f, 1f, 1f, r, g, bCol, matType, 32f)
    return b.build()
  }

  fun createShadowDiscMesh(): GlMesh {
    val b = MeshDataBuilder()
    val segs = 14
    val step = (2.0 * PI / segs).toFloat()
    for (i in 0 until segs) {
      val a0 = i * step
      val a1 = (i + 1) * step
      b.addQuad(
        0f, 0.015f, 0f,
        cos(a0) * 0.5f, 0.015f, sin(a0) * 0.5f,
        cos(a1) * 0.5f, 0.015f, sin(a1) * 0.5f,
        0f, 0.015f, 0f,
        0.04f, 0.04f, 0.05f, 0f, 4f,
      )
    }
    return b.build()
  }

  fun createPickupMesh(): GlMesh {
    val b = MeshDataBuilder()
    // Spinning emerald cash prism
    b.addBox(0f, 0f, 0f, 0.9f, 0.55f, 0.45f, 0.18f, 0.92f, 0.52f, matType = 3.0f, shine = 64f)
    return b.build()
  }

  fun createCheckpointMesh(): GlMesh {
    val b = MeshDataBuilder()
    // Glowing cyan/gold beacon pillar
    b.addCylinderAlongY(0f, 0f, 0f, 2.2f, 2.2f, 10f, 16, 0.12f, 0.88f, 1.0f, matType = 3.0f, shine = 32f)
    return b.build()
  }
}
