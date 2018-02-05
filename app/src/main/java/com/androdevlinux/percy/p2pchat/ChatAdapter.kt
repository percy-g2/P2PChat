package com.androdevlinux.percy.p2pchat

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.androdevlinux.percy.p2p.common.P2PDevice
import com.androdevlinux.percy.p2p.common.messages.MessageWrapper

class ChatAdapter(private val messages: List<MessageWrapper>, private val currentDevice: P2PDevice) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val USER = 0
    private val OWNER = 1

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val viewHolder: RecyclerView.ViewHolder
        val inflater = LayoutInflater.from(viewGroup.context)

        viewHolder = if (viewType == OWNER) {
            val v1 = inflater.inflate(R.layout.adapter_chat_owner, viewGroup, false)
            ViewHolderOwner(v1)
        } else {
            val v2 = inflater.inflate(R.layout.adapter_chat, viewGroup, false)
            ViewHolderUser(v2)
        }
        return viewHolder
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val itemType = getItemViewType(position)
        if (itemType == OWNER) {
            val vh1 = viewHolder as ViewHolderOwner
            configureViewHolder1(vh1, position)
        } else if (itemType == USER) {
            val vh2 = viewHolder as ViewHolderUser
            configureViewHolder2(vh2, position)
        }
    }

    override fun getItemCount() = messages.size


    inner class ViewHolderOwner(v: View) : RecyclerView.ViewHolder(v) {

        var ownerMessage: TextView? = null

        init {
            ownerMessage = v.findViewById<TextView>(R.id.text_view_message_owner) as TextView
        }
    }

    inner class ViewHolderUser(v: View) : RecyclerView.ViewHolder(v) {

        var userName: TextView? = null
        var userMessage: TextView? = null

        init {
            userName = v.findViewById<TextView>(R.id.text_view_username) as TextView
            userMessage = v.findViewById<TextView>(R.id.text_view_message) as TextView
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].getp2pDevice() == currentDevice) {
            OWNER
        } else {
            USER
        }
    }


    private fun configureViewHolder1(vh1: ViewHolderOwner, position: Int) {
        val message = messages[position]
        vh1.ownerMessage!!.text = message.message
    }

    private fun configureViewHolder2(vh2: ViewHolderUser, position: Int) {
        val message = messages[position]
        vh2.userName!!.text = message.getp2pDevice()!!.deviceName
        vh2.userMessage!!.text = message.message
    }
}