package com.jochen.demo;

import android.app.Application;

import com.jochen.bluetoothmanager.ble.BLEManager;
import com.jochen.bluetoothmanager.spp.SPPManager;
import com.jochen.bluetoothmanager.utils.BluetoothUtils;

/**
 * 文件名：DemoApplication
 * 描述：初始化BluetoothManager
 * 创建人：jochen.zhang
 * 创建时间：2019/8/1
 */
public class DemoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        BluetoothUtils.init(this);
        SPPManager.getInstance().init(this);
        BLEManager.getInstance().init(this);
    }
}
