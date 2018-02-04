package com.androdevlinux.percy.p2p.common.messages;


import com.androdevlinux.percy.p2p.common.P2PDevice;

public class DisconnectionMessageContent {

    private P2PDevice p2PDevice;


    public void setWroupDevice(P2PDevice p2PDevice) {
        this.p2PDevice = p2PDevice;
    }

    public P2PDevice getWroupDevice() {
        return p2PDevice;
    }

}
