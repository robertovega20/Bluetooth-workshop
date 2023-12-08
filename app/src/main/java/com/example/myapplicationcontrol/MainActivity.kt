package com.example.myapplicationcontrol

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.myapplicationcontrol.ui.theme.MyApplicationcontrolTheme
import java.util.UUID
import android.Manifest
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight.Companion.Bold

class MainActivity : ComponentActivity() {

    private var pairedDevices: Set<BluetoothDevice>? = null
    private var bluetoothService: MyBluetoothService? = null

    private val BLUETOOTH_CONNECT_PERMISSION_REQUEST_CODE = 1

    private var messages: MutableList<String> = mutableStateListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions()

        BluetoothAdapter.getDefaultAdapter()?.apply {
            pairedDevices = this.bondedDevices
            bluetoothService = MyBluetoothService(handler, this)
        }

        setContent {
            MyApplicationcontrolTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .weight(.5f)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(.5f)
                                .background(color = Color.Cyan.copy(alpha = .1f))
                                .fillMaxHeight()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Paired devices", fontWeight = Bold)
                            pairedDevices?.forEach { device ->
                                Text(
                                    text = device.name,
                                    modifier = Modifier.clickable { bluetoothService?.connect(device) })
                            }
                        }
                        Column(
                            modifier = Modifier
                                .weight(.5f)
                                .verticalScroll(rememberScrollState())
                                .background(color = Color.Gray),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            messages.forEach { message ->
                                Text(text = message)
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.weight(.5f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(color = Color.Green),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CustomImage(
                                modifier = Modifier,
                                message = "UP",
                                degrees = 90f,
                                onClick = { writeMessage(it) })
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(color = Color.Red)
                                .padding(40.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CustomImage(
                                modifier = Modifier,
                                message = "Left",
                                degrees = 0f,
                                onClick = { writeMessage(it) })
                            CustomImage(
                                modifier = Modifier,
                                message = "Right",
                                degrees = 180f,
                                onClick = { writeMessage(it) })
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(color = Color.Cyan),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CustomImage(
                                modifier = Modifier,
                                message = "down",
                                degrees = -90f,
                                onClick = { writeMessage(it) })
                        }
                    }
                }
            }
        }
    }

    fun writeMessage(msg: String) {
        bluetoothService?.write(msg)
    }

    fun MutableList<String>.addMessage(message: String) {
        this.add(message)
    }

    @Composable
    fun CustomImage(
        modifier: Modifier,
        message: String,
        degrees: Float,
        onClick: (String) -> Unit
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_left),
            contentDescription = "",
            modifier = modifier
                .size(100.dp)
                .rotate(degrees)
                .clickable { onClick(message) }
        )
    }

    private fun requestPermissions() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // If permission is not granted, request it
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT),
                BLUETOOTH_CONNECT_PERMISSION_REQUEST_CODE
            )
        }
    }

    private val handler: Handler = @SuppressLint("HandlerLeak") object : Handler() {

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_WRITE -> {
                    val writeBuf = msg.obj as ByteArray
                    val writeMessage = String(writeBuf)
                    messages.add(writeMessage)
                }

                MESSAGE_READ -> {
                    val readBuf = msg.obj as ByteArray
                    val readMessage = String(readBuf, 0, msg.arg1)
                    messages.add(readMessage)
                }
            }
        }
    }

    companion object {
        val MY_UUID_SECURE_ESP_32: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        val MY_UUID_SECURE: UUID = UUID.fromString("fa87c0d0-afac-11de-8a36-0800200c9a66")
        const val MESSAGE_READ = 2
        const val MESSAGE_WRITE = 3
    }
}
