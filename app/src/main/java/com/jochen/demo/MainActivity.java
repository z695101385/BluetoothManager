package com.jochen.demo;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.jochen.bluetoothmanager.base.BaseDevice;
import com.jochen.bluetoothmanager.ble.BLEManager;
import com.jochen.bluetoothmanager.function.BluetoothScanCallback;
import com.jochen.bluetoothmanager.spp.SPPDevice;
import com.jochen.bluetoothmanager.spp.SPPManager;
import com.jochen.bluetoothmanager.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final int REQUEST_PERMISSIONS_CODE = 0x1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SPPManager.getInstance().init(this);
        BLEManager.getInstance().init(this);
        findViewById(R.id.btn_ble_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BLEManager.getInstance().isScanning()) {
                    BLEManager.getInstance().cancelScan();
                } else {
                    BLEManager.getInstance().startScan(new BluetoothScanCallback(5000) {
                        @Override
                        public void onScanDevice(BaseDevice device) {
                            LogUtils.i(TAG, "BLEManager onScanDevice: " + device.toString());
                            if (device.device.getAddress().equalsIgnoreCase("2C:4D:79:17:71:14")) {
                                BLEManager.getInstance().cancelScan();
                                device.connect();
                            }
                        }

                        @Override
                        public void onScanTimeout() {
                            LogUtils.i(TAG, "BLEManager onScanTimeout");
                        }

                        @Override
                        public void onScanCancel() {
                            LogUtils.i(TAG, "BLEManager onScanCancel");
                        }
                    });
                }
            }
        });
        findViewById(R.id.btn_spp_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SPPManager.getInstance().isScanning()) {
                    SPPManager.getInstance().cancelScan();
                } else {
                    SPPManager.getInstance().startScan(new BluetoothScanCallback(5000) {
                        @Override
                        public void onScanDevice(BaseDevice device) {
                            LogUtils.i(TAG, "SPPManager onScanDevice: " + device.toString());
                        }

                        @Override
                        public void onScanTimeout() {
                            LogUtils.i(TAG, "SPPManager onScanTimeout");
                        }

                        @Override
                        public void onScanCancel() {
                            LogUtils.i(TAG, "SPPManager onScanCancel");
                        }
                    });
                }
            }
        });

        checkPermissions(getManifestPermissions());

        SPPDevice sppDevice = (SPPDevice) SPPManager.getInstance().getDevice("66:55:44:33:22:11");
        sppDevice.connect();
    }

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
