package com.androdevlinux.percy.p2p.common.listeners


import com.androdevlinux.percy.p2p.common.WiFiP2PError

interface ServiceRegisteredListener {

    fun onSuccessServiceRegistered()

    fun onErrorServiceRegistered(wiFiP2PError: WiFiP2PError)

}
