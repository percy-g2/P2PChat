package com.androdevlinux.percy.p2p.common.listeners


import com.androdevlinux.percy.p2p.common.messages.MessageWrapper

interface DataReceivedListener {

    fun onDataReceived(messageWrapper: MessageWrapper?)

}
