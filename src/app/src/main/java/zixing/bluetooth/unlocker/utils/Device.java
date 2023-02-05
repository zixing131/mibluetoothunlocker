package zixing.bluetooth.unlocker.utils;


import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/* loaded from: classes.dex */
public class Device implements Parcelable {
    private static final String ANONYMOUS_ADDRESS = "00:00:00:00:00:00";
    private boolean mConnectible;
    private final BluetoothDevice mDevice;
    private final int mDeviceIndex;
    private boolean mHadName;
    private int mHighestRssi;
    private long mLastPacketTime;
    private ScanResult mLastScanResult;
    private final LinkedList<AdvDataWithStats> mPackets;
    private final LinkedList<PacketData> mPacketsMetaData;
    private int mRssi;
    private String mUserName;
    private boolean mWasAdvertisingExtension;
    private boolean mWasBluetoothMesh;
    private static final byte[] EMPTY_ARRAY = new byte[0];
    public static final Parcelable.Creator<Device> CREATOR = new Parcelable.Creator<Device>() { // from class: no.nordicsemi.android.mcp.ble.model.Device.1
        @Override // android.os.Parcelable.Creator
        public Device createFromParcel(Parcel parcel) {
            return new Device(parcel);
        }

        @Override // android.os.Parcelable.Creator
        public Device[] newArray(int i) {
            return new Device[i];
        }
    };

    public void clearLastPacketTime() {
        this.mLastPacketTime = 0L;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Device) {
            Device device = (Device) obj;
            if (isAnonymous() && device.isAnonymous()) {
                return this.mPackets.getFirst().dataEquals(device.mPackets.getFirst().getRawData());
            }
            return this.mDevice.equals(device.mDevice);
        } else if (!(obj instanceof BluetoothDevice)) {
            return false;
        } else {
            String address = ((BluetoothDevice) obj).getAddress();
            String substring = address.substring(0, 15);
            String format = String.format(Locale.US, "%02X", Integer.valueOf((Integer.valueOf(address.substring(15), 16).intValue() + 1) & 255));
            StringBuilder sb = new StringBuilder();
            sb.append(substring);
            sb.append(format);
            return getBluetoothDevice().getAddress().equals(address) || getBluetoothDevice().getAddress().equals(sb.toString());
        }
    }

    public String getAddress(Context context) {
        if (isAnonymous()) {
            return "anonymous";
        }
        return this.mDevice.getAddress();
    }

    public long getAdvInterval() {
        return this.mPackets.get(0).getIntervalNanos() / 1000000;
    }

    public int getAppearance() {
        if (!this.mPackets.isEmpty()) {
            return this.mPackets.get(0).getAppearance();
        }
        return 0;
    }

    public BluetoothDevice getBluetoothDevice() {
        return this.mDevice;
    }

    @SuppressLint("MissingPermission")
    public String getBondState() {
        return ParserUtils.bondingStateToString(this.mDevice.getBondState());
    }

    public String getDeviceHash() {
        LinkedList<AdvDataWithStats> linkedList = this.mPackets;
        if (linkedList != null && !linkedList.isEmpty() && isAnonymous()) {
            return this.mPackets.getFirst().dataAsString();
        }
        return this.mDevice.getAddress();
    }

    public int getDeviceIndex() {
        return this.mDeviceIndex;
    }

    public int getHighestRssi() {
        return this.mHighestRssi;
    }

    public List<DataUnion> getInfo() {
        return this.mPackets.getFirst().getAllInfo();
    }

    public int getInfoSize() {
        if (!this.mPackets.isEmpty()) {
            return this.mPackets.getFirst().getAllInfo().size();
        }
        return 0;
    }

    public ScanResult getLastScanResult() {
        return this.mLastScanResult;
    }

    @SuppressLint("MissingPermission")
    public String getName() {
        if (!TextUtils.isEmpty(this.mUserName)) {
            return this.mUserName;
        }
        LinkedList<AdvDataWithStats> linkedList = this.mPackets;
        return linkedList != null ? linkedList.getFirst().getName() : this.mDevice.getName();
    }

    public List<AdvDataWithStats> getPacketsHistory() {
        return this.mPackets;
    }

    public List<PacketData> getPacketsMetaData() {
        return this.mPacketsMetaData;
    }

    public String getRawDataAsString() {
        byte[] rawData = this.mPackets.getFirst().getRawData();
        return (rawData == null || rawData.length == 0) ? "empty" : ParserUtils.bytesToHex(rawData, 0, rawData.length, true);
    }

    public String[] getRawDataDetails() {
        byte[] rawData = this.mPackets.getFirst().getRawData();
        ArrayList arrayList = new ArrayList(12);
        if (rawData != null && rawData.length > 0) {
            int i = 0;
            while (i < rawData.length) {
                int i2 = i + 1;
                int i3 = rawData[i] & FlagsParser.UNKNOWN_FLAGS;
                arrayList.add(String.valueOf(i3));
                arrayList.add(ParserUtils.bytesToHex(rawData, i2, 1, true));
                arrayList.add(ParserUtils.bytesToHex(rawData, i2 + 1, i3 - 1, true));
                i = i3 + i2;
            }
        }
        return (String[]) arrayList.toArray(new String[0]);
    }

    public long getRawDataHash() {
        byte[] rawData = this.mPackets.getFirst().getRawData();
        if (rawData == null || rawData.length == 0) {
            return 0L;
        }
        return Arrays.hashCode(rawData);
    }

    public int getRssi() {
        return this.mRssi;
    }

    public boolean hadName() {
        return this.mHadName || this.mUserName != null;
    }

    public int hashCode() {
        return this.mDevice.hashCode();
    }

    public boolean isAdvertisingExtension() {
        return this.mWasAdvertisingExtension;
    }

    public boolean isAnonymous() {
        return ANONYMOUS_ADDRESS.equals(this.mDevice.getAddress());
    }

    public boolean isBluetoothMeshDevice() {
        return this.mWasBluetoothMesh;
    }

    @SuppressLint("MissingPermission")
    public boolean isBonded() {
        return this.mDevice.getBondState() == 12;
    }

    public boolean isConnectible() {
        return this.mConnectible;
    }

    public boolean isInRange() {
        return this.mLastPacketTime > SystemClock.elapsedRealtime() - 3000;
    }

    public void setUserName(String str) {
        this.mUserName = str;
    }

    public Device smallCopy() {
        if (this.mPackets != null) {
            Device device = new Device(this.mDevice, this.mDeviceIndex);
            device.mUserName = this.mUserName;
            device.mHighestRssi = this.mHighestRssi;
            device.mRssi = this.mRssi;
            device.mHadName = this.mHadName;
            device.mWasBluetoothMesh = this.mWasBluetoothMesh;
            device.mWasAdvertisingExtension = this.mWasAdvertisingExtension;
            device.mConnectible = this.mConnectible;
            device.mPackets.add(this.mPackets.getFirst().copy());
            device.mPacketsMetaData.add(this.mPacketsMetaData.getFirst());
            return device;
        }
        Device device2 = new Device(this.mDevice);
        device2.mUserName = this.mUserName;
        return device2;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ");
        sb.append(!TextUtils.isEmpty(getName()) ? getName() : "n/a");
        sb.append("\nAddress: ");
        sb.append(getAddress(null));
        if (this.mPackets != null) {
            sb.append("\nRSSI: ");
            sb.append(this.mRssi);
            sb.append(" dBm");
            sb.append("\nLast advertisement packet:");
            sb.append("\nRaw data: ");
            sb.append(getRawDataAsString());
            sb.append("\n");
        }
        return sb.toString();
    }
/*

    public void updateInfo(DatabaseHelper databaseHelper, byte[] bArr, int i, long j) {
        this.mLastPacketTime = SystemClock.elapsedRealtime();
        this.mConnectible = true;
        this.mRssi = i;
        if (this.mHighestRssi < i) {
            this.mHighestRssi = i;
        }
        AdvDataWithStats peek = this.mPackets.peek();
        PacketData peek2 = this.mPacketsMetaData.peek();
        this.mPacketsMetaData.addFirst(new PacketData(i, j));
        if (peek != null && peek.dataEquals(bArr)) {
            peek.increaseCount();
            if (peek2 == null || peek2.last) {
                return;
            }
            peek.setIntervalIfLower(j - peek2.timestampNanos);
            return;
        }
        AdvDataWithStats createDataWithStats = GenericAccessProfileParser.createDataWithStats(databaseHelper, this.mDevice, bArr);
        if (peek != null) {
            createDataWithStats.copySelectedIndexes(peek);
        }
        this.mPackets.addFirst(createDataWithStats);
    }
*/

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.mDevice, 0);
        parcel.writeString(this.mUserName);
        parcel.writeInt(this.mRssi);
        parcel.writeInt(this.mHighestRssi);
        parcel.writeInt(this.mWasBluetoothMesh ? 1 : 0);
        parcel.writeInt(this.mWasAdvertisingExtension ? 1 : 0);
        parcel.writeInt(this.mHadName ? 1 : 0);
        parcel.writeInt(this.mDeviceIndex);
        parcel.writeInt(this.mPackets != null ? 1 : 0);
        if (this.mPackets != null) {
            LinkedList linkedList = new LinkedList();
            linkedList.add(this.mPackets.getFirst().copy());
            parcel.writeList(linkedList);
            LinkedList linkedList2 = new LinkedList();
            linkedList2.add(this.mPacketsMetaData.getFirst());
            parcel.writeList(linkedList2);
        }
        parcel.writeInt(this.mConnectible ? 1 : 0);
        if (Build.VERSION.SDK_INT >= 21) {
            parcel.writeParcelable(this.mLastScanResult, 0);
        }
    }

    public Device(BluetoothDevice bluetoothDevice) {
        this.mHighestRssi = -100;
        this.mDevice = bluetoothDevice;
        this.mDeviceIndex = 0;
        this.mPackets = null;
        this.mPacketsMetaData = null;
        this.mConnectible = true;
    }

    public DataUnion getInfo(int i) {
        return this.mPackets.getFirst().getInfo(i);
    }

    public Device(BluetoothDevice bluetoothDevice, int i) {
        this.mHighestRssi = -100;
        this.mDevice = bluetoothDevice;
        this.mDeviceIndex = i;
        this.mPackets = new LinkedList<>();
        this.mPacketsMetaData = new LinkedList<>();
        this.mConnectible = false;
    }

    private Device(Parcel parcel) {
        this.mHighestRssi = -100;
        this.mDevice = (BluetoothDevice) parcel.readParcelable(BluetoothDevice.class.getClassLoader());
        this.mUserName = parcel.readString();
        this.mRssi = parcel.readInt();
        this.mHighestRssi = parcel.readInt();
        boolean z = false;
        this.mWasBluetoothMesh = parcel.readInt() == 1;
        this.mWasAdvertisingExtension = parcel.readInt() == 1;
        this.mHadName = parcel.readInt() == 1;
        this.mDeviceIndex = parcel.readInt();
        if (parcel.readInt() == 1) {
            LinkedList<AdvDataWithStats> linkedList = new LinkedList<>();
            this.mPackets = linkedList;
            parcel.readList(linkedList, AdvDataWithStats.class.getClassLoader());
            LinkedList<PacketData> linkedList2 = new LinkedList<>();
            this.mPacketsMetaData = linkedList2;
            parcel.readList(linkedList2, PacketData.class.getClassLoader());
        } else {
            this.mPackets = null;
            this.mPacketsMetaData = null;
        }
        this.mConnectible = parcel.readInt() == 1 ? true : z;
        if (Build.VERSION.SDK_INT >= 21) {
            this.mLastScanResult = (ScanResult) parcel.readParcelable(ScanResult.class.getClassLoader());
        }
    }
/*
        public void updateInfo(DatabaseHelper databaseHelper, ScanResult scanResult) {
            this.mLastScanResult = scanResult;
            this.mLastPacketTime = SystemClock.elapsedRealtime();
            int i = Build.VERSION.SDK_INT;
            if (i >= 26) {
                this.mConnectible = this.mConnectible || scanResult.isConnectable();
            } else {
                this.mConnectible = true;
            }
            int rssi = scanResult.getRssi();
            this.mRssi = rssi;
            if (this.mHighestRssi < rssi) {
                this.mHighestRssi = rssi;
            }
            byte[] bytes = scanResult.getScanRecord() != null ? scanResult.getScanRecord().getBytes() : EMPTY_ARRAY;
            long timestampNanos = scanResult.getTimestampNanos();
            AdvDataWithStats peek = this.mPackets.peek();
            PacketData peek2 = this.mPacketsMetaData.peek();
            this.mPacketsMetaData.addFirst(new PacketData(rssi, timestampNanos));
            if (peek != null && peek.dataEquals(bytes)) {
                peek.increaseCount();
                if (peek2 != null && !peek2.last) {
                    peek.setIntervalIfLower(timestampNanos - peek2.timestampNanos);
                }
            } else {
                AdvDataWithStats createDataWithStats = GenericAccessProfileParser.createDataWithStats(databaseHelper, this.mDevice, bytes, scanResult);
                if (createDataWithStats.getAppearance() == 26) {
                    this.mWasBluetoothMesh = true;
                }
                if (i >= 26 && !scanResult.isLegacy()) {
                    this.mWasAdvertisingExtension = true;
                }
                if (createDataWithStats.getName() != null) {
                    this.mHadName = true;
                }
                if (peek != null) {
                    createDataWithStats.copySelectedIndexes(peek);
                }
                this.mPackets.addFirst(createDataWithStats);
            }
            if (this.mPackets.size() > 100) {
                for (int count = this.mPackets.removeLast().getCount(); count > 0; count--) {
                    this.mPacketsMetaData.removeLast();
                }
            }
    }*/
}