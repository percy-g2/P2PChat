package com.androdevlinux.percy.p2p.client


import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.AsyncTask
import android.os.Handler
import android.util.Log
import com.androdevlinux.percy.p2p.common.*
import com.androdevlinux.percy.p2p.common.direct.WiFiDirectUtils
import com.androdevlinux.percy.p2p.common.listeners.*
import com.androdevlinux.percy.p2p.common.messages.DisconnectionMessageContent
import com.androdevlinux.percy.p2p.common.messages.MessageWrapper
import com.androdevlinux.percy.p2p.common.messages.RegisteredDevicesMessageContent
import com.androdevlinux.percy.p2p.common.messages.RegistrationMessageContent
import com.androdevlinux.percy.p2p.service.P2PService
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import org.apache.commons.io.IOUtils
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.*

/**
 * Singleton class acting as a client device.
 *
 *
 * P2P Library will allow you to create a "Server" device and multiple "Client" devices. The
 * [P2PService] can register a service which could be discover by the multiple client
 * devices. The client will search the Wroup services registered in the local network and could
 * connect to any ot them.
 *
 *
 * P2PClient only discover Group services, can exist multiple services registered with WiFi-P2P in
 * the same local network but only a P2PClient instance will found services registered by a
 * P2PService device.
 *
 *
 * To discover the Group services registered you only need to do the following:
 * <pre>
 * `wiFiP2PClient = P2PClient.getInstance(getApplicationContext());
 * wiFiP2PClient.discoverServices(5000L, new ServiceDiscoveredListener() {
 *
 * public void onNewServiceDeviceDiscovered(P2PDeviceService serviceDevice) {
 * Log.i(TAG, "New service found:");
 * Log.i(TAG, "\tName: " + serviceDevice.getDeviceName());
 * }
 *
 * public void onFinishServiceDeviceDiscovered(List<P2PDeviceService> serviceDevices) {
 * Log.i(TAG, "Found '" + serviceDevices.size() + "' services");
 * }
 *
 * public void onError(WiFiP2PError wiFiP2PError) {
 * Toast.makeText(getApplicationContext(), "Error searching groups: " + wiFiP2PError, Toast.LENGTH_LONG).show();
 * }
 * });
` *
</pre> *
 * Once that you have the desired service to which connect you must call to
 * [.connectToService] passing as argument the
 * appropriate [P2PDeviceService] obtained in the `discoverServices()` call.
 */
class P2PClient private constructor(context: Context) : PeerConnectedListener, ServiceDisconnectedListener {

    private val serviceDevices = ArrayList<P2PDeviceService>()

    private var dnsSdTxtRecordListener: DnsSdTxtRecordListener? = null
    private var dnsSdServiceResponseListener: DnsSdServiceResponseListener? = null
    private var serviceConnectedListener: ServiceConnectedListener? = null
    private var dataReceivedListener: DataReceivedListener? = null
    private var serviceDisconnectedListener: ServiceDisconnectedListener? = null
    private var clientConnectedListener: ClientConnectedListener? = null
    private var clientDisconnectedListener: ClientDisconnectedListener? = null

    private var serverSocket: ServerSocket? = null

    private val wiFiP2PInstance: WiFiP2PInstance
    private var serviceDevice: P2PDevice? = null
    private val clientsConnected: MutableMap<String, P2PDevice>
    private var isRegistered: Boolean? = false

    private val serviceResponseListener: DnsSdServiceResponseListener
        get() = DnsSdServiceResponseListener { instanceName, registrationType, srcDevice -> }

    init {
        wiFiP2PInstance = WiFiP2PInstance.getInstance(context)
        wiFiP2PInstance.setPeerConnectedListener(this)
        wiFiP2PInstance.setServerDisconnectedListener(this)
        this.clientsConnected = HashMap()
    }

    /**
     * Start to discover Wroup services registered in the current local network.
     *
     *
     * Before you start to discover services you must to register the `WiFiDirectBroadcastReceiver`
     * in the `onResume()` method of your activity.
     *
     * @param discoveringTimeInMillis   The time in milliseconds to search for registered Wroup services.
     * @param serviceDiscoveredListener The listener to notify changes of the services found by the client.
     * @see WiFiDirectBroadcastReceiver
     */
    fun discoverServices(discoveringTimeInMillis: Long?, serviceDiscoveredListener: ServiceDiscoveredListener) {
        serviceDevices.clear()

        // We need to start discovering peers to activate the service search
        wiFiP2PInstance.startPeerDiscovering()

        setupDnsListeners(wiFiP2PInstance, serviceDiscoveredListener)
        WiFiDirectUtils.clearServiceRequest(wiFiP2PInstance)

        val serviceRequest = WifiP2pDnsSdServiceRequest.newInstance()
        wiFiP2PInstance.wifiP2pManager!!.addServiceRequest(wiFiP2PInstance.channel, serviceRequest, object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
                Log.d(TAG, "Success adding service request")
            }

            override fun onFailure(reason: Int) {
                val wiFiP2PError = WiFiP2PError.fromReason(reason)
                Log.e(TAG, "Error adding service request. Reason: " + WiFiP2PError.fromReason(reason)!!)
                serviceDiscoveredListener.onError(wiFiP2PError!!)
            }

        })

        wiFiP2PInstance.wifiP2pManager!!.discoverServices(wiFiP2PInstance.channel, object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
                Log.d(TAG, "Success initiating discovering services")
            }

            override fun onFailure(reason: Int) {
                val wiFiP2PError = WiFiP2PError.fromReason(reason)
                if (wiFiP2PError != null) {
                    Log.e(TAG, "Error discovering services. Reason: " + wiFiP2PError.name)
                    serviceDiscoveredListener.onError(wiFiP2PError)
                }
            }

        })

        val handler = Handler()
        handler.postDelayed({ serviceDiscoveredListener.onFinishServiceDeviceDiscovered(serviceDevices) }, discoveringTimeInMillis!!)
    }

    /**
     * Start the connection with the `P2PDeviceService` passed by argument. When the
     * connection is stabilised with the device service the [ServiceConnectedListener.onServiceConnected]
     * method is called.
     *
     *
     * When the client is connected to the service, it's connected to the WiFi Direct Group created
     * by the service device. Once the client belongs to the "Wroup" (group), it can know when a new
     * client is connected or disconnected from it.
     *
     * @param serviceDevice            The P2PDeviceService with you want to connect.
     * @param serviceConnectedListener The listener to know when the client device is connected to
     * the desired service.
     */
    fun connectToService(serviceDevice: P2PDeviceService, serviceConnectedListener: ServiceConnectedListener) {
        this.serviceDevice = serviceDevice
        this.serviceConnectedListener = serviceConnectedListener

        val wifiP2pConfig = WifiP2pConfig()
        wifiP2pConfig.deviceAddress = serviceDevice.deviceMac

        wiFiP2PInstance.wifiP2pManager!!.connect(wiFiP2PInstance.channel, wifiP2pConfig, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.i(TAG, "Initiated connection to device: ")
                Log.i(TAG, "\tDevice name: " + serviceDevice.deviceName)
                Log.i(TAG, "\tDevice address: " + serviceDevice.deviceMac)
            }

            override fun onFailure(reason: Int) {
                Log.e(TAG, "Fail initiation connection. Reason: " + WiFiP2PError.fromReason(reason)!!)
            }
        })
    }

    /**
     * Set the listener to know when data is received from the service device or other client devices
     * connected to the same group.
     *
     * @param dataReceivedListener The `DataReceivedListener` to notify data entries.
     */
    fun setDataReceivedListener(dataReceivedListener: DataReceivedListener) {
        this.dataReceivedListener = dataReceivedListener
    }

    /**
     * Set the listener to notify when the service device has been disconnected.
     *
     * @param serviceDisconnectedListener The `ServiceDisconnectedListener` to notify
     * service device disconnections.
     */
    fun setServerDisconnetedListener(serviceDisconnectedListener: ServiceDisconnectedListener) {
        this.serviceDisconnectedListener = serviceDisconnectedListener
    }

    /**
     * Set the listener to know when a new client is registered in the actual group.
     *
     * @param clientConnectedListener The `ClientConnectedListener` to notify new
     * connections in the group.
     */
    fun setClientConnectedListener(clientConnectedListener: ClientConnectedListener) {
        this.clientConnectedListener = clientConnectedListener
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

    override fun onPeerConnected(wifiP2pInfo: WifiP2pInfo) {
        Log.i(TAG, "OnPeerConnected...")

        if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
            Log.e(TAG, "I shouldn't be the group owner, I'am a client!")
        }

        if (wifiP2pInfo.groupFormed && serviceDevice != null && (!isRegistered!!)) {
            serviceDevice!!.deviceServerSocketIP = wifiP2pInfo.groupOwnerAddress.hostAddress
            Log.i(TAG, "The Server Address is: " + wifiP2pInfo.groupOwnerAddress.hostAddress)

            // We are connected to the server. Create a server socket to receive messages
            createServerSocket()

            // Wait 2 seconds for the server socket creation
            val handler = Handler()
            handler.postDelayed({
                // We send the negotiation message to the server
                sendServerRegistrationMessage()
                if (serviceConnectedListener != null) {
                    serviceConnectedListener!!.onServiceConnected(serviceDevice!!)
                }

                isRegistered = true
            }, 2000)
        }
    }

    override fun onServerDisconnectedListener() {
        // If the server is disconnected the client is cleared
        disconnect()

        if (serviceDisconnectedListener != null) {
            serviceDisconnectedListener!!.onServerDisconnectedListener()
        }
    }

    /**
     * Send a message to the service device.
     *
     * @param message The message to be sent.
     */
    fun sendMessageToServer(message: MessageWrapper) {
        sendMessage(serviceDevice, message)
    }

    /**
     * Send a message to all the devices connected to the group, including the service device.
     *
     * @param message The message to be sent.
     */
    fun sendMessageToAllClients(message: MessageWrapper) {
        sendMessageToServer(message)

        clientsConnected.values
                .filter { it.deviceMac != wiFiP2PInstance.thisDevice!!.deviceMac }
                .forEach { sendMessage(it, message) }
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
        message.setP2pDevice(wiFiP2PInstance.thisDevice!!)

        object : AsyncTask<MessageWrapper, Void, Void>() {
            override fun doInBackground(vararg params: MessageWrapper): Void? {
                if (device?.deviceServerSocketIP != null) {
                    try {
                        val socket = Socket()
                        socket.bind(null)

                        val hostAddress = InetSocketAddress(device.deviceServerSocketIP, device.deviceServerSocketPort)
                        socket.connect(hostAddress, 2000)

                        val mapper = ObjectMapper()
                        val messageJson = mapper.writeValueAsString(params[0])

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

    /**
     * Disconnect from the actual group connected. Before the disconnection, the client sends a
     * message to the service device to notify the disconnection.
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

        sendDisconnectionMessage()

        // Wait 2 seconds to disconnection message was sent
        val handler = Handler()
        handler.postDelayed({
            WiFiDirectUtils.clearServiceRequest(wiFiP2PInstance)
            WiFiDirectUtils.stopPeerDiscovering(wiFiP2PInstance)
            WiFiDirectUtils.removeGroup(wiFiP2PInstance)

            serverSocket = null
            isRegistered = false
            clientsConnected.clear()
        }, 2000)
    }

    /**
     * Obtain the devices connected to the actual group.
     *
     * @return the devices connected to the actual group.
     */
    fun getClientsConnected(): Collection<P2PDevice> {
        return clientsConnected.values
    }

    private fun setupDnsListeners(wiFiP2PInstance: WiFiP2PInstance, serviceDiscoveredListener: ServiceDiscoveredListener) {
        if (dnsSdTxtRecordListener == null || dnsSdServiceResponseListener == null) {
            dnsSdTxtRecordListener = getTxtRecordListener(serviceDiscoveredListener)
            dnsSdServiceResponseListener = serviceResponseListener

            wiFiP2PInstance.wifiP2pManager!!.setDnsSdResponseListeners(wiFiP2PInstance.channel, dnsSdServiceResponseListener, dnsSdTxtRecordListener)
        }
    }

    private fun getTxtRecordListener(serviceDiscoveredListener: ServiceDiscoveredListener): DnsSdTxtRecordListener {
        return DnsSdTxtRecordListener { fullDomainName, txtRecordMap, device ->
            if (txtRecordMap.containsKey(P2PService.SERVICE_NAME_PROPERTY) && txtRecordMap[P2PService.SERVICE_NAME_PROPERTY].equals(P2PService.SERVICE_NAME_VALUE, ignoreCase = true)) {
                val servicePort = Integer.valueOf(txtRecordMap[P2PService.SERVICE_PORT_PROPERTY])
                val serviceDevice = P2PDeviceService(device)
                serviceDevice.deviceServerSocketPort = servicePort!!
                serviceDevice.txtRecordMap = txtRecordMap

                if (!serviceDevices.contains(serviceDevice)) {
                    Log.i(TAG, "Found a new Wroup service: ")
                    Log.i(TAG, "\tDomain Name: " + fullDomainName)
                    Log.i(TAG, "\tDevice Name: " + device.deviceName)
                    Log.i(TAG, "\tDevice Address: " + device.deviceAddress)
                    Log.i(TAG, "\tServer socket Port: " + serviceDevice.deviceServerSocketPort)

                    serviceDevices.add(serviceDevice)
                    serviceDiscoveredListener.onNewServiceDeviceDiscovered(serviceDevice)
                }
            } else {
                Log.d(TAG, "Found a new service: ")
                Log.d(TAG, "\tDomain Name: " + fullDomainName)
                Log.d(TAG, "\tDevice Name: " + device.deviceName)
                Log.d(TAG, "\tDevice Address: " + device.deviceAddress)
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private fun createServerSocket() {
        if (serverSocket == null) {
            object : AsyncTask<Void, Void, Void>() {

                override fun doInBackground(vararg params: Void): Void? {

                    try {
                        serverSocket = ServerSocket(0)

                        val port = serverSocket!!.localPort
                        wiFiP2PInstance.thisDevice!!.deviceServerSocketPort = port

                        Log.i(TAG, "Client ServerSocket created. Accepting requests...")
                        Log.i(TAG, "\tPort: " + port)

                        while (true) {
                            val socket = serverSocket!!.accept()

                            val dataReceived = IOUtils.toString(socket.getInputStream())
                            Log.i(TAG, "Data received: " + dataReceived)
                            Log.i(TAG, "From IP: " + socket.inetAddress.hostAddress)

                            val mapper = ObjectMapper()
                            val messageWrapper = mapper.readValue(dataReceived, MessageWrapper::class.java)
                            onMessageReceived(messageWrapper)
                        }
                    } catch (e: IOException) {
                        Log.e(TAG, "Error creating/closing client ServerSocket: " + e.message)
                    }

                    return null
                }

            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    private fun onMessageReceived(messageWrapper: MessageWrapper) {
        if (MessageWrapper.MessageType.CONNECTION_MESSAGE == messageWrapper.messageType) {
            val gson = Gson()

            val messageContentStr = messageWrapper.message
            val registrationMessageContent = gson.fromJson<RegistrationMessageContent>(messageContentStr, RegistrationMessageContent::class.java)
            val device = registrationMessageContent.p2pDevice
            clientsConnected[device!!.deviceMac!!] = device

            if (clientConnectedListener != null) {
                clientConnectedListener!!.onClientConnected(device)
            }

            Log.d(TAG, "New client connected to the group:")
            Log.d(TAG, "\tDevice name: " + device.deviceName)
            Log.d(TAG, "\tDecive mac: " + device.deviceMac)
            Log.d(TAG, "\tDevice IP: " + device.deviceServerSocketIP)
            Log.d(TAG, "\tDevice ServerSocket port: " + device.deviceServerSocketPort)
        } else if (MessageWrapper.MessageType.DISCONNECTION_MESSAGE == messageWrapper.messageType) {
            val gson = Gson()

            val messageContentStr = messageWrapper.message
            val disconnectionMessageContent = gson.fromJson<DisconnectionMessageContent>(messageContentStr, DisconnectionMessageContent::class.java)
            val device = disconnectionMessageContent.p2pDevice
            clientsConnected.remove(device!!.deviceMac)

            if (clientDisconnectedListener != null) {
                clientDisconnectedListener!!.onClientDisconnected(device)
            }

            Log.d(TAG, "Client disconnected from the group:")
            Log.d(TAG, "\tDevice name: " + device.deviceName)
            Log.d(TAG, "\tDecive mac: " + device.deviceMac)
            Log.d(TAG, "\tDevice IP: " + device.deviceServerSocketIP)
            Log.d(TAG, "\tDevice ServerSocket port: " + device.deviceServerSocketPort)
        } else if (MessageWrapper.MessageType.REGISTERED_DEVICES == messageWrapper.messageType) {
            val gson = Gson()

            val messageContentStr = messageWrapper.message
            val registeredDevicesMessageContent = gson.fromJson<RegisteredDevicesMessageContent>(messageContentStr, RegisteredDevicesMessageContent::class.java)
            val devicesConnected = registeredDevicesMessageContent.devicesRegistered

            for (device in devicesConnected!!) {
                clientsConnected[device.deviceMac!!] = device
                Log.d(TAG, "Client already connected to the group:")
                Log.d(TAG, "\tDevice name: " + device.deviceName)
                Log.d(TAG, "\tDecive mac: " + device.deviceMac)
                Log.d(TAG, "\tDevice IP: " + device.deviceServerSocketIP)
                Log.d(TAG, "\tDevice ServerSocket port: " + device.deviceServerSocketPort)
            }
        } else {
            if (dataReceivedListener != null) {
                dataReceivedListener!!.onDataReceived(messageWrapper)
            }
        }
    }

    private fun sendServerRegistrationMessage() {
        val content = RegistrationMessageContent()
        content.p2pDevice = wiFiP2PInstance.thisDevice

        val gson = Gson()

        val negotiationMessage = MessageWrapper()
        negotiationMessage.messageType = MessageWrapper.MessageType.CONNECTION_MESSAGE
        negotiationMessage.message = gson.toJson(content)

        sendMessageToServer(negotiationMessage)
    }

    private fun sendDisconnectionMessage() {
        val content = DisconnectionMessageContent()
        content.p2pDevice = wiFiP2PInstance.thisDevice

        val gson = Gson()

        val disconnectionMessage = MessageWrapper()
        disconnectionMessage.messageType = MessageWrapper.MessageType.DISCONNECTION_MESSAGE
        disconnectionMessage.message = gson.toJson(content)

        sendMessageToServer(disconnectionMessage)
    }

    companion object {

        private val TAG = P2PClient::class.java.simpleName

        private var instance: P2PClient? = null

        /**
         * Return the P2PClient instance. If the instance doesn't exist yet, it's created and returned.
         *
         * @param context The application context.
         * @return The actual P2PClient instance.
         */
        fun getInstance(context: Context?): P2PClient? {
            if (instance == null && context != null) {
                instance = P2PClient(context)
            }
            return instance
        }
    }

}
