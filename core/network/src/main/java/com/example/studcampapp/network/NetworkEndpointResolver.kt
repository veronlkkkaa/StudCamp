package com.example.studcampapp.network

import android.util.Log
import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkEndpointResolver {
    fun resolveHostIp(): String {
        val interfaces = runCatching { NetworkInterface.getNetworkInterfaces()?.toList().orEmpty() }
            .getOrDefault(emptyList())

        val ipv4Addresses = interfaces.asSequence()
            .filter { it.isUp && !it.isLoopback && !it.isVirtual }
            .flatMap { it.inetAddresses.toList().asSequence() }
            .filterIsInstance<Inet4Address>()
            .toList()

        Log.d("StudCampWS", "resolveHostIp: candidates=${ipv4Addresses.map { it.hostAddress }}")

        val selected = ipv4Addresses
            .firstOrNull { it.isSiteLocalAddress && !it.isLinkLocalAddress }
            ?.hostAddress
            ?.takeIf { it.isNotBlank() }
            ?: ipv4Addresses
                .firstOrNull { !it.isLoopbackAddress }
                ?.hostAddress
                ?.takeIf { it.isNotBlank() }
            ?: "127.0.0.1"

        Log.d("StudCampWS", "resolveHostIp: selected=$selected")
        return selected
    }
}
