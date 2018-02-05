package com.androdevlinux.percy.p2p.common.listeners


import com.androdevlinux.percy.p2p.common.P2PDevice

interface ClientDisconnectedListener {

    fun onClientDisconnected(p2PDevice: P2PDevice?)

}
