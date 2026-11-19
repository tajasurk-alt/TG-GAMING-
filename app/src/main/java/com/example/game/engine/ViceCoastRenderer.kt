package com.example.game.engine

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.game.audio.SoundEngine
import com.example.game.model.CameraMode
import com.example.game.model.HudSnapshot
import com.example.game.model.VehicleEntity
import com.example.game.model.VehicleModelType
import com.example.game.sim.GameSimulation
import com.example.game.world.WorldMap
import java.util.concurrent.ConcurrentLinkedQueue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ViceCoastRenderer(
  val simulation: GameSimulation,
  val soundEngine: SoundEngine,
) : GLSurfaceView.Renderer {

  private val _hudState = MutableStateFlow(simulation.buildHudSnapshot(60))
  val hudState: StateFlow<HudSnapshot> = _hudState.asStateFlow()

  private val pendingActions = ConcurrentLinkedQueue<() -> Unit>()
  @Volatile var isPaused: Boolean = false

  // Shaders
  private lateinit var skyShader: ShaderProgram
  private lateinit var groundShader: ShaderProgram
  private lateinit var litShader: ShaderProgram

  // Meshes
  private lateinit var skyQuad: GlMesh
  private lateinit var groundPlane: GlMesh
  private lateinit var oceanPlane: GlMesh
  private lateinit var buildingMesh: GlMesh
  private lateinit var rampMesh: GlMesh
  private lateinit var palmMesh: GlMesh
  private lateinit var lampMesh: GlMesh
  private lateinit var supercarMesh: GlMesh
  private lateinit var hypercarMesh: GlMesh
  private lateinit var muscleMesh: GlMesh
  private lateinit var sedanMesh: GlMesh
  private lateinit var policeMesh: GlMesh
  private lateinit var sportBikeMesh: GlMesh
  private lateinit var dirtBikeMesh: GlMesh
  private lateinit var carWheelMesh: GlMesh
  private lateinit var bikeWheelMesh: GlMesh
  private lateinit var shadowMesh: GlMesh
  private lateinit var pickupMesh: GlMesh
  private lateinit var checkpointMesh: GlMesh
  private lateinit var unitBoxBody: GlMesh
  private lateinit var unitBoxSkin: GlMesh
  private lateinit var unitBoxDark: GlMesh
  private lateinit var unitBoxFlame: GlMesh
  private lateinit var unitBoxSkid: GlMesh

  // Matrices (zero allocation during draw)
  private val projMatrix = FloatArray(16)
  private val viewMatrix = FloatArray(16)
  private val viewProjMatrix = FloatArray(16)
  private val invViewProjMatrix = FloatArray(16)
  private val modelMatrix = FloatArray(16)
  private val mvpMatrix = FloatArray(16)
  private val wheelMatrix = FloatArray(16)

  private var aspectRatio = 16f / 9f
  private var lastFrameNs = System.nanoTime()
  private var simTimeSec = 0f
  private var hudAccumSec = 0f
  private var fpsCounter = 0
  private var fpsAccum = 0f
  private var currentFps = 60

  // Smooth chase camera state
  private var camX = 114f
  private var camY = 5f
  private var camZ = 28f
  private var lookX = 114f
  private var lookY = 1.5f
  private var lookZ = 14f

  fun enqueueAction(action: () -> Unit) {
    pendingActions.add(action)
  }

  override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    GLES30.glClearColor(0.05f, 0.06f, 0.10f, 1.0f)
    GLES30.glEnable(GLES30.GL_DEPTH_TEST)
    GLES30.glDisable(GLES30.GL_CULL_FACE)

    skyShader = ShaderProgram(GameShaders.SKY_VERTEX, GameShaders.SKY_FRAGMENT)
    groundShader = ShaderProgram(GameShaders.WORLD_VERTEX, GameShaders.GROUND_FRAGMENT)
    litShader = ShaderProgram(GameShaders.WORLD_VERTEX, GameShaders.LIT_FRAGMENT)

    skyQuad = MeshFactory.createSkyQuad()
    groundPlane = MeshFactory.createGroundPlane()
    oceanPlane = MeshFactory.createOceanPlane()
    buildingMesh = MeshFactory.createUnitBuildingMesh()
    rampMesh = MeshFactory.createRampMesh()
    palmMesh = MeshFactory.createPalmTreeMesh()
    lampMesh = MeshFactory.createStreetLampMesh()
    supercarMesh = MeshFactory.createSupercarChassis()
    hypercarMesh = MeshFactory.createHypercarChassis()
    muscleMesh = MeshFactory.createMuscleOrSedanChassis(isMuscle = true)
    sedanMesh = MeshFactory.createMuscleOrSedanChassis(isMuscle = false)
    policeMesh = MeshFactory.createPoliceChassis()
    sportBikeMesh = MeshFactory.createMotorbikeMesh(isDirtBike = false)
    dirtBikeMesh = MeshFactory.createMotorbikeMesh(isDirtBike = true)
    carWheelMesh = MeshFactory.createWheelMesh(radius = 0.38f, width = 0.28f)
    bikeWheelMesh = MeshFactory.createWheelMesh(radius = 0.34f, width = 0.17f)
    shadowMesh = MeshFactory.createShadowDiscMesh()
    pickupMesh = MeshFactory.createPickupMesh()
    checkpointMesh = MeshFactory.createCheckpointMesh()
    unitBoxBody = MeshFactory.createUnitBoxMesh(0.92f, 0.28f, 0.22f)
    unitBoxSkin = MeshFactory.createUnitBoxMesh(0.84f, 0.64f, 0.50f)
    unitBoxDark = MeshFactory.createUnitBoxMesh(0.14f, 0.16f, 0.22f)
    unitBoxFlame = MeshFactory.createUnitBoxMesh(1.0f, 0.56f, 0.12f, matType = 3.0f)
    unitBoxSkid = MeshFactory.createUnitBoxMesh(0.05f, 0.05f, 0.06f)

    lastFrameNs = System.nanoTime()
  }

  override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    GLES30.glViewport(0, 0, width, height)
    aspectRatio = (width.toFloat() / maxOf(height, 1)).coerceAtLeast(0.4f)
  }

  override fun onDrawFrame(gl: GL10?) {
    while (true) {
      val act = pendingActions.poll() ?: break
      act()
    }

    val now = System.nanoTime()
    val rawDt = ((now - lastFrameNs) / 1_000_000_000.0).toFloat().coerceIn(0.001f, 0.05f)
    lastFrameNs = now

    if (!isPaused) {
      simulation.step(rawDt)
      simTimeSec += rawDt
    }

    // Update FPS & Audio & HUD
    fpsCounter++
    fpsAccum += rawDt
    if (fpsAccum >= 0.5f) {
      currentFps = (fpsCounter / fpsAccum).toInt().coerceIn(1, 120)
      fpsCounter = 0
      fpsAccum = 0f
    }

    val curVeh = simulation.activeVehicle()
    if (!isPaused && curVeh != null) {
      val ratio = abs(curVeh.forwardSpeed) / curVeh.model.maxSpeedMps
      val rpm = ((ratio * 6f) % 1f).coerceIn(0.15f, 1f) * 0.65f + ratio * 0.35f
      soundEngine.updateTelemetry(
        inVehicle = true,
        isBike = curVeh.model.isBike,
        rpmNorm = rpm,
        baseHz = curVeh.model.engineBaseHz,
        drifting = curVeh.isDrifting,
        sirenActive = simulation.wantedStars > 0,
      )
    } else {
      soundEngine.updateTelemetry(
        inVehicle = false,
        isBike = false,
        rpmNorm = 0f,
        baseHz = 50f,
        drifting = false,
        sirenActive = !isPaused && simulation.wantedStars > 0,
      )
    }

    hudAccumSec += rawDt
    if (hudAccumSec >= 0.045f) {
      hudAccumSec = 0f
      _hudState.value = simulation.buildHudSnapshot(currentFps)
    }

    updateCamera(rawDt)

    // Render Scene
    GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
    drawSkyDome()
    drawGroundAndOcean()
    drawCityWorld()
  }

  private fun updateCamera(dt: Float) {
    val active = simulation.activeVehicle()
    val targetX = active?.x ?: simulation.playerX
    val targetY = active?.y ?: simulation.playerY
    val targetZ = active?.z ?: simulation.playerZ
    val yaw = active?.yawRad ?: simulation.playerYaw
    val speed = active?.let { abs(it.forwardSpeed) } ?: 0f

    val fx = sin(yaw)
    val fz = cos(yaw)

    val fov = (58f + (speed / 65f) * 14f).coerceIn(56f, 74f)
    Matrix.perspectiveM(projMatrix, 0, fov, aspectRatio, 0.35f, 540f)

    if (active != null && simulation.cameraMode == CameraMode.BUMPER) {
      val eyeH = if (active.model.isBike) 1.52f else 1.28f
      val fwdOff = if (active.model.isBike) 0.35f else 0.75f
      camX = targetX + fx * fwdOff
      camY = targetY + eyeH
      camZ = targetZ + fz * fwdOff
      lookX = targetX + fx * 22f
      lookY = targetY + 1.0f
      lookZ = targetZ + fz * 22f
    } else {
      val backDist =
        when {
          active == null -> 5.2f
          simulation.cameraMode == CameraMode.CINEMATIC_LOW -> if (active.model.isBike) 4.6f else 6.4f
          else -> if (active.model.isBike) 5.6f else 8.2f
        } + (speed * 0.035f)
      val heightOff =
        when {
          active == null -> 2.4f
          simulation.cameraMode == CameraMode.CINEMATIC_LOW -> 1.85f
          else -> if (active.model.isBike) 2.55f else 3.25f
        }

      val desiredCamX = targetX - fx * backDist
      val desiredCamY = targetY + heightOff
      val desiredCamZ = targetZ - fz * backDist

      val smooth = (10f * dt).coerceIn(0.05f, 1f)
      camX += (desiredCamX - camX) * smooth
      camY += (desiredCamY - camY) * smooth
      camZ += (desiredCamZ - camZ) * smooth

      val lookAhead = if (active != null) 4.5f else 2.0f
      val desiredLookX = targetX + fx * lookAhead
      val desiredLookY = targetY + 1.15f
      val desiredLookZ = targetZ + fz * lookAhead
      lookX += (desiredLookX - lookX) * (smooth * 1.3f).coerceAtMost(1f)
      lookY += (desiredLookY - lookY) * (smooth * 1.3f).coerceAtMost(1f)
      lookZ += (desiredLookZ - lookZ) * (smooth * 1.3f).coerceAtMost(1f)
    }

    Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, lookX, lookY, lookZ, 0f, 1f, 0f)
    Matrix.multiplyMM(viewProjMatrix, 0, projMatrix, 0, viewMatrix, 0)
    Matrix.invertM(invViewProjMatrix, 0, viewProjMatrix, 0)
  }

  private fun drawSkyDome() {
    val tod = simulation.timeOfDay
    GLES30.glDepthMask(false)
    skyShader.use()
    GLES30.glUniformMatrix4fv(skyShader.uniform("uInvViewProj"), 1, false, invViewProjMatrix, 0)
    GLES30.glUniform3f(skyShader.uniform("uCamPos"), camX, camY, camZ)
    GLES30.glUniform3fv(skyShader.uniform("uSunDir"), 1, tod.sunDir, 0)
    GLES30.glUniform3fv(skyShader.uniform("uSunColor"), 1, tod.sunColor, 0)
    GLES30.glUniform3fv(skyShader.uniform("uZenithColor"), 1, tod.skyZenith, 0)
    GLES30.glUniform3fv(skyShader.uniform("uHorizonColor"), 1, tod.skyHorizon, 0)
    skyQuad.draw()
    GLES30.glDepthMask(true)
  }

  private fun drawGroundAndOcean() {
    val tod = simulation.timeOfDay
    val curVeh = simulation.activeVehicle()
    val headOn = if (curVeh != null && curVeh.headlightsOn) 1f else 0f
    val headYaw = curVeh?.yawRad ?: simulation.playerYaw
    val headX = (curVeh?.x ?: simulation.playerX) + sin(headYaw) * 2.0f
    val headY = (curVeh?.y ?: simulation.playerY) + 0.9f
    val headZ = (curVeh?.z ?: simulation.playerZ) + cos(headYaw) * 2.0f

    groundShader.use()
    MatrixUtils.setIdentity(modelMatrix)
    GLES30.glUniformMatrix4fv(groundShader.uniform("uModel"), 1, false, modelMatrix, 0)
    GLES30.glUniformMatrix4fv(groundShader.uniform("uMvp"), 1, false, viewProjMatrix, 0)
    GLES30.glUniform3f(groundShader.uniform("uCamPos"), camX, camY, camZ)
    GLES30.glUniform3fv(groundShader.uniform("uSunDir"), 1, tod.sunDir, 0)
    GLES30.glUniform3fv(groundShader.uniform("uSunColor"), 1, tod.sunColor, 0)
    GLES30.glUniform3fv(groundShader.uniform("uFogColor"), 1, tod.fogColor, 0)
    GLES30.glUniform1f(groundShader.uniform("uAmbient"), tod.ambientStrength)
    GLES30.glUniform1f(groundShader.uniform("uTime"), simTimeSec)
    GLES30.glUniform3f(groundShader.uniform("uHeadlightPos"), headX, headY, headZ)
    GLES30.glUniform3f(groundShader.uniform("uHeadlightDir"), sin(headYaw), -0.18f, cos(headYaw))
    GLES30.glUniform1f(groundShader.uniform("uHeadlightOn"), headOn)

    groundPlane.draw()
    oceanPlane.draw()
  }

  private fun drawCityWorld() {
    val tod = simulation.timeOfDay
    litShader.use()
    GLES30.glUniform3f(litShader.uniform("uCamPos"), camX, camY, camZ)
    GLES30.glUniform3fv(litShader.uniform("uSunDir"), 1, tod.sunDir, 0)
    GLES30.glUniform3fv(litShader.uniform("uSunColor"), 1, tod.sunColor, 0)
    GLES30.glUniform3fv(litShader.uniform("uFogColor"), 1, tod.fogColor, 0)
    GLES30.glUniform3fv(litShader.uniform("uSkyZenith"), 1, tod.skyZenith, 0)
    GLES30.glUniform1f(litShader.uniform("uAmbient"), tod.ambientStrength)
    GLES30.glUniform1f(litShader.uniform("uWindowGlow"), tod.windowGlow)
    GLES30.glUniform1f(litShader.uniform("uHeadlightOn"), if (simulation.headlightsOn) 1f else 0f)
    GLES30.glUniform3f(litShader.uniform("uEmissiveTint"), 0f, 0f, 0f)

    // 1. Buildings (two architectural tiers per building!)
    val useOverrideU = litShader.uniform("uUsePaintOverride")
    val paintOverrideU = litShader.uniform("uPaintOverride")

    GLES30.glUniform1f(useOverrideU, 0f)
    for (b in WorldMap.buildings) {
      // Lower podium/tower
      MatrixUtils.setIdentity(modelMatrix)
      MatrixUtils.translate(modelMatrix, b.centerX, 0f, b.centerZ)
      MatrixUtils.scale(modelMatrix, b.widthX, b.tierHeight, b.depthZ)
      drawWithModel(buildingMesh)

      // Upper setback spire
      if (b.height > b.tierHeight + 2f) {
        MatrixUtils.setIdentity(modelMatrix)
        MatrixUtils.translate(modelMatrix, b.centerX, b.tierHeight, b.centerZ)
        MatrixUtils.scale(modelMatrix, b.widthX * 0.72f, b.height - b.tierHeight, b.depthZ * 0.72f)
        drawWithModel(buildingMesh)
      }
    }

    // 2. Stunt Ramps
    for (ramp in WorldMap.ramps) {
      MatrixUtils.setIdentity(modelMatrix)
      MatrixUtils.translate(modelMatrix, ramp.x, 0f, ramp.z)
      MatrixUtils.rotateYDeg(modelMatrix, MatrixUtils.radToDeg(ramp.yawRad))
      MatrixUtils.scale(modelMatrix, ramp.width, ramp.height, ramp.length)
      drawWithModel(rampMesh)
    }

    // 3. Palm Trees
    for (palm in WorldMap.palms) {
      MatrixUtils.setIdentity(modelMatrix)
      MatrixUtils.translate(modelMatrix, palm.x, 0f, palm.z)
      MatrixUtils.rotateYDeg(modelMatrix, MatrixUtils.radToDeg(palm.rotY))
      MatrixUtils.scale(modelMatrix, palm.height, palm.height, palm.height)
      drawWithModel(palmMesh)
    }

    // 4. Street Lamps
    for (lamp in WorldMap.streetLamps) {
      val dx = lamp.x - camX
      val dz = lamp.z - camZ
      if (dx * dx + dz * dz > 280f * 280f) continue
      MatrixUtils.setIdentity(modelMatrix)
      MatrixUtils.translate(modelMatrix, lamp.x, 0f, lamp.z)
      MatrixUtils.rotateYDeg(modelMatrix, MatrixUtils.radToDeg(lamp.rotY))
      drawWithModel(lampMesh)
    }

    // 5. Cash Pickups (floating & spinning)
    for (i in WorldMap.pickups.indices) {
      if (simulation.pickupCooldowns[i] > 0f) continue
      val p = WorldMap.pickups[i]
      val bob = sin(simTimeSec * 3.5f + i) * 0.25f + 1.0f
      MatrixUtils.setIdentity(modelMatrix)
      MatrixUtils.translate(modelMatrix, p.x, bob, p.z)
      MatrixUtils.rotateYDeg(modelMatrix, simTimeSec * 95f)
      drawWithModel(pickupMesh)
    }

    // 6. Active Time Trial Checkpoint
    if (simulation.timeTrialActive) {
      val cp = WorldMap.checkpoints[simulation.activeCheckpointIdx]
      MatrixUtils.setIdentity(modelMatrix)
      MatrixUtils.translate(modelMatrix, cp.x, 0f, cp.z)
      drawWithModel(checkpointMesh)
    }

    // 7. Tire Skidmarks
    for (skid in simulation.skidMarks) {
      val dx = skid.x2 - skid.x1
      val dz = skid.z2 - skid.z1
      val len = kotlin.math.hypot(dx, dz).coerceAtLeast(0.15f)
      val yaw = kotlin.math.atan2(dx, dz)
      MatrixUtils.setIdentity(modelMatrix)
      MatrixUtils.translate(modelMatrix, (skid.x1 + skid.x2) * 0.5f, skid.y1, (skid.z1 + skid.z2) * 0.5f)
      MatrixUtils.rotateYDeg(modelMatrix, MatrixUtils.radToDeg(yaw))
      MatrixUtils.scale(modelMatrix, 0.24f, 0.02f, len)
      drawWithModel(unitBoxSkid)
    }

    // 8. Vehicles (Cars & Motorbikes + Contact Shadows + Wheels + Police Lightbars)
    for (v in simulation.vehicles) {
      drawVehicle(v, useOverrideU, paintOverrideU)
    }

    // 9. On-Foot Player Character
    if (simulation.activeVehicleId == null) {
      GLES30.glUniform1f(useOverrideU, 0f)
      drawPedestrian(simulation.playerX, simulation.playerY, simulation.playerZ, simulation.playerYaw, simulation.playerWalkCycle)
    }
  }

  private fun drawVehicle(v: VehicleEntity, useOverrideU: Int, paintOverrideU: Int) {
    val spec = v.model
    val surfY = WorldMap.getSurfaceHeight(v.x, v.z)

    // Grounded Contact Shadow
    GLES30.glUniform1f(useOverrideU, 0f)
    MatrixUtils.setIdentity(modelMatrix)
    MatrixUtils.translate(modelMatrix, v.x, surfY + 0.02f, v.z)
    MatrixUtils.rotateYDeg(modelMatrix, MatrixUtils.radToDeg(v.yawRad))
    val shadowScale = (1.0f - (v.y - surfY) * 0.08f).coerceIn(0.45f, 1.0f)
    MatrixUtils.scale(modelMatrix, spec.width * 1.25f * shadowScale, 1f, spec.length * 1.15f * shadowScale)
    drawWithModel(shadowMesh)

    // Vehicle Body Matrix (position + yaw + lean/pitch/roll)
    MatrixUtils.setIdentity(modelMatrix)
    MatrixUtils.translate(modelMatrix, v.x, v.y, v.z)
    MatrixUtils.rotateYDeg(modelMatrix, MatrixUtils.radToDeg(v.yawRad))
    if (spec.isBike) {
      MatrixUtils.rotateZDeg(modelMatrix, MatrixUtils.radToDeg(v.leanAngle))
      MatrixUtils.rotateXDeg(modelMatrix, MatrixUtils.radToDeg(v.wheeliePitch))
    } else {
      MatrixUtils.rotateXDeg(modelMatrix, MatrixUtils.radToDeg(v.bodyPitch))
      MatrixUtils.rotateZDeg(modelMatrix, MatrixUtils.radToDeg(v.bodyRoll))
    }

    if (spec.isPolice) {
      GLES30.glUniform1f(useOverrideU, 0f)
    } else {
      GLES30.glUniform1f(useOverrideU, 1f)
      GLES30.glUniform3f(paintOverrideU, v.paintR, v.paintG, v.paintB)
    }

    val chassisMesh =
      when (spec) {
        VehicleModelType.INFERNUS_GT -> supercarMesh
        VehicleModelType.ZENTORNO_RS -> hypercarMesh
        VehicleModelType.BANSHEE_V8 -> muscleMesh
        VehicleModelType.VICE_PD -> policeMesh
        VehicleModelType.URBAN_SEDAN -> sedanMesh
        VehicleModelType.AKUMA_1000R, VehicleModelType.BATI_801RR -> sportBikeMesh
        VehicleModelType.SANCHEZ_MX -> dirtBikeMesh
      }
    drawWithModel(chassisMesh)

    // Police flashing roof lightbar
    if (spec.isPolice) {
      val sirenBlink = ((simTimeSec * 7f).toInt() % 2 == 0) && simulation.wantedStars > 0
      val emissiveU = litShader.uniform("uEmissiveTint")
      GLES30.glUniform1f(useOverrideU, 0f)
      MatrixUtils.copy(modelMatrix, wheelMatrix)

      // Red left beacon
      MatrixUtils.setIdentity(modelMatrix)
      MatrixUtils.translate(modelMatrix, v.x, v.y, v.z)
      MatrixUtils.rotateYDeg(modelMatrix, MatrixUtils.radToDeg(v.yawRad))
      MatrixUtils.translate(modelMatrix, -0.32f, 1.42f, -0.15f)
      MatrixUtils.scale(modelMatrix, 0.56f, 0.14f, 0.24f)
      if (sirenBlink) GLES30.glUniform3f(emissiveU, 0.9f, 0.0f, 0.0f)
      else GLES30.glUniform3f(emissiveU, 0.2f, 0.0f, 0.0f)
      drawWithModel(unitBoxFlame)

      // Blue right beacon
      MatrixUtils.setIdentity(modelMatrix)
      MatrixUtils.translate(modelMatrix, v.x, v.y, v.z)
      MatrixUtils.rotateYDeg(modelMatrix, MatrixUtils.radToDeg(v.yawRad))
      MatrixUtils.translate(modelMatrix, 0.32f, 1.42f, -0.15f)
      MatrixUtils.scale(modelMatrix, 0.56f, 0.14f, 0.24f)
      if (!sirenBlink && simulation.wantedStars > 0) GLES30.glUniform3f(emissiveU, 0.0f, 0.3f, 1.0f)
      else GLES30.glUniform3f(emissiveU, 0.0f, 0.08f, 0.28f)
      drawWithModel(unitBoxFlame)

      GLES30.glUniform3f(emissiveU, 0f, 0f, 0f)
    }

    // NOS Exhaust Flame when boosting
    if (v.nosActive) {
      val flameScale = 0.7f + sin(simTimeSec * 45f) * 0.25f
      GLES30.glUniform1f(useOverrideU, 0f)
      MatrixUtils.setIdentity(modelMatrix)
      MatrixUtils.translate(modelMatrix, v.x, v.y, v.z)
      MatrixUtils.rotateYDeg(modelMatrix, MatrixUtils.radToDeg(v.yawRad))
      val exZ = if (spec.isBike) -1.08f else -2.35f
      val exY = if (spec.isBike) 0.40f else 0.34f
      MatrixUtils.translate(modelMatrix, 0f, exY, exZ)
      MatrixUtils.scale(modelMatrix, 0.28f, 0.22f, 0.75f * flameScale)
      drawWithModel(unitBoxFlame)
    }

    // Wheels (4 wheels for cars, 2 wheels for motorbikes)
    GLES30.glUniform1f(useOverrideU, 0f)
    val spinDeg = MatrixUtils.radToDeg(v.wheelSpinRad)
    val steerDeg = MatrixUtils.radToDeg(-v.steerAngle)

    if (spec.isBike) {
      drawSingleWheel(v, 0f, 0.34f, 0.78f, steerDeg, spinDeg, bikeWheelMesh)
      drawSingleWheel(v, 0f, 0.34f, -0.78f, 0f, spinDeg, bikeWheelMesh)
    } else {
      val xOff = spec.width * 0.45f
      val zOff = spec.length * 0.31f
      drawSingleWheel(v, -xOff, 0.38f, zOff, steerDeg, spinDeg, carWheelMesh)
      drawSingleWheel(v, xOff, 0.38f, zOff, steerDeg, spinDeg, carWheelMesh)
      drawSingleWheel(v, -xOff, 0.38f, -zOff, 0f, spinDeg, carWheelMesh)
      drawSingleWheel(v, xOff, 0.38f, -zOff, 0f, spinDeg, carWheelMesh)
    }
  }

  private fun drawSingleWheel(
    v: VehicleEntity,
    lx: Float,
    ly: Float,
    lz: Float,
    steerDeg: Float,
    spinDeg: Float,
    mesh: GlMesh,
  ) {
    MatrixUtils.setIdentity(modelMatrix)
    MatrixUtils.translate(modelMatrix, v.x, v.y, v.z)
    MatrixUtils.rotateYDeg(modelMatrix, MatrixUtils.radToDeg(v.yawRad))
    if (v.model.isBike) {
      MatrixUtils.rotateZDeg(modelMatrix, MatrixUtils.radToDeg(v.leanAngle))
      MatrixUtils.rotateXDeg(modelMatrix, MatrixUtils.radToDeg(v.wheeliePitch))
    } else {
      MatrixUtils.rotateXDeg(modelMatrix, MatrixUtils.radToDeg(v.bodyPitch))
      MatrixUtils.rotateZDeg(modelMatrix, MatrixUtils.radToDeg(v.bodyRoll))
    }
    MatrixUtils.translate(modelMatrix, lx, ly, lz)
    if (steerDeg != 0f) MatrixUtils.rotateYDeg(modelMatrix, steerDeg)
    MatrixUtils.rotateXDeg(modelMatrix, spinDeg)
    drawWithModel(mesh)
  }

  private fun drawPedestrian(x: Float, y: Float, z: Float, yaw: Float, walkCycle: Float) {
    val swing = sin(walkCycle) * 28f
    val yawDeg = MatrixUtils.radToDeg(yaw)

    // Shadow
    MatrixUtils.setIdentity(modelMatrix)
    MatrixUtils.translate(modelMatrix, x, y + 0.02f, z)
    MatrixUtils.scale(modelMatrix, 0.9f, 1f, 0.9f)
    drawWithModel(shadowMesh)

    // Torso (sunset jacket)
    MatrixUtils.setIdentity(modelMatrix)
    MatrixUtils.translate(modelMatrix, x, y + 1.12f, z)
    MatrixUtils.rotateYDeg(modelMatrix, yawDeg)
    MatrixUtils.scale(modelMatrix, 0.48f, 0.62f, 0.26f)
    drawWithModel(unitBoxBody)

    // Head
    MatrixUtils.setIdentity(modelMatrix)
    MatrixUtils.translate(modelMatrix, x, y + 1.62f, z)
    MatrixUtils.rotateYDeg(modelMatrix, yawDeg)
    MatrixUtils.scale(modelMatrix, 0.28f, 0.28f, 0.28f)
    drawWithModel(unitBoxSkin)

    // Sunglasses
    MatrixUtils.setIdentity(modelMatrix)
    MatrixUtils.translate(modelMatrix, x + sin(yaw) * 0.14f, y + 1.64f, z + cos(yaw) * 0.14f)
    MatrixUtils.rotateYDeg(modelMatrix, yawDeg)
    MatrixUtils.scale(modelMatrix, 0.24f, 0.08f, 0.06f)
    drawWithModel(unitBoxDark)

    // Left & Right Legs
    drawLimb(x, y, z, yawDeg, -0.13f, 0.44f, 0.17f, 0.78f, 0.19f, swing, unitBoxDark)
    drawLimb(x, y, z, yawDeg, 0.13f, 0.44f, 0.17f, 0.78f, 0.19f, -swing, unitBoxDark)

    // Left & Right Arms
    drawLimb(x, y, z, yawDeg, -0.32f, 1.12f, 0.15f, 0.56f, 0.15f, -swing * 0.8f, unitBoxSkin)
    drawLimb(x, y, z, yawDeg, 0.32f, 1.12f, 0.15f, 0.56f, 0.15f, swing * 0.8f, unitBoxSkin)
  }

  private fun drawLimb(
    px: Float, py: Float, pz: Float, yawDeg: Float,
    lx: Float, ly: Float, sx: Float, sy: Float, sz: Float,
    swingDeg: Float, mesh: GlMesh,
  ) {
    MatrixUtils.setIdentity(modelMatrix)
    MatrixUtils.translate(modelMatrix, px, py + ly, pz)
    MatrixUtils.rotateYDeg(modelMatrix, yawDeg)
    MatrixUtils.translate(modelMatrix, lx, 0f, 0f)
    MatrixUtils.rotateXDeg(modelMatrix, swingDeg)
    MatrixUtils.scale(modelMatrix, sx, sy, sz)
    drawWithModel(mesh)
  }

  private fun drawWithModel(mesh: GlMesh) {
    Matrix.multiplyMM(mvpMatrix, 0, viewProjMatrix, 0, modelMatrix, 0)
    GLES30.glUniformMatrix4fv(litShader.uniform("uModel"), 1, false, modelMatrix, 0)
    GLES30.glUniformMatrix4fv(litShader.uniform("uMvp"), 1, false, mvpMatrix, 0)
    mesh.draw()
  }
}
