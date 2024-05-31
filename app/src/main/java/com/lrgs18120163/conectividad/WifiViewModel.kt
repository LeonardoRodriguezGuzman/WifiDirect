package com.lrgs18120163.conectividad

import android.app.Application
import android.content.Context
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

data class WifiDeviceItem(val deviceName: String, val deviceAddress: String)

class WifiViewModel(application: Application) : AndroidViewModel(application) {
    private val context = getApplication<Application>().applicationContext
    private val wifiManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val channel = wifiManager.initialize(context, context.mainLooper, null)

    private val _discoveredDevices = MutableStateFlow<List<WifiP2pDevice>>(emptyList()) // Cambiar el tipo
    val discoveredDevices: StateFlow<List<WifiP2pDevice>> = _discoveredDevices.asStateFlow()

    private val _selectedDevice = MutableStateFlow<WifiP2pDevice?>(null)
    val selectedDevice: StateFlow<WifiP2pDevice?> = _selectedDevice.asStateFlow()
    private val _selectedDeviceAddress = MutableStateFlow<String?>(null)
    val selectedDeviceAddress: StateFlow<String?> = _selectedDeviceAddress.asStateFlow()

    fun selectDevice(deviceAddress: String) { // Cambiar a recibir la dirección del dispositivo
        _selectedDevice.value = _discoveredDevices.value.find { it.deviceAddress == deviceAddress }
        _selectedDeviceAddress.value = deviceAddress
    }
    private val _receivedInformation = MutableStateFlow("")
    val receivedInformation: StateFlow<String> = _receivedInformation.asStateFlow()

    private var isDiscovering = true

    private val peerListListener = WifiP2pManager.PeerListListener { peerList ->
        _discoveredDevices.value = peerList.deviceList.toList()
    }

    private val actionListener = object : WifiP2pManager.ActionListener {
        override fun onSuccess() {
            isDiscovering = true
            wifiManager.requestPeers(channel, peerListListener)
        }

        override fun onFailure(reasonCode: Int) {
            isDiscovering = false
            // Manejar el fallo (puedes mostrar un mensaje de error, por ejemplo)
        }
    }

    fun startDiscovery() {
        if (!isDiscovering) {
            isDiscovering = true
            wifiManager.discoverPeers(channel, actionListener)
        }
    }

    fun stopDiscovery() {
        if (isDiscovering) {
            isDiscovering = false
            wifiManager.stopPeerDiscovery(channel, null)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopDiscovery()
    }

    fun connectToDevice(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }

        wifiManager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // Conexión exitosa
            }

            override fun onFailure(reason: Int) {
                // Manejar el fallo de la conexión
            }
        })
    }

    fun sendMessage(message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                selectedDevice.value?.let { device ->
                    wifiManager.requestConnectionInfo(channel) { connectionInfo ->
                        if (connectionInfo?.groupOwnerAddress != null) {
                            val socket = Socket()
                            socket.bind(null)
                            socket.connect(InetSocketAddress(connectionInfo.groupOwnerAddress, 8888), 5000)
                            val outputStream = socket.getOutputStream()
                            outputStream.write(message.toByteArray(), 0, message.toByteArray().size)
                            outputStream.close()
                            socket.close()
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e("WifiViewModel", "Error al enviar mensaje: ${e.message}")
                // Manejar el error (puedes mostrar un mensaje de error, por ejemplo)
            }
        }
    }

    private var serverSocket: ServerSocket? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(8888)
                while (true) {
                    val client = serverSocket?.accept()
                    handleClient(client)
                }
            } catch (e: IOException) {
                Log.e("WifiViewModel", "Error en el servidor: ${e.message}")
            } finally {
                serverSocket?.close()
            }
        }
    }

    private fun handleClient(socket: Socket?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = socket?.getInputStream()
                val bytes = ByteArray(1024)
                var read: Int
                val stringBuilder = StringBuilder()
                while (inputStream?.read(bytes).also { read = it ?: -1 } != -1) {
                    stringBuilder.append(String(bytes, 0, read))
                }
                val receivedInfo = stringBuilder.toString()
                withContext(Dispatchers.Main) {
                    _receivedInformation.value = receivedInfo
                }
            } catch (e: IOException) {
                Log.e("WifiViewModel", "Error al recibir información: ${e.message}")
            } finally {
                socket?.close()
            }
        }
    }
}
