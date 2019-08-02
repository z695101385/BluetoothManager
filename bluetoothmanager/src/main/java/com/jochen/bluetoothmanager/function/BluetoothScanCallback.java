package com.jochen.bluetoothmanager.function;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.jochen.bluetoothmanager.base.BaseDevice;
import com.jochen.bluetoothmanager.ble.BLEDevice;
import com.jochen.bluetoothmanager.spp.SPPDevice;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件名：BluetoothScanCallback
 * 描述：蓝牙搜索回调
 * 创建人：jochen.zhang
 * 创建时间：2019/4/11.
 */
public abstract class BluetoothScanCallback extends BroadcastReceiver implements BluetoothAdapter.LeScanCallback {
    private static final String TAG = "BluetoothScanCallback";
    private Map<BluetoothDevice, BaseDevice> devices = new HashMap<>();
    //毫秒，若搜索时长小于等于0，则无超时
    private int scanTimeOut;
    private boolean isScanning = true;

    public BluetoothScanCallback(int scanTimeOut) {
        this.scanTimeOut = scanTimeOut;
    }

    public void reset() {
        isScanning = true;
        devices.clear();
    }

    public void stop() {
        isScanning = false;
        devices.clear();
    }

    public int getScanTimeOut() {
        return scanTimeOut;
    }

    public abstract void onScanDevice(BaseDevice device);

    protected void onRefreshDevice(BaseDevice device) {
    }

    public abstract void onScanTimeout();

    public abstract void onScanCancel();

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (!isScanning) {
            return;
        }
        if (devices.containsKey(device)) {
            BLEDevice bleDevice = (BLEDevice) devices.get(device);
            if (bleDevice != null) {
                bleDevice.rssi = rssi;
                bleDevice.scanRecord = scanRecord;
                onRefreshDevice(bleDevice);
            }
        } else {
            BLEDevice bleDevice = new BLEDevice(device);
            bleDevice.rssi = rssi;
            bleDevice.scanRecord = scanRecord;
            devices.put(device, bleDevice);
            onScanDevice(bleDevice);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction()) && isScanning) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null) {
                if (devices.containsKey(device)) {
                    SPPDevice sppDevice = (SPPDevice) devices.get(device);
                    if (sppDevice != null) {
                        Bundle extras = intent.getExtras();
                        if (extras != null) {
                            sppDevice.extras = extras;
                            sppDevice.rssi = extras.getShort(BluetoothDevice.EXTRA_RSSI);
                        }
                        onRefreshDevice(sppDevice);
                    }
                } else {
                    SPPDevice sppDevice = new SPPDevice(device);
                    Bundle extras = intent.getExtras();
                    if (extras != null) {
                        sppDevice.extras = extras;
                        sppDevice.rssi = extras.getShort(BluetoothDevice.EXTRA_RSSI);
                    }
                    devices.put(device, sppDevice);
                    onScanDevice(sppDevice);
                }
            }
        }
    }
}
