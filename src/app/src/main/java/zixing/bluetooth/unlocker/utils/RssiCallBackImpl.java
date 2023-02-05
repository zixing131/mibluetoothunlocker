package zixing.bluetooth.unlocker.utils;

import android.util.Log;

import zixing.bluetooth.unlocker.interfaces.RssiCallBack;

public class RssiCallBackImpl implements RssiCallBack {
    @Override
    public void processResponse(int rssi) {
        Log.i("RssiCallBackImpl","读取到rssi值："+rssi);
    }
}
