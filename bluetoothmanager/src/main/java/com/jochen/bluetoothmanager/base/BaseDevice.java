package com.jochen.bluetoothmanager.base;

import android.bluetooth.BluetoothDevice;

/**
 * 文件名：BaseDevice
 * 描述：设备Model基类
 * 创建人：jochen.zhang
 * 创建时间：2019/7/30
 */
public class BaseDevice {
    boolean isBLE = false;
    public BluetoothDevice device;
    public int rssi;

    public BaseDevice(boolean isBLE, BluetoothDevice device) {
        this.isBLE = isBLE;
        this.device = device;
    }
}
