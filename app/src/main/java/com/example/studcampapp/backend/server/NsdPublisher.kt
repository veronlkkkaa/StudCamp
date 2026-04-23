package com.example.studcampapp.backend.server

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NsdPublisher(private val context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var isRegistered = false

    private var currentServiceName = ""
    private var currentDisplayName = ""
    private var currentPort = 0

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var republishJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    @Synchronized
    fun start(serviceName: String, port: Int, displayName: String) {
        stop()
        currentServiceName = serviceName
        currentDisplayName = displayName
        currentPort = port
        registerInternal()
    }

    @Synchronized
    fun stop() {
        stopNetworkMonitoring()
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
            override fun onAvailable(network: Network) = scheduleRepublish()
            override fun onLinkPropertiesChanged(network: Network, lp: LinkProperties) = scheduleRepublish()
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
        republishJob?.cancel()
        republishJob = scope.launch {
            delay(2_000L)
            republish()
        }
    }

    @Synchronized
    private fun republish() {
        if (currentServiceName.isBlank()) return
        val listener = registrationListener
        if (listener != null) {
            runCatching { nsdManager.unregisterService(listener) }
            registrationListener = null
            isRegistered = false
        }
        registerInternal()
    }

    private fun registerInternal() {
        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = currentServiceName
            serviceType = SERVICE_TYPE
            setPort(currentPort)
            runCatching { setAttribute("name", currentDisplayName) }
        }

        val listener = object : NsdManager.RegistrationListener {
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                isRegistered = false
            }
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                isRegistered = false
            }
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                isRegistered = true
            }
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
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
