package com.shary.app.services.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.*
import java.io.OutputStream
import java.util.*

class BluetoothService(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        _discoveredDevices.update { current ->
                            if (current.none { it.address == device.address }) current + device else current
                        }
                    }
                }
            }
        }
    }

    init {
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(receiver, filter)
    }

    // ✅ Use inside startDiscovery() / stopDiscovery()
    @SuppressLint("InlinedApi")
    private fun hasBluetoothScanPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (!hasBluetoothScanPermission()) return
        bluetoothAdapter?.takeIf { it.isEnabled }?.startDiscovery()
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        if (!hasBluetoothScanPermission()) return
        bluetoothAdapter?.cancelDiscovery()
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice, onConnected: (Boolean) -> Unit) {
        try {
            stopDiscovery()
            val uuid = device.uuids?.firstOrNull()?.uuid ?: DEFAULT_UUID
            socket = device.createRfcommSocketToServiceRecord(uuid)
            socket?.connect()
            outputStream = socket?.outputStream
            onConnected(true)
        } catch (e: Exception) {
            Log.e("BluetoothService", "Connection failed: ${e.message}")
            onConnected(false)
        }
    }

    fun sendData(data: String) {
        try {
            outputStream?.write(data.toByteArray())
            outputStream?.flush()
        } catch (e: Exception) {
            Log.e("BluetoothService", "Send failed: ${e.message}")
        }
    }

    fun close() {
        try {
            outputStream?.close()
            socket?.close()
            context.unregisterReceiver(receiver)
        } catch (_: Exception) {}
    }

    companion object {
        val DEFAULT_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP UUID
    }
}
