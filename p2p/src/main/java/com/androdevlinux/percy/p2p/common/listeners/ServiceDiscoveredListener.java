package com.androdevlinux.percy.p2p.common.listeners;

import com.androdevlinux.percy.p2p.common.P2PDeviceService;
import com.androdevlinux.percy.p2p.common.WiFiP2PError;

import java.util.List;

public interface ServiceDiscoveredListener {

    void onNewServiceDeviceDiscovered(P2PDeviceService p2PDeviceService);

    void onFinishServiceDeviceDiscovered(List<P2PDeviceService> p2PDeviceServices);

    void onError(WiFiP2PError wiFiP2PError);

}
