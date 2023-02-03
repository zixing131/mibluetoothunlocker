package zixing.bluetooth.unlocker.utils;


import java.util.List;

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