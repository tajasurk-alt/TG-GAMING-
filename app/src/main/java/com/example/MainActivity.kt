package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.game.model.GameMode
import com.example.game.model.PaintPreset
import com.example.game.model.TimeOfDay
import com.example.game.model.VehicleModelType
import com.example.ui.GameScreen
import com.example.ui.MainMenuScreen
import com.example.ui.theme.ViceCoastTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    )
    setContent {
      ViceCoastTheme {
        var inGame by rememberSaveable { mutableStateOf(false) }
        var selectedVehicle by rememberSaveable { mutableStateOf(VehicleModelType.INFERNUS_GT) }
        var selectedPaint by rememberSaveable { mutableStateOf(PaintPreset.SUNSET_CRIMSON) }
        var selectedTimeOfDay by rememberSaveable { mutableStateOf(TimeOfDay.SUNSET) }
        var selectedMode by rememberSaveable { mutableStateOf(GameMode.FREE_ROAM) }

        if (inGame) {
          GameScreen(
            initialVehicle = selectedVehicle,
            initialPaint = selectedPaint,
            initialTimeOfDay = selectedTimeOfDay,
            initialMode = selectedMode,
            onExitToMenu = { inGame = false },
          )
        } else {
          MainMenuScreen(
            selectedVehicle = selectedVehicle,
            selectedPaint = selectedPaint,
            selectedTimeOfDay = selectedTimeOfDay,
            onSelectVehicle = { selectedVehicle = it },
            onSelectPaint = { selectedPaint = it },
            onSelectTimeOfDay = { selectedTimeOfDay = it },
            onLaunchGame = { mode ->
              selectedMode = mode
              inGame = true
            },
          )
        }
      }
    }
  }
}
