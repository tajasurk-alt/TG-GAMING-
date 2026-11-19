package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.model.GameMode
import com.example.game.model.PaintPreset
import com.example.game.model.TimeOfDay
import com.example.game.model.VehicleModelType
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
fun MainMenuScreen(
  selectedVehicle: VehicleModelType,
  selectedPaint: PaintPreset,
  selectedTimeOfDay: TimeOfDay,
  onSelectVehicle: (VehicleModelType) -> Unit,
  onSelectPaint: (PaintPreset) -> Unit,
  onSelectTimeOfDay: (TimeOfDay) -> Unit,
  onLaunchGame: (GameMode) -> Unit,
) {
  val starterRides =
    listOf(
      VehicleModelType.INFERNUS_GT,
      VehicleModelType.ZENTORNO_RS,
      VehicleModelType.AKUMA_1000R,
      VehicleModelType.BATI_801RR,
      VehicleModelType.BANSHEE_V8,
      VehicleModelType.SANCHEZ_MX,
      VehicleModelType.VICE_PD,
    )

  Box(modifier = Modifier.fillMaxSize().background(ObsidianBg)) {
    // Hero Background Key Art
    Image(
      painter = painterResource(id = R.drawable.img_vice_coast_banner),
      contentDescription = stringResource(id = R.string.cd_game_banner),
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )

    // Cinematic Vignette Gradient
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.verticalGradient(
              colorStops =
                arrayOf(
                  0.0f to ObsidianBg.copy(alpha = 0.55f),
                  0.40f to ObsidianBg.copy(alpha = 0.45f),
                  0.78f to ObsidianBg.copy(alpha = 0.90f),
                  1.0f to ObsidianBg.copy(alpha = 0.98f),
                )
            )
          )
    )

    BoxWithConstraints(
      modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(20.dp)
    ) {
      val isWide = maxWidth > 680.dp

      Column(
        modifier =
          Modifier.fillMaxSize()
            .widthIn(max = 840.dp)
            .align(Alignment.Center)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        // Title Header
        Column(modifier = Modifier.padding(top = 8.dp)) {
          Surface(
            color = SunsetCoral.copy(alpha = 0.18f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, SunsetCoral.copy(alpha = 0.55f)),
          ) {
            Text(
              text = "OPEN WORLD 3D SANDBOX",
              style = MaterialTheme.typography.labelSmall,
              color = SunsetCoral,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "VICE COAST",
            style =
              MaterialTheme.typography.displaySmall.copy(
                fontSize = if (isWide) 46.sp else 38.sp,
                letterSpacing = 2.sp,
              ),
            color = TextPrimary,
          )
          Text(
            text = "Supercars, Sportbikes, Police Chases & Coastal Stunts",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Vehicle & Atmosphere Customization Panel
        Card(
          colors = CardDefaults.cardColors(containerColor = AsphaltSurface.copy(alpha = 0.88f)),
          shape = RoundedCornerShape(20.dp),
          border = BorderStroke(1.dp, OutlineSubtle),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "SELECT STARTER RIDE",
              style = MaterialTheme.typography.labelLarge,
              color = AmberGold,
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              starterRides.forEach { ride ->
                val isSelected = ride == selectedVehicle
                Card(
                  colors =
                    CardDefaults.cardColors(
                      containerColor =
                        if (isSelected) SunsetCoral.copy(alpha = 0.22f) else AsphaltCard
                    ),
                  shape = RoundedCornerShape(14.dp),
                  border =
                    BorderStroke(
                      width = if (isSelected) 2.dp else 1.dp,
                      color = if (isSelected) SunsetCoral else OutlineSubtle,
                    ),
                  modifier =
                    Modifier.testTag("starter_ride_${ride.name.lowercase()}")
                      .clickable { onSelectVehicle(ride) },
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                  ) {
                    Icon(
                      imageVector =
                        if (ride.isBike) Icons.Filled.TwoWheeler else Icons.Filled.DirectionsCar,
                      contentDescription = ride.displayName,
                      tint = if (isSelected) SunsetCoral else NeonCyan,
                      modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                      Text(
                        text = ride.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                      )
                      Text(
                        text = "${ride.subtitle} • ${(ride.maxSpeedMps * 3.6f).toInt()} km/h",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                      )
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Paint Color Swatches & Time of Day
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(
                  text = "CUSTOM PAINT",
                  style = MaterialTheme.typography.labelSmall,
                  color = TextSecondary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                  PaintPreset.entries.forEach { paint ->
                    val chosen = paint == selectedPaint
                    Box(
                      modifier =
                        Modifier.size(34.dp)
                          .clip(CircleShape)
                          .background(paint.uiColor)
                          .border(
                            width = if (chosen) 3.dp else 1.dp,
                            color = if (chosen) Color.White else OutlineSubtle,
                            shape = CircleShape,
                          )
                          .testTag("paint_${paint.name.lowercase()}")
                          .clickable { onSelectPaint(paint) }
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
              text = "LIGHTING & ATMOSPHERE",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              TimeOfDay.entries.forEach { tod ->
                val active = tod == selectedTimeOfDay
                Surface(
                  color = if (active) NeonCyan.copy(alpha = 0.18f) else AsphaltCard,
                  shape = RoundedCornerShape(10.dp),
                  border = BorderStroke(1.dp, if (active) NeonCyan else OutlineSubtle),
                  modifier =
                    Modifier.testTag("tod_${tod.name.lowercase()}").clickable {
                      onSelectTimeOfDay(tod)
                    },
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.WbSunny,
                      contentDescription = tod.label,
                      tint = if (active) NeonCyan else TextSecondary,
                      modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = tod.label,
                      style = MaterialTheme.typography.labelLarge,
                      color = if (active) NeonCyan else TextPrimary,
                    )
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action CTAs
        Row(
          modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Button(
            onClick = { onLaunchGame(GameMode.FREE_ROAM) },
            colors = ButtonDefaults.buttonColors(containerColor = SunsetCoral, contentColor = ObsidianBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.weight(1.3f).height(56.dp).testTag("play_free_roam_button"),
          ) {
            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "FREE ROAM",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            )
          }

          OutlinedButton(
            onClick = { onLaunchGame(GameMode.TIME_TRIAL) },
            border = BorderStroke(1.5.dp, NeonCyan),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.weight(1f).height(56.dp).testTag("play_time_trial_button"),
          ) {
            Icon(imageVector = Icons.Filled.Flag, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "TIME TRIAL",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
          }
        }
      }
    }
  }
}
