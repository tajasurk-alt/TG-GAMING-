package com.example.game.physics

import com.bulletphysics.collision.broadphase.DbvtBroadphase
import com.bulletphysics.collision.dispatch.CollisionDispatcher
import com.bulletphysics.collision.dispatch.CollisionObject
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration
import com.bulletphysics.collision.shapes.BoxShape
import com.bulletphysics.collision.shapes.CompoundShape
import com.bulletphysics.collision.shapes.ConvexHullShape
import com.bulletphysics.collision.shapes.StaticPlaneShape
import com.bulletphysics.dynamics.DiscreteDynamicsWorld
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.RigidBodyConstructionInfo
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver
import com.bulletphysics.dynamics.vehicle.DefaultVehicleRaycaster
import com.bulletphysics.dynamics.vehicle.RaycastVehicle
import com.bulletphysics.dynamics.vehicle.VehicleTuning
import com.bulletphysics.linearmath.DefaultMotionState
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.util.ObjectArrayList
import com.example.game.model.VehicleEntity
import com.example.game.model.VehicleModelType
import com.example.game.world.WorldMap
import javax.vecmath.Matrix3f
import javax.vecmath.Quat4f
import javax.vecmath.Vector3f
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class PhysicsVehicleHandle(
  val entityId: Int,
  val model: VehicleModelType,
  val chassisBody: RigidBody,
  val raycastVehicle: RaycastVehicle,
  val tuning: VehicleTuning,
  var lastSpeedMps: Float = 0f,
)

class PhysicsWorld {
  private val collisionConfig = DefaultCollisionConfiguration()
  private val dispatcher = CollisionDispatcher(collisionConfig)
  private val broadphase = DbvtBroadphase()
  private val solver = SequentialImpulseConstraintSolver()
  val dynamicsWorld: DiscreteDynamicsWorld =
    DiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfig)

  private val vehicleRaycaster = DefaultVehicleRaycaster(dynamicsWorld)
  private val staticBodies = mutableListOf<RigidBody>()
  private val vehicleHandles = HashMap<Int, PhysicsVehicleHandle>()

  // Reusable scratch objects to keep frame allocations minimal
  private val tmpTransform = Transform()
  private val tmpQuat = Quat4f()
  private val tmpLinVel = Vector3f()
  private val tmpAngVel = Vector3f()
  private val tmpForce = Vector3f()
  private val tmpBasis = Matrix3f()

  init {
    dynamicsWorld.setGravity(Vector3f(0f, -20.0f, 0f))
    buildStaticWorldColliders()
  }

  private fun buildStaticWorldColliders() {
    // 1. Flat ground plane at Y = 0
    val groundShape = StaticPlaneShape(Vector3f(0f, 1f, 0f), 0f)
    val groundBody = createStaticBody(groundShape, 0f, 0f, 0f, 0f, friction = 1.0f, restitution = 0.05f)
    dynamicsWorld.addRigidBody(groundBody)
    staticBodies.add(groundBody)

    // 2. City Buildings (AABB boxes)
    for (b in WorldMap.buildings) {
      val shape = BoxShape(Vector3f(b.widthX * 0.5f, b.height * 0.5f, b.depthZ * 0.5f))
      val body =
        createStaticBody(
          shape = shape,
          x = b.centerX,
          y = b.height * 0.5f,
          z = b.centerZ,
          yawRad = 0f,
          friction = 0.45f,
          restitution = 0.25f,
        )
      dynamicsWorld.addRigidBody(body)
      staticBodies.add(body)
    }

    // 3. Stunt Ramps (6-vertex 3D wedge ConvexHullShape)
    for (ramp in WorldMap.ramps) {
      val hw = ramp.width * 0.5f
      val hl = ramp.length * 0.5f
      val h = ramp.height
      val pts = ObjectArrayList<Vector3f>()
      pts.add(Vector3f(-hw, 0f, -hl))
      pts.add(Vector3f(hw, 0f, -hl))
      pts.add(Vector3f(-hw, 0f, hl))
      pts.add(Vector3f(hw, 0f, hl))
      pts.add(Vector3f(-hw, h, hl))
      pts.add(Vector3f(hw, h, hl))

      val rampShape = ConvexHullShape(pts)
      val body =
        createStaticBody(
          shape = rampShape,
          x = ramp.x,
          y = 0f,
          z = ramp.z,
          yawRad = ramp.yawRad,
          friction = 1.1f,
          restitution = 0.0f,
        )
      dynamicsWorld.addRigidBody(body)
      staticBodies.add(body)
    }
  }

  private fun createStaticBody(
    shape: com.bulletphysics.collision.shapes.CollisionShape,
    x: Float,
    y: Float,
    z: Float,
    yawRad: Float,
    friction: Float,
    restitution: Float,
  ): RigidBody {
    val t = Transform()
    t.setIdentity()
    t.origin.set(x, y, z)
    val q = Quat4f()
    setYawQuat(q, yawRad)
    t.setRotation(q)
    val motionState = DefaultMotionState(t)
    val info = RigidBodyConstructionInfo(0f, motionState, shape, Vector3f(0f, 0f, 0f))
    info.friction = friction
    info.restitution = restitution
    return RigidBody(info)
  }

  fun clearVehicles() {
    for (handle in vehicleHandles.values) {
      dynamicsWorld.removeAction(handle.raycastVehicle)
      dynamicsWorld.removeRigidBody(handle.chassisBody)
    }
    vehicleHandles.clear()
  }

  fun removeVehicle(entityId: Int) {
    val handle = vehicleHandles.remove(entityId) ?: return
    dynamicsWorld.removeAction(handle.raycastVehicle)
    dynamicsWorld.removeRigidBody(handle.chassisBody)
  }

  fun registerVehicle(v: VehicleEntity): PhysicsVehicleHandle {
    vehicleHandles[v.id]?.let { return it }

    val spec = v.model
    val isBike = spec.isBike

    // Compound chassis so origin sits near road reference & CoM stays low against rollovers
    val compound = CompoundShape()
    val localTrans = Transform()
    localTrans.setIdentity()
    if (isBike) {
      val box = BoxShape(Vector3f(0.32f, 0.42f, spec.length * 0.44f))
      localTrans.origin.set(0f, 0.72f, 0f)
      compound.addChildShape(localTrans, box)
    } else {
      val box = BoxShape(Vector3f(spec.width * 0.46f, 0.38f, spec.length * 0.47f))
      localTrans.origin.set(0f, 0.68f, 0f)
      compound.addChildShape(localTrans, box)
    }

    val mass =
      when (spec) {
        VehicleModelType.AKUMA_1000R -> 230f
        VehicleModelType.SANCHEZ_MX -> 190f
        VehicleModelType.INFERNUS_GT -> 1320f
        VehicleModelType.BANSHEE_V8 -> 1520f
        VehicleModelType.VICE_PD -> 1650f
        VehicleModelType.URBAN_SEDAN -> 1450f
      }

    val inertia = Vector3f()
    compound.calculateLocalInertia(mass, inertia)

    val startTrans = Transform()
    startTrans.setIdentity()
    startTrans.origin.set(v.x, v.y + 0.15f, v.z)
    val q = Quat4f()
    setYawQuat(q, v.yawRad)
    startTrans.setRotation(q)

    val motionState = DefaultMotionState(startTrans)
    val rbInfo = RigidBodyConstructionInfo(mass, motionState, compound, inertia)
    rbInfo.linearDamping = 0.04f
    rbInfo.angularDamping = if (isBike) 0.25f else 0.18f
    rbInfo.restitution = 0.15f
    rbInfo.friction = 0.55f

    val chassis = RigidBody(rbInfo)
    chassis.activationState = CollisionObject.DISABLE_DEACTIVATION
    dynamicsWorld.addRigidBody(chassis)

    val tuning = VehicleTuning().apply {
      suspensionStiffness = if (isBike) 42f else 34f
      suspensionCompression = if (isBike) 4.4f else 3.6f
      suspensionDamping = if (isBike) 5.0f else 4.2f
      maxSuspensionTravelCm = 35f
      frictionSlip = if (isBike) 4.2f else 3.2f
      maxSuspensionForce = mass * 35f
    }

    val raycastVehicle = RaycastVehicle(tuning, chassis, vehicleRaycaster)
    // X = right, Y = up, Z = forward
    raycastVehicle.setCoordinateSystem(0, 1, 2)
    dynamicsWorld.addAction(raycastVehicle)

    val wheelDir = Vector3f(0f, -1f, 0f)
    val wheelAxle = Vector3f(-1f, 0f, 0f)
    val suspRest = if (isBike) 0.34f else 0.30f
    val connY = if (isBike) 0.58f else 0.56f

    if (isBike) {
      val zWheel = spec.length * 0.36f
      // Front wheel (index 0)
      raycastVehicle.addWheel(
        Vector3f(0f, connY, zWheel),
        wheelDir,
        wheelAxle,
        suspRest,
        spec.wheelRadius,
        tuning,
        true,
      )
      // Rear wheel (index 1)
      raycastVehicle.addWheel(
        Vector3f(0f, connY, -zWheel),
        wheelDir,
        wheelAxle,
        suspRest,
        spec.wheelRadius,
        tuning,
        false,
      )
    } else {
      val xWheel = spec.width * 0.44f
      val zWheel = spec.length * 0.32f
      // Front Left & Front Right
      raycastVehicle.addWheel(Vector3f(xWheel, connY, zWheel), wheelDir, wheelAxle, suspRest, spec.wheelRadius, tuning, true)
      raycastVehicle.addWheel(Vector3f(-xWheel, connY, zWheel), wheelDir, wheelAxle, suspRest, spec.wheelRadius, tuning, true)
      // Rear Left & Rear Right
      raycastVehicle.addWheel(Vector3f(xWheel, connY, -zWheel), wheelDir, wheelAxle, suspRest, spec.wheelRadius, tuning, false)
      raycastVehicle.addWheel(Vector3f(-xWheel, connY, -zWheel), wheelDir, wheelAxle, suspRest, spec.wheelRadius, tuning, false)
    }

    for (i in 0 until raycastVehicle.numWheels) {
      val w = raycastVehicle.getWheelInfo(i)
      w.suspensionStiffness = tuning.suspensionStiffness
      w.wheelsDampingRelaxation = tuning.suspensionDamping
      w.wheelsDampingCompression = tuning.suspensionCompression
      w.frictionSlip = tuning.frictionSlip
      w.rollInfluence = if (isBike) 0.02f else 0.14f
    }

    val handle = PhysicsVehicleHandle(v.id, spec, chassis, raycastVehicle, tuning)
    vehicleHandles[v.id] = handle
    return handle
  }

  fun applyVehicleControl(
    v: VehicleEntity,
    throttle: Float,
    brake: Float,
    steer: Float,
    handbrake: Boolean,
    nosActive: Boolean,
    wheelieHeld: Boolean,
    dt: Float,
  ) {
    val handle = registerVehicle(v)
    val rv = handle.raycastVehicle
    val chassis = handle.chassisBody
    val spec = v.model

    chassis.getWorldTransform(tmpTransform)
    chassis.getLinearVelocity(tmpLinVel)
    tmpBasis.set(tmpTransform.basis)

    // Forward vector (+Z column of basis)
    val fx = tmpBasis.m02
    val fy = tmpBasis.m12
    val fz = tmpBasis.m22
    val fwdSpeed = tmpLinVel.x * fx + tmpLinVel.y * fy + tmpLinVel.z * fz

    val maxFwd = spec.maxSpeedMps * (if (nosActive) 1.32f else 1.0f)
    val speedRatio = (abs(fwdSpeed) / maxFwd).coerceIn(0f, 1.2f)

    // Steering decreases gracefully at high speeds so handling stays crisp
    val maxSteerAngle = if (spec.isBike) 0.42f else 0.46f
    val speedSteerScale = (1f - speedRatio * 0.58f).coerceIn(0.28f, 1f)
    // Negative steerInput = Left (+steer in +X/+Z coord), Positive steerInput = Right (-steer)
    val steerVal = -steer * maxSteerAngle * speedSteerScale

    val mass = 1f / chassis.invMass
    val enginePower = mass * spec.accelMps2 * (if (nosActive) 1.55f else 1.0f)

    var driveForce = 0f
    var brakeForce = 0f

    if (throttle > 0.05f && fwdSpeed < maxFwd) {
      driveForce = throttle * enginePower * (1f - (speedRatio * 0.45f)).coerceAtLeast(0.2f)
    }
    if (brake > 0.05f) {
      if (fwdSpeed > 1.8f) {
        brakeForce = brake * mass * 0.85f
      } else if (fwdSpeed > -16f) {
        driveForce = -brake * enginePower * 0.55f
      }
    }

    if (spec.isBike) {
      rv.setSteeringValue(steerVal, 0)
      rv.applyEngineForce(driveForce, 1)
      rv.setBrake(brakeForce + (if (handbrake) mass * 0.6f else 0f), 1)
      rv.setBrake(brakeForce * 0.65f, 0)

      // Apply gyroscopic roll-lean stabilization & wheelie pitch for 2-wheel motorbike!
      stabilizeMotorbikeOrientation(v, chassis, tmpBasis, steer, fwdSpeed, wheelieHeld || nosActive, dt)
    } else {
      rv.setSteeringValue(steerVal, 0)
      rv.setSteeringValue(steerVal, 1)

      // AWD/RWD split for responsive supercar & muscle car drive
      rv.applyEngineForce(driveForce * 0.35f, 0)
      rv.applyEngineForce(driveForce * 0.35f, 1)
      rv.applyEngineForce(driveForce * 0.65f, 2)
      rv.applyEngineForce(driveForce * 0.65f, 3)

      val rearBrake = brakeForce + (if (handbrake) mass * 1.4f else 0f)
      rv.setBrake(brakeForce * 0.7f, 0)
      rv.setBrake(brakeForce * 0.7f, 1)
      rv.setBrake(rearBrake, 2)
      rv.setBrake(rearBrake, 3)

      // Handbrake drift lowers rear lateral friction slip cleanly
      val rearGrip = if (handbrake && abs(fwdSpeed) > 7f) 0.95f else 3.2f
      rv.getWheelInfo(2).frictionSlip = rearGrip
      rv.getWheelInfo(3).frictionSlip = rearGrip
    }

    // NOS extra forward impulse when grounded
    if (nosActive && v.grounded) {
      tmpForce.set(fx * mass * 14f, 0f, fz * mass * 14f)
      chassis.applyCentralForce(tmpForce)
    }
  }

  private fun stabilizeMotorbikeOrientation(
    v: VehicleEntity,
    chassis: RigidBody,
    basis: Matrix3f,
    steerInput: Float,
    fwdSpeed: Float,
    wantWheelie: Boolean,
    dt: Float,
  ) {
    // Current local up & right & forward vectors
    val upX = basis.m01
    val upY = basis.m11
    val upZ = basis.m21
    val rx = basis.m00
    val ry = basis.m10
    val rz = basis.m20
    val fx = basis.m02
    val fy = basis.m12
    val fz = basis.m22

    val speedFactor = (abs(fwdSpeed) / 20f).coerceIn(0f, 1f)
    val targetLean = steerInput * 0.48f * speedFactor
    v.leanAngle += (targetLean - v.leanAngle) * (10f * dt).coerceAtMost(1f)

    // Measure current roll & pitch relative to world Y
    val currentRoll = asin((rx * 0f + ry * 1f + rz * 0f).coerceIn(-0.99f, 0.99f))
    val currentPitch = asin((-fy).coerceIn(-0.99f, 0.99f))

    val targetPitch =
      if (wantWheelie && fwdSpeed > 6f && v.grounded) -0.28f else 0f
    v.wheeliePitch += (targetPitch - v.wheeliePitch) * (8f * dt).coerceAtMost(1f)

    chassis.getAngularVelocity(tmpAngVel)

    // Roll stabilization PD controller around forward axis
    val rollError = -targetLean - currentRoll
    val angVelAlongFwd = tmpAngVel.x * fx + tmpAngVel.y * fy + tmpAngVel.z * fz
    val rollCorrection = rollError * 14f - angVelAlongFwd * 0.55f

    // Pitch stabilization PD controller around right axis (only strong when grounded so ramps launch cleanly)
    val pitchGain = if (v.grounded) 10f else 2.5f
    val pitchError = targetPitch - currentPitch
    val angVelAlongRight = tmpAngVel.x * rx + tmpAngVel.y * ry + tmpAngVel.z * rz
    val pitchCorrection = pitchError * pitchGain - angVelAlongRight * 0.5f

    // Keep yaw angular velocity untouched by stabilizer!
    val yawVel = tmpAngVel.y * upY
    tmpAngVel.x = fx * rollCorrection + rx * pitchCorrection
    tmpAngVel.y = yawVel
    tmpAngVel.z = fz * rollCorrection + rz * pitchCorrection

    // Blend gently so collisions still push the bike naturally
    chassis.getAngularVelocity(tmpForce)
    tmpAngVel.x = tmpForce.x + (tmpAngVel.x - tmpForce.x) * (16f * dt).coerceIn(0f, 1f)
    tmpAngVel.z = tmpForce.z + (tmpAngVel.z - tmpForce.z) * (16f * dt).coerceIn(0f, 1f)
    chassis.setAngularVelocity(tmpAngVel)
  }

  fun step(dt: Float, vehicles: List<VehicleEntity>, onImpact: (VehicleEntity, Float) -> Unit) {
    // Ensure every simulation vehicle has a JBullet handle
    for (v in vehicles) {
      if (!vehicleHandles.containsKey(v.id)) {
        registerVehicle(v)
      }
    }

    dynamicsWorld.stepSimulation(dt, 4, 1f / 60f)

    // Sync JBullet physics transforms & wheel states back to VehicleEntity
    for (v in vehicles) {
      val handle = vehicleHandles[v.id] ?: continue
      val chassis = handle.chassisBody
      val rv = handle.raycastVehicle

      chassis.getWorldTransform(tmpTransform)
      chassis.getLinearVelocity(tmpLinVel)
      tmpBasis.set(tmpTransform.basis)

      v.x = tmpTransform.origin.x
      v.y = tmpTransform.origin.y
      v.z = tmpTransform.origin.z

      v.vx = tmpLinVel.x
      v.vy = tmpLinVel.y
      v.vz = tmpLinVel.z

      val fx = tmpBasis.m02
      val fy = tmpBasis.m12
      val fz = tmpBasis.m22
      v.forwardSpeed = v.vx * fx + v.vy * fy + v.vz * fz
      v.yawRad = atan2(fx, fz)

      if (!v.model.isBike) {
        v.bodyPitch = asin((-fy).coerceIn(-0.95f, 0.95f))
        v.bodyRoll = asin((tmpBasis.m10).coerceIn(-0.95f, 0.95f))
      }

      // Check wheel ground contact & spin
      var anyGrounded = false
      for (i in 0 until rv.numWheels) {
        val w = rv.getWheelInfo(i)
        if (w.raycastInfo.isInContact) anyGrounded = true
      }
      v.grounded = anyGrounded
      v.wheelSpinRad += (v.forwardSpeed / v.model.wheelRadius) * dt

      // Detect strong collision delta for GTA wanted/impact feedback
      val curSpeed = hypot(v.vx, v.vz)
      val speedDrop = handle.lastSpeedMps - curSpeed
      if (speedDrop > 11f) {
        onImpact(v, speedDrop)
      }
      handle.lastSpeedMps = curSpeed
    }
  }

  fun teleportVehicle(v: VehicleEntity, x: Float, y: Float, z: Float, yawRad: Float) {
    v.x = x
    v.y = y
    v.z = z
    v.yawRad = yawRad
    v.vx = 0f
    v.vy = 0f
    v.vz = 0f
    v.forwardSpeed = 0f

    val handle = vehicleHandles[v.id] ?: return
    val t = Transform()
    t.setIdentity()
    t.origin.set(x, y + 0.2f, z)
    val q = Quat4f()
    setYawQuat(q, yawRad)
    t.setRotation(q)
    handle.chassisBody.setWorldTransform(t)
    handle.chassisBody.setLinearVelocity(Vector3f(0f, 0f, 0f))
    handle.chassisBody.setAngularVelocity(Vector3f(0f, 0f, 0f))
    handle.chassisBody.clearForces()
    handle.lastSpeedMps = 0f
  }

  private fun setYawQuat(out: Quat4f, yawRad: Float) {
    val half = yawRad * 0.5f
    out.set(0f, sin(half), 0f, cos(half))
  }
}
