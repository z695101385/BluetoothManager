package com.jochen.demo.item;

import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.jochen.bluetoothmanager.base.BaseDevice;

public class DeviceItem implements MultiItemEntity {
    public static final int TYPE_NORMAL = 0;
    public BaseDevice device;
    private int itemType;

    public DeviceItem(int itemType, BaseDevice device) {
        this.device = device;
        this.itemType = itemType;
    }

    @Override
    public int getItemType() {
        return itemType;
    }
}
