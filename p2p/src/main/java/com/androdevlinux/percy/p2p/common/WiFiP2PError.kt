package com.androdevlinux.percy.p2p.common


enum class WiFiP2PError private constructor(val reason: Int) {

    ERROR(0), P2P_NOT_SUPPORTED(1), BUSSY(2);

    companion object {
        fun fromReason(reason: Int): WiFiP2PError? {
            return WiFiP2PError.values().firstOrNull { reason == it.reason }
        }
    }
}
