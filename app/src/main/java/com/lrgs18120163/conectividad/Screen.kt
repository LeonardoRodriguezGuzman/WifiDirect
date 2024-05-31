package com.lrgs18120163.conectividad

import android.net.wifi.p2p.WifiP2pDevice
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiDirectScreen(viewModel: WifiViewModel) {
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val receivedInfo by viewModel.receivedInformation.collectAsState()
    var messageText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Wifi Direct") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Sección de dispositivos descubiertos
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                items(discoveredDevices) { device -> // Ahora 'device' es WifiP2pDevice
                    DeviceCard(device, isSelected = device.deviceAddress == viewModel.selectedDeviceAddress.collectAsState().value) {
                        viewModel.selectDevice(device.deviceAddress)
                        viewModel.connectToDevice(device)
                    }
                }
            }

            // Sección de mensajes recibidos
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text("Mensajes Recibidos:")
                Text(receivedInfo) // Mostrar el último mensaje recibido
            }

            // Sección de envío de mensajes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text("Mensaje") },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(messageText) // Enviar mensaje
                            messageText = ""
                        }
                    },
                    enabled = messageText.isNotBlank() && viewModel.selectedDevice.collectAsState().value != null
                ) {
                    Text("Enviar")
                }
            }
        }
    }
}

@Composable
fun DeviceCard(device: WifiP2pDevice, isSelected: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onSelect() },
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(text = "Nombre: ${device.deviceName}", fontWeight = FontWeight.Bold)
            Text(text = "Dirección: ${device.deviceAddress}")
        }
    }
}
