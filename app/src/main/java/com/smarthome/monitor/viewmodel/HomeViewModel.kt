package com.smarthome.monitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.monitor.model.Device
import com.smarthome.monitor.model.DeviceState
import com.smarthome.monitor.model.Floor
import com.smarthome.monitor.model.UsageLog
import com.smarthome.monitor.repository.FirestoreRepository
import com.smarthome.monitor.repository.RtdbRepository
import com.smarthome.monitor.repository.SafetyAlert
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val floors: List<Floor> = emptyList(),
    val devices: List<Device> = emptyList(),
    val safetyAlerts: List<SafetyAlert> = emptyList(),
    val usageLogs: List<UsageLog> = emptyList(),
    val selectedFloorIndex: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isSeeding: Boolean = false
)

class HomeViewModel : ViewModel() {

    private val firestoreRepo = FirestoreRepository()
    private val rtdbRepo = RtdbRepository()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeFloors()
        observeAllDevices()
        observeSafetyAlerts()
        observeIronSafetyCutoffs()
        observeUsageLogs()
    }

    private fun observeFloors() {
        viewModelScope.launch {
            firestoreRepo.getFloorsFlow()
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { floors ->
                    _uiState.update { it.copy(floors = floors, isLoading = false) }
                }
        }
    }

    private fun observeAllDevices() {
        viewModelScope.launch {
            firestoreRepo.getDevicesFlow()
                .catch { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
                .collect { devices ->
                    _uiState.update { it.copy(devices = devices) }
                }
        }
    }

    // ── Client-side Iron Safety Cutoff ──────────────────────────────
    // Tracks one coroutine Job per iron device: deviceId -> Job
    // Each job waits the remaining time then forces the device OFF.
    private val ironCutoffJobs = HashMap<String, Job>()


    private fun observeIronSafetyCutoffs() {
        viewModelScope.launch {
            firestoreRepo.getDevicesFlow()
                .catch { /* non-critical, ignore */ }
                .collect { devices ->
                    val onIrons = devices.filter {
                        it.type == "IRON" && it.state == DeviceState.ON.name && it.onSince != null
                    }
                    val onIronIds = onIrons.map { it.id }.toSet()

                    // Cancel jobs for irons that are now OFF
                    ironCutoffJobs.keys.toList().forEach { id ->
                        if (id !in onIronIds) {
                            ironCutoffJobs[id]?.cancel()
                            ironCutoffJobs.remove(id)
                        }
                    }

                    // Schedule new jobs for irons that just turned ON
                    onIrons.forEach { device ->
                        if (ironCutoffJobs.containsKey(device.id)) return@forEach // already scheduled

                        val onSince = device.onSince?.toDate() ?: return@forEach
                        val effectiveMinutes = device.maxOnDurationMinutes
                        val maxMs = effectiveMinutes * 60 * 1000L
                        val elapsedMs = System.currentTimeMillis() - onSince.time
                        val remainingMs = maxMs - elapsedMs

                        if (remainingMs <= 0) {
                            // Already overdue — cut off immediately
                            viewModelScope.launch {
                                runIronCutoff(device, effectiveMinutes)
                            }
                            return@forEach
                        }

                        android.util.Log.d(
                            "IronSafety",
                            "Scheduling auto-off for '${device.name}' in ${remainingMs / 1000}s (${effectiveMinutes}min limit)"
                        )

                        ironCutoffJobs[device.id] = viewModelScope.launch {
                            delay(remainingMs)
                            android.util.Log.d("IronSafety", "Auto-cutting off '${device.name}'")
                            runIronCutoff(device, effectiveMinutes)
                            ironCutoffJobs.remove(device.id)
                        }
                    }
                }
        }
    }

    private suspend fun runIronCutoff(device: Device, durationMinutes: Int) {
        try {
            // Log the auto-cutoff usage session before turning OFF
            val onSince = device.onSince ?: firestoreRepo.getDeviceOnSince(device.id)
            if (onSince != null) {
                firestoreRepo.logUsageSession(
                    deviceId = device.id,
                    deviceName = device.name,
                    deviceType = device.type,
                    startTime = onSince,
                    endTime = com.google.firebase.Timestamp.now(),
                    autoCutoff = true
                )
            }
            firestoreRepo.toggleDeviceState(device.id, DeviceState.OFF.name)
            rtdbRepo.pushSafetyAlert(
                deviceId = device.id,
                deviceName = device.name,
                message = "⚠️ ${device.name} was auto-OFF after $durationMinutes minutes (safety cutoff)"
            )
        } catch (e: Exception) {
            android.util.Log.e("IronSafety", "Cutoff failed: ${e.message}")
        }
    }
    // ────────────────────────────────────────────────────────────────

    private fun observeSafetyAlerts() {
        viewModelScope.launch {
            rtdbRepo.getSafetyAlertsFlow()
                .catch { /* RTDB not critical, ignore */ }
                .collect { alerts ->
                    _uiState.update { it.copy(safetyAlerts = alerts) }
                }
        }
    }

    fun selectFloor(index: Int) {
        _uiState.update { it.copy(selectedFloorIndex = index) }
    }

    fun getDevicesForFloor(floorId: String): List<Device> {
        return _uiState.value.devices.filter { it.floorId == floorId }
    }

    fun toggleDevice(device: Device) {
        val newState = if (device.state == DeviceState.ON.name) DeviceState.OFF.name else DeviceState.ON.name
        viewModelScope.launch {
            try {
                // If turning OFF a trackable device (IRON or LIGHT), log the usage session first
                if (newState == DeviceState.OFF.name &&
                    (device.type == "IRON" || device.type == "LIGHT")
                ) {
                    val onSince = device.onSince ?: firestoreRepo.getDeviceOnSince(device.id)
                    if (onSince != null) {
                        val endTime = com.google.firebase.Timestamp.now()
                        firestoreRepo.logUsageSession(
                            deviceId = device.id,
                            deviceName = device.name,
                            deviceType = device.type,
                            startTime = onSince,
                            endTime = endTime,
                            autoCutoff = false
                        )
                    }
                }
                firestoreRepo.toggleDeviceState(device.id, newState)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to toggle device: ${e.message}") }
            }
        }
    }

    fun updateSwitchState(deviceId: String, switchKey: String, newState: String) {
        viewModelScope.launch {
            try {
                firestoreRepo.updateSwitchState(deviceId, switchKey, newState)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to update switch: ${e.message}") }
            }
        }
    }

    fun updateDeviceSchedule(deviceId: String, onTime: String?, offTime: String?, enabled: Boolean) {
        viewModelScope.launch {
            try {
                firestoreRepo.updateDeviceSchedule(deviceId, onTime, offTime, enabled)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to update schedule: ${e.message}") }
            }
        }
    }

    fun updateMaxOnDuration(deviceId: String, minutes: Int) {
        viewModelScope.launch {
            try {
                firestoreRepo.updateMaxOnDuration(deviceId, minutes)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to update duration: ${e.message}") }
            }
        }
    }

    fun addFloor(name: String) {
        viewModelScope.launch {
            try {
                val count = _uiState.value.floors.size
                firestoreRepo.addFloor(
                    com.smarthome.monitor.model.Floor(
                        name = name,
                        imageAsset = "floor_ground",
                        order = count
                    )
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to add floor: ${e.message}") }
            }
        }
    }

    fun deleteDevice(deviceId: String) {
        viewModelScope.launch {
            try {
                firestoreRepo.deleteDevice(deviceId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete device: ${e.message}") }
            }
        }
    }

    private fun observeUsageLogs() {
        viewModelScope.launch {
            firestoreRepo.getUsageLogsFlow()
                .catch { e ->
                    _uiState.update { it.copy(error = "Failed to load reports: ${e.message}") }
                }
                .collect { logs ->
                    _uiState.update { it.copy(usageLogs = logs) }
                }
        }
    }

    // Keep as public no-op for backward compat with any callsites in the UI
    fun loadUsageLogs() { /* now handled live by observeUsageLogs() */ }

    fun seedDemoData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSeeding = true) }
            try {
                firestoreRepo.seedDemoData()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Seed failed: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isSeeding = false) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
