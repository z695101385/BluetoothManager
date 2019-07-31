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
import com.jochen.bluetoothmanager.utils.ConfigUtils;
import com.jochen.bluetoothmanager.function.ConnectState;
import com.jochen.bluetoothmanager.utils.LogUtils;
import com.jochen.bluetoothmanager.utils.ProtocolUtils;

import java.util.List;

public class BLEDevice extends BaseDevice {
    public byte[] scanRecord;

    public BLEDevice(boolean isBLE, BluetoothDevice device) {
        super(isBLE, device);
    }

    @Override
    public boolean connect() {
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

    @Override
    public String toString() {
        return "[" + device.getName() + "] mac: " + device.getAddress() + " scanRecord: " + ProtocolUtils.bytesToHexStr(scanRecord);
    }

    /***********************************************************************************************
     * BLE 连接实现
     **********************************************************************************************/
    // write characteristic
    private static BluetoothGattCharacteristic mTxCharacteristic = null;
    // notify characteristic
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
                    if (bluetoothGattService.getUuid().toString().equalsIgnoreCase(ConfigUtils.getRxServiceUUID())) {
                        for (BluetoothGattCharacteristic bluetoothGattCharacteristic : bluetoothGattService.getCharacteristics()) {
                            if (bluetoothGattCharacteristic.getUuid().toString().equalsIgnoreCase(ConfigUtils.getRxCharacteristicUUID())) {
                                RxCharacteristicReady = setRxCharacteristic(bluetoothGattCharacteristic);
                            }
                        }
                    }
                    if (bluetoothGattService.getUuid().toString().equalsIgnoreCase(ConfigUtils.getTxServiceUUID())) {
                        for (BluetoothGattCharacteristic bluetoothGattCharacteristic : bluetoothGattService.getCharacteristics()) {
                            if (bluetoothGattCharacteristic.getUuid().toString().equalsIgnoreCase(ConfigUtils.getTxCharacteristicUUID())) {
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
     * Request a write on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value) {
        if (mBluetoothGatt == null) {
            LogUtils.w("[" + device.getName() + "] mBluetoothGatt not initialized");
            return false;
        }
        characteristic.setValue(value);
        LogUtils.d("[" + device.getName() + "] 发送 长度: " + value.length + " 数据: " + ProtocolUtils.bytesToHexStr(value));
        return mBluetoothGatt.writeCharacteristic(characteristic);
    }

    /**
     * Request a write on a given {@code BluetoothGattDescriptor}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param descriptor The descriptor to read from.
     */
    public void writeDescriptor(BluetoothGattDescriptor descriptor) {
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

    /**
     * 设置Write Characteristic
     *
     * @param bluetoothGattCharacteristic Write Characteristic
     * @return 是为true，否为false
     */
    public boolean setTxCharacteristic(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        int properties = bluetoothGattCharacteristic.getProperties();
        if (((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) || ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0)) {
            mTxCharacteristic = bluetoothGattCharacteristic;
            return true;
        }
        return false;
    }

    /**
     * 设置Notify Characteristic
     *
     * @param bluetoothGattCharacteristic Write Characteristic
     * @return 是为true，否为false
     */
    public boolean setRxCharacteristic(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
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
    public void clearTxRxCharacteristic() {
        mTxCharacteristic = null;
        mRxCharacteristic = null;
    }

    /**
     * Enables or disables notification or indicate on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification or indicate.  False otherwise.
     */
    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
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
                bluetoothGattDescriptor = characteristic.getDescriptor(ConfigUtils.getDescriptorConfigUUID());
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
