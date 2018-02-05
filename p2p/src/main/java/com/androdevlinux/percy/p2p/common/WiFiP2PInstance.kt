package com.androdevlinux.percy.p2p.common


import android.content.Context
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log

import com.androdevlinux.percy.p2p.common.listeners.PeerConnectedListener
import com.androdevlinux.percy.p2p.common.listeners.ServiceDisconnectedListener


class WiFiP2PInstance private constructor() : WifiP2pManager.ConnectionInfoListener {

    var wifiP2pManager: WifiP2pManager? = null
    var channel: WifiP2pManager.Channel? = null
    var broadcastReceiver: WiFiDirectBroadcastReceiver? = null

    var thisDevice: P2PDevice? = null

    private var peerConnectedListener: PeerConnectedListener? = null
    private var serviceDisconnectedListener: ServiceDisconnectedListener? = null

    private constructor(context: Context) : this() {

        wifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = wifiP2pManager!!.initialize(context, context.mainLooper, null)
        broadcastReceiver = WiFiDirectBroadcastReceiver(this)
    }

    fun setPeerConnectedListener(peerConnectedListener: PeerConnectedListener) {
        this.peerConnectedListener = peerConnectedListener
    }

    fun setServerDisconnectedListener(serviceDisconnectedListener: ServiceDisconnectedListener) {
        this.serviceDisconnectedListener = serviceDisconnectedListener
    }

    fun startPeerDiscovering() {
        wifiP2pManager!!.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.i(TAG, "Peers discovering initialized")
            }

            override fun onFailure(reason: Int) {
                Log.e(TAG, "Error initiating peer disconvering. Reason: " + reason)
            }
        })
    }

    override fun onConnectionInfoAvailable(info: WifiP2pInfo) {
        if (peerConnectedListener != null) {
            peerConnectedListener!!.onPeerConnected(info)
        }
    }

    fun onServerDeviceDisconnected() {
        if (serviceDisconnectedListener != null) {
            serviceDisconnectedListener!!.onServerDisconnectedListener()
        }
    }

    companion object {

        private val TAG = WiFiP2PInstance::class.java.simpleName

        private var instance: WiFiP2PInstance? = null


        fun getInstance(context: Context): WiFiP2PInstance {
            if (instance == null) {
                instance = WiFiP2PInstance(context)
            }

            return instance!!
        }
    }

}
