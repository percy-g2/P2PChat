package com.androdevlinux.percy.p2p.common


import android.net.wifi.p2p.WifiP2pDevice


open class P2PDevice {

    var deviceName: String? = null
    var deviceMac: String? = null
    var deviceServerSocketIP: String? = null
    var deviceServerSocketPort: Int = 0

    var customName: String? = null

    constructor()

    constructor(device: WifiP2pDevice) {
        this.deviceName = device.deviceName
        this.deviceMac = device.deviceAddress
    }

    override fun toString(): String {
        return "P2PDevice[deviceName=$deviceName][deviceMac=$deviceMac][deviceServerSocketIP=$deviceServerSocketIP][deviceServerSocketPort=$deviceServerSocketPort]"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val that = other as P2PDevice?

        return (if (deviceName != null) deviceName == that!!.deviceName else that!!.deviceName == null) && if (deviceMac != null) deviceMac == that.deviceMac else that.deviceMac == null
    }

    override fun hashCode(): Int {
        var result = if (deviceName != null) deviceName!!.hashCode() else 0
        result = 31 * result + if (deviceMac != null) deviceMac!!.hashCode() else 0
        return result
    }

}
