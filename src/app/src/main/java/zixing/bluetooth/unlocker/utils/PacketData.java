package zixing.bluetooth.unlocker.utils;


import android.os.Parcel;
import android.os.Parcelable;

/* loaded from: classes.dex */
public class PacketData implements Parcelable {
    public static final Parcelable.Creator<PacketData> CREATOR = new Parcelable.Creator<PacketData>() { // from class: no.nordicsemi.android.mcp.ble.model.PacketData.1
        @Override // android.os.Parcelable.Creator
        public PacketData createFromParcel(Parcel parcel) {
            return new PacketData(parcel);
        }

        @Override // android.os.Parcelable.Creator
        public PacketData[] newArray(int i) {
            return new PacketData[i];
        }
    };
    public boolean last;
    public final int rssi;
    public final long timestampNanos;

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.rssi);
        parcel.writeLong(this.timestampNanos);
        parcel.writeInt(this.last ? 1 : 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PacketData(int i, long j) {
        this.rssi = i;
        this.timestampNanos = j;
        this.last = false;
    }

    private PacketData(Parcel parcel) {
        this.rssi = parcel.readInt();
        this.timestampNanos = parcel.readLong();
        this.last = parcel.readInt() != 1 ? false : true;
    }
}