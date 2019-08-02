# BluetoothManager
Android BLE、SPP的工具类  
Manager: BLEManager、SPPManager BLE、SPP管理类  
Device: BLEDevice、SPPDevice 连接通信实现类，若不需要搜索操作可只使用此类  
BluetoothUtils: 提供基础蓝牙操作、绑定操作、A2DP、HFP的相关操作

## 初始化

```java
BluetoothUtils.init(this);
SPPManager.getInstance().init(this);
BLEManager.getInstance().init(this);
```

## Manager
Manger主要负责设备搜索与管理  
BLEManager、SPPManager都继承于BluetoothManager  
使用BLEManager搜索到的设备就是BLE设备，SPPManager搜索到的设备就是传统蓝牙设备

### BluetoothManager的属性与方法：
```java
// ConnectState不等于DISCONNECT的设备会在数组中
public HashMap<String, BaseDevice> connectedDevices = new HashMap<>();

/**
 * 开始搜索
 *
 * @param callback 扫描回调
 * @return 开启结果
 */
public boolean startScan(BluetoothScanCallback callback)

/**
 * 取消搜索
 */
public void cancelScan()
```

### SPPManager独有方法:
```java
/**
 * 获取已绑定设备
 *
 * @return 绑定设备数组
 */
public List<SPPDevice> getBondedDevices()
```
### 示例
```java
BLEManager.getInstance().startScan(new BluetoothScanCallback(5000) {
    @Override
    public void onScanDevice(BaseDevice device) {
		// 扫描到的BLE设备会从这里回调，每个设备只会回调一次
    }

    @Override
    public void onScanTimeout() {
    }

    @Override
    public void onScanCancel() {
    }
});

BLEManager.getInstance().cancelScan();
```

## Device
Device负责建立链路以及数据通信（可以不依靠Manager单独使用）  
BLEDevice、SPPDevice都继承于BaseDevice  
只需要构造方法中传入系统BluetoothDevice作为参数

### BaseDevice中的属性与方法:
```java
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
```

### 示例
```java
// 连接
BLEDevice device = new BLEDevice(device);
device.registerReceiveDataCallback(mReceiveDataCallback);
device.connect();
// 发送数据
device.write(data);
// 断开
device.disconnect();
device.unregisterReceiveDataCallback(mReceiveDataCallback);
```
