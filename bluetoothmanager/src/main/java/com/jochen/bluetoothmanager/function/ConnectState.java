package com.jochen.bluetoothmanager.function;

public class ConnectState {
    public static final int STATE_DISCONNECTED = 0;       // 未连接
    public static final int STATE_CONNECTING   = 1;       // 正在连接
    public static final int STATE_CONNECTED    = 2;       // 已连接
    public static final int STATE_DATA_READY   = 3;       // 读写通道建立完成

    public static String toString(int state) {
        switch (state) {
            case STATE_DISCONNECTED:
                return "未连接";
            case STATE_CONNECTING:
                return "正在连接";
            case STATE_CONNECTED:
                return "已连接";
            case STATE_DATA_READY:
                return "数据通道";
            default:
                return "未知";
        }
    }
}
