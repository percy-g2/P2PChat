package com.androdevlinux.percy.p2pchat

import android.app.ProgressDialog
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.androdevlinux.percy.p2p.client.P2PClient
import com.androdevlinux.percy.p2p.common.*
import com.androdevlinux.percy.p2p.common.listeners.ServiceConnectedListener
import com.androdevlinux.percy.p2p.common.listeners.ServiceDiscoveredListener
import com.androdevlinux.percy.p2p.common.listeners.ServiceRegisteredListener
import com.androdevlinux.percy.p2p.service.P2PService
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.util.*

class MainActivity : AppCompatActivity(), GroupCreationDialog.GroupCreationAcceptButtonListener {

    override fun onAcceptButtonListener(groupName: String) {
        if (!groupName.isEmpty()) {
            p2pService = P2PService.getInstance(applicationContext)
            p2pService!!.registerService(groupName, object : ServiceRegisteredListener {

                override fun onSuccessServiceRegistered() {
                    Log.i(TAG, "Group created. Launching GroupChatActivity...")
                    startGroupChatActivity(groupName, true)
                    groupCreationDialog!!.dismiss()
                }

                override fun onErrorServiceRegistered(wiFiP2PError: WiFiP2PError) {
                    Toast.makeText(applicationContext, "Error creating group", Toast.LENGTH_SHORT).show()
                }

            })
        } else {
            Toast.makeText(applicationContext, "Please, insert a group name", Toast.LENGTH_SHORT).show()
        }
    }

    private val TAG = MainActivity::class.java.simpleName

    private var wiFiDirectBroadcastReceiver: WiFiDirectBroadcastReceiver? = null
    private var p2pService: P2PService? = null
    private var p2pClient: P2PClient? = null

    private var groupCreationDialog: GroupCreationDialog? = null


    private fun startGroupChatActivity(groupName: String, isGroupOwner: Boolean) {
        val intent = Intent(applicationContext, GroupChatActivity::class.java)
        intent.putExtra(GroupChatActivity().EXTRA_GROUP_NAME, groupName)
        intent.putExtra(GroupChatActivity().EXTRA_IS_GROUP_OWNER, isGroupOwner)
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        wiFiDirectBroadcastReceiver = WiFiP2PInstance.getInstance(this).broadcastReceiver
        btnCreateGroup.setOnClickListener({
            groupCreationDialog = GroupCreationDialog()
            groupCreationDialog!!.addGroupCreationAcceptListener(this@MainActivity)
            groupCreationDialog!!.show(supportFragmentManager, GroupCreationDialog::class.java.simpleName)
        })
        btnJoinGroup.setOnClickListener({
            searchAvailableGroups()
        })

    }

    override fun onResume() {
        super.onResume()

        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        registerReceiver(wiFiDirectBroadcastReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(wiFiDirectBroadcastReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (p2pService != null) {
            p2pService!!.disconnect()
        }

        if (p2pClient != null) {
            p2pClient!!.disconnect()
        }
    }

    private fun searchAvailableGroups() {
        val progressDialog = ProgressDialog(this@MainActivity)
        progressDialog.isIndeterminate = true
        progressDialog.setMessage(getString(R.string.prgrss_searching_groups))
        progressDialog.show()

        p2pClient = P2PClient.getInstance(applicationContext)
        p2pClient!!.discoverServices(5000L, object : ServiceDiscoveredListener {

            override fun onNewServiceDeviceDiscovered(p2PDeviceService: P2PDeviceService) {
                Log.i(TAG, "New group found:")
                Log.i(TAG, "\tName: " + p2PDeviceService.txtRecordMap!!.get(P2PService.SERVICE_GROUP_NAME))
            }

            override fun onFinishServiceDeviceDiscovered(p2PDeviceServices: List<P2PDeviceService>) {
                Log.i(TAG, "Found '" + p2PDeviceServices.size + "' groups")
                progressDialog.dismiss()

                if (p2PDeviceServices.isEmpty()) {
                    Toast.makeText(applicationContext, getString(R.string.toast_not_found_groups), Toast.LENGTH_LONG).show()
                } else {
                    showPickGroupDialog(p2PDeviceServices)
                }
            }

            override fun onError(wiFiP2PError: WiFiP2PError) {
                Toast.makeText(applicationContext, "Error searching groups: " + wiFiP2PError, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun showPickGroupDialog(devices: List<P2PDeviceService>) {
        val deviceNames = ArrayList<String>()
        for (device in devices) {
            deviceNames.add(device.txtRecordMap!!.get(P2PService.SERVICE_GROUP_NAME).toString())
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select a group")
        builder.setItems(deviceNames.toTypedArray()) { dialog, which ->
            val serviceSelected = devices[which]
            val progressDialog = ProgressDialog(this@MainActivity)
            progressDialog.setMessage(getString(R.string.prgrss_connecting_to_group))
            progressDialog.isIndeterminate = true
            progressDialog.show()

            p2pClient!!.connectToService(serviceSelected, object : ServiceConnectedListener {
                override fun onServiceConnected(p2PDevice: P2PDevice) {
                    progressDialog.dismiss()
                    startGroupChatActivity(serviceSelected.txtRecordMap!!.get(P2PService.SERVICE_GROUP_NAME).toString(), false)
                }
            })
        }

        val pickGroupDialog = builder.create()
        pickGroupDialog.show()
    }
}
