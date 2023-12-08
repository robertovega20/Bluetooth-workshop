package com.example.myapplicationcontrol

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

private const val TAG = "MY_APP_DEBUG_TAG"

class MyBluetoothService(
    private val handler: Handler, val bluetoothAdapter: BluetoothAdapter?
) {

    private var connectThread: ConnectThread? = null
    private var communicationThread: CommunicationThread? = null

    fun connect(device: BluetoothDevice) {
        connectThread = ConnectThread(device)
        connectThread?.start()
    }

    fun onConnected(socket: BluetoothSocket?) {
        connectThread?.cancel()
        connectThread = null

        communicationThread = CommunicationThread(socket)
        communicationThread?.start()
    }

    fun write(message: String) {
        communicationThread?.write(message)
    }

    private inner class ConnectThread(device: BluetoothDevice) : Thread() {

        private val mmSocket: BluetoothSocket?

        init {
            mmSocket = device.createRfcommSocketToServiceRecord(MainActivity.MY_UUID_SECURE)
        }

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter?.cancelDiscovery()
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mmSocket?.connect()

            synchronized(this@MyBluetoothService) { connectThread = null }

            onConnected(mmSocket)
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e("TAG", "Could not close the client socket", e)
            }
        }
    }

    private inner class CommunicationThread(socket: BluetoothSocket?) : Thread() {

        private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream
        private val mmSocket: BluetoothSocket? = socket
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?

        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            try {
                tmpIn = socket?.inputStream
                tmpOut = socket?.outputStream
            } catch (e: IOException) {
                Log.e(TAG, "temp sockets not created", e)
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut
        }

        override fun run() {
            var numBytes: Int // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                // Read from the InputStream.
                numBytes = try {
                    mmInStream?.read(mmBuffer) ?: 0
                } catch (e: IOException) {
                    Log.d(TAG, "Input stream was disconnected", e)
                    break
                }

                // Send the obtained bytes to the UI activity.
                val readMsg = handler.obtainMessage(
                    MainActivity.MESSAGE_READ, numBytes, -1,
                    mmBuffer
                )
                readMsg.sendToTarget()
            }
        }

        // Call this from the main activity to send data to the remote device.
        fun write(bytes: String) {
            val buffer = bytes.toByteArray()
            try {
                mmOutStream?.write(buffer)
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when sending data", e)
                return
            }

            // Share the sent message with the UI activity.
            val writtenMsg = handler.obtainMessage(
                MainActivity.MESSAGE_WRITE, -1, -1, buffer
            )
            writtenMsg.sendToTarget()
        }

        // Call this method from the main activity to shut down the connection.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }
}