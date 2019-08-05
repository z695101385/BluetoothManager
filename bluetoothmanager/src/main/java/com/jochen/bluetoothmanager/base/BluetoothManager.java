package com.jochen.bluetoothmanager.base;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.jochen.bluetoothmanager.ble.BLEDevice;
import com.jochen.bluetoothmanager.function.BluetoothScanCallback;
import com.jochen.bluetoothmanager.spp.SPPDevice;
import com.jochen.bluetoothmanager.utils.BluetoothUtils;
import com.jochen.bluetoothmanager.utils.LogUtils;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 文件名：BluetoothManager
 * 描述：蓝牙管理基类
 * 创建人：jochen.zhang
 * 创建时间：2019/7/26
 */
public abstract class BluetoothManager {
    protected boolean isBLE = false;
    protected Context mContext;
    // ConnectState不等于DISCONNECT的设备会在数组中
    private final HashMap<String, BaseDevice> connectedDevices = new HashMap<>();
    private BluetoothScanCallback bluetoothScanCallback = null;
    private Timer mScanTimer = new Timer();
    private TimerTask mScanTimerTask;

    public void init(Context context) {
        deInit();
        mContext = context.getApplicationContext();
    }

    public void deInit() {
        if (mContext != null) {
            mContext = null;
        }
    }

    public Context getContext() {
        return mContext;
    }

    public HashMap<String, BaseDevice> getConnectedDevices() {
        synchronized (connectedDevices) {
            return connectedDevices;
        }
    }

    public void putConnectedDevice(BaseDevice device) {
        synchronized (connectedDevices) {
            connectedDevices.put(device.device.getAddress(), device);
        }
    }

    public void removeConnectedDevice(BaseDevice device) {
        synchronized (connectedDevices) {
            connectedDevices.remove(device.device.getAddress());
        }
    }

    public BaseDevice getConnectedDevice(String address) {
        synchronized (connectedDevices) {
            return connectedDevices.get(address);
        }
    }

    /**
     * 根据mac地址获取device的model
     * 若为已连接设备则返回连接设备Model，否则根据BluetoothDevice创建BLEDevice或SPPDevice
     * @param address 设备mac地址
     * @return BLEDevice or SPPDevice
     */
    public BaseDevice getDevice(String address) {
        BaseDevice baseDevice = getConnectedDevice(address);
        if (baseDevice != null) {
            return baseDevice;
        }
        BluetoothDevice device = BluetoothUtils.getBluetoothAdapter().getRemoteDevice(address);
        if (device == null) {
            return null;
        }
        if (isBLE) {
            baseDevice = new BLEDevice(device);
        } else {
            baseDevice = new SPPDevice(device);
        }
        return baseDevice;
    }

    /**
     * 开启搜索的函数
     */
    protected abstract boolean startScanFunction(BluetoothScanCallback callback);

    /**
     * 关闭搜索的函数
     */
    protected abstract void stopScanFunction(BluetoothScanCallback callback);

    /**
     * 当前是否正在搜索
     * @return bool
     */
    public boolean isScanning() {
        return bluetoothScanCallback != null;
    }

    /**
     * 开始搜索
     *
     * @param callback 扫描回调
     * @return 开启结果
     */
    public boolean startScan(BluetoothScanCallback callback) {
        if (null == bluetoothScanCallback) {
            bluetoothScanCallback = callback;
            LogUtils.d("[" + TAG() + "] 搜索开始");
            callback.reset();
            boolean result = startScanFunction(callback);
            if (result) {
                //开启成功，开启超时定时器
                int timeout = bluetoothScanCallback.getScanTimeOut();
                if (timeout > 0) {
                    mScanTimerTask = new TimerTask() {
                        @Override
                        public void run() {
                            if (null != bluetoothScanCallback) {
                                bluetoothScanCallback.onScanTimeout();
                                stopScan();
                                LogUtils.d("[" + TAG() + "] 搜索超时");
                            }
                        }
                    };
                    mScanTimer.schedule(mScanTimerTask, timeout);
                }
            } else {
                // 开启失败
                LogUtils.d("[" + TAG() + "] 开启搜索失败");
                bluetoothScanCallback.onScanCancel();
                bluetoothScanCallback = null;
            }
            return result;
        } else {
            cancelScan();
            return startScan(callback);
        }
    }

    /**
     * 停止搜索
     */
    private void stopScan() {
        if (null != mScanTimerTask) {
            mScanTimerTask.cancel();
            mScanTimerTask = null;
        }
        if (null != bluetoothScanCallback) {
            bluetoothScanCallback.stop();
            stopScanFunction(bluetoothScanCallback);
            bluetoothScanCallback = null;
        }
    }

    /**
     * 取消搜索
     */
    public void cancelScan() {
        if (null != bluetoothScanCallback) {
            bluetoothScanCallback.onScanCancel();
            stopScan();
            LogUtils.d("[" + TAG() + "] 搜索取消");
        }
    }

    private String TAG() {
        return isBLE ? "BLEManager" : "SPPManager";
    }
}
