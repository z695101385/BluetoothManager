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

/**
 * 文件名：SPPDevice
 * 描述：封装了SPP链路的连接、通信方法
 * 构造只需要传入系统的BluetoothDevice模型
 * SPPDevice sppDevice = new SPPDevice(device);
 * 创建人：jochen.zhang
 * 创建时间：2019/8/2
 */
public class SPPDevice extends BaseDevice {
    // 搜索到的设备额外信息
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
        ConnectedThread r;
        //同步获取通信线程
        synchronized (this) {
            if (connectionState < ConnectState.STATE_CONNECTED) return false;
            r = mConnectedThread;
        }
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
     * 连接成功后开启通信线程
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        LogUtils.i("[" + device.getName() + "] connected");
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
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
    }

    /**
     * 清空连接相关线程，设置状态
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
     * 连接失败
     */
    private synchronized void connectionFailed() {
        LogUtils.d("[" + device.getName() + "] connectionFailed");
        clearConnection();
    }

    /**
     * 连接丢失
     */
    private synchronized void connectionLost() {
        LogUtils.d("[" + device.getName() + "] connectionLost");
        clearConnection();
    }


    /**
     * 连接线程
     * 尝试与设备建立SPP连接
     */
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            try {
                tmp = device.createRfcommSocketToServiceRecord(ConfigUtils.getRfcommUUID(device));
            } catch (IOException e) {
                LogUtils.e("[" + device.getName() + "] rfcomm create() failed", e);
            }
            mmSocket = tmp;
            setConnectState(ConnectState.STATE_CONNECTING);
        }

        public void run() {
            LogUtils.i("[" + device.getName() + "] BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            BluetoothUtils.getBluetoothAdapter().cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (Exception e) {
                // Close the socket
                LogUtils.e("[" + device.getName() + "] unable to connect() socket during connection failure", e);
                try {
                    if (mmSocket != null) {
                        mmSocket.close();
                    }
                } catch (IOException e2) {
                    LogUtils.e("[" + device.getName() + "] unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (SPPDevice.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
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

    /**
     * 通信线程
     * 在SPP连接成功后，开启通信线程
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket socket) {
            LogUtils.d("[" + device.getName() + "] create ConnectedThread");
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
         * 发送数据
         *
         * @param buffer 待发送数据
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
