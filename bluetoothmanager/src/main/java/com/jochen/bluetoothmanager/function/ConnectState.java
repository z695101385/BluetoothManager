package com.jochen.bluetoothmanager.function;

public class ConnectState {
    public static final int STATE_DISCONNECTED = 0;       // 未连接
    public static final int STATE_CONNECTING   = 1;       // 正在连接
    public static final int STATE_CONNECTED    = 2;       // 已连接
    public static final int STATE_DATA_READY   = 3;       // 读写通道建立完成
}
