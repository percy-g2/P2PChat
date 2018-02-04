package com.androdevlinux.percy.p2p.common.messages;

import com.androdevlinux.percy.p2p.common.P2PDevice;

public class MessageWrapper {

    public enum MessageType {
        NORMAL, CONNECTION_MESSAGE, DISCONNECTION_MESSAGE, REGISTERED_DEVICES;
    }

    private String message;
    private MessageType messageType;
    private P2PDevice p2PDevice;


    public void setWroupDevice(P2PDevice p2PDevice) {
        this.p2PDevice = p2PDevice;
    }

    public P2PDevice getWroupDevice() {
        return p2PDevice;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    @Override
    public String toString() {
        return "MessageWrapper{" +
                "message = '" + message + '\'' +
                ", messageType = " + messageType +
                ", p2PDevice = " + p2PDevice +
                '}';
    }

}
