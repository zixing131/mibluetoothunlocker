package zixing.bluetooth.unlocker.utils;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* loaded from: classes.dex */
public class AdvData implements Parcelable {
    public static final Parcelable.Creator<AdvData> CREATOR = new Parcelable.Creator<AdvData>() { // from class: no.nordicsemi.android.mcp.ble.model.AdvData.1
        @Override // android.os.Parcelable.Creator
        public AdvData createFromParcel(Parcel parcel) {
            return new AdvData(parcel);
        }

        @Override // android.os.Parcelable.Creator
        public AdvData[] newArray(int i) {
            return new AdvData[i];
        }
    };
    private byte[] data;
    private final List<DataUnion> info;
    private int mAppearance;
    private Boolean mConnectible;
    private String mName;

    public AdvData() {
        this.info = new ArrayList(11);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void copySelectedIndexes(AdvData advData) {
        for (int i = 0; i < advData.info.size() && i < this.info.size(); i++) {
            this.info.get(i).setSelected(advData.info.get(i).getSelectedIndex());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String dataAsString() {
        return Arrays.toString(this.data);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean dataEquals(byte[] bArr) {
        if (bArr == null && this.data == null) {
            return true;
        }
        if (this.data == null || bArr == null) {
            return false;
        }
        for (int i = 0; i < bArr.length; i++) {
            byte[] bArr2 = this.data;
            if ((i >= bArr2.length && bArr[i] != 0) || (i < bArr2.length && bArr[i] != bArr2[i])) {
                return false;
            }
        }
        return true;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public List<DataUnion> getAllInfo() {
        return this.info;
    }

    public int getAppearance() {
        return this.mAppearance;
    }

    public DataUnion getInfo(int i) {
        return this.info.get(i);
    }

    public String getName() {
        return this.mName;
    }

    public byte[] getRawData() {
        return this.data;
    }

    public Boolean isConnectible() {
        return this.mConnectible;
    }

    public DataUnion lastInfo() {
        if (this.info.size() == 0) {
            return newInfo();
        }
        List<DataUnion> list = this.info;
        return list.get(list.size() - 1).clear();
    }

    public DataUnion newInfo() {
        DataUnion dataUnion = new DataUnion();
        this.info.add(dataUnion);
        return dataUnion;
    }

    public void setAppearance(int i) {
        setAppearance(i, false);
    }

    public void setConnectible(boolean z) {
        this.mConnectible = Boolean.valueOf(z);
    }

    public void setName(String str) {
        this.mName = str;
    }

    public void setRawData(byte[] bArr) {
        this.data = bArr;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByteArray(this.data);
        parcel.writeList(this.info);
        parcel.writeInt(this.mAppearance);
        parcel.writeValue(this.mConnectible);
        parcel.writeString(this.mName);
    }

    public void setAppearance(int i, boolean z) {
        if (z || this.mAppearance == 0) {
            this.mAppearance = i;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public AdvData(Parcel parcel) {
        this.data = parcel.createByteArray();
        ArrayList arrayList = new ArrayList(11);
        this.info = arrayList;
        parcel.readList(arrayList, DataUnion.class.getClassLoader());
        this.mAppearance = parcel.readInt();
        this.mConnectible = (Boolean) parcel.readValue(Boolean.class.getClassLoader());
        this.mName = parcel.readString();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public AdvData(AdvData advData) {
        this.info = advData.info;
        this.data = advData.data;
        this.mAppearance = advData.mAppearance;
        this.mConnectible = advData.mConnectible;
        this.mName = advData.mName;
    }
}