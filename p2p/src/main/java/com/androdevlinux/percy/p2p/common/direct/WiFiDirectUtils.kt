package com.androdevlinux.percy.p2p.common.direct


import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import com.androdevlinux.percy.p2p.common.WiFiP2PError
import com.androdevlinux.percy.p2p.common.WiFiP2PInstance

object WiFiDirectUtils {

    private val TAG = WiFiDirectUtils::class.java.simpleName

    fun clearServiceRequest(wiFiP2PInstance: WiFiP2PInstance) {
        wiFiP2PInstance.wifiP2pManager!!.clearServiceRequests(wiFiP2PInstance.channel, object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
                Log.d(TAG, "Success clearing service request")
            }

            override fun onFailure(reason: Int) {
                Log.e(TAG, "Error clearing service request: " + reason)
            }

        })
    }

    fun clearLocalServices(wiFiP2PInstance: WiFiP2PInstance) {
        wiFiP2PInstance.wifiP2pManager!!.clearLocalServices(wiFiP2PInstance.channel, object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
                Log.d(TAG, "Local services cleared")
            }

            override fun onFailure(reason: Int) {
                Log.e(TAG, "Error clearing local services: " + WiFiP2PError.fromReason(reason)!!)
            }

        })
    }

    fun cancelConnect(wiFiP2PInstance: WiFiP2PInstance) {
        wiFiP2PInstance.wifiP2pManager!!.cancelConnect(wiFiP2PInstance.channel, object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
                Log.d(TAG, "Connect canceled successfully")
            }

            override fun onFailure(reason: Int) {
                Log.e(TAG, "Error canceling connect: " + WiFiP2PError.fromReason(reason)!!)
            }

        })
    }

    fun removeGroup(wiFiP2PInstance: WiFiP2PInstance) {
        wiFiP2PInstance.wifiP2pManager!!.requestGroupInfo(wiFiP2PInstance.channel) { group ->
            wiFiP2PInstance.wifiP2pManager!!.removeGroup(wiFiP2PInstance.channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.i(TAG, "Group removed: " + group.networkName)
                }

                override fun onFailure(reason: Int) {
                    Log.e(TAG, "Fail disconnecting from group. Reason: " + WiFiP2PError.fromReason(reason)!!)
                }
            })
        }
    }

    fun stopPeerDiscovering(wiFiP2PInstance: WiFiP2PInstance) {
        wiFiP2PInstance.wifiP2pManager!!.stopPeerDiscovery(wiFiP2PInstance.channel, object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
                Log.d(TAG, "Peer discovering stopped")
            }

            override fun onFailure(reason: Int) {
                Log.e(TAG, "Error stopping peer discovering: " + WiFiP2PError.fromReason(reason)!!)
            }

        })
    }

}
