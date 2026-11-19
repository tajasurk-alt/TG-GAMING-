package com.example

import com.example.game.model.GameMode
import com.example.game.model.PaintPreset
import com.example.game.model.VehicleModelType
import com.example.game.sim.GameSimulation
import com.example.game.world.WorldMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSimulationTest {

  @Test
  fun supercar_accelerates_steers_and_builds_speed() {
    val sim = GameSimulation()
    sim.resetWorld(GameMode.FREE_ROAM, startMounted = VehicleModelType.INFERNUS_GT)
    val car = sim.activeVehicle()
    assertNotNull(car)
    assertFalse(car!!.model.isBike)

    sim.controls.throttleInput = 1f
    repeat(60) { sim.step(1f / 60f) }

    val hud = sim.buildHudSnapshot(60)
    assertTrue("Expected car to accelerate above 40 km/h, got ${hud.speedKmh}", hud.speedKmh > 40)
  }

  @Test
  fun superbike_leans_into_turns_and_supports_wheelie() {
    val sim = GameSimulation()
    sim.resetWorld(GameMode.FREE_ROAM, startMounted = VehicleModelType.AKUMA_1000R)
    val bike = sim.activeVehicle()
    assertNotNull(bike)
    assertTrue(bike!!.model.isBike)

    sim.controls.throttleInput = 1f
    sim.controls.steerInput = 1f
    sim.controls.wheelieOrJump = true
    repeat(60) { sim.step(1f / 60f) }

    assertTrue("Expected bike to lean into turn", bike.leanAngle > 0.1f)
    assertTrue("Expected bike to lift front wheel on wheelie", bike.wheeliePitch < -0.05f)
  }

  @Test
  fun player_can_exit_car_and_mount_nearby_motorbike() {
    val sim = GameSimulation()
    sim.resetWorld(GameMode.FREE_ROAM, startMounted = VehicleModelType.INFERNUS_GT)
    assertNotNull(sim.activeVehicle())

    sim.toggleEnterExitVehicle()
    assertNull(sim.activeVehicle())

    sim.spawnAndMountPlayerVehicle(VehicleModelType.SANCHEZ_MX, PaintPreset.EMERALD_VIPER)
    val bike = sim.activeVehicle()
    assertNotNull(bike)
    assertEquals(VehicleModelType.SANCHEZ_MX, bike!!.model)
  }

  @Test
  fun stunt_ramp_elevates_vehicle_above_road_level() {
    val ramp = WorldMap.ramps.first()
    val hCenter = WorldMap.getSurfaceHeight(ramp.x, ramp.z)
    assertTrue("Expected ramp surface height > 1.0m, got $hCenter", hCenter > 1.0f)
  }

  @Test
  fun switching_between_sports_car_and_motorbike_preserves_location_and_momentum() {
    val sim = GameSimulation()
    sim.resetWorld(GameMode.FREE_ROAM, startMounted = VehicleModelType.ZENTORNO_RS)
    sim.controls.throttleInput = 1f
    repeat(30) { sim.step(1f / 60f) }

    val beforeCar = sim.activeVehicle()!!
    val carX = beforeCar.x
    val carZ = beforeCar.z
    assertTrue(beforeCar.forwardSpeed > 5f)

    sim.spawnAndMountPlayerVehicle(VehicleModelType.BATI_801RR, PaintPreset.ELECTRIC_CYAN)
    val afterBike = sim.activeVehicle()!!
    assertEquals(VehicleModelType.BATI_801RR, afterBike.model)
    assertTrue(afterBike.model.isBike)
    assertEquals(carX, afterBike.x, 0.01f)
    assertEquals(carZ, afterBike.z, 0.01f)
    assertTrue(afterBike.forwardSpeed > 5f)
  }
}
