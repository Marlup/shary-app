package com.shary.app.ui.screens.home.utils

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shary.app.services.bluetooth.BluetoothService

@SuppressLint("MissingPermission")
@Composable
fun BluetoothDeviceSelectorDialog(
    bluetoothService: BluetoothService,
    dataToSend: String,
    onDismiss: () -> Unit,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val devices by bluetoothService.discoveredDevices.collectAsState()

    var connectingTo by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        bluetoothService.startDiscovery()
    }

    AlertDialog(
        onDismissRequest = {
            bluetoothService.stopDiscovery()
            onDismiss()
        },
        confirmButton = {
            TextButton(onClick = {
                bluetoothService.stopDiscovery()
                onDismiss()
            }) {
                Text("Cancel")
            }
        },
        title = { Text("Select Bluetooth Device") },
        text = {
            if (devices.isEmpty()) {
                Text("Searching for devices...")
            } else {
                Column {
                    devices.forEach { device ->
                        Text(
                            text = device.name ?: device.address,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    connectingTo = device.name ?: device.address
                                    bluetoothService.connectToDevice(device) { connected ->
                                        if (connected) {
                                            bluetoothService.sendData(dataToSend)
                                            Toast.makeText(context, "Data sent to ${device.name}", Toast.LENGTH_SHORT).show()
                                            bluetoothService.stopDiscovery()
                                            onFinished()
                                        } else {
                                            Toast.makeText(context, "Failed to connect to ${device.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                .padding(8.dp)
                        )
                    }
                    connectingTo?.let {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("Connecting to $it...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    )
}
