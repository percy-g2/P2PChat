package com.androdevlinux.percy.p2p.common.messages;

import com.androdevlinux.percy.p2p.common.P2PDevice;

import java.util.List;

public class RegisteredDevicesMessageContent {

    private List<P2PDevice> devicesRegistered;

    public List<P2PDevice> getDevicesRegistered() {
        return devicesRegistered;
    }

    public void setDevicesRegistered(List<P2PDevice> devicesRegistered) {
        this.devicesRegistered = devicesRegistered;
    }

}
