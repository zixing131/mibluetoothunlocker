package zixing.bluetooth.unlocker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.util.Log;
import android.bluetooth.BluetoothDevice;

import java.lang.reflect.Field;
import java.util.ArrayList;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;

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
            String rssi = XSPUtils.getString("rssi", "-50", type);
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

    //type1是setting，2是系统界面，0是软件本体
    @SuppressLint("MissingPermission")
    public static boolean CanUnlockByBluetoothOld(Context context,String mac, ClassLoader classLoader, int type) {
        try {
            int baseRSSI = -50;
            String rssi = XSPUtils.getString("rssi","-50",type);
            baseRSSI = Integer.parseInt(rssi);

            //myLog("------------------------  "+rssi+"  "+baseRSSI);


            //if (BluetoothControllerImplInstance == null) {
            //    return false;
            //}
            BluetoothAdapter mDefaultBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if (mDefaultBluetoothAdapter.isEnabled() == false) {
                return false;
            }

            BluetoothDevice device = mDefaultBluetoothAdapter.getRemoteDevice(mac);

            final int[] rssiready = {-200};

            BluetoothGatt bluetoothGatt = device.connectGatt(context,false,new BluetoothGattCallback()
            {
                @Override
                public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                    super.onReadRemoteRssi(gatt, rssi, status);
                    rssiready[0] = rssi;
                }
            });

            if (bluetoothGatt == null) {

                myLog("bluetoothGatt is null");
                return false;
            }

            Thread.sleep(1);
            Thread.yield();
            Thread.sleep(1);
            Thread.yield();
            Thread.sleep(1);
            Thread.yield();

            boolean rdRemoteRssi = bluetoothGatt.readRemoteRssi();

            if(rdRemoteRssi==false)
            {
                myLog("rdRemoteRssi is false,读取rssi失败 ");
                return false;
            }

            for (int i=0;i<=50;i++)
            {
                Thread.sleep(100);
                Thread.yield();
                if( rssiready[0]>=-150)
                {
                    break;
                }
                //myLog("rssiready"+rssiready[0] );
            }

            myLog("信号强度："+rssiready[0]+"，信号阈值："+baseRSSI);
            if(rssiready[0]>=baseRSSI)
            {
                return true;
            }
            return false;
            /*
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
                        if (mDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                            return false;
                        }
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

             */
        }catch (Exception ex)
        {
            myLog("发生错误："+ex.toString());

            return false;
        }
    }
}
