package com.androdevlinux.percy.p2p.common;


import android.net.wifi.p2p.WifiP2pDevice;

import java.util.Map;

public class P2PDeviceService extends P2PDevice {


    private Map<String, String> txtRecordMap;

    public P2PDeviceService(WifiP2pDevice wifiP2pDevice) {
        super(wifiP2pDevice);
    }

    public Map<String, String> getTxtRecordMap() {
        return txtRecordMap;
    }

    public void setTxtRecordMap(Map<String, String> txtRecordMap) {
        this.txtRecordMap = txtRecordMap;
    }

}
