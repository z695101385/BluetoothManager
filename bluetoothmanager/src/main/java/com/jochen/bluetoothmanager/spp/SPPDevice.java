package com.jochen.bluetoothmanager.spp;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;

import com.jochen.bluetoothmanager.base.BaseDevice;

public class SPPDevice extends BaseDevice {
    public Bundle extras;
    public SPPDevice(boolean isBLE, BluetoothDevice device) {
        super(isBLE, device);
    }

    @Override
    public String toString() {
        return "SPPDevice Name: " + device.getName() + " mac: " + device.getAddress();
    }
}
