package com.jochen.demo.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.jochen.bluetoothmanager.base.BaseDevice;
import com.jochen.bluetoothmanager.ble.BLEDevice;
import com.jochen.bluetoothmanager.utils.ProtocolUtils;
import com.jochen.demo.R;
import com.jochen.demo.item.DeviceItem;
import com.jochen.demo.ui.DeviceActivity;

import java.util.List;

public class DeviceAdapter extends BaseMultiItemQuickAdapter<DeviceItem, BaseViewHolder> {
    private Context mContext;
    /**
     * Same as QuickAdapter#QuickAdapter(Context,int) but with
     * some initialization data.
     *
     * @param data A new list is created out of this one to avoid mutable list
     */
    public DeviceAdapter(Context context, List<DeviceItem> data) {
        super(data);
        mContext = context;
        addItemType(DeviceItem.TYPE_NORMAL, R.layout.item_bluetooth_device);
    }

    @Override
    protected void convert(BaseViewHolder holder, final DeviceItem item) {
        switch (holder.getItemViewType()) {
            case DeviceItem.TYPE_NORMAL:
                holder.setText(R.id.tv_title, item.device.device.getName() == null ? "未知设备" : item.device.device.getName());
                holder.setText(R.id.tv_sub_title, item.device.device.getAddress());
                holder.setText(R.id.tv_content, item.device.isBLE ? "BLE" : "SPP");
                holder.setText(R.id.tv_sub_content, item.device.isBLE ? getRecordString(item.device) : getBondString(item.device.device));
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        jumpTo(item.device);
                    }
                });
                break;
            default:
                break;
        }
    }

    private void jumpTo(BaseDevice device) {
        DeviceActivity.device = device;
        Intent intent = new Intent(mContext, DeviceActivity.class);
        mContext.startActivity(intent);
    }

    private String getRecordString(BaseDevice device) {
        BLEDevice bleDevice = (BLEDevice) device;
        byte[] scanRecord = bleDevice.scanRecord;
        if (scanRecord == null) {
            return "无广播";
        } else {
            return ProtocolUtils.bytesToHexStr(scanRecord);
        }
    }

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
}
