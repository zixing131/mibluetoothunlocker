package zixing.bluetooth.unlocker.interfaces;


import java.util.List;

import zixing.bluetooth.unlocker.utils.Device;

/* loaded from: classes.dex */
public interface IBluetoothLeService {
    IBluetoothLeConnection createConnection(Device device, boolean z);

    void disconnectAndClose(IBluetoothLeConnection iBluetoothLeConnection, boolean z);

    void disconnectAndCloseAll(boolean z);

    List<Device> getConnectedDevices();

    IBluetoothLeConnection getConnection(Device device);

    int getSelectedTabPosition();

    void setSelectedTabPosition(int i);

    void stopServerIfNoConnections();
}