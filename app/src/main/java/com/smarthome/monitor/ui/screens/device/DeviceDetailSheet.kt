package com.smarthome.monitor.ui.screens.device

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.monitor.model.Device
import com.smarthome.monitor.model.DeviceState
import com.smarthome.monitor.model.DeviceType
import com.smarthome.monitor.ui.components.StatusBadge
import com.smarthome.monitor.ui.components.deviceStateColor
import com.smarthome.monitor.ui.components.deviceTypeIcon
import com.smarthome.monitor.ui.theme.*
import com.smarthome.monitor.viewmodel.HomeViewModel

/**
 * Bottom sheet showing device-specific controls and details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailSheet(
    device: Device,
    homeViewModel: HomeViewModel,
    onDismiss: () -> Unit,
    onNavigateToCamera: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val (typeIcon, typeColor) = deviceTypeIcon(device.type)
    val stateColor = deviceStateColor(device.state)
    val isOn = device.state == DeviceState.ON.name

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            if (isOn) typeColor.copy(alpha = 0.18f) else SurfaceVariantDark,
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(typeIcon, null, tint = if (isOn) typeColor else TextSecondary, modifier = Modifier.size(30.dp))
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(device.name, style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusBadge(state = device.state)
                        Text("${DeviceType.valueOf(device.type).displayName}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                    if (device.roomName.isNotEmpty()) {
                        Text("📍 ${device.roomName}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = TextSecondary)
                }
            }

            Spacer(Modifier.height(24.dp))
            Divider(color = BorderSubtle)
            Spacer(Modifier.height(20.dp))

            // Device-specific controls
            when (device.type) {
                DeviceType.OUTLET.name -> OutletControls(device = device, homeViewModel = homeViewModel)
                DeviceType.MULTI_SWITCH.name -> MultiSwitchControls(device = device, homeViewModel = homeViewModel)
                DeviceType.IRON.name -> IronControls(device = device, homeViewModel = homeViewModel)
                DeviceType.LIGHT.name -> LightControls(device = device, homeViewModel = homeViewModel)
                DeviceType.CAMERA.name -> CameraControls(device = device, onOpenCamera = onNavigateToCamera)
            }

            Spacer(Modifier.height(24.dp))

            // Last updated
            device.lastUpdated?.let { ts ->
                Text(
                    "Last updated: ${java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(ts.toDate())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun OutletControls(device: Device, homeViewModel: HomeViewModel) {
    val isOn = device.state == DeviceState.ON.name
    SectionTitle("Power Control")
    BigToggleButton(
        isOn = isOn,
        label = if (isOn) "Turn Off" else "Turn On",
        color = if (isOn) StateOn else StateOff
    ) { homeViewModel.toggleDevice(device) }
}

@Composable
private fun MultiSwitchControls(device: Device, homeViewModel: HomeViewModel) {
    SectionTitle("Switch Controls — ${device.switchCount}-gang unit")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        (1..device.switchCount).forEach { i ->
            val key = "switch_$i"
            val switchState = device.switchStates[key] ?: DeviceState.OFF.name
            val isOn = switchState == DeviceState.ON.name

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isOn) SwitchColor.copy(alpha = 0.1f) else SurfaceVariantDark
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ToggleOn,
                        null,
                        tint = if (isOn) SwitchColor else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Switch $i",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatusBadge(state = switchState)
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = isOn,
                        onCheckedChange = { on ->
                            homeViewModel.updateSwitchState(device.id, key, if (on) DeviceState.ON.name else DeviceState.OFF.name)
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = SwitchColor,
                            uncheckedTrackColor = SurfaceVariantDark
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun IronControls(device: Device, homeViewModel: HomeViewModel) {
    val isOn = device.state == DeviceState.ON.name
    var maxMinutes by remember { mutableIntStateOf(device.maxOnDurationMinutes) }
    var showSaved by remember { mutableStateOf(false) }

    SectionTitle("Iron Safety Controls")

    // Warning card
    Card(
        colors = CardDefaults.cardColors(containerColor = IronColor.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Whatshot, null, tint = IronColor, modifier = Modifier.size(20.dp))
            Text(
                "Safety cutoff: Automatically turns OFF after $maxMinutes minutes",
                style = MaterialTheme.typography.bodySmall,
                color = IronColor
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    BigToggleButton(
        isOn = isOn,
        label = if (isOn) "Turn Off Iron" else "Turn On Iron",
        color = if (isOn) IronColor else StateOff
    ) { homeViewModel.toggleDevice(device) }

    Spacer(Modifier.height(20.dp))

    SectionTitle("Max On Duration")
    Text("${maxMinutes} minutes", style = MaterialTheme.typography.headlineMedium, color = IronColor, fontWeight = FontWeight.Bold)
    Slider(
        value = maxMinutes.toFloat(),
        onValueChange = { maxMinutes = it.toInt() },
        valueRange = 1f..30f,
        steps = 28,
        colors = SliderDefaults.colors(activeTrackColor = IronColor, thumbColor = IronColor)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("1 min", style = MaterialTheme.typography.bodySmall, color = TextMuted)
        Text("30 min", style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
    Button(
        onClick = {
            homeViewModel.updateMaxOnDuration(device.id, maxMinutes)
            showSaved = true
        },
        colors = ButtonDefaults.buttonColors(containerColor = IronColor),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Text("Save Duration")
    }
    AnimatedVisibility(showSaved) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            showSaved = false
        }
        Text("✓ Saved", color = TealAccent, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LightControls(device: Device, homeViewModel: HomeViewModel) {
    val isOn = device.state == DeviceState.ON.name
    var scheduleEnabled by remember { mutableStateOf(device.scheduleEnabled) }
    var onTime by remember { mutableStateOf(device.scheduleOnTime ?: "06:00") }
    var offTime by remember { mutableStateOf(device.scheduleOffTime ?: "22:00") }

    SectionTitle("Light Control")
    BigToggleButton(
        isOn = isOn,
        label = if (isOn) "Turn Off" else "Turn On",
        color = if (isOn) LightColor else StateOff
    ) { homeViewModel.toggleDevice(device) }

    Spacer(Modifier.height(20.dp))
    SectionTitle("Auto Schedule")

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (scheduleEnabled) LightColor.copy(alpha = 0.08f) else SurfaceVariantDark
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Schedule, null, tint = LightColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Scheduled On/Off", style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                Switch(
                    checked = scheduleEnabled,
                    onCheckedChange = { scheduleEnabled = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = LightColor)
                )
            }

            AnimatedVisibility(scheduleEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TimePickerRow("Turn ON at", onTime, LightColor) { onTime = it }
                    TimePickerRow("Turn OFF at", offTime, StateOff) { offTime = it }
                    Button(
                        onClick = { homeViewModel.updateDeviceSchedule(device.id, onTime, offTime, scheduleEnabled) },
                        colors = ButtonDefaults.buttonColors(containerColor = LightColor),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save Schedule", color = Color.Black, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun CameraControls(device: Device, onOpenCamera: () -> Unit) {
    SectionTitle("Security Camera")
    Card(
        colors = CardDefaults.cardColors(containerColor = CameraColor.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(CameraColor, androidx.compose.foundation.shape.CircleShape)
                )
                Text("LIVE", color = CameraColor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(device.cameraStreamUrl ?: "No stream URL", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = onOpenCamera,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = CameraColor)
            ) {
                Icon(Icons.Default.Videocam, null, modifier = Modifier.size(18.dp), tint = Color.Black)
                Spacer(Modifier.width(8.dp))
                Text("Open Live View", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BigToggleButton(isOn: Boolean, label: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isOn) color.copy(alpha = 0.15f) else color.copy(alpha = 0.08f)
        )
    ) {
        Icon(
            if (isOn) Icons.Default.PowerSettingsNew else Icons.Default.Power,
            null,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = TextMuted,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun TimePickerRow(label: String, value: String, color: Color, onValueChange: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f))
        OutlinedButton(
            onClick = { showDialog = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Schedule, null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(value, fontWeight = FontWeight.SemiBold)
        }
    }
}
