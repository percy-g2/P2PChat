package com.androdevlinux.percy.p2p.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log


class WiFiDirectBroadcastReceiver(private val wiFiP2PInstance: WiFiP2PInstance?) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION == action) {

            val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Log.i(TAG, "WiFi P2P is active")
            } else {
                Log.i(TAG, "WiFi P2P isn't active")
            }

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION == action) {

            Log.d(TAG, "New peers detected. Requesting peers list...")

            wiFiP2PInstance?.wifiP2pManager?.requestPeers(wiFiP2PInstance.channel) { peers ->
                if (!peers.deviceList.isEmpty()) {
                    Log.d(TAG, "Peers detected:")

                    for (device in peers.deviceList) {
                        Log.d(TAG, "\tDevice Name: " + device.deviceName)
                        Log.d(TAG, "\tDevice Address: " + device.deviceAddress)
                    }
                } else {
                    Log.d(TAG, "No peers detected")
                }
            }

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION == action) {

            val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
            if (networkInfo.isConnected) {
                Log.d(TAG, "New device is connected")
                wiFiP2PInstance!!.wifiP2pManager!!.requestConnectionInfo(wiFiP2PInstance.channel, wiFiP2PInstance)
            } else {
                Log.d(TAG, "The server device has been disconnected")
                wiFiP2PInstance!!.onServerDeviceDisconnected()
            }

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION == action) {

            val device = intent.getParcelableExtra<WifiP2pDevice>(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
            Log.d(TAG, "This device name: " + device.deviceName)
            Log.d(TAG, "This device address: " + device.deviceAddress)

            if (wiFiP2PInstance!!.thisDevice == null) {
                wiFiP2PInstance.thisDevice = P2PDevice(device)
            }
        }
    }

    companion object {

        private val TAG = WiFiDirectBroadcastReceiver::class.java.name
    }

}
