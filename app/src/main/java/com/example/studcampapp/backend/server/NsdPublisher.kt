package com.example.studcampapp.backend.server

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class NsdPublisher(private val context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var isRegistered = false

    private var currentServiceName = ""
    private var currentDisplayName = ""
    private var currentPort = 0

    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var republishJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var unregisterSignal: CompletableDeferred<Unit>? = null

    @Synchronized
    fun start(serviceName: String, port: Int, displayName: String) {
        stop()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        currentServiceName = serviceName
        currentDisplayName = displayName
        currentPort = port
        registerInternal()
    }

    @Synchronized
    fun stop() {
        stopNetworkMonitoring()
        scope.cancel()
        currentServiceName = ""
        val listener = registrationListener ?: return
        runCatching { nsdManager.unregisterService(listener) }
        registrationListener = null
        isRegistered = false
    }

    fun startNetworkMonitoring() {
        if (networkCallback != null) return
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("StudCampNSD", "network callback: onAvailable")
                scheduleRepublish()
            }
            override fun onLinkPropertiesChanged(network: Network, lp: LinkProperties) {
                Log.d("StudCampNSD", "network callback: onLinkPropertiesChanged")
                scheduleRepublish()
            }
            override fun onLost(network: Network) {
                Log.d("StudCampNSD", "network callback: onLost")
            }
        }
        runCatching { cm.registerDefaultNetworkCallback(cb) }.onSuccess { networkCallback = cb }
    }

    private fun stopNetworkMonitoring() {
        republishJob?.cancel()
        republishJob = null
        val cb = networkCallback ?: return
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        runCatching { cm?.unregisterNetworkCallback(cb) }
        networkCallback = null
    }

    private fun scheduleRepublish() {
        Log.d("StudCampNSD", "scheduleRepublish (debounced)")
        republishJob?.cancel()
        republishJob = scope.launch {
            delay(2_000L)
            republish()
        }
    }

    private suspend fun republish() {
        if (currentServiceName.isBlank()) return
        Log.d("StudCampNSD", "republish: START unregister→register")
        val listener = registrationListener
        if (listener != null) {
            val signal = CompletableDeferred<Unit>()
            unregisterSignal = signal
            runCatching { nsdManager.unregisterService(listener) }.onFailure { e ->
                Log.e("StudCampNSD", "republish: unregisterService threw", e)
                signal.complete(Unit)
            }
            Log.d("StudCampNSD", "republish: awaiting unregister callback")
            val result = withTimeoutOrNull(3_000) { signal.await() }
            if (result == null) {
                Log.w("StudCampNSD", "republish: unregister callback timeout after 3s, proceeding anyway")
            }
            registrationListener = null
            isRegistered = false
        }
        Log.d("StudCampNSD", "republish: unregister done, calling registerInternal")
        registerInternal()
        Log.d("StudCampNSD", "republish: END")
    }

    private fun registerInternal() {
        Log.d("StudCampNSD", "register: name=$currentServiceName port=$currentPort")
        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = currentServiceName
            serviceType = SERVICE_TYPE
            setPort(currentPort)
            runCatching { setAttribute("name", currentDisplayName) }
        }

        val listener = object : NsdManager.RegistrationListener {
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("StudCampNSD", "register failed: errorCode=$errorCode name=$currentServiceName")
                isRegistered = false
            }
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("StudCampNSD", "unregister failed: errorCode=$errorCode")
                unregisterSignal?.complete(Unit)
                unregisterSignal = null
                isRegistered = false
            }
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                Log.d("StudCampNSD", "registered: name=${serviceInfo.serviceName}")
                isRegistered = true
            }
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.d("StudCampNSD", "unregistered")
                unregisterSignal?.complete(Unit)
                unregisterSignal = null
                isRegistered = false
            }
        }

        registrationListener = listener
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    companion object {
        const val SERVICE_TYPE = "_lyra._tcp."
    }
}
