package com.jochen.bluetoothmanager.base;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.jochen.bluetoothmanager.function.BluetoothScanCallback;
import com.jochen.bluetoothmanager.utils.LogUtils;

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

    protected BluetoothScanCallback bluetoothScanCallback = null;
    private Timer mScanTimer = new Timer();
    private TimerTask mScanTimerTask;

    public void init(Context context) {
        mContext = context.getApplicationContext();
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
            LogUtils.d(TAG(), "搜索开始");
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
                                LogUtils.d(TAG(), "搜索超时");
                            }
                        }
                    };
                    mScanTimer.schedule(mScanTimerTask, timeout);
                }
            } else {
                // 开启失败
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
            LogUtils.d(TAG(), "搜索取消");
        }
    }

    protected String TAG() {
        return isBLE ? "BLEManager" : "SPPManager";
    }
}
