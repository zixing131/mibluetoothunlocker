package zixing.bluetooth.unlocker.interfaces;


import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.net.Uri;
import java.util.List;

/* loaded from: classes.dex */
public interface IBluetoothLeBasicConnection {
    public static final int STATE_CLOSED = 5;
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_DISCONNECTING = 3;
    public static final int STATE_DISCONNECTING_AND_CLOSING = 4;
    public static final int STATUS_DISCONNECTED_PEER_USER = 19;
    public static final int STATUS_FAILED_ABORTED = -2;
    public static final int STATUS_FAILED_ASSERT = -4;
    public static final int STATUS_FAILED_ATT_NOT_FOUND = -3;
    public static final int STATUS_FAILED_TIMEOUT = 8;
    public static final int STATUS_FAILED_UNKNOWN_REASON = -1;
    public static final int STATUS_SUCCESS = 0;

    void abortOperation();

    boolean abortReliableWrite();

    boolean beginReliableWrite();

    void connect();

    void connect(boolean z3);

    boolean createBond();

    void disconnect();

    void discoverServices();

    boolean executeReliableWrite();

    void flashLog(boolean z3);

    int getBondState();

    int getConnectionState();


    String getDeviceName();

    int getLastRemoteRssi();

    Uri getLogSessionUri();

    int getMtu();

    Integer getPreferredPhy();

    List<BluetoothGattService> getServerGattServices(boolean z3);

    List<BluetoothGattService> getSupportedGattServices();

    Boolean getWriteExecuteResult();

    boolean hasAutoConnect();

    boolean hasDfuService();

    boolean hasEddystoneConfigService();

    boolean hasIndicationsEnabled(BluetoothGattCharacteristic bluetoothGattCharacteristic);

    boolean hasNotificationsEnabled(BluetoothGattCharacteristic bluetoothGattCharacteristic);

    boolean hasServicesDiscovered();

    boolean isDfuInProgress();

    boolean isMcuMgr();

    boolean isMicrobit();

    boolean isOperationInProgress();

    boolean isReliableWriteInProgress();

    boolean isThingy();

    void onBluetoothOff();

    void onCharacteristicValueSet(BluetoothGattCharacteristic bluetoothGattCharacteristic);

    void onDescriptorValueSet(BluetoothGattDescriptor bluetoothGattDescriptor);

    void onExecuteWrite(boolean z3);

    void onNotificationSent(int i4);

    void onReadRequest(BluetoothGattCharacteristic bluetoothGattCharacteristic);

    void onReadRequest(BluetoothGattDescriptor bluetoothGattDescriptor);

    void onWriteRequest(BluetoothGattCharacteristic bluetoothGattCharacteristic, BluetoothGattCharacteristic bluetoothGattCharacteristic2);

    void onWriteRequest(BluetoothGattDescriptor bluetoothGattDescriptor, BluetoothGattDescriptor bluetoothGattDescriptor2);

    void readCharacteristic(BluetoothGattCharacteristic bluetoothGattCharacteristic);

    void readDescriptor(BluetoothGattDescriptor bluetoothGattDescriptor);

    boolean readPhy();

    boolean readRemoteRssi();

    boolean refreshCache();

    boolean removeBond();

    boolean requestConnectionPriority(int i4);

    boolean requestMtu(int i4);

    void saveLogAndFlash(int i4, String str);

    void saveLogBulk(int i4, String str);

    void saveLogBulk(int i4, List<BluetoothGattService> list);

    void sendCharacteristicIndication(BluetoothGattCharacteristic bluetoothGattCharacteristic);

    void sendCharacteristicNotification(BluetoothGattCharacteristic bluetoothGattCharacteristic);

    void setCharacteristicIndication(BluetoothGattCharacteristic bluetoothGattCharacteristic, boolean z3);

    void setCharacteristicNotification(BluetoothGattCharacteristic bluetoothGattCharacteristic, boolean z3);

    void setDeviceName(String str);

    void setMtu(int i4);

    boolean setPreferredPhy(int i4, int i5, int i6);

    void setWriteType(BluetoothGattCharacteristic bluetoothGattCharacteristic, int i4);

    void sleep(long j4);

    void sleep(BluetoothGattCharacteristic bluetoothGattCharacteristic, byte[] bArr, boolean z3, long j4, boolean z4);

    void startDfuUpload(int i4, String str, String str2, Uri uri, String str3, Uri uri2);

    void waitForExecuteWrite(boolean z3);

    void waitForNotification(BluetoothGattCharacteristic bluetoothGattCharacteristic);

    void waitForPhyUpdate();

    void waitForReadRequest(BluetoothGattCharacteristic bluetoothGattCharacteristic);

    void waitForReadRequest(BluetoothGattDescriptor bluetoothGattDescriptor);

    void waitForWriteRequest(BluetoothGattCharacteristic bluetoothGattCharacteristic);

    void waitForWriteRequest(BluetoothGattDescriptor bluetoothGattDescriptor);

    int waitUntilOperationCompleted();

    void writeCharacteristic(BluetoothGattCharacteristic bluetoothGattCharacteristic, byte[] bArr);

    void writeDescriptor(BluetoothGattDescriptor bluetoothGattDescriptor, byte[] bArr);
}
