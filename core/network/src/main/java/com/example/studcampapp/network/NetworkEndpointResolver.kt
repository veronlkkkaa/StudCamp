package com.example.studcampapp.network

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

        ipv4Addresses
            .firstOrNull { it.isSiteLocalAddress && !it.isLinkLocalAddress }
            ?.hostAddress
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        ipv4Addresses
            .firstOrNull { !it.isLoopbackAddress }
            ?.hostAddress
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        return "127.0.0.1"
    }
}

