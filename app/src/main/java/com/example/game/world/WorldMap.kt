package com.example.game.world

import com.example.game.model.BuildingDef
import com.example.game.model.CheckpointDef
import com.example.game.model.PalmTreeDef
import com.example.game.model.PickupDef
import com.example.game.model.RampDef
import com.example.game.model.StreetLampDef
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object WorldMap {
  // World dimensions: -340..340 in Z, -340..340 in X
  const val WORLD_HALF = 340f
  const val BEACH_START_X = 132f
  const val WATER_START_X = 172f

  // Major North-South Avenue X centers (each 18m wide)
  val avenueXs = floatArrayOf(-240f, -120f, 0f, 114f)
  // Major East-West Boulevard Z centers (each 18m wide)
  val streetZs = floatArrayOf(-220f, -110f, 0f, 110f, 220f)
  const val ROAD_HALF_WIDTH = 9.5f

  val buildings: List<BuildingDef> = buildCityBuildings()
  val ramps: List<RampDef> = buildStuntRamps()
  val palms: List<PalmTreeDef> = buildPalmTrees()
  val streetLamps: List<StreetLampDef> = buildStreetLamps()
  val pickups: List<PickupDef> = buildCashPickups()
  val checkpoints: List<CheckpointDef> = buildTimeTrialCircuit()

  private fun buildCityBuildings(): List<BuildingDef> {
    val list = mutableListOf<BuildingDef>()

    // Grid blocks between avenues (-240, -120, 0, 114) and streets (-220, -110, 0, 110, 220)
    val xIntervals = listOf(-222f to -138f, -102f to -18f, 18f to 96f)
    val zIntervals = listOf(-202f to -128f, -92f to -18f, 18f to 92f, 128f to 202f)

    val palettes =
      listOf(
        floatArrayOf(0.24f, 0.27f, 0.34f, 0.14f, 0.72f, 0.92f), // Steel & cyan glass
        floatArrayOf(0.32f, 0.28f, 0.26f, 0.95f, 0.58f, 0.22f), // Warm sandstone & amber
        floatArrayOf(0.18f, 0.20f, 0.26f, 0.95f, 0.28f, 0.45f), // Dark obsidian & neon coral
        floatArrayOf(0.36f, 0.38f, 0.42f, 0.72f, 0.85f, 0.95f), // Silver skyscraper
        floatArrayOf(0.28f, 0.22f, 0.30f, 0.55f, 0.35f, 0.95f), // Dusk tower
      )

    var idx = 0
    for ((xi, xr) in xIntervals.withIndex()) {
      for ((zi, zr) in zIntervals.withIndex()) {
        // Leave central stunt plaza open at (-102..-18, 18..92)
        if (xi == 1 && zi == 2) continue

        val xMid = (xr.first + xr.second) * 0.5f
        val zMid = (zr.first + zr.second) * 0.5f
        val pad = 5f

        // Two or four buildings per city block with alleyway between them
        val splitX = (xi + zi) % 2 == 0
        if (splitX) {
          val p1 = palettes[idx++ % palettes.size]
          val h1 = if (xi == 1) 52f + ((idx * 17) % 42) else 28f + ((idx * 13) % 34)
          list.add(
            BuildingDef(
              minX = xr.first + pad,
              maxX = xMid - 4f,
              minZ = zr.first + pad,
              maxZ = zr.second - pad,
              height = h1,
              tierHeight = h1 * 0.65f,
              baseR = p1[0],
              baseG = p1[1],
              baseB = p1[2],
              accentR = p1[3],
              accentG = p1[4],
              accentB = p1[5],
            )
          )
          val p2 = palettes[idx++ % palettes.size]
          val h2 = if (xi == 2) 30f + ((idx * 19) % 28) else 42f + ((idx * 11) % 36)
          list.add(
            BuildingDef(
              minX = xMid + 4f,
              maxX = xr.second - pad,
              minZ = zr.first + pad,
              maxZ = zr.second - pad,
              height = h2,
              tierHeight = h2 * 0.7f,
              baseR = p2[0],
              baseG = p2[1],
              baseB = p2[2],
              accentR = p2[3],
              accentG = p2[4],
              accentB = p2[5],
            )
          )
        } else {
          val p1 = palettes[idx++ % palettes.size]
          val h1 = 36f + ((idx * 15) % 44)
          list.add(
            BuildingDef(
              minX = xr.first + pad,
              maxX = xr.second - pad,
              minZ = zr.first + pad,
              maxZ = zMid - 4f,
              height = h1,
              tierHeight = h1 * 0.6f,
              baseR = p1[0],
              baseG = p1[1],
              baseB = p1[2],
              accentR = p1[3],
              accentG = p1[4],
              accentB = p1[5],
            )
          )
          val p2 = palettes[idx++ % palettes.size]
          val h2 = 26f + ((idx * 13) % 32)
          list.add(
            BuildingDef(
              minX = xr.first + pad,
              maxX = xr.second - pad,
              minZ = zMid + 4f,
              maxZ = zr.second - pad,
              height = h2,
              tierHeight = h2 * 0.68f,
              baseR = p2[0],
              baseG = p2[1],
              baseB = p2[2],
              accentR = p2[3],
              accentG = p2[4],
              accentB = p2[5],
            )
          )
        }
      }
    }

    // Coastal Resort Tower & Pier Pavilion on Ocean Blvd (non-blocking boardwalk strip)
    list.add(
      BuildingDef(
        minX = 18f,
        maxX = 88f,
        minZ = -286f,
        maxZ = -242f,
        height = 46f,
        tierHeight = 30f,
        baseR = 0.88f,
        baseG = 0.82f,
        baseB = 0.76f,
        accentR = 0.08f,
        accentG = 0.86f,
        accentB = 0.94f,
      )
    )
    list.add(
      BuildingDef(
        minX = -96f,
        maxX = -22f,
        minZ = 242f,
        maxZ = 286f,
        height = 58f,
        tierHeight = 38f,
        baseR = 0.22f,
        baseG = 0.24f,
        baseB = 0.30f,
        accentR = 1.0f,
        accentG = 0.42f,
        accentB = 0.22f,
      )
    )

    return list
  }

  private fun buildStuntRamps(): List<RampDef> =
    listOf(
      // Ocean Boulevard Pier Jump facing East toward beach
      RampDef(x = 128f, z = 42f, yawRad = (Math.PI / 2).toFloat(), width = 8.5f, length = 16f, height = 3.6f),
      // Ocean Blvd Northbound speed kicker
      RampDef(x = 114f, z = -64f, yawRad = 0f, width = 7.5f, length = 15f, height = 3.2f),
      // Central Stunt Plaza opposing kickers (near -60, 55)
      RampDef(x = -60f, z = 36f, yawRad = 0f, width = 9.0f, length = 16f, height = 3.8f),
      RampDef(x = -60f, z = 74f, yawRad = Math.PI.toFloat(), width = 9.0f, length = 16f, height = 3.8f),
      // Downtown Avenue 0 bridge jump
      RampDef(x = 0f, z = -165f, yawRad = Math.PI.toFloat(), width = 8.0f, length = 15f, height = 3.4f),
      // West Industrial ramp
      RampDef(x = -120f, z = 165f, yawRad = 0f, width = 8.0f, length = 15f, height = 3.5f),
    )

  private fun buildPalmTrees(): List<PalmTreeDef> {
    val palms = mutableListOf<PalmTreeDef>()
    // Line Ocean Boulevard & Beachfront
    for (z in -260..260 step 26) {
      if (abs(z) % 110 > 14) {
        palms.add(PalmTreeDef(x = 127f, z = z.toFloat(), height = 8.5f + ((z * 7) and 3), rotY = z * 0.4f))
        palms.add(PalmTreeDef(x = 148f, z = (z + 13).toFloat(), height = 9.2f + ((z * 3) and 2), rotY = z * 0.7f))
      }
    }
    // Stunt Plaza corner palms
    palms.add(PalmTreeDef(x = -96f, z = 24f, height = 8.8f, rotY = 0.5f))
    palms.add(PalmTreeDef(x = -24f, z = 24f, height = 9.4f, rotY = 1.9f))
    palms.add(PalmTreeDef(x = -96f, z = 86f, height = 8.6f, rotY = 3.1f))
    palms.add(PalmTreeDef(x = -24f, z = 86f, height = 9.1f, rotY = 4.4f))
    return palms
  }

  private fun buildStreetLamps(): List<StreetLampDef> {
    val lamps = mutableListOf<StreetLampDef>()
    for (ax in avenueXs) {
      for (z in -240..240 step 48) {
        if (streetZs.none { abs(it - z) < 16f }) {
          lamps.add(StreetLampDef(x = ax - 10.6f, z = z.toFloat(), rotY = (Math.PI / 2).toFloat()))
          lamps.add(StreetLampDef(x = ax + 10.6f, z = z.toFloat(), rotY = (-Math.PI / 2).toFloat()))
        }
      }
    }
    return lamps
  }

  private fun buildCashPickups(): List<PickupDef> =
    listOf(
      PickupDef(1, 114f, 55f, 500),
      PickupDef(2, 138f, 42f, 1000), // after pier ramp jump!
      PickupDef(3, 0f, -55f, 500),
      PickupDef(4, -60f, 55f, 750), // stunt plaza center
      PickupDef(5, -120f, 0f, 500),
      PickupDef(6, -240f, -110f, 750),
      PickupDef(7, 114f, -165f, 500),
      PickupDef(8, 0f, 165f, 500),
      PickupDef(9, 152f, -110f, 1000),
      PickupDef(10, -120f, -220f, 750),
    )

  private fun buildTimeTrialCircuit(): List<CheckpointDef> =
    listOf(
      CheckpointDef(114f, -45f),
      CheckpointDef(114f, -165f),
      CheckpointDef(55f, -220f),
      CheckpointDef(-60f, -220f),
      CheckpointDef(-120f, -165f),
      CheckpointDef(-120f, -55f),
      CheckpointDef(-120f, 55f),
      CheckpointDef(-60f, 110f),
      CheckpointDef(0f, 165f),
      CheckpointDef(60f, 220f),
      CheckpointDef(114f, 165f),
      CheckpointDef(114f, 40f),
    )

  fun isOnRoad(x: Float, z: Float): Boolean {
    for (ax in avenueXs) {
      if (abs(x - ax) <= ROAD_HALF_WIDTH && abs(z) <= 275f) return true
    }
    for (sz in streetZs) {
      if (abs(z - sz) <= ROAD_HALF_WIDTH && x in -275f..124f) return true
    }
    return false
  }

  fun isWater(x: Float, z: Float): Boolean = x > WATER_START_X || abs(x) > WORLD_HALF || abs(z) > WORLD_HALF

  fun getSurfaceHeight(x: Float, z: Float): Float {
    var maxH = 0f
    // Slight beach slope toward ocean
    if (x > BEACH_START_X) {
      val t = ((x - BEACH_START_X) / (WATER_START_X - BEACH_START_X)).coerceIn(0f, 1.3f)
      maxH = -0.35f * t
    }
    for (ramp in ramps) {
      val dx = x - ramp.x
      val dz = z - ramp.z
      val c = cos(-ramp.yawRad)
      val s = sin(-ramp.yawRad)
      val localX = dx * c - dz * s
      val localZ = dx * s + dz * c
      val halfW = ramp.width * 0.5f
      val halfL = ramp.length * 0.5f
      if (abs(localX) <= halfW && abs(localZ) <= halfL) {
        // Rises from -halfL (bottom) to +halfL (lip)
        val t = ((localZ + halfL) / ramp.length).coerceIn(0f, 1f)
        val h = t * ramp.height
        if (h > maxH) maxH = h
      }
    }
    return maxH
  }

  fun resolveBuildingCollision(x: Float, z: Float, radius: Float): Pair<Float, Float>? {
    var rx = x
    var rz = z
    var hit = false
    for (b in buildings) {
      val closestX = rx.coerceIn(b.minX, b.maxX)
      val closestZ = rz.coerceIn(b.minZ, b.maxZ)
      val dx = rx - closestX
      val dz = rz - closestZ
      val distSq = dx * dx + dz * dz
      if (distSq < radius * radius) {
        hit = true
        val dist = kotlin.math.sqrt(distSq)
        if (dist > 0.0001f) {
          val push = radius - dist
          rx += (dx / dist) * push
          rz += (dz / dist) * push
        } else {
          // Inside AABB fallback push to nearest edge
          val dl = abs(rx - b.minX)
          val dr = abs(b.maxX - rx)
          val dt = abs(rz - b.minZ)
          val db = abs(b.maxZ - rz)
          val m = minOf(dl, dr, dt, db)
          when (m) {
            dl -> rx = b.minX - radius
            dr -> rx = b.maxX + radius
            dt -> rz = b.minZ - radius
            else -> rz = b.maxZ + radius
          }
        }
      }
    }
    return if (hit) rx to rz else null
  }

  fun getZoneName(x: Float, z: Float): String =
    when {
      x >= BEACH_START_X -> "Vice Beach"
      x >= 95f -> "Ocean Boulevard"
      x in -105f..-15f && z in 15f..95f -> "Stunt Plaza"
      x in -130f..30f && z in -120f..120f -> "Downtown Financial"
      z < -120f -> "North Palms"
      z > 120f -> "Harbor District"
      else -> "Westside Industrial"
    }
}
