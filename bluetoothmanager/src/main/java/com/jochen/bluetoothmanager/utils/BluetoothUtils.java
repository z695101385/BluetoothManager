package com.jochen.bluetoothmanager.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import java.lang.reflect.Method;
/**
 * 文件名：BluetoothUtils
 * 描述：蓝牙工具类
 * 创建人：jochen.zhang
 * 创建时间：2019/7/30
 */
public class BluetoothUtils {
    /**
     * 获取BluetoothAdapter
     */
    private static BluetoothAdapter mBtAdapter;
    public static BluetoothAdapter getBluetoothAdapter() {
        if (mBtAdapter == null) {
            mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        return mBtAdapter;
    }

    /**
     * 蓝牙状态
     *
     * @return true 开启; false 关闭
     */
    public static boolean isEnable() {
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
        if (bluetoothAdapter != null) {
            return bluetoothAdapter.isEnabled();
        } else {
            return false;
        }
    }

    /**
     * 蓝牙开启/关闭
     *
     * @param enable true 开启; false 关闭
     * @return 操作结果
     */
    public static boolean setEnable(boolean enable) {
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
        if (bluetoothAdapter != null) {
            return enable ? bluetoothAdapter.enable() : bluetoothAdapter.disable();
        } else {
            return false;
        }
    }

    /**
     * 绑定设备
     *
     * @param device 待绑定设备
     * @return Bool
     */
    public static boolean createBond(BluetoothDevice device){
        boolean result = false;
        try{
            Method m = device.getClass().getDeclaredMethod("createBond");
            m.setAccessible(true);
            result = (Boolean) m.invoke(device);
        }catch(Exception ignored){
        }
        return result;
    }
}
