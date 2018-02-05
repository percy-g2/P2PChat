package com.androdevlinux.percy.p2p.common.listeners

import com.androdevlinux.percy.p2p.common.P2PDeviceService
import com.androdevlinux.percy.p2p.common.WiFiP2PError

interface ServiceDiscoveredListener {

    fun onNewServiceDeviceDiscovered(p2PDeviceService: P2PDeviceService)

    fun onFinishServiceDeviceDiscovered(p2PDeviceServices: List<P2PDeviceService>)

    fun onError(wiFiP2PError: WiFiP2PError)

}
