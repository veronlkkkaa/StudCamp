package com.example.studcampapp.backend.server

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo

class NsdPublisher(context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var isRegistered = false

    @Synchronized
    fun start(serviceName: String, port: Int) {
        stop()

        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = serviceName
            serviceType = SERVICE_TYPE
            setPort(port)
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

    @Synchronized
    fun stop() {
        val listener = registrationListener ?: return
        runCatching { nsdManager.unregisterService(listener) }
        registrationListener = null
        isRegistered = false
    }

    companion object {
        const val SERVICE_TYPE = "_lyra._tcp."
    }
}

