package com.smarthome.monitor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.monitor.model.Device
import com.smarthome.monitor.model.DeviceState
import com.smarthome.monitor.model.DeviceType
import com.smarthome.monitor.ui.theme.*

/**
 * Returns the icon and accent color for each device type.
 */
fun deviceTypeIcon(type: String): Pair<ImageVector, Color> = when (type) {
    DeviceType.OUTLET.name -> Pair(Icons.Rounded.Outlet, OutletColor)
    DeviceType.MULTI_SWITCH.name -> Pair(CustomIcons.Switch, SwitchColor)
    DeviceType.IRON.name -> Pair(CustomIcons.Iron, IronColor)
    DeviceType.LIGHT.name -> Pair(Icons.Rounded.Lightbulb, LightColor)
    DeviceType.CAMERA.name -> Pair(CustomIcons.Camera, CameraColor)
    else -> Pair(Icons.Rounded.DeviceHub, TextSecondary)
}

fun deviceStateColor(state: String): Color = when (state) {
    DeviceState.ON.name -> StateOn
    DeviceState.OFF.name -> StateOff
    DeviceState.ERROR.name -> StateError
    DeviceState.DISCONNECTED.name -> StateDisconnected
    else -> StateOff
}

/**
 * A premium device card for the floor plan device list.
 */
@Composable
fun DeviceCard(
    device: Device,
    onToggle: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stateColor = deviceStateColor(device.state)
    val (typeIcon, typeColor) = deviceTypeIcon(device.type)
    val isOn = device.state == DeviceState.ON.name
    val isInteractable = device.state != DeviceState.ERROR.name &&
            device.state != DeviceState.DISCONNECTED.name

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, if (isOn) typeColor.copy(alpha = 0.3f) else BorderSubtle, RoundedCornerShape(24.dp))
            .clickable { onTap() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isOn) 8.dp else 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isOn) Brush.linearGradient(
                        colors = listOf(typeColor.copy(alpha = 0.12f), SurfaceDark.copy(alpha = 0.6f))
                    ) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                )
        ) {
            // Removed the old top glow border because we now use the full card border

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device type icon circle
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            if (isOn) typeColor.copy(alpha = 0.12f) else GlassWhite,
                            RoundedCornerShape(16.dp)
                        )
                        .border(1.dp, if (isOn) typeColor.copy(alpha = 0.2f) else GlassWhite, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = if (isOn) typeColor else TextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusBadge(state = device.state)
                        if (device.roomName.isNotEmpty()) {
                            Text(
                                text = "• ${device.roomName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                    // Iron safety info
                    if (device.type == DeviceType.IRON.name && isOn) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "⏱ Max ${device.maxOnDurationMinutes}min",
                            style = MaterialTheme.typography.bodySmall,
                            color = IronColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // Schedule info for lights
                    if (device.type == DeviceType.LIGHT.name && device.scheduleEnabled) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "🕐 ${device.scheduleOnTime} – ${device.scheduleOffTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = LightColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Toggle switch
                Switch(
                    checked = isOn,
                    onCheckedChange = { if (isInteractable) onToggle() },
                    enabled = isInteractable,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = typeColor,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = SurfaceVariantDark
                    )
                )
            }
        }
    }
}

/**
 * Small status pill badge.
 */
@Composable
fun StatusBadge(state: String, modifier: Modifier = Modifier) {
    val color = deviceStateColor(state)
    val text = DeviceState.fromString(state).displayName

    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(color, androidx.compose.foundation.shape.CircleShape)
            )
            Text(text, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Grid cell icon for the floor plan overlay.
 */
@Composable
fun GridDeviceMarker(
    device: Device,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stateColor = deviceStateColor(device.state)
    val (typeIcon, typeColor) = deviceTypeIcon(device.type)
    val isOn = device.state == DeviceState.ON.name

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isOn) typeColor.copy(alpha = 0.2f) else SurfaceVariantDark.copy(alpha = 0.85f)
            )
            .border(1.5.dp, if (isOn) typeColor.copy(alpha = 0.6f) else BorderSubtle, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = typeIcon,
            contentDescription = device.name,
            tint = if (isOn) typeColor else TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        // Online dot indicator
        Box(
            modifier = Modifier
                .size(7.dp)
                .align(Alignment.TopEnd)
                .offset(x = 2.dp, y = (-2).dp)
                .background(stateColor, androidx.compose.foundation.shape.CircleShape)
                .border(1.dp, CardDark, androidx.compose.foundation.shape.CircleShape)
        )
    }
}
