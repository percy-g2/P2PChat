package com.androdevlinux.percy.p2pchat

import android.graphics.BitmapFactory
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.androdevlinux.percy.p2p.common.P2PDevice
import com.androdevlinux.percy.p2p.common.messages.MessageWrapper



class ChatAdapter(private val messages: List<MessageWrapper>, private val currentDevice: P2PDevice) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val others = 0
    private val self = 1

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val viewHolder: RecyclerView.ViewHolder
        val inflater = LayoutInflater.from(viewGroup.context)

        viewHolder = if (viewType == self) {
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
        if (itemType == self) {
            val vh1 = viewHolder as ViewHolderOwner
            configureViewHolder1(vh1, position)
        } else if (itemType == others) {
            val vh2 = viewHolder as ViewHolderUser
            configureViewHolder2(vh2, position)
        }
    }

    override fun getItemCount() = messages.size


    inner class ViewHolderOwner(v: View) : RecyclerView.ViewHolder(v) {

        var ownerMessage: TextView? = null
        var ownerImage: ImageView? = null
        var textLayout: LinearLayout? = null
        var imageLayout: LinearLayout? = null
        init {
            ownerMessage = v.findViewById(R.id.text_view_message_owner) as TextView
            ownerImage = v.findViewById(R.id.image_view_message_owner) as ImageView
            textLayout = v.findViewById(R.id.textLayout) as LinearLayout
            imageLayout = v.findViewById(R.id.imageLayout) as LinearLayout
        }
    }

    inner class ViewHolderUser(v: View) : RecyclerView.ViewHolder(v) {

        var userName: TextView? = null
        var userNameImage: TextView? = null
        var userMessage: TextView? = null
        var userImage: ImageView? = null
        var textLayout: LinearLayout? = null
        var imageLayout: LinearLayout? = null
        init {
            userName = v.findViewById(R.id.text_view_username) as TextView
            userMessage = v.findViewById(R.id.text_view_message) as TextView
            userNameImage = v.findViewById(R.id.text_view_username_image) as TextView
            userImage = v.findViewById(R.id.image_view_message) as ImageView
            textLayout = v.findViewById(R.id.textLayout) as LinearLayout
            imageLayout = v.findViewById(R.id.imageLayout) as LinearLayout
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].getp2pDevice() == currentDevice) {
            self
        } else {
            others
        }
    }


    private fun configureViewHolder1(vh1: ViewHolderOwner, position: Int) {
        val message = messages[position]
        if (message.messageSubType!! == MessageWrapper.MessageSubType.TEXT) {
            vh1.ownerMessage!!.text = message.message
            vh1.imageLayout!!.visibility = View.GONE
        } else if (message.messageSubType!! == MessageWrapper.MessageSubType.IMAGE) {
            val bmp = BitmapFactory.decodeByteArray(message.image, 0, message.image!!.size)
            vh1.ownerImage!!.setImageBitmap(bmp)
            vh1.textLayout!!.visibility = View.GONE
        }
    }

    private fun configureViewHolder2(vh2: ViewHolderUser, position: Int) {
        val message = messages[position]
        if (message.messageSubType!! == MessageWrapper.MessageSubType.TEXT) {
            vh2.userName!!.text = message.getp2pDevice()!!.deviceName
            vh2.userMessage!!.text = message.message
            vh2.imageLayout!!.visibility = View.GONE
        } else if (message.messageSubType!! == MessageWrapper.MessageSubType.IMAGE) {
            val bmp = BitmapFactory.decodeByteArray(message.image, 0, message.image!!.size)
            vh2.userNameImage!!.text = message.getp2pDevice()!!.deviceName
            vh2.userImage!!.setImageBitmap(bmp)
            vh2.textLayout!!.visibility = View.GONE
        }
    }
}