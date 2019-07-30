package com.jochen.bluetoothmanager.ble;

import android.bluetooth.BluetoothDevice;

import com.jochen.bluetoothmanager.base.BaseDevice;
import com.jochen.bluetoothmanager.utils.ProtocolUtils;

public class BLEDevice extends BaseDevice {
    public byte[] scanRecord;

    public BLEDevice(boolean isBLE, BluetoothDevice device) {
        super(isBLE, device);
    }

    @Override
    public String toString() {
        return "BLEDevice Name: " + device.getName() + " mac: " + device.getAddress() + " scanRecord: " + ProtocolUtils.bytesToHexStr(scanRecord);
    }
}
