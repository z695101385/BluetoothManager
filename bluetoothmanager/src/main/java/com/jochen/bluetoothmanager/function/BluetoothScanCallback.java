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
import com.jochen.bluetoothmanager.utils.LogUtils;

/**
 * 文件名：BluetoothScanCallback
 * 描述：蓝牙搜索回调
 * 创建人：jochen.zhang
 * 创建时间：2019/4/11.
 */
public abstract class BluetoothScanCallback extends BroadcastReceiver implements BluetoothAdapter.LeScanCallback {
    private static final String TAG = "BluetoothScanCallback";
    //毫秒，若搜索时长小于等于0，则无超时
    private int scanTimeOut;

    public BluetoothScanCallback(int scanTimeOut) {
        this.scanTimeOut = scanTimeOut;
    }

    public int getScanTimeOut() {
        return scanTimeOut;
    }

    public abstract void onScanDevice(BaseDevice device);

    public abstract void onScanTimeout();

    public abstract void onScanCancel();

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        BLEDevice bleDevice = new BLEDevice(true, device);
        bleDevice.rssi = rssi;
        bleDevice.scanRecord = scanRecord;
        onScanDevice(bleDevice);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null) {
                SPPDevice sppDevice = new SPPDevice(false, device);
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    sppDevice.extras = extras;
                    sppDevice.rssi = extras.getShort(BluetoothDevice.EXTRA_RSSI);
                }
                onScanDevice(sppDevice);
            }
        }
    }
}
