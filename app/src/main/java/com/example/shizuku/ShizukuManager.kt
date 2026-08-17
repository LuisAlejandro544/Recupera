package com.example.shizuku

import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

object ShizukuManager {

    private const val TAG = "ShizukuManager"
    const val SHIZUKU_REQUEST_CODE = 4096

    private val _shizukuState = MutableStateFlow(ShizukuState())
    val shizukuState: StateFlow<ShizukuState> = _shizukuState.asStateFlow()

    private var isInitialized = false

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.i(TAG, "Shizuku Binder received")
        refreshStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Log.w(TAG, "Shizuku Binder died")
        _shizukuState.value = ShizukuState(
            status = ShizukuStatus.NOT_RUNNING,
            isBinderAlive = false,
            isPermissionGranted = false,
            isEnhancedScanEnabled = _shizukuState.value.isEnhancedScanEnabled
        )
    }

    private val requestPermissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_REQUEST_CODE) {
            val granted = grantResult == PackageManager.PERMISSION_GRANTED
            Log.i(TAG, "Shizuku permission result: granted=$granted")
            refreshStatus()
        }
    }

    fun initialize(packageName: String = "com.example") {
        if (isInitialized) return
        isInitialized = true

        try {
            try {
                rikka.sui.Sui.init(packageName)
            } catch (e: Throwable) {
                Log.d(TAG, "Sui init skipped or not installed: ${e.message}")
            }

            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
            refreshStatus()
        } catch (e: Throwable) {
            Log.w(TAG, "Error initializing Shizuku listeners: ${e.message}")
        }
    }

    fun refreshStatus() {
        try {
            val isBinderAlive = Shizuku.pingBinder()
            if (!isBinderAlive) {
                _shizukuState.value = ShizukuState(
                    status = ShizukuStatus.NOT_RUNNING,
                    isBinderAlive = false,
                    isPermissionGranted = false,
                    isEnhancedScanEnabled = _shizukuState.value.isEnhancedScanEnabled
                )
                return
            }

            if (Shizuku.isPreV11()) {
                Log.w(TAG, "Shizuku pre-v11 is unsupported")
                _shizukuState.value = ShizukuState(
                    status = ShizukuStatus.NOT_RUNNING,
                    isBinderAlive = false,
                    isPermissionGranted = false,
                    isEnhancedScanEnabled = _shizukuState.value.isEnhancedScanEnabled
                )
                return
            }

            val isGranted = try {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (e: Throwable) {
                false
            }

            val version = try { Shizuku.getVersion() } catch (e: Throwable) { 0 }
            val uid = try { Shizuku.getUid() } catch (e: Throwable) { -1 }
            val isSui = try {
                rikka.sui.Sui.isSui()
            } catch (_: Throwable) {
                false
            }

            val status = if (isGranted) {
                ShizukuStatus.AUTHORIZED_ACTIVE
            } else {
                ShizukuStatus.RUNNING_UNAUTHORIZED
            }

            _shizukuState.value = ShizukuState(
                status = status,
                isBinderAlive = true,
                isPermissionGranted = isGranted,
                version = version,
                uid = uid,
                isSui = isSui,
                isEnhancedScanEnabled = if (isGranted) _shizukuState.value.isEnhancedScanEnabled else false
            )
            Log.i(TAG, "Shizuku status refreshed: status=$status, version=$version, uid=$uid, isSui=$isSui")
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to refresh Shizuku status: ${e.message}")
            _shizukuState.value = ShizukuState(
                status = ShizukuStatus.NOT_RUNNING,
                isBinderAlive = false,
                isPermissionGranted = false,
                isEnhancedScanEnabled = _shizukuState.value.isEnhancedScanEnabled
            )
        }
    }

    fun requestPermission(): Boolean {
        try {
            if (!Shizuku.pingBinder()) {
                Log.w(TAG, "Cannot request permission: Shizuku binder is not alive")
                return false
            }

            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                refreshStatus()
                return true
            }

            if (Shizuku.shouldShowRequestPermissionRationale()) {
                Log.i(TAG, "Should show request permission rationale for Shizuku")
            }

            Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
            return true
        } catch (e: Throwable) {
            Log.e(TAG, "Exception requesting Shizuku permission", e)
            return false
        }
    }

    fun toggleEnhancedScan(enabled: Boolean) {
        val currentState = _shizukuState.value
        if (enabled && (!currentState.isBinderAlive || !currentState.isPermissionGranted)) {
            Log.w(TAG, "Cannot enable enhanced scan without authorized Shizuku service")
            return
        }
        _shizukuState.value = currentState.copy(isEnhancedScanEnabled = enabled)
    }

    fun cleanup() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
        } catch (e: Throwable) {
            Log.w(TAG, "Error cleaning up Shizuku listeners: ${e.message}")
        }
    }
}
