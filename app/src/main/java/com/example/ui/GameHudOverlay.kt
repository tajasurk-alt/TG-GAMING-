package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Garage
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.model.ControlState
import com.example.game.model.HudSnapshot
import com.example.game.world.WorldMap
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AsphaltSurface
import com.example.ui.theme.CashEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.PoliceBlue
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WantedCrimson
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun GameHudOverlay(
  hud: HudSnapshot,
  controls: ControlState,
  isMuted: Boolean,
  onToggleMute: () -> Unit,
  onCycleCamera: () -> Unit,
  onTogglePause: () -> Unit,
  onOpenGarage: () -> Unit,
  onEnterExitVehicle: () -> Unit,
  onToggleHeadlights: () -> Unit,
) {
  BoxWithConstraints(
    modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp)
  ) {
    val isCompact = maxWidth < 520.dp
    val mapSize = if (isCompact) 112.dp else 136.dp

    // TOP ROW: Minimap (Left) + Cash/Stars/Toolbar (Right)
    Row(
      modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Top,
    ) {
      // Live Radar Minimap + Zone Name
      Column(horizontalAlignment = Alignment.Start) {
        MinimapRadar(hud = hud, size = mapSize)
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
          color = ObsidianBg.copy(alpha = 0.72f),
          shape = RoundedCornerShape(6.dp),
          border = BorderStroke(1.dp, OutlineSubtle),
        ) {
          Text(
            text = hud.zoneName.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = NeonCyan,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
      }

      // Top Right: Cash, Wanted Stars, Time Trial Timer, Quick Action Icons
      Column(horizontalAlignment = Alignment.End) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          // Cash pill
          Surface(
            color = ObsidianBg.copy(alpha = 0.78f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, CashEmerald.copy(alpha = 0.5f)),
          ) {
            Text(
              text = "$${"%,d".format(hud.cash)}",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
              color = CashEmerald,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
          }

          // Toolbar Icons (min 48dp touch targets)
          HudIconBtn(
            icon = Icons.Filled.Garage,
            cd = stringResource(R.string.cd_garage),
            tag = "hud_garage_button",
            tint = AmberGold,
            onClick = onOpenGarage,
          )
          HudIconBtn(
            icon = Icons.Filled.Videocam,
            cd = stringResource(R.string.cd_camera),
            tag = "hud_camera_button",
            tint = NeonCyan,
            onClick = onCycleCamera,
          )
          HudIconBtn(
            icon = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
            cd = if (isMuted) "Unmute audio" else "Mute audio",
            tag = "hud_mute_button",
            tint = TextPrimary,
            onClick = onToggleMute,
          )
          HudIconBtn(
            icon = Icons.Filled.Pause,
            cd = stringResource(R.string.cd_pause),
            tag = "hud_pause_button",
            tint = SunsetCoral,
            onClick = onTogglePause,
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 5-Star Wanted Level Row
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp),
          modifier =
            Modifier.clip(RoundedCornerShape(8.dp))
              .background(ObsidianBg.copy(alpha = 0.65f))
              .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
          for (star in 1..5) {
            val active = star <= hud.wantedStars
            Icon(
              imageVector = if (active) Icons.Filled.Star else Icons.Outlined.StarOutline,
              contentDescription = null,
              tint =
                if (active) {
                  if ((System.currentTimeMillis() / 280L) % 2L == 0L) WantedCrimson else PoliceBlue
                } else {
                  TextSecondary.copy(alpha = 0.35f)
                },
              modifier = Modifier.size(18.dp),
            )
          }
          if (hud.wantedStars > 0 && hud.wantedCooldownRatio > 0f) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "EVADING ${(hud.wantedCooldownRatio * 100).toInt()}%",
              style = MaterialTheme.typography.labelSmall,
              color = NeonCyan,
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Live In-Game Ride Switcher Pill (Cars & Motorbikes)
        Surface(
          color = ObsidianBg.copy(alpha = 0.80f),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.2.dp, AmberGold.copy(alpha = 0.75f)),
          modifier =
            Modifier.height(48.dp)
              .testTag("open_vehicle_menu_button")
              .clickable { onOpenGarage() },
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 12.dp),
          ) {
            Icon(
              imageVector =
                if (hud.currentVehicleIsBike) Icons.Filled.TwoWheeler
                else Icons.Filled.DirectionsCar,
              contentDescription = stringResource(R.string.cd_garage),
              tint = AmberGold,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = if (hud.isInVehicle) hud.currentVehicleName else "SWITCH RIDE",
              style = MaterialTheme.typography.labelLarge,
              color = TextPrimary,
            )
            Icon(
              imageVector = Icons.Filled.SwapHoriz,
              contentDescription = null,
              tint = NeonCyan,
              modifier = Modifier.size(18.dp),
            )
          }
        }

        // Time Trial Timer Pill
        if (hud.timeTrialActive) {
          Spacer(modifier = Modifier.height(6.dp))
          Surface(
            color = ObsidianBg.copy(alpha = 0.78f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
          ) {
            Text(
              text = "CP ${hud.activeCheckpointIndex + 1}/${hud.totalCheckpoints} • ${"%.1f".format(hud.timeTrialSeconds)}s",
              style = MaterialTheme.typography.labelLarge,
              color = NeonCyan,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
          }
        }
      }
    }

    // CENTER EVENT BANNER (Stunt jump / Hijack / Escaped / Checkpoint)
    AnimatedVisibility(
      visible = hud.bannerMessage != null,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.align(Alignment.TopCenter).padding(top = if (isCompact) 140.dp else 90.dp),
    ) {
      Surface(
        color = ObsidianBg.copy(alpha = 0.85f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, AmberGold),
      ) {
        Text(
          text = hud.bannerMessage.orEmpty(),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
          color = AmberGold,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
      }
    }

    // BOTTOM CONTROLS AREA
    Column(
      modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Prompt to Enter/Ride Nearby Vehicle when on foot
      AnimatedVisibility(visible = !hud.isInVehicle && hud.nearbyVehiclePrompt != null) {
        Surface(
          color = SunsetCoral,
          shape = RoundedCornerShape(24.dp),
          modifier =
            Modifier.padding(bottom = 10.dp)
              .testTag("enter_nearby_vehicle_button")
              .clickable { onEnterExitVehicle() },
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
          ) {
            Icon(
              imageVector =
                if (hud.nearbyVehiclePrompt?.contains("Akuma") == true ||
                  hud.nearbyVehiclePrompt?.contains("Sanchez") == true
                )
                  Icons.Filled.TwoWheeler
                else Icons.Filled.DirectionsCar,
              contentDescription = null,
              tint = ObsidianBg,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = hud.nearbyVehiclePrompt.orEmpty().uppercase(),
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
              color = ObsidianBg,
            )
          }
        }
      }

      if (hud.isInVehicle) {
        VehicleControlsRow(
          hud = hud,
          controls = controls,
          isCompact = isCompact,
          onEnterExitVehicle = onEnterExitVehicle,
          onToggleHeadlights = onToggleHeadlights,
        )
      } else {
        OnFootControlsRow(
          controls = controls,
          canEnterNearby = hud.nearbyVehiclePrompt != null,
          onEnterExitVehicle = onEnterExitVehicle,
          onOpenGarage = onOpenGarage,
        )
      }
    }
  }
}

@Composable
private fun VehicleControlsRow(
  hud: HudSnapshot,
  controls: ControlState,
  isCompact: Boolean,
  onEnterExitVehicle: () -> Unit,
  onToggleHeadlights: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Bottom,
  ) {
    // LEFT: Steering Left / Right + Headlights + Wheelie/Drift helper
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallTapChip(
          icon = Icons.Filled.Highlight,
          label = if (hud.headlightsOn) "LIGHTS ON" else "LIGHTS",
          active = hud.headlightsOn,
          tag = "headlights_button",
          onClick = onToggleHeadlights,
        )
        SmallHoldChip(
          label = if (hud.currentVehicleIsBike) "WHEELIE" else "DRIFT",
          accent = AmberGold,
          tag = "drift_wheelie_button",
          onHoldChange = { held ->
            if (hud.currentVehicleIsBike) controls.wheelieOrJump = held
            else controls.handbrake = held
          },
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        HoldSteerPad(
          icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
          cd = "Steer Left",
          tag = "steer_left_pad",
          size = if (isCompact) 66.dp else 76.dp,
          onHoldChange = { held ->
            controls.steerInput = if (held) -1f else if (controls.steerInput < 0f) 0f else controls.steerInput
          },
        )
        HoldSteerPad(
          icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          cd = "Steer Right",
          tag = "steer_right_pad",
          size = if (isCompact) 66.dp else 76.dp,
          onHoldChange = { held ->
            controls.steerInput = if (held) 1f else if (controls.steerInput > 0f) 0f else controls.steerInput
          },
        )
      }
    }

    // CENTER: Digital Speedometer + Gear + Nitro Bar (visible cleanly on wider or compact)
    if (!isCompact) {
      SpeedometerCluster(hud = hud)
    }

    // RIGHT: Exit + NOS + Brake/Reverse + Throttle Pedal
    Column(
      horizontalAlignment = Alignment.End,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (isCompact) {
        SpeedometerCompactPill(hud = hud)
      }
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          horizontalAlignment = Alignment.End,
        ) {
          // Exit Vehicle
          Surface(
            color = AsphaltSurface.copy(alpha = 0.85f),
            shape = CircleShape,
            border = BorderStroke(1.5.dp, SunsetCoral),
            modifier =
              Modifier.size(48.dp)
                .testTag("exit_vehicle_button")
                .clickable { onEnterExitVehicle() },
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.ExitToApp,
                contentDescription = stringResource(R.string.cd_enter_exit_vehicle),
                tint = SunsetCoral,
                modifier = Modifier.size(22.dp),
              )
            }
          }

          // NOS Boost Hold
          HoldActionCircle(
            icon = Icons.Filled.Bolt,
            label = "NOS",
            accent = NeonCyan,
            tag = "nos_boost_button",
            size = 56.dp,
            onHoldChange = { held -> controls.nosBoost = held },
          )
        }

        // Brake / Reverse Pedal
        HoldPedalPad(
          label = "BRK",
          accent = WantedCrimson,
          tag = "brake_pedal",
          width = if (isCompact) 54.dp else 64.dp,
          height = if (isCompact) 82.dp else 94.dp,
          onHoldChange = { held -> controls.brakeInput = if (held) 1f else 0f },
        )

        // Gas / Throttle Pedal
        HoldPedalPad(
          label = "GAS",
          accent = CashEmerald,
          tag = "gas_pedal",
          width = if (isCompact) 62.dp else 72.dp,
          height = if (isCompact) 104.dp else 118.dp,
          onHoldChange = { held -> controls.throttleInput = if (held) 1f else 0f },
        )
      }
    }
  }
}

@Composable
private fun OnFootControlsRow(
  controls: ControlState,
  canEnterNearby: Boolean,
  onEnterExitVehicle: () -> Unit,
  onOpenGarage: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Bottom,
  ) {
    // 360 Analog Joystick
    VirtualJoystick(
      modifier = Modifier.testTag("walk_joystick"),
      onMove = { jx, jy ->
        controls.walkJoyX = jx
        controls.walkJoyY = jy
      },
    )

    // Right On-Foot Action Buttons
    Row(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.Bottom,
    ) {
      SmallHoldChip(
        label = "SPRINT",
        accent = NeonCyan,
        tag = "sprint_button",
        onHoldChange = { held -> controls.sprint = held },
      )
      SmallHoldChip(
        label = "JUMP",
        accent = AmberGold,
        tag = "jump_button",
        onHoldChange = { held -> controls.wheelieOrJump = held },
      )
      Surface(
        color = if (canEnterNearby) SunsetCoral else AsphaltSurface.copy(alpha = 0.85f),
        shape = CircleShape,
        border = BorderStroke(1.5.dp, if (canEnterNearby) Color.White else NeonCyan),
        modifier =
          Modifier.size(62.dp)
            .testTag("onfoot_vehicle_action_button")
            .clickable {
              if (canEnterNearby) onEnterExitVehicle() else onOpenGarage()
            },
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = if (canEnterNearby) Icons.Filled.DirectionsCar else Icons.Filled.DirectionsRun,
            contentDescription = stringResource(R.string.cd_enter_exit_vehicle),
            tint = if (canEnterNearby) ObsidianBg else NeonCyan,
            modifier = Modifier.size(28.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun SpeedometerCluster(hud: HudSnapshot) {
  Surface(
    color = ObsidianBg.copy(alpha = 0.78f),
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, OutlineSubtle),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = "${hud.speedKmh}",
          style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
          color = TextPrimary,
        )
        Text(
          text = "KM/H",
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "GEAR ${hud.gearLabel}",
            style = MaterialTheme.typography.labelLarge,
            color = AmberGold,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = hud.currentVehicleName,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
          )
        }
        Spacer(modifier = Modifier.height(6.dp))
        // Nitro bar
        Box(
          modifier =
            Modifier.width(120.dp)
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp))
              .background(OutlineSubtle)
        ) {
          Box(
            modifier =
              Modifier.fillMaxWidth(hud.nitroNormalized.coerceIn(0f, 1f))
                .height(6.dp)
                .background(NeonCyan)
          )
        }
      }
    }
  }
}

@Composable
private fun SpeedometerCompactPill(hud: HudSnapshot) {
  Surface(
    color = ObsidianBg.copy(alpha = 0.78f),
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, OutlineSubtle),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
      Text(
        text = "${hud.speedKmh} KM/H",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
        color = TextPrimary,
      )
      Text(
        text = "G${hud.gearLabel}",
        style = MaterialTheme.typography.labelSmall,
        color = AmberGold,
      )
      Text(
        text = "NOS ${(hud.nitroNormalized * 100).toInt()}%",
        style = MaterialTheme.typography.labelSmall,
        color = NeonCyan,
      )
    }
  }
}

@Composable
private fun MinimapRadar(hud: HudSnapshot, size: Dp) {
  val cdMinimap = stringResource(R.string.cd_minimap)
  Box(
    modifier =
      Modifier.size(size)
        .clip(RoundedCornerShape(18.dp))
        .background(ObsidianBg.copy(alpha = 0.82f))
        .border(2.dp, OutlineSubtle, RoundedCornerShape(18.dp))
        .semantics { contentDescription = cdMinimap }
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = this.size.width * 0.5f
      val cy = this.size.height * 0.5f
      val scale = this.size.width / 260f // ~260m radar radius visible

      // Draw roads & ocean relative to player
      val rotDeg = -Math.toDegrees(hud.playerYawRad.toDouble()).toFloat() + 180f

      rotate(degrees = rotDeg, pivot = Offset(cx, cy)) {
        // Ocean on East (+X)
        val waterScreenX = cx + (WorldMap.WATER_START_X - hud.playerX) * scale
        drawRect(
          color = Color(0xFF0A3D5C),
          topLeft = Offset(waterScreenX, -this.size.height),
          size = Size(this.size.width * 3f, this.size.height * 3f),
        )

        // Avenues
        for (ax in WorldMap.avenueXs) {
          val rx = cx + (ax - hud.playerX) * scale
          drawLine(
            color = Color(0xFF44506A),
            start = Offset(rx, -this.size.height),
            end = Offset(rx, this.size.height * 2f),
            strokeWidth = 6f,
          )
        }
        // Streets
        for (sz in WorldMap.streetZs) {
          val rz = cy + (sz - hud.playerZ) * scale
          drawLine(
            color = Color(0xFF44506A),
            start = Offset(-this.size.width, rz),
            end = Offset(this.size.width * 2f, rz),
            strokeWidth = 6f,
          )
        }

        // Blips (vehicles, police, cash, checkpoint)
        for (b in hud.blips) {
          val bx = cx + (b.x - hud.playerX) * scale
          val bz = cy + (b.z - hud.playerZ) * scale
          if (hypot(bx - cx, bz - cy) < this.size.width * 0.65f) {
            val col =
              when {
                b.isCheckpoint -> NeonCyan
                b.isPolice -> WantedCrimson
                b.isPickup -> CashEmerald
                b.isBike -> AmberGold
                else -> Color.White.copy(alpha = 0.7f)
              }
            val r = if (b.isCheckpoint) 6f else if (b.isPolice || b.isPickup) 4.5f else 3f
            drawCircle(color = col, radius = r, center = Offset(bx, bz))
          }
        }
      }

      // Player center triangle
      val arrow =
        Path().apply {
          moveTo(cx, cy - 8f)
          lineTo(cx - 6f, cy + 6f)
          lineTo(cx, cy + 3f)
          lineTo(cx + 6f, cy + 6f)
          close()
        }
      drawPath(path = arrow, color = SunsetCoral)
      drawCircle(color = NeonCyan.copy(alpha = 0.25f), radius = this.size.width * 0.46f, style = Stroke(width = 1.5f))
    }
  }
}

@Composable
private fun HudIconBtn(
  icon: ImageVector,
  cd: String,
  tag: String,
  tint: Color,
  onClick: () -> Unit,
) {
  Surface(
    color = ObsidianBg.copy(alpha = 0.75f),
    shape = CircleShape,
    border = BorderStroke(1.dp, OutlineSubtle),
    modifier = Modifier.size(48.dp).testTag(tag),
  ) {
    IconButton(onClick = onClick) {
      Icon(imageVector = icon, contentDescription = cd, tint = tint, modifier = Modifier.size(22.dp))
    }
  }
}

@Composable
private fun HoldSteerPad(
  icon: ImageVector,
  cd: String,
  tag: String,
  size: Dp,
  onHoldChange: (Boolean) -> Unit,
) {
  var pressed by remember { mutableStateOf(false) }
  Surface(
    color = if (pressed) NeonCyan.copy(alpha = 0.35f) else ObsidianBg.copy(alpha = 0.72f),
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(1.5.dp, if (pressed) NeonCyan else OutlineSubtle),
    modifier =
      Modifier.size(size)
        .testTag(tag)
        .semantics { contentDescription = cd }
        .pointerInput(Unit) {
          awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            pressed = true
            onHoldChange(true)
            do {
              val event = awaitPointerEvent()
            } while (event.changes.any { it.pressed })
            pressed = false
            onHoldChange(false)
          }
        },
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = cd,
        tint = if (pressed) Color.White else TextPrimary,
        modifier = Modifier.size(36.dp),
      )
    }
  }
}

@Composable
private fun HoldPedalPad(
  label: String,
  accent: Color,
  tag: String,
  width: Dp,
  height: Dp,
  onHoldChange: (Boolean) -> Unit,
) {
  var pressed by remember { mutableStateOf(false) }
  Surface(
    color = if (pressed) accent.copy(alpha = 0.42f) else ObsidianBg.copy(alpha = 0.76f),
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(2.dp, if (pressed) accent else accent.copy(alpha = 0.55f)),
    modifier =
      Modifier.width(width)
        .height(height)
        .testTag(tag)
        .pointerInput(Unit) {
          awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            pressed = true
            onHoldChange(true)
            do {
              val event = awaitPointerEvent()
            } while (event.changes.any { it.pressed })
            pressed = false
            onHoldChange(false)
          }
        },
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = label,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
        color = if (pressed) Color.White else accent,
      )
    }
  }
}

@Composable
private fun HoldActionCircle(
  icon: ImageVector,
  label: String,
  accent: Color,
  tag: String,
  size: Dp,
  onHoldChange: (Boolean) -> Unit,
) {
  var pressed by remember { mutableStateOf(false) }
  Surface(
    color = if (pressed) accent.copy(alpha = 0.42f) else ObsidianBg.copy(alpha = 0.78f),
    shape = CircleShape,
    border = BorderStroke(1.8.dp, accent),
    modifier =
      Modifier.size(size)
        .testTag(tag)
        .pointerInput(Unit) {
          awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            pressed = true
            onHoldChange(true)
            do {
              val event = awaitPointerEvent()
            } while (event.changes.any { it.pressed })
            pressed = false
            onHoldChange(false)
          }
        },
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (pressed) Color.White else accent,
        modifier = Modifier.size(20.dp),
      )
      Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextPrimary, fontSize = 9.sp)
    }
  }
}

@Composable
private fun SmallHoldChip(
  label: String,
  accent: Color,
  tag: String,
  onHoldChange: (Boolean) -> Unit,
) {
  var pressed by remember { mutableStateOf(false) }
  Surface(
    color = if (pressed) accent.copy(alpha = 0.35f) else ObsidianBg.copy(alpha = 0.74f),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.2.dp, if (pressed) accent else OutlineSubtle),
    modifier =
      Modifier.height(48.dp)
        .testTag(tag)
        .pointerInput(Unit) {
          awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            pressed = true
            onHoldChange(true)
            do {
              val event = awaitPointerEvent()
            } while (event.changes.any { it.pressed })
            pressed = false
            onHoldChange(false)
          }
        },
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (pressed) Color.White else accent,
      )
    }
  }
}

@Composable
private fun SmallTapChip(
  icon: ImageVector,
  label: String,
  active: Boolean,
  tag: String,
  onClick: () -> Unit,
) {
  Surface(
    color = if (active) AmberGold.copy(alpha = 0.25f) else ObsidianBg.copy(alpha = 0.74f),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.2.dp, if (active) AmberGold else OutlineSubtle),
    modifier = Modifier.height(48.dp).testTag(tag).clickable { onClick() },
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 12.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (active) AmberGold else TextSecondary,
        modifier = Modifier.size(16.dp),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = if (active) AmberGold else TextSecondary,
      )
    }
  }
}

@Composable
private fun VirtualJoystick(
  modifier: Modifier = Modifier,
  onMove: (Float, Float) -> Unit,
) {
  var knobOffset by remember { mutableStateOf(Offset.Zero) }
  val maxRadiusPx = 95f

  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier.size(120.dp)
        .clip(CircleShape)
        .background(ObsidianBg.copy(alpha = 0.65f))
        .border(2.dp, OutlineSubtle, CircleShape)
        .pointerInput(Unit) {
          awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val center = Offset(size.width * 0.5f, size.height * 0.5f)
            fun update(pos: Offset) {
              val delta = pos - center
              val dist = hypot(delta.x, delta.y)
              val clamped =
                if (dist > maxRadiusPx) Offset(delta.x / dist * maxRadiusPx, delta.y / dist * maxRadiusPx)
                else delta
              knobOffset = clamped
              onMove((clamped.x / maxRadiusPx).coerceIn(-1f, 1f), (clamped.y / maxRadiusPx).coerceIn(-1f, 1f))
            }
            update(down.position)
            do {
              val event = awaitPointerEvent()
              val active = event.changes.firstOrNull { it.pressed }
              if (active != null) update(active.position)
            } while (event.changes.any { it.pressed })
            knobOffset = Offset.Zero
            onMove(0f, 0f)
          }
        },
  ) {
    Box(
      modifier =
        Modifier.offset { IntOffset(knobOffset.x.roundToInt(), knobOffset.y.roundToInt()) }
          .size(48.dp)
          .clip(CircleShape)
          .background(NeonCyan.copy(alpha = 0.85f))
          .border(2.dp, Color.White, CircleShape)
    )
  }
}
