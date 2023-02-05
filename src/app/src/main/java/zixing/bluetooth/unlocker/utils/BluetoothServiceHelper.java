package zixing.bluetooth.unlocker.utils;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import zixing.bluetooth.unlocker.interfaces.IBluetoothLeConnection;
import zixing.bluetooth.unlocker.interfaces.IBluetoothLeService;
import zixing.bluetooth.unlocker.interfaces.RssiCallBack;
import zixing.bluetooth.unlocker.interfaces.ServiceConstants;

public class BluetoothServiceHelper extends android.app.Service{

    protected IBluetoothLeConnection mConnection;

    private static final String TAG = "BluetoothServiceHelper";

    private boolean mAutoConnect=true;

    private IBluetoothLeService mService;

    private Device mDevice;
    private RssiCallBack mRssiCallBack;


    public BluetoothServiceHelper(){

    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.i("BluetoothServiceHelper","BluetoothServiceHelper:oncreate");
        super.onCreate();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("BluetoothServiceHelper","BluetoothServiceHelper:onStartCommand");
        //服务
        Log.i("BluetoothServiceHelper","unbind service providing!");
        return super.onStartCommand(intent, flags, startId);
    }
    @Override
    public void onDestroy() {
        Log.i("BluetoothServiceHelper","BluetoothServiceHelper:onDestroy");
        super.onDestroy();
    }

    public BluetoothServiceHelper(BluetoothDevice bluetoothDevice,RssiCallBack rssiCallBack){
        mDevice=new Device(bluetoothDevice);
        mRssiCallBack = rssiCallBack;
        NewService();
    }



    private int readRssiSelf()
    {
        return rssiSelf;
    }

    public Boolean readRemoteRssi()
    {
        if(this.mConnection==null)
        {
            return false;
        }
        return this.mConnection.readRemoteRssi();
    }

    private int rssiSelf = -120;

    private  BroadcastReceiver mConnectionBroadcastReceiver;

    private ServiceConnection mServiceConnection;
    public void NewService()
    {
        mServiceConnection = new ServiceConnection() {

            @Override // android.content.ServiceConnection
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                IBluetoothLeService iBluetoothLeService = BluetoothServiceHelper.this.mService = (IBluetoothLeService) iBinder;

                IBluetoothLeConnection connection = iBluetoothLeService.getConnection(BluetoothServiceHelper.this.mDevice);
                if (connection == null) {
                    Log.w(BluetoothServiceHelper.TAG, "Fragment bound before activity! Creating connection in the fragment.");
                    connection = iBluetoothLeService.createConnection(BluetoothServiceHelper.this.mDevice, true);
                }

                mConnection = connection;

                if (!connection.isConnectionAttemptDone()) {
                    connection.setAutoConnect(BluetoothServiceHelper.this.mAutoConnect);
                    connection.connect();
                } else {
                    BluetoothServiceHelper.this.mAutoConnect = connection.hasAutoConnect();
                }
            }
            @Override // android.content.ServiceConnection
            public void onServiceDisconnected(ComponentName componentName) {
                BluetoothServiceHelper.this.mService = null;
                BluetoothServiceHelper.this.mConnection = null;
            }
        };
        mConnectionBroadcastReceiver  = new BroadcastReceiver() {
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.startsWith(ServiceConstants.ACTION_RSSI_RECEIVED)) {
                    IBluetoothLeConnection iBluetoothLeConnection = BluetoothServiceHelper.this.mConnection;
                    if (iBluetoothLeConnection != null && iBluetoothLeConnection.isMacroRunning()) {
                        return;
                    }
                    int rssi = Integer.valueOf(intent.getIntExtra(ServiceConstants.EXTRA_DATA, 0));
                    rssiSelf = rssi;
                    mRssiCallBack.processResponse(rssi);
                }
            }
        };

    }
}
