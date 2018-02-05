package com.androdevlinux.percy.p2p.common.listeners


import com.androdevlinux.percy.p2p.common.P2PDevice

interface ClientConnectedListener {

    fun onClientConnected(p2PDevice: P2PDevice)

}
