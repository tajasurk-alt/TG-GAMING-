package com.example.ui

import android.opengl.GLSurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.audio.SoundEngine
import com.example.game.engine.ViceCoastRenderer
import com.example.game.model.GameMode
import com.example.game.model.PaintPreset
import com.example.game.model.TimeOfDay
import com.example.game.model.VehicleModelType
import com.example.game.sim.GameSimulation
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AsphaltCard
import com.example.ui.theme.AsphaltSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GameScreen(
  initialVehicle: VehicleModelType,
  initialPaint: PaintPreset,
  initialTimeOfDay: TimeOfDay,
  initialMode: GameMode,
  onExitToMenu: () -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  val soundEngine = remember { SoundEngine() }
  val simulation = remember {
    GameSimulation().apply {
      preferredPaint = initialPaint
      timeOfDay = initialTimeOfDay
      headlightsOn = initialTimeOfDay.autoHeadlights
      resetWorld(initialMode, startMounted = initialVehicle)
      onChimeEvent = { soundEngine.triggerChime() }
    }
  }
  val renderer = remember { ViceCoastRenderer(simulation, soundEngine) }
  val glSurfaceView = remember {
    GLSurfaceView(context).apply {
      setEGLContextClientVersion(3)
      setRenderer(renderer)
      renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }
  }

  var showPauseModal by remember { mutableStateOf(false) }
  var showVehicleMenu by remember { mutableStateOf(false) }
  var isMuted by remember { mutableStateOf(false) }
  val hudState by renderer.hudState.collectAsStateWithLifecycle()

  DisposableEffect(lifecycleOwner) {
    soundEngine.start()
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_PAUSE -> {
          renderer.isPaused = true
          glSurfaceView.onPause()
          soundEngine.setMuted(true)
        }
        Lifecycle.Event.ON_RESUME -> {
          glSurfaceView.onResume()
          renderer.isPaused = showPauseModal || showVehicleMenu
          soundEngine.setMuted(isMuted)
        }
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      soundEngine.stop()
    }
  }

  BackHandler {
    when {
      showVehicleMenu -> {
        showVehicleMenu = false
        renderer.isPaused = false
      }
      showPauseModal -> {
        showPauseModal = false
        renderer.isPaused = false
      }
      else -> {
        showPauseModal = true
        renderer.isPaused = true
      }
    }
  }

  Box(modifier = Modifier.fillMaxSize().background(ObsidianBg)) {
    AndroidView(factory = { glSurfaceView }, modifier = Modifier.fillMaxSize())

    GameHudOverlay(
      hud = hudState,
      controls = simulation.controls,
      isMuted = isMuted,
      onToggleMute = {
        isMuted = !isMuted
        soundEngine.setMuted(isMuted)
      },
      onCycleCamera = { renderer.enqueueAction { simulation.cycleCamera() } },
      onTogglePause = {
        showVehicleMenu = false
        showPauseModal = true
        renderer.isPaused = true
      },
      onOpenGarage = {
        showPauseModal = false
        showVehicleMenu = true
        renderer.isPaused = true
      },
      onEnterExitVehicle = { renderer.enqueueAction { simulation.toggleEnterExitVehicle() } },
      onToggleHeadlights = { renderer.enqueueAction { simulation.toggleHeadlights() } },
    )

    if (showVehicleMenu) {
      VehicleSwitcherMenu(
        currentVehicle = hudState.currentVehicleModel,
        selectedPaint = simulation.preferredPaint,
        onSelectPaint = { paint ->
          renderer.enqueueAction { simulation.applyPaint(paint) }
        },
        onSwitchVehicle = { model, paint ->
          renderer.enqueueAction { simulation.spawnAndMountPlayerVehicle(model, paint) }
          showVehicleMenu = false
          renderer.isPaused = false
        },
        onDismiss = {
          showVehicleMenu = false
          renderer.isPaused = false
        },
      )
    }

    if (showPauseModal) {
      PauseAndGarageOverlay(
        currentTimeOfDay = hudState.timeOfDay,
        currentPaint = simulation.preferredPaint,
        onResume = {
          showPauseModal = false
          renderer.isPaused = false
        },
        onSpawnVehicle = { model ->
          renderer.enqueueAction { simulation.spawnAndMountPlayerVehicle(model, simulation.preferredPaint) }
          showPauseModal = false
          renderer.isPaused = false
        },
        onApplyPaint = { paint ->
          renderer.enqueueAction { simulation.applyPaint(paint) }
        },
        onSelectTimeOfDay = { tod ->
          renderer.enqueueAction {
            simulation.timeOfDay = tod
            simulation.headlightsOn = tod.autoHeadlights
            simulation.activeVehicle()?.headlightsOn = tod.autoHeadlights
          }
        },
        onRestartMode = { mode ->
          renderer.enqueueAction { simulation.resetWorld(mode, startMounted = initialVehicle) }
          showPauseModal = false
          renderer.isPaused = false
        },
        onExitToMenu = onExitToMenu,
      )
    }
  }
}

@Composable
private fun PauseAndGarageOverlay(
  currentTimeOfDay: TimeOfDay,
  currentPaint: PaintPreset,
  onResume: () -> Unit,
  onSpawnVehicle: (VehicleModelType) -> Unit,
  onApplyPaint: (PaintPreset) -> Unit,
  onSelectTimeOfDay: (TimeOfDay) -> Unit,
  onRestartMode: (GameMode) -> Unit,
  onExitToMenu: () -> Unit,
) {
  val garageRides =
    listOf(
      VehicleModelType.ZENTORNO_RS,
      VehicleModelType.INFERNUS_GT,
      VehicleModelType.BATI_801RR,
      VehicleModelType.AKUMA_1000R,
      VehicleModelType.BANSHEE_V8,
      VehicleModelType.SANCHEZ_MX,
      VehicleModelType.VICE_PD,
    )

  Box(
    contentAlignment = Alignment.Center,
    modifier =
      Modifier.fillMaxSize()
        .background(ObsidianBg.copy(alpha = 0.78f))
        .clickable(enabled = false) {}
        .padding(20.dp),
  ) {
    Card(
      colors = CardDefaults.cardColors(containerColor = AsphaltSurface),
      shape = RoundedCornerShape(22.dp),
      border = BorderStroke(1.5.dp, OutlineSubtle),
      modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
    ) {
      Column(
        modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = "VICE GARAGE & PAUSE",
              style = MaterialTheme.typography.headlineMedium,
              color = TextPrimary,
            )
            Text(
              text = "Instant Vehicle Delivery, Respray & Time of Day",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary,
            )
          }
          IconButton(onClick = onResume, modifier = Modifier.testTag("close_pause_button")) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = "Resume", tint = TextPrimary)
          }
        }

        Text(
          text = "DELIVER CAR OR MOTORBIKE",
          style = MaterialTheme.typography.labelLarge,
          color = AmberGold,
        )

        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          garageRides.forEach { ride ->
            Surface(
              color = AsphaltCard,
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, OutlineSubtle),
              modifier =
                Modifier.testTag("garage_spawn_${ride.name.lowercase()}")
                  .clickable { onSpawnVehicle(ride) },
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
              ) {
                Icon(
                  imageVector =
                    if (ride.isBike) Icons.Filled.TwoWheeler else Icons.Filled.DirectionsCar,
                  contentDescription = ride.displayName,
                  tint = if (ride.isBike) NeonCyan else SunsetCoral,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(text = ride.displayName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                  Text(text = ride.subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
              }
            }
          }
        }

        Text(
          text = "CUSTOM PAINT RESPRAY",
          style = MaterialTheme.typography.labelLarge,
          color = NeonCyan,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          PaintPreset.entries.forEach { paint ->
            val selected = paint == currentPaint
            Box(
              modifier =
                Modifier.size(34.dp)
                  .clip(CircleShape)
                  .background(paint.uiColor)
                  .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else OutlineSubtle, CircleShape)
                  .clickable { onApplyPaint(paint) }
            )
          }
        }

        Text(
          text = "TIME OF DAY",
          style = MaterialTheme.typography.labelLarge,
          color = TextSecondary,
        )
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          TimeOfDay.entries.forEach { tod ->
            val active = tod == currentTimeOfDay
            Surface(
              color = if (active) NeonCyan.copy(alpha = 0.2f) else AsphaltCard,
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, if (active) NeonCyan else OutlineSubtle),
              modifier = Modifier.clickable { onSelectTimeOfDay(tod) },
            ) {
              Text(
                text = tod.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (active) NeonCyan else TextPrimary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
          onClick = onResume,
          colors = ButtonDefaults.buttonColors(containerColor = SunsetCoral, contentColor = ObsidianBg),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("resume_game_button"),
        ) {
          Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("RESUME GAME", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = { onRestartMode(GameMode.FREE_ROAM) },
            border = BorderStroke(1.dp, OutlineSubtle),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).height(48.dp).testTag("restart_free_roam_button"),
          ) {
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, tint = TextPrimary)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reset", color = TextPrimary)
          }
          OutlinedButton(
            onClick = onExitToMenu,
            border = BorderStroke(1.dp, OutlineSubtle),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).height(48.dp).testTag("exit_to_menu_button"),
          ) {
            Icon(imageVector = Icons.Filled.Home, contentDescription = null, tint = TextPrimary)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Main Menu", color = TextPrimary)
          }
        }
      }
    }
  }
}
