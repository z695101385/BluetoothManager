package com.jochen.bluetoothmanager.base;

import android.bluetooth.BluetoothDevice;

import com.jochen.bluetoothmanager.ble.BLEManager;
import com.jochen.bluetoothmanager.event.Event;
import com.jochen.bluetoothmanager.event.EventCode;
import com.jochen.bluetoothmanager.function.ConnectState;
import com.jochen.bluetoothmanager.function.ReceiveDataCallback;
import com.jochen.bluetoothmanager.function.UUIDConfig;
import com.jochen.bluetoothmanager.spp.SPPManager;
import com.jochen.bluetoothmanager.utils.LogUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件名：BaseDevice
 * 描述：设备Model基类
 * 创建人：jochen.zhang
 * 创建时间：2019/7/30
 */
public abstract class BaseDevice {
    // true BLE; false SPP
    public boolean isBLE;
    // 系统蓝牙Model
    public BluetoothDevice device;
    // 信号强度
    public int rssi;
    // 连接状态
    public int connectionState = ConnectState.STATE_DISCONNECTED;
    // UUID配置
    protected UUIDConfig mUUIDConfig;
    // 接收数据回调
    private List<ReceiveDataCallback> receiveDataCallbackList = new ArrayList<>();

    protected BaseDevice(boolean isBLE, BluetoothDevice device, UUIDConfig uuidConfig) {
        this.isBLE = isBLE;
        this.device = device;
        mUUIDConfig = uuidConfig;
    }

    /**
     * 设置UUID配置信息
     * 设备连接前必须配置UUIDConfig
     * SPPDevice初始化时若未配置UUIDConfig，则默认使用SPP链路UUID
     * BLEDevice必须手动配置UUIDConfig，否则无法连接设备
     *
     * @param uuidConfig UUID配置信息
     */
    public void setUUIDConfig(UUIDConfig uuidConfig) {
        mUUIDConfig = uuidConfig;
    }

    /**
     * 设置连接状态
     *
     * @param state ConnectState
     */
    protected void setConnectState(int state) {
        if (state != connectionState) {
            LogUtils.i("[" + device.getName() + "] 连接状态 " + connectionState + " -> " + state);
            connectionState = state;
            switch (state) {
                case ConnectState.STATE_DISCONNECTED:
                    if (isBLE) {
                        BLEManager.getInstance().removeConnectedDevice(this);
                    } else {
                        SPPManager.getInstance().removeConnectedDevice(this);
                    }
                    break;
                case ConnectState.STATE_CONNECTING:
                    if (isBLE) {
                        BLEManager.getInstance().putConnectedDevice(this);
                    } else {
                        SPPManager.getInstance().putConnectedDevice(this);
                    }
                    break;
                case ConnectState.STATE_CONNECTED:
                    break;
                case ConnectState.STATE_DATA_READY:
                    break;
            }
            EventBus.getDefault().post(new Event<>(EventCode.ConnectionStateChangedCode, this));
        }
    }

    /**
     * 建立连接
     */
    public abstract boolean connect();

    /**
     * 断开连接
     */
    public abstract void disconnect();

    /**
     * 写入数据
     *
     * @param data 代写入数据
     */
    public abstract boolean write(byte[] data);

    /**
     * 设备发送的数据的数据
     * @param data 设备发送的原始数据
     */
    protected void receive(byte[] data) {
        for (ReceiveDataCallback receiveDataCallback: receiveDataCallbackList) {
            receiveDataCallback.onReceive(data);
        }
    }

    /**
     * 注册设备响应数据的监听者
     * @param callback 设备数据回调
     */
    public void registerReceiveDataCallback(ReceiveDataCallback callback) {
        receiveDataCallbackList.add(callback);
    }

    /**
     * 解绑设备响应数据的监听者
     * @param callback 设备数据回调
     */
    public boolean unregisterReceiveDataCallback(ReceiveDataCallback callback) {
        return receiveDataCallbackList.remove(callback);
    }
}
