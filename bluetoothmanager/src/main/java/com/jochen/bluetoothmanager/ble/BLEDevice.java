package com.jochen.bluetoothmanager.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Build;

import com.jochen.bluetoothmanager.base.BaseDevice;
import com.jochen.bluetoothmanager.function.UUIDConfig;
import com.jochen.bluetoothmanager.function.ConnectState;
import com.jochen.bluetoothmanager.utils.LogUtils;
import com.jochen.bluetoothmanager.utils.ProtocolUtils;

import java.util.List;

/**
 * 文件名：BLEDevice
 * 描述：封装了BLE链路的连接、通信方法
 * 构造只需要传入系统的BluetoothDevice模型
 * BLEDevice bleDevice = new BLEDevice(device);
 * 创建人：jochen.zhang
 * 创建时间：2019/8/2
 */
public class BLEDevice extends BaseDevice {
    public byte[] scanRecord;

    /**
     * 搜索到设备时会调用此构造方法
     * 此时BLEDevice未配置UUID，连接前需要先配置UUID
     *
     * @param device BluetoothDevice
     */
    public BLEDevice(BluetoothDevice device) {
        super(true, device, null);
    }

    /**
     * BLEDevice构造函数
     *
     * @param device BluetoothDevice
     * @param uuidConfig 连接配置的UUID
     */
    public BLEDevice(BluetoothDevice device, UUIDConfig uuidConfig) {
        super(true, device, uuidConfig);
    }

    @Override
    public boolean connect() {
        if (mUUIDConfig == null) {
            LogUtils.e("连接设备前请先配置UUID(setUUIDConfig)");
            return false;
        }
        if (connectionState == ConnectState.STATE_DISCONNECTED) {
            // We want to directly connect to the device, so we are setting the autoConnect
            // parameter to false.
            mBluetoothGatt = device.connectGatt(BLEManager.getInstance().getContext(), false, mGattCallback);
            setConnectState(ConnectState.STATE_CONNECTING);
            return true;
        }
        return false;
    }

    @Override
    public void disconnect() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            // 若刚连接就断开，系统不会回调Disconnect，导致无法更新ConnectState
            // 添加断连操作后关闭GATT操作
            mBluetoothGatt.close();
            mBluetoothGatt = null;
            setConnectState(ConnectState.STATE_DISCONNECTED);
            clearTxRxCharacteristic();
        }
    }

    @Override
    public boolean write(byte[] data) {
        if (mTxCharacteristic == null) {
            LogUtils.w("[" + device.getName() + "] mTxCharacteristic not initialized");
            return false;
        }
        return writeCharacteristic(mTxCharacteristic, data);
    }

    /**
     * 设置MTU
     *
     * @param mtu MTU大小
     * @return 设置结果
     */
    public boolean setMTU(int mtu) {
        if (null == mBluetoothGatt) {
            LogUtils.w("[" + device.getName() + "] mBluetoothGatt not initialized.");
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return mBluetoothGatt.requestMtu(mtu);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "[" + device.getName() + "] mac: " + device.getAddress() + " scanRecord: " + ProtocolUtils.bytesToHexStr(scanRecord);
    }

    /***********************************************************************************************
     * BLE 连接实现
     **********************************************************************************************/
    // 写特征
    private static BluetoothGattCharacteristic mTxCharacteristic = null;
    // 读特征
    private static BluetoothGattCharacteristic mRxCharacteristic = null;
    private BluetoothGatt mBluetoothGatt;

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                setConnectState(ConnectState.STATE_CONNECTED);
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                setConnectState(ConnectState.STATE_DISCONNECTED);
                gatt.close();
                mBluetoothGatt = null;
                clearTxRxCharacteristic();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> bluetoothGattServices = gatt.getServices();
                boolean RxCharacteristicReady = false;
                boolean TxCharacteristicReady = false;
                for (BluetoothGattService bluetoothGattService : bluetoothGattServices) {
                    if (bluetoothGattService.getUuid().toString().equalsIgnoreCase(mUUIDConfig.getRxServiceUUID())) {
                        for (BluetoothGattCharacteristic bluetoothGattCharacteristic : bluetoothGattService.getCharacteristics()) {
                            if (bluetoothGattCharacteristic.getUuid().toString().equalsIgnoreCase(mUUIDConfig.getRxCharacteristicUUID())) {
                                RxCharacteristicReady = setRxCharacteristic(bluetoothGattCharacteristic);
                            }
                        }
                    }
                    if (bluetoothGattService.getUuid().toString().equalsIgnoreCase(mUUIDConfig.getTxServiceUUID())) {
                        for (BluetoothGattCharacteristic bluetoothGattCharacteristic : bluetoothGattService.getCharacteristics()) {
                            if (bluetoothGattCharacteristic.getUuid().toString().equalsIgnoreCase(mUUIDConfig.getTxCharacteristicUUID())) {
                                TxCharacteristicReady = setTxCharacteristic(bluetoothGattCharacteristic);
                            }
                        }
                    }
                }
                if (RxCharacteristicReady && TxCharacteristicReady) {
                    LogUtils.i("[" + device.getName() + "] 读写通道建立完成.");
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    setConnectState(ConnectState.STATE_DATA_READY);
                    return;
                }
            }
            LogUtils.i("[" + device.getName() + "] 读写通道建立失败,断开连接.");
            disconnect();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                receiveData(characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            receiveData(characteristic);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                receiveData(descriptor);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            LogUtils.i("[" + device.getName() + "] onMtuChanged: " + mtu);
        }
    };

    /**
     * 发送数据
     *
     * @param characteristic 待写特征
     * @param value          待发送数据
     */
    private boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value) {
        if (mBluetoothGatt == null) {
            LogUtils.w("[" + device.getName() + "] mBluetoothGatt not initialized");
            return false;
        }
        characteristic.setValue(value);
        LogUtils.d("[" + device.getName() + "] 发送 长度: " + value.length + " 数据: " + ProtocolUtils.bytesToHexStr(value));
        return mBluetoothGatt.writeCharacteristic(characteristic);
    }

    /**
     * 写Descriptor
     *
     * @param descriptor 待写Descriptor
     */
    private void writeDescriptor(BluetoothGattDescriptor descriptor) {
        if (mBluetoothGatt == null) {
            LogUtils.w("[" + device.getName() + "] mBluetoothGatt not initialized");
            return;
        }
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    /**
     * 接收到设备Read或Notify
     *
     * @param characteristic 接收数据的BluetoothGattCharacteristic
     */
    private void receiveData(final BluetoothGattCharacteristic characteristic) {
        LogUtils.d("[" + device.getName() + "] 接收 长度: " + characteristic.getValue().length + " 数据: " + ProtocolUtils.bytesToHexStr(characteristic.getValue()));
        receive(characteristic.getValue());
    }

    /**
     * 接收到设备Descriptor
     *
     * @param descriptor 接收数据的BluetoothGattDescriptor
     */
    private void receiveData(final BluetoothGattDescriptor descriptor) {
        LogUtils.d("[" + device.getName() + "] 接收 长度: " + descriptor.getValue().length + " 数据: " + ProtocolUtils.bytesToHexStr(descriptor.getValue()));
        receive(descriptor.getValue());
    }

    /**
     * 设置写通道
     *
     * @param bluetoothGattCharacteristic 写特征
     * @return 是为true，否为false
     */
    private boolean setTxCharacteristic(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        int properties = bluetoothGattCharacteristic.getProperties();
        if (((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) || ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0)) {
            mTxCharacteristic = bluetoothGattCharacteristic;
            return true;
        }
        return false;
    }

    /**
     * 设置读通道
     *
     * @param bluetoothGattCharacteristic 读特征
     * @return 是为true，否为false
     */
    private boolean setRxCharacteristic(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        int properties = bluetoothGattCharacteristic.getProperties();
        if (((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) || ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0)) {
            mRxCharacteristic = bluetoothGattCharacteristic;
            return setCharacteristicNotification(mRxCharacteristic, true);
        }
        return false;
    }

    /**
     * 清空上行下行通道
     */
    private void clearTxRxCharacteristic() {
        mTxCharacteristic = null;
        mRxCharacteristic = null;
    }

    /**
     * 开启/关闭Notification
     *
     * @param characteristic 待操作特征.
     * @param enabled        true 开启; false 关闭
     */
    private boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothGatt == null) {
            LogUtils.w("[" + device.getName() + "] mBluetoothGatt not initialized");
            return false;
        }

        if (mBluetoothGatt.setCharacteristicNotification(characteristic, enabled)) {
            //查找CLIENT_CHARACTERISTIC_CONFIG的BluetoothGattDescriptor
            BluetoothGattDescriptor bluetoothGattDescriptor;
            if (characteristic.getDescriptors() != null && characteristic.getDescriptors().size() == 1) {
                bluetoothGattDescriptor = characteristic.getDescriptors().get(0);
            } else {
                bluetoothGattDescriptor = characteristic.getDescriptor(UUIDConfig.getDescriptorConfigUUID());
            }

            if (bluetoothGattDescriptor != null) {
                int properties = characteristic.getProperties();
                if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    if (enabled) {
                        bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    } else {
                        bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    }
                } else if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
                    if (enabled) {
                        bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    } else {
                        bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    }
                }
                writeDescriptor(bluetoothGattDescriptor);
            }
            return true;
        } else {
            return false;
        }
    }
}
