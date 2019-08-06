package com.jochen.bluetoothmanager.function;

import java.util.UUID;

/**
 * 文件名：UUIDConfig
 * 描述：设备连接或通信时使用的UUID
 * 创建人：jochen.zhang
 * 创建时间：2019/8/5
 */
public class UUIDConfig {
    // setNotification时Descriptor所属UUID
    private static final String DESCRIPTOR_CONFIG_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    // SPP UUID
    private static final String SPP_UUID = "00001101-0000-1000-8000-00805f9b34fb";

    /**
     * BLEDevice使用的UUID
     * BLE设备连接成功后，想要进行数据通信，需要读写特征
     *
     * @param rxServiceUUID        读特征所在服务
     * @param rxCharacteristicUUID 读特征
     * @param txServiceUUID        写特征所在服务
     * @param txCharacteristicUUID 写特征
     */
    public UUIDConfig(String rxServiceUUID, String rxCharacteristicUUID, String txServiceUUID, String txCharacteristicUUID) {
        mRxServiceUUID = rxServiceUUID;
        mRxCharacteristicUUID = rxCharacteristicUUID;
        mTxServiceUUID = txServiceUUID;
        mTxCharacteristicUUID = txCharacteristicUUID;
    }

    /**
     * SPPDevice使用的UUID
     * 传统蓝牙设备建立连接时需要建立Rfcomm链路，SPP为指定UUID的Rfcomm链路
     *
     * @param rfcommUUID 传统蓝牙使用的Rfcomm
     */
    public UUIDConfig(String rfcommUUID) {
        mRfcommUUID = rfcommUUID;
    }

    /**
     * 获取SPP设备UUIDConfig
     *
     * @return UUIDConfig
     */
    public static UUIDConfig getSppConfig() {
        return new UUIDConfig(SPP_UUID);
    }

    ///////////////////////////////////////////////////////////////////////////
    // BLE 读写通道 UUID
    ///////////////////////////////////////////////////////////////////////////
    // 读特征所在服务
    private String mRxServiceUUID;
    // 读特征
    private String mRxCharacteristicUUID;
    // 写特征所在服务
    private String mTxServiceUUID;
    // 写特征
    private String mTxCharacteristicUUID;

    public static UUID getDescriptorConfigUUID() {
        return UUID.fromString(DESCRIPTOR_CONFIG_UUID);
    }

    public String getRxServiceUUID() {
        return mRxServiceUUID;
    }

    public String getRxCharacteristicUUID() {
        return mRxCharacteristicUUID;
    }

    public String getTxServiceUUID() {
        return mTxServiceUUID;
    }

    public String getTxCharacteristicUUID() {
        return mTxCharacteristicUUID;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Rfcomm UUID 通常使用SPP
    ///////////////////////////////////////////////////////////////////////////
    // 传统蓝牙设备的Rfcomm链路UUID
    private String mRfcommUUID;

    public UUID getRfcommUUID() {
        return UUID.fromString(mRfcommUUID);
    }
}
