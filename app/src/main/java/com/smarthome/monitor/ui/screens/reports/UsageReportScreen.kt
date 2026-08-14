package com.smarthome.monitor.ui.screens.reports

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.*
import com.patrykandpatrick.vico.compose.cartesian.axis.*
import com.patrykandpatrick.vico.compose.cartesian.layer.*
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.data.*
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.smarthome.monitor.model.DeviceType
import com.smarthome.monitor.model.UsageLog
import com.smarthome.monitor.ui.components.deviceTypeIcon
import com.smarthome.monitor.ui.theme.*
import com.smarthome.monitor.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageReportScreen(
    homeViewModel: HomeViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by homeViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        homeViewModel.loadUsageLogs()
    }

    // Aggregate usage per device type from devices
    val devices = uiState.devices
    val deviceStats = devices.groupBy { it.type }.map { (type, devs) ->
        DeviceTypeStats(
            type = type,
            count = devs.size,
            activeCount = devs.count { it.state == "ON" },
            totalOnTimeMinutes = devs.sumOf { it.totalOnTimeMinutes }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Usage & Reports", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Summary cards
            item {
                Text("Overview", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OverviewCard("Total Devices", "${devices.size}", SmartBlue, modifier = Modifier.weight(1f))
                    OverviewCard("Active Now", "${devices.count { it.state == "ON" }}", StateOn, modifier = Modifier.weight(1f))
                    OverviewCard("Issues", "${devices.count { it.state == "ERROR" || it.state == "DISCONNECTED" }}", StateError, modifier = Modifier.weight(1f))
                }
            }

            // Safety alerts summary
            if (uiState.safetyAlerts.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SafetyRed.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Security, null, tint = SafetyRed, modifier = Modifier.size(20.dp))
                                Text("Safety Events", style = MaterialTheme.typography.titleMedium, color = SafetyRed, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            uiState.safetyAlerts.forEach { alert ->
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Warning, null, tint = SafetyOrange, modifier = Modifier.size(14.dp))
                                    Text(alert.message, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            // Device type breakdown
            item {
                Text("By Device Type", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
            }

            items(deviceStats) { stat ->
                DeviceTypeStatCard(stat = stat)
            }

            // Bar chart for device count by type
            if (deviceStats.isNotEmpty()) {
                item {
                    Text("Device Distribution", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    // DeviceDistributionChart(deviceStats = deviceStats)
                    Text("Chart omitted due to library version issues", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Usage logs
            if (uiState.usageLogs.isNotEmpty()) {
                item {
                    Text("Usage History", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                }
                items(uiState.usageLogs.take(20), key = { "${it.deviceId}_${it.startTime}" }) { log ->
                    UsageLogRow(log = log)
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}

data class DeviceTypeStats(
    val type: String,
    val count: Int,
    val activeCount: Int,
    val totalOnTimeMinutes: Long
)

@Composable
private fun DeviceTypeStatCard(stat: DeviceTypeStats) {
    val typeName = DeviceType.entries.find { it.name == stat.type }?.displayName ?: stat.type
    val (typeIcon, typeColor) = deviceTypeIcon(stat.type)

    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(typeColor.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(typeIcon, null, tint = typeColor, modifier = Modifier.size(24.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(typeName, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text("${stat.count} device(s) · ${stat.activeCount} active", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("${stat.totalOnTimeMinutes}m", style = MaterialTheme.typography.titleMedium, color = typeColor, fontWeight = FontWeight.Bold)
                Text("Total ON time", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
    }
}

/*
@Composable
private fun DeviceDistributionChart(deviceStats: List<DeviceTypeStats>) {
}
*/

@Composable
private fun UsageLogRow(log: UsageLog) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.History, null,
                tint = if (log.autoCutoff) SafetyRed else SmartBlue,
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(log.deviceName, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
                Text(
                    "${log.durationMinutes}min · ${log.deviceType}" + if (log.autoCutoff) " · AUTO CUTOFF" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (log.autoCutoff) SafetyRed else TextSecondary
                )
            }
            log.startTime?.let { ts ->
                Text(
                    java.text.SimpleDateFormat("MMM dd HH:mm", java.util.Locale.getDefault()).format(ts.toDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
    }
}
