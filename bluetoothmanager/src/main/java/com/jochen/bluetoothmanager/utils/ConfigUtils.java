package com.jochen.bluetoothmanager.utils;

import java.util.UUID;

/**
 * 文件名：ConfigUtils
 * 描述：连接UUID管理
 * 创建人：jochen.zhang
 * 创建时间：2019/4/10.
 */
public class ConfigUtils {
    ///////////////////////////////////////////////////////////////////////////
    // BLE
    ///////////////////////////////////////////////////////////////////////////
    private static final String DESCRIPTOR_CONFIG_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    private static final String SERVICE_RX_UUID = "00000205-0000-1000-8000-00805F9B34FC";
    private static final String SERVICE_TX_UUID = "00000205-0000-1000-8000-00805F9B34FC";
    private static final String CHARACTERISTIC_RX_UUID = "00000207-0000-1000-8000-00805F9B34FC";
    private static final String CHARACTERISTIC_TX_UUID = "00000208-0000-1000-8000-00805F9B34FC";

    public static UUID getDescriptorConfigUUID() {
        return UUID.fromString(DESCRIPTOR_CONFIG_UUID);
    }

    public static String getRxServiceUUID() {
        return SERVICE_RX_UUID;
    }

    public static String getRxCharacteristicUUID() {
        return CHARACTERISTIC_RX_UUID;
    }

    public static String getTxServiceUUID() {
        return SERVICE_TX_UUID;
    }

    public static String getTxCharacteristicUUID() {
        return CHARACTERISTIC_TX_UUID;
    }
    ///////////////////////////////////////////////////////////////////////////
    // SPP
    ///////////////////////////////////////////////////////////////////////////
    private static final String SPP_UUID = "00001101-0000-1000-8000-00805f9b34fb";

    public static UUID getRfcommUUID() {
        return UUID.fromString(SPP_UUID);
    }
}
