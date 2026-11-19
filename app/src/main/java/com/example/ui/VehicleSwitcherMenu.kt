package com.example.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.game.model.PaintPreset
import com.example.game.model.VehicleModelType
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AsphaltCard
import com.example.ui.theme.AsphaltElevated
import com.example.ui.theme.AsphaltSurface
import com.example.ui.theme.CashEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class VehicleCategoryFilter(val label: String) {
  ALL("All Rides"),
  SPORTS_CARS("Sports Cars"),
  MOTORBIKES("Motorbikes"),
}

@Composable
fun VehicleSwitcherMenu(
  currentVehicle: VehicleModelType?,
  selectedPaint: PaintPreset,
  onSelectPaint: (PaintPreset) -> Unit,
  onSwitchVehicle: (VehicleModelType, PaintPreset) -> Unit,
  onDismiss: () -> Unit,
) {
  var activeFilter by remember { mutableStateOf(VehicleCategoryFilter.ALL) }
  var localPaint by remember(selectedPaint) { mutableStateOf(selectedPaint) }

  val allSwitcherVehicles = remember {
    listOf(
      VehicleModelType.ZENTORNO_RS,
      VehicleModelType.INFERNUS_GT,
      VehicleModelType.BANSHEE_V8,
      VehicleModelType.BATI_801RR,
      VehicleModelType.AKUMA_1000R,
      VehicleModelType.SANCHEZ_MX,
      VehicleModelType.VICE_PD,
    )
  }

  val filteredVehicles =
    remember(activeFilter) {
      when (activeFilter) {
        VehicleCategoryFilter.ALL -> allSwitcherVehicles
        VehicleCategoryFilter.SPORTS_CARS -> allSwitcherVehicles.filter { !it.isBike }
        VehicleCategoryFilter.MOTORBIKES -> allSwitcherVehicles.filter { it.isBike }
      }
    }

  Box(
    contentAlignment = Alignment.Center,
    modifier =
      Modifier.fillMaxSize()
        .background(ObsidianBg.copy(alpha = 0.72f))
        .clickable { onDismiss() }
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(16.dp),
  ) {
    Card(
      colors = CardDefaults.cardColors(containerColor = AsphaltSurface.copy(alpha = 0.96f)),
      shape = RoundedCornerShape(24.dp),
      border = BorderStroke(1.5.dp, OutlineSubtle),
      modifier =
        Modifier.fillMaxWidth()
          .widthIn(max = 640.dp)
          .clickable(enabled = false) {},
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = "VEHICLE SWITCHER",
              style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
              color = TextPrimary,
            )
            Text(
              text = "Tap any sports car or motorbike to switch immediately",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary,
            )
          }
          Surface(
            color = AsphaltCard,
            shape = CircleShape,
            border = BorderStroke(1.dp, OutlineSubtle),
            modifier = Modifier.size(48.dp),
          ) {
            IconButton(
              onClick = onDismiss,
              modifier = Modifier.testTag("close_vehicle_menu_button"),
            ) {
              Icon(imageVector = Icons.Filled.Close, contentDescription = "Close vehicle menu", tint = TextPrimary)
            }
          }
        }

        // Category Filter Tabs
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          VehicleCategoryFilter.entries.forEach { tab ->
            val selected = tab == activeFilter
            Surface(
              color = if (selected) SunsetCoral else AsphaltCard,
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, if (selected) SunsetCoral else OutlineSubtle),
              modifier =
                Modifier.height(44.dp)
                  .testTag("vehicle_filter_${tab.name.lowercase()}")
                  .clickable { activeFilter = tab },
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp),
              ) {
                if (tab == VehicleCategoryFilter.SPORTS_CARS) {
                  Icon(
                    imageVector = Icons.Filled.DirectionsCar,
                    contentDescription = null,
                    tint = if (selected) ObsidianBg else NeonCyan,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                } else if (tab == VehicleCategoryFilter.MOTORBIKES) {
                  Icon(
                    imageVector = Icons.Filled.TwoWheeler,
                    contentDescription = null,
                    tint = if (selected) ObsidianBg else AmberGold,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                  text = tab.label,
                  style = MaterialTheme.typography.labelLarge,
                  color = if (selected) ObsidianBg else TextPrimary,
                )
              }
            }
          }
        }

        // Paint Selector Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "PAINT: ${localPaint.label.uppercase()}",
            style = MaterialTheme.typography.labelSmall,
            color = NeonCyan,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaintPreset.entries.forEach { paint ->
              val isChosen = paint == localPaint
              Box(
                modifier =
                  Modifier.size(30.dp)
                    .clip(CircleShape)
                    .background(paint.uiColor)
                    .border(
                      width = if (isChosen) 2.5.dp else 1.dp,
                      color = if (isChosen) Color.White else OutlineSubtle,
                      shape = CircleShape,
                    )
                    .testTag("switcher_paint_${paint.name.lowercase()}")
                    .clickable {
                      localPaint = paint
                      onSelectPaint(paint)
                    }
              )
            }
          }
        }

        // Vehicle Cards List / Grid
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
          val twoColumns = maxWidth >= 500.dp
          Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            if (twoColumns) {
              filteredVehicles.chunked(2).forEach { pair ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  pair.forEach { model ->
                    VehicleSwitchCard(
                      model = model,
                      isActive = model == currentVehicle,
                      accentPaint = localPaint.uiColor,
                      onClick = { onSwitchVehicle(model, localPaint) },
                      modifier = Modifier.weight(1f),
                    )
                  }
                  if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
              }
            } else {
              filteredVehicles.forEach { model ->
                VehicleSwitchCard(
                  model = model,
                  isActive = model == currentVehicle,
                  accentPaint = localPaint.uiColor,
                  onClick = { onSwitchVehicle(model, localPaint) },
                  modifier = Modifier.fillMaxWidth(),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun VehicleSwitchCard(
  model: VehicleModelType,
  isActive: Boolean,
  accentPaint: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val typeAccent = if (model.isBike) AmberGold else NeonCyan
  Card(
    colors =
      CardDefaults.cardColors(
        containerColor = if (isActive) AsphaltElevated else AsphaltCard
      ),
    shape = RoundedCornerShape(16.dp),
    border =
      BorderStroke(
        width = if (isActive) 2.dp else 1.dp,
        color = if (isActive) CashEmerald else OutlineSubtle,
      ),
    modifier =
      modifier
        .testTag("switch_vehicle_${model.name.lowercase()}")
        .clickable { onClick() },
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Surface(
            color = (if (model.isPolice) Color(0xFF2D7DFF) else accentPaint).copy(alpha = 0.22f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, typeAccent.copy(alpha = 0.5f)),
            modifier = Modifier.size(44.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector =
                  if (model.isBike) Icons.Filled.TwoWheeler else Icons.Filled.DirectionsCar,
                contentDescription = model.displayName,
                tint = typeAccent,
                modifier = Modifier.size(24.dp),
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = model.displayName,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = TextPrimary,
            )
            Text(
              text = model.subtitle,
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary,
            )
          }
        }

        if (isActive) {
          Surface(
            color = CashEmerald.copy(alpha = 0.2f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, CashEmerald),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Active",
                tint = CashEmerald,
                modifier = Modifier.size(14.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (model.isBike) "RIDING" else "DRIVING",
                style = MaterialTheme.typography.labelSmall,
                color = CashEmerald,
              )
            }
          }
        } else {
          Surface(
            color = SunsetCoral.copy(alpha = 0.16f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, SunsetCoral.copy(alpha = 0.6f)),
          ) {
            Text(
              text = if (model.isBike) "RIDE" else "DRIVE",
              style = MaterialTheme.typography.labelSmall,
              color = SunsetCoral,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
          }
        }
      }

      // Performance Stat Bars
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.Speed,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(14.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${model.topSpeedKmh} km/h",
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary,
          )
        }
        StatBar(label = "ACC", progress = model.accelRating, color = SunsetCoral, modifier = Modifier.weight(1f))
        StatBar(label = "GRIP", progress = model.handlingRating, color = NeonCyan, modifier = Modifier.weight(1f))
      }
    }
  }
}

@Composable
private fun StatBar(
  label: String,
  progress: Float,
  color: Color,
  modifier: Modifier = Modifier,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp),
    modifier = modifier,
  ) {
    Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    Box(
      modifier =
        Modifier.weight(1f)
          .height(5.dp)
          .clip(RoundedCornerShape(3.dp))
          .background(OutlineSubtle)
    ) {
      Box(
        modifier =
          Modifier.fillMaxWidth(progress.coerceIn(0.1f, 1f))
            .height(5.dp)
            .background(color)
      )
    }
  }
}
