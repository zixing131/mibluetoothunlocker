package zixing.bluetooth.unlocker.interfaces;


import android.bluetooth.BluetoothGattService;

import zixing.bluetooth.unlocker.utils.Device;

/* loaded from: classes.dex */
public interface IBluetoothLeConnection extends IBluetoothLeBasicConnection {
    boolean areServicesDiscovered();

    boolean canDisconnect();

    boolean containsKey(String str);

    void enableAllServices();

    Object get(String str);

    Device getDevice();

    int getDfuMaxAvgSpeed();

    String getLogContent();

    boolean isConnected();

    boolean isConnectedToServer();

    boolean isConnectionAttemptDone();

    boolean isMacroRunning();

    boolean isMacroTracked(int i);

    boolean isPredefinedServerService(BluetoothGattService bluetoothGattService);

    boolean isRecordingMacro();

    void newLogSession(boolean z);

    void notifyDatasetChanged(boolean z);

    Object put(String str, Object obj);

    void readAllCharacteristics();

    void registerEddystoneSlot(String str, byte[] bArr);

    Object remove(String str);

    void setAsCentral(boolean z);

    void setAutoConnect(boolean z);

    void setPreferredPhy(Integer num);

    void untrackMacro(int i);
}