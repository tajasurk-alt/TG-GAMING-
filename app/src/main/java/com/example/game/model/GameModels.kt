package com.example.game.model

import androidx.compose.ui.graphics.Color

enum class VehicleModelType(
  val displayName: String,
  val subtitle: String,
  val isBike: Boolean,
  val isPolice: Boolean,
  val maxSpeedMps: Float,
  val accelMps2: Float,
  val brakeMps2: Float,
  val turnRateRad: Float,
  val lateralGrip: Float,
  val length: Float,
  val width: Float,
  val wheelRadius: Float,
  val engineBaseHz: Float,
) {
  INFERNUS_GT(
    displayName = "Infernus GT",
    subtitle = "Twin-Turbo Supercar",
    isBike = false,
    isPolice = false,
    maxSpeedMps = 62f,
    accelMps2 = 27f,
    brakeMps2 = 38f,
    turnRateRad = 2.4f,
    lateralGrip = 8.5f,
    length = 4.4f,
    width = 2.0f,
    wheelRadius = 0.38f,
    engineBaseHz = 56f,
  ),
  ZENTORNO_RS(
    displayName = "Zentorno RS",
    subtitle = "Apex Carbon Hypercar",
    isBike = false,
    isPolice = false,
    maxSpeedMps = 72f,
    accelMps2 = 32f,
    brakeMps2 = 42f,
    turnRateRad = 2.55f,
    lateralGrip = 9.4f,
    length = 4.45f,
    width = 2.04f,
    wheelRadius = 0.38f,
    engineBaseHz = 62f,
  ),
  BANSHEE_V8(
    displayName = "Banshee V8",
    subtitle = "Widebody Muscle Coupe",
    isBike = false,
    isPolice = false,
    maxSpeedMps = 56f,
    accelMps2 = 24f,
    brakeMps2 = 34f,
    turnRateRad = 2.25f,
    lateralGrip = 6.8f,
    length = 4.5f,
    width = 2.05f,
    wheelRadius = 0.40f,
    engineBaseHz = 44f,
  ),
  AKUMA_1000R(
    displayName = "Akuma 1000R",
    subtitle = "Track-Spec Superbike",
    isBike = true,
    isPolice = false,
    maxSpeedMps = 66f,
    accelMps2 = 34f,
    brakeMps2 = 42f,
    turnRateRad = 3.1f,
    lateralGrip = 10.5f,
    length = 2.25f,
    width = 0.78f,
    wheelRadius = 0.34f,
    engineBaseHz = 78f,
  ),
  BATI_801RR(
    displayName = "Bati 801RR",
    subtitle = "Carbon GP Superbike",
    isBike = true,
    isPolice = false,
    maxSpeedMps = 70f,
    accelMps2 = 37f,
    brakeMps2 = 44f,
    turnRateRad = 3.25f,
    lateralGrip = 11.0f,
    length = 2.22f,
    width = 0.76f,
    wheelRadius = 0.34f,
    engineBaseHz = 86f,
  ),
  SANCHEZ_MX(
    displayName = "Sanchez MX",
    subtitle = "Street & Stunt Dirt Bike",
    isBike = true,
    isPolice = false,
    maxSpeedMps = 50f,
    accelMps2 = 29f,
    brakeMps2 = 36f,
    turnRateRad = 3.4f,
    lateralGrip = 9.2f,
    length = 2.15f,
    width = 0.74f,
    wheelRadius = 0.36f,
    engineBaseHz = 64f,
  ),
  VICE_PD(
    displayName = "Vice PD Interceptor",
    subtitle = "Pursuit Patrol Cruiser",
    isBike = false,
    isPolice = true,
    maxSpeedMps = 58f,
    accelMps2 = 25f,
    brakeMps2 = 36f,
    turnRateRad = 2.4f,
    lateralGrip = 8.0f,
    length = 4.6f,
    width = 2.05f,
    wheelRadius = 0.39f,
    engineBaseHz = 50f,
  ),
  URBAN_SEDAN(
    displayName = "Asterope Sedan",
    subtitle = "City Street Coupe",
    isBike = false,
    isPolice = false,
    maxSpeedMps = 44f,
    accelMps2 = 18f,
    brakeMps2 = 28f,
    turnRateRad = 2.1f,
    lateralGrip = 7.4f,
    length = 4.3f,
    width = 1.92f,
    wheelRadius = 0.36f,
    engineBaseHz = 48f,
  ),
  ;

  val topSpeedKmh: Int get() = (maxSpeedMps * 3.6f).toInt()
  val accelRating: Float get() = (accelMps2 / 38f).coerceIn(0.2f, 1f)
  val handlingRating: Float get() = (lateralGrip / 11f).coerceIn(0.2f, 1f)
}

enum class PaintPreset(
  val label: String,
  val r: Float,
  val g: Float,
  val b: Float,
  val uiColor: Color,
) {
  SUNSET_CRIMSON("Sunset Crimson", 0.88f, 0.14f, 0.18f, Color(0xFFE0242E)),
  ELECTRIC_CYAN("Electric Cyan", 0.04f, 0.76f, 0.94f, Color(0xFF0AC2F0)),
  PEARL_GOLD("Solar Gold", 0.95f, 0.68f, 0.08f, Color(0xFFF2AD14)),
  EMERALD_VIPER("Viper Emerald", 0.10f, 0.78f, 0.42f, Color(0xFF1AC76B)),
  OBSIDIAN_BLACK("Obsidian Matte", 0.11f, 0.12f, 0.15f, Color(0xFF282C35)),
  ARCTIC_PEARL("Arctic Pearl", 0.90f, 0.93f, 0.97f, Color(0xFFE6EDF7)),
}

enum class TimeOfDay(
  val label: String,
  val sunDir: FloatArray,
  val sunColor: FloatArray,
  val skyZenith: FloatArray,
  val skyHorizon: FloatArray,
  val fogColor: FloatArray,
  val ambientStrength: Float,
  val windowGlow: Float,
  val autoHeadlights: Boolean,
) {
  SUNSET(
    label = "Golden Sunset",
    sunDir = floatArrayOf(0.75f, 0.32f, -0.58f),
    sunColor = floatArrayOf(1.0f, 0.68f, 0.40f),
    skyZenith = floatArrayOf(0.10f, 0.14f, 0.32f),
    skyHorizon = floatArrayOf(0.96f, 0.46f, 0.25f),
    fogColor = floatArrayOf(0.74f, 0.42f, 0.34f),
    ambientStrength = 0.44f,
    windowGlow = 0.45f,
    autoHeadlights = true,
  ),
  NOON(
    label = "Coastal Noon",
    sunDir = floatArrayOf(0.35f, 0.88f, -0.32f),
    sunColor = floatArrayOf(1.0f, 0.97f, 0.92f),
    skyZenith = floatArrayOf(0.16f, 0.46f, 0.86f),
    skyHorizon = floatArrayOf(0.66f, 0.84f, 0.96f),
    fogColor = floatArrayOf(0.62f, 0.78f, 0.90f),
    ambientStrength = 0.62f,
    windowGlow = 0.08f,
    autoHeadlights = false,
  ),
  MIDNIGHT(
    label = "Midnight Neon",
    sunDir = floatArrayOf(-0.45f, 0.65f, 0.55f),
    sunColor = floatArrayOf(0.36f, 0.46f, 0.74f),
    skyZenith = floatArrayOf(0.02f, 0.03f, 0.09f),
    skyHorizon = floatArrayOf(0.09f, 0.12f, 0.24f),
    fogColor = floatArrayOf(0.06f, 0.08f, 0.16f),
    ambientStrength = 0.26f,
    windowGlow = 1.0f,
    autoHeadlights = true,
  ),
}

enum class CameraMode(val label: String) {
  CHASE("Chase Cam"),
  CINEMATIC_LOW("Action Low"),
  BUMPER("Hood / Rider"),
}

enum class GameMode(val label: String) {
  FREE_ROAM("Free Roam"),
  TIME_TRIAL("Coastal Sprint"),
}

data class BuildingDef(
  val minX: Float,
  val maxX: Float,
  val minZ: Float,
  val maxZ: Float,
  val height: Float,
  val tierHeight: Float,
  val baseR: Float,
  val baseG: Float,
  val baseB: Float,
  val accentR: Float,
  val accentG: Float,
  val accentB: Float,
) {
  val centerX: Float get() = (minX + maxX) * 0.5f
  val centerZ: Float get() = (minZ + maxZ) * 0.5f
  val widthX: Float get() = maxX - minX
  val depthZ: Float get() = maxZ - minZ
}

data class RampDef(
  val x: Float,
  val z: Float,
  val yawRad: Float,
  val width: Float,
  val length: Float,
  val height: Float,
)

data class PalmTreeDef(val x: Float, val z: Float, val height: Float, val rotY: Float)

data class StreetLampDef(val x: Float, val z: Float, val rotY: Float)

data class PickupDef(val id: Int, val x: Float, val z: Float, val amount: Int)

data class CheckpointDef(val x: Float, val z: Float)

data class VehicleEntity(
  val id: Int,
  val model: VehicleModelType,
  var x: Float,
  var y: Float,
  var z: Float,
  var yawRad: Float,
  var vx: Float = 0f,
  var vy: Float = 0f,
  var vz: Float = 0f,
  var forwardSpeed: Float = 0f,
  var steerAngle: Float = 0f,
  var leanAngle: Float = 0f,
  var wheeliePitch: Float = 0f,
  var bodyPitch: Float = 0f,
  var bodyRoll: Float = 0f,
  var wheelSpinRad: Float = 0f,
  var paintR: Float,
  var paintG: Float,
  var paintB: Float,
  var isPolice: Boolean = false,
  var isAiTraffic: Boolean = false,
  var aiTargetYaw: Float = yawRad,
  var aiSpeedTarget: Float = 15f,
  var headlightsOn: Boolean = true,
  var isDrifting: Boolean = false,
  var nosActive: Boolean = false,
  var grounded: Boolean = true,
)

data class MinimapBlip(
  val x: Float,
  val z: Float,
  val isPolice: Boolean,
  val isBike: Boolean,
  val isPickup: Boolean,
  val isCheckpoint: Boolean,
)

data class HudSnapshot(
  val speedKmh: Int = 0,
  val gearLabel: String = "N",
  val rpmNormalized: Float = 0f,
  val nitroNormalized: Float = 1f,
  val cash: Int = 2500,
  val wantedStars: Int = 0,
  val wantedCooldownRatio: Float = 0f,
  val isInVehicle: Boolean = false,
  val currentVehicleModel: VehicleModelType? = null,
  val currentVehicleName: String = "",
  val currentVehicleIsBike: Boolean = false,
  val nearbyVehiclePrompt: String? = null,
  val headlightsOn: Boolean = true,
  val playerX: Float = 0f,
  val playerZ: Float = 0f,
  val playerYawRad: Float = 0f,
  val zoneName: String = "Ocean Boulevard",
  val bannerMessage: String? = null,
  val activeCheckpointIndex: Int = 0,
  val totalCheckpoints: Int = 0,
  val timeTrialActive: Boolean = false,
  val timeTrialSeconds: Float = 0f,
  val bestLapSeconds: Float? = null,
  val cameraMode: CameraMode = CameraMode.CHASE,
  val timeOfDay: TimeOfDay = TimeOfDay.SUNSET,
  val blips: List<MinimapBlip> = emptyList(),
  val fps: Int = 60,
)

class ControlState {
  @Volatile var steerInput: Float = 0f // -1 (left) to +1 (right)
  @Volatile var throttleInput: Float = 0f // 0..1
  @Volatile var brakeInput: Float = 0f // 0..1
  @Volatile var handbrake: Boolean = false
  @Volatile var nosBoost: Boolean = false
  @Volatile var wheelieOrJump: Boolean = false
  @Volatile var walkJoyX: Float = 0f // -1..1
  @Volatile var walkJoyY: Float = 0f // -1..1
  @Volatile var sprint: Boolean = false
}
