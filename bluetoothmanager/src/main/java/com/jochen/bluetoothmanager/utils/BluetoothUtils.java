package com.jochen.bluetoothmanager.utils;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    ///////////////////////////////////////////////////////////////////////////
    // Receiver
    ///////////////////////////////////////////////////////////////////////////
    private static Receiver mReceiver = new Receiver();
    private static class Receiver extends BroadcastReceiver {
        private boolean mRegistered;

        public void register(Context context, boolean register) {
            if (mRegistered == register) return;
            if (register) {
                final IntentFilter filter = new IntentFilter();
                filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                context.getApplicationContext().registerReceiver(this, filter);
            } else {
                context.getApplicationContext().unregisterReceiver(this);
            }
            mRegistered = register;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (null == action) {
                return;
            }
            switch (action) {
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    handleBondIntent(intent);
                    break;
            }
        }
    }

    /***********************************************************************************************
     * 绑定相关方法
     **********************************************************************************************/
    /**
     * 绑定设备回调
     */
    public interface BondCallback {
        void onBondState(int state);
    }

    // 绑定回调Map
    private static Map<BluetoothDevice, BondCallback> bondCallbackMap = new HashMap<>();

    /**
     * 获取已绑定设备
     *
     * @return 已绑定的蓝牙设备
     */
    public static Set<BluetoothDevice> getBondedDevices() {
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
        if (bluetoothAdapter != null) {
            return bluetoothAdapter.getBondedDevices();
        } else {
            return null;
        }
    }

    /**
     * 绑定设备
     *
     * @param device 待绑定设备
     * @return Bool
     */
    public static boolean createBond(Context context, BluetoothDevice device, BondCallback bondCallback) {
        boolean result = false;
        if (device.getBondState() == BluetoothDevice.BOND_NONE) {
            bondCallbackMap.put(device, bondCallback);
            mReceiver.register(context, true);
            //如果这个设备取消了配对，则尝试配对
            result = device.createBond();
            if (!result) {
                bondCallbackMap.remove(device);
            }
        }
        return result;
    }

    /**
     * 解绑设备
     *
     * @param device 待解绑设备
     * @return Bool
     */
    public static boolean removeBond(Context context, BluetoothDevice device, BondCallback bondCallback) {
        boolean result = false;
        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
            try {
                Method m = device.getClass().getDeclaredMethod("removeBond");
                m.setAccessible(true);
                mReceiver.register(context, true);
                bondCallbackMap.put(device, bondCallback);
                result = (Boolean) m.invoke(device);
                if (!result) {
                    bondCallbackMap.remove(device);
                }
            } catch(Exception ignored) {
                bondCallbackMap.remove(device);
            }
        }
        return result;
    }

    private static void handleBondIntent(Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        BondCallback bondCallback = bondCallbackMap.get(device);
        if (bondCallback != null) {
            switch (device.getBondState()) {
                case BluetoothDevice.BOND_NONE:
                    bondCallbackMap.remove(device);
                    LogUtils.i("[" + device.getName() + "] mac: " + device.getAddress() + " BOND_NONE");
                    break;
                case BluetoothDevice.BOND_BONDING:
                    LogUtils.i("[" + device.getName() + "] mac: " + device.getAddress() + " BOND_BONDING");
                    break;
                case BluetoothDevice.BOND_BONDED:
                    bondCallbackMap.remove(device);
                    LogUtils.i("[" + device.getName() + "] mac: " + device.getAddress() + " BOND_BONDED");
                    break;
            }
            bondCallback.onBondState(device.getBondState());
        }
    }

    /***********************************************************************************************
     * A2DP相关操作
     **********************************************************************************************/
    private static BluetoothA2dp mBluetoothA2dp;
    private static BluetoothHeadset mBluetoothHeadset;
    public static void init(Context context) {
        //获取A2DP代理对象
        getBluetoothAdapter().getProfileProxy(context, mListener, BluetoothProfile.A2DP);
        getBluetoothAdapter().getProfileProxy(context, mListener, BluetoothProfile.HEADSET);
    }

    private static BluetoothProfile.ServiceListener mListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.A2DP) {
                mBluetoothA2dp = null;
            } else if (profile == BluetoothProfile.HEADSET) {
                mBluetoothHeadset = null;
            }
        }
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if(profile == BluetoothProfile.A2DP){
                mBluetoothA2dp = (BluetoothA2dp) proxy; //转换
            } else if (profile == BluetoothProfile.HEADSET) {
                mBluetoothHeadset = (BluetoothHeadset) proxy;
            }
        }
    };

    /**
     * 设置优先级
     *
     * @param device    蓝牙设备
     * @param priority  优先级
     */
    private static void setA2DPPriority(Context context, BluetoothDevice device, int priority) {
        if (mBluetoothA2dp == null) {
            init(context);
            return;
        }
        try {//通过反射获取BluetoothA2dp中setPriority方法（hide的），设置优先级
            Method connectMethod = BluetoothA2dp.class.getMethod("setPriority", BluetoothDevice.class,int.class);
            connectMethod.invoke(mBluetoothA2dp, device, priority);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 建立A2DP连接
     *
     * @param device 待连接设备
     */
    public static void connectA2DP(Context context, BluetoothDevice device) {
        if (mBluetoothA2dp == null) {
            init(context);
            return;
        }
        setA2DPPriority(context, device, 100); //设置priority
        try {
            //通过反射获取BluetoothA2dp中connect方法（hide的），进行连接。
            Method connectMethod = BluetoothA2dp.class.getMethod("connect", BluetoothDevice.class);
            connectMethod.invoke(mBluetoothA2dp, device);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 断开A2DP连接
     *
     * @param device 待断开设备
     */
    public static void disconnectA2DP(Context context, BluetoothDevice device) {
        if (mBluetoothA2dp == null) {
            init(context);
            return;
        }
        setA2DPPriority(context, device, 0);
        try {
            //通过反射获取BluetoothA2dp中connect方法（hide的），断开连接。
            Method connectMethod = BluetoothA2dp.class.getMethod("disconnect", BluetoothDevice.class);
            connectMethod.invoke(mBluetoothA2dp, device);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /***********************************************************************************************
     * HFP相关操作
     **********************************************************************************************/
    /**
     * 设置优先级
     *
     * @param device    蓝牙设备
     * @param priority  优先级
     */
    private static void setHFPPriority(Context context, BluetoothDevice device, int priority) {
        if (mBluetoothHeadset == null) {
            init(context);
            return;
        }
        try {//通过反射获取BluetoothA2dp中setPriority方法（hide的），设置优先级
            Method connectMethod = BluetoothHeadset.class.getMethod("setPriority", BluetoothDevice.class,int.class);
            connectMethod.invoke(mBluetoothHeadset, device, priority);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 建立HFP连接
     *
     * @param device 待连接设备
     */
    public static void connectHFP(Context context, BluetoothDevice device) {
        if (mBluetoothHeadset == null) {
            init(context);
            return;
        }
        setHFPPriority(context, device, 100); //设置priority
        try {
            //通过反射获取BluetoothA2dp中connect方法（hide的），进行连接。
            Method connectMethod = BluetoothHeadset.class.getMethod("connect", BluetoothDevice.class);
            connectMethod.invoke(mBluetoothHeadset, device);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 断开HFP连接
     *
     * @param device 待断开设备
     */
    public static void disconnectHFP(Context context, BluetoothDevice device) {
        if (mBluetoothHeadset == null) {
            init(context);
            return;
        }
        setHFPPriority(context, device, 0);
        try {
            //通过反射获取BluetoothHeadset中connect方法（hide的），断开连接。
            Method connectMethod = BluetoothHeadset.class.getMethod("disconnect", BluetoothDevice.class);
            connectMethod.invoke(mBluetoothHeadset, device);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
