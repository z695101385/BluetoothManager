package com.jochen.bluetoothmanager.base;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.jochen.bluetoothmanager.ble.BLEDevice;
import com.jochen.bluetoothmanager.event.Event;
import com.jochen.bluetoothmanager.event.EventCode;
import com.jochen.bluetoothmanager.function.BluetoothScanCallback;
import com.jochen.bluetoothmanager.function.ConnectState;
import com.jochen.bluetoothmanager.spp.SPPDevice;
import com.jochen.bluetoothmanager.utils.BluetoothUtils;
import com.jochen.bluetoothmanager.utils.LogUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

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

    public HashMap<String, BaseDevice> connectedDevices = new HashMap<>();
    private BluetoothScanCallback bluetoothScanCallback = null;
    private Timer mScanTimer = new Timer();
    private TimerTask mScanTimerTask;

    public void init(Context context) {
        deInit();
        mContext = context.getApplicationContext();
        EventBus.getDefault().register(this);
    }

    public void deInit() {
        if (mContext != null) {
            mContext = null;
        }
        EventBus.getDefault().unregister(this);
    }

    public Context getContext() {
        return mContext;
    }

    /**
     * 根据mac地址获取device的model
     *
     * @param address
     * @return
     */
    public BaseDevice getDevice(String address) {
        BaseDevice baseDevice = connectedDevices.get(address);
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

    protected String TAG() {
        return isBLE ? "BLEManager" : "SPPManager";
    }

    /**
     * 监听EventBus消息
     *
     * @param event 接收到的event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Event event) {
        switch (event.getCode()) {
            case EventCode.ConnectionStateChangedCode:
                BaseDevice device = (BaseDevice) event.getData();
                if (device.isBLE == isBLE) {
                    // 对应类型
                    switch (device.connectionState) {
                        case ConnectState.STATE_DISCONNECTED:
                            connectedDevices.remove(device.device.getAddress());
                            LogUtils.i("[" + TAG() + "] connectedDevices " + connectedDevices.size());
                            break;
                        case ConnectState.STATE_CONNECTING:
                            connectedDevices.put(device.device.getAddress(), device);
                            LogUtils.i("[" + TAG() + "] connectedDevices " + connectedDevices.size());
                            break;
                    }
                }
                break;
        }
    }
}
