package com.example.location

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.util.AppConstants
import com.example.util.ContactHashHelper
import com.example.util.matches
import kotlin.math.pow

class ProximityManager(
    private val context: Context,
    private val onContactDetected: (contactName: String, rssi: Int, distanceMeters: Double) -> Unit
) {

    private val TAG = "ProximityManager"
    private val mainHandler = Handler(Looper.getMainLooper())

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bleAdvertiser: BluetoothLeAdvertiser? = null
    private var bleScanner: BluetoothLeScanner? = null
    private var isScanning = false
    private var ownPhoneHash: ByteArray? = null
    private var knownHashes: Set<ByteArray> = emptySet()

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d(TAG, "BLE advertising started")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "BLE advertising failed: $errorCode")
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result ?: return
            val record = result.scanRecord ?: return
            val manufacturerData = record.getManufacturerSpecificData(AppConstants.Ble.COMPANY_ID) ?: return

            val rssi = result.rssi
            val estimatedDistance = estimateDistance(rssi)

            if (estimatedDistance > 100.0) return

            val receivedHash = manufacturerData.copyOf(AppConstants.Ble.HASH_BYTES.coerceAtMost(manufacturerData.size))

            if (knownHashes.any { it.matches(receivedHash) }) {
                val hex = ContactHashHelper.hashToHex(receivedHash)
                Log.d(TAG, "Contact detected: hash=$hex rssi=$rssi dist=${"%.1f".format(estimatedDistance)}m")
                onContactDetected("[Contacto detectado]", rssi, estimatedDistance)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE scan failed: $errorCode")
        }
    }

    fun init(): Boolean {
        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return false
        bluetoothAdapter = bm.adapter ?: return false
        bleAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        bleScanner = bluetoothAdapter?.bluetoothLeScanner
        return true
    }

    fun startAdvertising(phoneHash: ByteArray) {
        ownPhoneHash = phoneHash
        val advertiser = bleAdvertiser ?: return

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(false)
            .setTimeout(0)
            .build()

        val data = AdvertiseData.Builder()
            .addManufacturerData(AppConstants.Ble.COMPANY_ID, phoneHash)
            .build()

        try {
            advertiser.startAdvertising(settings, data, advertiseCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "BLE advertising permission denied", e)
        }
    }

    fun updateScanHashes(hashes: Set<ByteArray>) {
        knownHashes = hashes
    }

    fun startScanning() {
        if (isScanning) return
        val scanner = bleScanner ?: return

        val filter = ScanFilter.Builder()
            .setManufacturerData(AppConstants.Ble.COMPANY_ID, ByteArray(AppConstants.Ble.HASH_BYTES))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build()

        try {
            scanner.startScan(listOf(filter), settings, scanCallback)
            isScanning = true
            Log.d(TAG, "BLE scanning started")
        } catch (e: SecurityException) {
            Log.e(TAG, "BLE scan permission denied", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        if (!isScanning) return
        try {
            bleScanner?.stopScan(scanCallback)
        } catch (_: Exception) { }
        isScanning = false
    }

    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        ownPhoneHash?.let {
            try {
                bleAdvertiser?.stopAdvertising(advertiseCallback)
            } catch (_: Exception) { }
        }
        ownPhoneHash = null
    }

    fun stop() {
        stopScanning()
        stopAdvertising()
        mainHandler.removeCallbacksAndMessages(null)
    }

    fun isBluetoothEnabled(): Boolean =
        bluetoothAdapter?.isEnabled == true

    private fun estimateDistance(rssi: Int): Double {
        val txPower = AppConstants.Ble.TX_POWER_1M
        val n = AppConstants.Ble.PATH_LOSS_EXPONENT
        return 10.0.pow((txPower - rssi) / (10.0 * n))
    }
}
