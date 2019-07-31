package com.jochen.bluetoothmanager.spp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;

import com.jochen.bluetoothmanager.base.BaseDevice;
import com.jochen.bluetoothmanager.function.ConnectState;
import com.jochen.bluetoothmanager.utils.BluetoothUtils;
import com.jochen.bluetoothmanager.utils.ConfigUtils;
import com.jochen.bluetoothmanager.utils.LogUtils;
import com.jochen.bluetoothmanager.utils.ProtocolUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SPPDevice extends BaseDevice {
    public Bundle extras;

    public SPPDevice(BluetoothDevice device) {
        super(false, device);
    }

    @Override
    public boolean connect() {
        if (connectionState == ConnectState.STATE_DISCONNECTED) {
            // Start the thread to connect with the given device
            mConnectThread = new ConnectThread(device);
            mConnectThread.start();
            return true;
        }
        return false;
    }

    @Override
    public void disconnect() {
        LogUtils.d("[" + device.getName() + "] disconnect");
        clearConnection();
    }

    @Override
    public boolean write(byte[] data) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (connectionState < ConnectState.STATE_CONNECTED) return false;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(data);
        return true;
    }

    @Override
    public String toString() {
        return "[" + device.getName() + "] mac: " + device.getAddress() + " BondState: " + device.getBondState();
    }

    /***********************************************************************************************
     * SPP 连接实现
     **********************************************************************************************/
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device, final String socketType) {
        LogUtils.i("[" + device.getName() + "] connected, Socket Type:" + socketType);
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();
    }

    /**
     * 清空连接
     */
    private synchronized void clearConnection() {
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setConnectState(ConnectState.STATE_DISCONNECTED);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private synchronized void connectionFailed() {
        LogUtils.d("[" + device.getName() + "] connectionFailed");
        clearConnection();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private synchronized void connectionLost() {
        LogUtils.d("[" + device.getName() + "] connectionLost");
        clearConnection();
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = "Secure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(ConfigUtils.getRfcommUUID());
            } catch (IOException e) {
                LogUtils.e("[" + device.getName() + "] Socket Type: " + mSocketType + " create() failed", e);
            }
            mmSocket = tmp;
            setConnectState(ConnectState.STATE_CONNECTING);
        }

        public void run() {
            LogUtils.i("[" + device.getName() + "] BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            BluetoothUtils.getBluetoothAdapter().cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (Exception e) {
                // Close the socket
                LogUtils.e("[" + device.getName() + "] unable to connect() " + mSocketType + " socket during connection failure", e);
                try {
                    if (mmSocket != null) {
                        mmSocket.close();
                    }
                } catch (IOException e2) {
                    LogUtils.e("[" + device.getName() + "] unable to close() " + mSocketType + " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (SPPDevice.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        void cancel() {
            try {
                if (mmSocket != null) {
                    mmSocket.close();
                }
            } catch (IOException e) {
                LogUtils.e("[" + device.getName() + "] close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket socket, String socketType) {
            LogUtils.d("[" + device.getName() + "] create ConnectedThread: " + socketType);
            setConnectState(ConnectState.STATE_CONNECTED);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                LogUtils.e("[" + device.getName() + "] temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            setConnectState(ConnectState.STATE_DATA_READY);
        }

        public synchronized void run() {
            LogUtils.i("[" + device.getName() + "] BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int len;

            // Keep listening to the InputStream while connected
            while (connectionState >= ConnectState.STATE_DATA_READY) {
                try {
                    // Read from the InputStream
                    len = mmInStream.read(buffer);

                    if (len > 0) {
                        byte[] receiveBytes = new byte[len];
                        System.arraycopy(buffer, 0, receiveBytes, 0, len);

                        //log receive data
                        LogUtils.d("[" + device.getName() + "] 接收 长度: " + len + " 数据: " + ProtocolUtils.bytesToHexStr(receiveBytes));
                        receive(receiveBytes);
                    }
                } catch (IOException e) {
                    LogUtils.e("[" + device.getName() + "] disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        void write(byte[] buffer) {
            try {
                LogUtils.d("[" + device.getName() + "] 发送 长度: " + buffer.length + " 数据: " + ProtocolUtils.bytesToHexStr(buffer));
                mmOutStream.write(buffer);
            } catch (IOException e) {
                LogUtils.e("[" + device.getName() + "] Exception during write");
            }
        }

        void cancel() {
            try {
                if (mmSocket != null) {
                    mmSocket.close();
                }
            } catch (IOException e) {
                LogUtils.e("[" + device.getName() + "] close() of connect socket failed", e);
            }
        }
    }
}
