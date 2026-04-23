package com.example.studcampapp.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.compose.runtime.mutableStateListOf
import com.example.studcampapp.model.DiscoveredRoom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object NsdDiscovery {
    const val SERVICE_NAME_PREFIX = "lyra-"

    val rooms = mutableStateListOf<DiscoveredRoom>()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var nsdManager: NsdManager? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var resolveQueue: Channel<NsdServiceInfo>? = null
    private var resolveJob: Job? = null

    fun start(context: Context) {
        stop()
        scope.launch(Dispatchers.Main) { rooms.clear() }

        val queue = Channel<NsdServiceInfo>(Channel.UNLIMITED)
        resolveQueue = queue
        resolveJob = scope.launch {
            for (info in queue) resolveOnce(info)
        }

        val manager = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
        nsdManager = manager

        val listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onDiscoveryStopped(serviceType: String) {}

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceName.startsWith(SERVICE_NAME_PREFIX)) {
                    queue.trySend(serviceInfo)
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                scope.launch(Dispatchers.Main) {
                    rooms.removeAll { it.serviceName == serviceInfo.serviceName }
                }
            }
        }
        discoveryListener = listener
        manager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    fun stop() {
        val listener = discoveryListener
        if (listener != null) {
            nsdManager?.let { runCatching { it.stopServiceDiscovery(listener) } }
            discoveryListener = null
        }
        nsdManager = null
        resolveQueue?.close()
        resolveQueue = null
        resolveJob?.cancel()
        resolveJob = null
        scope.launch(Dispatchers.Main) { rooms.clear() }
    }

    private suspend fun resolveOnce(serviceInfo: NsdServiceInfo) {
        val manager = nsdManager ?: return
        suspendCancellableCoroutine<Unit> { cont ->
            manager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    if (cont.isActive) cont.resume(Unit)
                }

                override fun onServiceResolved(resolved: NsdServiceInfo) {
                    val rawServiceName = resolved.serviceName
                    val ip = resolved.host?.hostAddress
                    if (ip != null && rawServiceName.startsWith(SERVICE_NAME_PREFIX)) {
                        val roomId = rawServiceName.removePrefix(SERVICE_NAME_PREFIX)
                        val displayName = runCatching {
                            resolved.attributes?.get("name")?.let { String(it, Charsets.UTF_8) }
                        }.getOrNull() ?: rawServiceName
                        val room = DiscoveredRoom(
                            serviceName = rawServiceName,
                            displayName = displayName,
                            roomId = roomId,
                            ip = ip,
                            port = resolved.port
                        )
                        scope.launch(Dispatchers.Main) {
                            if (rooms.none { it.roomId == room.roomId }) rooms.add(room)
                        }
                    }
                    if (cont.isActive) cont.resume(Unit)
                }
            })
        }
    }

    const val SERVICE_TYPE = "_lyra._tcp."
}
