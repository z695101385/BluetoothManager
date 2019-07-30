package com.jochen.bluetoothmanager.spp;

import android.bluetooth.BluetoothDevice;
import android.content.IntentFilter;

import com.jochen.bluetoothmanager.base.BluetoothManager;
import com.jochen.bluetoothmanager.function.BluetoothScanCallback;
import com.jochen.bluetoothmanager.utils.BluetoothUtils;

public class SPPManager extends BluetoothManager {
    @Override
    protected boolean startScanFunction(BluetoothScanCallback callback) {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        mContext.registerReceiver(callback, filter);
        return BluetoothUtils.getBluetoothAdapter().startDiscovery();
    }

    @Override
    protected void stopScanFunction(BluetoothScanCallback callback) {
        mContext.unregisterReceiver(callback);
        BluetoothUtils.getBluetoothAdapter().cancelDiscovery();
    }

    /***********************************************************************************************
     * 单例化
     **********************************************************************************************/
    private SPPManager() {
        // 私有化构造函数
        isBLE = false;
    }

    public static SPPManager getInstance() {
        return Singleton.SINGLETON.getSingleTon();
    }

    public enum Singleton {
        SINGLETON; // 枚举本身序列化之后返回的实例
        private SPPManager singleton;

        Singleton() {
            // JVM保证只实例一次
            singleton = new SPPManager();
        }

        // 公布对外方法
        public SPPManager getSingleTon() {
            return singleton;
        }
    }
}
