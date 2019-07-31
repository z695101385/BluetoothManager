package com.jochen.bluetoothmanager.function;

/**
 * 文件名：ReceiveDataCallback
 * 描述：
 * 创建人：jochen.zhang
 * 创建时间：2019/4/10.
 */
public interface ReceiveDataCallback {
    /**
     * 接收数据
     *
     * @param data byte[]
     */
    void onReceive(byte[] data);
}
