package com.androdevlinux.percy.p2p.common.messages

import com.androdevlinux.percy.p2p.common.P2PDevice

class MessageWrapper {

    var message: String? = null
    var image: ByteArray? = null
    var messageType: MessageType? = null
    var messageSubType: MessageSubType? = null
    private var p2PDevice: P2PDevice? = null

    enum class MessageType {
        NORMAL, CONNECTION_MESSAGE, DISCONNECTION_MESSAGE, REGISTERED_DEVICES
    }

    enum class MessageSubType {
        TEXT, IMAGE
    }

    fun setp2pDevice(p2PDevice: P2PDevice) {
        this.p2PDevice = p2PDevice
    }

    fun getp2pDevice(): P2PDevice? {
        return p2PDevice
    }

    override fun toString(): String {
        return "MessageWrapper{" +
                "message = '" + message + '\'' +
                ", messageType = " + messageType +
                ", p2PDevice = " + p2PDevice +
                '}'
    }

}
