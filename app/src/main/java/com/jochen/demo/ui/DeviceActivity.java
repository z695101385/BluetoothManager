package com.jochen.demo.ui;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jochen.bluetoothmanager.base.BaseDevice;
import com.jochen.bluetoothmanager.ble.BLEDevice;
import com.jochen.bluetoothmanager.event.Event;
import com.jochen.bluetoothmanager.event.EventCode;
import com.jochen.bluetoothmanager.function.ConnectState;
import com.jochen.bluetoothmanager.function.ReceiveDataCallback;
import com.jochen.bluetoothmanager.utils.BluetoothUtils;
import com.jochen.bluetoothmanager.utils.LogUtils;
import com.jochen.bluetoothmanager.utils.ProtocolUtils;
import com.jochen.demo.R;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * 文件名：DeviceActivity
 * 描述：设备管理界面（SPP、BLE）
 * 功能：
 * 1、连接控制、数据通信
 * 2、SPP：绑定、解绑、A2DP与HFP链路控制
 * 3、BLE：设置MTU
 * 创建人：jochen.zhang
 * 创建时间：2019/8/1
 */
public class DeviceActivity extends AppCompatActivity {
    private static final String TAG = "DeviceActivity";
    public static BaseDevice device;

    private TextView mDeviceNameTextView;
    private Button mOperaButton;

    private TextView mDeviceMacTextView;
    private TextView mConnectStateTextView;

    private LinearLayout mSPPLinearLayout;
    private Button mBondButton;
    private Button mUnBondButton;
    private TextView mBondStateTextView;
    private Button mConnectA2DPButton;
    private Button mDisconnectA2DPButton;
    private Button mConnectHFPButton;
    private Button mDisconnectHFPButton;

    private LinearLayout mBLELinearLayout;
    private EditText mMTUEditText;
    private Button mMTUButton;

    private TextView mCommandTextView;
    private EditText mCommandEditText;
    private Button mSendButton;

    // 设备发送数据回调
    private ReceiveDataCallback mReceiveDataCallback = new ReceiveDataCallback() {
        @Override
        public void onReceive(final byte[] data) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addMessage(false, ProtocolUtils.bytesToHexStr(data));
                }
            });
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        initData();
        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        device.unregisterReceiveDataCallback(mReceiveDataCallback);
    }

    private void initData() {
        EventBus.getDefault().register(this);
        device.registerReceiveDataCallback(mReceiveDataCallback);
    }

    private void initView() {
        // 通用
        mDeviceNameTextView = findViewById(R.id.tv_name);
        mOperaButton = findViewById(R.id.btn_opera);
        mDeviceMacTextView = findViewById(R.id.tv_mac);
        mConnectStateTextView = findViewById(R.id.tv_connect_state);
        mCommandTextView = findViewById(R.id.tv_command);
        mCommandEditText = findViewById(R.id.et_command);
        mSendButton = findViewById(R.id.btn_send);
        // SPP
        mSPPLinearLayout = findViewById(R.id.ll_spp);
        mBondButton = findViewById(R.id.btn_bond);
        mUnBondButton = findViewById(R.id.btn_unbond);
        mBondStateTextView = findViewById(R.id.tv_bond_state);
        mConnectA2DPButton = findViewById(R.id.btn_connect_a2dp);
        mDisconnectA2DPButton = findViewById(R.id.btn_disconnect_a2dp);
        mConnectHFPButton = findViewById(R.id.btn_connect_hfp);
        mDisconnectHFPButton = findViewById(R.id.btn_disconnect_hfp);
        // BLE
        mBLELinearLayout = findViewById(R.id.ll_ble);
        mMTUEditText = findViewById(R.id.et_mtu);
        mMTUButton = findViewById(R.id.btn_mtu);


        mDeviceNameTextView.setText(device.device.getName());
        mDeviceMacTextView.setText(device.device.getAddress());
        mCommandTextView.setMovementMethod(ScrollingMovementMethod.getInstance());

        mOperaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                opera();
            }
        });

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send();
            }
        });

        if (device.isBLE) {
            // BLE设备界面
            mSPPLinearLayout.setVisibility(View.GONE);
            mBLELinearLayout.setVisibility(View.VISIBLE);

            mMTUButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BLEDevice bleDevice = (BLEDevice) device;
                    try {
                        int mtu = Integer.parseInt(mMTUEditText.getText().toString());
                        bleDevice.setMTU(mtu);
                    } catch (NumberFormatException e) {
                        LogUtils.e("MTU输入异常");
                    }
                }
            });
        } else {
            // SPP设备界面
            mSPPLinearLayout.setVisibility(View.VISIBLE);
            mBLELinearLayout.setVisibility(View.GONE);

            mBondButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BluetoothUtils.createBond(DeviceActivity.this, device.device, new BluetoothUtils.BondCallback() {
                        @Override
                        public void onBondState(int state) {
                            LogUtils.i(TAG, "bond state " + state);
                            refreshUI();
                        }
                    });
                }
            });

            mUnBondButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BluetoothUtils.removeBond(DeviceActivity.this, device.device, new BluetoothUtils.BondCallback() {
                        @Override
                        public void onBondState(int state) {
                            LogUtils.i(TAG, "bond state " + state);
                            refreshUI();
                        }
                    });
                }
            });

            mConnectA2DPButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BluetoothUtils.connectA2DP(DeviceActivity.this, device.device);
                }
            });

            mDisconnectA2DPButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BluetoothUtils.disconnectA2DP(DeviceActivity.this, device.device);
                }
            });

            mConnectHFPButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BluetoothUtils.connectHFP(DeviceActivity.this, device.device);
                }
            });

            mDisconnectHFPButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BluetoothUtils.disconnectHFP(DeviceActivity.this, device.device);
                }
            });
        }

        refreshUI();
    }

    /**
     * 刷新UI
     */
    private void refreshUI() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            mConnectStateTextView.setText(ConnectState.toString(device.connectionState));
            if (device.connectionState == ConnectState.STATE_DISCONNECTED) {
                mOperaButton.setText("连接");
            } else {
                mOperaButton.setText("断开");
            }
            if (!device.isBLE) {
                mBondStateTextView.setText(getBondString(device.device));
            }
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshUI();
                }
            });
        }
    }

    /**
     * 连接、断开设备
     */
    private void opera() {
        if (device.connectionState == ConnectState.STATE_DISCONNECTED) {
            //连接
            device.connect();
        } else {
            //断连
            device.disconnect();
        }
    }

    /**
     * 发送数据
     */
    private void send() {
        String command = mCommandEditText.getText().toString();
        mCommandEditText.getText().clear();
        byte[] data = ProtocolUtils.hexStrToBytes(command);
        if (data != null && device.write(data)) {
            addMessage(true, ProtocolUtils.bytesToHexStr(data));
        }
    }

    /**
     * 向TextView添加消息
     *
     * @param isSend true 发送 or false 接收
     * @param message 16进制数据
     */
    private void addMessage(boolean isSend, String message) {
        String command = mCommandTextView.getText().toString();
        if (isSend) {
            if (command.isEmpty()) {
                command = "发送 " + message;
            } else {
                command += "\n发送 " + message;
            }
        } else {
            if (command.isEmpty()) {
                command = "接收 " + message;
            } else {
                command += "\n接收 " + message;
            }
        }
        mCommandTextView.setText(command);
        int offset = mCommandTextView.getLineCount() * mCommandTextView.getLineHeight();
        if (offset > mCommandTextView.getHeight()) {
            mCommandTextView.scrollTo(0, offset - mCommandTextView.getHeight());
        }
    }

    /**
     * 获取绑定状态字符串
     *
     * @param device BluetoothDevice
     * @return String
     */
    private String getBondString(BluetoothDevice device) {
        switch (device.getBondState()) {
            case BluetoothDevice.BOND_NONE:
                return "未绑定";
            case BluetoothDevice.BOND_BONDING:
                return "绑定中";
            case BluetoothDevice.BOND_BONDED:
                return "已绑定";
            default:
                return "未知状态";
        }
    }

    /**
     * 监听EventBus消息
     *
     * @param event 接收到的event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Event event) {
        switch (event.getCode()) {
            case EventCode.ConnectionStateChangedCode:
                if (DeviceActivity.device == event.getData()) {
                    refreshUI();
                }
                break;
        }
    }
}
