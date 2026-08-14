package com.smarthome.monitor.ui.screens.floor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.smarthome.monitor.model.Device
import com.smarthome.monitor.model.Floor
import com.smarthome.monitor.ui.components.GridDeviceMarker
import com.smarthome.monitor.ui.theme.*

/**
 * Floor plan view showing device markers placed on a grid overlay
 * over the floor plan image.
 */
@Composable
fun FloorPlanScreen(
    floor: Floor,
    devices: List<Device>,
    onDeviceTap: (Device) -> Unit,
    modifier: Modifier = Modifier
) {
    val cellSize: Dp = 52.dp
    val gridWidth = floor.gridCols * cellSize.value
    val gridHeight = floor.gridRows * cellSize.value

    val hScrollState = rememberScrollState()
    val vScrollState = rememberScrollState()

    Column(modifier = modifier.fillMaxSize()) {
        // Floor plan header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(TealAccent, androidx.compose.foundation.shape.CircleShape)
                )
                Text(
                    "${floor.name} — Grid ${floor.gridCols}×${floor.gridRows}",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${devices.size} devices",
                    style = MaterialTheme.typography.labelSmall,
                    color = SmartBlue
                )
            }
        }

        // Scrollable floor plan with grid overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .horizontalScroll(hScrollState)
                .verticalScroll(vScrollState)
        ) {
            // Floor plan background with grid lines
            Box(
                modifier = Modifier
                    .size(width = (gridWidth).dp, height = (gridHeight).dp)
            ) {
                // Grid background (simulates floor plan)
                FloorGridBackground(
                    cols = floor.gridCols,
                    rows = floor.gridRows,
                    cellSize = cellSize
                )

                // Device markers at their grid positions
                devices.forEach { device ->
                    val x = (device.gridX * cellSize.value).dp
                    val y = (device.gridY * cellSize.value).dp

                    Box(
                        modifier = Modifier
                            .offset(x = x, y = y)
                            .size(cellSize)
                            .padding(8.dp)
                    ) {
                        GridDeviceMarker(
                            device = device,
                            onClick = { onDeviceTap(device) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloorGridBackground(cols: Int, rows: Int, cellSize: Dp) {
    // Room zones (approximate)
    val rooms = listOf(
        RoomZone("Living Room", 0, 0, cols / 2, rows / 2, Color(0xFF1A2744)),
        RoomZone("Kitchen", cols / 2, 0, cols, rows / 3, Color(0xFF1A3328)),
        RoomZone("Bedroom", 0, rows / 2, cols / 2, rows, Color(0xFF1D1A37)),
        RoomZone("Bathroom", cols / 2, rows / 3, cols, rows * 2 / 3, Color(0xFF1D2B37)),
        RoomZone("Hallway", cols / 2, rows * 2 / 3, cols, rows, Color(0xFF201E2A))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1520))
    ) {
        // Room zones
        rooms.forEach { room ->
            Box(
                modifier = Modifier
                    .offset(
                        x = (room.startCol * cellSize.value).dp,
                        y = (room.startRow * cellSize.value).dp
                    )
                    .size(
                        width = ((room.endCol - room.startCol) * cellSize.value).dp,
                        height = ((room.endRow - room.startRow) * cellSize.value).dp
                    )
                    .background(room.color, RoundedCornerShape(8.dp))
            )
        }

        // Grid lines overlay
        for (col in 0..cols) {
            Box(
                modifier = Modifier
                    .offset(x = (col * cellSize.value).dp, y = 0.dp)
                    .width(1.dp)
                    .height((rows * cellSize.value).dp)
                    .background(BorderSubtle.copy(alpha = 0.4f))
            )
        }
        for (row in 0..rows) {
            Box(
                modifier = Modifier
                    .offset(x = 0.dp, y = (row * cellSize.value).dp)
                    .width((cols * cellSize.value).dp)
                    .height(1.dp)
                    .background(BorderSubtle.copy(alpha = 0.4f))
            )
        }
    }
}

private data class RoomZone(
    val name: String,
    val startCol: Int,
    val startRow: Int,
    val endCol: Int,
    val endRow: Int,
    val color: Color
)
