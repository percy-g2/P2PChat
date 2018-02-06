package com.androdevlinux.percy.p2p.service


import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.os.AsyncTask
import android.util.Log
import com.androdevlinux.percy.p2p.common.P2PDevice
import com.androdevlinux.percy.p2p.common.WiFiP2PError
import com.androdevlinux.percy.p2p.common.WiFiP2PInstance
import com.androdevlinux.percy.p2p.common.direct.WiFiDirectUtils
import com.androdevlinux.percy.p2p.common.listeners.*
import com.androdevlinux.percy.p2p.common.messages.DisconnectionMessageContent
import com.androdevlinux.percy.p2p.common.messages.MessageWrapper
import com.androdevlinux.percy.p2p.common.messages.RegisteredDevicesMessageContent
import com.androdevlinux.percy.p2p.common.messages.RegistrationMessageContent
import com.google.gson.Gson
import org.apache.commons.io.IOUtils
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.*

/**
 * Singleton class acting as a "server" device.
 *
 *
 * With P2P Library you can register a service in the current local network to be discovered by
 * other devices. When a service is registered a WiFi P2P Group is created, we know it as Wroup ;)
 *
 *
 * `P2PService` is the group owner and it manages the group changes (connections and
 * disconnections). When a new client is connected/disconnected the service device notify to the
 * other devices connected.
 *
 *
 * To register a service you must do the following:
 * <pre>
 * `wiFiP2PService = P2PService.getInstance(getApplicationContext());
 * wiFiP2PService.registerService(groupName, new ServiceRegisteredListener() {
 *
 * public void onSuccessServiceRegistered() {
 * Log.i(TAG, "Group created. Waiting for client connections...");
 * }
 *
 * public void onErrorServiceRegistered(WiFiP2PError wiFiP2PError) {
 * Log.e(TAG, "Error creating group");
 * }
 *
 * });
` *
</pre> *
 */
class P2PService private constructor(context: Context) : PeerConnectedListener {

    private var dataReceivedListener: DataReceivedListener? = null
    private var clientConnectedListener: ClientConnectedListener? = null
    private var clientDisconnectedListener: ClientDisconnectedListener? = null
    private val clientsConnected = HashMap<String, P2PDevice>()
    private val wiFiP2PInstance: WiFiP2PInstance

    private var serverSocket: ServerSocket? = null
    private var groupAlreadyCreated: Boolean? = false

    init {
        wiFiP2PInstance = WiFiP2PInstance.getInstance(context)
        wiFiP2PInstance.setPeerConnectedListener(this)
    }

    /**
     * Start a Wroup service registration in the actual local network with the name indicated in
     * the arguments. When te service is registered the method
     * [ServiceRegisteredListener.onSuccessServiceRegistered] is called.
     *
     * @param groupName                 The name of the group that want to be created.
     * @param serviceRegisteredListener The `ServiceRegisteredListener` to notify
     * registration changes.
     */
    fun registerService(groupName: String, serviceRegisteredListener: ServiceRegisteredListener) {
        registerService(groupName, null, serviceRegisteredListener)
    }

    /**
     * Start a Wroup service registration in the actual local network with the name indicated in
     * the arguments. When te service is registered the method
     * [ServiceRegisteredListener.onSuccessServiceRegistered] is called.
     *
     * @param groupName                 The name of the group that want to be created.
     * @param customProperties          A Map of custom properties which will be registered with the
     * service. This properties can be accessed by the client devices
     * when the service is discovered.
     * @param serviceRegisteredListener The `ServiceRegisteredListener` to notify
     * registration changes.
     */
    fun registerService(groupName: String, customProperties: Map<String, String>?, serviceRegisteredListener: ServiceRegisteredListener) {

        // We need to start peer discovering because otherwise the clients cannot found the service
        wiFiP2PInstance.startPeerDiscovering()

        val record = HashMap<String, String>()
        record.put(SERVICE_PORT_PROPERTY, SERVICE_PORT_VALUE.toString())
        record.put(SERVICE_NAME_PROPERTY, SERVICE_NAME_VALUE)
        record.put(SERVICE_GROUP_NAME, groupName)

        // Insert the custom properties to the record Map
        if (customProperties != null) {
            for ((key, value) in customProperties) {
                record.put(key, value)
            }
        }

        val serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(groupName, SERVICE_TYPE, record)

        wiFiP2PInstance.wifiP2pManager!!.clearLocalServices(wiFiP2PInstance.channel, object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
                Log.d(TAG, "Success clearing local services")
            }

            override fun onFailure(reason: Int) {
                Log.e(TAG, "Error clearing local services: " + reason)
            }
        })

        wiFiP2PInstance.wifiP2pManager!!.addLocalService(wiFiP2PInstance.channel, serviceInfo, object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
                Log.d(TAG, "Service registered")
                serviceRegisteredListener.onSuccessServiceRegistered()

                // Create the group to the clients can connect to it
                removeAndCreateGroup()

                // Create the socket that will accept request
                createServerSocket()
            }

            override fun onFailure(reason: Int) {
                val wiFiP2PError = WiFiP2PError.fromReason(reason)
                if (wiFiP2PError != null) {
                    Log.e(TAG, "Failure registering the service. Reason: " + wiFiP2PError.name)
                    serviceRegisteredListener.onErrorServiceRegistered(wiFiP2PError)
                }
            }

        })
    }

    /**
     * Remove the group created. Before the disconnection, the server sends a message to all
     * clients connected to notify the disconnection.
     */
    fun disconnect() {
        if (serverSocket != null) {
            try {
                serverSocket!!.close()
                Log.i(TAG, "ServerSocket closed")
            } catch (e: IOException) {
                Log.e(TAG, "Error closing the serverSocket")
            }

        }

        groupAlreadyCreated = false
        serverSocket = null
        clientsConnected.clear()

        WiFiDirectUtils.removeGroup(wiFiP2PInstance)
        WiFiDirectUtils.clearLocalServices(wiFiP2PInstance)
        WiFiDirectUtils.stopPeerDiscovering(wiFiP2PInstance)
    }

    /**
     * Set the listener to know when data is received from the client devices connected to the group.
     *
     * @param dataReceivedListener The `DataReceivedListener` to notify data entries.
     */
    fun setDataReceivedListener(dataReceivedListener: DataReceivedListener) {
        this.dataReceivedListener = dataReceivedListener
    }

    /**
     * Set the listener to know when a client has been disconnected from the group.
     *
     * @param clientDisconnectedListener The `ClientDisconnectedListener` to notify
     * client disconnections.
     */
    fun setClientDisconnectedListener(clientDisconnectedListener: ClientDisconnectedListener) {
        this.clientDisconnectedListener = clientDisconnectedListener
    }

    /**
     * Set the listener to know when a new client is registered in the group.
     *
     * @param clientConnectedListener The `ClientConnectedListener` to notify new
     * connections in the group.
     */
    fun setClientConnectedListener(clientConnectedListener: ClientConnectedListener) {
        this.clientConnectedListener = clientConnectedListener
    }

    override fun onPeerConnected(wifiP2pInfo: WifiP2pInfo) {
        Log.i(TAG, "OnPeerConnected...")

        if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
            Log.i(TAG, "I am the group owner")
            Log.i(TAG, "My address is: " + wifiP2pInfo.groupOwnerAddress.hostAddress)
        }
    }

    /**
     * Send a message to all the devices connected to the group.
     *
     * @param message The message to be sent.
     */
    fun sendMessageToAllClients(message: MessageWrapper) {
        for (clientDevice in clientsConnected.values) {
            sendMessage(clientDevice, message)
        }
    }

    /**
     * Send a message to the desired device who it's connected in the group.
     *
     * @param device  The receiver of the message.
     * @param message The message to be sent.
     */
    @SuppressLint("StaticFieldLeak")
    fun sendMessage(device: P2PDevice?, message: MessageWrapper) {
        // Set the actual device to the message
        message.setp2pDevice(wiFiP2PInstance.thisDevice!!)

        object : AsyncTask<MessageWrapper, Void, Void>() {
            override fun doInBackground(vararg params: MessageWrapper): Void? {
                if (device?.deviceServerSocketIP != null) {
                    try {
                        val socket = Socket()
                        socket.bind(null)

                        val hostAddres = InetSocketAddress(device.deviceServerSocketIP, device.deviceServerSocketPort)
                        socket.connect(hostAddres, 2000)

                        val gson = Gson()
                        val messageJson = gson.toJson(params[0])

                        val outputStream = socket.getOutputStream()
                        outputStream.write(messageJson.toByteArray(), 0, messageJson.toByteArray().size)

                        Log.d(TAG, "Sending data: " + params[0])

                        socket.close()
                        outputStream.close()
                    } catch (e: IOException) {
                        Log.e(TAG, "Error creating client socket: " + e.message)
                    }

                }

                return null
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, message)
    }

    @SuppressLint("StaticFieldLeak")
    private fun createServerSocket() {
        if (serverSocket == null) {
            object : AsyncTask<Void, Void, Void>() {

                override fun doInBackground(vararg params: Void): Void? {

                    try {
                        serverSocket = ServerSocket(SERVICE_PORT_VALUE)
                        Log.i(TAG, "Server socket created. Accepting requests...")

                        while (true) {
                            val socket = serverSocket!!.accept()

                            val dataReceived = IOUtils.toString(socket.getInputStream())
                            Log.i(TAG, "Data received: " + dataReceived)
                            Log.i(TAG, "From IP: " + socket.inetAddress.hostAddress)

                            val gson = Gson()
                            val messageWrapper = gson.fromJson(dataReceived, MessageWrapper::class.java)
                            onMessageReceived(messageWrapper, socket.inetAddress)
                        }
                    } catch (e: IOException) {
                        Log.e(TAG, "Error creating/closing server socket: " + e.message)
                    }

                    return null
                }

            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }


    private fun removeAndCreateGroup() {
        wiFiP2PInstance.wifiP2pManager!!.requestGroupInfo(wiFiP2PInstance.channel) { group ->
            if (group != null) {
                wiFiP2PInstance.wifiP2pManager!!.removeGroup(wiFiP2PInstance.channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Log.d(TAG, "Group deleted")
                        Log.d(TAG, "\tNetwordk Name: " + group.networkName)
                        Log.d(TAG, "\tInterface: " + group.`interface`)
                        Log.d(TAG, "\tPassword: " + group.passphrase)
                        Log.d(TAG, "\tOwner Name: " + group.owner.deviceName)
                        Log.d(TAG, "\tOwner Address: " + group.owner.deviceAddress)
                        Log.d(TAG, "\tClient list size: " + group.clientList.size)

                        groupAlreadyCreated = false

                        // Now we can create the group
                        createGroup()
                    }

                    override fun onFailure(reason: Int) {
                        Log.e(TAG, "Error deleting group")
                    }
                })
            } else {
                createGroup()
            }
        }
    }

    private fun createGroup() {
        if ((!groupAlreadyCreated!!)) {
            wiFiP2PInstance.wifiP2pManager!!.createGroup(wiFiP2PInstance.channel, object : WifiP2pManager.ActionListener {

                override fun onSuccess() {
                    Log.i(TAG, "Group created!")
                    groupAlreadyCreated = true
                }

                override fun onFailure(reason: Int) {
                    Log.e(TAG, "Error creating group. Reason: " + WiFiP2PError.fromReason(reason)!!)
                }
            })
        }
    }

    private fun onMessageReceived(messageWrapper: MessageWrapper, fromAddress: InetAddress) {
        if (messageWrapper.messageType == MessageWrapper.MessageType.CONNECTION_MESSAGE) {
            val gson = Gson()

            val messageContentStr = messageWrapper.message
            val registrationMessageContent = gson.fromJson(messageContentStr, RegistrationMessageContent::class.java)
            val client = registrationMessageContent.p2pDevice
            client!!.deviceServerSocketIP = fromAddress.hostAddress
            clientsConnected[client.deviceMac!!] = client

            Log.d(TAG, "New client registered:")
            Log.d(TAG, "\tDevice name: " + client.deviceName!!)
            Log.d(TAG, "\tDevice mac: " + client.deviceMac!!)
            Log.d(TAG, "\tDevice IP: " + client.deviceServerSocketIP!!)
            Log.d(TAG, "\tDevice ServerSocket port: " + client.deviceServerSocketPort)

            // Sending to all clients that new client is connected
            for (device in clientsConnected.values) {
                if (client.deviceMac != device.deviceMac) {
                    sendConnectionMessage(device, client)
                } else {
                    sendRegisteredDevicesMessage(device)
                }
            }

            if (clientConnectedListener != null) {
                clientConnectedListener!!.onClientConnected(client)
            }
        } else if (messageWrapper.messageType == MessageWrapper.MessageType.DISCONNECTION_MESSAGE) {
            val gson = Gson()

            val messageContentStr = messageWrapper.message
            val disconnectionMessageContent = gson.fromJson(messageContentStr, DisconnectionMessageContent::class.java)
            val client = disconnectionMessageContent.p2pDevice
            clientsConnected.remove(client!!.deviceMac)

            Log.d(TAG, "Client disconnected:")
            Log.d(TAG, "\tDevice name: " + client.deviceName!!)
            Log.d(TAG, "\tDevice mac: " + client.deviceMac!!)
            Log.d(TAG, "\tDevice IP: " + client.deviceServerSocketIP!!)
            Log.d(TAG, "\tDevice ServerSocket port: " + client.deviceServerSocketPort)

            // Sending to all clients that a client is disconnected now
            clientsConnected.values
                    .filter { client.deviceMac != it.deviceMac }
                    .forEach { sendDisconnectionMessage(it, client) }

            if (clientDisconnectedListener != null) {
                clientDisconnectedListener!!.onClientDisconnected(client)
            }
        } else {
            if (dataReceivedListener != null) {
                dataReceivedListener!!.onDataReceived(messageWrapper)
            }
        }
    }

    private fun sendConnectionMessage(deviceToSend: P2PDevice, deviceConnected: P2PDevice) {
        val content = RegistrationMessageContent()
        content.p2pDevice = deviceConnected

        val gson = Gson()

        val messageWrapper = MessageWrapper()
        messageWrapper.messageType = MessageWrapper.MessageType.CONNECTION_MESSAGE
        messageWrapper.message = gson.toJson(content)

        sendMessage(deviceToSend, messageWrapper)
    }

    private fun sendDisconnectionMessage(deviceToSend: P2PDevice, deviceDisconnected: P2PDevice) {
        val content = DisconnectionMessageContent()
        content.p2pDevice = deviceDisconnected

        val gson = Gson()

        val disconnectionMessage = MessageWrapper()
        disconnectionMessage.messageType = MessageWrapper.MessageType.DISCONNECTION_MESSAGE
        disconnectionMessage.message = gson.toJson(content)

        sendMessage(deviceToSend, disconnectionMessage)
    }

    private fun sendRegisteredDevicesMessage(deviceToSend: P2PDevice) {
        val devicesConnected = clientsConnected.values.filter { it.deviceMac != deviceToSend.deviceMac }

        val content = RegisteredDevicesMessageContent()
        content.devicesRegistered = devicesConnected

        val gson = Gson()

        val messageWrapper = MessageWrapper()
        messageWrapper.messageType = MessageWrapper.MessageType.REGISTERED_DEVICES
        messageWrapper.message = gson.toJson(content)

        sendMessage(deviceToSend, messageWrapper)
    }

    companion object {


        private val TAG = P2PService::class.java.simpleName

        private val SERVICE_TYPE = "_p2p._tcp"
        val SERVICE_PORT_PROPERTY = "SERVICE_PORT"
        val SERVICE_PORT_VALUE = 9999
        val SERVICE_NAME_PROPERTY = "SERVICE_NAME"
        val SERVICE_NAME_VALUE = "P2P"
        val SERVICE_GROUP_NAME = "GROUP_NAME"

        private var instance: P2PService? = null

        /**
         * Return the `P2PService` instance. If the instance doesn't exist yet, it's
         * created and returned.
         *
         * @param context The application context.
         * @return The actual `P2PService` instance.
         */
        fun getInstance(context: Context): P2PService {
            if (instance == null) {
                instance = P2PService(context)
            }
            return instance!!
        }
    }

}
