package com.androdevlinux.percy.p2p.common.listeners;


import com.androdevlinux.percy.p2p.common.WiFiP2PError;

public interface ServiceRegisteredListener {

    void onSuccessServiceRegistered();

    void onErrorServiceRegistered(WiFiP2PError wiFiP2PError);

}
