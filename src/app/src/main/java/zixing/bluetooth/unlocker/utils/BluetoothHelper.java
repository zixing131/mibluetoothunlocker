package zixing.bluetooth.unlocker.utils;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import zixing.bluetooth.unlocker.Xp.MyXp;
import zixing.bluetooth.unlocker.activity.MainActivity;

public class BluetoothHelper {

    private static void myLog(String msg) {
        try {
            Log.i("hookhelper",msg);
            if(MainActivity.self==null)
            {
                XposedBridge.log(msg);
            }
        }
        catch (Exception ex)
        {

        }
    }

    public  static  Object BluetoothControllerImplInstance;
    @SuppressLint("MissingPermission")
    public static boolean CanUnlockByBluetooth(Context context,String mac, ClassLoader classLoader, int type) {
        try {
            int baseRSSI = -50;
            String rssi = ProviderUtil.getString("rssi", "-50", type);
            baseRSSI = Integer.parseInt(rssi);

            myLog("-------------mac---------------" + mac + "----" + baseRSSI);

            if(BluetoothControllerImplInstance==null)
            {
                myLog("-------------BluetoothControllerImplInstance---------------" + BluetoothControllerImplInstance);
                return false;
            }

            final Class BluetoothControllerImplClass = XposedHelpers.findClass("com.android.systemui.statusbar.policy.BluetoothControllerImpl", classLoader);

            final Class CachedBluetoothDeviceClass = XposedHelpers.findClass("com.android.settingslib.bluetooth.CachedBluetoothDevice", classLoader);

            ArrayList ConnectedDevices = (ArrayList) (XposedHelpers.callMethod(BluetoothControllerImplInstance, "getConnectedDevices"));
            if (ConnectedDevices == null || ConnectedDevices.size() == 0) {
                myLog("没有连接的设备");
                return false;
            }
            myLog("连接了设备"+ConnectedDevices.size()+"个设备");
            for (int i = 0; i < ConnectedDevices.size(); i++) {
                Object nowblueitem = ConnectedDevices.get(i);
                BluetoothDevice mDevice = null;
                Field mDeviceField = CachedBluetoothDeviceClass.getDeclaredField("mDevice");
                mDeviceField.setAccessible(true);
                Object mDeviceObj = mDeviceField.get(nowblueitem);
                mDevice = (BluetoothDevice) mDeviceObj;
                if (mDevice != null) {
                    String macaddress = mDevice.getAddress();
                    if (macaddress.equals(mac) == false) {
                        continue;
                    } else {
                        Field mRssiField = CachedBluetoothDeviceClass.getDeclaredField("mRssi");
                        mRssiField.setAccessible(true);
                        short mRssi= (short)mRssiField.get(nowblueitem);
                        myLog("信号强度："+mRssi+"，信号阈值："+baseRSSI);
                        if(mRssi>=baseRSSI)
                        {
                            return true;
                        }
                        return false;
                    }
                }
            }
        } catch (Exception ex) {
            myLog("发生错误：" + ex.toString());
        }
        return false;
    }
    //public static Object BluetoothControllerImplInstance = null;
    @SuppressLint("MissingPermission")
    private static void initbluetoothGattInstance(Context context,String mac, ClassLoader classLoader, int type)
    {
        BluetoothAdapter mDefaultBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mDefaultBluetoothAdapter.isEnabled() == false) {
            return ;
        }
        String maclocal = ProviderUtil.getString("mac","",type);
        if(maclocal!=null && !mac.isEmpty())
        {
            mac = maclocal;
        }

        BluetoothDevice device = mDefaultBluetoothAdapter.getRemoteDevice(mac);
        bluetoothGattInstance = device.connectGatt(context,false,new BluetoothGattCallback()
        {
            @SuppressLint("MissingPermission")
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (newState==BluetoothGatt.STATE_DISCONNECTED){
                    if (bluetoothGattInstance!=null)
                        bluetoothGattInstance.disconnect();
                }
                bluetoothGattInstance=gatt;
            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                super.onReadRemoteRssi(gatt, rssi, status);
                //gatt.close();
                myLog("信号强度："+rssi+"，信号阈值："+baseRSSI);
                if(type == 1)
                {
                    if(rssi>=baseRSSI)
                    {
                        MyXp.SetBluetoothStatus((byte)2);
                        myLog("手环状态-成功");
                    }
                    else{
                        MyXp.SetBluetoothStatus((byte)1);
                        myLog("手环状态-距离太远");
                    }
                }
                else if(type==2)
                {
                    if(rssi>=baseRSSI)
                    {
                        MyXp.UnlockPhone();
                        myLog("已解锁手机");
                    }
                    else{
                        myLog("不符合解锁条件");
                    }
                }

            }
        });
    }
    static int baseRSSI;


    //type1是setting，2是系统界面，0是软件本体
    @SuppressLint("MissingPermission")
    public static void CanUnlockByBluetoothOldDirect(Context context,String mac, ClassLoader classLoader, int type) {
        try {
            if(mac==null || mac.isEmpty())
            {
                return;
            }
            String rssi = ProviderUtil.getString("rssi","-50",type);
            baseRSSI  = Integer.parseInt(rssi);
            if(bluetoothGattInstance==null)
            {
                initbluetoothGattInstance(context,mac,classLoader,type);
            }
            Thread.yield();
            Thread.sleep(100);
            Thread.yield();
            if (bluetoothGattInstance == null) {
                myLog("bluetoothGatt is null");
                return;
            }
            boolean rdRemoteRssi = bluetoothGattInstance.readRemoteRssi();
            if(!rdRemoteRssi)
            {
                myLog("第一次读取rssi失败，重试一次 ");
                bluetoothGattInstance.close();
                Thread.yield();
                Thread.sleep(100);
                Thread.yield();
                initbluetoothGattInstance(context,mac,classLoader,type);
                Thread.yield();
                Thread.sleep(100);
                Thread.yield();
               rdRemoteRssi = bluetoothGattInstance.readRemoteRssi();
               if(!rdRemoteRssi)
               {
                   myLog("二次读取rssi失败 ");
               }
            }
        }catch (Exception ex)
        {
            myLog("发生错误："+ex.toString());
        }
    }
    static  BluetoothGatt bluetoothGattInstance = null;
}
