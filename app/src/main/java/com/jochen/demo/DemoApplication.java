package com.jochen.demo;

import android.app.Application;

import com.jochen.bluetoothmanager.ble.BLEManager;
import com.jochen.bluetoothmanager.spp.SPPManager;
import com.jochen.bluetoothmanager.utils.BluetoothUtils;

public class DemoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        BluetoothUtils.init(this);
        SPPManager.getInstance().init(this);
        BLEManager.getInstance().init(this);
    }
}
