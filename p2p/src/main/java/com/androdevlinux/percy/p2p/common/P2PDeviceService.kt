package com.androdevlinux.percy.p2p.common


import android.net.wifi.p2p.WifiP2pDevice

class P2PDeviceService(wifiP2pDevice: WifiP2pDevice) : P2PDevice(wifiP2pDevice) {
    var txtRecordMap: Map<String, String>? = null
}
