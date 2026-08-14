package com.smarthome.monitor.ui.screens.camera

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.smarthome.monitor.model.DeviceType
import com.smarthome.monitor.ui.components.StatusBadge
import com.smarthome.monitor.ui.theme.*
import com.smarthome.monitor.viewmodel.HomeViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    deviceId: String,
    homeViewModel: HomeViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val device = uiState.devices.find { it.id == deviceId }

    // Cycle through snapshot frames to simulate "live" feed
    var frameKey by remember { mutableIntStateOf(0) }
    var isRecording by remember { mutableStateOf(true) }

    LaunchedEffect(isRecording) {
        while (isRecording) {
            delay(4000)
            frameKey++
        }
    }

    // Pulsing animation for LIVE indicator
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val livePulse by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f, label = "pulse",
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(device?.name ?: "Camera", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text(device?.roomName ?: "", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                    }
                },
                actions = {
                    device?.let { StatusBadge(state = it.state) }
                    Spacer(Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Camera feed
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0A0F1E))
            ) {
                // Mock snapshot cycling (simulates live stream)
                val snapshotUrl = device?.cameraSnapshotUrl
                    ?: "https://picsum.photos/640/360?random=${frameKey % 5 + 10}"
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("$snapshotUrl&frame=$frameKey")
                        .crossfade(true)
                        .build(),
                    contentDescription = "Camera feed",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark overlay at top for controls
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.TopStart)
                        .background(
                            Brush.verticalGradient(listOf(Color.Black.copy(0.6f), Color.Transparent))
                        )
                )

                // LIVE badge
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart)
                        .background(Color.Black.copy(0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                CameraColor.copy(alpha = livePulse),
                                androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    Text("LIVE", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }

                // Stream URL badge
                device?.cameraStreamUrl?.let { url ->
                    Text(
                        text = url,
                        color = Color.White.copy(0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Timestamp
                Text(
                    text = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                    color = Color.White.copy(0.8f),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .background(Color.Black.copy(0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CameraControlButton(
                    icon = if (isRecording) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                    label = if (isRecording) "Pause" else "Resume",
                    color = if (isRecording) StateOn else TextSecondary,
                    modifier = Modifier.weight(1f)
                ) { isRecording = !isRecording }

                CameraControlButton(
                    icon = Icons.Default.Refresh,
                    label = "Refresh",
                    color = SmartBlue,
                    modifier = Modifier.weight(1f)
                ) { frameKey++ }

                CameraControlButton(
                    icon = Icons.Default.Screenshot,
                    label = "Snapshot",
                    color = TealAccent,
                    modifier = Modifier.weight(1f)
                ) { /* Snapshot action */ }
            }

            // Device info card
            device?.let { cam ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Camera Info", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        InfoRow("Stream URL", cam.cameraStreamUrl ?: "Not set")
                        InfoRow("Location", cam.roomName)
                        InfoRow("Grid Position", "(${cam.gridX}, ${cam.gridY})")
                        InfoRow("Status", DeviceType.CAMERA.displayName)
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraControlButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        border = ButtonDefaults.outlinedButtonBorder.copy()
    ) {
        Icon(icon, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = TextMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(100.dp))
        Text(value, color = TextPrimary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}
