package com.androdevlinux.percy.p2pchat

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.widget.Toast
import com.androdevlinux.percy.p2p.client.P2PClient
import com.androdevlinux.percy.p2p.common.P2PDevice
import com.androdevlinux.percy.p2p.common.WiFiP2PInstance
import com.androdevlinux.percy.p2p.common.listeners.ClientConnectedListener
import com.androdevlinux.percy.p2p.common.listeners.ClientDisconnectedListener
import com.androdevlinux.percy.p2p.common.listeners.DataReceivedListener
import com.androdevlinux.percy.p2p.common.messages.MessageWrapper
import com.androdevlinux.percy.p2p.service.P2PService
import kotlinx.android.synthetic.main.activity_group_chat.*

/**
 * Created by percy on 5/2/18.
 */
class GroupChatActivity : AppCompatActivity(), DataReceivedListener, ClientConnectedListener, ClientDisconnectedListener {

    val EXTRA_GROUP_NAME: String = "groupNameExtra"
    val EXTRA_IS_GROUP_OWNER: String = "isGroupOwnerExtra"

    private var chatAdapter: ChatAdapter? = null

    private var groupName: String? = null
    private var isGroupOwner = false

    private var p2pService: P2PService? = null
    private var p2pClient: P2PClient? = null
    private val messages: List<MessageWrapper>? = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_chat)
        val linearLayoutManager = LinearLayoutManager(this)
        list_view_group_chat.layoutManager = linearLayoutManager
        chatAdapter = ChatAdapter(messages!!, WiFiP2PInstance.getInstance(applicationContext).thisDevice!!)

        val startIntent = intent
        groupName = startIntent.getStringExtra(EXTRA_GROUP_NAME)
        isGroupOwner = startIntent.getBooleanExtra(EXTRA_IS_GROUP_OWNER, false)

        if (isGroupOwner) {
            p2pService = P2PService.getInstance(applicationContext)
            p2pService!!.setDataReceivedListener(this)
            p2pService!!.setClientDisconnectedListener(this)
            p2pService!!.setClientConnectedListener(this)
        } else {
            p2pClient = P2PClient.getInstance(applicationContext)
            p2pClient!!.setDataReceivedListener(this)
            p2pClient!!.setClientDisconnectedListener(this)
            p2pClient!!.setClientConnectedListener(this)
        }


        list_view_group_chat.adapter = chatAdapter

        button_send_message.setOnClickListener {
            val messageStr = edit_text_chat_message.text.toString()
            if (!messageStr.isEmpty()) {
                val normalMessage = MessageWrapper()
                normalMessage.setp2pDevice(WiFiP2PInstance.getInstance(applicationContext).thisDevice!!)
                normalMessage.message = edit_text_chat_message.text.toString()
                normalMessage.message = edit_text_chat_message.text.toString()
                normalMessage.messageType = MessageWrapper.MessageType.NORMAL

                if (isGroupOwner) {
                    p2pService!!.sendMessageToAllClients(normalMessage)
                } else {
                    p2pClient!!.sendMessageToAllClients(normalMessage)
                }
                (messages as ArrayList<MessageWrapper>).add(normalMessage)
                chatAdapter!!.notifyDataSetChanged()
                edit_text_chat_message.editableText.clear()
            }
        }

        setActionBarTitle(groupName.toString())
        title = groupName
    }

    private fun setActionBarTitle(title: String) {
        if (actionBar != null) {
            actionBar!!.title = title
        }
    }

    override fun onClientConnected(p2PDevice: P2PDevice?) {
        runOnUiThread { Toast.makeText(applicationContext, getString(R.string.device_connected, p2PDevice!!.deviceName), Toast.LENGTH_LONG).show() }
    }

    override fun onClientDisconnected(p2PDevice: P2PDevice?) {
        runOnUiThread { Toast.makeText(applicationContext, getString(R.string.device_disconnected, p2PDevice!!.deviceName), Toast.LENGTH_LONG).show() }
    }

    override fun onDataReceived(messageWrapper: MessageWrapper?) {
        runOnUiThread {
            if (messageWrapper != null) {
                (messages as ArrayList<MessageWrapper>).add(messageWrapper)
                chatAdapter!!.notifyDataSetChanged()
            }
        }
    }
}