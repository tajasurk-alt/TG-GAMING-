package com.example.game.sim

import com.example.game.model.CameraMode
import com.example.game.model.ControlState
import com.example.game.model.GameMode
import com.example.game.model.HudSnapshot
import com.example.game.model.MinimapBlip
import com.example.game.model.PaintPreset
import com.example.game.model.TimeOfDay
import com.example.game.model.VehicleEntity
import com.example.game.model.VehicleModelType
import com.example.game.world.WorldMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

data class SkidMark(
  val x1: Float,
  val y1: Float,
  val z1: Float,
  val x2: Float,
  val y2: Float,
  val z2: Float,
  var life: Float,
)

class GameSimulation(
  val controls: ControlState = ControlState(),
) {
  var timeOfDay: TimeOfDay = TimeOfDay.SUNSET
  var cameraMode: CameraMode = CameraMode.CHASE
  var gameMode: GameMode = GameMode.FREE_ROAM
  var preferredPaint: PaintPreset = PaintPreset.SUNSET_CRIMSON

  // Player on-foot state
  var playerX = 114f
  var playerY = 0f
  var playerZ = 18f
  var playerVy = 0f
  var playerYaw = PI.toFloat() // Facing South toward parked rides
  var playerWalkCycle = 0f
  var playerGrounded = true

  // Active vehicle
  var activeVehicleId: Int? = null
  val vehicles = mutableListOf<VehicleEntity>()
  private var nextVehicleId = 1

  // Economy & progression
  var cash = 2500
  var wantedStars = 0
  var wantedCooldown = 0f
  var bustedTimer = 0f
  var nitroAmount = 1f
  var headlightsOn = true

  // Airborne stunt tracking
  private var airborneTime = 0f
  private var launchX = 0f
  private var launchZ = 0f

  // Pickups & Time Trial
  val pickupCooldowns = FloatArray(WorldMap.pickups.size)
  var activeCheckpointIdx = 0
  var timeTrialActive = false
  var timeTrialSeconds = 0f
  var bestLapSeconds: Float? = null

  // Banner message
  private var bannerText: String? = null
  private var bannerTimer = 0f

  // Skidmarks
  val skidMarks = ArrayDeque<SkidMark>(180)

  // Events callback for audio chime
  var onChimeEvent: (() -> Unit)? = null

  init {
    resetWorld(GameMode.FREE_ROAM, startMounted = VehicleModelType.INFERNUS_GT)
  }

  fun resetWorld(mode: GameMode, startMounted: VehicleModelType? = null) {
    gameMode = mode
    vehicles.clear()
    skidMarks.clear()
    nextVehicleId = 1

    playerX = 114f
    playerY = 0f
    playerZ = 14f
    playerVy = 0f
    playerYaw = PI.toFloat()
    playerGrounded = true
    activeVehicleId = null
    wantedStars = 0
    wantedCooldown = 0f
    bustedTimer = 0f
    nitroAmount = 1f
    airborneTime = 0f
    pickupCooldowns.fill(0f)

    // Spawn signature player rides on Ocean Boulevard!
    val supercar =
      spawnVehicle(
        model = VehicleModelType.INFERNUS_GT,
        x = 110.5f,
        z = 4f,
        yawRad = PI.toFloat(),
        paint = preferredPaint,
      )
    spawnVehicle(
      model = VehicleModelType.AKUMA_1000R,
      x = 117.5f,
      z = 4f,
      yawRad = PI.toFloat(),
      paint = PaintPreset.ELECTRIC_CYAN,
    )
    spawnVehicle(
      model = VehicleModelType.BANSHEE_V8,
      x = 110.5f,
      z = -24f,
      yawRad = 0f,
      paint = PaintPreset.PEARL_GOLD,
    )
    spawnVehicle(
      model = VehicleModelType.SANCHEZ_MX,
      x = -60f,
      z = 55f,
      yawRad = 0f,
      paint = PaintPreset.EMERALD_VIPER,
    )
    spawnVehicle(
      model = VehicleModelType.AKUMA_1000R,
      x = -54f,
      z = 55f,
      yawRad = 0f,
      paint = PaintPreset.SUNSET_CRIMSON,
    )

    // Spawn lively AI traffic cars & motorbikes on avenues
    val trafficSpecs =
      listOf(
        Triple(VehicleModelType.URBAN_SEDAN, 0f, -80f),
        Triple(VehicleModelType.AKUMA_1000R, 0f, 120f),
        Triple(VehicleModelType.BANSHEE_V8, -120f, -150f),
        Triple(VehicleModelType.URBAN_SEDAN, -120f, 60f),
        Triple(VehicleModelType.SANCHEZ_MX, 114f, -130f),
        Triple(VehicleModelType.URBAN_SEDAN, -240f, 30f),
        Triple(VehicleModelType.VICE_PD, 0f, 0f),
        Triple(VehicleModelType.VICE_PD, -120f, -110f),
      )
    val paints = PaintPreset.entries
    trafficSpecs.forEachIndexed { idx, (spec, tx, tz) ->
      val v =
        spawnVehicle(
          model = spec,
          x = tx,
          z = tz,
          yawRad = if (idx % 2 == 0) 0f else PI.toFloat(),
          paint = paints[(idx + 2) % paints.size],
        )
      v.isAiTraffic = true
      v.aiTargetYaw = v.yawRad
      v.aiSpeedTarget = if (spec.isPolice) 16f else 13f + (idx % 4) * 2.5f
    }

    if (mode == GameMode.TIME_TRIAL) {
      timeTrialActive = true
      timeTrialSeconds = 0f
      activeCheckpointIdx = 0
      val chosen = startMounted ?: VehicleModelType.INFERNUS_GT
      val ride = vehicles.firstOrNull { it.model == chosen } ?: supercar
      mountVehicle(ride)
      showBanner("COASTAL SPRINT STARTED! HIT ALL 12 CHECKPOINTS")
    } else {
      timeTrialActive = false
      timeTrialSeconds = 0f
      activeCheckpointIdx = 0
      if (startMounted != null) {
        val ride = vehicles.firstOrNull { it.model == startMounted } ?: supercar
        mountVehicle(ride)
      }
      showBanner("WELCOME TO VICE COAST — TAP ANY RIDE OR CRUISE!")
    }
  }

  fun spawnAndMountPlayerVehicle(model: VehicleModelType, paint: PaintPreset) {
    preferredPaint = paint
    val current = activeVehicle()
    if (current != null) {
      // Seamless in-place vehicle swap preserving location, heading, and momentum
      val keptSpeed = current.forwardSpeed.coerceIn(-12f, model.maxSpeedMps)
      val rx = current.x
      val rz = current.z
      val ryaw = current.yawRad
      val rvx = sin(ryaw) * keptSpeed
      val rvz = cos(ryaw) * keptSpeed
      vehicles.removeAll { it.id == current.id }
      val next = spawnVehicle(model, rx, rz, ryaw, paint)
      next.forwardSpeed = keptSpeed
      next.vx = rvx
      next.vz = rvz
      mountVehicle(next)
      showBanner("SWITCHED TO ${model.displayName.uppercase()}")
      onChimeEvent?.invoke()
      return
    }

    val refX = playerX
    val refZ = playerZ
    val refYaw = playerYaw

    // Find safe road or clear spot nearby
    val spawnX = (refX + sin(refYaw) * 2.5f).coerceIn(-290f, 155f)
    val spawnZ = (refZ + cos(refYaw) * 2.5f).coerceIn(-290f, 290f)
    val safePos = WorldMap.resolveBuildingCollision(spawnX, spawnZ, 2.5f)
    val finalX = safePos?.first ?: spawnX
    val finalZ = safePos?.second ?: spawnZ

    // Remove old custom spawned excess vehicles to keep world snappy
    if (vehicles.size > 16) {
      vehicles.removeAll { it.id != activeVehicleId && !it.isAiTraffic }
    }

    val v = spawnVehicle(model, finalX, finalZ, refYaw, paint)
    mountVehicle(v)
    showBanner("DELIVERED: ${model.displayName.uppercase()}")
    onChimeEvent?.invoke()
  }

  private fun spawnVehicle(
    model: VehicleModelType,
    x: Float,
    z: Float,
    yawRad: Float,
    paint: PaintPreset,
  ): VehicleEntity {
    val isCop = model.isPolice
    val v =
      VehicleEntity(
        id = nextVehicleId++,
        model = model,
        x = x,
        y = WorldMap.getSurfaceHeight(x, z),
        z = z,
        yawRad = yawRad,
        paintR = if (isCop) 0.12f else paint.r,
        paintG = if (isCop) 0.14f else paint.g,
        paintB = if (isCop) 0.18f else paint.b,
        isPolice = isCop,
        headlightsOn = timeOfDay.autoHeadlights || headlightsOn,
      )
    vehicles.add(v)
    return v
  }

  fun activeVehicle(): VehicleEntity? =
    activeVehicleId?.let { id -> vehicles.firstOrNull { it.id == id } }

  fun toggleEnterExitVehicle() {
    val current = activeVehicle()
    if (current != null) {
      // Exit vehicle to left side
      val exitYaw = current.yawRad - (PI / 2).toFloat()
      val sideOffset = if (current.model.isBike) 1.5f else 2.2f
      playerX = current.x + sin(exitYaw) * sideOffset
      playerZ = current.z + cos(exitYaw) * sideOffset
      playerY = WorldMap.getSurfaceHeight(playerX, playerZ)
      playerVy = 0f
      playerYaw = current.yawRad
      activeVehicleId = null
      current.isDrifting = false
      current.nosActive = false
      return
    }

    // Find nearest vehicle within 5.5m
    val nearest = findNearestEnterableVehicle(5.8f)
    if (nearest != null) {
      if (nearest.isAiTraffic) {
        nearest.isAiTraffic = false
        addWantedStars(if (nearest.isPolice) 2 else 1)
        showBanner(
          if (nearest.isPolice) "HIJACKED VICE PD CRUISER! WANTED!"
          else "COMMANDEERED ${nearest.model.displayName.uppercase()}"
        )
      }
      mountVehicle(nearest)
      onChimeEvent?.invoke()
    }
  }

  private fun mountVehicle(v: VehicleEntity) {
    activeVehicleId = v.id
    v.isAiTraffic = false
    v.headlightsOn = headlightsOn
    airborneTime = 0f
  }

  fun findNearestEnterableVehicle(maxDist: Float): VehicleEntity? {
    var best: VehicleEntity? = null
    var bestDistSq = maxDist * maxDist
    for (v in vehicles) {
      if (v.id == activeVehicleId) continue
      val dx = v.x - playerX
      val dz = v.z - playerZ
      val dSq = dx * dx + dz * dz
      if (dSq < bestDistSq) {
        bestDistSq = dSq
        best = v
      }
    }
    return best
  }

  fun toggleHeadlights() {
    headlightsOn = !headlightsOn
    activeVehicle()?.headlightsOn = headlightsOn
  }

  fun cycleCamera() {
    val entries = CameraMode.entries
    cameraMode = entries[(cameraMode.ordinal + 1) % entries.size]
  }

  fun applyPaint(paint: PaintPreset) {
    preferredPaint = paint
    val v = activeVehicle()
    if (v != null && !v.isPolice) {
      v.paintR = paint.r
      v.paintG = paint.g
      v.paintB = paint.b
      showBanner("RESPRAYED: ${paint.label.uppercase()}")
      onChimeEvent?.invoke()
    }
  }

  fun step(rawDt: Float) {
    val dt = rawDt.coerceIn(0.001f, 0.05f)

    if (bannerTimer > 0f) {
      bannerTimer -= dt
      if (bannerTimer <= 0f) bannerText = null
    }

    for (i in pickupCooldowns.indices) {
      if (pickupCooldowns[i] > 0f) pickupCooldowns[i] = (pickupCooldowns[i] - dt).coerceAtLeast(0f)
    }

    // Fade skidmarks
    val skidIter = skidMarks.iterator()
    while (skidIter.hasNext()) {
      val s = skidIter.next()
      s.life -= dt
      if (s.life <= 0f) skidIter.remove()
    }

    // Step player or active vehicle
    val curVeh = activeVehicle()
    if (curVeh != null) {
      stepPlayerVehicle(curVeh, dt)
      playerX = curVeh.x
      playerY = curVeh.y
      playerZ = curVeh.z
      playerYaw = curVeh.yawRad
    } else {
      stepPlayerOnFoot(dt)
    }

    // Step all other vehicles (parked + AI traffic + AI police)
    for (v in vehicles) {
      if (v.id == activeVehicleId) continue
      stepOtherVehicle(v, dt)
    }

    // Vehicle-to-vehicle collisions
    resolveVehicleCollisions()

    // Check pickups & checkpoints
    checkWorldInteractions(dt)

    // Wanted / Police chase logic
    updatePoliceSystem(dt)
  }

  private fun stepPlayerOnFoot(dt: Float) {
    val jx = controls.walkJoyX
    val jy = controls.walkJoyY
    val moveMag = hypot(jx, jy).coerceAtMost(1f)
    val speed = if (controls.sprint) 9.8f else 5.8f

    if (moveMag > 0.08f) {
      // Joystick Y negative is forward (up on screen), +X is right
      // Camera looks along playerYaw (or fixed chase offset), move relative to world/facing
      val desiredAngle = atan2(jx, -jy)
      playerYaw = lerpAngle(playerYaw, desiredAngle, (10f * dt).coerceAtMost(1f))
      playerX += sin(playerYaw) * moveMag * speed * dt
      playerZ += cos(playerYaw) * moveMag * speed * dt
      playerWalkCycle += dt * (if (controls.sprint) 13f else 8.5f)
    } else {
      playerWalkCycle = 0f
    }

    if (controls.wheelieOrJump && playerGrounded) {
      playerVy = 7.4f
      playerGrounded = false
    }

    playerVy -= 21f * dt
    playerY += playerVy * dt
    val floorY = WorldMap.getSurfaceHeight(playerX, playerZ)
    if (playerY <= floorY) {
      playerY = floorY
      playerVy = 0f
      playerGrounded = true
    }

    // World bounds & building collision
    WorldMap.resolveBuildingCollision(playerX, playerZ, 0.65f)?.let { (cx, cz) ->
      playerX = cx
      playerZ = cz
    }

    if (WorldMap.isWater(playerX, playerZ)) {
      playerX = 148f
      playerZ = playerZ.coerceIn(-260f, 260f)
      playerY = WorldMap.getSurfaceHeight(playerX, playerZ)
      playerVy = 0f
      showBanner("SHALLOW WATERS — RETURNED TO VICE BEACH")
    }
  }

  private fun stepPlayerVehicle(v: VehicleEntity, dt: Float) {
    val spec = v.model
    val throttle = controls.throttleInput.coerceIn(0f, 1f)
    val brake = controls.brakeInput.coerceIn(0f, 1f)
    val steer = controls.steerInput.coerceIn(-1f, 1f)
    val drifting = controls.handbrake && abs(v.forwardSpeed) > 8f && v.grounded
    v.isDrifting = drifting

    // Nitro boost
    val wantNos = controls.nosBoost && nitroAmount > 0.04f && throttle > 0.1f
    v.nosActive = wantNos
    if (wantNos) {
      nitroAmount = (nitroAmount - 0.34f * dt).coerceAtLeast(0f)
    } else {
      nitroAmount = (nitroAmount + 0.12f * dt).coerceAtMost(1f)
    }

    val nosMult = if (wantNos) 1.32f else 1.0f
    val maxFwd = spec.maxSpeedMps * nosMult
    val maxRev = -16f

    // Forward/backward thrust
    val fx = sin(v.yawRad)
    val fz = cos(v.yawRad)
    val rx = cos(v.yawRad)
    val rz = -sin(v.yawRad)

    var fwdVel = v.vx * fx + v.vz * fz
    var latVel = v.vx * rx + v.vz * rz

    if (v.grounded) {
      if (throttle > 0.05f) {
        fwdVel += throttle * spec.accelMps2 * (if (wantNos) 1.65f else 1f) * dt
      }
      if (brake > 0.05f) {
        if (fwdVel > 1.5f) {
          fwdVel -= brake * spec.brakeMps2 * dt
        } else {
          fwdVel -= brake * spec.accelMps2 * 0.65f * dt
        }
      }
      if (drifting) {
        fwdVel *= (1f - 1.15f * dt).coerceAtLeast(0f)
      }
      // Natural rolling + aero drag
      val drag = if (throttle < 0.05f && brake < 0.05f) 0.9f else 0.22f
      fwdVel *= (1f - drag * dt).coerceAtLeast(0f)
      fwdVel = fwdVel.coerceIn(maxRev, maxFwd)

      // Steering (invert when reversing)
      val speedFactor = (abs(fwdVel) / 8f).coerceIn(0f, 1f)
      val dirSign = if (fwdVel >= -0.3f) 1f else -1f
      val turnBoost = if (drifting) 1.42f else 1f
      // Negative steer = left (CCW +yaw in right-handed? Wait: sin(yaw) is +X, cos(yaw) is +Z.
      // Let's check: yaw=0 -> forward (0, 1) +Z. Left of +Z when looking from behind (-Z toward +Z) is +X or -X?
      // Camera behind vehicle is at (-fx, -fz), looking toward (+fx, +fz), up is +Y.
      // Cross(forward=(0,0,1), up=(0,1,0)) = (-1, 0, 0). So RIGHT is -X, LEFT is +X!
      // Wait: sin(yaw) for yaw > 0 gives +X -> LEFT! So increasing yaw turns LEFT, decreasing yaw turns RIGHT!
      v.yawRad -= steer * spec.turnRateRad * speedFactor * dirSign * turnBoost * dt

      // Recompute basis after yaw update
      val nfx = sin(v.yawRad)
      val nfz = cos(v.yawRad)
      val nrx = cos(v.yawRad)
      val nrz = -sin(v.yawRad)

      val grip = if (drifting) spec.lateralGrip * 0.24f else spec.lateralGrip
      latVel *= (1f - grip * dt).coerceAtLeast(0f)

      v.vx = nfx * fwdVel + nrx * latVel
      v.vz = nfz * fwdVel + nrz * latVel
      v.forwardSpeed = fwdVel
    } else {
      // Airborne slight air-control
      v.yawRad -= steer * 0.65f * dt
      v.forwardSpeed = v.vx * fx + v.vz * fz
    }

    v.steerAngle = steer * 0.48f

    // Bike lean & wheelie OR Car body pitch/roll suspension
    if (spec.isBike) {
      val targetLean = steer * 0.56f * (abs(v.forwardSpeed) / 22f).coerceIn(0.15f, 1f)
      v.leanAngle += (targetLean - v.leanAngle) * (10f * dt).coerceAtMost(1f)
      val wantWheelie = (controls.wheelieOrJump || wantNos) && v.forwardSpeed > 6f && v.grounded
      val targetPitch = if (wantWheelie) -0.36f else (-throttle * 0.05f + brake * 0.08f)
      v.wheeliePitch += (targetPitch - v.wheeliePitch) * (8f * dt).coerceAtMost(1f)
      v.bodyRoll = 0f
      v.bodyPitch = 0f
    } else {
      val targetPitch = (-throttle * 0.045f + brake * 0.065f)
      val targetRoll = -steer * 0.06f * (abs(v.forwardSpeed) / 30f).coerceIn(0f, 1f)
      v.bodyPitch += (targetPitch - v.bodyPitch) * (8f * dt).coerceAtMost(1f)
      v.bodyRoll += (targetRoll - v.bodyRoll) * (9f * dt).coerceAtMost(1f)
      v.leanAngle = 0f
      v.wheeliePitch = 0f
    }

    val prevX = v.x
    val prevZ = v.z
    val prevY = v.y

    v.x += v.vx * dt
    v.z += v.vz * dt
    v.wheelSpinRad += (v.forwardSpeed / spec.wheelRadius) * dt

    // Vertical & ramp physics
    val surfH = WorldMap.getSurfaceHeight(v.x, v.z)
    val prevSurfH = WorldMap.getSurfaceHeight(prevX, prevZ)
    v.vy -= 22f * dt
    v.y += v.vy * dt

    if (v.y <= surfH + 0.03f) {
      // Check if we just landed from an air stunt!
      if (!v.grounded && airborneTime > 0.45f) {
        val jumpDist = hypot(v.x - launchX, v.z - launchZ).toInt()
        val bonus = (250 + jumpDist * 12).coerceAtMost(1200)
        cash += bonus
        showBanner("STUNT JUMP ${jumpDist}m! +$$bonus")
        onChimeEvent?.invoke()
      }
      airborneTime = 0f
      // Ramp upward slope launch velocity
      val slopeVy = (surfH - prevSurfH) / dt
      v.y = surfH
      v.vy = slopeVy.coerceIn(-4f, 14f)
      v.grounded = true
    } else {
      if (v.grounded) {
        launchX = prevX
        launchZ = prevZ
        // Launch kicker if leaving high ramp lip
        if (prevSurfH > 1.2f && surfH < prevSurfH - 0.5f) {
          v.vy = maxOf(v.vy, abs(v.forwardSpeed) * 0.28f + 4.5f)
        }
      }
      v.grounded = false
      airborneTime += dt
    }

    // Skidmarks when drifting or hard cornering
    if (v.grounded && (drifting || abs(latVel) > 6.5f)) {
      val wOff = if (spec.isBike) 0f else spec.width * 0.36f
      val bOff = -spec.length * 0.34f
      val wx1 = prevX + sin(v.yawRad) * bOff + cos(v.yawRad) * wOff
      val wz1 = prevZ + cos(v.yawRad) * bOff - sin(v.yawRad) * wOff
      val wx2 = v.x + sin(v.yawRad) * bOff + cos(v.yawRad) * wOff
      val wz2 = v.z + cos(v.yawRad) * bOff - sin(v.yawRad) * wOff
      addSkidMark(wx1, v.y + 0.03f, wz1, wx2, v.y + 0.03f, wz2)
      if (!spec.isBike) {
        val ox1 = prevX + sin(v.yawRad) * bOff - cos(v.yawRad) * wOff
        val oz1 = prevZ + cos(v.yawRad) * bOff + sin(v.yawRad) * wOff
        val ox2 = v.x + sin(v.yawRad) * bOff - cos(v.yawRad) * wOff
        val oz2 = v.z + cos(v.yawRad) * bOff + sin(v.yawRad) * wOff
        addSkidMark(ox1, v.y + 0.03f, oz1, ox2, v.y + 0.03f, oz2)
      }
    }

    // Building collisions
    val radius = if (spec.isBike) 0.9f else 1.75f
    WorldMap.resolveBuildingCollision(v.x, v.z, radius)?.let { (cx, cz) ->
      val impact = hypot(v.vx, v.vz)
      v.x = cx
      v.z = cz
      v.vx *= -0.32f
      v.vz *= -0.32f
      v.forwardSpeed *= -0.32f
      if (impact > 24f) {
        showBanner("HEAVY IMPACT!")
      }
    }

    // Ocean boundary rescue
    if (WorldMap.isWater(v.x, v.z)) {
      v.x = 122f
      v.z = v.z.coerceIn(-250f, 250f)
      v.y = WorldMap.getSurfaceHeight(v.x, v.z)
      v.vx = 0f
      v.vz = 0f
      v.vy = 0f
      v.forwardSpeed = 0f
      showBanner("VEHICLE RECOVERED TO OCEAN BLVD")
    }
  }

  private fun stepOtherVehicle(v: VehicleEntity, dt: Float) {
    if (v.isPolice && wantedStars > 0) {
      // Active Police Chase AI!
      val dx = playerX - v.x
      val dz = playerZ - v.z
      val dist = hypot(dx, dz)
      val targetYaw = atan2(dx, dz)
      v.yawRad = lerpAngle(v.yawRad, targetYaw, (2.2f * dt).coerceAtMost(1f))
      val chaseSpeed = (22f + wantedStars * 4.5f).coerceAtMost(v.model.maxSpeedMps * 0.85f)
      val targetSpd = if (dist > 6f) chaseSpeed else 6f
      v.forwardSpeed += (targetSpd - v.forwardSpeed) * (2.5f * dt).coerceAtMost(1f)
      v.vx = sin(v.yawRad) * v.forwardSpeed
      v.vz = cos(v.yawRad) * v.forwardSpeed
      v.x += v.vx * dt
      v.z += v.vz * dt
      v.y = WorldMap.getSurfaceHeight(v.x, v.z)
      v.wheelSpinRad += (v.forwardSpeed / v.model.wheelRadius) * dt
      WorldMap.resolveBuildingCollision(v.x, v.z, 1.8f)?.let { (cx, cz) ->
        v.x = cx
        v.z = cz
        v.yawRad += 0.9f
      }
      return
    }

    if (v.isAiTraffic) {
      // Cruise along avenues and streets
      v.forwardSpeed += (v.aiSpeedTarget - v.forwardSpeed) * (2f * dt).coerceAtMost(1f)
      v.vx = sin(v.yawRad) * v.forwardSpeed
      v.vz = cos(v.yawRad) * v.forwardSpeed
      v.x += v.vx * dt
      v.z += v.vz * dt
      v.y = WorldMap.getSurfaceHeight(v.x, v.z)
      v.wheelSpinRad += (v.forwardSpeed / v.model.wheelRadius) * dt

      // Bike subtle lean in AI traffic
      if (v.model.isBike) {
        v.leanAngle *= (1f - 4f * dt).coerceAtLeast(0f)
      }

      // Turn around at world edges or intersections
      if (abs(v.z) > 255f || v.x < -265f || v.x > 125f) {
        v.yawRad += PI.toFloat()
        v.x = v.x.coerceIn(-260f, 122f)
        v.z = v.z.coerceIn(-250f, 250f)
      }
      WorldMap.resolveBuildingCollision(v.x, v.z, 1.8f)?.let { (cx, cz) ->
        v.x = cx
        v.z = cz
        v.yawRad += (PI / 2).toFloat()
      }
    } else {
      // Parked vehicle friction
      v.vx *= (1f - 4f * dt).coerceAtLeast(0f)
      v.vz *= (1f - 4f * dt).coerceAtLeast(0f)
      v.forwardSpeed = v.vx * sin(v.yawRad) + v.vz * cos(v.yawRad)
      v.x += v.vx * dt
      v.z += v.vz * dt
      v.y = WorldMap.getSurfaceHeight(v.x, v.z)
    }
  }

  private fun resolveVehicleCollisions() {
    val active = activeVehicle()
    for (i in vehicles.indices) {
      val a = vehicles[i]
      for (j in i + 1 until vehicles.size) {
        val b = vehicles[j]
        val dx = b.x - a.x
        val dz = b.z - a.z
        val minDist = (a.model.width + b.model.width) * 0.65f
        val dSq = dx * dx + dz * dz
        if (dSq < minDist * minDist && dSq > 0.0001f) {
          val dist = sqrt(dSq)
          val nx = dx / dist
          val nz = dz / dist
          val overlap = (minDist - dist) * 0.5f
          a.x -= nx * overlap
          a.z -= nz * overlap
          b.x += nx * overlap
          b.z += nz * overlap

          val relVx = b.vx - a.vx
          val relVz = b.vz - a.vz
          val sepVel = relVx * nx + relVz * nz
          if (sepVel < 0f) {
            val impulse = -sepVel * 0.72f
            a.vx -= nx * impulse
            a.vz -= nz * impulse
            b.vx += nx * impulse
            b.vz += nz * impulse

            // Raise wanted level if player slams into police or hits traffic hard
            if ((a == active || b == active) && abs(sepVel) > 10f) {
              val other = if (a == active) b else a
              if (other.isPolice) {
                addWantedStars(1)
              } else if (abs(sepVel) > 20f && wantedStars == 0) {
                addWantedStars(1)
              }
            }
          }
        }
      }
    }
  }

  private fun checkWorldInteractions(dt: Float) {
    // Cash pickups
    for (i in WorldMap.pickups.indices) {
      if (pickupCooldowns[i] > 0f) continue
      val p = WorldMap.pickups[i]
      val distSq = (playerX - p.x) * (playerX - p.x) + (playerZ - p.z) * (playerZ - p.z)
      if (distSq < 9.5f) {
        pickupCooldowns[i] = 45f
        cash += p.amount
        showBanner("CASH STASH COLLECTED +$${p.amount}")
        onChimeEvent?.invoke()
      }
    }

    // Time trial checkpoints
    if (timeTrialActive) {
      timeTrialSeconds += dt
      val cp = WorldMap.checkpoints[activeCheckpointIdx]
      val distSq = (playerX - cp.x) * (playerX - cp.x) + (playerZ - cp.z) * (playerZ - cp.z)
      if (distSq < 42f) {
        activeCheckpointIdx++
        onChimeEvent?.invoke()
        if (activeCheckpointIdx >= WorldMap.checkpoints.size) {
          val lap = timeTrialSeconds
          val prevBest = bestLapSeconds
          if (prevBest == null || lap < prevBest) {
            bestLapSeconds = lap
          }
          cash += 2500
          showBanner("CIRCUIT COMPLETE! ${"%.1f".format(lap)}s (+$2,500)")
          activeCheckpointIdx = 0
          timeTrialSeconds = 0f
        } else {
          showBanner("CHECKPOINT $activeCheckpointIdx / ${WorldMap.checkpoints.size}")
        }
      }
    }
  }

  private fun updatePoliceSystem(dt: Float) {
    val active = activeVehicle()
    if (wantedStars == 0) {
      // Speeding past a cop car (> 150 km/h within 20m) triggers 1 star
      if (active != null && abs(active.forwardSpeed) > 42f) {
        for (v in vehicles) {
          if (v.isPolice && v.id != active.id) {
            val d = hypot(v.x - active.x, v.z - active.z)
            if (d < 20f) {
              addWantedStars(1)
              showBanner("SPEED TRAP! VICE PD IN PURSUIT")
              break
            }
          }
        }
      }
      return
    }

    // Check distance to nearest police car
    var nearestCopDist = Float.MAX_VALUE
    for (v in vehicles) {
      if (v.isPolice && v.id != activeVehicleId) {
        val d = hypot(v.x - playerX, v.z - playerZ)
        if (d < nearestCopDist) nearestCopDist = d
      }
    }

    // Busted check if slow & pinned right next to a cop
    val playerSpeed = active?.let { abs(it.forwardSpeed) } ?: 0f
    if (nearestCopDist < 7.5f && playerSpeed < 2.2f) {
      bustedTimer += dt
      if (bustedTimer > 3.2f) {
        val bail = minOf(cash, 500)
        cash -= bail
        wantedStars = 0
        wantedCooldown = 0f
        bustedTimer = 0f
        showBanner("BUSTED! BAIL -$${bail}")
        return
      }
    } else {
      bustedTimer = 0f
    }

    // Evade cooldown when far from all police
    if (nearestCopDist > 105f) {
      wantedCooldown += dt
      if (wantedCooldown >= 8.5f) {
        val reward = wantedStars * 400
        cash += reward
        wantedStars = 0
        wantedCooldown = 0f
        showBanner("PURSUIT EVADED! +$$reward")
        onChimeEvent?.invoke()
      }
    } else {
      wantedCooldown = 0f
    }
  }

  private fun addWantedStars(add: Int) {
    val prev = wantedStars
    wantedStars = (wantedStars + add).coerceIn(1, 5)
    wantedCooldown = 0f
    if (prev == 0) {
      // Ensure at least 2 police units are reasonably close to pursue
      var copsMoved = 0
      for (v in vehicles) {
        if (v.isPolice && v.id != activeVehicleId) {
          val d = hypot(v.x - playerX, v.z - playerZ)
          if (d > 140f && copsMoved < 2) {
            v.x = (playerX - 75f).coerceIn(-250f, 114f)
            v.z = (playerZ - 65f).coerceIn(-240f, 240f)
            v.y = WorldMap.getSurfaceHeight(v.x, v.z)
            copsMoved++
          }
        }
      }
    }
  }

  private fun addSkidMark(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float) {
    if (skidMarks.size >= 170) skidMarks.removeFirst()
    skidMarks.addLast(SkidMark(x1, y1, z1, x2, y2, z2, life = 5.5f))
  }

  private fun showBanner(msg: String) {
    bannerText = msg
    bannerTimer = 3.4f
  }

  private fun lerpAngle(a: Float, b: Float, t: Float): Float {
    var diff = (b - a) % (2f * PI.toFloat())
    if (diff > PI.toFloat()) diff -= 2f * PI.toFloat()
    if (diff < -PI.toFloat()) diff += 2f * PI.toFloat()
    return a + diff * t
  }

  fun buildHudSnapshot(fps: Int): HudSnapshot {
    val curVeh = activeVehicle()
    val speedKmh = if (curVeh != null) (abs(curVeh.forwardSpeed) * 3.6f).toInt() else 0
    val gear =
      if (curVeh == null) "N"
      else if (curVeh.forwardSpeed < -0.8f) "R"
      else if (speedKmh < 4) "1"
      else ((speedKmh / 38) + 1).coerceIn(1, 6).toString()

    val rpmNorm =
      if (curVeh == null) 0f
      else {
        val ratio = abs(curVeh.forwardSpeed) / curVeh.model.maxSpeedMps
        ((ratio * 6f) % 1f).coerceIn(0.18f, 1f) * 0.65f + ratio * 0.35f
      }

    val nearby = if (curVeh == null) findNearestEnterableVehicle(5.8f) else null
    val prompt =
      nearby?.let {
        if (it.model.isBike) "Ride ${it.model.displayName}" else "Drive ${it.model.displayName}"
      }

    val blips = ArrayList<MinimapBlip>(24)
    for (v in vehicles) {
      if (v.id == activeVehicleId) continue
      blips.add(
        MinimapBlip(
          x = v.x,
          z = v.z,
          isPolice = v.isPolice,
          isBike = v.model.isBike,
          isPickup = false,
          isCheckpoint = false,
        )
      )
    }
    for (i in WorldMap.pickups.indices) {
      if (pickupCooldowns[i] <= 0f) {
        val p = WorldMap.pickups[i]
        blips.add(MinimapBlip(p.x, p.z, isPolice = false, isBike = false, isPickup = true, isCheckpoint = false))
      }
    }
    if (timeTrialActive) {
      val cp = WorldMap.checkpoints[activeCheckpointIdx]
      blips.add(MinimapBlip(cp.x, cp.z, isPolice = false, isBike = false, isPickup = false, isCheckpoint = true))
    }

    return HudSnapshot(
      speedKmh = speedKmh,
      gearLabel = gear,
      rpmNormalized = rpmNorm.coerceIn(0f, 1f),
      nitroNormalized = nitroAmount,
      cash = cash,
      wantedStars = wantedStars,
      wantedCooldownRatio = if (wantedStars > 0) (wantedCooldown / 8.5f).coerceIn(0f, 1f) else 0f,
      isInVehicle = curVeh != null,
      currentVehicleModel = curVeh?.model,
      currentVehicleName = curVeh?.model?.displayName ?: "On Foot",
      currentVehicleIsBike = curVeh?.model?.isBike ?: false,
      nearbyVehiclePrompt = prompt,
      headlightsOn = curVeh?.headlightsOn ?: headlightsOn,
      playerX = playerX,
      playerZ = playerZ,
      playerYawRad = playerYaw,
      zoneName = WorldMap.getZoneName(playerX, playerZ),
      bannerMessage = bannerText,
      activeCheckpointIndex = activeCheckpointIdx,
      totalCheckpoints = WorldMap.checkpoints.size,
      timeTrialActive = timeTrialActive,
      timeTrialSeconds = timeTrialSeconds,
      bestLapSeconds = bestLapSeconds,
      cameraMode = cameraMode,
      timeOfDay = timeOfDay,
      blips = blips,
      fps = fps,
    )
  }
}
