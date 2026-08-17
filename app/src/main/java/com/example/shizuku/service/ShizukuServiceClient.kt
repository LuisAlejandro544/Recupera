package com.example.shizuku.service

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import com.example.shizuku.ShizukuManager
import com.example.shizuku.model.ShizukuScannedItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume

object ShizukuServiceClient {

    private const val TAG = "ShizukuServiceClient"
    private const val SERVICE_TAG = "recuperador_pro_scan_user_service"
    private const val SERVICE_VERSION = 1

    private var scanServiceBinder: IBinder? = null
    private var isBinding = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.i(TAG, "ShizukuScanUserService connected: $name")
            scanServiceBinder = service
            isBinding = false
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "ShizukuScanUserService disconnected: $name")
            scanServiceBinder = null
            isBinding = false
        }
    }

    suspend fun getServiceBinder(context: Context): IBinder? = withContext(Dispatchers.IO) {
        val existing = scanServiceBinder
        if (existing != null && existing.isBinderAlive && existing.pingBinder()) {
            return@withContext existing
        }

        if (!ShizukuManager.shizukuState.value.isReadyForEnhancedScan) {
            Log.w(TAG, "Shizuku is not authorized or ready for enhanced scan")
            return@withContext null
        }

        return@withContext withTimeoutOrNull(4000L) {
            suspendCancellableCoroutine { continuation ->
                val connection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                        Log.i(TAG, "UserService onServiceConnected received via coroutine")
                        scanServiceBinder = service
                        if (continuation.isActive) {
                            continuation.resume(service)
                        }
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        scanServiceBinder = null
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                }

                try {
                    val componentName = ComponentName(context.packageName, ShizukuScanUserService::class.java.name)
                    val args = Shizuku.UserServiceArgs(componentName)
                        .tag(SERVICE_TAG)
                        .version(SERVICE_VERSION)
                        .processNameSuffix("shizuku_scanner")
                        .debuggable(false)
                        .daemon(false)

                    Shizuku.bindUserService(args, connection)
                } catch (e: Throwable) {
                    Log.e(TAG, "Failed to bind Shizuku UserService: ${e.message}", e)
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        }
    }

    suspend fun scanProtectedDirectories(
        context: Context,
        customPaths: List<String> = emptyList()
    ): List<ShizukuScannedItem> = withContext(Dispatchers.IO) {
        val binder = getServiceBinder(context) ?: return@withContext emptyList()

        try {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            try {
                data.writeInterfaceToken(ShizukuScanUserService.DESCRIPTOR)
                data.writeStringList(customPaths)
                binder.transact(ShizukuScanUserService.TRANSACTION_SCAN_PROTECTED_DIRECTORIES, data, reply, 0)
                reply.readException()
                val results = mutableListOf<ShizukuScannedItem>()
                reply.readTypedList(results, ShizukuScannedItem.CREATOR)
                Log.i(TAG, "Shizuku scan completed: found ${results.size} protected items")
                return@withContext results
            } finally {
                data.recycle()
                reply.recycle()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error executing protected directory scan via Shizuku: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    fun releaseService(context: Context) {
        val binder = scanServiceBinder
        if (binder != null) {
            try {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                try {
                    binder.transact(ShizukuScanUserService.TRANSACTION_DESTROY, data, reply, 0)
                    reply.readException()
                } finally {
                    data.recycle()
                    reply.recycle()
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Error calling destroy on Shizuku UserService: ${e.message}")
            }
        }

        try {
            val componentName = ComponentName(context.packageName, ShizukuScanUserService::class.java.name)
            val args = Shizuku.UserServiceArgs(componentName)
                .tag(SERVICE_TAG)
                .version(SERVICE_VERSION)
            Shizuku.unbindUserService(args, serviceConnection, true)
        } catch (e: Throwable) {
            Log.w(TAG, "Error unbinding Shizuku UserService: ${e.message}")
        } finally {
            scanServiceBinder = null
        }
    }
}
