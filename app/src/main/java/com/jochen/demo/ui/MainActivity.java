package com.jochen.demo.ui;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import com.jochen.bluetoothmanager.base.BaseDevice;
import com.jochen.bluetoothmanager.ble.BLEManager;
import com.jochen.bluetoothmanager.function.BluetoothScanCallback;
import com.jochen.bluetoothmanager.function.UUIDConfig;
import com.jochen.bluetoothmanager.spp.SPPDevice;
import com.jochen.bluetoothmanager.spp.SPPManager;
import com.jochen.bluetoothmanager.utils.LogUtils;
import com.jochen.demo.R;
import com.jochen.demo.adapter.DeviceAdapter;
import com.jochen.demo.item.DeviceItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * 文件名：MainActivity
 * 描述：主界面（设备管理）
 * 1、搜索BLE、SPP设备
 * 2、获取已连接设备
 * 3、获取已绑定设备
 * 创建人：jochen.zhang
 * 创建时间：2019/8/1
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String SERVICE_RX_UUID = "00000205-0000-1000-8000-00805F9B34FC";
    private static final String SERVICE_TX_UUID = "00000205-0000-1000-8000-00805F9B34FC";
    private static final String CHARACTERISTIC_RX_UUID = "00000207-0000-1000-8000-00805F9B34FC";
    private static final String CHARACTERISTIC_TX_UUID = "00000208-0000-1000-8000-00805F9B34FC";

    private List<DeviceItem> deviceItems = new ArrayList<>();
    private DeviceAdapter mDeviceAdapter;
    private RecyclerView mRecyclerView;
    private Button mBLEButton;
    private Button mSPPButton;
    private Button mConnectedButton;
    private Button mBondedButton;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions(getManifestPermissions());
        initData();
        initView();
    }

    private void initData() {
        mDeviceAdapter = new DeviceAdapter(this, deviceItems);
    }

    private void initView() {
        mRecyclerView = findViewById(R.id.rv_scan_list);
        mBLEButton = findViewById(R.id.btn_scan_ble);
        mSPPButton = findViewById(R.id.btn_scan_spp);
        mConnectedButton = findViewById(R.id.btn_connected);
        mBondedButton = findViewById(R.id.btn_bonded);

        mRecyclerView.setAdapter(mDeviceAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));

        mBLEButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBLEButton.setEnabled(false);
                stopScan();
                deviceItems.clear();
                refreshList();
                BLEManager.getInstance().startScan(new BluetoothScanCallback(5000) {
                    @Override
                    public void onScanDevice(BaseDevice device) {
                        LogUtils.i(TAG, device.toString());
                        UUIDConfig uuidConfig = new UUIDConfig(SERVICE_RX_UUID, CHARACTERISTIC_RX_UUID, SERVICE_TX_UUID, CHARACTERISTIC_TX_UUID);
                        device.setUUIDConfig(uuidConfig);
                        deviceItems.add(new DeviceItem(DeviceItem.TYPE_NORMAL, device));
                        refreshList();
                    }

                    @Override
                    public void onScanTimeout() {
                        refreshList();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mBLEButton.setEnabled(true);
                            }
                        });
                    }

                    @Override
                    public void onScanCancel() {
                        refreshList();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mBLEButton.setEnabled(true);
                            }
                        });
                    }
                });
            }
        });

        mSPPButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSPPButton.setEnabled(false);
                stopScan();
                deviceItems.clear();
                refreshList();
                SPPManager.getInstance().startScan(new BluetoothScanCallback(5000) {
                    @Override
                    public void onScanDevice(BaseDevice device) {
                        LogUtils.i(TAG, device.toString());
                        deviceItems.add(new DeviceItem(DeviceItem.TYPE_NORMAL, device));
                        refreshList();
                    }

                    @Override
                    public void onScanTimeout() {
                        refreshList();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mSPPButton.setEnabled(true);
                            }
                        });
                    }

                    @Override
                    public void onScanCancel() {
                        refreshList();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mSPPButton.setEnabled(true);
                            }
                        });
                    }
                });
            }
        });

        mConnectedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopScan();
                deviceItems.clear();
                Collection<BaseDevice> bleDevices = BLEManager.getInstance().getConnectedDevices().values();
                for (BaseDevice device : bleDevices) {
                    deviceItems.add(new DeviceItem(DeviceItem.TYPE_NORMAL, device));
                }

                Collection<BaseDevice> sppDevices = SPPManager.getInstance().getConnectedDevices().values();
                for (BaseDevice device : sppDevices) {
                    deviceItems.add(new DeviceItem(DeviceItem.TYPE_NORMAL, device));
                }
                refreshList();
            }
        });

        mBondedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopScan();
                List<SPPDevice> devices = SPPManager.getInstance().getBondedDevices();
                deviceItems.clear();
                for (SPPDevice device : devices) {
                    deviceItems.add(new DeviceItem(DeviceItem.TYPE_NORMAL, device));
                }
                refreshList();
            }
        });
    }

    private void stopScan() {
        BLEManager.getInstance().cancelScan();
        SPPManager.getInstance().cancelScan();
    }

    /**
     * 刷新列表
     */
    private void refreshList() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                safeRefresh();
            }
        });
    }

    private boolean isWaiting = false;
    private void safeRefresh() {
        if (isWaiting) return;
        if (mRecyclerView.isComputingLayout()) {
            // 延时递归处理。
            isWaiting = true;
            mRecyclerView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isWaiting = false;
                    safeRefresh();
                }
            }, 100);
        } else {
            mDeviceAdapter.notifyDataSetChanged();
        }
    }

    /***********************************************************************************************
     * 权限请求
     **********************************************************************************************/
    private static final int REQUEST_PERMISSIONS_CODE = 0x1000;
    /**
     * 从Manifest文件中获取Permission数组
     *
     * @return Permission数组
     */
    protected String[] getManifestPermissions() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), PackageManager.GET_PERMISSIONS);
            return packageInfo.requestedPermissions;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 权限请求相关函数
     *
     * @param permissions String[] 所有请求
     */
    protected void checkPermissions(String[] permissions) {
        List<String> permissionDeniedList = new ArrayList<>();
        for (String permission : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted(permission);
            } else {
                permissionDeniedList.add(permission);
            }
        }
        if (!permissionDeniedList.isEmpty()) {
            String[] deniedPermissions = permissionDeniedList.toArray(new String[permissionDeniedList.size()]);
            ActivityCompat.requestPermissions(this, deniedPermissions, REQUEST_PERMISSIONS_CODE);
        }
    }

    @Override
    public final void onRequestPermissionsResult(int requestCode
            , @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSIONS_CODE:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            onPermissionGranted(permissions[i]);
                        } else {
                            onPermissionFailed(permissions[i]);
                        }
                    }
                }
                break;
        }
    }

    /**
     * 权限允许
     *
     * @param permission permission String
     */
    protected void onPermissionGranted(String permission) {
        LogUtils.d(TAG, String.format(Locale.getDefault(), "%s %s", permission, "请求成功"));
    }

    /**
     * 权限拒绝
     *
     * @param permission permission String
     */
    protected void onPermissionFailed(String permission) {
        LogUtils.d(TAG, String.format(Locale.getDefault(), "%s %s", permission, "请求失败"));
    }
}
