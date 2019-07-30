package com.jochen.bluetoothmanager.ble;

import com.jochen.bluetoothmanager.base.BluetoothManager;
import com.jochen.bluetoothmanager.function.BluetoothScanCallback;
import com.jochen.bluetoothmanager.utils.BluetoothUtils;

public class BLEManager extends BluetoothManager {
    @Override
    protected boolean startScanFunction(BluetoothScanCallback callback) {
        return BluetoothUtils.getBluetoothAdapter().startLeScan(callback);
    }

    @Override
    protected void stopScanFunction(BluetoothScanCallback callback) {
        BluetoothUtils.getBluetoothAdapter().stopLeScan(callback);
    }

    /***********************************************************************************************
     * 单例化
     **********************************************************************************************/
    private BLEManager() {
        // 私有化构造函数
        isBLE = true;
    }

    public static BLEManager getInstance() {
        return Singleton.SINGLETON.getSingleTon();
    }

    public enum Singleton {
        SINGLETON; // 枚举本身序列化之后返回的实例
        private BLEManager singleton;

        Singleton() {
            // JVM保证只实例一次
            singleton = new BLEManager();
        }

        // 公布对外方法
        public BLEManager getSingleTon() {
            return singleton;
        }
    }
}
