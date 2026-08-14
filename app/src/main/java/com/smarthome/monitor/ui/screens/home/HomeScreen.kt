package com.smarthome.monitor.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarthome.monitor.model.Device
import com.smarthome.monitor.model.DeviceType
import com.smarthome.monitor.ui.components.DeviceCard
import com.smarthome.monitor.ui.components.StatusBadge
import com.smarthome.monitor.ui.screens.device.DeviceDetailSheet
import com.smarthome.monitor.ui.screens.floor.FloorPlanScreen
import com.smarthome.monitor.ui.theme.*
import com.smarthome.monitor.viewmodel.AuthViewModel
import com.smarthome.monitor.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    authViewModel: AuthViewModel,
    onNavigateToCamera: (String) -> Unit,
    onNavigateToReports: () -> Unit,
    onSignOut: () -> Unit
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { maxOf(uiState.floors.size, 1) })
    val scope = rememberCoroutineScope()
    var selectedDevice by remember { mutableStateOf<Device?>(null) }
    var showAddFloorDialog by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(ViewMode.LIST) }
    var showSafetyAlerts by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.floors.isNotEmpty()) {
        if (uiState.floors.isEmpty()) {
            homeViewModel.seedDemoData()
        }
    }

    // Device detail bottom sheet
    selectedDevice?.let { deviceSnapshot ->
        val latestDevice = uiState.devices.find { it.id == deviceSnapshot.id } ?: deviceSnapshot
        DeviceDetailSheet(
            device = latestDevice,
            homeViewModel = homeViewModel,
            onDismiss = { selectedDevice = null },
            onNavigateToCamera = { onNavigateToCamera(latestDevice.id) }
        )
    }

    if (showAddFloorDialog) {
        AddFloorDialog(
            onConfirm = { name ->
                homeViewModel.addFloor(name)
                showAddFloorDialog = false
            },
            onDismiss = { showAddFloorDialog = false }
        )
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(SurfaceDark, BackgroundDark))
                    )
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Brush.linearGradient(listOf(OutletColor, SwitchColor)),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(CustomIcons.HomeLogo, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Nexus Home",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${uiState.devices.count { it.state == "ON" }} devices active",
                            style = MaterialTheme.typography.bodySmall,
                            color = TealAccent
                        )
                    }

                    // Safety alerts badge
                    if (uiState.safetyAlerts.isNotEmpty()) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = StateError) {
                                    Text("${uiState.safetyAlerts.size}")
                                }
                            }
                        ) {
                            IconButton(onClick = { showSafetyAlerts = !showSafetyAlerts }) {
                                Icon(Icons.Default.Warning, null, tint = SafetyOrange)
                            }
                        }
                    }

                    // View mode toggle
                    IconButton(onClick = { viewMode = if (viewMode == ViewMode.LIST) ViewMode.FLOORPLAN else ViewMode.LIST }) {
                        Icon(
                            if (viewMode == ViewMode.LIST) Icons.Default.Map else Icons.Default.ViewList,
                            null, tint = SmartBlue
                        )
                    }

                    IconButton(onClick = { onNavigateToReports() }) {
                        Icon(Icons.Default.BarChart, null, tint = TextSecondary)
                    }

                    IconButton(onClick = { onSignOut() }) {
                        Icon(Icons.Default.Logout, null, tint = TextSecondary)
                    }
                }

                // Safety alerts banner
                AnimatedVisibility(showSafetyAlerts && uiState.safetyAlerts.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = SafetyRed.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, null, tint = SafetyRed, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Safety Alerts", color = SafetyRed, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            }
                            uiState.safetyAlerts.take(3).forEach { alert ->
                                Text(
                                    "• ${alert.message}",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Floor Tabs
                if (uiState.floors.isNotEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = Color.Transparent,
                        contentColor = SmartBlue,
                        edgePadding = 16.dp,
                        indicator = { tabPositions ->
                            if (pagerState.currentPage < tabPositions.size) {
                                Box(
                                    modifier = Modifier
                                        .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                                        .height(3.dp)
                                        .background(
                                            Brush.horizontalGradient(listOf(SmartBlue, TealAccent)),
                                            RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                                        )
                                )
                            }
                        },
                        divider = {}
                    ) {
                        uiState.floors.forEachIndexed { index, floor ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                text = {
                                    Text(
                                        floor.name,
                                        color = if (pagerState.currentPage == index) SmartBlue else TextSecondary,
                                        fontWeight = if (pagerState.currentPage == index) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                        // Add floor tab
                        Tab(
                            selected = false,
                            onClick = { showAddFloorDialog = true },
                            icon = { Icon(Icons.Default.Add, null, tint = TextMuted, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }
        },
        containerColor = BackgroundDark
    ) { padding ->
        if (uiState.isLoading || uiState.isSeeding) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = SmartBlue)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (uiState.isSeeding) "Setting up your home..." else "Loading...",
                        color = TextSecondary
                    )
                }
            }
        } else if (uiState.floors.isEmpty()) {
            EmptyState(onAddFloor = { showAddFloorDialog = true }, modifier = Modifier.padding(padding))
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) { pageIndex ->
                if (pageIndex < uiState.floors.size) {
                    val floor = uiState.floors[pageIndex]
                    val floorDevices = homeViewModel.getDevicesForFloor(floor.id)

                    when (viewMode) {
                        ViewMode.LIST -> DeviceListPage(
                            devices = floorDevices,
                            floorName = floor.name,
                            onDeviceTap = { selectedDevice = it },
                            onDeviceToggle = { homeViewModel.toggleDevice(it) }
                        )
                        ViewMode.FLOORPLAN -> FloorPlanScreen(
                            floor = floor,
                            devices = floorDevices,
                            onDeviceTap = { selectedDevice = it }
                        )
                    }
                }
            }
        }
    }

    // Error snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            homeViewModel.clearError()
        }
    }
}

enum class ViewMode { LIST, FLOORPLAN }

@Composable
private fun DeviceListPage(
    devices: List<Device>,
    floorName: String,
    onDeviceTap: (Device) -> Unit,
    onDeviceToggle: (Device) -> Unit,
    modifier: Modifier = Modifier
) {
    if (devices.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.DeviceHub, null, tint = TextMuted, modifier = Modifier.size(60.dp))
                Spacer(Modifier.height(16.dp))
                Text("No devices on $floorName", color = TextMuted, style = MaterialTheme.typography.bodyLarge)
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Summary row
            item {
                DeviceSummaryRow(devices = devices)
            }

            // Group by type
            val grouped = devices.groupBy { it.type }
            DeviceType.entries.forEach { type ->
                grouped[type.name]?.let { typeDevices ->
                    item {
                        Text(
                            text = type.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(typeDevices, key = { it.id }) { device ->
                        DeviceCard(
                            device = device,
                            onToggle = { onDeviceToggle(device) },
                            onTap = { onDeviceTap(device) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceSummaryRow(devices: List<Device>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SummaryChip(
            label = "Active",
            value = "${devices.count { it.state == "ON" }}",
            color = StateOn,
            modifier = Modifier.weight(1f)
        )
        SummaryChip(
            label = "Total",
            value = "${devices.size}",
            color = SmartBlue,
            modifier = Modifier.weight(1f)
        )
        SummaryChip(
            label = "Issues",
            value = "${devices.count { it.state == "ERROR" || it.state == "DISCONNECTED" }}",
            color = StateError,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}

@Composable
private fun EmptyState(onAddFloor: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.Home, null, tint = TextMuted, modifier = Modifier.size(80.dp))
            Text("No floors yet", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Text("Add a floor to start managing your home", color = TextSecondary)
            Button(onClick = onAddFloor, colors = ButtonDefaults.buttonColors(containerColor = SmartBlue)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add First Floor")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFloorDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Floor", color = TextPrimary) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Floor Name") },
                placeholder = { Text("e.g. Ground Floor") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SmartBlue,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                colors = ButtonDefaults.buttonColors(containerColor = SmartBlue)
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = SurfaceDark
    )
}
