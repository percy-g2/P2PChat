package com.androdevlinux.percy.p2p.common.listeners;


import com.androdevlinux.percy.p2p.common.messages.MessageWrapper;

public interface DataReceivedListener {

    void onDataReceived(MessageWrapper messageWrapper);

}
